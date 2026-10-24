<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue';
import { getCapabilitySettings, updateCapabilitySetting } from '../api/capabilities';
import type { Setting } from '../api/generated/models';
import { useAuthStore } from '../stores/auth';
import AdminIcon from './AdminIcon.vue';

const auth = useAuthStore();
const settings = ref<Setting[]>([]);
const loading = ref(false);
const saving = ref(false);
const error = ref('');
const notice = ref('');
const dialog = ref<HTMLDialogElement>();
const pending = ref<Setting>();
const reason = ref('');
const actionError = ref('');
const definitions = [
  { key: 'tradeApplicationEnabled', name: '贸易增量申请', description: '控制新建、保存、提交及补件修改；关闭后已有报单可只读查看。' },
  { key: 'trademarkConsultationEnabled', name: '商标注册咨询', description: '控制商标咨询入口及咨询申请的新增、修改和提交。' },
  { key: 'qualificationConsultationEnabled', name: '资质申报服务', description: '控制资质咨询入口及申请的新增、修改和提交。' },
  { key: 'categoryMatchEnabled', name: '经营范围品类参考', description: '控制品类入口和关键词推荐，不代表取得经营许可。' },
  { key: 'licenseOcrEnabled', name: '营业执照图片识别', description: '控制腾讯云 OCR 图片识别；依赖品类参考功能开启。' },
  { key: 'sensitiveMaterialUploadEnabled', name: '敏感资料提交', description: '控制敏感附件、敏感文字和开票信息提交；仍需相应服务开启。' },
  { key: 'customerServiceEnabled', name: '微信客服入口', description: '控制小程序原生客服入口；须先完成微信公众平台客服配置。' },
];
const rows = computed(() => definitions.map(item => ({ ...item, setting: settings.value.find(s => s.key === item.key) })));
function effective(setting?: Setting) {
  if (!setting?.enabled) return false;
  if (setting.key === 'licenseOcrEnabled') {
    const category = settings.value.find(s => s.key === 'categoryMatchEnabled');
    return Boolean(category?.enabled);
  }
  return true;
}
function blockedReason(setting?: Setting) {
  if (!setting) return '未读取到配置，暂不能修改';
  if (setting.key === 'licenseOcrEnabled' && setting.enabled && !effective(setting)) return '品类参考未开启，图片识别暂不生效。';
  return '由超级管理员直接控制；开启前请确认业务资质、隐私和所需服务配置。';
}
async function refresh() {
  if (loading.value || saving.value) return;
  loading.value = true; error.value = '';
  try {
    if (!auth.hasRole('SUPER_ADMIN')) throw new Error('仅超级管理员可以管理功能开关');
    settings.value = await getCapabilitySettings();
  } catch (cause) {
    settings.value = [];
    error.value = cause instanceof Error ? cause.message : '读取配置失败，请重试';
  } finally { loading.value = false; }
}
async function startChange(setting?: Setting) {
  if (!setting || setting.version == null || saving.value || loading.value) return;
  pending.value = { ...setting }; reason.value = ''; actionError.value = ''; notice.value = '';
  await nextTick(); dialog.value?.showModal();
}
function cancel(event?: Event) {
  if (saving.value) { event?.preventDefault(); return; }
  dialog.value?.close(); pending.value = undefined;
}
async function save() {
  const target = pending.value;
  if (!target?.key || target.version == null || saving.value) return;
  if (!reason.value.trim()) { actionError.value = '请填写本次调整原因'; return; }
  const name = definitions.find(item => item.key === target.key)?.name || target.key;
  if (!window.confirm(`确认${target.enabled ? '关闭' : '开启'}“${name}”？关闭相关服务将使草稿和补件暂不可修改。`)) return;
  saving.value = true; actionError.value = '';
  try {
    settings.value = await updateCapabilitySetting(target.key, !target.enabled, target.version, reason.value.trim());
    dialog.value?.close(); pending.value = undefined;
    notice.value = '配置已保存并记录审计。小程序重新进入相关页面后更新，后端立即按新配置校验。';
    // 服务端回读实际配置，避免仅相信提交时的本地开关值。
    settings.value = await getCapabilitySettings();
  } catch (cause) {
    actionError.value = cause instanceof Error ? cause.message : '保存失败，请刷新后重试';
    error.value = actionError.value;
    try { settings.value = await getCapabilitySettings(); } catch { settings.value = []; }
    // 冲突时只更新候选版本，仍要求用户重新确认，不自动覆盖。
    pending.value = settings.value.find(item => item.key === target.key);
  } finally { saving.value = false; }
}
onMounted(() => { void refresh(); });
</script>

