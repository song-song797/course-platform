<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import brandLogo from '../assets/brand/star45-logo-clean.png'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const roleLabel = computed(() => ({
  ADMIN: '管理工作台',
  TEACHER: '教师工作台',
  STUDENT: '学生工作台',
}[authStore.user?.role] || '平台工作台'))

const roleDescription = computed(() => ({
  ADMIN: '维护课程、成员和名单导入，保障课程项目评分流程的初始化数据稳定可用。',
  TEACHER: '组织分组、处理教师评分、治理异常记录，并持续跟踪课程结果。',
  STUDENT: '查看课程任务、提交作品、参与互评，并跟踪自己的得分与排名。',
}[authStore.user?.role] || '在统一工作台中继续当前角色的课程项目评分流程。'))

const menuItems = computed(() => {
  const role = authStore.user?.role
  return router.getRoutes()
    .filter((item) => item.meta?.menu && item.meta.roles?.includes(role))
    .sort((a, b) => a.meta.order - b.meta.order)
})

const currentTitle = computed(() => route.meta?.title || roleLabel.value)
const currentDescription = computed(() => route.meta?.description || roleDescription.value)
const homeTitle = computed(() => menuItems.value[0]?.meta?.title || '工作台')

const breadcrumbs = computed(() => {
  if (route.path === '/change-password') {
    return ['安全设置', '修改密码']
  }
  if (currentTitle.value === homeTitle.value) {
    return [homeTitle.value]
  }
  return [homeTitle.value, currentTitle.value]
})

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
    <aside class="workspace-sidebar">
      <div class="workspace-brand">
        <div class="workspace-brand__mark">
          <img class="workspace-brand__logo" :src="brandLogo" alt="星评小站 logo" />
        </div>
        <div>
          <strong>课程项目评分平台</strong>
          <small>{{ roleLabel }}</small>
        </div>
      </div>

      <div class="workspace-profile">
        <span class="layout-chip">当前登录</span>
        <strong>{{ authStore.user?.displayName }}</strong>
        <p>{{ roleDescription }}</p>
      </div>

      <nav class="workspace-nav" aria-label="主导航">
        <button
          v-for="item in menuItems"
          :key="item.path"
          type="button"
          :class="['workspace-nav__item', { 'workspace-nav__item--active': isActive(item.path) }]"
          @click="navigateTo(item.path)"
        >
          <span>{{ item.meta.title }}</span>
          <small>{{ item.meta.description }}</small>
        </button>
      </nav>

      <div class="workspace-sidebar__footer">
        <button type="button" class="workspace-secondary" @click="router.push('/change-password')">修改密码</button>
        <button type="button" class="workspace-primary" @click="handleLogout">退出登录</button>
      </div>
    </aside>

    <div class="workspace-main">
      <header class="workspace-toolbar">
        <div>
          <div class="workspace-toolbar__breadcrumbs">
            <span v-for="item in breadcrumbs" :key="item">{{ item }}</span>
          </div>
          <h1>{{ currentTitle }}</h1>
          <p>{{ currentDescription }}</p>
        </div>

        <div class="workspace-toolbar__actions">
          <span class="workspace-toolbar__meta">角色：{{ roleLabel }}</span>
          <span class="workspace-toolbar__meta">账号：{{ authStore.user?.username }}</span>
        </div>
      </header>

      <main class="layout-main">
        <router-view />
      </main>
    </div>
  </div>
</template>
