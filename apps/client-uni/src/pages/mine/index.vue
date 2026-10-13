<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppIcon from '../../components/AppIcon.vue'
import BottomNav from '../../components/BottomNav.vue'
import { getMyCasePage } from '../../api/case'
import { errorMessage } from '../../api/response'
import { useAuthStore } from '../../stores/auth'
import { useCompanyStore } from '../../stores/company'

const authStore = useAuthStore()
const companyStore = useCompanyStore()
const total = ref(0)
const loading = ref(false)
const pageError = ref('')
const logoutVisible = ref(false)
const logoutBusy = ref(false)
const stats = reactive({ processing: 0, supplement: 0, completed: 0 })

const companyName = computed(() => companyStore.selectedCompany?.companyName || '尚未绑定企业')
const companyInitial = computed(() => companyName.value.slice(0, 1))
const companyMeta = computed(() => {
  const company = companyStore.selectedCompany
  if (!company) return '点击查看并完善企业资料'
  const role = company.memberRole === 'OWNER' ? '企业负责人' : company.memberRole === 'STAFF' ? '企业成员' : '已绑定企业'
  return `${role} · 点击查看企业资料`
})

async function loadStats() {
  const [all, processing, supplement, completed] = await Promise.all([
    getMyCasePage(1, 1),
    getMyCasePage(1, 1, 'PENDING_REVIEW,PROCESSING'),
    getMyCasePage(1, 1, 'NEED_SUPPLEMENT'),
    getMyCasePage(1, 1, 'COMPLETED'),
  ])
  total.value = all.total || 0
  stats.processing = processing.total || 0
  stats.supplement = supplement.total || 0
  stats.completed = completed.total || 0
}

async function loadPage() {
  if (loading.value) return
  loading.value = true
  pageError.value = ''
  try {
    await authStore.ensureSession()
    await Promise.all([
      companyStore.companies.length ? Promise.resolve() : companyStore.loadCompanies(),
      loadStats(),
    ])
  } catch (cause) {
    pageError.value = errorMessage(cause, '企业资料加载失败')
  } finally {
    loading.value = false
  }
}

function openCaseList(filter: 'all' | 'processing' | 'supplement' | 'completed') {
  uni.navigateTo({ url: `/pages/case/list?filter=${filter}` })
}

function openCompanyProfile() {
  uni.navigateTo({ url: '/pages/company/profile' })
}

function openNotifications() {
  uni.navigateTo({ url: '/pages/notification/index' })
}

function showContactNotice() {
  uni.showToast({ title: '请在微信小程序中打开客服会话', icon: 'none', duration: 2600 })
}

async function confirmLogout() {
  if (logoutBusy.value) return
  logoutBusy.value = true
  try {
    await authStore.logout()
  } finally {
    logoutBusy.value = false
    logoutVisible.value = false
  }
}

onShow(() => { void loadPage() })
</script>

<template>
  <view class="safe-page mine-page">
    <view class="profile-card" @click="openCompanyProfile">
      <view class="avatar">{{ companyInitial }}</view>
      <view class="profile-copy">
        <text class="profile-name">{{ companyName }}</text>
        <text class="profile-company">{{ companyMeta }}</text>
      </view>
      <AppIcon class="profile-chevron" type="right" :size="19" />
    </view>

    <view class="stats-card">
      <button @click="openCaseList('all')"><strong>{{ total }}</strong><text>全部报单</text></button>
      <button @click="openCaseList('processing')"><strong>{{ stats.processing }}</strong><text>处理中</text></button>
      <button @click="openCaseList('supplement')"><strong>{{ stats.supplement }}</strong><text>待补资料</text></button>
      <button @click="openCaseList('completed')"><strong>{{ stats.completed }}</strong><text>已完成</text></button>
    </view>

    <view v-if="pageError" class="state-panel error-panel">
      <text class="state-title">企业资料加载失败</text><text class="state-copy">{{ pageError }}</text>
      <button class="state-action" @click="loadPage">重新加载</button>
    </view>

    <view class="mine-links">
      <button @click="openCompanyProfile"><view class="link-label"><AppIcon type="shop" :size="21" /><text>企业资料</text></view><AppIcon class="link-arrow" type="right" :size="17" /></button>
      <button @click="openNotifications"><view class="link-label"><AppIcon type="notification" :size="21" /><text>消息通知</text></view><AppIcon class="link-arrow" type="right" :size="17" /></button>
      <!-- #ifdef MP-WEIXIN -->
      <button open-type="contact"><view class="link-label"><AppIcon type="headphones" :size="21" /><text>联系客服</text></view><AppIcon class="link-arrow" type="right" :size="17" /></button>
      <!-- #endif -->
      <!-- #ifndef MP-WEIXIN -->
      <button @click="showContactNotice"><view class="link-label"><AppIcon type="headphones" :size="21" /><text>联系客服</text></view><AppIcon class="link-arrow" type="right" :size="17" /></button>
      <!-- #endif -->
      <button class="logout-link" @click="logoutVisible = true"><view class="link-label"><AppIcon type="person" :size="21" /><text>退出登录 / 登录其他账号</text></view><AppIcon class="link-arrow" type="right" :size="17" /></button>
    </view>

    <view v-if="logoutVisible" class="logout-backdrop" @click.self="logoutVisible = false">
      <view class="logout-dialog">
        <view class="logout-icon"><AppIcon type="info-filled" :size="28" /></view>
        <text class="logout-title">退出当前账号？</text>
        <text class="logout-copy">退出后会清除本机保存的企业选择和报单草稿缓存，你可以重新使用微信或其他客户账号登录。</text>
        <view class="logout-actions">
          <button @click="logoutVisible = false">取消</button>
          <button class="confirm" :disabled="logoutBusy" @click="confirmLogout">{{ logoutBusy ? '正在退出…' : '退出并切换账号' }}</button>
        </view>
      </view>
    </view>

    <BottomNav active="mine" />
  </view>
