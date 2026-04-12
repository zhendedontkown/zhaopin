<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import client from '../api/client'
import { defaultRouteByRole, useAuthStore } from '../stores/auth'

type AuthMode = 'login' | 'register'
type RegisterRole = 'company' | 'jobseeker'

interface DemoAccount {
  label: string
  account: string
  password: string
}

const router = useRouter()
const authStore = useAuthStore()

const mode = ref<AuthMode>('login')
const registerRole = ref<RegisterRole>('jobseeker')
const submitting = ref(false)

const demoAccounts: DemoAccount[] = [
  { label: '管理员', account: 'admin@recruitment.local', password: '123456' },
  { label: '企业用户', account: 'hr@futuretech.com', password: '123456' },
  { label: '求职者', account: 'alice@example.com', password: '123456' },
]

const sidePanelContent = {
  login: {
    title: '智能招聘协同平台',
    points: ['岗位发布、筛选与投递处理集中完成', '实时消息通知，招聘进度全程可视化'],
  },
  register: {
    title: '构建智能化招聘协同平台',
    points: ['简历、岗位与投递记录全流程联动', '多角色权限清晰，审核与状态流转明确', '在线沟通、进度追踪与后台管理协同顺畅'],
  },
}

const loginForm = reactive({
  account: '',
  password: '',
})

const companyForm = reactive({
  companyName: '',
  unifiedSocialCreditCode: '',
  contactPerson: '',
  phone: '',
  email: '',
  password: '',
})

const jobseekerForm = reactive({
  fullName: '',
  phone: '',
  email: '',
  password: '',
})

function resetLoginForm(options?: { preserveAccount?: boolean }) {
  const preserveAccount = options?.preserveAccount ?? false
  const nextAccount = preserveAccount ? loginForm.account : ''
  loginForm.account = nextAccount
  loginForm.password = ''
}

function resetJobseekerForm() {
  jobseekerForm.fullName = ''
  jobseekerForm.phone = ''
  jobseekerForm.email = ''
  jobseekerForm.password = ''
}

function resetCompanyForm() {
  companyForm.companyName = ''
  companyForm.unifiedSocialCreditCode = ''
  companyForm.contactPerson = ''
  companyForm.phone = ''
  companyForm.email = ''
  companyForm.password = ''
}

function switchMode(nextMode: AuthMode) {
  if (nextMode === 'register') {
    resetJobseekerForm()
    resetCompanyForm()
  } else {
    resetLoginForm({ preserveAccount: true })
  }
  mode.value = nextMode
}

function switchRegisterRole(nextRole: RegisterRole) {
  if (nextRole === registerRole.value) {
    return
  }

  if (nextRole === 'jobseeker') {
    resetJobseekerForm()
  } else {
    resetCompanyForm()
  }

  registerRole.value = nextRole
}

function useDemoAccount(demo: DemoAccount) {
  mode.value = 'login'
  loginForm.account = demo.account
  loginForm.password = demo.password
}

void demoAccounts
void useDemoAccount

async function submitLogin() {
  submitting.value = true
  try {
    await authStore.login(loginForm)
    ElMessage.success('登录成功')
    await router.push(defaultRouteByRole(authStore.role))
  } finally {
    submitting.value = false
  }
}

async function submitCompanyRegister() {
  submitting.value = true
  try {
    const registeredEmail = companyForm.email
    await client.post('/auth/company/register', companyForm)
    ElMessage.success('企业账号已创建，请等待管理员审核')
    switchMode('login')
    loginForm.account = registeredEmail
    loginForm.password = ''
    resetCompanyForm()
  } finally {
    submitting.value = false
  }
}

