export const CASE_STATUS_LABEL: Record<string, string> = {
  DRAFT: '草稿',
  PENDING_REVIEW: '待审核',
  NEED_SUPPLEMENT: '待补资料',
  PROCESSING: '处理中',
  COMPLETED: '已完成',
  FAILED: '失败',
}

export function caseStatusLabel(status?: string) {
  return status ? CASE_STATUS_LABEL[status] || status : '状态待更新'
}

export function caseStatusTone(status?: string) {
  if (status === 'COMPLETED') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'NEED_SUPPLEMENT') return 'warning'
  if (status === 'PROCESSING' || status === 'PENDING_REVIEW') return 'info'
  return 'neutral'
}

export function materialStatusLabel(submitStatus?: string, reviewStatus?: string, inputType?: string) {
  if (reviewStatus === 'NEED_SUPPLEMENT') return '待补充'
  if (reviewStatus === 'PASSED') return '已通过'
  if (reviewStatus === 'NOT_REQUIRED') return '无需提交'
  if (submitStatus === 'SUBMITTED') return inputType === 'FILE' ? '已上传' : '已填写'
  return '待补充'
}

export function materialStatusTone(submitStatus?: string, reviewStatus?: string) {
  if (reviewStatus === 'PASSED' || reviewStatus === 'NOT_REQUIRED') return 'success'
  if (reviewStatus === 'NEED_SUPPLEMENT' || submitStatus !== 'SUBMITTED') return 'warning'
  return 'info'
}