<template>
  <div v-if="auth.hasRole('SUPER_ADMIN')" class="capability-page">
    <div class="page-intro">
      <div><h1>小程序功能开关</h1><p>统一管理客户功能，仅超级管理员可操作。</p></div>
      <button class="secondary refresh-control" :disabled="loading || saving" @click="refresh"><AdminIcon name="refresh" />{{ loading ? '读取中…' : '刷新配置' }}</button>
    </div>
    <div class="capability-notice"><AdminIcon name="alert" /><div><b>超级管理员可直接控制全部功能</b><p>开关保存后后端立即校验，小程序重新进入页面更新展示。开启不代表审核通过，也不会自动配置 OCR 或微信客服；请先确认业务资质、隐私及所需审核。关闭后已有报单和授权附件仍可查看。</p></div></div>
    <p v-if="error" class="feedback error" role="alert">{{ error }}</p>
    <p v-if="notice" class="feedback success" role="status">{{ notice }}</p>
    <div class="panel capability-list" :aria-busy="loading">
      <div class="capability-head"><span>功能与影响范围</span><span>配置开关</span><span>实际生效</span></div>
      <article v-for="row in rows" :key="row.key" class="capability-row">
        <div class="capability-copy"><h2>{{ row.name }}</h2><p>{{ row.description }}</p><small>{{ blockedReason(row.setting) }}</small></div>
        <div class="switch-cell">
          <button class="capability-switch" type="button" role="switch" :aria-checked="Boolean(row.setting?.enabled)" :aria-label="`${row.name}配置开关`"
            :class="{ on: row.setting?.enabled }" :disabled="loading || saving || !row.setting || row.setting.version == null" @click="startChange(row.setting)"><span /></button>
          <small>{{ row.setting ? row.setting.enabled ? '已配置开启' : '已配置关闭' : '未读取' }}</small>
        </div>
        <div class="effective-cell"><span class="effective-badge" :class="{ active: effective(row.setting) }">{{ effective(row.setting) ? '已生效' : '未生效' }}</span><small>版本 {{ row.setting?.version ?? '—' }}</small></div>
      </article>
    </div>
    <dialog ref="dialog" class="capability-dialog" aria-labelledby="capability-dialog-title" @cancel="cancel" @close="pending = undefined">
      <form @submit.prevent="save">
        <h2 id="capability-dialog-title">{{ pending?.enabled ? '关闭' : '开启' }}{{ definitions.find(item => item.key === pending?.key)?.name }}</h2>
        <p>调整会影响所有客户，后端即时校验。请确认实际业务与所需资质、审核范围一致。</p>
        <label for="capability-reason">调整原因（必填）</label>
        <textarea id="capability-reason" v-model="reason" autofocus required maxlength="200" :disabled="saving" placeholder="填写维护或开放原因，请勿填写客户敏感信息" />
        <p v-if="actionError" class="feedback error" role="alert">{{ actionError }}</p>
        <div class="dialog-actions"><button type="button" class="secondary" :disabled="saving" @click="cancel()">取消</button><button type="submit" class="primary" :disabled="saving || !pending || !reason.trim()">{{ saving ? '保存中…' : '确认保存' }}</button></div>
      </form>
    </dialog>
  </div>
</template>

<style scoped>
.refresh-control { display: flex; align-items: center; gap: 7px; }
.refresh-control svg { width: 17px; height: 17px; }
.capability-notice { display: flex; gap: 12px; padding: 18px 20px; margin-bottom: 20px; border: 1px solid #ebd6ac; border-radius: 12px; background: #fff8e9; color: #87612a; }
.capability-notice > svg { width: 22px; height: 22px; flex: none; }
.capability-notice p { margin: 7px 0 0; line-height: 1.7; font-size: 13px; }
.capability-head, .capability-row { display: grid; grid-template-columns: minmax(0, 1fr) 140px 110px; gap: 24px; padding: 20px 24px; align-items: center; }
.capability-head { background: #f6f9f9; border-radius: 15px 15px 0 0; color: #798d90; font-size: 12px; }
.capability-row { border-top: 1px solid #edf1f1; }
.capability-copy h2 { margin: 0; font-size: 15px; color: #284649; }
.capability-copy p { margin: 7px 0; font-size: 13px; color: #687c80; line-height: 1.7; }
.capability-copy small { font-size: 12px; color: #778e86; line-height: 1.6; }
.capability-copy small.blocked { color: #9e7538; }
.switch-cell, .effective-cell { display: flex; flex-direction: column; align-items: flex-start; gap: 8px; }
.switch-cell small, .effective-cell small { color: #809397; font-size: 11px; }
.capability-switch { width: 46px; height: 26px; padding: 3px; border: 0; border-radius: 20px; background: #b5c2c5; cursor: pointer; }
.capability-switch span { display: block; width: 20px; height: 20px; border-radius: 50%; background: white; transition: transform .15s; box-shadow: 0 1px 3px #0002; }
.capability-switch.on { background: #347f76; }
.capability-switch.on span { transform: translateX(20px); }
.capability-switch:disabled { opacity: .45; cursor: not-allowed; }
.capability-switch:focus-visible { outline: 3px solid #efbd5b; outline-offset: 3px; }
.effective-badge { font-size: 12px; padding: 5px 9px; border-radius: 6px; background: #eff2f3; color: #7a8b8f; }
.effective-badge.active { background: #e7f4ed; color: #34765d; }
.feedback { border-radius: 8px; padding: 12px 16px; font-size: 13px; line-height: 1.6; }
.feedback.error { background: #fff0eb; color: #b25a40; }
.feedback.success { background: #edf7f0; color: #397c5e; }
.capability-dialog { width: min(520px, calc(100vw - 32px)); padding: 26px; border: 1px solid #dbe5e5; border-radius: 16px; color: #29474a; }
.capability-dialog::backdrop { background: #142e3d66; }
.capability-dialog h2 { margin-top: 0; font-size: 18px; }
.capability-dialog p { font-size: 13px; line-height: 1.7; color: #718489; }
.capability-dialog label { display: block; margin: 18px 0 8px; font-size: 13px; }
.capability-dialog textarea { width: 100%; min-height: 110px; padding: 12px; box-sizing: border-box; border: 1px solid #dbe5e5; border-radius: 8px; resize: vertical; font: inherit; font-size: 13px; }
.dialog-actions { display: flex; gap: 12px; justify-content: flex-end; margin-top: 20px; }
@media (max-width: 850px) { .capability-head { display: none; } .capability-row { grid-template-columns: 1fr 1fr; gap: 16px; } .capability-copy { grid-column: 1 / -1; } }
</style>
