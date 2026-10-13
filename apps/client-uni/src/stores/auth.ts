import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { CurrentUser, LoginResult } from '../api/generated/models'
import {
  getCurrentUser,
  loginForLocalDevelopment,
  loginWithPassword,
  loginWithWechat,
  logoutClient,
} from '../api/auth'
import { CLIENT_TOKEN_STORAGE_KEY } from '../api/generated-client'
import { errorMessage } from '../api/response'
import { useCaseDraftStore } from './caseDraft'
import { useCatalogStore } from './catalog'
import { useCompanyStore } from './company'

type SessionStatus = 'idle' | 'loading' | 'ready' | 'error'
let sessionPromise: Promise<boolean> | undefined
const CLIENT_AUTH_MODE = String((import.meta as ImportMeta & { env?: Record<string, string> }).env?.VITE_CLIENT_AUTH_MODE || '')
  .trim()
  .toLowerCase()

function requestWechatCode() {
  return new Promise<string>((resolve, reject) => {
    uni.login({
      provider: 'weixin',
      success: (result) => result.code ? resolve(result.code) : reject(new Error('微信未返回登录凭证')),
      fail: () => reject(new Error('微信登录授权失败')),
    })
  })
}

function currentPagePath() {
  const pages = getCurrentPages()
  return pages[pages.length - 1]?.route || ''
}

export const useAuthStore = defineStore('client-auth', () => {
  const token = ref(String(uni.getStorageSync(CLIENT_TOKEN_STORAGE_KEY) || ''))
  const user = ref<CurrentUser>()
  const status = ref<SessionStatus>('idle')
  const error = ref('')
  const isReady = computed(() => status.value === 'ready' && Boolean(token.value) && Boolean(user.value))
  const isDevelopmentLogin = computed(() => CLIENT_AUTH_MODE === 'dev')

  function saveToken(value: string) {
    token.value = value
    uni.setStorageSync(CLIENT_TOKEN_STORAGE_KEY, value)
  }

  function clearBusinessState() {
    useCompanyStore().reset()
    useCatalogStore().reset()
    useCaseDraftStore().reset()
  }

  function clearLocalSession() {
    token.value = ''
    user.value = undefined
    uni.removeStorageSync(CLIENT_TOKEN_STORAGE_KEY)
    clearBusinessState()
  }

  function goToLogin() {
    if (currentPagePath() === 'pages/auth/login') return
    uni.reLaunch({ url: '/pages/auth/login' })
  }

  async function acceptLogin(result: LoginResult | undefined) {
    if (!result?.token) throw new Error('登录响应缺少访问令牌')
    clearBusinessState()
    saveToken(result.token)
    user.value = await getCurrentUser()
    status.value = 'ready'
    error.value = ''
  }

  async function restoreSession() {
    if (isReady.value) return true
    if (!token.value) {
      status.value = 'idle'
      return false
    }
    if (sessionPromise) return sessionPromise
    sessionPromise = (async () => {
      status.value = 'loading'
      error.value = ''
      try {
        user.value = await getCurrentUser()
        status.value = 'ready'
        return true
      } catch {
        clearLocalSession()
        status.value = 'idle'
        return false
      }
    })().finally(() => { sessionPromise = undefined })
    return sessionPromise
  }

  async function ensureSession() {
    if (await restoreSession()) return
    goToLogin()
    throw new Error('请先登录客户账号')
  }

  async function loginByWechat() {
    status.value = 'loading'
    error.value = ''
    try {
      const result = isDevelopmentLogin.value
        ? await loginForLocalDevelopment()
        : await loginWithWechat(await requestWechatCode())
      await acceptLogin(result)
    } catch (cause) {
      clearLocalSession()
      status.value = 'error'
      error.value = errorMessage(cause, '微信登录失败')
      throw cause
    }
  }

  async function loginByPassword(username: string, password: string, bindWechat: boolean) {
    status.value = 'loading'
    error.value = ''
    try {
      let wechatCode: string | undefined
      let shouldBind = bindWechat && !isDevelopmentLogin.value
      // #ifndef MP-WEIXIN
      shouldBind = false
      // #endif
      // #ifdef MP-WEIXIN
      if (shouldBind) wechatCode = await requestWechatCode()
      // #endif
      const result = await loginWithPassword({ username, password, bindWechat: shouldBind, wechatCode })
      await acceptLogin(result)
    } catch (cause) {
      clearLocalSession()
      status.value = 'error'
      error.value = errorMessage(cause, '账号登录失败')
      throw cause
    }
  }

  async function logout() {
    try {
      if (token.value) await logoutClient()
    } finally {
      clearLocalSession()
      status.value = 'idle'
      error.value = ''
      goToLogin()
    }
  }

  function handleExpiredSession() {
    clearLocalSession()
    status.value = 'idle'
    error.value = '登录状态已失效，请重新登录'
    goToLogin()
  }

  return {
    token,
    user,
    status,
    error,
    isReady,
    isDevelopmentLogin,
    restoreSession,
    ensureSession,
    loginByWechat,
    loginByPassword,
    logout,
    handleExpiredSession,
  }
})
