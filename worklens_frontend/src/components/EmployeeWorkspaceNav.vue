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
  display: inline-grid;
  grid-auto-flow: column;
  align-items: center;
  gap: 10px;
  padding: 4px;
  width: fit-content;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid rgba(29, 46, 71, 0.12);
}

.workspace-link {
  position: relative;
  padding: 9px 13px;
  border-radius: 8px;
  text-decoration: none;
  color: #42566f;
  font-size: 0.92rem;
  white-space: nowrap;
  transition: background 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

.workspace-link:hover {
  transform: translateY(-1px);
  background: rgba(29, 46, 71, 0.06);
}

.workspace-link--active {
  background: #1d2e47;
  color: #eef5fc;
}

@media (max-width: 720px) {
  .workspace-nav {
    grid-auto-flow: row;
    width: 100%;
    border-radius: 10px;
  }

  .workspace-link {
    width: 100%;
    text-align: center;
  }
}
</style>
