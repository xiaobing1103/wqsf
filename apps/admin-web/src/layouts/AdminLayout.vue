<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getDashboard } from "../api/dashboard";
import { getAdminCase, listAdminCases, requestSupplement, reviewMaterial, updateAdminCaseStatus } from "../api/cases";
import { listAdminCompanies } from "../api/companies";
import { getProductTemplate, getServiceCatalog, listAdminProducts } from "../api/catalog";
import { createMaterialZip, createReviewExcel, getExportDownload, getExportJob, type ExportJobView } from "../api/exports";
import { importCreate, importTemplate } from "../api/generated/endpoints";
import { unwrapResponse } from "../api/unwrap";
import { useAuthStore } from "../stores/auth";
import { ApiError } from "../types/api";
import type { DashboardData } from "../types/dashboard";
import type { CaseSummary, CaseView, CompanyView, JobView as ImportJobView, ModuleView, ProductAdminView, TemplateDownload, TemplateView } from "../api/generated/models";

type ViewKey = "dashboard" | "cases" | "customers" | "exports" | "products";

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

const cases = ref<CaseSummary[]>([]);
const recentCases = ref<CaseSummary[]>([]);
const caseLoading = ref(false);
const caseError = ref("");
const casePage = ref({ page: 1, pageSize: 20, total: 0, totalPages: 0 });
const caseDetailLoading = ref(false);
const caseActionLoading = ref(false);

const companies = ref<CompanyView[]>([]);
const companyLoading = ref(false);
const companyError = ref("");
const companySearch = ref("");
const companyPage = ref({ page: 1, pageSize: 20, total: 0, totalPages: 0 });
const importInput = ref<HTMLInputElement | null>(null);
const importLoading = ref(false);

const exportJobs = ref<ExportJobView[]>([]);
const exportLoading = ref(false);
const exportError = ref("");
const catalog = ref<ModuleView[]>([]);
const products = ref<ProductAdminView[]>([]);
const selectedTemplate = ref<TemplateView | null>(null);
const catalogLoading = ref(false);

const displayName = computed(() => auth.user?.displayName || "管理员");
const avatarText = computed(() => displayName.value.slice(0, 1));
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

function statusText(status?: string) { return statusLabels[status || ""] || status || "未知"; }
function statusClass(status?: string) { return status === "COMPLETED" ? "success" : status === "PROCESSING" ? "processing" : status === "CANCELED" ? "muted-status" : "warning"; }
function progressText(item: CaseSummary) { return `${item.materials?.completed ?? 0} / ${item.materials?.total ?? 0}`; }
function progressPercent(item: CaseSummary) { const total = item.materials?.total ?? 0; return total ? Math.round(((item.materials?.completed ?? 0) / total) * 100) : 0; }
function formatTime(value?: string) { if (!value) return "—"; return new Intl.DateTimeFormat("zh-CN", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" }).format(new Date(value)); }
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
  try { caseDetail.value = await getAdminCase(id); }
  catch (error) { caseError.value = error instanceof ApiError ? error.message : "报单详情加载失败"; caseDetail.value = null; }
  finally { caseDetailLoading.value = false; }
}
function closeCase() { selectedCaseId.value = null; caseDetail.value = null; }
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
  try { selectedTemplate.value = await getProductTemplate(productId); }
  catch (error) { notify(error instanceof ApiError ? error.message : "资料模板加载失败"); }
}
function nav(view: ViewKey) { closeCase(); const path = view === "customers" ? "/companies" : `/${view}`; void router.push(path); }
function notify(message: string) { toast.value = message; setTimeout(() => (toast.value = ""), 2600); }
async function logout() { try { await auth.logout(); } finally { await router.replace("/login"); } }

watch(() => route.name, (name) => {
  closeCase();
  if (name === "dashboard") void loadDashboard();
  if (name === "cases") void loadCases();
  if (name === "companies") void loadCompanies();
  if (name === "exports") void loadExports();
  if (name === "products") void loadCatalog();
});
watch([search, filter], () => { if (current.value === "cases") { casePage.value.page = 1; void loadCases(); } });
watch(companySearch, () => { if (current.value === "customers") { companyPage.value.page = 1; void loadCompanies(); } });
onMounted(() => { if (current.value === "dashboard") void loadDashboard(); else if (current.value === "cases") void loadCases(); else if (current.value === "customers") void loadCompanies(); else if (current.value === "exports") void loadExports(); else if (current.value === "products") void loadCatalog(); });
</script>