</template>

<style scoped>
.mine-page { padding-right: 32rpx; padding-left: 32rpx; background: #f4f7f7; }
.profile-card { display: flex; align-items: center; margin-top: 28rpx; padding: 32rpx; border-radius: 30rpx; background: linear-gradient(130deg, #164f50, #246967); color: #fff; }
.avatar { display: flex; flex: none; align-items: center; justify-content: center; width: 88rpx; height: 88rpx; border: 5rpx solid rgba(255,255,255,.42); border-radius: 50%; background: var(--color-gold); color: #123f41; font-size: 34rpx; font-weight: 900; }
.profile-copy { flex: 1; min-width: 0; margin-left: 22rpx; }
.profile-name, .profile-company { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.profile-name { font-size: 33rpx; font-weight: 900; }
.profile-company { margin-top: 8rpx; color: #c4d9d8; font-size: 18rpx; }
.profile-chevron { color: #fff; }
.stats-card { display: grid; grid-template-columns: repeat(4, 1fr); margin-top: 18rpx; padding: 12rpx 8rpx; border: 1rpx solid #dce5e5; border-radius: 24rpx; background: #fff; }
.stats-card button { position: relative; width: 100%; min-width: 0; margin: 0; padding: 12rpx 2rpx; border-radius: 16rpx; background: transparent; color: var(--color-text); text-align: center; line-height: 1.2; }
.stats-card button:active { background: #eaf4f3; }
.stats-card button:not(:last-child)::before { position: absolute; top: 16rpx; right: 0; bottom: 16rpx; width: 1rpx; background: #e4eaea; content: ''; }
.stats-card strong, .stats-card text { display: block; }
.stats-card strong { font-size: 30rpx; }
.stats-card text { margin-top: 6rpx; color: #7d8c8e; font-size: 16rpx; }
.mine-links { margin-top: 24rpx; overflow: hidden; border: 1rpx solid #dbe4e5; border-radius: 22rpx; background: #fff; }
.mine-links button { display: flex; align-items: center; justify-content: space-between; width: 100%; height: 76rpx; margin: 0; padding: 0 22rpx; border-bottom: 1rpx solid #e6ecec; background: transparent; color: var(--color-text); font-size: 21rpx; text-align: left; line-height: 76rpx; }
.mine-links button:last-child { border-bottom: 0; }
.mine-links .logout-link { color: #a14640; }
.mine-links .logout-link :deep(.app-icon) { color: #a14640; }
.link-label { display: flex; align-items: center; gap: 16rpx; }
.link-label :deep(.app-icon) { width: 38rpx; color: var(--color-primary-light); }
.link-arrow { color: #869496; }
.error-panel { margin-right: 0; margin-left: 0; background: #fff8ef; }
.logout-backdrop { position: fixed; inset: 0; z-index: 80; display: flex; align-items: flex-end; justify-content: center; padding: 32rpx 32rpx calc(32rpx + env(safe-area-inset-bottom)); background: rgba(10,31,32,.45); }
.logout-dialog { width: 100%; padding: 34rpx 28rpx 28rpx; border-radius: 30rpx; background: #fff; text-align: center; box-shadow: 0 30rpx 80rpx rgba(8,35,36,.22); }
.logout-icon { display: flex; align-items: center; justify-content: center; width: 70rpx; height: 70rpx; margin: 0 auto; border-radius: 50%; background: #fff0ee; color: #a14640; }
.logout-title, .logout-copy { display: block; }
.logout-title { margin-top: 18rpx; font-size: 30rpx; font-weight: 900; }
.logout-copy { margin-top: 12rpx; color: #718083; font-size: 21rpx; line-height: 1.65; }
.logout-actions { display: grid; grid-template-columns: .8fr 1.2fr; gap: 14rpx; margin-top: 28rpx; }
.logout-actions button { height: 74rpx; margin: 0; border-radius: 20rpx; background: #edf2f1; color: #445d5e; font-size: 22rpx; font-weight: 700; line-height: 74rpx; }
.logout-actions .confirm { background: #a14640; color: #fff; }
</style>
