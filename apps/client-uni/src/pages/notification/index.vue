<script setup lang="ts">
import { ref } from 'vue'
import { formatBeijingTime } from '../../utils/time'
import { onReachBottom, onShow } from '@dcloudio/uni-app'
import AppIcon from '../../components/AppIcon.vue'
import type { NotificationEntity } from '../../api/generated/models'
import { getCaseDetail } from '../../api/case'
import { getMyNotifications, markNotificationRead } from '../../api/notification'
import { errorMessage } from '../../api/response'
import { caseDetailRoute } from '../../constants/service'
import { useAuthStore } from '../../stores/auth'

const authStore = useAuthStore()
const notifications = ref<NotificationEntity[]>([])
const page = ref(1)
const totalPages = ref(1)
const loading = ref(false)
const loadingMore = ref(false)
const openingId = ref<number>()
const pageError = ref('')

function formatTime(value?: string) {
  return formatBeijingTime(value)
}

function typeLabel(type?: string) {
  if (type === 'CASE_STATUS_CHANGED') return '办理进度'
  if (type === 'SUPPLEMENT_REQUIRED') return '补件提醒'
  return '系统通知'
}

async function loadFirstPage() {
  if (loading.value) return
  loading.value = true
  pageError.value = ''
  try {
    await authStore.ensureSession()
    const result = await getMyNotifications(1, 12)
    notifications.value = result.items || []
    page.value = result.page || 1
    totalPages.value = result.totalPages || 1
  } catch (cause) {
    pageError.value = errorMessage(cause, '消息通知加载失败')
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (loading.value || loadingMore.value || page.value >= totalPages.value) return
  loadingMore.value = true
  try {
    const result = await getMyNotifications(page.value + 1, 12)
    notifications.value.push(...(result.items || []))
    page.value = result.page || page.value + 1
    totalPages.value = result.totalPages || totalPages.value
  } catch (cause) {
    uni.showToast({ title: errorMessage(cause, '加载更多失败'), icon: 'none' })
  } finally {
    loadingMore.value = false
  }
}

async function openNotification(item: NotificationEntity) {
  if (!item.id || openingId.value) return
  openingId.value = item.id
  try {
    if (!item.readAt) {
      await markNotificationRead(item.id)
      item.readAt = new Date().toISOString()
    }
    if (item.caseId) {
      const detail = await getCaseDetail(item.caseId)
      uni.navigateTo({ url: caseDetailRoute(item.caseId, detail.serviceType) })
    }
  } catch (cause) {
    uni.showToast({ title: errorMessage(cause, '消息打开失败'), icon: 'none', duration: 2800 })
  } finally {
    openingId.value = undefined
  }
}

onShow(() => { void loadFirstPage() })
onReachBottom(() => { void loadMore() })
</script>

<template>
  <view class="notification-page">
    <view v-if="loading && !notifications.length" class="state-panel"><text class="state-title">正在读取消息</text></view>
    <view v-else-if="pageError" class="state-panel error-panel">
      <text class="state-title">消息加载失败</text><text class="state-copy">{{ pageError }}</text>
      <button class="state-action" @click="loadFirstPage">重新加载</button>
    </view>
    <view v-else-if="!notifications.length" class="state-panel"><text class="state-title">暂无消息</text><text class="state-copy">报单状态更新和补件提醒会显示在这里。</text></view>
    <view v-else class="notification-list">
      <button v-for="item in notifications" :key="item.id" class="notification-card" :class="{ unread: !item.readAt }" @click="openNotification(item)">
        <view class="message-icon"><AppIcon type="notification-filled" :size="23" /></view>
        <view class="message-body">
          <view class="message-head"><text>{{ typeLabel(item.notificationType) }}</text><text>{{ formatTime(item.createdAt) }}</text></view>
          <text class="message-title">{{ item.title || '业务消息' }}</text>
          <text class="message-content">{{ item.content || '暂无详细内容' }}</text>
          <view v-if="item.caseId" class="message-link"><text>查看相关报单</text><AppIcon type="right" :size="15" /></view>
        </view>
        <view v-if="!item.readAt" class="unread-dot" />
      </button>
      <text class="list-footer">{{ loadingMore ? '正在加载更多…' : page >= totalPages ? '— 已显示全部消息 —' : '继续上滑加载更多' }}</text>
    </view>
  </view>
</template>

<style scoped>
.notification-page { min-height: 100vh; padding-bottom: calc(42rpx + env(safe-area-inset-bottom)); background: #f4f7f7; }
.notification-list { padding: 22rpx 32rpx; }
.notification-card { position: relative; display: flex; align-items: flex-start; width: 100%; margin: 0 0 16rpx; padding: 25rpx 24rpx; border: 1rpx solid #dce5e5; border-radius: 24rpx; background: #fff; color: var(--color-text); line-height: 1.3; text-align: left; }
.notification-card.unread { border-color: #b8d4d1; box-shadow: 0 10rpx 24rpx rgba(30, 91, 89, .06); }
.message-icon { display: flex; flex: none; align-items: center; justify-content: center; width: 54rpx; height: 54rpx; border-radius: 17rpx; background: #e5f2f1; color: #287170; }
.message-body { flex: 1; min-width: 0; margin-left: 18rpx; }
.message-head { display: flex; align-items: center; justify-content: space-between; color: #829092; font-size: 17rpx; }
.message-head text:first-child { color: #26706f; font-weight: 800; }
.message-title, .message-content { display: block; }
.message-title { margin-top: 10rpx; font-size: 23rpx; font-weight: 900; }
.message-content { margin-top: 8rpx; color: #647477; font-size: 20rpx; line-height: 1.55; }
.message-link { display: flex; align-items: center; gap: 5rpx; margin-top: 12rpx; color: var(--color-primary-light); font-size: 19rpx; font-weight: 700; }
.unread-dot { position: absolute; top: 19rpx; right: 18rpx; width: 12rpx; height: 12rpx; border-radius: 50%; background: #e15b55; }
.list-footer { display: block; padding: 10rpx 0 20rpx; color: #9ca8aa; font-size: 18rpx; text-align: center; }
.error-panel { background: #fff8ef; }
</style>