<template>
  <div class="admin-shell">
    <aside class="sidebar">
      <div class="brand-block">
        <div class="brand-symbol">WQ</div>
        <div><strong>万企商服通</strong><span>BUSINESS SERVICE</span></div>
      </div>
      <div class="workspace-pill">
        <div class="mini-avatar">{{ avatarText }}</div>
        <div><b>运营工作台</b><small>企业服务中心</small></div>
        <span>⌄</span>
      </div>
      <nav>
        <button
          :class="{ active: current === 'dashboard' }"
          @click="nav('dashboard')"
        >
          <i>⌂</i>工作台
        </button>
        <button :class="{ active: current === 'cases' }" @click="nav('cases')">
          <i>▤</i>报单管理
          <em>{{
            dashboardStats.pendingReview + dashboardStats.supplement
          }}</em>
        </button>
        <button
          :class="{ active: current === 'customers' }"
          @click="nav('customers')"
        >
          <i>◎</i>客户资料
        </button>
        <button
          :class="{ active: current === 'exports' }"
          @click="nav('exports')"
        >
          <i>⇩</i>导出中心
        </button>
        <div class="nav-divider" />
        <label>系统设置</label>
        <button
          :class="{ active: current === 'products' }"
          @click="nav('products')"
        ><i>◈</i>业务模板</button>
        <button @click="notify('团队与权限设置')"><i>⚙</i>团队与权限</button>
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
                  : "业务模板"
          }}
        </div>
        <div class="top-actions">
          <button class="icon-btn" @click="notify('通知中心将在后续阶段接入')">
            ♢<b /></button
          ><button
            class="user-menu"
            type="button"
            title="退出登录"
            @click="logout"
          >
            <div class="mini-avatar">{{ avatarText }}</div>
            <span>{{ displayName }}</span
            ><small>退出</small>
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
            {{ dashboardLoading ? "正在刷新…" : "刷新数据" }} <span>↻</span>
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
            <div class="metric-top"><span>待审核报单</span><i>▤</i></div>
            <strong>{{ dashboardStats.pendingReview }}</strong>
            <div class="metric-foot">
              <span>真实状态汇总</span><small>需要优先处理</small>
            </div>
          </div>
          <div class="metric-card">
            <div class="metric-top">
              <span>处理中报单</span><i class="green-icon">↗</i>
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
              <span>待补资料</span><i class="orange-icon">!</i>
            </div>
            <strong>{{ dashboardStats.supplement }}</strong>
            <div class="metric-foot">
              <span>当前状态</span><small>请及时提醒客户</small>
            </div>
          </div>
          <div class="metric-card">
            <div class="metric-top">
              <span>启用企业客户</span><i class="blue-icon">◎</i>
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
                <div class="attention-icon blue">⇧</div>
                <div>
                  <b>客户 Excel 导入</b><span>等待或正在解析的任务</span>
                </div>
                <strong>{{ dashboardStats.pendingImports }}</strong>
              </div>
              <div class="queue-item">
                <div class="attention-icon amber">⇩</div>
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
              <div class="attention-icon amber">!</div>
              <div><b>待补资料</b><span>等待客户重新提交</span></div>
              <strong>{{ dashboardStats.supplement }}</strong>
            </div>
            <div class="attention-item">
              <div class="attention-icon rose">⌕</div>
              <div><b>待审核</b><span>等待运营审核资料</span></div>
              <strong>{{ dashboardStats.pendingReview }}</strong>
            </div>
            <div class="attention-item">
              <div class="attention-icon blue">↗</div>
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
            <span>▤</span><b>暂无真实报单</b>
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
                  <p>身份证、征信、网银类文件仅向有权限的审核人员开放。</p>
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
          <button class="primary" @click="downloadImportTemplate">
            ↓ 下载导入模板
          </button>
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
                <th>最近报单</th>
                <th>资料状态</th>
                <th>加入时间</th>
                <th />
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
                <td class="more-cell">···</td>
              </tr>
              <tr v-if="!companyLoading && !companies.length"><td colspan="7" class="empty-cell">暂无企业客户数据</td></tr>
            </tbody>
          </table>
          <div class="pagination"><span>共 {{ companyPage.total }} 家企业</span><button :disabled="companyPage.page <= 1" @click="companyPage.page--; loadCompanies()">‹</button><button class="current-page">{{ companyPage.page }}</button><button :disabled="companyPage.page >= companyPage.totalPages" @click="companyPage.page++; loadCompanies()">›</button></div>
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
            <div v-for="product in module.products || []" :key="product.id" class="catalog-product">
              <div><b>{{ product.productName }}</b><small>{{ product.description }} · {{ product.materialCount }} 项资料</small></div>
              <button class="link-btn" @click="showTemplate(product.id!)">查看模板</button>
            </div>
          </div>
          <div v-if="!catalog.length && !catalogLoading" class="panel empty-live-data"><span>◈</span><b>暂无已发布服务</b><p>请先在数据库发布产品和资料模板。</p></div>
        </div>
        <div v-if="selectedTemplate" class="panel template-detail">
          <div class="panel-head"><div><h2>{{ selectedTemplate.templateName }}</h2><p>版本 {{ selectedTemplate.versionNo }} · {{ selectedTemplate.items?.length || 0 }} 项资料</p></div><button class="link-btn" @click="selectedTemplate = null">关闭</button></div>
          <div class="template-items"><div v-for="item in selectedTemplate.items || []" :key="item.id" class="template-item"><span>{{ item.sortOrder }}</span><b>{{ item.itemName }}</b><small>{{ item.inputType }} · {{ item.required ? '必填' : '选填' }}{{ item.sensitive ? ' · 敏感资料' : '' }}</small></div></div>
        </div>
        <div class="panel table-panel"><div class="panel-head"><div><h2>后台产品记录</h2><p>用于核对服务目录与管理端产品配置是否一致。</p></div></div><table><thead><tr><th>产品编码</th><th>产品名称</th><th>模板编号</th><th>状态</th></tr></thead><tbody><tr v-for="product in products" :key="product.id"><td class="mono">{{ product.productCode }}</td><td>{{ product.productName }}</td><td>{{ product.templateId }}</td><td><span class="status" :class="product.status === 'PUBLISHED' ? 'success' : 'warning'">{{ product.status }}</span></td></tr><tr v-if="!products.length"><td colspan="4" class="empty-cell">暂无产品配置</td></tr></tbody></table></div>
      </section>
    </main>
    <div v-if="toast" class="toast">{{ toast }}</div>
  </div>
</template>
