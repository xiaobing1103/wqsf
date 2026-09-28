export type CaseStatus =
  | "DRAFT"
  | "PENDING_REVIEW"
  | "NEED_SUPPLEMENT"
  | "PROCESSING"
  | "COMPLETED"
  | "CANCELED";

export interface DashboardData {
  enabledCompanies: number;
  caseStatuses: Record<CaseStatus, number>;
  pendingImports: number;
  pendingExports: number;
}
