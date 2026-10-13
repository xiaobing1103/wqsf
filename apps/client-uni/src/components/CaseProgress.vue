<script setup lang="ts">
withDefaults(defineProps<{ activeStep?: 1 | 2 | 3 }>(), { activeStep: 2 })
const steps = ['选择服务', '提交资料', '审核处理']
</script>

<template>
  <view class="case-steps">
    <view v-for="(label, index) in steps" :key="label" class="case-step" :class="{ active: index + 1 === activeStep, done: index + 1 < activeStep }">
      <view class="step-dot">{{ index + 1 }}</view>
      <text>{{ label }}</text>
    </view>
  </view>
</template>

<style scoped>
.case-steps { position: relative; display: grid; grid-template-columns: repeat(3, 1fr); padding: 24rpx 60rpx 20rpx; background: #fff; }
.case-steps::before { position: absolute; top: 43rpx; right: 150rpx; left: 150rpx; height: 2rpx; background: #d7e1e2; content: ''; }
.case-step { position: relative; z-index: 1; display: flex; flex-direction: column; align-items: center; color: #859194; font-size: 18rpx; }
.step-dot { display: flex; align-items: center; justify-content: center; width: 40rpx; height: 40rpx; margin-bottom: 8rpx; border: 2rpx solid #cad6d7; border-radius: 50%; background: #fff; color: #708083; }
.case-step.active .step-dot, .case-step.done .step-dot { border-color: var(--color-primary-light); background: var(--color-primary-light); color: #fff; }
.case-step.active { color: var(--color-primary); font-weight: 700; }
</style>
