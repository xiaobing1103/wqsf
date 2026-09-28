import { defineStore } from "pinia";
import { getCurrentUser, loginAdmin, logoutAdmin } from "../api/auth";
import { ADMIN_TOKEN_STORAGE_KEY } from "../api/request";
import type { AdminLoginPayload, AdminUser } from "../types/auth";

interface AuthState {
  token: string;
  user: AdminUser | null;
  restoring: boolean;
}

export const useAuthStore = defineStore("auth", {
  state: (): AuthState => ({
    token: sessionStorage.getItem(ADMIN_TOKEN_STORAGE_KEY) || "",
    user: null,
    restoring: false,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token && state.user),
    hasPermission: (state) => (permission: string) =>
      Boolean(state.user?.permissions.includes(permission)),
  },
  actions: {
    async login(payload: AdminLoginPayload) {
      const result = await loginAdmin(payload);
      this.token = result.token;
      sessionStorage.setItem(ADMIN_TOKEN_STORAGE_KEY, result.token);
      try {
        await this.fetchCurrentUser();
      } catch (error) {
        this.clearSession();
        throw error;
      }
    },
    async fetchCurrentUser() {
      const user = await getCurrentUser();
      if (user.accountType !== "ADMIN")
        throw new Error("当前账号不是后台管理员");
      this.user = user;
      return user;
    },
    async restoreSession() {
      if (!this.token) return false;
      if (this.user) return true;
      if (this.restoring) return false;
      this.restoring = true;
      try {
        await this.fetchCurrentUser();
        return true;
      } catch {
        this.clearSession();
        return false;
      } finally {
        this.restoring = false;
      }
    },
    async logout() {
      try {
        await logoutAdmin();
      } finally {
        this.clearSession();
      }
    },
    clearSession() {
      this.token = "";
      this.user = null;
      sessionStorage.removeItem(ADMIN_TOKEN_STORAGE_KEY);
    },
  },
});
