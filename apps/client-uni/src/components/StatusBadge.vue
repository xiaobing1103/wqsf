<script setup lang="ts">
withDefaults(defineProps<{ label: string; tone?: string }>(), { tone: 'neutral' })
</script>

<template><text class="badge" :class="`badge-${tone}`">{{ label }}</text></template>

<style scoped>
.badge { display: inline-flex; align-items: center; height: 44rpx; padding: 0 18rpx; border-radius: 22rpx; font-size: 22rpx; }
.badge-warning { background: #fff2d7; color: #a36a0d; }
.badge-info { background: #e7f1f7; color: #276986; }
.badge-success { background: #e4f3ec; color: #247051; }
.badge-danger { background: #fae9e7; color: #a84136; }
.badge-neutral { background: #edf1f2; color: #667679; }
</style>
