import { adminCompanyList } from "./generated/endpoints";
import type { AccountView, AdminCompanyListParams, CompanyView, PageResponseCompanyView } from "./generated/models";
import { apiRequest } from "./request";
import { unwrapResponse } from "./unwrap";

export interface CompanyPayload {
  companyName: string;
  creditCode?: string;
  contactName?: string;
  contactPhone?: string;
  legalPerson?: string;
  province?: string;
  city?: string;
  companyEmail?: string;
  detailAddress?: string;
}

export interface CompanyUpdatePayload extends CompanyPayload {
  version: number;
}

export interface ClientAccountPayload {
  username: string;
  password: string;
  displayName: string;
  phone: string;
  memberRole: "OWNER" | "CONTACT" | "VIEWER";
}

export function listAdminCompanies(params: AdminCompanyListParams = {}) {
  return adminCompanyList(params).then((response) =>
    unwrapResponse<PageResponseCompanyView>(response),
  );
}

export function getAdminCompany(companyId: number) {
  return apiRequest<CompanyView>(`/admin/companies/${companyId}`);
}

export function createAdminCompany(payload: CompanyPayload) {
  return apiRequest<CompanyView>("/admin/companies", {
    method: "POST",
    body: payload,
  });
}

export function updateAdminCompany(companyId: number, payload: CompanyUpdatePayload) {
  return apiRequest<CompanyView>(`/admin/companies/${companyId}`, {
    method: "PATCH",
    body: payload,
  });
}

export function deleteAdminCompany(companyId: number, version: number) {
  return apiRequest<void>(`/admin/companies/${companyId}?version=${encodeURIComponent(version)}`, {
    method: "DELETE",
  });
}

export function listCompanyClientAccounts(companyId: number) {
  return apiRequest<AccountView[]>(`/admin/companies/${companyId}/client-accounts`);
}

export function createCompanyClientAccount(companyId: number, payload: ClientAccountPayload) {
  return apiRequest<AccountView>(`/admin/companies/${companyId}/client-accounts`, {
    method: "POST",
    body: payload,
  });
}

export function resetCompanyClientPassword(companyId: number, userId: number, password: string) {
  return apiRequest<AccountView>(`/admin/companies/${companyId}/client-accounts/${userId}/password`, {
    method: "PUT",
    body: { password },
  });
}
