<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <div class="app-shell">
    <div class="app-shell__noise"></div>
    <RouterView />
  </div>
</template>

<style scoped>
.app-shell {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background:
    radial-gradient(circle at top left, rgba(230, 173, 89, 0.22), transparent 24%),
    radial-gradient(circle at 85% 15%, rgba(101, 140, 184, 0.22), transparent 26%),
    linear-gradient(145deg, #f7f1e8 0%, #edf2f6 48%, #e3ebf4 100%);
}

.app-shell__noise {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.22;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.14) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.14) 1px, transparent 1px);
  background-size: 22px 22px;
  mask-image: radial-gradient(circle at center, black 45%, transparent 100%);
}
</style>
