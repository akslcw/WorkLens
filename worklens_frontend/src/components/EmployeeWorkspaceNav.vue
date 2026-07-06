<script setup lang="ts">
import { RouterLink } from 'vue-router'

defineProps<{
  current: 'dashboard' | 'access'
}>()
</script>

<template>
  <nav class="workspace-nav" aria-label="Employee workspace navigation">
    <RouterLink
      class="workspace-link"
      :class="{ 'workspace-link--active': current === 'dashboard' }"
      to="/employee"
    >
      个人效率
    </RouterLink>
    <RouterLink
      class="workspace-link"
      :class="{ 'workspace-link--active': current === 'access' }"
      to="/employee/access-records"
    >
      审批与记录
    </RouterLink>
  </nav>
</template>

<style scoped>
.workspace-nav {
  display: inline-flex;
  gap: 10px;
  padding: 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(29, 46, 71, 0.1);
}

.workspace-link {
  padding: 10px 16px;
  border-radius: 999px;
  text-decoration: none;
  color: #42566f;
  font-size: 0.92rem;
  transition: background 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

.workspace-link:hover {
  transform: translateY(-1px);
}

.workspace-link--active {
  background: #1d2e47;
  color: #eef5fc;
}
</style>
