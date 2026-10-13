import type { ProductView } from '../api/generated/models'

export type ServiceKind = 'TRADE_INCREMENT' | 'SIMPLE'
export type ServiceType = 'TRADE_INCREMENT' | 'IP' | 'QUALIFICATION'

export function serviceKind(product?: ProductView): ServiceKind {
  const marker = `${product?.productCode || ''} ${product?.productName || ''}`.toUpperCase()
  return /TRADE|贸易|增量/.test(marker) ? 'TRADE_INCREMENT' : 'SIMPLE'
}

export function serviceIcon(product?: ProductView) {
  const marker = `${product?.productCode || ''} ${product?.productName || ''}`.toUpperCase()
  if (/TRADE|贸易|增量/.test(marker)) return 'arrow'
  if (/IP|知识|产权/.test(marker)) return 'diamond'
  return 'spark'
}

export function serviceRoute(product: ProductView) {
  const query = `productId=${product.id || ''}&serviceType=${serviceType(product)}`
  return serviceKind(product) === 'TRADE_INCREMENT'
    ? `/pages/case/materials?${query}`
    : `/pages/service/simple?${query}`
}

export function caseDetailRoute(caseId: number, type?: string) {
  return type === 'TRADE_INCREMENT'
    ? `/pages/case/materials?caseId=${caseId}`
    : `/pages/service/simple?caseId=${caseId}&serviceType=${type || ''}`
}

export function serviceType(product?: ProductView): ServiceType {
  const marker = `${product?.productCode || ''} ${product?.productName || ''}`.toUpperCase()
  if (/TRADE|贸易|增量/.test(marker)) return 'TRADE_INCREMENT'
  if (/IP|知识|产权|商标/.test(marker)) return 'IP'
  return 'QUALIFICATION'
}
