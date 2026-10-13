import type { AccountView } from "./generated/models";
import { apiRequest } from "./request";

export interface CreateAdminAccountPayload {
  accountType: "ADMIN";
  username: string;
  password: string;
  displayName: string;
  phone: string;
  roles: string[];
}

export function listAdminAccounts() {
  return apiRequest<AccountView[]>("/admin/accounts");
}

export function createAdminAccount(payload: CreateAdminAccountPayload) {
  return apiRequest<AccountView>("/admin/accounts", {
    method: "POST",
    body: payload,
  });
}

export function updateAdminAccountRoles(userId: number, roles: string[]) {
  return apiRequest<AccountView>(`/admin/accounts/${userId}/roles`, {
    method: "PUT",
    body: { roles },
  });
}