async function submitJobseekerRegister() {
  submitting.value = true
  try {
    const registeredEmail = jobseekerForm.email
    await client.post('/auth/jobseeker/register', jobseekerForm)
    ElMessage.success('求职者账号创建成功，请登录后继续完善信息')
    switchMode('login')
    loginForm.account = registeredEmail
    loginForm.password = ''
    resetJobseekerForm()
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-shell">
    <div class="auth-blur auth-blur-a" />
    <div class="auth-blur auth-blur-b" />
    <div class="auth-noise" />

    <main class="auth-stage">
      <section class="auth-card" :class="{ 'is-register': mode === 'register' }">
        <aside class="auth-side">
          <div class="auth-side__aurora auth-side__aurora-a" />
          <div class="auth-side__aurora auth-side__aurora-b" />
          <div class="auth-side__mesh" />
          <div class="auth-side__ring auth-side__ring-a" />
          <div class="auth-side__ring auth-side__ring-b" />
          <div class="auth-side__line auth-side__line-a" />
          <div class="auth-side__line auth-side__line-b" />
          <Transition name="side-swap" mode="out-in">
            <div :key="mode" class="auth-side__inner">
              <span class="auth-side__badge">Recruitment Workspace</span>
              <h2>{{ mode === 'login' ? sidePanelContent.login.title : sidePanelContent.register.title }}</h2>
              <div class="auth-side__points">
                <div
                  v-for="point in mode === 'login' ? sidePanelContent.login.points : sidePanelContent.register.points"
                  :key="point"
                  class="auth-side__point"
                >
                  <span class="auth-side__dot" />
                  <span>{{ point }}</span>
                </div>
              </div>
            </div>
          </Transition>
        </aside>

        <section class="auth-pane">
          <Transition name="pane-swap" mode="out-in">
            <div v-if="mode === 'login'" key="login" class="auth-pane__content">
              <header class="auth-pane__head">
                <span class="auth-pane__eyebrow">Login</span>
                <h1>登录</h1>
              </header>

              <el-form class="auth-form" label-position="top" @submit.prevent="submitLogin">
                <el-form-item label="账号">
                  <el-input v-model="loginForm.account" autocomplete="username" placeholder="邮箱或手机号" />
                </el-form-item>
                <el-form-item label="密码">
                  <el-input
                    v-model="loginForm.password"
                    autocomplete="current-password"
                    type="password"
                    show-password
                    placeholder="请输入密码"
                  />
                </el-form-item>
                <el-button class="auth-submit" type="primary" native-type="submit" :loading="submitting">
                  登录
                </el-button>
              </el-form>

              <footer class="auth-pane__foot">
                <span>没有账号？</span>
                <button type="button" class="auth-text-btn" @click="switchMode('register')">立即注册</button>
              </footer>
            </div>

            <div v-else key="register" class="auth-pane__content">
              <header class="auth-pane__head">
                <span class="auth-pane__eyebrow">Registration</span>
                <h1>注册</h1>
              </header>

              <div class="auth-role-switch">
                <button
                  type="button"
                  class="auth-role-switch__item"
                  :class="{ 'is-active': registerRole === 'jobseeker' }"
                  @click="switchRegisterRole('jobseeker')"
                >
                  求职者
                </button>
                <button
                  type="button"
                  class="auth-role-switch__item"
                  :class="{ 'is-active': registerRole === 'company' }"
                  @click="switchRegisterRole('company')"
                >
                  企业用户
                </button>
              </div>

              <Transition name="form-swap" mode="out-in">
                <el-form
                  v-if="registerRole === 'jobseeker'"
                  key="jobseeker"
                  class="auth-form"
                  label-position="top"
                  @submit.prevent="submitJobseekerRegister"
                >
                  <div class="auth-form__grid">
                    <el-form-item label="姓名">
                      <el-input v-model="jobseekerForm.fullName" placeholder="请输入姓名" />
                    </el-form-item>
                    <el-form-item label="手机号">
                      <el-input v-model="jobseekerForm.phone" placeholder="请输入手机号" />
                    </el-form-item>
                  </div>
                  <div class="auth-form__grid">
                    <el-form-item label="邮箱">
                      <el-input v-model="jobseekerForm.email" autocomplete="email" placeholder="请输入邮箱" />
                    </el-form-item>
                    <el-form-item label="密码">
                      <el-input
                        v-model="jobseekerForm.password"
                        autocomplete="new-password"
                        type="password"
                        show-password
                        placeholder="8 到 32 位密码"
                      />
                    </el-form-item>
                  </div>
                  <el-button class="auth-submit" type="primary" native-type="submit" :loading="submitting">
                    创建求职者账号
                  </el-button>
                </el-form>

                <el-form
                  v-else
                  key="company"
                  class="auth-form"
                  label-position="top"
                  @submit.prevent="submitCompanyRegister"
                >
                  <div class="auth-form__grid">
                    <el-form-item label="企业名称">
                      <el-input v-model="companyForm.companyName" placeholder="请输入企业名称" />
                    </el-form-item>
                    <el-form-item label="联系人">
                      <el-input v-model="companyForm.contactPerson" placeholder="请输入联系人姓名" />
                    </el-form-item>
                  </div>
                  <div class="auth-form__grid">
                    <el-form-item label="统一社会信用代码">
                      <el-input
                        v-model="companyForm.unifiedSocialCreditCode"
                        maxlength="18"
                        placeholder="18 位统一社会信用代码"
                      />
                    </el-form-item>
                    <el-form-item label="手机号">
                      <el-input v-model="companyForm.phone" placeholder="请输入手机号" />
                    </el-form-item>
                  </div>
                  <div class="auth-form__grid">
                    <el-form-item label="邮箱">
                      <el-input v-model="companyForm.email" autocomplete="email" placeholder="请输入企业邮箱" />
                    </el-form-item>
                    <el-form-item label="密码">
                      <el-input
                        v-model="companyForm.password"
                        autocomplete="new-password"
                        type="password"
                        show-password
                        placeholder="8 到 32 位密码"
                      />
                    </el-form-item>
                  </div>
                  <el-button class="auth-submit" type="primary" native-type="submit" :loading="submitting">
                    提交企业注册
                  </el-button>
                </el-form>
              </Transition>

              <footer class="auth-pane__foot">
                <span>已经有账号？</span>
                <button type="button" class="auth-text-btn" @click="switchMode('login')">返回登录</button>
              </footer>
            </div>
          </Transition>
        </section>
      </section>
    </main>
  </div>
</template>

<style scoped>
.auth-shell {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background:
    radial-gradient(circle at 15% 18%, rgba(169, 193, 255, 0.5), transparent 24%),
    radial-gradient(circle at 88% 82%, rgba(153, 207, 255, 0.42), transparent 22%),
    linear-gradient(135deg, #f6f9ff 0%, #edf4ff 52%, #f7fbff 100%);
  background-size: 100% 100%, 100% 100%, 180% 180%;
  animation: shell-gradient 20s ease-in-out infinite alternate;
}

.auth-shell::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(130deg, rgba(255, 255, 255, 0.26), transparent 42%),
    radial-gradient(circle at 28% 24%, rgba(255, 255, 255, 0.24), transparent 18%);
  opacity: 0.8;
  pointer-events: none;
}

.auth-stage {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 28px 18px;
}

.auth-card {
  position: relative;
  display: grid;
  grid-template-columns: minmax(240px, 0.72fr) minmax(460px, 1.18fr);
  width: min(980px, 100%);
  min-height: 640px;
  border-radius: 34px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.72);
  background: rgba(255, 255, 255, 0.58);
  box-shadow:
    0 28px 90px rgba(82, 111, 171, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(10px);
  transition:
    grid-template-columns 480ms cubic-bezier(0.22, 1, 0.36, 1),
    background-color 320ms ease,
    box-shadow 320ms ease,
    transform 320ms ease;
}

.auth-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(120deg, rgba(255, 255, 255, 0.24), transparent 34%, rgba(132, 162, 255, 0.08) 68%, transparent 100%);
  opacity: 0.95;
  mix-blend-mode: screen;
  pointer-events: none;
}

.auth-card.is-register {
  grid-template-columns: minmax(460px, 1.18fr) minmax(240px, 0.72fr);
}

.auth-side {
  position: relative;
  display: grid;
  place-items: center;
  padding: 34px 28px;
  background:
    linear-gradient(160deg, #90abff 0%, #7899f4 38%, #6f8de4 72%, #88a7ff 100%);
  background-size: 180% 180%;
  animation: side-gradient 14s ease-in-out infinite;
}

.auth-card.is-register .auth-side {
  order: 2;
}

.auth-side::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at top left, rgba(255, 255, 255, 0.26), transparent 30%),
    radial-gradient(circle at bottom right, rgba(255, 255, 255, 0.12), transparent 28%);
  pointer-events: none;
}

