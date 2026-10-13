<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onReachBottom } from '@dcloudio/uni-app'
import AppIcon from '../../components/AppIcon.vue'
import ServiceGlyph from '../../components/ServiceGlyph.vue'
import StatusBadge from '../../components/StatusBadge.vue'
import { getMyCasePage } from '../../api/case'
import type { CaseSummary } from '../../api/generated/models'
import { errorMessage } from '../../api/response'
import { caseStatusLabel, caseStatusTone } from '../../constants/status'
import { caseDetailRoute, serviceIcon, serviceKind } from '../../constants/service'
import { useAuthStore } from '../../stores/auth'
import { useCatalogStore } from '../../stores/catalog'

type CaseFilter = 'all' | 'processing' | 'supplement' | 'completed'

const FILTERS: Array<{ key: CaseFilter; label: string; status?: string; empty: string }> = [
  { key: 'all', label: '全部报单', empty: '还没有报单' },
  { key: 'processing', label: '处理中', status: 'PENDING_REVIEW,PROCESSING', empty: '暂无处理中的报单' },
  { key: 'supplement', label: '待补资料', status: 'NEED_SUPPLEMENT', empty: '暂无待补资料的报单' },
  { key: 'completed', label: '已完成', status: 'COMPLETED', empty: '暂无已完成的报单' },
]

const authStore = useAuthStore()
const catalogStore = useCatalogStore()
const activeFilter = ref<CaseFilter>('all')
const cases = ref<CaseSummary[]>([])
const page = ref(1)
const totalPages = ref(1)
const total = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const pageError = ref('')

const filterConfig = computed(() => FILTERS.find((item) => item.key === activeFilter.value) || FILTERS[0])

function productFor(caseItem: CaseSummary) {
  const serviceType = caseItem.serviceType || ''
  return catalogStore.products.find((product) => {
    const code = product.productCode || ''
    return code === serviceType || code.includes(serviceType) || serviceType.includes(code)
  })
}

function serviceName(caseItem: CaseSummary) {
  return productFor(caseItem)?.productName || caseItem.serviceType || '企业服务'
}

function formatCaseNo(caseItem: CaseSummary) {
  const month = caseItem.caseMonth ? ` · ${caseItem.caseMonth}` : ''
  return `${caseItem.caseNo || '编号待生成'}${month}`
}

function openCase(caseItem: CaseSummary) {
  if (!caseItem.id) return
  uni.navigateTo({ url: caseDetailRoute(caseItem.id, caseItem.serviceType) })
}

