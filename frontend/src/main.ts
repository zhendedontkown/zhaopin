import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './style.css'
import App from './App.vue'
import { router } from './router'
import { AUTH_STORAGE_KEY, AUTH_TOKEN_KEY } from './stores/auth'

// Always force a fresh login when the app is opened or refreshed.
localStorage.removeItem(AUTH_STORAGE_KEY)
localStorage.removeItem(AUTH_TOKEN_KEY)

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

app.mount('#app')
