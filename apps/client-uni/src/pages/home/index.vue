<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppIcon from '../../components/AppIcon.vue'
import BottomNav from '../../components/BottomNav.vue'
import ServiceGlyph from '../../components/ServiceGlyph.vue'
import StatusBadge from '../../components/StatusBadge.vue'
import { getMyCases } from '../../api/case'
import { errorMessage } from '../../api/response'
import type { CaseSummary, ProductView } from '../../api/generated/models'
import { useAuthStore } from '../../stores/auth'
import { useCatalogStore } from '../../stores/catalog'
import { useCompanyStore } from '../../stores/company'
import { caseStatusLabel, caseStatusTone } from '../../constants/status'
import { serviceIcon, serviceKind } from '../../constants/service'

const authStore = useAuthStore()
const companyStore = useCompanyStore()
const catalogStore = useCatalogStore()
const cases = ref<CaseSummary[]>([])
const loading = ref(false)
const pageError = ref('')

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const displayName = computed(() => authStore.user?.displayName || companyStore.selectedCompany?.contactName || '企业用户')
const companyName = computed(() => companyStore.selectedCompany?.companyName || '企业资料协作')
const featuredServices = computed(() => {
  const products = [...catalogStore.products]
  const tradeIndex = products.findIndex((item) => serviceKind(item) === 'TRADE_INCREMENT')
  if (tradeIndex > 0) products.unshift(...products.splice(tradeIndex, 1))
  return products.slice(0, 3)
})
const currentCase = computed(() => cases.value[0])

function formatDate(value?: string) {
  if (!value) return '提交时间待更新'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value.slice(0, 10)
  return `${String(date.getMonth() + 1).padStart(2, '0')}月${String(date.getDate()).padStart(2, '0')}日提交`
}

function serviceName(caseItem: CaseSummary) {
  const serviceType = caseItem.serviceType || ''
  if (!serviceType) return '企业服务'
  return catalogStore.products.find((item) => {
    const productCode = item.productCode || ''
    return productCode === serviceType || productCode.includes(serviceType) || serviceType.includes(productCode)
  })?.productName
    || serviceType
}

function selectService(product?: ProductView) {
  const query = product?.id ? `?productId=${product.id}` : ''
  uni.navigateTo({ url: `/pages/service/select${query}` })
}

function openMyCases() {
  uni.navigateTo({ url: '/pages/case/list?filter=all' })
}

function showNotifications() {
  uni.navigateTo({ url: '/pages/notification/index' })
}

async function loadPage() {
  if (loading.value) return
  loading.value = true
  pageError.value = ''
  try {
    await authStore.ensureSession()
    const results = await Promise.allSettled([
      companyStore.loadCompanies(),
      catalogStore.loadCatalog(true),
      getMyCases(1, 3),
    ])
    const caseResult = results[2]
    if (caseResult.status === 'fulfilled') cases.value = caseResult.value
    const rejected = results.find((item): item is PromiseRejectedResult => item.status === 'rejected')
    if (rejected) pageError.value = errorMessage(rejected.reason, '部分首页数据加载失败')
  } catch (cause) {
    pageError.value = errorMessage(cause, '首页加载失败')
  } finally {
    loading.value = false
  }
}

onShow(() => { void loadPage() })
</script>

