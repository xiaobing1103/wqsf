<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppIcon from '../../components/AppIcon.vue'
import { errorMessage } from '../../api/response'
import { useAuthStore } from '../../stores/auth'
import { useCompanyStore } from '../../stores/company'

const authStore = useAuthStore()
const companyStore = useCompanyStore()
const loading = ref(false)
const pageError = ref('')
const company = computed(() => companyStore.selectedCompany)
const region = computed(() => [company.value?.province, company.value?.city].filter(Boolean).join(' · ') || '暂未填写')
const roleLabel = computed(() => ({ OWNER: '企业负责人', ADMIN: '企业管理员', VIEWER: '企业成员' }[company.value?.memberRole || ''] || '企业成员'))

async function loadPage() {
  if (loading.value) return
  loading.value = true
  pageError.value = ''
  try {
    await authStore.ensureSession()
    await companyStore.loadCompanies()
  } catch (cause) {
    pageError.value = errorMessage(cause, '企业资料加载失败')
  } finally {
    loading.value = false
  }
}

onShow(() => { void loadPage() })
</script>

<template>
  <view class="profile-page">
    <view v-if="loading && !company" class="state-panel"><text class="state-title">正在读取企业资料</text></view>
    <view v-else-if="pageError" class="state-panel error-panel">
      <text class="state-title">企业资料加载失败</text><text class="state-copy">{{ pageError }}</text>
      <button class="state-action" @click="loadPage">重新加载</button>
    </view>
    <view v-else-if="!company" class="state-panel"><text class="state-title">尚未绑定企业</text><text class="state-copy">请联系后台管理员完成企业成员绑定。</text></view>
    <template v-else>
      <view class="company-hero">
        <view class="company-mark"><AppIcon type="shop-filled" :size="27" /></view>
        <view class="company-copy"><text>{{ company.companyName }}</text><text>{{ roleLabel }}</text></view>
        <view class="status-pill">{{ company.status === 'ENABLED' ? '正常' : '停用' }}</view>
      </view>
      <view v-if="companyStore.companies.length > 1" class="company-switcher">
        <text>切换企业</text>
        <scroll-view scroll-x class="company-scroll">
          <button v-for="item in companyStore.companies" :key="item.id" :class="{ active: item.id === company.id }" @click="item.id && companyStore.selectCompany(item.id)">{{ item.companyName }}</button>
        </scroll-view>
      </view>
      <view class="info-card">
        <view><text>统一社会信用代码</text><strong>{{ company.creditCodeMask || '暂未填写' }}</strong></view>
        <view><text>法定代表人</text><strong>{{ company.legalPerson || '暂未填写' }}</strong></view>
        <view><text>联系人</text><strong>{{ company.contactName || '暂未填写' }}</strong></view>
        <view><text>联系电话</text><strong>{{ company.contactPhoneMask || '暂未填写' }}</strong></view>
        <view><text>所在地区</text><strong>{{ region }}</strong></view>
        <view><text>企业邮箱</text><strong>{{ company.companyEmail || '暂未填写' }}</strong></view>
        <view class="address-row"><text>详细地址</text><strong>{{ company.detailAddress || '暂未填写' }}</strong></view>
      </view>
      <view class="security-note"><AppIcon type="locked-filled" :size="20" /><text>为保护企业信息，信用代码与联系电话仅展示脱敏内容。资料修改请联系后台管理员。</text></view>
    </template>
  </view>
</template>

<style scoped>
.profile-page { min-height: 100vh; padding-bottom: calc(48rpx + env(safe-area-inset-bottom)); background: #f4f7f7; }
.company-hero { display: flex; align-items: center; margin: 28rpx 32rpx 0; padding: 32rpx; border-radius: 28rpx; background: linear-gradient(135deg, #164f50, #28706f); color: #fff; }
.company-mark { display: flex; flex: none; align-items: center; justify-content: center; width: 76rpx; height: 76rpx; border-radius: 22rpx; background: var(--color-gold); color: #163f40; font-size: 31rpx; font-weight: 900; }
.company-copy { flex: 1; min-width: 0; margin-left: 20rpx; }
.company-copy text { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.company-copy text:first-child { font-size: 29rpx; font-weight: 900; }
.company-copy text:last-child { margin-top: 8rpx; color: #c5dddc; font-size: 19rpx; }
.status-pill { flex: none; padding: 8rpx 14rpx; border-radius: 18rpx; background: rgba(255,255,255,.14); color: #d9eeee; font-size: 18rpx; }
.company-switcher { margin: 20rpx 32rpx 0; }
.company-switcher > text { font-size: 21rpx; font-weight: 800; }
.company-scroll { width: 100%; margin-top: 12rpx; white-space: nowrap; }
.company-scroll button { display: inline-block; height: 58rpx; margin: 0 12rpx 0 0; padding: 0 22rpx; border: 1rpx solid #d8e2e2; border-radius: 18rpx; background: #fff; color: #5d6d6f; font-size: 20rpx; line-height: 58rpx; }
.company-scroll button.active { border-color: var(--color-primary-light); background: #eaf4f3; color: var(--color-primary); font-weight: 800; }
.info-card { margin: 20rpx 32rpx 0; overflow: hidden; border: 1rpx solid #dce5e5; border-radius: 26rpx; background: #fff; }
.info-card view { display: flex; align-items: center; justify-content: space-between; min-height: 82rpx; padding: 18rpx 24rpx; border-bottom: 1rpx solid #e8eeee; }
.info-card view:last-child { border-bottom: 0; }
.info-card text { flex: none; color: #7a898b; font-size: 20rpx; }
.info-card strong { min-width: 0; margin-left: 24rpx; font-size: 21rpx; font-weight: 700; text-align: right; word-break: break-all; }
.info-card .address-row { align-items: flex-start; }
.security-note { display: flex; align-items: flex-start; gap: 14rpx; margin: 18rpx 32rpx 0; padding: 22rpx 24rpx; border-radius: 20rpx; background: #eaf3f2; color: #50706f; font-size: 19rpx; line-height: 1.6; }
.security-note :deep(.app-icon) { margin-top: 2rpx; }
.error-panel { background: #fff8ef; }
</style>
