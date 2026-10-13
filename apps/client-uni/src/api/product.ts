import { catalogCatalog, catalogTemplate } from './generated/endpoints'
import type { ModuleView, TemplateView } from './generated/models'
import { requireData } from './response'

export function getPublishedCatalog() {
  return catalogCatalog().then((response) => requireData<ModuleView[]>(response, '读取服务目录失败'))
}

export function getProductTemplate(productId: number) {
  return catalogTemplate(productId).then((response) => requireData<TemplateView>(response, '读取资料模板失败'))
}
