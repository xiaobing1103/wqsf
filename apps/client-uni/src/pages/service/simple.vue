<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import AppIcon from '../../components/AppIcon.vue'
import StatusBadge from '../../components/StatusBadge.vue'
import { errorMessage } from '../../api/response'
import { caseStatusLabel, caseStatusTone } from '../../constants/status'
import { serviceType } from '../../constants/service'
import { useAuthStore } from '../../stores/auth'
import { useCaseDraftStore } from '../../stores/caseDraft'
import { useCatalogStore } from '../../stores/catalog'
import { useCompanyStore } from '../../stores/company'

const authStore = useAuthStore()
const caseDraftStore = useCaseDraftStore()
const catalogStore = useCatalogStore()
const companyStore = useCompanyStore()
const pageError = ref('')
const routeQuery = ref<Record<string, string>>()
const ready = ref(false)
const saving = ref(false)
const consent = ref(false)
const requestedProductId = ref<number>()
const form = reactive({ legalPersonName: '', companyName: '', contactPhone: '' })

const detail = computed(() => caseDraftStore.caseDetail)
const product = computed(() => catalogStore.products.find((item) => item.id === detail.value?.productId || item.id === requestedProductId.value))
const serviceName = computed(() => product.value?.productName || (detail.value?.serviceType === 'QUALIFICATION' ? '资质申报服务' : '商标注册咨询'))
const editable = computed(() => ['DRAFT', 'NEED_SUPPLEMENT'].includes(detail.value?.status || ''))
const savedPhone = computed(() => detail.value?.materials?.some((item) => item.materialCode === 'CONTACT_PHONE' && item.submitStatus === 'SUBMITTED') || false)
const notes = computed(() => (detail.value?.materials || []).filter((item) => item.clientVisibleNote))

function syncForm() {
  const company = companyStore.companies.find((item) => item.id === detail.value?.companyId) || companyStore.selectedCompany
  form.legalPersonName = detail.value?.legalPersonName || company?.legalPerson || ''
  form.companyName = detail.value?.companyName || company?.companyName || ''
  form.contactPhone = ''
}

function validateForm(requirePhone: boolean) {
  if (!form.legalPersonName.trim()) return '请填写法人姓名'
  if (!form.companyName.trim()) return '请填写公司名称'
  if (requirePhone && !form.contactPhone.trim()) return '请填写联系电话'
  if (form.contactPhone.trim() && !/^[0-9+ -]{7,20}$/.test(form.contactPhone.trim())) return '联系电话格式不正确'
  if (!consent.value) return '请先确认资料使用授权'
  return ''
}

async function saveForm(showToast = true) {
  const detailValue = detail.value
  if (!detailValue) throw new Error('咨询报单尚未加载')
  const nameChanged = form.legalPersonName.trim() !== (detailValue.legalPersonName || '')
    || form.companyName.trim() !== (detailValue.companyName || '')
  const needsSupplement = detailValue.status === 'NEED_SUPPLEMENT'
    || (detailValue.materials || []).some((item) => item.reviewStatus === 'REJECTED')
  const message = validateForm(!savedPhone.value || nameChanged || needsSupplement)
  if (message) throw new Error(message)
  if (form.contactPhone.trim()) {
    await caseDraftStore.saveSimpleForm({
      legalPersonName: form.legalPersonName.trim(),
      companyName: form.companyName.trim(),
      contactPhone: form.contactPhone.trim(),
      consent: true,
    })
    syncForm()
  }
  if (showToast) uni.showToast({ title: '草稿已保存', icon: 'success' })
}

async function saveDraft() {
  if (!editable.value || saving.value) return
  saving.value = true
  try {
    await saveForm()
  } catch (cause) {
    uni.showToast({ title: errorMessage(cause, '保存失败'), icon: 'none', duration: 2800 })
  } finally {
    saving.value = false
  }
}

async function submitConsultation() {
  if (!editable.value || saving.value) return
  saving.value = true
  try {
    await saveForm(false)
    await caseDraftStore.submitCurrentCase()
    uni.showToast({ title: '提交成功', icon: 'success' })
    setTimeout(() => uni.redirectTo({ url: '/pages/case/list?filter=processing' }), 600)
  } catch (cause) {
    uni.showToast({ title: errorMessage(cause, '提交失败'), icon: 'none', duration: 3000 })
  } finally {
    saving.value = false
  }
}

