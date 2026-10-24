<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import AppIcon from '../../components/AppIcon.vue'
import FileUploadCard from '../../components/FileUploadCard.vue'
import CaseTimeNotice from '../../components/CaseTimeNotice.vue'
import type { InvoiceRequest, ItemView, MaterialView } from '../../api/generated/models'
import { errorMessage } from '../../api/response'
import type { SelectedLocalFile } from '../../composables/useFileUpload'
import { useCaseDraftStore } from '../../stores/caseDraft'
import { useCompanyStore } from '../../stores/company'
import { useCapabilitiesStore } from '../../stores/capabilities'
const capabilities = useCapabilitiesStore()

type FormKey = keyof InvoiceRequest

const caseDraftStore = useCaseDraftStore()
const companyStore = useCompanyStore()
const caseId = ref<number>()
const activeTab = ref<'invoice' | 'files'>('invoice')
const focusedMaterialId = ref<number>()
const pageError = ref('')
const savingInvoice = ref(false)
const editingInvoice = ref(false)
const urlValues = reactive<Record<number, string>>({})

const invoiceForm = reactive<InvoiceRequest>({
  companyName: '',
  taxNo: '',
  contactAddress: '',
  contactPhone: '',
  legalPerson: '',
  companyEmail: '',
  bankBranch: '',
  basicAccount: '',
})

const invoiceFields: Array<{ key: FormKey; label: string; placeholder: string; sensitive?: boolean }> = [
  { key: 'companyName', label: '公司名称', placeholder: '请输入公司全称' },
  { key: 'taxNo', label: '税号', placeholder: '请输入纳税人识别号', sensitive: true },
  { key: 'contactAddress', label: '联系地址', placeholder: '请输入公司联系地址' },
  { key: 'contactPhone', label: '联系电话', placeholder: '请输入手机号或企业座机', sensitive: true },
  { key: 'legalPerson', label: '法定代表人', placeholder: '请输入法人姓名' },
  { key: 'companyEmail', label: '公司邮箱', placeholder: '请输入公司邮箱' },
  { key: 'bankBranch', label: '开户行网点', placeholder: '请输入开户行网点' },
  { key: 'basicAccount', label: '基本户账号', placeholder: '请输入基本户账号', sensitive: true },
]

const fileMaterials = computed(() => caseDraftStore.materials.filter((item) => item.inputType === 'FILE'))
const urlMaterials = computed(() => caseDraftStore.materials.filter((item) => item.inputType === 'URL'))
const invoiceSaved = computed(() => Boolean(caseDraftStore.caseDetail?.invoiceInfo))

function templateItem(material: MaterialView): ItemView | undefined {
  return caseDraftStore.template?.items?.find((item) => item.itemCode === material.materialCode)
}

function hydrateInvoiceForm() {
  const invoice = caseDraftStore.caseDetail?.invoiceInfo
  const company = companyStore.selectedCompany
  invoiceForm.companyName = invoice?.companyName || company?.companyName || caseDraftStore.caseDetail?.companyName || ''
  invoiceForm.contactAddress = invoice?.contactAddress || [company?.province, company?.city, company?.detailAddress].filter(Boolean).join('')
  invoiceForm.legalPerson = invoice?.legalPerson || company?.legalPerson || ''
  invoiceForm.companyEmail = invoice?.companyEmail || company?.companyEmail || ''
  invoiceForm.bankBranch = invoice?.bankBranch || ''
  invoiceForm.taxNo = ''
  invoiceForm.contactPhone = ''
  invoiceForm.basicAccount = ''
  for (const material of urlMaterials.value) {
    if (material.id) urlValues[material.id] = material.textValue || ''
  }
}

function validateInvoice() {
  const missing = invoiceFields.find((field) => !String(invoiceForm[field.key] || '').trim())
  if (missing) throw new Error(`请填写${missing.label}`)
  if (!/^[0-9+ -]{7,20}$/.test(invoiceForm.contactPhone)) throw new Error('联系电话格式不正确')
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(invoiceForm.companyEmail)) throw new Error('公司邮箱格式不正确')
  if (!/^[0-9A-Za-z]+$/.test(invoiceForm.taxNo)) throw new Error('税号只能包含数字和字母')
  if (!/^[0-9 ]+$/.test(invoiceForm.basicAccount)) throw new Error('基本户账号只能包含数字和空格')
}

