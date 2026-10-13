<script setup lang="ts">
import AppIcon from './AppIcon.vue'

defineProps<{ active: 'home' | 'category' | 'mine' }>()

const go = (target: 'home' | 'category' | 'mine') => {
  const routes = {
    home: '/pages/home/index',
    category: '/pages/category/match',
    mine: '/pages/mine/index',
  }
  uni.reLaunch({ url: routes[target] })
}
</script>

<template>
  <view class="bottom-nav">
    <button class="nav-item" :class="{ active: active === 'home' }" @click="go('home')">
      <view class="nav-icon-wrap" :class="{ active: active === 'home' }"><AppIcon :type="active === 'home' ? 'home-filled' : 'home'" :size="24" /></view><text>首页</text>
    </button>
    <button class="nav-item" :class="{ active: active === 'category' }" @click="go('category')">
      <view class="nav-icon-wrap" :class="{ active: active === 'category' }"><AppIcon type="scan" :size="25" /></view><text>品类匹配</text>
    </button>
    <button class="nav-item" :class="{ active: active === 'mine' }" @click="go('mine')">
      <view class="nav-icon-wrap" :class="{ active: active === 'mine' }"><AppIcon :type="active === 'mine' ? 'person-filled' : 'person'" :size="25" /></view><text>我的</text>
    </button>
  </view>
</template>

<style scoped>
.bottom-nav {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 20;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  height: calc(120rpx + env(safe-area-inset-bottom));
  padding: 10rpx 14rpx calc(10rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid rgba(205, 220, 220, 0.82);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 -12rpx 30rpx rgba(24, 71, 71, 0.08);
  backdrop-filter: blur(16px);
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  margin: 0;
  padding: 0;
  border: 0;
  border-radius: 22rpx;
  background: transparent;
  color: #899497;
  font-size: 21rpx;
  line-height: 1;
}

.nav-item:active { background: #f1f7f6; }
.nav-item.active { color: var(--color-primary); font-weight: 700; }
.nav-icon-wrap { display: flex; align-items: center; justify-content: center; width: 64rpx; height: 52rpx; border-radius: 18rpx; color: #899497; }
.nav-icon-wrap.active { background: #e4f2f0; color: var(--color-primary); }
.nav-item :deep(.app-icon) { width: 34rpx; height: 34rpx; }
.nav-item text { margin-top: 6rpx; }
</style>
