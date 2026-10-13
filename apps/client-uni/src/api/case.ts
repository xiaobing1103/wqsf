import { caseCreate, caseDetail, caseInvoice, caseMine, caseSimple, caseSubmit, caseText } from './generated/endpoints'
import type { CaseSummary, CaseView, CreateRequest, InvoiceRequest, InvoiceView, MaterialView, PageResponseCaseSummary, SimpleRequest } from './generated/models'
import { requireData } from './response'

export async function getMyCases(page = 1, pageSize = 10): Promise<CaseSummary[]> {
  return (await getMyCasePage(page, pageSize)).items || []
}

export async function getMyCasePage(page = 1, pageSize = 10, status?: string): Promise<PageResponseCaseSummary> {
  const response = await caseMine({ page, pageSize, status })
  return requireData<PageResponseCaseSummary>(response, '读取报单失败')
}

export function getCaseDetail(caseId: number) {
  return caseDetail(caseId).then((response) => requireData<CaseView>(response, '读取报单详情失败'))
}

export function createCaseDraft(request: CreateRequest, idempotencyKey: string) {
  return caseCreate(request, { headers: { 'Idempotency-Key': idempotencyKey } })
    .then((response) => requireData<CaseView>(response, '创建报单草稿失败'))
}

export function saveInvoice(caseId: number, request: InvoiceRequest) {
  return caseInvoice(caseId, request).then((response) => requireData<InvoiceView>(response, '保存开票信息失败'))
}

export function saveSimpleInfo(caseId: number, request: SimpleRequest) {
  return caseSimple(caseId, request).then((response) => requireData<CaseView>(response, '保存咨询信息失败'))
}

export function saveMaterialText(caseId: number, materialId: number, value: string) {
  return caseText(caseId, materialId, { value })
    .then((response) => requireData<MaterialView>(response, '保存资料内容失败'))
}

export function submitCase(caseId: number, idempotencyKey: string) {
  return caseSubmit(caseId, { headers: { 'Idempotency-Key': idempotencyKey } })
    .then((response) => requireData<CaseView>(response, '提交报单失败'))
}