async function saveInvoiceForm() {
  savingInvoice.value = true
  try {
    validateInvoice()
    await caseDraftStore.saveInvoiceInfo({
      ...invoiceForm,
      companyName: invoiceForm.companyName.trim(),
      taxNo: invoiceForm.taxNo.trim(),
      contactAddress: invoiceForm.contactAddress.trim(),
      contactPhone: invoiceForm.contactPhone.trim(),
      legalPerson: invoiceForm.legalPerson.trim(),
      companyEmail: invoiceForm.companyEmail.trim(),
      bankBranch: invoiceForm.bankBranch.trim(),
      basicAccount: invoiceForm.basicAccount.replace(/\s/g, ''),
    })
    editingInvoice.value = false
    hydrateInvoiceForm()
    uni.showToast({ title: '开票信息已保存', icon: 'success' })
    activeTab.value = 'files'
  } catch (cause) {
    uni.showToast({ title: errorMessage(cause, '开票信息保存失败'), icon: 'none', duration: 2800 })
  } finally {
    savingInvoice.value = false
  }
}

function showFileError(cause: Error) {
  uni.showToast({ title: errorMessage(cause, '文件上传失败'), icon: 'none', duration: 3000 })
}

async function uploadSelectedFiles(material: MaterialView, files: SelectedLocalFile[]) {
  if (!material.id || !caseDraftStore.isEditable) return
  try {
    for (const file of files) {
      await caseDraftStore.uploadFile(material.id, file.path)
    }
    if (files.length) uni.showToast({ title: '文件上传成功', icon: 'success' })
  } catch (cause) {
    uni.showToast({ title: errorMessage(cause, '文件上传失败'), icon: 'none', duration: 3000 })
  }
}

async function deleteFile(materialId: number, fileId: number) {
  const confirmed = await new Promise<boolean>((resolve) => {
    uni.showModal({ title: '删除文件', content: '删除后需要重新上传，确认继续吗？', confirmColor: '#a3483e', success: (result) => resolve(result.confirm), fail: () => resolve(false) })
  })
  if (!confirmed) return
  try {
    await caseDraftStore.deleteFile(materialId, fileId)
    uni.showToast({ title: '文件已删除', icon: 'success' })
  } catch (cause) {
    uni.showToast({ title: errorMessage(cause, '删除文件失败'), icon: 'none' })
  }
}

async function saveUrl(material: MaterialView) {
  if (!material.id) return
  const value = String(urlValues[material.id] || '').trim()
  if (!/^https?:\/\//i.test(value)) {
    uni.showToast({ title: '请输入有效的 http/https 链接', icon: 'none' })
    return
  }
  try {
    await caseDraftStore.saveTextMaterial(material.id, value)
    urlValues[material.id] = value
    uni.showToast({ title: '链接已保存', icon: 'success' })
  } catch (cause) {
    uni.showToast({ title: errorMessage(cause, '链接保存失败'), icon: 'none' })
  }
}

function copyMaterialUrl(material: MaterialView) {
  if (material.textValue) uni.setClipboardData({ data: material.textValue })
}

function nextStep() {
  if (activeTab.value === 'invoice') {
    if (!invoiceSaved.value || editingInvoice.value) void saveInvoiceForm()
    else activeTab.value = 'files'
    return
  }
  uni.navigateBack()
}

async function loadPage() {
  if (!caseId.value) { pageError.value = '缺少报单编号'; return }
  pageError.value = ''
  try {
    if (!companyStore.companies.length) await companyStore.loadCompanies()
    await caseDraftStore.loadCase(caseId.value)
    uni.setNavigationBarTitle({ title: caseDraftStore.isEditable ? '填写报单资料' : '查看报单资料' })
    hydrateInvoiceForm()
    editingInvoice.value = !invoiceSaved.value
  } catch (cause) {
    pageError.value = errorMessage(cause, '资料页面加载失败')
  }
}

onLoad((query) => {
  const value = Number(query?.caseId)
  if (Number.isFinite(value) && value > 0) caseId.value = value
  activeTab.value = query?.tab === 'files' ? 'files' : 'invoice'
  const materialId = Number(query?.materialId)
  if (Number.isFinite(materialId) && materialId > 0) focusedMaterialId.value = materialId
  void loadPage()
})
</script>

