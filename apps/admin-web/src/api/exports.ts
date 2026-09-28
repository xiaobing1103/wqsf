import { exportDownload, exportExcel, exportGet, exportZip } from "./generated/endpoints";
import type { DownloadView, ExportJobView } from "./generated/models";
import { unwrapResponse } from "./unwrap";

export type { ExportJobView } from "./generated/models";

/**
 * 导出接口目前在 OpenAPI 中与资料审核请求共用了历史类型名。
 * 领域适配层按后端真实契约传递 caseIds，页面不感知这个兼容细节。
 */
export function createReviewExcel(caseIds: number[], idempotencyKey: string) {
  return exportExcel({ caseIds }, { headers: { "Idempotency-Key": idempotencyKey } }).then((response) =>
    unwrapResponse<ExportJobView>(response),
  );
}

export function createMaterialZip(caseId: number, includeSensitive = false, idempotencyKey = `zip-${caseId}-${Date.now()}`) {
  return exportZip({ caseId, includeSensitive }, { headers: { "Idempotency-Key": idempotencyKey } }).then((response) =>
    unwrapResponse<ExportJobView>(response),
  );
}

export function getExportJob(jobId: number) {
  return exportGet(jobId).then((response) => unwrapResponse<ExportJobView>(response));
}

export function getExportDownload(jobId: number) {
  return exportDownload(jobId).then((response) => unwrapResponse<DownloadView>(response));
}