<template>
  <view class="safe-page home-page">
    <view class="welcome-row">
      <view>
        <text class="eyebrow">企业服务 · 资料协作</text>
        <text class="welcome-title">{{ greeting }}，{{ displayName }}</text>
        <text class="company-name">{{ companyName }}</text>
      </view>
      <button class="notice-button" aria-label="通知" @click="showNotifications">
        <AppIcon type="notification-filled" :size="25" />
      </button>
    </view>

    <view class="hero-card">
      <view class="hero-orbit orbit-one" />
      <view class="hero-orbit orbit-two" />
      <view class="hero-copy">
        <text class="hero-kicker">一站式企业服务协作</text>
        <text class="hero-title">业务办理</text>
        <text class="hero-title hero-gold">清晰可追踪</text>
        <text class="hero-desc">提交资料、补充材料、查看进度，一站完成。</text>
        <button class="hero-action" @click="selectService()">开始报单 <AppIcon type="right" :size="16" /></button>
      </view>
      <view class="hero-file">
        <AppIcon type="locked-filled" :size="28" />
        <text class="hero-file-title">资料</text>
        <text class="hero-file-copy">安全传输</text>
      </view>
    </view>

    <view v-if="pageError" class="inline-error">
      <view><text class="inline-error-title">部分数据未能更新</text><text>{{ pageError }}</text></view>
      <button @click="loadPage">重试</button>
    </view>

    <view class="section-heading">
      <view><text class="section-title">选择服务</text><text class="section-subtitle">按业务类型提交资料</text></view>
      <button class="text-action" @click="selectService()">全部服务 <AppIcon type="right" :size="15" /></button>
    </view>

    <view v-if="loading && !featuredServices.length" class="service-loading">正在读取已发布服务…</view>
    <view v-else-if="featuredServices.length" class="service-grid">
      <button
        v-for="(product, index) in featuredServices"
        :key="product.id || product.productCode"
        class="service-card"
        :class="{ featured: index === 0 }"
        @click="selectService(product)"
      >
        <ServiceGlyph
          :kind="serviceIcon(product)"
          :tone="index === 0 ? 'dark' : index === 1 ? 'purple' : 'mint'"
        />
        <view class="service-copy">
          <text class="service-name">{{ product.productName || '企业服务' }}</text>
          <text class="service-desc">{{ product.materialCount || 0 }} 项资料清单</text>
        </view>
        <AppIcon class="service-arrow" type="right" :size="18" />
      </button>
    </view>
    <view v-else class="state-panel compact-state">
      <text class="state-title">暂无已发布服务</text>
      <text class="state-copy">后台发布产品后会自动显示在这里。</text>
    </view>

    <view class="section-heading case-heading">
      <view><text class="section-title">当前报单</text><text class="section-subtitle">你的业务进度</text></view>
      <button class="text-action" @click="openMyCases">查看全部 <AppIcon type="right" :size="15" /></button>
    </view>

    <button v-if="currentCase" class="case-card" @click="openMyCases">
      <ServiceGlyph kind="arrow" tone="mint" />
      <view class="case-copy">
        <text class="case-name">{{ serviceName(currentCase) }}</text>
        <text class="case-no">{{ currentCase.caseNo || '编号待生成' }} · {{ formatDate(currentCase.createdAt) }}</text>
      </view>
      <view class="case-side">
        <StatusBadge :label="caseStatusLabel(currentCase.status)" :tone="caseStatusTone(currentCase.status)" />
        <text class="case-progress">{{ currentCase.materials?.completed || 0 }} / {{ currentCase.materials?.total || 0 }}</text>
      </view>
    </button>
    <view v-else-if="!loading" class="empty-case">
      <view><text class="empty-title">还没有报单</text><text class="empty-copy">选择服务后开始准备业务资料。</text></view>
      <button @click="selectService()">去选择</button>
    </view>

    <BottomNav active="home" />
  </view>
</template>

