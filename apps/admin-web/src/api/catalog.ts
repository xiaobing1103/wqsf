import { catalogCatalog, catalogTemplate, adminProductList } from "./generated/endpoints";
import type {
  ModuleView,
  ProductAdminView,
  TemplateView,
} from "./generated/models";
import { unwrapResponse } from "./unwrap";

export function getServiceCatalog() {
  return catalogCatalog().then((response) => unwrapResponse<ModuleView[]>(response));
}

export function getProductTemplate(productId: number) {
  return catalogTemplate(productId).then((response) => unwrapResponse<TemplateView>(response));
}

export function listAdminProducts() {
  return adminProductList().then((response) => unwrapResponse<ProductAdminView[]>(response));
}
