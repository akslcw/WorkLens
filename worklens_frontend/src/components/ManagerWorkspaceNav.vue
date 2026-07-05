<script setup lang="ts">
import { RouterLink } from 'vue-router'

defineProps<{
  current: 'directory' | 'team'
}>()
</script>

<template>
  <nav class="workspace-nav" aria-label="Manager workspace navigation">
    <RouterLink
      class="workspace-link"
      :class="{ 'workspace-link--active': current === 'directory' }"
      to="/manager"
    >
      员工档案
    </RouterLink>
    <RouterLink
      class="workspace-link"
      :class="{ 'workspace-link--active': current === 'team' }"
      to="/manager/team"
    >
      团队效能
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
