import type { AdminLoginPayload, AdminUser, LoginResult } from "../types/auth";
import { apiRequest } from "./request";

export function loginAdmin(payload: AdminLoginPayload) {
  return apiRequest<LoginResult>("/auth/admin/login", {
    method: "POST",
    body: payload,
    auth: false,
  });
}

export function getCurrentUser() {
  return apiRequest<AdminUser>("/auth/me");
}

export function logoutAdmin() {
  return apiRequest<void>("/auth/logout", { method: "POST" });
}
