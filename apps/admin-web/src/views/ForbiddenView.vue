<script setup lang="ts">
import { useRouter } from "vue-router";

const router = useRouter();
</script>

<template>
  <main class="result-page">
    <section class="result-card">
      <div class="result-code">403</div>
      <h1>没有访问权限</h1>
      <p>当前账号缺少该页面所需权限。如需使用，请联系管理员调整角色。</p>
      <button class="primary" @click="router.replace('/dashboard')">
        返回工作台
      </button>
    </section>
  </main>
</template>
