<script setup lang="ts">
import type { MaterialView } from '../api/generated/models'
import StatusBadge from './StatusBadge.vue'
import { materialStatusLabel, materialStatusTone } from '../constants/status'

defineProps<{
  material: MaterialView
  index: number
  disabled?: boolean
}>()

defineEmits<{ open: [material: MaterialView] }>()
</script>

<template>
  <button class="material-row" :disabled="disabled" @click="$emit('open', material)">
    <text class="material-index">{{ String(index + 1).padStart(2, '0') }}</text>
    <view class="material-copy">
      <view class="material-title-line">
        <text class="material-name">{{ material.materialName }}</text>
        <text v-if="material.required" class="required-mark">必填</text>
      </view>
      <text class="material-help">{{ material.clientVisibleNote || (material.submitStatus === 'SUBMITTED' ? '资料已保存' : '资料尚未提交') }}</text>
    </view>
    <StatusBadge
      :label="materialStatusLabel(material.submitStatus, material.reviewStatus, material.inputType)"
      :tone="materialStatusTone(material.submitStatus, material.reviewStatus)"
    />
  </button>
</template>

<style scoped>
.material-row { display: flex; align-items: center; width: 100%; min-height: 88rpx; margin: 0; padding: 15rpx 18rpx; border-bottom: 1rpx solid #e2e9e9; background: #fff; color: var(--color-text); text-align: left; line-height: 1.25; }
.material-row:last-child { border-bottom: 0; }
.material-row[disabled] { opacity: 0.75; }
.material-index { display: flex; flex: none; align-items: center; justify-content: center; width: 42rpx; height: 42rpx; border-radius: 12rpx; background: #e9f4f2; color: var(--color-primary-light); font-size: 18rpx; font-weight: 800; }
.material-copy { flex: 1; min-width: 0; margin: 0 16rpx; }
.material-title-line { display: flex; align-items: center; }
.material-name { overflow: hidden; font-size: 22rpx; font-weight: 800; text-overflow: ellipsis; white-space: nowrap; }
.required-mark { flex: none; margin-left: 8rpx; color: #a86c14; font-size: 16rpx; }
.material-help { display: block; margin-top: 6rpx; overflow: hidden; color: #849093; font-size: 17rpx; text-overflow: ellipsis; white-space: nowrap; }
</style>