async function loadPage(query?: Record<string, string>) {
  if (query) routeQuery.value = query
  const currentQuery = query || routeQuery.value
  ready.value = false
  pageError.value = ''
  try {
    await authStore.ensureSession()
    await Promise.all([companyStore.loadCompanies(), catalogStore.loadCatalog()])
    const caseId = Number(currentQuery?.caseId)
    if (Number.isFinite(caseId) && caseId > 0) {
      const loaded = await caseDraftStore.loadCase(caseId)
      if (loaded.serviceType === 'TRADE_INCREMENT') throw new Error('该报单不属于咨询简表')
      if (loaded.companyId) companyStore.selectCompany(loaded.companyId)
    } else {
      const productId = Number(currentQuery?.productId)
      if (!Number.isFinite(productId) || productId <= 0) throw new Error('服务缺少产品编号')
      const selectedProduct = catalogStore.products.find((item) => item.id === productId)
      if (!selectedProduct) throw new Error('服务产品不存在或尚未发布')
      const type = serviceType(selectedProduct)
      if (type === 'TRADE_INCREMENT') throw new Error('贸易增量服务不使用咨询简表')
      const companyId = companyStore.selectedCompany?.id
      if (!companyId) throw new Error('当前账号尚未绑定企业')
      requestedProductId.value = productId
      await caseDraftStore.prepareSimpleDraft(companyId, productId, type)
    }
    syncForm()
    uni.setNavigationBarTitle({ title: serviceName.value })
    ready.value = true
  } catch (cause) {
    pageError.value = errorMessage(cause, '咨询报单加载失败')
  }
}

onLoad((query) => { void loadPage(query as Record<string, string> | undefined) })
</script>

<template>
  <view class="simple-page">
    <view v-if="!ready && !pageError" class="state-panel">
      <text class="state-title">正在准备咨询报单</text><text class="state-copy">正在读取企业资料和服务模板。</text>
    </view>
    <view v-else-if="pageError" class="state-panel error-panel">
      <text class="state-title">页面加载失败</text><text class="state-copy">{{ pageError }}</text>
      <button class="state-action" @click="loadPage()">重新加载</button>
    </view>

    <template v-else-if="detail">
      <view class="summary-card">
        <view><text class="summary-kicker">企业服务咨询</text><text class="summary-title">{{ serviceName }}</text><text class="summary-no">{{ detail.caseNo || '报单编号生成中' }}</text></view>
        <StatusBadge :label="caseStatusLabel(detail.status)" :tone="caseStatusTone(detail.status)" />
      </view>

      <view v-if="notes.length" class="supplement-card">
        <text class="supplement-title">需要补充的信息</text>
        <text v-for="item in notes" :key="item.id" class="supplement-copy">{{ item.materialName }}：{{ item.clientVisibleNote }}</text>
      </view>

      <view class="form-card">
        <view class="section-heading"><text class="section-step">基础信息</text><text class="section-note">仅收集本次咨询所需的三项信息</text></view>
        <label class="field"><text>法人姓名</text><input v-model="form.legalPersonName" :disabled="!editable" maxlength="50" placeholder="请输入法人姓名" /></label>
        <label class="field"><text>公司名称</text><input v-model="form.companyName" :disabled="!editable" maxlength="100" placeholder="请输入公司全称" /></label>
        <label class="field">
          <text>联系电话</text>
          <input v-model="form.contactPhone" :disabled="!editable" type="tel" maxlength="20" :placeholder="savedPhone ? `已保存 ${detail.contactPhoneMask || '脱敏号码'}，修改请重新输入` : '请输入联系电话'" />
          <text v-if="savedPhone && !form.contactPhone" class="saved-tip">已安全保存：{{ detail.contactPhoneMask }}</text>
        </label>

        <button v-if="editable" class="consent-row" @click="consent = !consent">
          <view class="check-box" :class="{ checked: consent }"><AppIcon v-if="consent" type="checkmarkempty" :size="15" /></view>
          <text>我确认上述资料真实，并授权用于本次业务咨询与后续联系。</text>
        </button>
        <view v-else class="readonly-note">当前报单已提交，资料仅供查看。如需修改，请等待后台发起补件。</view>
      </view>

      <view v-if="editable" class="action-bar">
        <button class="secondary-action" :disabled="saving" @click="saveDraft"><AppIcon type="compose" :size="18" />保存草稿</button>
        <button class="primary-action" :disabled="saving" @click="submitConsultation"><AppIcon type="paperplane" :size="18" />{{ saving ? '处理中…' : '提交咨询' }}</button>
      </view>
    </template>
  </view>
