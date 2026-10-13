<script setup lang="ts">
import UniIcons from '@dcloudio/uni-ui/lib/uni-icons/uni-icons.vue'

withDefaults(defineProps<{
  type: string
  size?: number | string
  color?: string
}>(), {
  size: 22,
  color: 'currentColor',
})
</script>

<template>
  <view class="app-icon" aria-hidden="true">
    <UniIcons :type="type" :size="size" :color="color" />
  </view>
</template>

<style scoped>
.app-icon {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  line-height: 1;
}
</style>
