import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/auth/LoginView.vue'
import ChangePasswordView from '../views/auth/ChangePasswordView.vue'
import MainLayout from '../layouts/MainLayout.vue'
import AdminCoursesView from '../views/admin/AdminCoursesView.vue'
import StudentCoursesView from '../views/student/StudentCoursesView.vue'
import StudentAssignmentView from '../views/student/StudentAssignmentView.vue'
import StudentSubmissionView from '../views/student/StudentSubmissionView.vue'
import StudentProjectsView from '../views/student/StudentProjectsView.vue'
import StudentDashboardView from '../views/student/StudentDashboardView.vue'
import TeacherCoursesView from '../views/teacher/TeacherCoursesView.vue'
import TeacherGroupsView from '../views/teacher/TeacherGroupsView.vue'
import TeacherRubricView from '../views/teacher/TeacherRubricView.vue'
import TeacherReviewView from '../views/teacher/TeacherReviewView.vue'
import TeacherStatsView from '../views/teacher/TeacherStatsView.vue'
import { useAuthStore } from '../stores/auth'
import { roleHome } from '../utils/menu'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: LoginView },
  { path: '/change-password', component: ChangePasswordView, meta: { requiresAuth: true } },
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: '/admin/courses', component: AdminCoursesView, meta: { requiresAuth: true, roles: ['ADMIN'], menu: true, title: '课程配置', order: 1 } },
      { path: '/student/courses', component: StudentCoursesView, meta: { requiresAuth: true, roles: ['STUDENT'], menu: true, title: '我的课程', order: 1 } },
      { path: '/student/assignments/:assignmentId', component: StudentAssignmentView, meta: { requiresAuth: true, roles: ['STUDENT'], menu: false, title: '作业详情', order: 2 } },
      { path: '/student/assignments/:assignmentId/submission', component: StudentSubmissionView, meta: { requiresAuth: true, roles: ['STUDENT'], menu: false, title: '项目提交', order: 3 } },
      { path: '/student/assignments/:assignmentId/projects', component: StudentProjectsView, meta: { requiresAuth: true, roles: ['STUDENT'], menu: false, title: '项目广场', order: 4 } },
      { path: '/student/assignments/:assignmentId/dashboard', component: StudentDashboardView, meta: { requiresAuth: true, roles: ['STUDENT'], menu: false, title: '结果看板', order: 5 } },
      { path: '/teacher/courses', component: TeacherCoursesView, meta: { requiresAuth: true, roles: ['TEACHER'], menu: true, title: '我的课程', order: 1 } },
      { path: '/teacher/assignments/:assignmentId/groups', component: TeacherGroupsView, meta: { requiresAuth: true, roles: ['TEACHER'], menu: false, title: '小组管理', order: 2 } },
      { path: '/teacher/assignments/:assignmentId/rubric', component: TeacherRubricView, meta: { requiresAuth: true, roles: ['TEACHER'], menu: false, title: 'Rubric 配置', order: 2 } },
      { path: '/teacher/assignments/:assignmentId/review', component: TeacherReviewView, meta: { requiresAuth: true, roles: ['TEACHER'], menu: false, title: '提交与评分', order: 3 } },
      { path: '/teacher/assignments/:assignmentId/stats', component: TeacherStatsView, meta: { requiresAuth: true, roles: ['TEACHER'], menu: false, title: '统计分析', order: 4 } },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

async function ensureValidSession(authStore) {
  if (!authStore.isLoggedIn) {
    return false
  }

  try {
    await authStore.refreshMe()
    return true
  } catch {
    authStore.clearAuth()
    return false
  }
}

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  if (!to.meta.requiresAuth) {
    if (to.path === '/login' && authStore.isLoggedIn) {
      if (!(await ensureValidSession(authStore))) {
        return true
      }
      return authStore.user?.firstLoginResetRequired
        ? '/change-password'
        : (roleHome[authStore.user?.role] || '/login')
    }
    return true
  }

  if (!authStore.isLoggedIn) {
    return '/login'
  }

  if (!(await ensureValidSession(authStore))) {
    return '/login'
  }

  if (authStore.user?.firstLoginResetRequired && to.path !== '/change-password') {
    return '/change-password'
  }

  if (to.meta.roles && !to.meta.roles.includes(authStore.user?.role)) {
    return roleHome[authStore.user?.role]
  }

  return true
})

export default router