.auth-side::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(150deg, rgba(255, 255, 255, 0.08), transparent 26%, transparent 72%, rgba(255, 255, 255, 0.06)),
    radial-gradient(circle at 74% 18%, rgba(255, 255, 255, 0.12), transparent 16%);
  mix-blend-mode: screen;
  opacity: 0.92;
  pointer-events: none;
}

.auth-side__aurora,
.auth-side__mesh,
.auth-side__ring,
.auth-side__line {
  position: absolute;
  pointer-events: none;
}

.auth-side__aurora {
  border-radius: 999px;
  filter: blur(10px);
  opacity: 0.44;
  mix-blend-mode: screen;
}

.auth-side__aurora-a {
  top: 14%;
  left: 8%;
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.22), rgba(255, 255, 255, 0));
  animation: side-aurora-a 12s ease-in-out infinite;
}

.auth-side__aurora-b {
  right: 6%;
  bottom: 10%;
  width: 240px;
  height: 240px;
  background: radial-gradient(circle, rgba(188, 227, 255, 0.24), rgba(188, 227, 255, 0));
  animation: side-aurora-b 14s ease-in-out infinite;
}

.auth-side__mesh {
  inset: 12% 10%;
  border-radius: 28px;
  background:
    linear-gradient(rgba(255, 255, 255, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.08) 1px, transparent 1px);
  background-size: 32px 32px;
  mask-image: radial-gradient(circle at center, black 34%, transparent 86%);
  opacity: 0.18;
}

