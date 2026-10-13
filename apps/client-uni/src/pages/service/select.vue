<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import AppIcon from '../../components/AppIcon.vue'
import ServiceGlyph from '../../components/ServiceGlyph.vue'
import type { ProductView } from '../../api/generated/models'
import { errorMessage } from '../../api/response'
import { serviceIcon, serviceKind, serviceRoute } from '../../constants/service'
import { useAuthStore } from '../../stores/auth'
import { useCatalogStore } from '../../stores/catalog'
import { useCaseDraftStore } from '../../stores/caseDraft'
import { useCompanyStore } from '../../stores/company'

const authStore = useAuthStore()
const catalogStore = useCatalogStore()
const companyStore = useCompanyStore()
const caseDraftStore = useCaseDraftStore()
const selectedId = ref<number>()
const loading = ref(false)
const preparing = ref(false)
const pageError = ref('')
const selectedProduct = computed(() => catalogStore.products.find((item) => item.id === selectedId.value) || catalogStore.products[0])
const otherProducts = computed(() => catalogStore.products.filter((item) => item.id !== selectedProduct.value?.id))

function productSubtitle(product: ProductView) {
  if (serviceKind(product) === 'TRADE_INCREMENT') return product.description || '适用于已合作企业提交月度业务材料'
  return product.description || '填写法人姓名、公司名称、联系电话'
}

function selectProduct(product: ProductView) {
  selectedId.value = product.id
}

async function confirmSelection() {
  if (!selectedProduct.value) {
    uni.showToast({ title: '请先选择一项服务', icon: 'none' })
    return
  }
  if (serviceKind(selectedProduct.value) !== 'TRADE_INCREMENT') {
    uni.navigateTo({ url: serviceRoute(selectedProduct.value) })
    return
  }
  if (!selectedProduct.value.id) {
    uni.showToast({ title: '服务缺少产品编号', icon: 'none' })
    return
  }
  preparing.value = true
  try {
    await authStore.ensureSession()
    if (!companyStore.selectedCompany?.id) await companyStore.loadCompanies()
    const companyId = companyStore.selectedCompany?.id
    if (!companyId) throw new Error('当前账号尚未绑定企业')
    const detail = await caseDraftStore.prepareTradeDraft(companyId, selectedProduct.value.id)
    if (!detail.id) throw new Error('草稿缺少报单编号')
    uni.navigateTo({ url: `/pages/case/materials?caseId=${detail.id}` })
  } catch (cause) {
    uni.showToast({ title: errorMessage(cause, '创建报单草稿失败'), icon: 'none', duration: 2800 })
  } finally {
    preparing.value = false
  }
}

async function loadCatalog(preferredId?: number) {
  loading.value = true
  pageError.value = ''
  try {
    await catalogStore.loadCatalog(true)
    selectedId.value = catalogStore.products.some((item) => item.id === preferredId)
      ? preferredId
      : catalogStore.products.find((item) => serviceKind(item) === 'TRADE_INCREMENT')?.id || catalogStore.products[0]?.id
  } catch (cause) {
    pageError.value = errorMessage(cause, '服务目录加载失败')
  } finally {
    loading.value = false
  }
}

onLoad((query) => {
  const preferredId = Number(query?.productId)
  void loadCatalog(Number.isFinite(preferredId) ? preferredId : undefined)
})
</script>

<template>
  <view class="select-page">
    <view class="page-content">
      <view class="step-heading">
        <text class="step-label">STEP 1/2</text>
        <text class="page-title">选择本次服务</text>
      </view>

      <view v-if="loading && !selectedProduct" class="state-panel">
        <text class="state-title">正在读取服务目录</text>
        <text class="state-copy">只展示后台已发布的服务。</text>
      </view>
      <view v-else-if="pageError" class="state-panel error-panel">
        <text class="state-title">服务目录加载失败</text>
        <text class="state-copy">{{ pageError }}</text>
        <button class="state-action" @click="loadCatalog(selectedId)">重新加载</button>
      </view>
      <view v-else-if="!selectedProduct" class="state-panel">
        <text class="state-title">暂无可办理服务</text>
        <text class="state-copy">请联系管理员发布服务产品后再试。</text>
      </view>

      <template v-else>
        <view class="selected-card">
          <view class="selected-topline">
            <ServiceGlyph :kind="serviceIcon(selectedProduct)" tone="dark" />
            <view class="selected-copy">
              <text class="product-code">{{ selectedProduct.productCode || 'SERVICE' }}</text>
              <text class="product-name">{{ selectedProduct.productName || '企业服务' }}</text>
            </view>
            <view class="selected-check"><AppIcon type="checkmarkempty" :size="18" /></view>
          </view>
          <text class="product-description">{{ productSubtitle(selectedProduct) }}</text>
        </view>

        <view class="service-facts">
          <view><text>资料清单</text><strong>{{ selectedProduct.materialCount || 0 }} 项</strong></view>
          <view><text>办理方式</text><strong>线上提交</strong></view>
          <view><text>进度通知</text><strong>实时查看</strong></view>
        </view>

        <view v-if="otherProducts.length" class="other-section">
          <text class="other-title">其他服务</text>
          <button
            v-for="(product, index) in otherProducts"
            :key="product.id || product.productCode"
            class="other-card"
            @click="selectProduct(product)"
          >
            <ServiceGlyph :kind="serviceIcon(product)" :tone="index % 2 ? 'mint' : 'purple'" />
            <view class="other-copy">
              <text class="other-name">{{ product.productName || '企业服务' }}</text>
              <text class="other-desc">{{ productSubtitle(product) }}</text>
            </view>
            <AppIcon class="other-chevron" type="right" :size="18" />
          </button>
        </view>

        <view class="privacy-note">
          <AppIcon class="privacy-lock" type="locked-filled" :size="22" />
          <view><text class="privacy-title">隐私提示</text><text class="privacy-copy">敏感资料仅用于企业业务办理，提交前请确认授权。</text></view>
        </view>

        <button class="confirm-button" :disabled="preparing" @click="confirmSelection">
          {{ preparing ? '正在准备草稿…' : '确认并填写资料' }} <AppIcon v-if="!preparing" type="right" :size="18" />
        </button>
      </template>
    </view>
  </view>
