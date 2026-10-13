export type ApiEnvelope<T> = {
  success?: boolean
  message?: string
  data?: T
}

export function requireData<T>(response: ApiEnvelope<T>, fallbackMessage = '服务暂时不可用'): T {
  if (response.success === false) throw new Error(response.message || fallbackMessage)
  if (response.data === undefined || response.data === null) throw new Error(response.message || fallbackMessage)
  return response.data
}

export function requireSuccess(response: ApiEnvelope<unknown>, fallbackMessage = '操作失败，请稍后重试') {
  if (response.success === false) throw new Error(response.message || fallbackMessage)
}

export function errorMessage(error: unknown, fallbackMessage = '请求失败，请稍后重试') {
  return error instanceof Error && error.message ? error.message : fallbackMessage
}
