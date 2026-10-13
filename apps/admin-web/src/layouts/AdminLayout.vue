<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { createAdminAccount, listAdminAccounts, updateAdminAccountRoles } from "../api/accounts";
import { getDashboard } from "../api/dashboard";
import { getAdminCase, getCredentialHandover, listAdminCases, requestSupplement, reviewMaterial, saveCredentialHandover, updateAdminCaseStatus } from "../api/cases";
import { createAdminCompany, createCompanyClientAccount, deleteAdminCompany, getAdminCompany, listAdminCompanies, listCompanyClientAccounts, resetCompanyClientPassword, updateAdminCompany, type CompanyPayload } from "../api/companies";
import { getProductTemplate, getServiceCatalog, listAdminProducts } from "../api/catalog";
import { createMaterialZip, createReviewExcel, getExportDownload, getExportJob, type ExportJobView } from "../api/exports";
import { importCreate, importTemplate } from "../api/generated/endpoints";
import { unwrapResponse } from "../api/unwrap";
import AdminIcon from "../components/AdminIcon.vue";
import { useAuthStore } from "../stores/auth";
import { ApiError } from "../types/api";
import type { DashboardData } from "../types/dashboard";
import type { AccountView, CaseSummary, CaseView, CompanyView, CredentialHandoverView, JobView as ImportJobView, ModuleView, ProductAdminView, TemplateDownload, TemplateView } from "../api/generated/models";

type ViewKey = "dashboard" | "cases" | "customers" | "exports" | "products" | "team";
type CompanyRecord = CompanyView & { version?: number };
type CompanyDialogMode = "create" | "view" | "edit";

interface CompanyFormState extends CompanyPayload {
  companyName: string;
}

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const current = computed<ViewKey>(() => route.name === "companies" ? "customers" : (route.name as ViewKey));
const selectedCaseId = ref<number | null>(null);
const caseDetail = ref<CaseView | null>(null);
const toast = ref("");
const search = ref("");
const filter = ref("全部状态");
const dashboard = ref<DashboardData | null>(null);
const dashboardLoading = ref(false);
const dashboardError = ref("");
const logoutConfirmVisible = ref(false);

const cases = ref<CaseSummary[]>([]);
const recentCases = ref<CaseSummary[]>([]);
const caseLoading = ref(false);
const caseError = ref("");
const casePage = ref({ page: 1, pageSize: 20, total: 0, totalPages: 0 });
const caseDetailLoading = ref(false);
const caseActionLoading = ref(false);
const credentialHandover = ref<CredentialHandoverView | null>(null);
const credentialHandoverLoading = ref(false);
const credentialHandoverError = ref("");

const companies = ref<CompanyRecord[]>([]);
const companyLoading = ref(false);
const companyError = ref("");
const companySearch = ref("");
const companyPage = ref({ page: 1, pageSize: 20, total: 0, totalPages: 0 });
const importInput = ref<HTMLInputElement | null>(null);
const importLoading = ref(false);
const companyMenuId = ref<number | null>(null);
const companyDialogMode = ref<CompanyDialogMode | null>(null);
const selectedCompany = ref<CompanyRecord | null>(null);
const companyActionLoading = ref(false);
const companyActionError = ref("");
const companyDeleteTarget = ref<CompanyRecord | null>(null);
const companyForm = ref<CompanyFormState>(emptyCompanyForm());
const clientAccountCompany = ref<CompanyRecord | null>(null);
const clientAccounts = ref<AccountView[]>([]);
const clientAccountLoading = ref(false);
const clientAccountError = ref("");
const clientAccountForm = ref({ displayName: "", username: "", phone: "", password: "", memberRole: "OWNER" as "OWNER" | "CONTACT" | "VIEWER" });
const clientPasswordTarget = ref<AccountView | null>(null);
const clientResetPassword = ref("");

const teamAccounts = ref<AccountView[]>([]);
const teamLoading = ref(false);
const teamError = ref("");
const teamSearch = ref("");
const createMemberVisible = ref(false);
const memberActionLoading = ref(false);
const memberActionError = ref("");
const roleEditorAccount = ref<AccountView | null>(null);
const permissionAccount = ref<AccountView | null>(null);
const selectedRole = ref("OPERATIONS");
const memberForm = ref({ displayName: "", username: "", phone: "", password: "", role: "OPERATIONS" });

const exportJobs = ref<ExportJobView[]>([]);
const exportLoading = ref(false);
const exportError = ref("");
const catalog = ref<ModuleView[]>([]);
const products = ref<ProductAdminView[]>([]);
const selectedTemplate = ref<TemplateView | null>(null);
const selectedProductId = ref<number | null>(null);
const templateLoadingProductId = ref<number | null>(null);
const templateDetailRef = ref<HTMLElement | null>(null);
const catalogLoading = ref(false);

const displayName = computed(() => auth.user?.displayName || "管理员");
const avatarText = computed(() => displayName.value.slice(0, 1));
const canDeleteCompanies = computed(() => auth.hasRole("SUPER_ADMIN"));
const roleOptions = [
  { value: "OPERATIONS", label: "运营人员", description: "负责客户、报单流转、导入与业务资料导出。" },
  { value: "REVIEWER", label: "审核人员", description: "负责资料审核、补件处理和受控资料查看。" },
  { value: "SUPER_ADMIN", label: "超级管理员", description: "拥有全部权限，可管理团队角色和停用企业。" },
];
const roleLabels: Record<string, string> = Object.fromEntries(roleOptions.map((item) => [item.value, item.label]));
const permissionLabels: Record<string, string> = {
  "dashboard:view": "查看工作台", "case:view": "查看报单", "case:create": "创建报单",
  "case:assign": "分配负责人", "case:review": "审核资料", "case:credential-handover": "登记线下凭证交接",
  "product:view": "查看业务模板", "product:edit": "编辑产品", "company:view": "查看与维护客户",
  "company:import": "导入客户 Excel", "file:sensitive:view": "查看敏感资料",
  "export:excel": "导出审核 Excel", "export:zip": "导出资料 ZIP", "audit:view": "查看操作日志",
};
const visibleTeamAccounts = computed(() => {
  const keyword = teamSearch.value.trim().toLowerCase();
  return teamAccounts.value.filter((item) => item.accountType === "ADMIN" && (!keyword
    || item.displayName?.toLowerCase().includes(keyword)
    || item.username?.toLowerCase().includes(keyword)
    || item.phoneMask?.toLowerCase().includes(keyword)));
});
const teamStats = computed(() => ({
  total: teamAccounts.value.filter((item) => item.accountType === "ADMIN").length,
  operations: teamAccounts.value.filter((item) => item.accountType === "ADMIN" && item.roles?.includes("OPERATIONS")).length,
  reviewers: teamAccounts.value.filter((item) => item.accountType === "ADMIN" && item.roles?.includes("REVIEWER")).length,
  admins: teamAccounts.value.filter((item) => item.accountType === "ADMIN" && item.roles?.includes("SUPER_ADMIN")).length,
}));
const currentDate = new Intl.DateTimeFormat("zh-CN", { year: "numeric", month: "2-digit", day: "2-digit", weekday: "long" }).format(new Date());
const statusLabels: Record<string, string> = {
  DRAFT: "草稿", PENDING_REVIEW: "待审核", NEED_SUPPLEMENT: "待补资料", PROCESSING: "处理中", COMPLETED: "已完成", CANCELED: "已取消",
};
const statusQuery: Record<string, string> = { "待审核": "PENDING_REVIEW", "待补资料": "NEED_SUPPLEMENT", 处理中: "PROCESSING", 已完成: "COMPLETED", 已取消: "CANCELED" };
const dashboardStats = computed(() => ({
  pendingReview: dashboard.value?.caseStatuses.PENDING_REVIEW ?? 0,
  processing: dashboard.value?.caseStatuses.PROCESSING ?? 0,
  completed: dashboard.value?.caseStatuses.COMPLETED ?? 0,
  supplement: dashboard.value?.caseStatuses.NEED_SUPPLEMENT ?? 0,
  enabledCompanies: dashboard.value?.enabledCompanies ?? 0,
  pendingImports: dashboard.value?.pendingImports ?? 0,
  pendingExports: dashboard.value?.pendingExports ?? 0,
}));
const filteredCases = computed(() => cases.value);
const caseProgress = computed(() => caseDetail.value?.progress ?? { completed: 0, total: 0, requiredRemaining: 0 });
const caseProgressPercent = computed(() => caseProgress.value.total ? Math.round(((caseProgress.value.completed ?? 0) / caseProgress.value.total) * 100) : 0);
const selectedCaseStatusLabel = computed(() => statusLabels[caseDetail.value?.status || ""] || caseDetail.value?.status || "");
const canManageCredentials = computed(() => auth.hasPermission("case:credential-handover"));
const credentialStatusOptions = [
  { value: "PENDING", label: "待线下交接" },
  { value: "HANDED_OVER", label: "已线下交接" },
  { value: "VERIFIED", label: "已核验" },
  { value: "RETURNED", label: "已归还" },
  { value: "NOT_REQUIRED", label: "无需交接" },
];

