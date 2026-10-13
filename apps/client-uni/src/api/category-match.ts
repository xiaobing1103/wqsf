import { categoryMatchRecommend } from './generated/endpoints'
import type { BusinessLicenseView, CategoryMatchRequest, CategoryMatchView } from './generated/models'
import { API_BASE_URL, CLIENT_AUTH_EXPIRED_EVENT, CLIENT_TOKEN_STORAGE_KEY } from './generated-client'
import { requireData } from './response'

export function recommendCategories(request: CategoryMatchRequest): Promise<CategoryMatchView> {
  return categoryMatchRecommend(request)
    .then((response) => requireData<CategoryMatchView>(response, '品类匹配失败'))
}

/** OCR 图片必须通过 uni.uploadFile 发送；后端识别完成后不会保存原图。 */
export function recognizeBusinessLicense(filePath: string): Promise<BusinessLicenseView> {
  const token = uni.getStorageSync(CLIENT_TOKEN_STORAGE_KEY)
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${API_BASE_URL}/category-match/recognize`,
      filePath,
      name: 'file',
      header: {
        Accept: 'application/json',
        Authorization: token ? `Bearer ${token}` : '',
        'X-Request-Id': `uni-ocr-${Date.now()}-${Math.random().toString(16).slice(2)}`,
      },
      success: (response) => {
        let payload: { success?: boolean; message?: string; data?: BusinessLicenseView }
        try { payload = JSON.parse(response.data) } catch { reject(new Error('识别服务返回了无法读取的数据')); return }
        if (response.statusCode === 401) {
          uni.removeStorageSync(CLIENT_TOKEN_STORAGE_KEY)
          uni.$emit(CLIENT_AUTH_EXPIRED_EVENT)
        }
        if (response.statusCode < 200 || response.statusCode >= 300 || payload.success === false) {
          reject(new Error(payload.message || `识别失败（${response.statusCode}）`))
          return
        }
        if (!payload.data) { reject(new Error('识别响应缺少营业执照信息')); return }
        resolve(payload.data)
      },
      fail: (error) => reject(new Error(error.errMsg || '无法连接营业执照识别服务')),
    })
  })
}
