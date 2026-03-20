<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'
import { roleHome } from '../../utils/menu'
import loginBackground from '../../assets/login-custom-bg.png'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  username: 's001',
  password: 's001',
})

const metrics = [
  { label: '统一课程入口', value: '12+' },
  { label: '评分规则模板', value: '多维' },
  { label: '结果看板视图', value: '实时' },
]

const features = [
  '学生可提交作品、仓库链接和项目说明',
  '系统支持匿名互评与 Rubric 结构化评分',
  '教师可统一复核结果并发布课程成绩看板',
]

const sampleAccounts = [
  {
    role: '管理员',
    account: 'admin / admin',
    description: '维护课程、作业、评分规则和成员配置',
  },
  {
    role: '教师',
    account: 't001 / t001',
    description: '查看课程提交、组织互评并完成评分发布',
  },
  {
    role: '学生',
    account: 's001 / s001',
    description: '提交项目作品、参与互评并查看评分结果',
  },
]

const roleCards = [
  {
    role: '学生',
    account: 's001',
    description: '提交作品与参与互评',
    className: 'role-card--student',
  },
  {
    role: '教师',
    account: 't001',
    description: '组织评分与结果发布',
    className: 'role-card--teacher',
  },
  {
    role: '管理员',
    account: 'admin',
    description: '维护课程与成员配置',
    className: 'role-card--admin',
  },
]

async function handleLogin() {
  const user = await authStore.loginByPassword(form)
  ElMessage.success(`欢迎回来：${user.displayName}`)
  router.push(user.firstLoginResetRequired ? '/change-password' : roleHome[user.role])
}
</script>

<template>
  <div class="auth-page" :style="{ '--login-bg': `url(${loginBackground})` }">
    <section class="auth-shell">
      <div class="auth-panel auth-panel--hero">
        <div class="workspace-brand auth-brand">
          <div class="workspace-brand__mark">C</div>
          <div>
            <strong>课程项目评分平台</strong>
            <small>课程作业提交、互评与评分中心</small>
          </div>
        </div>

        <span class="page-head__eyebrow auth-eyebrow">平台概览</span>
        <h1 class="page-head__title auth-title">项目提交、互评和评分流程一体化</h1>
        <p class="page-head__description auth-description">
          平台将课程作业、项目提交、匿名互评、教师复核和结果发布整合在同一系统中，
          让学生、教师和管理员都能围绕同一份课程数据协作。
        </p>

        <div class="page-head__stats">
          <span v-for="item in metrics" :key="item.label">{{ item.label }} · {{ item.value }}</span>
        </div>

        <div class="stack" style="margin-top: 26px;">
          <div class="section-card auth-card auth-card--accent">
            <span class="section-eyebrow">核心能力</span>
            <div class="stack auth-feature-list">
              <div v-for="item in features" :key="item" class="workspace-toolbar__meta auth-meta-chip">{{ item }}</div>
            </div>
          </div>

          <div class="section-card auth-card">
            <span class="section-eyebrow">账号进入</span>
            <div class="stack auth-account-list">
              <div v-for="item in sampleAccounts" :key="item.role" class="collection-card login-account-card">
                <div class="panel-header">
                  <strong>{{ item.role }}</strong>
                  <span class="layout-chip">{{ item.account }}</span>
                </div>
                <p class="section-subtitle">{{ item.description }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="auth-panel auth-panel--form">
        <div>
          <div class="panel-header">
            <div>
              <span class="section-eyebrow">账号登录</span>
              <h3 class="auth-form-title">进入课程项目评分平台</h3>
            </div>
            <span class="layout-chip">蓝白数据面板</span>
          </div>

          <p class="section-subtitle auth-form-description">
            登录后会根据当前角色进入对应工作台，直接查看课程、作业、评分和结果数据。
          </p>

          <el-form :model="form" label-position="top" class="auth-form">
            <el-form-item label="用户名">
              <el-input v-model="form.username" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="form.password" type="password" show-password @keyup.enter="handleLogin" />
            </el-form-item>
            <el-button type="primary" class="auth-submit" @click="handleLogin">进入平台</el-button>
          </el-form>
        </div>

        <div class="role-showcase">
          <article
            v-for="item in roleCards"
            :key="item.role"
            :class="['role-card', item.className]"
          >
            <div class="role-card__avatar">
              <div class="role-figure">
                <span class="role-figure__halo"></span>
                <span class="role-figure__head"></span>
                <span class="role-figure__body"></span>
                <span class="role-figure__accent"></span>
              </div>
            </div>
            <div class="role-card__content">
              <span class="muted">{{ item.role }}</span>
              <strong>{{ item.account }}</strong>
              <p>{{ item.description }}</p>
            </div>
          </article>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.auth-page {
  position: relative;
  min-height: 100vh;
  padding: 28px;
  background:
    linear-gradient(180deg, rgba(233, 242, 255, 0.46) 0%, rgba(240, 246, 255, 0.62) 100%),
    var(--login-bg) center center / cover no-repeat;
}

.auth-page::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 50% 36%, rgba(255, 255, 255, 0.68) 0%, rgba(255, 255, 255, 0.34) 24%, rgba(255, 255, 255, 0.08) 48%, rgba(255, 255, 255, 0) 70%),
    linear-gradient(180deg, rgba(231, 241, 255, 0.42) 0%, rgba(236, 244, 255, 0.2) 42%, rgba(242, 247, 255, 0.38) 100%);
  pointer-events: none;
}