<template>
  <view class="invoice-page">
    <view class="content">
      <view class="page-heading">
        <text class="heading-eyebrow">STEP 2/2</text>
        <text class="heading-title">开票信息与附件</text>
        <text class="heading-copy">敏感字段只发送到后端加密保存，不写入本地缓存。</text>
      </view>

      <view v-if="caseDraftStore.loading && !caseDraftStore.caseDetail" class="state-panel">
        <text class="state-title">正在读取报单资料</text>
      </view>
      <view v-else-if="pageError" class="state-panel error-panel">
        <text class="state-title">页面加载失败</text><text class="state-copy">{{ pageError }}</text>
        <button class="state-action" @click="loadPage">重新加载</button>
      </view>

      <template v-else-if="caseDraftStore.caseDetail">
        <CaseTimeNotice :detail="caseDraftStore.caseDetail" />
        <view class="tab-switch">
          <button :class="{ active: activeTab === 'invoice' }" @click="activeTab = 'invoice'">开票信息</button>
          <button :class="{ active: activeTab === 'files' }" @click="activeTab = 'files'">附件资料</button>
        </view>

        <view v-if="activeTab === 'invoice'" class="invoice-section">
          <view v-if="invoiceSaved && !editingInvoice" class="saved-invoice">
            <view class="saved-title"><text>开票信息已保存</text><button v-if="caseDraftStore.isEditable && capabilities.flags.sensitiveMaterialUploadEnabled" @click="editingInvoice = true">重新填写</button></view>
            <view class="saved-grid">
              <view><text>公司名称</text><strong>{{ caseDraftStore.caseDetail.invoiceInfo?.companyName }}</strong></view>
              <view><text>税号</text><strong>{{ caseDraftStore.caseDetail.invoiceInfo?.taxNoMask }}</strong></view>
              <view><text>联系电话</text><strong>{{ caseDraftStore.caseDetail.invoiceInfo?.contactPhoneMask }}</strong></view>
              <view><text>基本户账号</text><strong>{{ caseDraftStore.caseDetail.invoiceInfo?.basicAccountMask }}</strong></view>
              <view><text>联系地址</text><strong>{{ caseDraftStore.caseDetail.invoiceInfo?.contactAddress }}</strong></view>
              <view><text>法人姓名</text><strong>{{ caseDraftStore.caseDetail.invoiceInfo?.legalPerson }}</strong></view>
              <view><text>公司邮箱</text><strong>{{ caseDraftStore.caseDetail.invoiceInfo?.companyEmail }}</strong></view>
              <view><text>开户行网点</text><strong>{{ caseDraftStore.caseDetail.invoiceInfo?.bankBranch }}</strong></view>
            </view>
          </view>

          <view v-else class="invoice-form">
            <view v-for="field in invoiceFields" :key="field.key" class="form-field">
              <text>{{ field.label }}<small>*</small></text>
              <input v-model="invoiceForm[field.key]" :placeholder="field.placeholder" :disabled="!caseDraftStore.isEditable || !capabilities.flags.sensitiveMaterialUploadEnabled" />
              <small v-if="field.sensitive" class="field-hint">提交后仅显示掩码</small>
            </view>
            <text v-if="!capabilities.flags.sensitiveMaterialUploadEnabled" class="section-hint">开票及敏感资料提交暂未开放，已保存内容仍可查看。</text>
            <button class="save-invoice" :disabled="savingInvoice || !caseDraftStore.isEditable || !capabilities.flags.sensitiveMaterialUploadEnabled" @click="saveInvoiceForm">
              {{ savingInvoice ? '正在保存…' : '保存开票信息' }}
            </button>
          </view>
        </view>

        <view v-else class="files-section">
          <text class="section-title">附件资料</text>
          <text class="section-hint">按每项要求选择文件；上传失败只影响当前文件。</text>
          <FileUploadCard
            v-for="material in fileMaterials"
            :key="material.id"
            :material="material"
            :template-item="templateItem(material)"
            :files="material.id ? caseDraftStore.filesByMaterial[material.id] || [] : []"
            :busy="caseDraftStore.busyMaterialId === material.id"
            :disabled="!caseDraftStore.isEditable"
            :class="{ focused: focusedMaterialId === material.id }"
            @selected="uploadSelectedFiles"
            @error="showFileError"
            @delete="deleteFile"
          />

          <view v-for="material in urlMaterials" :key="material.id" class="url-card" :class="{ focused: focusedMaterialId === material.id }">
            <text class="url-title">{{ material.materialName }} <small v-if="material.required">必填</small></text>
            <text class="url-help">{{ templateItem(material)?.helpText || '请输入 http/https 链接' }}</text>
            <input v-if="material.id" v-model="urlValues[material.id]" placeholder="https://example.com/report" :disabled="!caseDraftStore.isEditable" />
            <button v-if="!caseDraftStore.isEditable && material.textValue" @click="copyMaterialUrl(material)">复制资料链接</button>
            <button v-if="caseDraftStore.isEditable" :disabled="caseDraftStore.busyMaterialId === material.id" @click="saveUrl(material)">
              {{ material.submitStatus === 'SUBMITTED' ? '更新链接' : '保存链接' }}
            </button>
          </view>

          <view class="security-note"><AppIcon type="locked-filled" :size="21" /><text>文件将进入私有存储，只有获得授权的客户本人和后台审核人员可以访问。</text></view>
        </view>

        <button class="continue-button" @click="nextStep">{{ activeTab === 'invoice' ? '保存并继续' : '返回资料清单' }} <AppIcon class="action-icon" type="right" :size="17" /></button>
      </template>
    </view>
  </view>
