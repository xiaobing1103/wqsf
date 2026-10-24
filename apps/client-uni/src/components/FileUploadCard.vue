<script setup lang="ts">
import type { FileView, ItemView, MaterialView } from '../api/generated/models'
import AppIcon from './AppIcon.vue'
import FilePicker from './FilePicker.vue'
import type { SelectedLocalFile } from '../composables/useFileUpload'
import { previewMaterialFile } from '../api/file'
import { useCapabilitiesStore } from '../stores/capabilities'
const capabilities = useCapabilitiesStore()

defineProps<{
  material: MaterialView
  templateItem?: ItemView
  files: FileView[]
  busy?: boolean
  disabled?: boolean
}>()

const emit = defineEmits<{
  selected: [material: MaterialView, files: SelectedLocalFile[]]
  error: [error: Error]
  delete: [materialId: number, fileId: number]
}>()

async function preview(file: FileView) {
  try { await previewMaterialFile(file) }
  catch (cause) { emit('error', cause instanceof Error ? cause : new Error('预览失败')) }
}

function formatSize(value?: number) {
  if (!value) return '0 KB'
  if (value >= 1024 * 1024) return `${(value / 1024 / 1024).toFixed(1)} MB`
  return `${Math.max(1, Math.round(value / 1024))} KB`
}
</script>

<template>
  <view class="upload-card" :class="{ warning: material.reviewStatus === 'NEED_SUPPLEMENT' }">
    <view class="upload-heading">
      <view class="upload-mark"><AppIcon type="cloud-upload" :size="23" /></view>
      <view class="upload-copy">
        <view><text class="upload-name">{{ material.materialName }}</text><text v-if="material.required" class="required">必填</text></view>
        <text class="upload-help">{{ material.clientVisibleNote || templateItem?.helpText || '请选择符合要求的文件' }}</text>
      </view>
      <FilePicker
        v-if="!disabled && (!material.sensitive || capabilities.flags.sensitiveMaterialUploadEnabled)"
        :count="(templateItem?.maxFileCount || 1) - files.length"
        :extensions="templateItem?.allowedExtensions || []"
        :max-file-size-mb="templateItem?.maxFileSizeMb"
        :disabled="busy || disabled"
        @selected="$emit('selected', material, $event)"
        @error="$emit('error', $event)"
      >
        {{ busy ? '处理中' : files.length ? '继续上传' : '选择文件' }}
      </FilePicker>
      <text v-else class="readonly-label">只读资料</text>
    </view>

    <view v-if="templateItem" class="upload-rule">
      {{ (templateItem.allowedExtensions || []).join('、').toUpperCase() }}
      <text v-if="templateItem.maxFileSizeMb"> · 单个不超过 {{ templateItem.maxFileSizeMb }}MB</text>
      <text v-if="templateItem.maxFileCount"> · 最多 {{ templateItem.maxFileCount }} 个</text>
    </view>

    <view v-if="files.length" class="file-list">
      <view v-for="file in files" :key="file.id" class="file-row">
        <view class="file-icon"><AppIcon type="paperclip" :size="18" /></view>
        <view class="file-copy" @click="preview(file)"><text>{{ file.originalName || '已上传文件' }}</text><small>{{ formatSize(file.fileSize) }} · 点击查看</small></view>
        <button class="preview-button" @click="preview(file)">查看</button>
        <button v-if="file.id && !disabled" class="delete-button" :disabled="busy" @click="$emit('delete', material.id || 0, file.id)"><AppIcon type="trash" :size="15" />删除</button>
      </view>
    </view>
  </view>
</template>

<style scoped>
.upload-card { margin-bottom: 18rpx; padding: 22rpx; border: 1rpx solid #dbe4e5; border-radius: 22rpx; background: #fff; }
.upload-card.warning { border-color: #efc9c2; background: #fffafa; }
.upload-heading { display: flex; align-items: center; }
.upload-mark { display: flex; flex: none; align-items: center; justify-content: center; width: 52rpx; height: 52rpx; border-radius: 15rpx; background: #e4f3f0; color: var(--color-primary-light); }
.upload-copy { flex: 1; min-width: 0; margin: 0 14rpx; }
.upload-copy text { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.upload-name { font-size: 22rpx; font-weight: 800; }
.required { margin-left: 8rpx; color: #a86c14; font-size: 16rpx; }
.upload-help { display: block; margin-top: 6rpx; color: #7d8b8d; font-size: 17rpx; }
.upload-rule { margin-top: 16rpx; padding: 12rpx 14rpx; border-radius: 12rpx; background: #f3f6f6; color: #728083; font-size: 16rpx; }
.file-list { margin-top: 14rpx; }
.file-row { display: flex; align-items: center; padding: 12rpx 0; border-top: 1rpx solid #edf1f1; }
.file-icon { display: flex; align-items: center; justify-content: center; width: 38rpx; height: 38rpx; border-radius: 10rpx; background: #edf4f4; }
.file-icon { color: var(--color-primary-light); }
.file-copy { flex: 1; min-width: 0; margin: 0 12rpx; }
.preview-button { margin: 0 8rpx; padding: 0 12rpx; color: var(--color-primary); font-size: 20rpx; background: #edf4f4; }
.readonly-label { color: #7b8e8d; font-size: 19rpx; }
.file-copy text, .file-copy small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.file-copy text { font-size: 19rpx; }
.file-copy small { margin-top: 4rpx; color: #8a9698; font-size: 16rpx; }
.delete-button { display: flex; align-items: center; gap: 6rpx; height: 46rpx; margin: 0; padding: 0 14rpx; border-radius: 13rpx; background: #f8ebea; color: #a3483e; font-size: 18rpx; line-height: 46rpx; }
</style>