</template>

<style scoped>
.select-page { min-height: 100vh; background: linear-gradient(180deg, #fff 0, #f4f7f7 24%); }
.page-content { padding: 34rpx 32rpx calc(48rpx + env(safe-area-inset-bottom)); }
.step-heading { margin-bottom: 34rpx; padding-left: 80rpx; }
.step-label, .page-title { display: block; }
.step-label { color: var(--color-primary-light); font-size: 19rpx; font-weight: 800; letter-spacing: 3rpx; }
.page-title { margin-top: 8rpx; font-size: 39rpx; font-weight: 900; letter-spacing: -1rpx; }
.selected-card { min-height: 274rpx; padding: 36rpx 34rpx; border: 4rpx solid #1b6868; border-radius: 28rpx; background: #fff; box-shadow: 0 12rpx 32rpx rgba(33, 83, 82, 0.05); }
.selected-topline { display: flex; align-items: center; }
.selected-topline :deep(.glyph) { width: 64rpx; height: 64rpx; }
.selected-copy { flex: 1; min-width: 0; margin-left: 22rpx; }
.product-code, .product-name, .product-description { display: block; }
.product-code { overflow: hidden; color: var(--color-primary-light); font-size: 18rpx; font-weight: 800; letter-spacing: 2rpx; text-overflow: ellipsis; white-space: nowrap; }
.product-name { margin-top: 7rpx; font-size: 34rpx; font-weight: 900; }
.selected-check { display: flex; align-items: center; justify-content: center; width: 42rpx; height: 42rpx; border-radius: 50%; background: #2c7877; }
.selected-check :deep(.app-icon) { color: #fff; }
.product-description { margin-top: 28rpx; color: #69797c; font-size: 22rpx; line-height: 1.7; }
.service-facts { display: grid; grid-template-columns: repeat(3, 1fr); margin-top: 18rpx; overflow: hidden; border: 1rpx solid #d9e2e2; border-radius: 24rpx; background: #fff; }
.service-facts view { padding: 20rpx 8rpx; text-align: center; }
.service-facts view + view { border-left: 1rpx solid #dfe6e6; }
.service-facts text, .service-facts strong { display: block; }
.service-facts text { color: #788688; font-size: 18rpx; }
.service-facts strong { margin-top: 6rpx; font-size: 23rpx; }
.other-section { margin-top: 32rpx; }
.other-title { display: block; margin-bottom: 14rpx; font-size: 22rpx; font-weight: 700; }
.other-card { display: flex; align-items: center; width: 100%; min-height: 118rpx; margin: 0 0 14rpx; padding: 20rpx 24rpx; border: 1rpx solid #dce5e5; border-radius: 24rpx; background: #fff; color: var(--color-text); text-align: left; line-height: 1.2; }
.other-copy { flex: 1; min-width: 0; margin-left: 20rpx; }
.other-name, .other-desc { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.other-name { font-size: 26rpx; font-weight: 800; }
.other-desc { margin-top: 8rpx; color: #7d8a8c; font-size: 18rpx; }
.other-chevron { margin-right: 2rpx; color: #799092; }
.privacy-note { display: flex; align-items: flex-start; margin-top: 18rpx; padding: 24rpx; border-radius: 22rpx; background: #fff2da; }
.privacy-lock { width: 34rpx; height: 34rpx; margin: 7rpx 18rpx 0 0; color: #9b6d20; }
.privacy-title, .privacy-copy { display: block; }
.privacy-title { color: #75501a; font-size: 22rpx; font-weight: 800; }
.privacy-copy { margin-top: 6rpx; color: #906b35; font-size: 19rpx; line-height: 1.55; }
.confirm-button { display: flex; align-items: center; justify-content: center; gap: 12rpx; width: 100%; height: 78rpx; margin: 22rpx 0 0; border-radius: 20rpx; background: linear-gradient(90deg, #164f50, #276f6e); color: #fff; font-size: 25rpx; font-weight: 800; line-height: 78rpx; box-shadow: 0 18rpx 32rpx rgba(27, 86, 85, 0.18); }
.confirm-button :deep(.app-icon) { color: var(--color-gold); }
.confirm-button[disabled] { opacity: 0.72; }
.error-panel { background: #fff7ec; }
</style>
