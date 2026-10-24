<script setup lang="ts">
import type { CaseView } from '../api/generated/models'
import { formatBeijingTime } from '../utils/time'
import { useCapabilitiesStore } from '../stores/capabilities'
const capabilities = useCapabilitiesStore()
defineProps<{ detail: CaseView }>()
</script>
<template>
  <view class="time-notice">
    <text>创建：{{ formatBeijingTime(detail.createdAt) }}</text>
    <text>最近保存：{{ formatBeijingTime(detail.updatedAt) }}</text>
    <text v-if="detail.submittedAt">最近提交：{{ formatBeijingTime(detail.submittedAt) }}</text>
    <text v-if="!capabilities.serviceEnabled(detail.serviceType)" class="notice-copy">该服务暂未开放新增或修改，已保存资料仍可只读查看。</text>
    <text v-else class="notice-copy">{{ detail.status === 'DRAFT' ? '这是尚未提交的草稿。已保存的资料可继续编辑，点击提交审核后才会进入后台待审核列表。' : detail.status === 'NEED_SUPPLEMENT' ? '请按补件说明修改资料，然后重新提交审核。' : '已提交的资料可以查看；图片支持点击放大，当前不能修改。' }}</text>
  </view>
</template>
<style scoped>
.time-notice { margin: 20rpx 0; padding: 20rpx; border: 1rpx solid #dbe6e5; border-radius: 16rpx; background: #edf5f3; color: #446562; font-size: 20rpx; }
.time-notice text { display: block; margin-bottom: 6rpx; }
.notice-copy { margin-top: 12rpx; line-height: 1.6; }
</style>
