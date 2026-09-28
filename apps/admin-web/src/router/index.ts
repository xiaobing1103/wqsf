import { createRouter, createWebHistory } from "vue-router";
import AdminLayout from "../layouts/AdminLayout.vue";
import { pinia } from "../stores";
import { useAuthStore } from "../stores/auth";
import ForbiddenView from "../views/ForbiddenView.vue";
import LoginView from "../views/login/index.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/dashboard" },
    {
      path: "/login",
      name: "login",
      component: LoginView,
      meta: { public: true },
    },
    {
      path: "/forbidden",
      name: "forbidden",
      component: ForbiddenView,
      meta: { requiresAuth: true },
    },
    {
      path: "/dashboard",
      name: "dashboard",
      component: AdminLayout,
      meta: { requiresAuth: true, permission: "dashboard:view" },
    },
    {
      path: "/cases",
      name: "cases",
      component: AdminLayout,
      meta: { requiresAuth: true, permission: "case:view" },
    },
    {
      path: "/companies",
      name: "companies",
      component: AdminLayout,
      meta: { requiresAuth: true, permission: "company:view" },
    },
    {
      path: "/exports",
      name: "exports",
      component: AdminLayout,
      meta: { requiresAuth: true, permission: "export:excel" },
    },
    {
      path: "/products",
      name: "products",
      component: AdminLayout,
      meta: { requiresAuth: true, permission: "product:view" },
    },
    { path: "/:pathMatch(.*)*", redirect: "/dashboard" },
  ],
});

router.beforeEach(async (to) => {
  const auth = useAuthStore(pinia);

  if (to.meta.public) {
    if (
      to.name === "login" &&
      auth.token &&
      (auth.user || (await auth.restoreSession()))
    ) {
      return "/dashboard";
    }
    return true;
  }

  if (to.meta.requiresAuth) {
    if (!auth.token) return { name: "login", query: { redirect: to.fullPath } };
    if (!auth.user && !(await auth.restoreSession())) {
      return { name: "login", query: { redirect: to.fullPath } };
    }
    const permission = to.meta.permission as string | undefined;
    if (permission && !auth.hasPermission(permission))
      return { name: "forbidden" };
  }

  return true;
});

export default router;
