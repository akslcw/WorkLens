import { createApp } from 'vue'
import App from './App.vue'
import { createAppRouter } from './router'
import { setUnauthorizedHandler } from './api/http'
import './style.css'

const app = createApp(App)
const router = createAppRouter()
setUnauthorizedHandler(() => {
  void router.replace('/login')
})

app.use(router)
app.mount('#app')
