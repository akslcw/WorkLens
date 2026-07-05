<script setup lang="ts">
import { clearSession, readStoredSession } from '../auth/session'
import { useRouter } from 'vue-router'

const router = useRouter()
const session = readStoredSession()

async function handleLogout() {
  clearSession()
  await router.replace('/login')
}
</script>

<template>
  <main class="home-layout home-layout--manager">
    <section class="hero-card">
      <div>
        <p class="eyebrow">Manager Home</p>
        <h1>团队视角占位页</h1>
      </div>
      <button class="ghost-button" type="button" @click="handleLogout">退出登录</button>
    </section>

    <section class="detail-grid">
      <article class="detail-card">
        <p class="eyebrow">Current Session</p>
        <h2>{{ session?.username ?? 'manager' }}</h2>
        <p>已登录为 `MANAGER`。后续模块会在这里接入员工档案、团队聚合面板、审计申请与团队报告。</p>
      </article>

      <article class="detail-card detail-card--accent">
        <p class="eyebrow">Routing Check</p>
        <h2>角色分流已生效</h2>
        <p>当前页面只验证 F1 的入口能力：登录成功后，管理者进入专属首页；未登录访问会被路由守卫拦回登录页。</p>
      </article>
    </section>
  </main>
</template>

<style scoped>
.home-layout {
  position: relative;
  z-index: 1;
  width: min(1100px, calc(100% - 40px));
  min-height: 100vh;
  margin: 0 auto;
  padding: 28px 0 36px;
}

.hero-card,
.detail-card {
  border-radius: 30px;
  border: 1px solid rgba(29, 46, 71, 0.12);
  box-shadow: 0 24px 64px rgba(73, 86, 108, 0.14);
  backdrop-filter: blur(16px);
}

.hero-card {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 18px;
  padding: 28px 30px;
  background:
    linear-gradient(125deg, rgba(255, 250, 243, 0.9), rgba(241, 228, 207, 0.88)),
    linear-gradient(155deg, #fbf3e9 0%, #eef4fb 100%);
}

.eyebrow {
  margin: 0 0 10px;
  color: #9e6538;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  font-size: 0.76rem;
}

.hero-card h1,
.detail-card h2 {
  margin: 0;
  font-family: Georgia, 'Palatino Linotype', serif;
  letter-spacing: -0.04em;
  color: #1d2e47;
}

.hero-card h1 {
  font-size: clamp(2.4rem, 4vw, 4.6rem);
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
  margin-top: 22px;
}

.detail-card {
  padding: 24px;
  background: rgba(255, 255, 255, 0.8);
}

.detail-card--accent {
  background: linear-gradient(160deg, rgba(29, 46, 71, 0.92), rgba(55, 82, 114, 0.92));
}

.detail-card--accent,
.detail-card--accent h2,
.detail-card--accent .eyebrow {
  color: #eef5fc;
}

.detail-card p {
  margin: 12px 0 0;
  line-height: 1.7;
  color: #57677c;
}

.detail-card--accent p {
  color: rgba(238, 245, 252, 0.82);
}

.ghost-button {
  border: 1px solid rgba(29, 46, 71, 0.14);
  border-radius: 999px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.6);
  color: #1d2e47;
  cursor: pointer;
}

@media (max-width: 860px) {
  .home-layout {
    width: min(100%, calc(100% - 24px));
    padding-top: 18px;
  }

  .hero-card,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .hero-card {
    flex-direction: column;
    align-items: start;
  }
}
</style>
