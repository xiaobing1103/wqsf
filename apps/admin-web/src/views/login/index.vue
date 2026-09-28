<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ApiError } from "../../types/api";
import { useAuthStore } from "../../stores/auth";

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const username = ref("");
const password = ref("");
const submitting = ref(false);
const errorMessage = ref("");

const canSubmit = computed(
  () =>
    username.value.trim().length > 0 &&
    password.value.length > 0 &&
    !submitting.value,
);

function safeRedirect() {
  const redirect =
    typeof route.query.redirect === "string"
      ? route.query.redirect
      : "/dashboard";
  return redirect.startsWith("/") && !redirect.startsWith("//")
    ? redirect
    : "/dashboard";
}

async function submit() {
  if (!canSubmit.value) return;
  errorMessage.value = "";
  submitting.value = true;
  try {
    await auth.login({
      username: username.value.trim(),
      password: password.value,
    });
    await router.replace(safeRedirect());
  } catch (error) {
    errorMessage.value =
      error instanceof ApiError ? error.message : "登录失败，请稍后重试";
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-brand-panel">
      <div class="login-brand">
        <div class="brand-symbol">WQ</div>
        <div><strong>万企商服通</strong><span>BUSINESS SERVICE</span></div>
      </div>
      <div class="login-brand-copy">
        <span>企业服务协作系统</span>
        <h1>让报单、审核与资料交付<br />清晰可追踪</h1>
        <p>后台仅向已授权的运营与审核人员开放，敏感文件访问会记录操作日志。</p>
      </div>
      <div class="login-security-note">● 本地开发环境 · 数据访问受控</div>
    </section>

    <section class="login-form-panel">
      <form class="login-card" @submit.prevent="submit">
        <div class="login-heading">
          <span>ADMIN CONSOLE</span>
          <h2>登录管理后台</h2>
          <p>请输入本地开发管理员账号</p>
        </div>

        <label for="username">用户名</label>
        <input
          id="username"
          v-model="username"
          name="username"
          autocomplete="username"
          placeholder="请输入用户名"
          :disabled="submitting"
        />

        <label for="password">密码</label>
        <input
          id="password"
          v-model="password"
          name="password"
          type="password"
          autocomplete="current-password"
          placeholder="请输入密码"
          :disabled="submitting"
        />

        <div v-if="errorMessage" class="login-error" role="alert">
          {{ errorMessage }}
        </div>

        <button class="login-submit" type="submit" :disabled="!canSubmit">
          {{ submitting ? "正在登录…" : "登录并进入工作台" }}
        </button>
        <p class="login-help">无法登录时，请先确认后端和 Valkey 已启动。</p>
      </form>
    </section>
  </main>
</template>