.auth-side__ring {
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  opacity: 0.7;
}

.auth-side__ring-a {
  top: 16%;
  right: 12%;
  width: 120px;
  height: 120px;
}

.auth-side__ring-b {
  left: 10%;
  bottom: 16%;
  width: 180px;
  height: 180px;
  border-color: rgba(255, 255, 255, 0.12);
}

.auth-side__line {
  height: 1px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0), rgba(255, 255, 255, 0.22), rgba(255, 255, 255, 0));
  opacity: 0.55;
}

.auth-side__line-a {
  top: 24%;
  left: 18%;
  width: 140px;
  transform: rotate(-26deg);
}

.auth-side__line-b {
  right: 14%;
  bottom: 24%;
  width: 170px;
  transform: rotate(24deg);
}

.auth-side__inner {
  position: relative;
  z-index: 2;
  display: grid;
  gap: 18px;
  max-width: 248px;
  width: 100%;
  color: #fff;
  text-align: center;
}

.auth-side__badge {
  justify-self: center;
  padding: 8px 14px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  background: rgba(255, 255, 255, 0.12);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.auth-side__inner h2 {
  margin: 0;
  font-size: clamp(25px, 2.5vw, 32px);
  line-height: 1.15;
  letter-spacing: -0.03em;
  white-space: normal;
  text-wrap: balance;
  max-width: 10ch;
  margin-inline: auto;
}

.auth-side__inner p {
  margin: 0;
  color: rgba(255, 255, 255, 0.88);
  line-height: 1.7;
}

.auth-side__points {
  display: grid;
  gap: 14px;
  margin-top: 10px;
  text-align: left;
}

.auth-side__point {
  display: grid;
  grid-template-columns: 10px 1fr;
  gap: 12px;
  align-items: start;
  padding: 14px 16px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.92);
  font-size: 12px;
  line-height: 1.65;
}

.auth-side__dot {
  width: 8px;
  height: 8px;
  margin-top: 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 0 12px rgba(255, 255, 255, 0.42);
}

.auth-pane {
  display: grid;
  align-items: center;
  padding: 40px 44px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.72), rgba(255, 255, 255, 0.58)),
    linear-gradient(135deg, rgba(255, 255, 255, 0.18), rgba(121, 151, 243, 0.06));
  backdrop-filter: blur(8px);
}

.auth-pane__content {
  display: grid;
  gap: 18px;
  will-change: transform, opacity, filter;
}

.auth-pane__head {
  display: grid;
  gap: 10px;
}

.auth-pane__eyebrow {
  color: #7a8fb8;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.auth-pane__head h1 {
  margin: 0;
  font-size: 42px;
  line-height: 1;
  letter-spacing: -0.04em;
  color: #171f35;
}

.auth-pane__head p {
  margin: 0;
  color: #7b879f;
  line-height: 1.7;
}

.auth-role-switch {
  display: inline-grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  padding: 6px;
  border-radius: 16px;
  background: rgba(120, 153, 244, 0.1);
}

.auth-role-switch__item {
  cursor: pointer;
  min-width: 0;
  padding: 12px 18px;
  border: 0;
  border-radius: 12px;
  background: transparent;
  color: #66748f;
  font-weight: 700;
  transition:
    background-color 180ms ease,
    color 180ms ease,
    transform 180ms ease;
}

.auth-role-switch__item.is-active {
  background: #fff;
  color: #4d67c8;
  box-shadow: 0 8px 18px rgba(120, 153, 244, 0.14);
}

.auth-form {
  margin-top: 4px;
}

.auth-form__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 14px;
}

.auth-submit {
  width: 100%;
  height: 48px;
  margin-top: 10px;
}

.auth-pane__foot {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #7b879f;
  font-size: 14px;
}

.auth-text-btn {
  cursor: pointer;
  border: 0;
  padding: 0;
  background: transparent;
  color: #6f8de4;
  font-weight: 700;
}

.side-swap-enter-active,
.side-swap-leave-active,
.pane-swap-enter-active,
.pane-swap-leave-active,
.form-swap-enter-active,
.form-swap-leave-active {
  transition:
    opacity 320ms cubic-bezier(0.22, 1, 0.36, 1),
    transform 420ms cubic-bezier(0.22, 1, 0.36, 1),
    filter 320ms ease;
}

