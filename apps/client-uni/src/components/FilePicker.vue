<script setup lang="ts">
import { ref } from 'vue'
import { chooseLocalFiles, validateLocalFile, type SelectedLocalFile } from '../composables/useFileUpload'

const props = withDefaults(defineProps<{
  count?: number
  extensions?: string[]
  maxFileSizeMb?: number
  disabled?: boolean
}>(), { count: 1, extensions: () => [], disabled: false })

const emit = defineEmits<{
  selected: [files: SelectedLocalFile[]]
  error: [error: Error]
}>()
const selecting = ref(false)

async function selectFiles() {
  if (props.disabled || selecting.value) return
  if (props.count <= 0) {
    emit('error', new Error('已达到文件数量上限，请先删除文件再上传'))
    return
  }
  selecting.value = true
  try {
    const files = await chooseLocalFiles(props.count, props.extensions)
    for (const file of files) validateLocalFile(file, props.extensions, props.maxFileSizeMb)
    if (files.length) emit('selected', files)
  } catch (cause) {
    const error = cause instanceof Error ? cause : new Error('选择文件失败')
    if (!/cancel/i.test(error.message)) emit('error', error)
  } finally {
    selecting.value = false
  }
}
</script>

<template>
  <button class="file-picker" :disabled="disabled || selecting" @click="selectFiles">
    <slot :selecting="selecting">{{ selecting ? '选择中' : '选择文件' }}</slot>
  </button>
</template>

<style scoped>
.file-picker { flex: none; height: 52rpx; margin: 0; padding: 0 16rpx; border-radius: 15rpx; background: var(--color-primary); color: #fff; font-size: 19rpx; line-height: 52rpx; }
.file-picker[disabled] { background: #9aacac; }
</style>
