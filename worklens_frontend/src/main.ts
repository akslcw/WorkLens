import { createApp } from 'vue'
import App from './App.vue'
import { createAppRouter } from './router'
import './style.css'

const app = createApp(App)
const router = createAppRouter()

app.use(router)
app.mount('#app')