.side-swap-enter-from,
.side-swap-leave-to {
  opacity: 0;
  transform: translateY(10px) scale(0.98);
  filter: blur(8px);
}

.pane-swap-enter-from,
.pane-swap-leave-to {
  opacity: 0;
  transform: translateX(18px) scale(0.985);
  filter: blur(10px);
}

.form-swap-enter-from,
.form-swap-leave-to {
  opacity: 0;
  transform: translateY(12px);
  filter: blur(8px);
}

.auth-blur,
.auth-noise {
  position: absolute;
  pointer-events: none;
}

.auth-blur {
  border-radius: 999px;
  filter: blur(28px);
  opacity: 0.75;
}

.auth-blur-a {
  top: 4%;
  left: 2%;
  width: 320px;
  height: 320px;
  background: radial-gradient(circle, rgba(142, 173, 255, 0.36), rgba(142, 173, 255, 0));
  animation: auth-float-a 16s ease-in-out infinite;
}

.auth-blur-b {
  right: 0;
  bottom: 8%;
  width: 360px;
  height: 360px;
  background: radial-gradient(circle, rgba(152, 218, 255, 0.3), rgba(152, 218, 255, 0));
  animation: auth-float-b 18s ease-in-out infinite;
}

.auth-noise {
  inset: 0;
  background:
    linear-gradient(rgba(255, 255, 255, 0.12), rgba(255, 255, 255, 0.12)),
    linear-gradient(rgba(143, 162, 212, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(143, 162, 212, 0.08) 1px, transparent 1px);
  background-size: auto, 42px 42px, 42px 42px;
  mask-image: radial-gradient(circle at center, black 20%, transparent 76%);
}

:deep(.auth-form .el-form-item) {
  margin-bottom: 16px;
}

:deep(.auth-form .el-form-item__label) {
  color: #42506d;
  font-weight: 600;
}

:deep(.auth-form .el-input__wrapper) {
  min-height: 48px;
  border-radius: 14px !important;
  background: rgba(244, 246, 252, 0.96) !important;
  box-shadow: 0 0 0 1px rgba(120, 153, 244, 0.08) inset !important;
}

:deep(.auth-form .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px rgba(111, 141, 228, 0.28) inset !important;
}

:deep(.auth-submit.el-button--primary) {
  --el-button-bg-color: #7899f4 !important;
  --el-button-border-color: #7899f4 !important;
  --el-button-hover-bg-color: #6f8de4 !important;
  --el-button-hover-border-color: #6f8de4 !important;
  --el-button-active-bg-color: #607dd6 !important;
  --el-button-active-border-color: #607dd6 !important;
}

@keyframes auth-float-a {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }

  50% {
    transform: translate3d(28px, 20px, 0);
  }
}

@keyframes auth-float-b {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }

  50% {
    transform: translate3d(-24px, -20px, 0);
  }
}

@keyframes shell-gradient {
  0% {
    background-position: center center, center center, 0% 50%;
  }

  100% {
    background-position: center center, center center, 100% 50%;
  }
}

@keyframes side-gradient {
  0% {
    background-position: 0% 50%;
  }

  50% {
    background-position: 100% 50%;
  }

  100% {
    background-position: 20% 50%;
  }
}

@keyframes side-aurora-a {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }

  50% {
    transform: translate3d(14px, -10px, 0) scale(1.08);
  }
}

@keyframes side-aurora-b {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }

  50% {
    transform: translate3d(-16px, 12px, 0) scale(1.06);
  }
}

@media (max-width: 960px) {
  .auth-card,
  .auth-card.is-register {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .auth-card.is-register .auth-side {
    order: 0;
  }

  .auth-side,
  .auth-pane {
    padding: 28px 24px;
  }

  .auth-side__inner {
    max-width: 100%;
  }
}

@media (max-width: 640px) {
  .auth-stage {
    padding: 16px;
  }

  .auth-card {
    border-radius: 26px;
  }

  .auth-form__grid {
    grid-template-columns: 1fr;
  }

  .auth-pane__head h1 {
    font-size: 34px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .auth-shell,
  .auth-blur-a,
  .auth-blur-b,
  .auth-side,
  .auth-side__aurora-a,
  .auth-side__aurora-b,
  .auth-role-switch__item,
  .side-swap-enter-active,
  .side-swap-leave-active,
  .pane-swap-enter-active,
  .pane-swap-leave-active,
  .form-swap-enter-active,
  .form-swap-leave-active {
    animation: none;
    transition: none;
  }
}
</style>
