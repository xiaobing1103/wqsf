<script setup lang="ts">
import AppIcon from './AppIcon.vue'

withDefaults(defineProps<{
  kind?: 'arrow' | 'diamond' | 'spark'
  tone?: 'dark' | 'mint' | 'purple'
}>(), {
  kind: 'arrow',
  tone: 'mint',
})

const iconType = {
  arrow: 'shop-filled',
  diamond: 'medal-filled',
  spark: 'auth-filled',
} as const
</script>

<template>
  <view class="glyph" :class="`glyph-${tone}`" aria-hidden="true">
    <AppIcon :type="iconType[kind]" :size="22" />
  </view>
</template>

<style scoped>
.glyph {
  position: relative;
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 58rpx;
  height: 58rpx;
  border-radius: 18rpx;
}

.glyph-dark { background: #164f50; color: var(--color-gold); }
.glyph-mint { background: #e4f3f0; color: #216f73; }
.glyph-purple { background: #f0ebff; color: #715bb5; }
</style>