</template>

<style scoped>
.simple-page { min-height: 100vh; padding-bottom: calc(160rpx + env(safe-area-inset-bottom)); background: linear-gradient(180deg, #fff 0, #f4f7f7 28%); }
.summary-card { display: flex; align-items: flex-start; justify-content: space-between; margin: 30rpx 32rpx 0; padding: 32rpx; border-radius: 28rpx; background: linear-gradient(135deg, #174f50, #267170); color: #fff; box-shadow: 0 16rpx 34rpx rgba(24, 78, 79, .15); }
.summary-kicker, .summary-title, .summary-no { display: block; }
.summary-kicker { color: #b8d9d7; font-size: 19rpx; font-weight: 700; letter-spacing: 2rpx; }
.summary-title { margin-top: 10rpx; font-size: 34rpx; font-weight: 900; }
.summary-no { margin-top: 12rpx; color: #d6e6e5; font-size: 18rpx; }
.supplement-card { margin: 18rpx 32rpx 0; padding: 24rpx; border: 1rpx solid #efcf98; border-radius: 22rpx; background: #fff5e4; }
.supplement-title, .supplement-copy { display: block; }
.supplement-title { color: #7d5214; font-size: 23rpx; font-weight: 800; }
.supplement-copy { margin-top: 9rpx; color: #8d682e; font-size: 20rpx; line-height: 1.55; }
.form-card { margin: 20rpx 32rpx 0; padding: 30rpx; border: 1rpx solid var(--color-border); border-radius: 28rpx; background: #fff; }
.section-heading { margin-bottom: 8rpx; }
.section-step, .section-note { display: block; }
.section-step { font-size: 30rpx; font-weight: 900; }
.section-note { margin-top: 7rpx; color: var(--color-muted); font-size: 20rpx; }
.field { display: block; min-width: 0; margin-top: 26rpx; }
.field > text:first-child { display: block; margin-bottom: 10rpx; font-size: 22rpx; font-weight: 800; }
.field input { display: block; box-sizing: border-box; min-width: 0; max-width: 100%; width: 100%; height: 84rpx; padding: 0 22rpx; border: 1rpx solid #d8e2e2; border-radius: 18rpx; background: #f9fbfb; font-size: 24rpx; }
.field input[disabled] { color: #667477; background: #f1f4f4; }
.saved-tip { display: block; margin-top: 9rpx; color: #27706f; font-size: 19rpx; }
.consent-row { display: flex; align-items: flex-start; width: 100%; margin: 28rpx 0 0; padding: 20rpx; border-radius: 18rpx; background: #eef7f6; color: #4f6668; font-size: 20rpx; line-height: 1.55; text-align: left; }
.check-box { display: flex; flex: none; align-items: center; justify-content: center; width: 34rpx; height: 34rpx; margin: 1rpx 15rpx 0 0; border: 2rpx solid #8da6a6; border-radius: 8rpx; background: #fff; color: #fff; font-size: 22rpx; line-height: 1; }
.check-box.checked { border-color: var(--color-primary-light); background: var(--color-primary-light); }
.readonly-note { margin-top: 26rpx; padding: 20rpx; border-radius: 18rpx; background: #f2f5f5; color: #6f7d80; font-size: 20rpx; line-height: 1.55; }
.action-bar { position: fixed; right: 0; bottom: 0; left: 0; z-index: 10; display: grid; grid-template-columns: 1fr 1.55fr; gap: 18rpx; padding: 20rpx 32rpx calc(20rpx + env(safe-area-inset-bottom)); border-top: 1rpx solid #dfe7e7; background: rgba(255,255,255,.97); }
.action-bar button { display: flex; align-items: center; justify-content: center; gap: 9rpx; height: 78rpx; margin: 0; border-radius: 20rpx; font-size: 24rpx; font-weight: 800; line-height: 78rpx; }
.secondary-action { border: 1rpx solid #b8caca; background: #fff; color: var(--color-primary); }
.primary-action { background: linear-gradient(90deg, #164f50, #287371); color: #fff; }
.action-bar button[disabled] { opacity: .62; }
.error-panel { background: #fff8ef; }
</style>
