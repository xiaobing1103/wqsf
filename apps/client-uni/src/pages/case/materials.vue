<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import AppIcon from '../../components/AppIcon.vue'
import CaseProgress from '../../components/CaseProgress.vue'
import MaterialItem from '../../components/MaterialItem.vue'
import type { MaterialView } from '../../api/generated/models'
import { errorMessage } from '../../api/response'
import { caseStatusLabel } from '../../constants/status'
import { useCaseDraftStore } from '../../stores/caseDraft'

const caseDraftStore = useCaseDraftStore()
const routeCaseId = ref<number>()
const pageError = ref('')
let skipNextShow = false

const caseTitle = computed(() => caseDraftStore.caseDetail?.caseNo || '贸易增量报单')
const missingMaterials = computed(() => caseDraftStore.materials.filter((item) =>
  item.required && item.submitStatus !== 'SUBMITTED' && item.reviewStatus !== 'NOT_REQUIRED',
))

function confirmDialog(title: string, content: string) {
  return new Promise<boolean>((resolve) => {
    uni.showModal({ title, content, confirmText: '确认提交', success: (result) => resolve(result.confirm), fail: () => resolve(false) })
  })
}

async function loadCase() {
  if (!routeCaseId.value) {
    pageError.value = '缺少报单编号，请从服务选择页重新进入'
    return
  }
  pageError.value = ''
  try {
    await caseDraftStore.loadCase(routeCaseId.value)
  } catch (cause) {
    pageError.value = errorMessage(cause, '资料清单加载失败')
  }
}

function openMaterial(material: MaterialView) {
  if (!caseDraftStore.caseDetail?.id || !caseDraftStore.isEditable) return
  const tab = material.inputType === 'FORM_GROUP' ? 'invoice' : 'files'
  uni.navigateTo({ url: `/pages/case/invoice?caseId=${caseDraftStore.caseDetail.id}&tab=${tab}&materialId=${material.id || ''}` })
}

function continueEditing() {
  if (!caseDraftStore.caseDetail?.id) return
  const target = missingMaterials.value[0]
  const tab = target?.inputType === 'FORM_GROUP' ? 'invoice' : 'files'
  uni.navigateTo({ url: `/pages/case/invoice?caseId=${caseDraftStore.caseDetail.id}&tab=${tab}&materialId=${target?.id || ''}` })
}

async function submitForReview() {
  if (!caseDraftStore.isEditable) {
    uni.showToast({ title: '当前报单已提交，不能重复提交', icon: 'none' })
    return
  }
  try {
    await caseDraftStore.refreshCase()
    if (missingMaterials.value.length) {
      const first = missingMaterials.value[0]
      const confirmed = await new Promise<boolean>((resolve) => {
        uni.showModal({
          title: `还有 ${missingMaterials.value.length} 项必填资料`,
          content: `请先补充“${first.materialName || '必填资料'}”。`,
          confirmText: '去补充',
          success: (result) => resolve(result.confirm),
          fail: () => resolve(false),
        })
      })
      if (confirmed) openMaterial(first)
      return
    }
    if (!await confirmDialog('提交审核', '提交后资料将进入后台审核，确认继续吗？')) return
    await caseDraftStore.submitCurrentCase()
    uni.showToast({ title: '已提交审核', icon: 'success' })
    setTimeout(() => uni.redirectTo({ url: '/pages/case/list?filter=processing' }), 500)
  } catch (cause) {
    uni.showToast({ title: errorMessage(cause, '提交失败，请稍后重试'), icon: 'none', duration: 3000 })
  }
}

function saveDraftNotice() {
  uni.showToast({ title: '草稿已自动保存', icon: 'none' })
}

function openMine() {
  uni.redirectTo({ url: '/pages/case/list?filter=all' })
}

onLoad((query) => {
  const caseId = Number(query?.caseId)
  if (Number.isFinite(caseId) && caseId > 0) routeCaseId.value = caseId
  skipNextShow = true
  void loadCase()
})

onShow(() => {
  if (skipNextShow) { skipNextShow = false; return }
  if (routeCaseId.value) void loadCase()
})
</script>