</template>

<style scoped>
.invoice-page { min-height: 100vh; background: #f4f7f7; }
.content { padding: 30rpx 32rpx calc(46rpx + env(safe-area-inset-bottom)); }
.page-heading { padding-left: 80rpx; }
.heading-eyebrow, .heading-title, .heading-copy { display: block; }
.heading-eyebrow { color: var(--color-primary-light); font-size: 18rpx; font-weight: 800; letter-spacing: 3rpx; }
.heading-title { margin-top: 6rpx; font-size: 38rpx; font-weight: 900; }
.heading-copy { margin-top: 8rpx; color: var(--color-muted); font-size: 19rpx; }
.tab-switch { display: grid; grid-template-columns: repeat(2, 1fr); margin-top: 24rpx; padding: 5rpx; border-radius: 18rpx; background: #e4eaeb; }
.tab-switch button { height: 58rpx; margin: 0; padding: 0; border-radius: 14rpx; background: transparent; color: #7a888a; font-size: 21rpx; line-height: 58rpx; }
.tab-switch button.active { background: #fff; color: var(--color-primary); font-weight: 800; box-shadow: 0 5rpx 12rpx rgba(31, 70, 70, 0.08); }
.invoice-section, .files-section { margin-top: 24rpx; }
.invoice-form { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18rpx 14rpx; }
.form-field { min-width: 0; }
.form-field > text { display: block; margin-bottom: 8rpx; color: #59696b; font-size: 18rpx; }
.form-field > text small, .url-title small { color: #b66d1b; }
.form-field input, .url-card input { display: block; box-sizing: border-box; min-width: 0; width: 100%; height: 72rpx; padding: 0 18rpx; border: 1rpx solid #d4dfe0; border-radius: 16rpx; background: #fff; font-size: 21rpx; }
.field-hint { display: block; margin: 6rpx 0 0 5rpx; color: #909b9d; font-size: 15rpx; }
.save-invoice { grid-column: 1 / -1; height: 68rpx; margin: 4rpx 0 0; border-radius: 18rpx; background: #e5f2f0; color: var(--color-primary); font-size: 22rpx; font-weight: 800; line-height: 68rpx; }
.save-invoice[disabled] { opacity: 0.65; }
.saved-invoice { padding: 24rpx; border: 1rpx solid #dce5e5; border-radius: 22rpx; background: #fff; }
.saved-title { display: flex; align-items: center; justify-content: space-between; font-weight: 800; }
.saved-title button { height: 50rpx; margin: 0; padding: 0 16rpx; border-radius: 14rpx; background: #e7f2f1; color: var(--color-primary); font-size: 18rpx; line-height: 50rpx; }
.saved-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18rpx; margin-top: 22rpx; }
.saved-grid text, .saved-grid strong { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.saved-grid text { color: #809092; font-size: 17rpx; }
.saved-grid strong { margin-top: 5rpx; font-size: 20rpx; }
.section-title, .section-hint { display: block; }
.section-title { font-size: 25rpx; font-weight: 800; }
.section-hint { margin: 5rpx 0 18rpx; color: var(--color-muted); font-size: 18rpx; }
.focused { box-shadow: 0 0 0 3rpx rgba(38, 112, 111, 0.24); }
.url-card { margin-top: 18rpx; padding: 22rpx; border: 1rpx solid #dbe4e5; border-radius: 22rpx; background: #fff; }
.url-title, .url-help { display: block; }
.url-title { font-size: 22rpx; font-weight: 800; }
.url-help { margin-top: 7rpx; color: #7d8b8d; font-size: 17rpx; }
.url-card input { margin-top: 16rpx; }
.url-card button { height: 56rpx; margin: 14rpx 0 0; padding: 0 20rpx; border-radius: 16rpx; background: #e4f2f0; color: var(--color-primary); font-size: 20rpx; line-height: 56rpx; }
.security-note { display: flex; align-items: flex-start; gap: 12rpx; margin-top: 18rpx; padding: 20rpx; border-radius: 17rpx; background: #e7f1ef; color: #597575; font-size: 18rpx; line-height: 1.55; }
.security-note :deep(.app-icon) { margin-top: 2rpx; color: var(--color-primary-light); }
.security-note text { flex: 1; min-width: 0; }
.continue-button { display: flex; align-items: center; justify-content: center; width: 100%; height: 76rpx; margin: 22rpx 0 0; border-radius: 20rpx; background: var(--color-primary); color: #fff; font-size: 24rpx; font-weight: 800; line-height: 1; box-shadow: 0 14rpx 30rpx rgba(21, 78, 78, 0.15); }
.action-icon { margin-left: 12rpx; color: var(--color-gold); }
.error-panel { background: #fff8ef; }
</style>