.auth-shell {
  position: relative;
  z-index: 1;
  width: min(1280px, 100%);
  margin: 0 auto;
  min-height: calc(100vh - 56px);
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) 420px;
  gap: 24px;
  align-items: stretch;
}

.auth-panel {
  border-radius: var(--radius-xl);
  border: 1px solid rgba(205, 223, 252, 0.82);
  background: rgba(255, 255, 255, 0.66);
  box-shadow: 0 28px 68px rgba(42, 88, 158, 0.14);
  backdrop-filter: blur(18px);
}

.auth-panel--hero {
  padding: 32px;
}

.auth-panel--form {
  padding: 32px;
  display: grid;
  grid-template-rows: auto 1fr;
  gap: 28px;
  background: rgba(255, 255, 255, 0.74);
}

.auth-brand {
  color: var(--text-strong);
}

.auth-brand :deep(small) {
  color: var(--text-body);
}

.auth-eyebrow {
  margin-top: 26px;
}

.auth-title {
  max-width: 840px;
}

.auth-description {
  max-width: 760px;
}

.auth-card {
  padding: 22px;
  background: rgba(255, 255, 255, 0.42);
}

.auth-card--accent {
  background:
    radial-gradient(circle at top right, rgba(104, 166, 255, 0.12) 0%, rgba(104, 166, 255, 0) 30%),
    rgba(245, 249, 255, 0.58);
}

.auth-feature-list,
.auth-account-list {
  gap: 12px;
  margin-top: 14px;
}

.auth-meta-chip {
  background: rgba(234, 242, 255, 0.9);
}

.login-account-card {
  padding: 16px;
  background: rgba(255, 255, 255, 0.5);
}

.auth-form-title {
  margin-top: 14px;
}

.auth-form-description {
  margin-top: 12px;
}

.auth-form {
  margin-top: 20px;
}

.auth-submit {
  width: 100%;
}

.role-showcase {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  align-content: end;
}

.role-card {
  position: relative;
  overflow: hidden;
  min-height: 176px;
  padding: 18px;
  border-radius: 26px;
  border: 1px solid rgba(206, 222, 248, 0.84);
  background:
    radial-gradient(circle at top right, rgba(98, 160, 255, 0.12) 0%, rgba(98, 160, 255, 0) 38%),
    rgba(255, 255, 255, 0.56);
  box-shadow: 0 18px 40px rgba(38, 82, 148, 0.08);
  backdrop-filter: blur(10px);
}

.role-card:last-child {
  grid-column: 1 / -1;
}

.role-card__avatar {
  margin-bottom: 14px;
}

.role-card__content strong,
.role-card__content span,
.role-card__content p {
  display: block;
}

.role-card__content strong {
  margin-top: 8px;
  font-size: 18px;
  color: var(--text-strong);
}

.role-card__content p {
  margin: 8px 0 0;
  color: var(--text-body);
  line-height: 1.6;
  font-size: 13px;
}

.role-figure {
  position: relative;
  width: 78px;
  height: 78px;
}

.role-figure__halo,
.role-figure__head,
.role-figure__body,
.role-figure__accent {
  position: absolute;
  display: block;
}

.role-figure__halo {
  inset: 0;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(99, 163, 255, 0.18) 0%, rgba(194, 221, 255, 0.5) 100%);
}

.role-figure__head {
  top: 12px;
  left: 24px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 6px 18px rgba(73, 118, 188, 0.15);
}

.role-figure__body {
  left: 17px;
  bottom: 12px;
  width: 44px;
  height: 32px;
  border-radius: 20px 20px 14px 14px;
  background: linear-gradient(135deg, #5d9eff 0%, #2c6ce2 100%);
}

.role-figure__accent {
  right: 10px;
  bottom: 18px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
}

.role-card--teacher .role-figure__body {
  background: linear-gradient(135deg, #4d89f6 0%, #295bc8 100%);
}

.role-card--teacher .role-figure__accent {
  width: 20px;
  height: 8px;
  border-radius: 999px;
}

.role-card--admin .role-figure__body {
  background: linear-gradient(135deg, #1d4fa8 0%, #4f8dff 100%);
}

.role-card--admin .role-figure__accent {
  width: 18px;
  height: 18px;
  border-radius: 8px;
}

@media (max-width: 1080px) {
  .auth-shell {
    grid-template-columns: 1fr;
  }

  .role-showcase {
    grid-template-columns: 1fr;
  }

  .role-card:last-child {
    grid-column: auto;
  }
}
</style>