function statusText(status?: string) { return statusLabels[status || ""] || status || "未知"; }
function statusClass(status?: string) { return status === "COMPLETED" ? "success" : status === "PROCESSING" ? "processing" : status === "CANCELED" ? "muted-status" : "warning"; }
function progressText(item: CaseSummary) { return `${item.materials?.completed ?? 0} / ${item.materials?.total ?? 0}`; }
function progressPercent(item: CaseSummary) { const total = item.materials?.total ?? 0; return total ? Math.round(((item.materials?.completed ?? 0) / total) * 100) : 0; }
function formatTime(value?: string) { if (!value) return "—"; return new Intl.DateTimeFormat("zh-CN", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" }).format(new Date(value)); }
function formatDateTime(value?: string) { if (!value) return "从未登录"; return new Intl.DateTimeFormat("zh-CN", { year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" }).format(new Date(value)); }
function roleText(roles?: string[]) { return (roles ?? []).map((role) => roleLabels[role] || role).join("、") || "未分配角色"; }
function jobLabel(job: ExportJobView) { return job.exportType === "MATERIAL_ZIP" ? "资料包 ZIP" : "审核表 Excel"; }
function jobFileName(job: ExportJobView) { return `${job.exportType === "MATERIAL_ZIP" ? "贸易增量资料包" : "客户审核情况"}_${job.id ?? "任务"}.${job.exportType === "MATERIAL_ZIP" ? "zip" : "xlsx"}`; }

async function loadDashboard() {
  if (dashboardLoading.value) return;
  dashboardLoading.value = true; dashboardError.value = "";
  try { dashboard.value = await getDashboard(); await loadRecentCases(); }
  catch (error) { dashboardError.value = error instanceof ApiError ? error.message : "工作台数据加载失败"; }
  finally { dashboardLoading.value = false; }
}
async function loadRecentCases() {
  try { recentCases.value = (await listAdminCases({ page: 1, pageSize: 5 })).items ?? []; }
  catch { recentCases.value = []; }
}
async function loadCases() {
  caseLoading.value = true; caseError.value = "";
  try {
    const page = await listAdminCases({ page: casePage.value.page, pageSize: casePage.value.pageSize, keyword: search.value.trim() || undefined, status: statusQuery[filter.value] });
    cases.value = page.items ?? []; casePage.value = { page: page.page ?? 1, pageSize: page.pageSize ?? 20, total: page.total ?? 0, totalPages: page.totalPages ?? 0 };
  } catch (error) { caseError.value = error instanceof ApiError ? error.message : "报单数据加载失败"; cases.value = []; }
  finally { caseLoading.value = false; }
}
async function openCase(id: number) {
  selectedCaseId.value = id; caseDetailLoading.value = true; caseError.value = "";
  try {
    caseDetail.value = await getAdminCase(id);
    if (caseDetail.value.serviceType === "TRADE_INCREMENT" && canManageCredentials.value) await loadCredentialHandover(id);
  }
  catch (error) { caseError.value = error instanceof ApiError ? error.message : "报单详情加载失败"; caseDetail.value = null; }
  finally { caseDetailLoading.value = false; }
}
function closeCase() { selectedCaseId.value = null; caseDetail.value = null; credentialHandover.value = null; credentialHandoverError.value = ""; }
async function loadCredentialHandover(caseId: number) {
  credentialHandoverLoading.value = true; credentialHandoverError.value = "";
  try { credentialHandover.value = await getCredentialHandover(caseId); }
  catch (error) { credentialHandoverError.value = error instanceof ApiError ? error.message : "线下凭证交接状态加载失败"; credentialHandover.value = null; }
  finally { credentialHandoverLoading.value = false; }
}
async function submitCredentialHandover() {
  if (!caseDetail.value?.id || !credentialHandover.value) return;
  credentialHandoverLoading.value = true; credentialHandoverError.value = "";
  try {
    credentialHandover.value = await saveCredentialHandover(caseDetail.value.id, {
      onlineBankingStatus: credentialHandover.value.onlineBankingStatus || "PENDING",
      uShieldStatus: credentialHandover.value.uShieldStatus || "PENDING",
      transactionAuthorizationStatus: credentialHandover.value.transactionAuthorizationStatus || "PENDING",
      version: credentialHandover.value.version ?? 0,
    });
    notify("线下凭证交接状态已保存");
  } catch (error) { credentialHandoverError.value = error instanceof ApiError ? error.message : "线下凭证交接状态保存失败"; notify(credentialHandoverError.value); }
  finally { credentialHandoverLoading.value = false; }
}
async function saveCaseStatus(event: Event) {
  if (!caseDetail.value) return;
  const targetStatus = (event.target as HTMLSelectElement).value;
  caseActionLoading.value = true;
  try { caseDetail.value = await updateAdminCaseStatus(caseDetail.value.id!, { targetStatus }); notify("报单状态已更新"); await loadCases(); }
  catch (error) { notify(error instanceof ApiError ? error.message : "状态更新失败"); }
  finally { caseActionLoading.value = false; }
}
async function reviewCaseMaterial(materialId: number, version: number | undefined, reviewStatus: string) {
  if (!version) { notify("资料版本号缺失，无法提交审核"); return; }
  caseActionLoading.value = true;
  try { await reviewMaterial(materialId, { reviewStatus, version }); await openCase(caseDetail.value!.id!); notify("资料审核结果已保存"); }
  catch (error) { notify(error instanceof ApiError ? error.message : "资料审核失败"); }
  finally { caseActionLoading.value = false; }
}
async function askSupplement() {
  if (!caseDetail.value) return;
  const ids = (caseDetail.value.materials ?? []).filter((item) => item.reviewStatus !== "PASSED").map((item) => item.id).filter((id): id is number => Boolean(id));
  if (!ids.length) { notify("当前没有待补资料项"); return; }
  caseActionLoading.value = true;
  try { await requestSupplement(caseDetail.value.id!, { materialIds: ids, clientMessage: "请根据资料说明补充或重新上传文件" }); await openCase(caseDetail.value.id!); notify("补件通知已发送"); }
  catch (error) { notify(error instanceof ApiError ? error.message : "补件通知发送失败"); }
  finally { caseActionLoading.value = false; }
}
async function loadCompanies() {
  companyLoading.value = true; companyError.value = "";
  try { const page = await listAdminCompanies({ page: companyPage.value.page, pageSize: companyPage.value.pageSize, keyword: companySearch.value.trim() || undefined }); companies.value = page.items ?? []; companyPage.value = { page: page.page ?? 1, pageSize: page.pageSize ?? 20, total: page.total ?? 0, totalPages: page.totalPages ?? 0 }; }
  catch (error) { companyError.value = error instanceof ApiError ? error.message : "企业数据加载失败"; companies.value = []; }
  finally { companyLoading.value = false; }
}
function emptyCompanyForm(): CompanyFormState {
  return { companyName: "", creditCode: "", contactName: "", contactPhone: "", legalPerson: "", province: "", city: "", companyEmail: "", detailAddress: "" };
}
function closeCompanyDialogs() {
  companyDialogMode.value = null; selectedCompany.value = null; companyActionError.value = ""; companyMenuId.value = null;
}
function openCompanyCreate() {
  selectedCompany.value = null; companyForm.value = emptyCompanyForm(); companyActionError.value = ""; companyDialogMode.value = "create";
}
async function fetchCompany(item: CompanyRecord) {
  if (!item.id) throw new Error("企业编号缺失");
  return await getAdminCompany(item.id) as CompanyRecord;
}
async function openCompanyDetail(item: CompanyRecord) {
  companyMenuId.value = null; companyActionLoading.value = true; companyActionError.value = "";
  try { selectedCompany.value = await fetchCompany(item); companyDialogMode.value = "view"; }
  catch (error) { notify(error instanceof ApiError ? error.message : "客户详情加载失败"); }
  finally { companyActionLoading.value = false; }
}
async function openCompanyEdit(item: CompanyRecord) {
  companyMenuId.value = null; companyActionLoading.value = true; companyActionError.value = "";
  try {
    selectedCompany.value = await fetchCompany(item);
    companyForm.value = {
      companyName: selectedCompany.value.companyName || "", creditCode: "", contactName: selectedCompany.value.contactName || "",
      contactPhone: "", legalPerson: selectedCompany.value.legalPerson || "", province: selectedCompany.value.province || "",
      city: selectedCompany.value.city || "", companyEmail: selectedCompany.value.companyEmail || "", detailAddress: selectedCompany.value.detailAddress || "",
    };
    companyDialogMode.value = "edit";
  } catch (error) { notify(error instanceof ApiError ? error.message : "客户资料加载失败"); }
  finally { companyActionLoading.value = false; }
}
function normalizeCompanyPayload(): CompanyPayload {
  const form = companyForm.value;
  return {
    companyName: form.companyName.trim(), creditCode: form.creditCode?.trim() || undefined,
    contactName: form.contactName?.trim() || undefined, contactPhone: form.contactPhone?.trim() || undefined,
    legalPerson: form.legalPerson?.trim() || undefined, province: form.province?.trim() || undefined,
    city: form.city?.trim() || undefined, companyEmail: form.companyEmail?.trim() || undefined,
    detailAddress: form.detailAddress?.trim() || undefined,
  };
}
async function submitCompany() {
  if (!companyForm.value.companyName.trim()) { companyActionError.value = "请填写企业名称"; return; }
  if (companyDialogMode.value === "create" && !companyForm.value.contactPhone?.trim()) { companyActionError.value = "请填写联系电话"; return; }
  companyActionLoading.value = true; companyActionError.value = "";
  try {
    const payload = normalizeCompanyPayload();
    if (companyDialogMode.value === "create") {
      await createAdminCompany(payload); notify("客户已创建");
    } else if (selectedCompany.value?.id !== undefined) {
      await updateAdminCompany(selectedCompany.value.id, { ...payload, version: selectedCompany.value.version ?? 0 }); notify("客户资料已更新");
    }
    closeCompanyDialogs(); await loadCompanies();
  } catch (error) { companyActionError.value = error instanceof ApiError ? error.message : "客户资料保存失败"; }
  finally { companyActionLoading.value = false; }
}
function requestCompanyDelete(item: CompanyRecord) { companyMenuId.value = null; companyDeleteTarget.value = item; companyActionError.value = ""; }
async function confirmCompanyDelete() {
  if (!companyDeleteTarget.value?.id) return;
  companyActionLoading.value = true; companyActionError.value = "";
  try {
    const latest = await fetchCompany(companyDeleteTarget.value);
    await deleteAdminCompany(latest.id!, latest.version ?? 0);
    companyDeleteTarget.value = null; notify("客户已安全停用"); await loadCompanies();
  } catch (error) { companyActionError.value = error instanceof ApiError ? error.message : "客户删除失败"; }
  finally { companyActionLoading.value = false; }
}
async function openClientAccounts(item: CompanyRecord) {
  if (!item.id) return;
  companyMenuId.value = null;
  clientAccountCompany.value = item;
  clientAccounts.value = [];
  clientAccountError.value = "";
  clientPasswordTarget.value = null;
  clientResetPassword.value = "";
  clientAccountForm.value = { displayName: item.contactName || "", username: "", phone: "", password: "", memberRole: "OWNER" };
  clientAccountLoading.value = true;
  try { clientAccounts.value = await listCompanyClientAccounts(item.id); }
  catch (error) { clientAccountError.value = error instanceof ApiError ? error.message : "客户登录账号加载失败"; }
  finally { clientAccountLoading.value = false; }
}
function closeClientAccounts() {
  clientAccountCompany.value = null;
  clientAccounts.value = [];
  clientAccountError.value = "";
  clientPasswordTarget.value = null;
  clientResetPassword.value = "";
}
async function submitClientAccount() {
  if (!clientAccountCompany.value?.id) return;
  const form = clientAccountForm.value;
  if (!form.displayName.trim() || !form.username.trim() || !form.phone.trim()) { clientAccountError.value = "请完整填写姓名、用户名和手机号"; return; }
  if (form.password.length < 8) { clientAccountError.value = "客户登录密码至少需要 8 位"; return; }
  clientAccountLoading.value = true; clientAccountError.value = "";
  try {
    await createCompanyClientAccount(clientAccountCompany.value.id, { ...form, displayName: form.displayName.trim(), username: form.username.trim(), phone: form.phone.trim() });
    clientAccounts.value = await listCompanyClientAccounts(clientAccountCompany.value.id);
    clientAccountForm.value = { displayName: "", username: "", phone: "", password: "", memberRole: "CONTACT" };
    notify("客户登录账号已创建并关联企业");
  } catch (error) { clientAccountError.value = error instanceof ApiError ? error.message : "客户账号创建失败"; }
  finally { clientAccountLoading.value = false; }
}
async function submitClientPasswordReset() {
  if (!clientAccountCompany.value?.id || !clientPasswordTarget.value?.id) return;
  if (clientResetPassword.value.length < 8) { clientAccountError.value = "新密码至少需要 8 位"; return; }
  clientAccountLoading.value = true; clientAccountError.value = "";
  try {
    await resetCompanyClientPassword(clientAccountCompany.value.id, clientPasswordTarget.value.id, clientResetPassword.value);
    clientPasswordTarget.value = null; clientResetPassword.value = ""; notify("客户登录密码已重置");
  } catch (error) { clientAccountError.value = error instanceof ApiError ? error.message : "密码重置失败"; }
  finally { clientAccountLoading.value = false; }
}
async function downloadImportTemplate() {
  try { const result = unwrapResponse<TemplateDownload>(await importTemplate()); if (result.downloadUrl) window.open(result.downloadUrl, "_blank", "noopener"); }
  catch (error) { notify(error instanceof ApiError ? error.message : "导入模板获取失败"); }
}
function chooseImportFile() { importInput.value?.click(); }
async function importCompanies(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]; if (!file) return;
  importLoading.value = true;
  try { const job = unwrapResponse<ImportJobView>(await importCreate({ file }, undefined, { headers: { "Idempotency-Key": `company-import-${file.name}-${file.lastModified}` } })); notify(`导入任务已创建：${job.jobNo ?? job.id}`); await loadCompanies(); }
  catch (error) { notify(error instanceof ApiError ? error.message : "Excel 导入失败"); }
  finally { importLoading.value = false; (event.target as HTMLInputElement).value = ""; }
}
async function loadTeam() {
  teamLoading.value = true; teamError.value = "";
  try { teamAccounts.value = await listAdminAccounts(); }
  catch (error) { teamError.value = error instanceof ApiError ? error.message : "团队成员加载失败"; teamAccounts.value = []; }
  finally { teamLoading.value = false; }
}
function openCreateMember() {
  memberForm.value = { displayName: "", username: "", phone: "", password: "", role: "OPERATIONS" };
  memberActionError.value = ""; createMemberVisible.value = true;
}
async function submitMember() {
  const form = memberForm.value;
  if (!form.displayName.trim() || !form.username.trim() || !form.phone.trim()) { memberActionError.value = "请完整填写姓名、用户名和手机号"; return; }
  if (form.password.length < 12) { memberActionError.value = "登录密码至少需要 12 位"; return; }
  memberActionLoading.value = true; memberActionError.value = "";
  try {
    await createAdminAccount({ accountType: "ADMIN", displayName: form.displayName.trim(), username: form.username.trim(), phone: form.phone.trim(), password: form.password, roles: [form.role] });
    createMemberVisible.value = false; notify("团队成员已创建"); await loadTeam();
  } catch (error) { memberActionError.value = error instanceof ApiError ? error.message : "团队成员创建失败"; }
  finally { memberActionLoading.value = false; }
}
function openRoleEditor(account: AccountView) {
  if (account.id === auth.user?.id) { notify("为防止失去管理权限，不能修改当前登录账号的角色"); return; }
  selectedRole.value = account.roles?.[0] || "OPERATIONS"; memberActionError.value = ""; roleEditorAccount.value = account;
}
async function saveAccountRole() {
  if (!roleEditorAccount.value?.id) return;
  memberActionLoading.value = true; memberActionError.value = "";
  try {
    await updateAdminAccountRoles(roleEditorAccount.value.id, [selectedRole.value]);
    roleEditorAccount.value = null; notify("成员角色已更新"); await loadTeam();
  } catch (error) { memberActionError.value = error instanceof ApiError ? error.message : "成员角色更新失败"; }
  finally { memberActionLoading.value = false; }
}
async function loadExports() {
    const ids = JSON.parse(sessionStorage.getItem("wqst_export_job_ids") || "[]") as number[];
  if (!ids.length) { exportJobs.value = []; return; }
  const jobs = await Promise.all(ids.slice(-20).reverse().map(async (id) => { try { return await getExportJob(id); } catch { return null; } }));
  exportJobs.value = jobs.filter((job): job is ExportJobView => Boolean(job));
}
function rememberExportJob(job: ExportJobView) { const ids = JSON.parse(sessionStorage.getItem("wqst_export_job_ids") || "[]") as number[]; const next = [job.id, ...ids.filter((id) => id !== job.id)].filter((id): id is number => Boolean(id)).slice(0, 20); sessionStorage.setItem("wqst_export_job_ids", JSON.stringify(next)); }
async function createExcelExport(caseIds: number[]) {
  exportLoading.value = true; exportError.value = "";
  try { const job = await createReviewExcel(caseIds, `review-excel-${caseIds.join("-")}-${Date.now()}`); rememberExportJob(job); await loadExports(); notify("审核表 Excel 任务已创建"); }
  catch (error) { exportError.value = error instanceof ApiError ? error.message : "Excel 导出失败"; notify(exportError.value); }
  finally { exportLoading.value = false; }
}
async function createZipExport(caseId: number) {
  exportLoading.value = true; exportError.value = "";
  try { const job = await createMaterialZip(caseId); rememberExportJob(job); await loadExports(); notify("资料 ZIP 任务已创建"); }
  catch (error) { exportError.value = error instanceof ApiError ? error.message : "ZIP 导出失败"; notify(exportError.value); }
  finally { exportLoading.value = false; }
}
async function downloadExport(job: ExportJobView) {
  try { const result = await getExportDownload(job.id!); if (result.downloadUrl) window.open(result.downloadUrl, "_blank", "noopener"); notify("下载链接已打开，有效期 30 分钟"); await loadExports(); }
  catch (error) { notify(error instanceof ApiError ? error.message : "下载链接获取失败"); }
}
async function loadCatalog() {
  catalogLoading.value = true;
  try { [catalog.value, products.value] = await Promise.all([getServiceCatalog(), listAdminProducts()]); }
  catch (error) { notify(error instanceof ApiError ? error.message : "服务模板加载失败"); }
  finally { catalogLoading.value = false; }
}
async function showTemplate(productId: number) {
  if (templateLoadingProductId.value !== null) return;
  templateLoadingProductId.value = productId;
  try {
    selectedTemplate.value = await getProductTemplate(productId);
    selectedProductId.value = productId;
    await nextTick();
    templateDetailRef.value?.scrollIntoView({ behavior: "smooth", block: "nearest" });
    templateDetailRef.value?.focus({ preventScroll: true });
  }
  catch (error) { notify(error instanceof ApiError ? error.message : "资料模板加载失败"); }
  finally { templateLoadingProductId.value = null; }
}
function closeTemplate() { selectedTemplate.value = null; selectedProductId.value = null; }
function nav(view: ViewKey) { closeCase(); const path = view === "customers" ? "/companies" : `/${view}`; void router.push(path); }
function notify(message: string) { toast.value = message; setTimeout(() => (toast.value = ""), 2600); }
async function logout() { try { await auth.logout(); } finally { await router.replace("/login"); } }
async function openWorkspace() {
  if (current.value === "dashboard") {
    await loadDashboard();
    return;
  }
  nav("dashboard");
}
function requestLogout() { logoutConfirmVisible.value = true; }
async function confirmLogout() {
  logoutConfirmVisible.value = false;
  await logout();
}

watch(() => route.name, (name) => {
  closeCase();
  if (name === "dashboard") void loadDashboard();
  if (name === "cases") void loadCases();
  if (name === "companies") void loadCompanies();
  if (name === "exports") void loadExports();
  if (name === "products") void loadCatalog();
  if (name === "team") void loadTeam();
});
watch([search, filter], () => { if (current.value === "cases") { casePage.value.page = 1; void loadCases(); } });
watch(companySearch, () => { if (current.value === "customers") { companyPage.value.page = 1; void loadCompanies(); } });
onMounted(() => { if (current.value === "dashboard") void loadDashboard(); else if (current.value === "cases") void loadCases(); else if (current.value === "customers") void loadCompanies(); else if (current.value === "exports") void loadExports(); else if (current.value === "products") void loadCatalog(); else if (current.value === "team") void loadTeam(); });
</script>

<template>
  <div class="admin-shell">
    <aside class="sidebar">
      <div class="brand-block">
        <div class="brand-symbol">WQ</div>
        <div><strong>万企商服通</strong><span>BUSINESS SERVICE</span></div>
      </div>
      <button
        type="button"
        class="workspace-pill"
        :class="{ active: current === 'dashboard' }"
        title="返回运营工作台"
        @click="openWorkspace"
      >
        <div class="mini-avatar">{{ avatarText }}</div>
        <div><b>运营工作台</b><small>企业服务中心</small></div>
        <AdminIcon name="arrow-right" />
      </button>
      <nav>
        <button
          :class="{ active: current === 'dashboard' }"
          @click="nav('dashboard')"
        >
          <i><AdminIcon name="dashboard" /></i>工作台
        </button>
        <button :class="{ active: current === 'cases' }" @click="nav('cases')">
          <i><AdminIcon name="cases" /></i>报单管理
          <em>{{
            dashboardStats.pendingReview + dashboardStats.supplement
          }}</em>
        </button>
        <button
          :class="{ active: current === 'customers' }"
          @click="nav('customers')"
        >
          <i><AdminIcon name="company" /></i>客户资料
        </button>
        <button
          :class="{ active: current === 'exports' }"
          @click="nav('exports')"
        >
          <i><AdminIcon name="download" /></i>导出中心
        </button>
        <div class="nav-divider" />
        <label>系统设置</label>
        <button
          :class="{ active: current === 'products' }"
          @click="nav('products')"
        ><i><AdminIcon name="templates" /></i>业务模板</button>
        <button
          v-if="auth.hasRole('SUPER_ADMIN')"
          :class="{ active: current === 'team' }"
          @click="nav('team')"
        ><i><AdminIcon name="users" /></i>团队与权限</button>
      </nav>
      <div class="sidebar-foot">
        <div class="secure-dot" />
        敏感资料已加密保护
      </div>
    </aside>

    <main class="main-area">
      <header class="top-header">
        <div class="breadcrumb">
          管理后台 <span>/</span>
          {{
            current === "dashboard"
              ? "工作台"
              : current === "cases"
                ? "报单管理"
              : current === "customers"
                ? "客户资料"
                : current === "exports"
                  ? "导出中心"
                  : current === "products"
                    ? "业务模板"
                    : "团队与权限"
          }}
        </div>
        <div class="top-actions">
          <button
            class="icon-btn"
            type="button"
            aria-label="通知中心"
            title="通知中心暂未接入"
            @click="notify('通知中心将在后续阶段接入')"
          >
            <AdminIcon name="bell" />
          </button>
          <div class="user-identity" title="当前登录账号">
            <div class="mini-avatar">{{ avatarText }}</div>
            <span>{{ displayName }}</span>
          </div>
          <button
            class="logout-button"
            type="button"
            title="退出登录"
            @click="requestLogout"
          >
            <AdminIcon name="logout" />
            <span>退出登录</span>
          </button>
        </div>
      </header>

      <section v-if="current === 'dashboard'" class="content dashboard-view">
        <div class="page-intro">
          <div>
            <div class="kicker">{{ currentDate }}</div>
            <h1>你好，{{ displayName }} <span>👋</span></h1>
            <p>以下数据来自当前本地后端与开发数据库。</p>
          </div>
          <button class="primary" @click="loadDashboard">
            {{ dashboardLoading ? "正在刷新…" : "刷新数据" }} <AdminIcon name="refresh" />
          </button>
        </div>
        <div v-if="dashboardError" class="dashboard-feedback error-state">
          <span>{{ dashboardError }}</span
          ><button @click="loadDashboard">重新加载</button>
        </div>
        <div
          v-else-if="dashboardLoading && !dashboard"
          class="dashboard-feedback"
        >
          正在读取工作台数据…
        </div>
        <div class="metric-grid">
          <div class="metric-card primary-metric">
            <div class="metric-top"><span>待审核报单</span><i><AdminIcon name="check-circle" /></i></div>
            <strong>{{ dashboardStats.pendingReview }}</strong>
            <div class="metric-foot">
              <span>真实状态汇总</span><small>需要优先处理</small>
            </div>
          </div>
          <div class="metric-card">
            <div class="metric-top">
              <span>处理中报单</span><i class="green-icon"><AdminIcon name="processing" /></i>
            </div>
            <strong>{{ dashboardStats.processing }}</strong>
            <div class="metric-foot">
              <span>已完成</span
              ><b class="green-text">{{ dashboardStats.completed }}</b
              ><small>全部历史数据</small>
            </div>
          </div>
          <div class="metric-card">
            <div class="metric-top">
              <span>待补资料</span><i class="orange-icon"><AdminIcon name="alert" /></i>
            </div>
            <strong>{{ dashboardStats.supplement }}</strong>
            <div class="metric-foot">
              <span>当前状态</span><small>请及时提醒客户</small>
            </div>
          </div>
          <div class="metric-card">
            <div class="metric-top">
              <span>启用企业客户</span><i class="blue-icon"><AdminIcon name="building" /></i>
            </div>
            <strong>{{ dashboardStats.enabledCompanies }}</strong>
            <div class="metric-foot">
              <span>企业状态</span><small>ENABLED</small>
            </div>
          </div>
        </div>
        <div class="dashboard-grid">
          <div class="panel queue-panel">
            <div class="panel-head">
              <div>
                <h2>异步任务队列</h2>
                <p>Excel 导入与文件导出的实时待处理数量</p>
              </div>
              <span class="live-label">● 实时接口</span>
            </div>
            <div class="queue-list">
              <div class="queue-item">
                <div class="attention-icon blue"><AdminIcon name="upload" /></div>
                <div>
                  <b>客户 Excel 导入</b><span>等待或正在解析的任务</span>
                </div>
                <strong>{{ dashboardStats.pendingImports }}</strong>
              </div>
              <div class="queue-item">
                <div class="attention-icon amber"><AdminIcon name="download" /></div>
                <div>
                  <b>Excel / ZIP 导出</b><span>等待或正在生成的任务</span>
                </div>
                <strong>{{ dashboardStats.pendingExports }}</strong>
              </div>
            </div>
          </div>
          <div class="panel pending-panel">
            <div class="panel-head">
              <div>
                <h2>报单状态分布</h2>
                <p>当前需要关注的三个状态</p>
              </div>
              <button class="text-btn" @click="nav('cases')">报单页面 ›</button>
            </div>
            <div class="attention-item">
              <div class="attention-icon amber"><AdminIcon name="alert" /></div>
              <div><b>待补资料</b><span>等待客户重新提交</span></div>
              <strong>{{ dashboardStats.supplement }}</strong>
            </div>
            <div class="attention-item">
              <div class="attention-icon rose"><AdminIcon name="search" /></div>
              <div><b>待审核</b><span>等待运营审核资料</span></div>
              <strong>{{ dashboardStats.pendingReview }}</strong>
            </div>
            <div class="attention-item">
              <div class="attention-icon blue"><AdminIcon name="processing" /></div>
              <div><b>处理中</b><span>已经进入业务办理</span></div>
              <strong>{{ dashboardStats.processing }}</strong>
            </div>
          </div>
        </div>
        <div class="panel table-panel upcoming-panel">
          <div class="panel-head">
            <div>
              <h2>最近报单</h2>
              <p>来自后台报单接口的最新 5 条记录。</p>
            </div>
            <button class="text-btn" @click="nav('cases')">
              查看全部 ›
            </button>
          </div>
          <div v-if="recentCases.length" class="recent-case-list">
            <div v-for="item in recentCases" :key="item.id" class="recent-case-row">
              <span class="company-logo">{{ (item.companyName || "企业").slice(0, 1) }}</span>
              <div class="recent-case-main"><b>{{ item.companyName || "未命名企业" }}</b><small>{{ item.caseNo }} · {{ item.serviceType || "—" }}</small></div>
              <span class="status" :class="statusClass(item.status)">{{ statusText(item.status) }}</span>
              <span class="muted">{{ formatTime(item.updatedAt || item.createdAt) }}</span>
            </div>
          </div>
          <div v-else class="empty-live-data">
            <span><AdminIcon name="cases" /></span><b>暂无真实报单</b>
            <p>客户提交报单后，最近记录会自动显示在这里。</p>
          </div>
        </div>
      </section>

      <section v-else-if="current === 'cases'" class="content cases-view">
        <div v-if="caseError" class="demo-data-notice error-state">{{ caseError }}</div>
        <div class="page-title-row">
          <div>
            <div class="kicker">OPERATIONS</div>
            <h1>报单管理</h1>
            <p>查看和处理客户提交的业务报单。</p>
          </div>
          <button class="primary" @click="loadCases">{{ caseLoading ? "正在刷新…" : "刷新报单" }}</button>
        </div>
        <div v-if="selectedCaseId === null" class="panel table-panel case-list-panel">
          <div class="toolbar">
            <div class="search">
              <span>⌕</span
              ><input v-model="search" placeholder="搜索企业名称或报单编号" />
            </div>
            <select v-model="filter">
              <option>全部状态</option>
              <option>待审核</option>
              <option>待补资料</option>
              <option>处理中</option>
              <option>已完成</option></select
            ><button class="outline" @click="search = ''; filter = '全部状态'; loadCases()">
              重置</button
            ><button class="outline" @click="createExcelExport(cases.map((item) => item.id).filter((id): id is number => Boolean(id)))">
              导出 Excel
            </button>
          </div>
          <table>
            <thead>
              <tr>
                <th>报单编号</th>
                <th>客户企业</th>
                <th>服务项目</th>
                <th>资料进度</th>
                <th>提交时间</th>
                <th>状态</th>
                <th>负责人</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in filteredCases" :key="item.id">
                <td class="mono">{{ item.caseNo || "—" }}</td>
                <td>
                  <div class="company-cell">
                    <span class="company-logo">{{ (item.companyName || "企").slice(0, 1) }}</span>
                    <div>
                      <b>{{ item.companyName || "未命名企业" }}</b
                      ><small>{{ item.caseMonth || "—" }}</small>
                    </div>
                  </div>
                </td>
                <td>
                  <span class="service-tag">{{ item.serviceType || "—" }}</span>
                </td>
                <td>
                  <div class="progress-cell">
                    <div class="progress">
                      <i :style="{ width: `${progressPercent(item)}%` }" />
                    </div>
                    <span>{{ progressText(item) }}</span>
                  </div>
                </td>
                <td class="muted">{{ formatTime(item.createdAt) }}</td>
                <td>
                  <span class="status" :class="statusClass(item.status)">{{ statusText(item.status) }}</span>
                </td>
                <td>{{ item.id ? `#${item.id}` : "—" }}</td>
                <td>
                  <button class="link-btn" @click="openCase(item.id!)">
                    查看详情
                  </button>
                </td>
              </tr>
              <tr v-if="!caseLoading && !filteredCases.length"><td colspan="8" class="empty-cell">暂无符合条件的真实报单</td></tr>
            </tbody>
          </table>
          <div class="pagination">
            <span>共 {{ casePage.total }} 条报单</span><button :disabled="casePage.page <= 1" @click="casePage.page--; loadCases()">‹</button
            ><button class="current-page">{{ casePage.page }}</button><button :disabled="casePage.page >= casePage.totalPages" @click="casePage.page++; loadCases()">›</button>
          </div>
        </div>
        <div v-else class="case-detail">
          <div class="detail-toolbar">
            <button class="back-btn" @click="closeCase">
              ← 返回报单列表
            </button>
            <div>
              <button class="outline" @click="createExcelExport(caseDetail?.id ? [caseDetail.id] : [])">
                导出审核表</button
              ><button v-if="caseDetail?.serviceType === 'TRADE_INCREMENT'" class="primary small" @click="createZipExport(caseDetail.id!)">
                下载资料 ZIP
              </button>
            </div>
          </div>
          <div v-if="caseDetailLoading" class="dashboard-feedback">正在读取报单详情…</div>
          <template v-else-if="caseDetail">
          <div class="detail-heading">
            <div>
              <div class="kicker">{{ caseDetail.caseNo }}</div>
              <h1>{{ caseDetail.companyName || "未命名企业" }}</h1>
              <p>{{ caseDetail.serviceType || "—" }} · {{ caseDetail.caseMonth || "—" }} · 提交于 {{ formatTime(caseDetail.submittedAt || caseDetail.createdAt) }}</p>
            </div>
            <span class="status large-status" :class="statusClass(caseDetail.status)">{{ selectedCaseStatusLabel }}</span>
          </div>
          <div class="detail-layout">
            <div class="panel material-panel">
              <div class="panel-head">
                <div>
                  <h2>资料审核</h2>
                  <p>共 {{ caseProgress.total }} 项资料，已完成 {{ caseProgress.completed }} 项</p>
                </div>
                <div class="big-progress">
                  <b>{{ caseProgressPercent }}%</b>
                  <div class="progress"><i :style="{ width: `${caseProgressPercent}%` }" /></div>
                </div>
              </div>
              <div class="material-row" v-for="item in caseDetail.materials || []" :key="item.id">
                <span class="material-no">{{ String(item.id || 0).padStart(2, "0") }}</span>
                <div class="material-name">
                  <b>{{ item.materialName || item.materialCode }}</b
                  ><small>{{ item.clientVisibleNote || (item.textValue ? "已填写文字或链接" : item.submitStatus === "SUBMITTED" ? "客户已提交" : "客户暂未提交") }}</small>
                </div>
                <span class="status" :class="item.reviewStatus === 'PASSED' ? 'success' : item.reviewStatus === 'NEED_SUPPLEMENT' ? 'warning' : 'processing'">{{ item.reviewStatus === 'PASSED' ? "已通过" : item.reviewStatus === 'NEED_SUPPLEMENT' ? "待补充" : "待审核" }}</span>
                <button v-if="item.id && item.reviewStatus !== 'PASSED'" class="link-btn" :disabled="caseActionLoading" @click="reviewCaseMaterial(item.id, item.version, 'PASSED')">通过</button>
                <button v-else-if="item.id" class="link-btn" :disabled="caseActionLoading" @click="reviewCaseMaterial(item.id, item.version, 'NEED_SUPPLEMENT')">补件</button>
              </div>
              <div v-if="!caseDetail.materials?.length" class="empty-cell">该报单暂无资料项</div>
            </div>
            <div class="detail-side">
              <div v-if="caseDetail.serviceType === 'TRADE_INCREMENT' && canManageCredentials" class="panel credential-panel">
                <div class="credential-panel-head">
                  <div>
                    <h2>线下凭证交接</h2>
                    <p>仅登记状态，不录入任何密码或验证码。</p>
                  </div>
                  <span class="credential-safety-badge">后台专用</span>
                </div>
                <div v-if="credentialHandoverError" class="credential-error">{{ credentialHandoverError }}</div>
                <template v-if="credentialHandover">
                  <label>网银凭证</label>
                  <select v-model="credentialHandover.onlineBankingStatus" :disabled="credentialHandoverLoading">
                    <option v-for="option in credentialStatusOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
                  </select>
                  <label>U 盾</label>
                  <select v-model="credentialHandover.uShieldStatus" :disabled="credentialHandoverLoading">
                    <option v-for="option in credentialStatusOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
                  </select>
                  <label>交易授权凭证</label>
                  <select v-model="credentialHandover.transactionAuthorizationStatus" :disabled="credentialHandoverLoading">
                    <option v-for="option in credentialStatusOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
                  </select>
                  <div class="credential-meta">
                    <span>最近经办：{{ credentialHandover.handledBy ? `管理员 #${credentialHandover.handledBy}` : "尚未登记" }}</span>
                    <span>登记时间：{{ formatTime(credentialHandover.handledAt) }}</span>
                  </div>
                  <button class="primary full" :disabled="credentialHandoverLoading" @click="submitCredentialHandover">
                    {{ credentialHandoverLoading ? "正在保存…" : "保存交接状态" }}
                  </button>
                </template>
                <div v-else-if="credentialHandoverLoading" class="credential-loading">正在读取交接状态…</div>
              </div>
              <div class="panel info-panel">
                <h2>处理操作</h2>
                <label>更新状态</label
                ><select :value="caseDetail.status" @change="saveCaseStatus">
                  <option value="PENDING_REVIEW">待审核</option>
                  <option value="NEED_SUPPLEMENT">待补资料</option>
                  <option value="PROCESSING">处理中</option>
                  <option value="COMPLETED">已完成</option></select
                ><label>补件说明</label
                ><textarea value="请根据资料说明补充或重新上传文件" readonly></textarea
                ><button class="primary full" :disabled="caseActionLoading" @click="askSupplement">
                  {{ caseActionLoading ? "正在保存…" : "发起补件通知" }}
                </button>
              </div>
              <div class="panel security-panel">
                <span class="lock">⌕</span>
                <div>
                  <b>敏感资料受控</b>
                  <p>身份证、征信和网银截图受控查看；网银密码、U 盾密码、交易密码不进入系统。</p>
                </div>
              </div>
            </div>
          </div>
          </template>
        </div>
      </section>

      <section
        v-else-if="current === 'customers'"
        class="content customers-view"
      >
        <div v-if="companyError" class="demo-data-notice error-state">{{ companyError }}</div>
        <div class="page-title-row">
          <div>
            <div class="kicker">CUSTOMERS</div>
            <h1>客户资料</h1>
            <p>企业资料、审核状态和导出记录。</p>
          </div>
          <div class="page-actions">
            <button class="outline" @click="downloadImportTemplate">下载导入模板</button>
            <button class="primary" @click="openCompanyCreate"><AdminIcon name="plus" />新增客户</button>
          </div>
        </div>
        <div class="customer-cards">
          <div class="customer-stat">
            <span>企业客户</span><b>{{ companyPage.total }}</b><small>真实接口总数</small>
          </div>
          <div class="customer-stat">
            <span>当前页</span><b>{{ companies.length }}</b
            ><small class="green-text">已加载记录</small>
          </div>
          <div class="customer-stat">
            <span>导入状态</span><b>{{ importLoading ? "…" : "就绪" }}</b
            ><small class="orange-text">支持 .xlsx</small>
          </div>
        </div>
        <div class="panel import-panel">
          <div class="import-icon">⇧</div>
          <div>
            <h2>批量导入客户</h2>
            <p>
              支持 .xlsx
              文件，按模板填写后上传，系统会自动校验手机号和企业名称。
            </p>
          </div>
          <input ref="importInput" type="file" accept=".xlsx,.xls" hidden @change="importCompanies" />
          <button class="outline" @click="chooseImportFile">
            {{ importLoading ? "正在导入…" : "选择 Excel 文件" }}
          </button>
        </div>
        <div class="panel table-panel">
          <div class="panel-head">
            <div>
              <h2>最近客户</h2>
              <p>已导入的企业客户信息</p>
            </div>
            <div class="search mini">
              <span>⌕</span><input v-model="companySearch" placeholder="搜索客户" />
            </div>
          </div>
          <table>
            <thead>
              <tr>
                <th>企业名称</th>
                <th>联系人</th>
                <th>联系电话</th>
                <th>企业编号</th>
                <th>资料状态</th>
                <th>所在地区</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in companies" :key="item.id">
                <td>
                  <div class="company-cell">
                    <span class="company-logo">{{
                      (item.companyName || "企").slice(0, 1)
                    }}</span
                    ><b>{{ item.companyName || "未命名企业" }}</b>
                  </div>
                </td>
                <td>{{ item.contactName || "—" }}</td>
                <td class="mono">{{ item.contactPhoneMask || "—" }}</td>
                <td class="mono">{{ item.id ? `企业 #${item.id}` : "—" }}</td>
                <td><span class="status" :class="item.status === 'ENABLED' ? 'success' : 'warning'">{{ item.status === 'ENABLED' ? "已启用" : item.status || "未知" }}</span></td>
                <td class="muted">{{ item.city || item.province || "—" }}</td>
                <td class="company-action-cell">
                  <div class="company-action-anchor">
                    <button
                      class="more-action"
                      type="button"
                      aria-label="客户操作"
                      :aria-expanded="companyMenuId === item.id"
                      @click.stop="companyMenuId = companyMenuId === item.id ? null : (item.id ?? null)"
                    ><AdminIcon name="more" /></button>
                    <div v-if="companyMenuId === item.id" class="action-menu" role="menu">
                      <button type="button" role="menuitem" @click.stop="openCompanyDetail(item)">查看详情</button>
                      <button type="button" role="menuitem" @click.stop="openCompanyEdit(item)">编辑资料</button>
                      <button type="button" role="menuitem" @click.stop="openClientAccounts(item)">登录账号</button>
                      <button v-if="canDeleteCompanies" type="button" role="menuitem" class="danger-item" @click.stop="requestCompanyDelete(item)">删除客户</button>
                    </div>
                  </div>
                </td>
              </tr>
              <tr v-if="!companyLoading && !companies.length"><td colspan="7" class="empty-cell">暂无企业客户数据</td></tr>
            </tbody>
          </table>
          <div class="pagination"><span>共 {{ companyPage.total }} 家企业</span><button :disabled="companyPage.page <= 1" @click="companyPage.page--; loadCompanies()">‹</button><button class="current-page">{{ companyPage.page }}</button><button :disabled="companyPage.page >= companyPage.totalPages" @click="companyPage.page++; loadCompanies()">›</button></div>
        </div>
      </section>

      <section v-else-if="current === 'team'" class="content team-view">
        <div v-if="teamError" class="demo-data-notice error-state">{{ teamError }}</div>
        <div class="page-title-row">
          <div>
            <div class="kicker">TEAM & ACCESS</div>
            <h1>团队与权限</h1>
            <p>按岗位分配角色，权限由后端统一校验。</p>
          </div>
          <button class="primary" type="button" @click="openCreateMember"><AdminIcon name="plus" />新增成员</button>
        </div>
        <div class="team-stat-grid">
          <div class="team-stat"><span>后台成员</span><b>{{ teamStats.total }}</b><small>已创建管理员账号</small></div>
          <div class="team-stat"><span>运营人员</span><b>{{ teamStats.operations }}</b><small>客户与报单流转</small></div>
          <div class="team-stat"><span>审核人员</span><b>{{ teamStats.reviewers }}</b><small>资料审核与补件</small></div>
          <div class="team-stat"><span>超级管理员</span><b>{{ teamStats.admins }}</b><small>团队与系统管理</small></div>
        </div>
        <div class="role-grid">
          <article v-for="role in roleOptions" :key="role.value" class="panel role-card">
            <div class="role-card-icon"><AdminIcon name="users" /></div>
            <div><span>{{ role.value }}</span><h2>{{ role.label }}</h2><p>{{ role.description }}</p></div>
          </article>
        </div>
        <div class="panel table-panel team-table">
          <div class="panel-head">
            <div><h2>团队成员</h2><p>角色调整后立即按新的后端权限生效。</p></div>
            <div class="search mini"><AdminIcon name="search" /><input v-model="teamSearch" placeholder="搜索姓名或账号" /></div>
          </div>
          <table>
            <thead><tr><th>成员</th><th>登录账号</th><th>联系方式</th><th>角色</th><th>权限</th><th>最近登录</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="account in visibleTeamAccounts" :key="account.id">
                <td><div class="member-cell"><span>{{ (account.displayName || "管").slice(0, 1) }}</span><div><b>{{ account.displayName || "未命名成员" }}</b><small>{{ account.status === "ENABLED" ? "账号正常" : account.status || "未知状态" }}</small></div></div></td>
                <td class="mono">{{ account.username || "—" }}</td>
                <td class="mono">{{ account.phoneMask || "—" }}</td>
                <td><span class="role-badge" :class="account.roles?.includes('SUPER_ADMIN') ? 'admin' : ''">{{ roleText(account.roles) }}</span></td>
                <td><button class="link-btn" type="button" @click="permissionAccount = account">{{ account.permissions?.length || 0 }} 项权限</button></td>
                <td class="muted">{{ formatDateTime(account.lastLoginAt) }}</td>
                <td><button class="outline compact" type="button" :disabled="account.id === auth.user?.id" @click="openRoleEditor(account)">{{ account.id === auth.user?.id ? "当前账号" : "调整角色" }}</button></td>
              </tr>
              <tr v-if="!teamLoading && !visibleTeamAccounts.length"><td colspan="7" class="empty-cell">暂无符合条件的后台成员</td></tr>
              <tr v-if="teamLoading"><td colspan="7" class="empty-cell">正在加载团队成员…</td></tr>
            </tbody>
          </table>
        </div>
      </section>

      <section v-else-if="current === 'exports'" class="content exports-view">
        <div v-if="exportError" class="demo-data-notice error-state">{{ exportError }}</div>
        <div class="page-title-row">
          <div>
            <div class="kicker">EXPORT CENTER</div>
            <h1>导出中心</h1>
            <p>管理审核表 Excel 和客户资料 ZIP 导出任务。</p>
          </div>
          <button class="primary" @click="nav('cases')">去报单选择客户</button>
        </div>
        <div class="export-grid">
          <div class="export-type teal">
            <div class="export-icon">▤</div>
            <h2>审核表 Excel</h2>
            <p>企业信息、资料明细、审核状态、备注。</p>
            <b @click="nav('cases')">先选择报单 →</b>
          </div>
          <div class="export-type gold">
            <div class="export-icon">⇩</div>
            <h2>资料包 ZIP</h2>
            <p>原始图片、PDF、视频和开票信息文件。</p>
            <b @click="nav('cases')">先选择贸易增量报单 →</b>
          </div>
        </div>
        <div class="panel table-panel">
          <div class="panel-head">
            <div>
              <h2>导出记录</h2>
              <p>所有导出操作均会记录操作人和下载次数。</p>
            </div>
            <span class="security-label">⌕ 已启用操作留痕</span>
          </div>
          <table>
            <thead>
              <tr>
                <th>文件名称</th>
                <th>类型</th>
                <th>创建时间</th>
                <th>操作人</th>
                <th>下载次数</th>
                <th>状态</th>
                <th />
              </tr>
            </thead>
            <tbody>
              <tr v-for="job in exportJobs" :key="job.id">
                <td class="mono">{{ jobFileName(job) }}</td>
                <td><span class="file-tag" :class="job.exportType === 'MATERIAL_ZIP' ? 'zip' : 'excel'">{{ job.exportType === 'MATERIAL_ZIP' ? 'ZIP' : 'XLSX' }}</span></td>
                <td class="muted">{{ formatTime(job.createdAt) }}</td>
                <td>{{ displayName }}</td>
                <td>{{ job.downloadCount ?? 0 }} 次</td>
                <td><span class="status" :class="job.status === 'SUCCEEDED' ? 'success' : job.status === 'FAILED' ? 'warning' : 'processing'">{{ job.status === 'SUCCEEDED' ? '可下载' : job.status === 'FAILED' ? '失败' : '生成中' }}</span></td>
                <td><button v-if="job.status === 'SUCCEEDED'" class="link-btn" :disabled="exportLoading" @click="downloadExport(job)">下载</button><button v-else class="link-btn" @click="loadExports">刷新</button></td>
              </tr>
              <tr v-if="!exportJobs.length"><td colspan="7" class="empty-cell">暂无导出任务。请从报单详情创建 Excel 或 ZIP 导出。</td></tr>
            </tbody>
          </table>
        </div>
      </section>

      <section v-else class="content products-view">
        <div class="page-title-row">
          <div><div class="kicker">SERVICE CATALOG</div><h1>业务模板</h1><p>服务目录和真实资料模板均来自后端数据库。</p></div>
          <button class="primary" @click="loadCatalog">{{ catalogLoading ? "正在刷新…" : "刷新模板" }}</button>
        </div>
        <div class="catalog-grid">
          <div v-for="module in catalog" :key="module.moduleCode" class="panel catalog-module">
            <div class="panel-head"><div><h2>{{ module.moduleName }}</h2><p>{{ module.description }}</p></div><span class="live-label">● 已发布</span></div>
            <button
              v-for="product in module.products || []"
              :key="product.id"
              type="button"
              class="catalog-product"
              :class="{ active: selectedProductId === product.id }"
              :disabled="!product.id || templateLoadingProductId !== null"
              :aria-pressed="selectedProductId === product.id"
              @click="showTemplate(product.id!)"
            >
              <span class="catalog-product-copy"><b>{{ product.productName }}</b><small>{{ product.description }} · {{ product.materialCount }} 项资料</small></span>
              <span class="catalog-product-action">
                {{ templateLoadingProductId === product.id ? "正在读取…" : "查看模板" }}
                <AdminIcon v-if="templateLoadingProductId !== product.id" name="arrow-right" />
              </span>
            </button>
          </div>
          <div v-if="!catalog.length && !catalogLoading" class="panel empty-live-data"><span>◈</span><b>暂无已发布服务</b><p>请先在数据库发布产品和资料模板。</p></div>
        </div>
        <div v-if="selectedTemplate" ref="templateDetailRef" class="panel template-detail" tabindex="-1" aria-live="polite">
          <div class="panel-head"><div><span class="template-eyebrow">MATERIAL TEMPLATE</span><h2>{{ selectedTemplate.templateName }}</h2><p>版本 {{ selectedTemplate.versionNo }} · {{ selectedTemplate.items?.length || 0 }} 项资料</p></div><button class="template-close" type="button" @click="closeTemplate">关闭</button></div>
          <div class="template-items">
            <div v-for="item in selectedTemplate.items || []" :key="item.id" class="template-item">
              <span class="template-item-index">{{ String(item.sortOrder || 0).padStart(2, "0") }}</span>
              <span class="template-item-copy"><b>{{ item.itemName }}</b><small>{{ item.inputType }} · {{ item.required ? '必填' : '选填' }}{{ item.sensitive ? ' · 敏感资料' : '' }}</small></span>
            </div>
          </div>
        </div>
        <div class="panel table-panel"><div class="panel-head"><div><h2>后台产品记录</h2><p>用于核对服务目录与管理端产品配置是否一致。</p></div></div><table><thead><tr><th>产品编码</th><th>产品名称</th><th>模板编号</th><th>状态</th></tr></thead><tbody><tr v-for="product in products" :key="product.id"><td class="mono">{{ product.productCode }}</td><td>{{ product.productName }}</td><td>{{ product.templateId }}</td><td><span class="status" :class="product.status === 'PUBLISHED' ? 'success' : 'warning'">{{ product.status }}</span></td></tr><tr v-if="!products.length"><td colspan="4" class="empty-cell">暂无产品配置</td></tr></tbody></table></div>
      </section>
      <div v-if="companyDialogMode === 'create' || companyDialogMode === 'edit'" class="dialog-backdrop" @click.self="closeCompanyDialogs">
        <form class="form-dialog" @submit.prevent="submitCompany">
          <div class="form-dialog-head">
            <div><span>{{ companyDialogMode === "create" ? "NEW CUSTOMER" : "EDIT CUSTOMER" }}</span><h2>{{ companyDialogMode === "create" ? "新增客户" : "编辑客户资料" }}</h2><p>敏感字段不会回显，编辑时留空表示保持原值。</p></div>
            <button type="button" class="dialog-close" aria-label="关闭" @click="closeCompanyDialogs">×</button>
          </div>
          <div v-if="companyActionError" class="form-error">{{ companyActionError }}</div>
          <div class="form-grid">
            <label class="full-field"><span>企业名称 *</span><input v-model="companyForm.companyName" required maxlength="128" placeholder="请输入企业名称" /></label>
            <label><span>统一社会信用代码</span><input v-model="companyForm.creditCode" maxlength="32" :placeholder="companyDialogMode === 'edit' ? '不修改请留空' : '请输入信用代码'" /></label>
            <label><span>法定代表人</span><input v-model="companyForm.legalPerson" maxlength="64" placeholder="请输入法人姓名" /></label>
            <label><span>联系人</span><input v-model="companyForm.contactName" maxlength="64" placeholder="请输入联系人" /></label>
            <label><span>联系电话{{ companyDialogMode === "create" ? " *" : "" }}</span><input v-model="companyForm.contactPhone" :required="companyDialogMode === 'create'" maxlength="20" :placeholder="companyDialogMode === 'edit' ? '不修改请留空' : '请输入手机号或座机'" /></label>
            <label><span>省份</span><input v-model="companyForm.province" maxlength="64" placeholder="例如：湖南省" /></label>
            <label><span>城市</span><input v-model="companyForm.city" maxlength="64" placeholder="例如：长沙市" /></label>
            <label class="full-field"><span>企业邮箱</span><input v-model="companyForm.companyEmail" type="email" maxlength="128" placeholder="service@example.com" /></label>
            <label class="full-field"><span>详细地址</span><textarea v-model="companyForm.detailAddress" maxlength="255" placeholder="请输入企业办公地址" /></label>
          </div>
          <div class="form-dialog-actions"><button type="button" class="secondary-action" @click="closeCompanyDialogs">取消</button><button class="primary" :disabled="companyActionLoading">{{ companyActionLoading ? "正在保存…" : "保存客户资料" }}</button></div>
        </form>
      </div>

      <div v-if="companyDialogMode === 'view' && selectedCompany" class="dialog-backdrop" @click.self="closeCompanyDialogs">
        <section class="form-dialog detail-dialog" role="dialog" aria-modal="true">
          <div class="form-dialog-head"><div><span>CUSTOMER DETAIL</span><h2>{{ selectedCompany.companyName }}</h2><p>企业编号 #{{ selectedCompany.id }} · 敏感信息已脱敏显示</p></div><button type="button" class="dialog-close" aria-label="关闭" @click="closeCompanyDialogs">×</button></div>
          <dl class="detail-list">
            <div><dt>统一社会信用代码</dt><dd class="mono">{{ selectedCompany.creditCodeMask || "未填写" }}</dd></div>
            <div><dt>客户状态</dt><dd><span class="status success">{{ selectedCompany.status === "ENABLED" ? "已启用" : selectedCompany.status }}</span></dd></div>
            <div><dt>联系人</dt><dd>{{ selectedCompany.contactName || "未填写" }}</dd></div>
            <div><dt>联系电话</dt><dd class="mono">{{ selectedCompany.contactPhoneMask || "未填写" }}</dd></div>
            <div><dt>法定代表人</dt><dd>{{ selectedCompany.legalPerson || "未填写" }}</dd></div>
            <div><dt>企业邮箱</dt><dd>{{ selectedCompany.companyEmail || "未填写" }}</dd></div>
            <div><dt>所在地区</dt><dd>{{ [selectedCompany.province, selectedCompany.city].filter(Boolean).join(" ") || "未填写" }}</dd></div>
            <div class="detail-wide"><dt>详细地址</dt><dd>{{ selectedCompany.detailAddress || "未填写" }}</dd></div>
          </dl>
          <div class="form-dialog-actions"><button type="button" class="secondary-action" @click="closeCompanyDialogs">关闭</button><button type="button" class="primary" @click="openCompanyEdit(selectedCompany)">编辑资料</button></div>
        </section>
      </div>

      <div v-if="companyDeleteTarget" class="dialog-backdrop" @click.self="companyDeleteTarget = null">
        <section class="logout-dialog" role="alertdialog" aria-modal="true">
          <div class="dialog-icon"><AdminIcon name="alert" /></div><h2>确认删除客户？</h2>
          <p>“{{ companyDeleteTarget.companyName }}”将被软删除，历史报单和审计记录会保留；存在未完成报单时系统会拒绝操作。</p>
          <div v-if="companyActionError" class="form-error">{{ companyActionError }}</div>
          <div class="dialog-actions"><button type="button" class="secondary" @click="companyDeleteTarget = null">取消</button><button type="button" class="danger" :disabled="companyActionLoading" @click="confirmCompanyDelete">{{ companyActionLoading ? "正在处理…" : "确认删除" }}</button></div>
        </section>
      </div>

      <div v-if="clientAccountCompany" class="dialog-backdrop" @click.self="closeClientAccounts">
        <section class="form-dialog client-account-dialog" role="dialog" aria-modal="true">
          <div class="form-dialog-head"><div><span>CLIENT LOGIN</span><h2>客户登录账号</h2><p>{{ clientAccountCompany.companyName }} · 账号创建后可登录小程序并绑定微信</p></div><button type="button" class="dialog-close" aria-label="关闭" @click="closeClientAccounts">×</button></div>
          <div v-if="clientAccountError" class="form-error">{{ clientAccountError }}</div>
          <div class="client-account-list">
            <div v-for="account in clientAccounts" :key="account.id" class="client-account-item">
              <div><b>{{ account.displayName || "客户用户" }}</b><span class="mono">{{ account.username || "仅微信登录" }}</span><small>{{ account.phoneMask || "未填写手机" }} · {{ account.status === "ENABLED" ? "账号正常" : account.status }}</small></div>
              <button type="button" class="outline compact" @click="clientPasswordTarget = account; clientResetPassword = ''; clientAccountError = ''">重置密码</button>
            </div>
            <p v-if="clientAccountLoading && !clientAccounts.length" class="client-account-empty">正在读取客户账号…</p>
            <p v-else-if="!clientAccounts.length" class="client-account-empty">该企业还没有客户登录账号，请在下方创建。</p>
          </div>
          <form v-if="!clientPasswordTarget" class="client-account-form" @submit.prevent="submitClientAccount">
            <h3>创建并关联新账号</h3>
            <div class="form-grid">
              <label><span>客户姓名 *</span><input v-model="clientAccountForm.displayName" required maxlength="64" placeholder="请输入使用人姓名" /></label>
              <label><span>登录用户名 *</span><input v-model="clientAccountForm.username" required maxlength="64" autocomplete="off" placeholder="建议使用英文或拼音" /></label>
              <label><span>手机号 *</span><input v-model="clientAccountForm.phone" required maxlength="20" autocomplete="off" placeholder="用于身份核对" /></label>
              <label><span>初始密码 *</span><input v-model="clientAccountForm.password" required minlength="8" maxlength="128" type="password" autocomplete="new-password" placeholder="至少 8 位" /></label>
              <label><span>企业成员身份</span><select v-model="clientAccountForm.memberRole"><option value="OWNER">企业负责人</option><option value="CONTACT">业务联系人</option><option value="VIEWER">只读成员</option></select></label>
            </div>
            <div class="form-dialog-actions"><button type="button" class="secondary-action" @click="closeClientAccounts">关闭</button><button class="primary" :disabled="clientAccountLoading">{{ clientAccountLoading ? "正在创建…" : "创建客户账号" }}</button></div>
          </form>
          <form v-else class="client-account-form" @submit.prevent="submitClientPasswordReset">
            <h3>重置 {{ clientPasswordTarget.displayName }} 的密码</h3>
            <div class="form-grid"><label class="full-field"><span>新密码 *</span><input v-model="clientResetPassword" required minlength="8" maxlength="128" type="password" autocomplete="new-password" placeholder="至少 8 位；请通过安全渠道告知客户" /></label></div>
            <div class="form-dialog-actions"><button type="button" class="secondary-action" @click="clientPasswordTarget = null; clientResetPassword = ''">取消</button><button class="primary" :disabled="clientAccountLoading">{{ clientAccountLoading ? "正在保存…" : "确认重置密码" }}</button></div>
          </form>
        </section>
      </div>

      <div v-if="createMemberVisible" class="dialog-backdrop" @click.self="createMemberVisible = false">
        <form class="form-dialog member-dialog" @submit.prevent="submitMember">
          <div class="form-dialog-head"><div><span>NEW MEMBER</span><h2>新增团队成员</h2><p>创建后台登录账号，并为成员分配一个岗位角色。</p></div><button type="button" class="dialog-close" aria-label="关闭" @click="createMemberVisible = false">×</button></div>
          <div v-if="memberActionError" class="form-error">{{ memberActionError }}</div>
          <div class="form-grid">
            <label><span>成员姓名 *</span><input v-model="memberForm.displayName" required maxlength="64" placeholder="请输入姓名" /></label>
            <label><span>登录用户名 *</span><input v-model="memberForm.username" required maxlength="64" autocomplete="off" placeholder="建议使用英文或拼音" /></label>
            <label><span>手机号 *</span><input v-model="memberForm.phone" required maxlength="20" autocomplete="off" placeholder="用于成员身份信息" /></label>
            <label><span>初始密码 *</span><input v-model="memberForm.password" required minlength="12" type="password" autocomplete="new-password" placeholder="至少 12 位" /></label>
          </div>
          <fieldset class="role-selector"><legend>岗位角色 *</legend><label v-for="role in roleOptions" :key="role.value" :class="{ active: memberForm.role === role.value }"><input v-model="memberForm.role" type="radio" :value="role.value" /><span><b>{{ role.label }}</b><small>{{ role.description }}</small></span></label></fieldset>
          <div class="form-dialog-actions"><button type="button" class="secondary-action" @click="createMemberVisible = false">取消</button><button class="primary" :disabled="memberActionLoading">{{ memberActionLoading ? "正在创建…" : "创建成员" }}</button></div>
        </form>
      </div>

      <div v-if="roleEditorAccount" class="dialog-backdrop" @click.self="roleEditorAccount = null">
        <section class="form-dialog member-dialog" role="dialog" aria-modal="true">
          <div class="form-dialog-head"><div><span>ROLE ASSIGNMENT</span><h2>调整成员角色</h2><p>{{ roleEditorAccount.displayName }} · {{ roleEditorAccount.username }}</p></div><button type="button" class="dialog-close" aria-label="关闭" @click="roleEditorAccount = null">×</button></div>
          <div v-if="memberActionError" class="form-error">{{ memberActionError }}</div>
          <fieldset class="role-selector"><legend>选择岗位角色</legend><label v-for="role in roleOptions" :key="role.value" :class="{ active: selectedRole === role.value }"><input v-model="selectedRole" type="radio" :value="role.value" /><span><b>{{ role.label }}</b><small>{{ role.description }}</small></span></label></fieldset>
          <div class="form-dialog-actions"><button type="button" class="secondary-action" @click="roleEditorAccount = null">取消</button><button type="button" class="primary" :disabled="memberActionLoading" @click="saveAccountRole">{{ memberActionLoading ? "正在保存…" : "保存角色" }}</button></div>
        </section>
      </div>

      <div v-if="permissionAccount" class="dialog-backdrop" @click.self="permissionAccount = null">
        <section class="form-dialog permission-dialog" role="dialog" aria-modal="true">
          <div class="form-dialog-head"><div><span>PERMISSION PREVIEW</span><h2>{{ permissionAccount.displayName }}的权限</h2><p>{{ roleText(permissionAccount.roles) }} · 权限来自当前角色配置</p></div><button type="button" class="dialog-close" aria-label="关闭" @click="permissionAccount = null">×</button></div>
          <div class="permission-list"><span v-for="permission in permissionAccount.permissions || []" :key="permission">{{ permissionLabels[permission] || permission }}<small>{{ permission }}</small></span><p v-if="!permissionAccount.permissions?.length">该成员当前没有业务权限。</p></div>
          <div class="form-dialog-actions single"><button type="button" class="primary" @click="permissionAccount = null">知道了</button></div>
        </section>
      </div>

      <div
        v-if="logoutConfirmVisible"
        class="dialog-backdrop"
        @click.self="logoutConfirmVisible = false"
      >
        <section
          class="logout-dialog"
          role="dialog"
          aria-modal="true"
          aria-labelledby="logout-dialog-title"
        >
          <div class="dialog-icon"><AdminIcon name="logout" /></div>
          <h2 id="logout-dialog-title">确认退出登录？</h2>
          <p>退出后需要重新输入管理员账号和密码。</p>
          <div class="dialog-actions">
            <button type="button" class="secondary" @click="logoutConfirmVisible = false">取消</button>
            <button type="button" class="danger" @click="confirmLogout">确认退出</button>
          </div>
        </section>
      </div>
    </main>
    <div v-if="toast" class="toast">{{ toast }}</div>
  </div>
</template>
