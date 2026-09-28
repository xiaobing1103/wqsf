import {
  adminCaseAssign,
  adminCaseDetail,
  adminCaseList,
  adminCaseReview,
  adminCaseStatus,
  adminCaseSupplement,
} from "./generated/endpoints";
import type {
  AdminCaseListParams,
  AssignRequest,
  CaseView,
  ReviewRequest,
  StatusRequest,
  SupplementRequest,
  MaterialView,
  PageResponseCaseSummary,
} from "./generated/models";
import { unwrapResponse } from "./unwrap";

export function listAdminCases(params: AdminCaseListParams = {}) {
  return adminCaseList(params).then((response) =>
    unwrapResponse<PageResponseCaseSummary>(response),
  );
}

export function getAdminCase(caseId: number) {
  return adminCaseDetail(caseId).then((response) => unwrapResponse<CaseView>(response));
}

export function assignAdminCase(caseId: number, request: AssignRequest) {
  return adminCaseAssign(caseId, request).then((response) => unwrapResponse<CaseView>(response));
}

export function updateAdminCaseStatus(caseId: number, request: StatusRequest) {
  return adminCaseStatus(caseId, request).then((response) => unwrapResponse<CaseView>(response));
}

export function reviewMaterial(materialId: number, request: ReviewRequest) {
  return adminCaseReview(materialId, request).then((response) => unwrapResponse<MaterialView>(response));
}

export function requestSupplement(caseId: number, request: SupplementRequest) {
  return adminCaseSupplement(caseId, request, {
    headers: { "Idempotency-Key": `admin-supplement-${caseId}-${Date.now()}` },
  }).then((response) => unwrapResponse(response));
}
