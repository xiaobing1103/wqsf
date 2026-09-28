import { adminCompanyList } from "./generated/endpoints";
import type { AdminCompanyListParams, PageResponseCompanyView } from "./generated/models";
import { unwrapResponse } from "./unwrap";

export function listAdminCompanies(params: AdminCompanyListParams = {}) {
  return adminCompanyList(params).then((response) =>
    unwrapResponse<PageResponseCompanyView>(response),
  );
}