async function loadFirstPage() {
  if (loading.value) return
  loading.value = true
  pageError.value = ''
  try {
    await authStore.ensureSession()
    await catalogStore.loadCatalog()
    const result = await getMyCasePage(1, 10, filterConfig.value.status)
    cases.value = result.items || []
    page.value = result.page || 1
    totalPages.value = result.totalPages || 1
    total.value = result.total || 0
  } catch (cause) {
    pageError.value = errorMessage(cause, '报单列表加载失败')
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (loading.value || loadingMore.value || page.value >= totalPages.value) return
  loadingMore.value = true
  try {
    const result = await getMyCasePage(page.value + 1, 10, filterConfig.value.status)
    cases.value.push(...(result.items || []))
    page.value = result.page || page.value + 1
    totalPages.value = result.totalPages || totalPages.value
  } catch (cause) {
    uni.showToast({ title: errorMessage(cause, '加载更多失败'), icon: 'none' })
  } finally {
    loadingMore.value = false
  }
}

function selectFilter(filter: CaseFilter) {
  if (activeFilter.value === filter) return
  activeFilter.value = filter
  void loadFirstPage()
}

function createCase() {
  uni.navigateTo({ url: '/pages/service/select' })
}

onLoad((query) => {
  const requested = query?.filter as CaseFilter | undefined
  if (FILTERS.some((item) => item.key === requested)) activeFilter.value = requested || 'all'
  void loadFirstPage()
})
onReachBottom(() => { void loadMore() })
</script>

<template>
  <view class="case-list-page">
    <scroll-view class="filter-scroll" scroll-x :show-scrollbar="false">
      <view class="filter-tabs">
        <button v-for="filter in FILTERS" :key="filter.key" :class="{ active: activeFilter === filter.key }" @click="selectFilter(filter.key)">
          {{ filter.label }}
        </button>
      </view>
    </scroll-view>

    <view class="list-summary">
      <view><text>{{ filterConfig.label }}</text><text>点击报单可查看资料状态和办理进度</text></view>
      <button :disabled="loading" @click="loadFirstPage"><AppIcon type="refresh" :size="16" />刷新</button>
    </view>

    <view v-if="loading && !cases.length" class="state-panel">
      <text class="state-title">正在读取报单</text>
    </view>
    <view v-else-if="pageError" class="state-panel error-panel">
      <text class="state-title">报单加载失败</text><text class="state-copy">{{ pageError }}</text>
      <button class="state-action" @click="loadFirstPage">重新加载</button>
    </view>
    <view v-else-if="!cases.length" class="state-panel empty-panel">
      <text class="state-title">{{ filterConfig.empty }}</text>
      <text class="state-copy">你可以切换上方分类，或创建新的企业服务报单。</text>
      <button class="state-action" @click="createCase">选择服务</button>
    </view>

    <view v-else class="case-list">
      <button v-for="caseItem in cases" :key="caseItem.id" class="case-card" @click="openCase(caseItem)">
        <view class="case-top">
          <ServiceGlyph :kind="serviceIcon(productFor(caseItem))" :tone="serviceKind(productFor(caseItem)) === 'TRADE_INCREMENT' ? 'mint' : 'purple'" />
          <view class="case-copy">
            <text class="case-name">{{ serviceName(caseItem) }}</text>
            <text class="case-no">{{ formatCaseNo(caseItem) }}</text>
          </view>
          <StatusBadge :label="caseStatusLabel(caseItem.status)" :tone="caseStatusTone(caseItem.status)" />
        </view>
        <view class="progress-track"><view :style="{ width: `${caseItem.materials?.total ? Math.round((caseItem.materials.completed || 0) / caseItem.materials.total * 100) : 0}%` }" /></view>
        <view class="progress-copy"><text>资料完成度</text><strong>{{ caseItem.materials?.completed || 0 }} / {{ caseItem.materials?.total || 0 }}</strong></view>
        <view v-if="caseItem.status === 'NEED_SUPPLEMENT'" class="supplement-note">后台已发起补件，请打开报单查看需要补充的资料。</view>
        <view class="case-footer"><text>查看资料状态</text><AppIcon type="right" :size="15" /></view>
      </button>
      <text class="list-footer">{{ loadingMore ? '正在加载更多…' : page >= totalPages ? `— 共 ${total} 笔报单 —` : '继续上滑加载更多' }}</text>
    </view>
  </view>
</template>

<style scoped>
.case-list-page { min-height: 100vh; padding-bottom: calc(42rpx + env(safe-area-inset-bottom)); background: #f4f7f7; }
.filter-scroll { width: 100%; background: #fff; white-space: nowrap; }
.filter-tabs { display: inline-flex; min-width: 100%; padding: 18rpx 24rpx; gap: 12rpx; }
.filter-tabs button { flex: 1 0 auto; height: 62rpx; margin: 0; padding: 0 24rpx; border: 1rpx solid #d9e3e3; border-radius: 18rpx; background: #fff; color: #667779; font-size: 21rpx; line-height: 62rpx; }
.filter-tabs button.active { border-color: var(--color-primary); background: var(--color-primary); color: #fff; font-weight: 800; box-shadow: 0 8rpx 20rpx rgba(22,79,80,.14); }
.list-summary { display: flex; align-items: flex-end; justify-content: space-between; padding: 26rpx 32rpx 12rpx; }
.list-summary text { display: block; }
.list-summary text:first-child { font-size: 29rpx; font-weight: 900; }
.list-summary text:last-child { margin-top: 5rpx; color: var(--color-muted); font-size: 18rpx; }
.list-summary button { display: flex; align-items: center; gap: 6rpx; height: 50rpx; margin: 0; padding: 0 14rpx; background: transparent; color: var(--color-primary); font-size: 19rpx; line-height: 50rpx; }
.case-list { padding: 0 32rpx; }
.case-card { width: 100%; margin: 0 0 16rpx; padding: 22rpx; border: 1rpx solid #dbe4e5; border-radius: 24rpx; background: #fff; color: var(--color-text); text-align: left; line-height: 1.2; box-shadow: 0 8rpx 24rpx rgba(22,79,80,.035); }
.case-card:active { background: #f8fbfb; }
.case-top { display: flex; align-items: center; }
.case-copy { flex: 1; min-width: 0; margin: 0 16rpx; }
.case-name, .case-no { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.case-name { font-size: 23rpx; font-weight: 800; }
.case-no { margin-top: 7rpx; color: #829092; font-size: 17rpx; }
.progress-track { height: 7rpx; margin-top: 20rpx; overflow: hidden; border-radius: 5rpx; background: #e4eaea; }
.progress-track view { height: 100%; border-radius: 5rpx; background: var(--color-primary-light); }
.progress-copy { display: flex; justify-content: space-between; margin-top: 8rpx; color: #7d8b8d; font-size: 17rpx; }
.progress-copy strong { color: var(--color-primary); }
.supplement-note { margin-top: 14rpx; padding: 13rpx 15rpx; border-radius: 13rpx; background: #fff2da; color: #8a5c17; font-size: 17rpx; }
.case-footer { display: flex; align-items: center; justify-content: flex-end; gap: 3rpx; margin-top: 14rpx; color: var(--color-primary-light); font-size: 18rpx; font-weight: 700; }
.list-footer { display: block; padding: 8rpx 0 20rpx; color: #9ca8aa; font-size: 18rpx; text-align: center; }
.error-panel { background: #fff8ef; }
.empty-panel { margin-top: 12rpx; }
</style>
