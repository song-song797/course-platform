<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { roleHome } from '../utils/menu'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const menuItems = computed(() => {
  const role = authStore.user?.role
  const all = router.getRoutes().filter((item) => item.meta?.menu && item.meta.roles?.includes(role))
  return all.sort((a, b) => a.meta.order - b.meta.order)
})

const roleLabel = computed(() => ({
  ADMIN: 'Admin Studio',
  TEACHER: 'Teaching Desk',
  STUDENT: 'Learning Space',
}[authStore.user?.role] || 'Workspace'))

const roleDescription = computed(() => ({
  ADMIN: '维护课程、作业和成员配置，让整套教学流程先顺起来。',
  TEACHER: '组织分组、查看提交、完成评分并跟进整门课的结果反馈。',
  STUDENT: '查看课程作业、提交作品、参与互评并追踪自己的学习结果。',
}[authStore.user?.role] || '在统一工作台里继续当前角色的课程流程。'))

function isActive(path) {
  return route.path === path
}

function navigateTo(path) {
  if (route.path !== path) {
    router.push(path)
  }
}

async function handleLogout() {
  await authStore.signOut()
  router.replace('/login')
}
</script>

<template>
  <div class="layout-shell">
    <div class="layout-frame">
      <header class="workspace-header">
        <div class="workspace-header__main">
          <div class="layout-brand">
            <div class="layout-brand__mark">C</div>
            <div>
              <strong>Course Demo</strong>
              <small>{{ roleLabel }}</small>
            </div>
          </div>

          <nav class="workspace-nav" aria-label="Workspace Navigation">
            <button
              v-for="item in menuItems"
              :key="item.path"
              type="button"
              :class="['workspace-nav__item', { 'workspace-nav__item--active': isActive(item.path) }]"
              @click="navigateTo(item.path)"
            >
              {{ item.meta.title }}
            </button>
          </nav>

          <div class="workspace-header__actions">
            <div class="layout-user">
              <span class="layout-chip">signed in</span>
              <div>
                <strong>{{ authStore.user?.displayName }}</strong>
                <p>{{ roleHome[authStore.user?.role] }}</p>
              </div>
            </div>

            <div class="toolbar">
              <el-button @click="router.push('/change-password')">修改密码</el-button>
              <el-button type="primary" @click="handleLogout">退出登录</el-button>
            </div>
          </div>
        </div>

        <div class="workspace-header__sub">
          <div>
            <span class="layout-chip">workspace guide</span>
            <h2>{{ roleLabel }}</h2>
          </div>
          <p>{{ roleDescription }}</p>
        </div>
      </header>

      <main class="layout-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.layout-frame {
  width: min(1320px, calc(100vw - 40px));
  margin: 0 auto;
  padding: 26px 0 34px;
  display: grid;
  gap: 24px;
}

.workspace-header {
  border-radius: var(--radius-xl);
  border: 1px solid var(--surface-border);
  background:
    radial-gradient(circle at top right, rgba(255, 213, 154, 0.22) 0%, rgba(255, 213, 154, 0) 28%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.88) 0%, rgba(255, 248, 236, 0.92) 100%);
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(10px);
}

.workspace-header__main,
.workspace-header__sub {
  padding: 22px 26px;
}

.workspace-header__main {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 20px;
  border-bottom: 1px solid rgba(239, 214, 179, 0.72);
}

.workspace-header__sub {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 20px;
}

.workspace-header__sub h2 {
  margin: 12px 0 0;
  font-size: 30px;
  line-height: 1.08;
}

.workspace-header__sub p {
  max-width: 560px;
  margin: 0;
  color: var(--text-body);
  line-height: 1.8;
}

.layout-brand {
  display: flex;
  align-items: center;
  gap: 14px;
}

.layout-brand__mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 16px;
  background: linear-gradient(135deg, #ffb24a 0%, #ff8a1d 100%);
  color: #fff;
  font-size: 20px;
  font-weight: 800;
}

.layout-brand strong,
.layout-brand small {
  display: block;
}

.layout-brand strong {
  font-size: 18px;
}

.layout-brand small {
  margin-top: 4px;
  color: var(--text-body);
}

.workspace-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 12px;
}

.workspace-nav__item {
  height: 42px;
  padding: 0 18px;
  border: 1px solid rgba(235, 192, 134, 0.8);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  color: #7f6a52;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.workspace-nav__item:hover {
  color: var(--accent-deep);
  background: #fff4df;
}

.workspace-nav__item--active {
  border-color: transparent;
  background: linear-gradient(135deg, #ffb24a 0%, #ff8a1d 100%);
  color: #fff;
  box-shadow: 0 16px 28px rgba(255, 142, 41, 0.18);
}

.workspace-header__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
  flex-wrap: wrap;
}

.layout-user {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(239, 214, 179, 0.76);
}

.layout-user strong,
.layout-user p {
  display: block;
}

.layout-user p {
  margin: 6px 0 0;
  color: var(--text-body);
}

@media (max-width: 1100px) {
  .workspace-header__main {
    grid-template-columns: 1fr;
    justify-items: start;
  }

  .workspace-nav {
    justify-content: flex-start;
  }

  .workspace-header__actions {
    width: 100%;
    justify-content: space-between;
  }

  .workspace-header__sub {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
