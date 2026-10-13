import { authClientLogin, authDevLogin, authLogout, authMe, authWechat } from './generated/endpoints'
import type { ClientLoginRequest, CurrentUser, LoginResult } from './generated/models'
import { requireData } from './response'

export function loginForLocalDevelopment() {
  return authDevLogin({}).then((response) => requireData<LoginResult>(response, '本地客户登录失败'))
}

export function loginWithWechat(code: string) {
  return authWechat({ code }).then((response) => requireData<LoginResult>(response, '微信登录失败'))
}

export function loginWithPassword(request: ClientLoginRequest) {
  return authClientLogin(request).then((response) => requireData<LoginResult>(response, '账号登录失败'))
}

export function getCurrentUser() {
  return authMe().then((response) => requireData<CurrentUser>(response, '读取当前用户失败'))
}

export async function logoutClient() {
  await authLogout()
}
