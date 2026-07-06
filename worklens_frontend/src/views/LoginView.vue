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
    <section class="login-card" aria-labelledby="login-title">
      <h1 id="login-title">WorkLens 登录</h1>

      <form data-test="login-form" class="login-form" @submit.prevent="handleLogin">
        <label class="field">
          <span>用户名</span>
          <input
            data-test="username-input"
            v-model="credentials.username"
            type="text"
            autocomplete="username"
            placeholder="请输入用户名"
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
    </section>
  </main>
</template>

<style scoped>
.auth-layout {
  display: grid;
  place-items: center;
  min-height: 100vh;
  padding: 24px;
}

.login-card {
  width: min(100%, 420px);
  padding: 32px;
  border: 1px solid var(--wl-border);
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 18px 48px rgba(73, 86, 108, 0.12);
}

.login-card h1 {
  margin: 0 0 24px;
  color: var(--wl-ink);
  font-size: 1.55rem;
  font-weight: 700;
  line-height: 1.25;
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
  color: var(--wl-ink);
  font-size: 0.94rem;
  font-weight: 600;
}

.field input {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #cfd8e3;
  border-radius: 8px;
  background: #ffffff;
  color: var(--wl-ink);
  font-size: 1rem;
  transition: border-color 0.16s ease, box-shadow 0.16s ease;
}

.field input:focus {
  outline: none;
  border-color: var(--wl-copper);
  box-shadow: 0 0 0 3px rgba(193, 125, 63, 0.14);
}

.primary-button {
  border: none;
  border-radius: 8px;
  padding: 12px 16px;
  background: var(--wl-ink);
  color: #ffffff;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.16s ease, opacity 0.16s ease;
}

.primary-button:hover {
  background: #263b59;
}

.primary-button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.feedback {
  margin-top: 16px;
  padding: 12px 14px;
  border-radius: 8px;
  font-size: 0.94rem;
}

.feedback--error {
  background: #fff0ed;
  color: #b64131;
}

@media (max-width: 920px) {
  .auth-layout {
    padding: 16px;
  }

  .login-card {
    padding: 24px;
  }
}
</style>