<template>
  <view class="safe-page materials-page">
    <CaseProgress :active-step="caseDraftStore.isEditable ? 2 : 3" />

    <view v-if="caseDraftStore.loading && !caseDraftStore.caseDetail" class="state-panel">
      <text class="state-title">正在读取资料清单</text>
      <text class="state-copy">资料项和数量以后台发布模板为准。</text>
    </view>
    <view v-else-if="pageError" class="state-panel error-panel">
      <text class="state-title">资料清单加载失败</text>
      <text class="state-copy">{{ pageError }}</text>
      <button class="state-action" @click="loadCase">重新加载</button>
    </view>

    <template v-else-if="caseDraftStore.caseDetail">
      <view class="materials-heading">
        <view>
          <text class="heading-eyebrow">TRADE INCREMENT</text>
          <text class="heading-title">资料提交清单</text>
          <text class="heading-copy">{{ caseTitle }} · {{ caseStatusLabel(caseDraftStore.caseDetail.status) }}</text>
        </view>
        <view class="completion-ring" :style="{ '--progress': `${caseDraftStore.completionPercent * 3.6}deg` }">
          <view><strong>{{ caseDraftStore.completionPercent }}%</strong><text>{{ caseDraftStore.completedCount }}/{{ caseDraftStore.totalCount }}</text></view>
        </view>
      </view>

      <view v-if="!caseDraftStore.isEditable" class="readonly-note">该报单已进入审核流程，当前资料为只读状态。</view>

      <view class="materials-list">
        <MaterialItem
          v-for="(material, index) in caseDraftStore.materials"
          :key="material.id || material.materialCode"
          :material="material"
          :index="index"
          :disabled="!caseDraftStore.isEditable"
          @open="openMaterial"
        />
      </view>

      <view class="materials-actions">
        <button class="draft-button" @click="saveDraftNotice">保存草稿</button>
        <button v-if="caseDraftStore.isEditable" class="submit-button" :disabled="caseDraftStore.submitting" @click="submitForReview">
          {{ caseDraftStore.submitting ? '正在提交…' : missingMaterials.length ? `继续补充（${missingMaterials.length}）` : '提交审核' }}
          <AppIcon class="action-icon" type="right" :size="17" />
        </button>
        <button v-else class="submit-button" @click="openMine">查看报单状态 <AppIcon class="action-icon" type="right" :size="17" /></button>
      </view>
    </template>

  </view>
</template>

<style scoped>
.materials-page { background: #f4f7f7; }
.materials-heading { display: flex; align-items: center; justify-content: space-between; padding: 28rpx 32rpx 18rpx; }
.heading-eyebrow, .heading-title, .heading-copy { display: block; }
.heading-eyebrow { color: var(--color-primary-light); font-size: 18rpx; font-weight: 800; letter-spacing: 3rpx; }
.heading-title { margin-top: 5rpx; font-size: 38rpx; font-weight: 900; }
.heading-copy { margin-top: 4rpx; color: var(--color-muted); font-size: 19rpx; }
.completion-ring { display: flex; align-items: center; justify-content: center; width: 86rpx; height: 86rpx; border-radius: 50%; background: conic-gradient(var(--color-primary-light) var(--progress), #dfe8e8 0); }
.completion-ring > view { display: flex; flex-direction: column; align-items: center; justify-content: center; width: 68rpx; height: 68rpx; border-radius: 50%; background: #f4f7f7; }
.completion-ring strong, .completion-ring text { display: block; }
.completion-ring strong { color: var(--color-primary); font-size: 22rpx; }
.completion-ring text { margin-top: 2rpx; color: #819092; font-size: 14rpx; }
.readonly-note { margin: 0 32rpx 16rpx; padding: 18rpx 20rpx; border-radius: 16rpx; background: #eaf2f6; color: #3a6f86; font-size: 20rpx; }
.materials-list { margin: 0 32rpx; overflow: hidden; border: 1rpx solid #dce5e5; border-radius: 24rpx; background: #fff; }
.materials-actions { display: grid; grid-template-columns: 0.75fr 1.25fr; gap: 14rpx; padding: 20rpx 32rpx 8rpx; }
.materials-actions button { display: flex; align-items: center; justify-content: center; width: 100%; height: 76rpx; margin: 0; border-radius: 20rpx; font-size: 24rpx; font-weight: 800; line-height: 1; }
.draft-button { border: 1rpx solid #ccd8d9; background: #fff; color: var(--color-primary); }
.submit-button { background: var(--color-primary); color: #fff; }
.submit-button[disabled] { opacity: 0.65; }
.action-icon { margin-left: 12rpx; color: var(--color-gold); }
.error-panel { background: #fff8ef; }
</style>
