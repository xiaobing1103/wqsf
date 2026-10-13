<script setup lang="ts">
import AppIcon from './AppIcon.vue'

defineProps<{
  eyebrow: string
  title: string
  description: string
}>()

const goHome = () => uni.reLaunch({ url: '/pages/home/index' })
</script>

<template>
  <view class="phase-card">
    <view class="phase-mark"><AppIcon type="plus-filled" :size="34" /></view>
    <text class="phase-eyebrow">{{ eyebrow }}</text>
    <text class="phase-title">{{ title }}</text>
    <text class="phase-description">{{ description }}</text>
    <view class="phase-status"><view class="status-dot" /><text>路由已接通 · 功能将在下一批开发</text></view>
    <button class="home-action" @click="goHome">返回首页</button>
  </view>
</template>

<style scoped>
.phase-card { margin: 80rpx 32rpx 0; padding: 52rpx 38rpx; border: 1rpx solid var(--color-border); border-radius: 32rpx; background: #fff; text-align: center; box-shadow: 0 20rpx 48rpx rgba(28, 65, 65, 0.07); }
.phase-mark { display: flex; align-items: center; justify-content: center; width: 100rpx; height: 100rpx; margin: 0 auto 30rpx; border-radius: 28rpx; background: #e3f1ef; color: var(--color-primary); }
.phase-eyebrow, .phase-title, .phase-description { display: block; }
.phase-eyebrow { color: var(--color-primary-light); font-size: 20rpx; font-weight: 800; letter-spacing: 3rpx; }
.phase-title { margin-top: 12rpx; font-size: 38rpx; font-weight: 900; }
.phase-description { margin-top: 20rpx; color: var(--color-muted); font-size: 24rpx; line-height: 1.75; }
.phase-status { display: flex; align-items: center; justify-content: center; margin-top: 30rpx; padding: 20rpx; border-radius: 18rpx; background: #f4f7f7; color: #637375; font-size: 21rpx; }
.status-dot { width: 12rpx; height: 12rpx; margin-right: 12rpx; border-radius: 50%; background: var(--color-gold); }
.home-action { width: 100%; height: 76rpx; margin-top: 28rpx; border-radius: 20rpx; background: var(--color-primary); color: #fff; font-size: 25rpx; font-weight: 700; line-height: 76rpx; }
</style>
