<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api/auth'
import { persistSession, resolveHomePath } from '../auth/session'

const router = useRouter()

const credentials = reactive({
  username: '',
  password: '',
})

const loggingIn = ref(false)
const errorMessage = ref('')

async function handleLogin() {
  if (loggingIn.value) {
    return
  }

  loggingIn.value = true
  errorMessage.value = ''

  try {
    const session = await login({
      username: credentials.username.trim(),
      password: credentials.password,
    })

    persistSession(session)
    credentials.password = ''
    await router.replace(resolveHomePath(session.role))
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败，请稍后重试。'
  } finally {
    loggingIn.value = false
  }
}
</script>

<template>
  <main class="auth-layout">
    <section class="intro-panel">
      <p class="eyebrow">WorkLens</p>
      <h1>把权限边界做成真正可点击的前端入口。</h1>
      <p class="intro-copy">
        管理者进入团队聚合视图，员工进入个人明细视图。前端只负责登录、跳转和展示，实际权限完全由后端判定。
      </p>

      <div class="principles">
        <article>
          <span class="tag">Manager</span>
          <strong>团队聚合</strong>
          <p>默认只能看整体趋势和统计结果，不直接落到个体明细。</p>
        </article>
        <article>
          <span class="tag">Employee</span>
          <strong>个人明细</strong>
          <p>只看自己的使用明细、审批记录和个人报告。</p>
        </article>
      </div>
    </section>

    <section class="login-panel">
      <div class="login-card">
        <div class="login-card__header">
          <p class="eyebrow">Secure Login</p>
          <span class="status-pill">Token Session</span>
        </div>

        <h2>登录到你的工作视角</h2>
        <p class="login-copy">使用后端已有账号登录，系统会按返回角色自动进入对应首页。</p>

        <form data-test="login-form" class="login-form" @submit.prevent="handleLogin">
          <label class="field">
            <span>用户名</span>
            <input
              data-test="username-input"
              v-model="credentials.username"
              type="text"
              autocomplete="username"
              placeholder="manager / employee.alice"
              required
            />
          </label>

          <label class="field">
            <span>密码</span>
            <input
              data-test="password-input"
              v-model="credentials.password"
              type="password"
              autocomplete="current-password"
              placeholder="请输入密码"
              required
            />
          </label>

          <button class="primary-button" type="submit" :disabled="loggingIn">
            {{ loggingIn ? '登录中...' : '登录' }}
          </button>
        </form>

        <p v-if="errorMessage" class="feedback feedback--error" role="alert">
          {{ errorMessage }}
        </p>
      </div>
    </section>
  </main>
</template>

<style scoped>
.auth-layout {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
  gap: 28px;
  width: min(1180px, calc(100% - 40px));
  min-height: 100vh;
  margin: 0 auto;
  padding: 32px 0;
  align-items: stretch;
}

.intro-panel,
.login-card {
  border: 1px solid rgba(28, 45, 69, 0.12);
  border-radius: 32px;
  box-shadow: 0 28px 70px rgba(78, 94, 118, 0.16);
  backdrop-filter: blur(18px);
}

.intro-panel {
  display: grid;
  align-content: space-between;
  gap: 24px;
  padding: 36px;
  background:
    linear-gradient(160deg, rgba(19, 31, 47, 0.94), rgba(41, 64, 93, 0.88)),
    linear-gradient(140deg, #182131, #33516f);
  color: #edf4fc;
}

.eyebrow {
  margin: 0 0 10px;
  color: #c99663;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  font-size: 0.76rem;
}

.intro-panel h1,
.login-card h2,
.principles strong {
  margin: 0;
  font-family: Georgia, 'Palatino Linotype', serif;
  letter-spacing: -0.04em;
}

.intro-panel h1 {
  font-size: clamp(2.6rem, 5vw, 5.2rem);
  line-height: 0.95;
  max-width: 680px;
}

.intro-copy,
.principles p,
.login-copy {
  margin: 0;
  line-height: 1.7;
}

.intro-copy {
  max-width: 620px;
  color: rgba(237, 244, 252, 0.8);
}

.principles {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.principles article {
  padding: 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.principles strong {
  display: block;
  margin-top: 14px;
  margin-bottom: 10px;
  font-size: 1.45rem;
}

.tag,
.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 7px 12px;
  border-radius: 999px;
  font-size: 0.78rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.tag {
  background: rgba(255, 255, 255, 0.12);
  color: #f5dcc2;
}

.login-panel {
  display: grid;
  align-items: center;
}

.login-card {
  padding: 30px;
  background: rgba(255, 255, 255, 0.82);
}

.login-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 22px;
}

.status-pill {
  background: #e8eef6;
  color: #35506b;
}

.login-card h2 {
  color: #1c2d45;
  font-size: 2.2rem;
}

.login-copy {
  margin-top: 12px;
  margin-bottom: 24px;
  color: #5f6f82;
}

.login-form {
  display: grid;
  gap: 16px;
}

.field {
  display: grid;
  gap: 8px;
}

.field span {
  color: #253650;
  font-size: 0.95rem;
}

.field input {
  width: 100%;
  padding: 15px 16px;
  border: 1px solid #cfdae7;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.98);
  font-size: 1rem;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.field input:focus {
  outline: none;
  border-color: #c17d3f;
  box-shadow: 0 0 0 4px rgba(193, 125, 63, 0.14);
  transform: translateY(-1px);
}

.primary-button {
  border: none;
  border-radius: 999px;
  padding: 15px 18px;
  background: linear-gradient(135deg, #1d2e47 0%, #345171 55%, #c17d3f 100%);
  color: #faf7f1;
  font-size: 0.98rem;
  cursor: pointer;
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.primary-button:hover {
  transform: translateY(-1px);
}

.primary-button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
}

.feedback {
  margin-top: 16px;
  padding: 15px 16px;
  border-radius: 18px;
}

.feedback--error {
  background: #fff0ed;
  color: #b64131;
}

@media (max-width: 920px) {
  .auth-layout {
    grid-template-columns: 1fr;
    width: min(100%, calc(100% - 24px));
    padding: 18px 0 24px;
  }

  .principles {
    grid-template-columns: 1fr;
  }
}
</style>
