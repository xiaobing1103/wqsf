<script setup lang="ts">
defineProps<{
  name:
    | "alert"
    | "arrow-right"
    | "bell"
    | "building"
    | "cases"
    | "check-circle"
    | "company"
    | "dashboard"
    | "download"
    | "logout"
    | "more"
    | "plus"
    | "processing"
    | "refresh"
    | "search"
    | "templates"
    | "upload"
    | "users";
}>();
</script>

<template>
  <svg
    class="admin-icon"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.8"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <template v-if="name === 'dashboard'">
      <rect x="3" y="3" width="7" height="7" rx="1.5" />
      <rect x="14" y="3" width="7" height="7" rx="1.5" />
      <rect x="3" y="14" width="7" height="7" rx="1.5" />
      <rect x="14" y="14" width="7" height="7" rx="1.5" />
    </template>
    <template v-else-if="name === 'cases'">
      <path d="M9 5h10a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V9" />
      <path d="M9 3H5a2 2 0 0 0-2 2v4h6z" />
      <path d="M9 13h8M9 17h6" />
    </template>
    <template v-else-if="name === 'company' || name === 'building'">
      <path d="M4 21V5a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v16" />
      <path d="M17 9h2a2 2 0 0 1 2 2v10M8 7h5M8 11h5M8 15h5M9 21v-2h3v2M2 21h20" />
    </template>
    <template v-else-if="name === 'download'">
      <path d="M12 3v12M7 10l5 5 5-5M5 21h14" />
    </template>
    <template v-else-if="name === 'upload'">
      <path d="M12 21V9M7 14l5-5 5 5M5 3h14" />
    </template>
    <template v-else-if="name === 'templates'">
      <path d="m12 2 8 4-8 4-8-4zM4 10l8 4 8-4M4 14l8 4 8-4" />
    </template>
    <template v-else-if="name === 'users'">
      <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
      <circle cx="9" cy="7" r="4" />
      <path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
    </template>
    <template v-else-if="name === 'bell'">
      <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" />
    </template>
    <template v-else-if="name === 'logout'">
      <path d="M10 17l5-5-5-5M15 12H3M14 3h5a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-5" />
    </template>
    <template v-else-if="name === 'more'">
      <circle cx="5" cy="12" r="1.3" fill="currentColor" stroke="none" />
      <circle cx="12" cy="12" r="1.3" fill="currentColor" stroke="none" />
      <circle cx="19" cy="12" r="1.3" fill="currentColor" stroke="none" />
    </template>
    <template v-else-if="name === 'plus'">
      <path d="M12 5v14M5 12h14" />
    </template>
    <template v-else-if="name === 'refresh'">
      <path d="M20 11a8 8 0 1 0 1.5 5M20 4v7h-7" />
    </template>
    <template v-else-if="name === 'check-circle'">
      <circle cx="12" cy="12" r="9" />
      <path d="m8 12 2.5 2.5L16 9" />
    </template>
    <template v-else-if="name === 'processing'">
      <path d="M21 12a9 9 0 1 1-2.64-6.36M21 3v6h-6" />
    </template>
    <template v-else-if="name === 'alert'">
      <path d="M10.3 3.7 2.4 18a2 2 0 0 0 1.75 3h15.7a2 2 0 0 0 1.75-3L13.7 3.7a2 2 0 0 0-3.4 0zM12 9v4M12 17h.01" />
    </template>
    <template v-else-if="name === 'search'">
      <circle cx="11" cy="11" r="7" />
      <path d="m20 20-4-4" />
    </template>
    <template v-else-if="name === 'arrow-right'">
      <path d="M5 12h14M13 6l6 6-6 6" />
    </template>
  </svg>
</template>
