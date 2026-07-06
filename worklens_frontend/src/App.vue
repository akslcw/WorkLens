<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <div class="app-shell">
    <div class="app-shell__noise"></div>
    <div class="app-shell__orb app-shell__orb--left"></div>
    <div class="app-shell__orb app-shell__orb--right"></div>
    <RouterView v-slot="{ Component, route }">
      <Transition name="page-fade" mode="out-in">
        <component :is="Component" :key="route.fullPath" />
      </Transition>
    </RouterView>
  </div>
</template>

<style scoped>
.app-shell {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  isolation: isolate;
  background:
    radial-gradient(circle at top left, rgba(230, 173, 89, 0.24), transparent 24%),
    radial-gradient(circle at 88% 12%, rgba(101, 140, 184, 0.24), transparent 28%),
    linear-gradient(145deg, var(--wl-sand) 0%, var(--wl-mist) 48%, var(--wl-blue-50) 100%);
}

.app-shell__noise {
  position: absolute;
  inset: 0;
  z-index: -2;
  pointer-events: none;
  opacity: 0.22;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.14) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.14) 1px, transparent 1px);
  background-size: 22px 22px;
  mask-image: radial-gradient(circle at center, black 45%, transparent 100%);
}

.app-shell__orb {
  position: fixed;
  z-index: -1;
  width: 360px;
  height: 360px;
  border-radius: 999px;
  pointer-events: none;
  filter: blur(18px);
  opacity: 0.34;
}

.app-shell__orb--left {
  left: -140px;
  bottom: 8%;
  background: radial-gradient(circle, rgba(193, 125, 63, 0.42), transparent 66%);
}

.app-shell__orb--right {
  right: -120px;
  top: 34%;
  background: radial-gradient(circle, rgba(70, 97, 123, 0.36), transparent 68%);
}

.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.page-fade-enter-from,
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

@media (prefers-reduced-motion: reduce) {
  .page-fade-enter-active,
  .page-fade-leave-active {
    transition: none;
  }
}
</style>