<style scoped>
.home-page { background: linear-gradient(180deg, #fff 0, #f4f7f7 28%); }
.welcome-row { display: flex; align-items: center; justify-content: space-between; padding: 28rpx 32rpx 20rpx; }
.eyebrow, .welcome-title, .company-name { display: block; }
.eyebrow { color: var(--color-primary-light); font-size: 20rpx; font-weight: 700; letter-spacing: 2rpx; }
.welcome-title { margin-top: 8rpx; font-size: 40rpx; font-weight: 800; letter-spacing: -1rpx; }
.company-name { max-width: 510rpx; margin-top: 6rpx; overflow: hidden; color: var(--color-muted); font-size: 22rpx; text-overflow: ellipsis; white-space: nowrap; }
.notice-button { display: flex; align-items: center; justify-content: center; width: 68rpx; height: 68rpx; margin: 0; padding: 0; border-radius: 50%; background: #e8f3f1; }
.notice-button :deep(.app-icon) { color: var(--color-primary); }
.hero-card { position: relative; height: 360rpx; margin: 0 32rpx; overflow: hidden; border-radius: 32rpx; background: linear-gradient(125deg, #104548, #276866); color: #fff; box-shadow: 0 22rpx 45rpx rgba(26, 78, 78, 0.15); }
.hero-copy { position: relative; z-index: 3; width: 70%; padding: 38rpx 0 0 44rpx; }
.hero-kicker, .hero-title, .hero-desc { display: block; }
.hero-kicker { font-size: 22rpx; font-weight: 700; }
.hero-title { margin-top: 22rpx; font-family: "Songti SC", serif; font-size: 50rpx; font-weight: 900; line-height: 0.75; letter-spacing: -2rpx; }
.hero-gold { margin-top: 16rpx; color: var(--color-gold); }
.hero-desc { margin-top: 24rpx; font-size: 21rpx; opacity: 0.88; }
.hero-action { display: flex; align-items: center; justify-content: center; gap: 8rpx; width: 160rpx; height: 58rpx; margin: 24rpx 0 0; padding: 0; border-radius: 18rpx; background: var(--color-gold); color: #17383a; font-size: 22rpx; font-weight: 700; line-height: 1; }
.hero-orbit { position: absolute; border: 1rpx solid rgba(255, 255, 255, 0.14); border-radius: 50%; }
.orbit-one { right: -120rpx; bottom: -200rpx; width: 440rpx; height: 440rpx; }
.orbit-two { right: -20rpx; bottom: -125rpx; width: 310rpx; height: 310rpx; }
.hero-file { position: absolute; right: 44rpx; bottom: 48rpx; z-index: 2; display: flex; flex-direction: column; align-items: center; justify-content: center; width: 124rpx; height: 150rpx; border: 1rpx solid rgba(255, 255, 255, 0.3); border-radius: 28rpx; background: rgba(255, 255, 255, 0.09); backdrop-filter: blur(8px); transform: rotate(5deg); }
.hero-file :deep(.app-icon) { color: var(--color-gold); }
.hero-file-title { margin-top: 8rpx; font-size: 22rpx; font-weight: 700; }
.hero-file-copy { margin-top: 2rpx; font-size: 17rpx; opacity: 0.65; }
.inline-error { display: flex; align-items: center; justify-content: space-between; margin: 22rpx 32rpx 0; padding: 20rpx 22rpx; border-radius: 18rpx; background: #fff1df; color: #855710; font-size: 21rpx; }
.inline-error text { display: block; }
.inline-error-title { margin-bottom: 4rpx; font-weight: 700; }
.inline-error button { height: 54rpx; margin: 0 0 0 16rpx; padding: 0 22rpx; border-radius: 16rpx; background: #fff; color: #855710; font-size: 22rpx; line-height: 54rpx; }
.section-heading { display: flex; align-items: flex-end; justify-content: space-between; padding: 34rpx 32rpx 14rpx; }
.section-title, .section-subtitle { display: block; }
.section-title { font-size: 30rpx; font-weight: 800; }
.section-subtitle { margin-top: 3rpx; color: var(--color-muted); font-size: 20rpx; }
.text-action { display: flex; align-items: center; justify-content: center; gap: 3rpx; height: 54rpx; margin: 0; padding: 0 14rpx; border: 1rpx solid #c8d8d8; border-radius: 16rpx; background: #fff; color: var(--color-primary); font-size: 20rpx; font-weight: 700; line-height: 1; box-shadow: 0 4rpx 12rpx rgba(24, 71, 71, 0.05); }
.text-action:active { background: #eef7f5; }
.service-loading { margin: 0 32rpx; padding: 34rpx; border-radius: 24rpx; background: #fff; color: var(--color-muted); text-align: center; }
.service-grid { display: grid; grid-template: repeat(2, 102rpx) / 1.1fr 0.9fr; gap: 14rpx; padding: 0 32rpx; }
.service-card { position: relative; display: flex; align-items: center; width: 100%; height: 102rpx; margin: 0; padding: 0 22rpx; border: 1rpx solid var(--color-border); border-radius: 24rpx; background: #fff; color: var(--color-text); text-align: left; line-height: 1.2; }
.service-card.featured { grid-row: 1 / span 2; display: block; height: 218rpx; padding: 26rpx 24rpx; border-color: #1c5d5d; background: linear-gradient(145deg, #164f50, #123f42); color: #fff; }
.service-copy { min-width: 0; margin-left: 18rpx; }
.featured .service-copy { margin: 24rpx 0 0; }
.service-name, .service-desc { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.service-name { font-size: 25rpx; font-weight: 800; }
.featured .service-name { font-size: 28rpx; }
.service-desc { margin-top: 8rpx; color: var(--color-muted); font-size: 18rpx; }
.featured .service-desc { color: #b8cece; }
.service-arrow { position: absolute; right: 20rpx; color: #9baaac; }
.featured .service-arrow { right: 24rpx; bottom: 26rpx; color: var(--color-gold); }
.compact-state { margin-top: 0; }
.case-heading { padding-top: 30rpx; }
.case-card { display: flex; align-items: center; width: calc(100% - 64rpx); min-height: 110rpx; margin: 0 32rpx; padding: 18rpx 20rpx; border: 1rpx solid var(--color-border); border-radius: 24rpx; background: #fff; color: var(--color-text); text-align: left; line-height: 1.2; }
.case-copy { flex: 1; min-width: 0; margin: 0 16rpx; }
.case-name, .case-no { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.case-name { font-size: 24rpx; font-weight: 800; }
.case-no { margin-top: 8rpx; color: var(--color-muted); font-size: 18rpx; }
.case-side { display: flex; flex-direction: column; align-items: flex-end; }
.case-progress { margin-top: 8rpx; color: var(--color-muted); font-size: 18rpx; }
.empty-case { display: flex; align-items: center; justify-content: space-between; margin: 0 32rpx; padding: 24rpx; border: 1rpx dashed #cbd8d8; border-radius: 24rpx; background: rgba(255, 255, 255, 0.6); }
.empty-title, .empty-copy { display: block; }
.empty-title { font-weight: 700; }
.empty-copy { margin-top: 6rpx; color: var(--color-muted); font-size: 21rpx; }
.empty-case button { height: 58rpx; margin: 0; padding: 0 24rpx; border-radius: 18rpx; background: var(--color-primary); color: #fff; font-size: 22rpx; line-height: 58rpx; }
</style>
