import type { DashboardData } from "../types/dashboard";
import { apiRequest } from "./request";

export function getDashboard() {
  return apiRequest<DashboardData>("/admin/dashboard");
}
