<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppIcon from '../../components/AppIcon.vue'
import { useAuthStore } from '../../stores/auth'

const authStore = useAuthStore()
const mode = ref<'quick' | 'account'>('quick')
const username = ref('')
const password = ref('')
const bindWechat = ref(true)
const showPassword = ref(false)
const submitting = ref(false)
const formError = ref('')
const canSubmit = computed(() => username.value.trim().length > 0 && password.value.length > 0 && !submitting.value)

async function finishLogin(action: () => Promise<void>) {
  if (submitting.value) return
  submitting.value = true
  formError.value = ''
  try {
    await action()
    uni.reLaunch({ url: '/pages/home/index' })
  } catch {
    formError.value = authStore.error || '登录失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}

function submitWechat() {
  void finishLogin(() => authStore.loginByWechat())
}

function submitAccount() {
  if (!username.value.trim() || !password.value) {
    formError.value = '请输入用户名和密码'
    return
  }
  void finishLogin(() => authStore.loginByPassword(username.value.trim(), password.value, bindWechat.value))
}

onShow(() => {
  void authStore.restoreSession().then((ready) => {
    if (ready) uni.reLaunch({ url: '/pages/home/index' })
  })
})
</script>

<template>
  <view class="login-page">
    <view class="brand-block">
      <view class="brand-mark"><AppIcon type="shop-filled" :size="34" /></view>
      <text class="brand-kicker">WANQI BUSINESS SERVICE</text>
      <text class="brand-title">万企商服通</text>
      <text class="brand-copy">一个账号管理企业资料、业务报单和办理进度</text>
    </view>

    <view class="login-card">
      <view class="login-tabs">
        <button :class="{ active: mode === 'quick' }" @click="mode = 'quick'; formError = ''">微信登录</button>
        <button :class="{ active: mode === 'account' }" @click="mode = 'account'; formError = ''">账号登录</button>
      </view>

      <view v-if="mode === 'quick'" class="quick-panel">
        <view class="quick-icon"><AppIcon type="chat-filled" :size="38" /></view>
        <text class="panel-title">微信一键登录</text>
        <text class="panel-copy">已绑定的客户账号可以直接登录；首次使用请先通过账号密码完成绑定。</text>
        <button class="wechat-button" :disabled="submitting" @click="submitWechat">
          {{ submitting ? '正在登录…' : authStore.isDevelopmentLogin ? '本地模拟微信登录' : '微信授权登录' }}
        </button>
        <button class="switch-button" @click="mode = 'account'; formError = ''">首次使用或登录其他账号</button>
      </view>

      <form v-else class="account-panel" @submit="submitAccount">
        <text class="panel-title">客户账号登录</text>
        <text class="panel-copy">请输入管理员为你发放的客户账号和密码。</text>
        <label class="field">
          <text>用户名</text>
          <view class="input-wrap"><AppIcon type="person" :size="20" /><input v-model="username" maxlength="64" autocomplete="username" placeholder="请输入客户用户名" /></view>
        </label>
        <label class="field">
          <text>密码</text>
          <view class="input-wrap"><AppIcon type="locked" :size="20" /><input v-model="password" :password="!showPassword" maxlength="128" autocomplete="current-password" placeholder="请输入登录密码" /><button type="button" class="password-toggle" @click="showPassword = !showPassword">{{ showPassword ? '隐藏' : '显示' }}</button></view>
        </label>
        <!-- #ifdef MP-WEIXIN -->
        <button v-if="!authStore.isDevelopmentLogin" type="button" class="bind-row" @click="bindWechat = !bindWechat">
          <view class="check-box" :class="{ checked: bindWechat }"><AppIcon v-if="bindWechat" type="checkmarkempty" :size="14" /></view>
          <view><text>同时绑定当前微信</text><small>绑定后下次可以微信一键登录</small></view>
        </button>
        <!-- #endif -->
        <view v-if="authStore.isDevelopmentLogin" class="dev-hint">
          <text>本地测试账号</text><text class="mono">local_client / LocalClient-2026!</text>
        </view>
        <button class="account-button" form-type="submit" :disabled="!canSubmit">{{ submitting ? '正在登录…' : '登录账号' }}</button>
        <button type="button" class="switch-button" @click="mode = 'quick'; formError = ''">返回微信登录</button>
      </form>

      <view v-if="formError" class="login-error"><AppIcon type="info-filled" :size="17" /><text>{{ formError }}</text></view>
    </view>

    <text class="security-copy">登录表示你同意按业务办理需要使用账号信息。密码只用于身份验证，不会以明文保存。</text>
  </view>
</template>

<style scoped>
.login-page { min-height: 100vh; padding: 70rpx 32rpx calc(50rpx + env(safe-area-inset-bottom)); background: radial-gradient(circle at 88% 8%, rgba(239,189,91,.26), transparent 26%), linear-gradient(155deg, #ecf5f3 0%, #f7f9f8 48%, #e7f1ef 100%); }
.brand-block { display: flex; flex-direction: column; align-items: center; text-align: center; }
.brand-mark { display: flex; align-items: center; justify-content: center; width: 92rpx; height: 92rpx; border-radius: 28rpx; background: linear-gradient(145deg, #164f50, #26706f); color: #fff; box-shadow: 0 20rpx 42rpx rgba(22,79,80,.2); }
.brand-kicker { margin-top: 24rpx; color: #527372; font-size: 18rpx; font-weight: 700; letter-spacing: 3rpx; }
.brand-title { margin-top: 8rpx; color: #153f40; font-size: 46rpx; font-weight: 900; letter-spacing: 2rpx; }
.brand-copy { max-width: 590rpx; margin-top: 12rpx; color: #708182; font-size: 22rpx; line-height: 1.6; }
.login-card { margin-top: 44rpx; padding: 18rpx 28rpx 30rpx; border: 1rpx solid rgba(202,219,217,.94); border-radius: 32rpx; background: rgba(255,255,255,.94); box-shadow: 0 28rpx 70rpx rgba(30,75,74,.11); }
.login-tabs { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8rpx; padding: 7rpx; border-radius: 20rpx; background: #edf3f2; }
.login-tabs button { height: 66rpx; margin: 0; padding: 0; border-radius: 16rpx; background: transparent; color: #718181; font-size: 23rpx; font-weight: 700; line-height: 66rpx; }
.login-tabs button.active { background: #fff; color: var(--color-primary); box-shadow: 0 5rpx 16rpx rgba(30,74,74,.08); }
.quick-panel, .account-panel { display: flex; flex-direction: column; align-items: stretch; padding-top: 32rpx; }
.quick-panel { align-items: center; text-align: center; }
.quick-icon { display: flex; align-items: center; justify-content: center; width: 92rpx; height: 92rpx; border-radius: 50%; background: #e8f6ed; color: #1d9a50; }
.panel-title, .panel-copy { display: block; }
.panel-title { margin-top: 18rpx; color: #173e40; font-size: 31rpx; font-weight: 900; text-align: center; }
.account-panel .panel-title { margin-top: 0; }
.panel-copy { margin: 10rpx auto 0; color: #778587; font-size: 21rpx; line-height: 1.65; text-align: center; }
.wechat-button, .account-button { width: 100%; height: 82rpx; margin: 30rpx 0 0; border-radius: 22rpx; background: #1f9d55; color: #fff; font-size: 26rpx; font-weight: 800; line-height: 82rpx; box-shadow: 0 14rpx 28rpx rgba(31,157,85,.17); }
.account-button { background: var(--color-primary); box-shadow: 0 14rpx 28rpx rgba(22,79,80,.16); }
.wechat-button[disabled], .account-button[disabled] { opacity: .5; }
.switch-button { height: 58rpx; margin: 12rpx 0 0; padding: 0 18rpx; background: transparent; color: var(--color-primary-light); font-size: 21rpx; line-height: 58rpx; }
.field { display: block; width: 100%; margin-top: 24rpx; }
.field > text { display: block; margin-bottom: 10rpx; color: #385557; font-size: 21rpx; font-weight: 700; }
.input-wrap { display: flex; align-items: center; width: 100%; height: 82rpx; padding: 0 20rpx; border: 1rpx solid #d5e1e0; border-radius: 20rpx; background: #f9fbfa; color: #7d8d8e; }
.input-wrap:focus-within { border-color: #458281; background: #fff; box-shadow: 0 0 0 5rpx rgba(38,112,111,.08); }
.input-wrap input { flex: 1; min-width: 0; height: 78rpx; margin-left: 14rpx; color: #1d3335; font-size: 24rpx; }
.password-toggle { flex: none; height: 54rpx; margin: 0 0 0 10rpx; padding: 0 10rpx; background: transparent; color: #477878; font-size: 20rpx; line-height: 54rpx; }
.bind-row { display: flex; align-items: center; width: 100%; min-height: 76rpx; margin: 18rpx 0 0; padding: 10rpx 0; background: transparent; color: #304c4e; text-align: left; line-height: 1.3; }
.check-box { display: flex; flex: none; align-items: center; justify-content: center; width: 34rpx; height: 34rpx; margin-right: 14rpx; border: 2rpx solid #aebdbc; border-radius: 9rpx; color: #fff; }
.check-box.checked { border-color: var(--color-primary); background: var(--color-primary); }
.bind-row text, .bind-row small { display: block; }
.bind-row text { font-size: 21rpx; font-weight: 700; }
.bind-row small { margin-top: 5rpx; color: #839091; font-size: 18rpx; }
.dev-hint { margin-top: 20rpx; padding: 18rpx 20rpx; border-radius: 16rpx; background: #fff7e7; color: #7f5b1c; font-size: 19rpx; }
.dev-hint text { display: block; }
.dev-hint .mono { margin-top: 7rpx; font-family: ui-monospace, SFMono-Regular, Consolas, monospace; }
.login-error { display: flex; align-items: flex-start; gap: 10rpx; margin-top: 18rpx; padding: 16rpx 18rpx; border-radius: 16rpx; background: #fff0ee; color: #a13f38; font-size: 20rpx; line-height: 1.5; }
.security-copy { display: block; margin: 26rpx auto 0; max-width: 600rpx; color: #8a9798; font-size: 18rpx; line-height: 1.6; text-align: center; }
</style>
