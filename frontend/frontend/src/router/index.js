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
  {
    path: '/login',
    component: LoginView,
    meta: { title: '登录平台', description: '进入课程项目评分平台' },
  },
  {
    path: '/change-password',
    component: ChangePasswordView,
    meta: { requiresAuth: true, title: '修改密码', description: '完成首次登录安全设置' },
  },
  {
    path: '/',
    component: MainLayout,
    children: [
      {
        path: '/admin/courses',
        component: AdminCoursesView,
        meta: {
          requiresAuth: true,
          roles: ['ADMIN'],
          menu: true,
          title: '课程与成员',
          description: '维护课程、成员和初始化配置',
          order: 1,
        },
      },
      {
        path: '/student/courses',
        component: StudentCoursesView,
        meta: {
          requiresAuth: true,
          roles: ['STUDENT'],
          menu: true,
          title: '我的课程',
          description: '查看课程任务、互评进度和结果入口',
          order: 1,
          theme: 'student-periwinkle',
        },
      },
      {
        path: '/student/assignments/:assignmentId',
        component: StudentAssignmentView,
        meta: {
          requiresAuth: true,
          roles: ['STUDENT'],
          menu: false,
          title: '作业详情',
          description: '查看作业说明、分组信息和评分规则',
          order: 2,
          theme: 'student-periwinkle',
        },
      },
      {
        path: '/student/assignments/:assignmentId/submission',
        component: StudentSubmissionView,
        meta: {
          requiresAuth: true,
          roles: ['STUDENT'],
          menu: false,
          title: '提交作品',
          description: '维护项目链接、附件和提交说明',
          order: 3,
          theme: 'student-periwinkle',
        },
      },
      {
        path: '/student/assignments/:assignmentId/projects',
        component: StudentProjectsView,
        meta: {
          requiresAuth: true,
          roles: ['STUDENT'],
          menu: false,
          title: '项目广场',
          description: '浏览项目并完成匿名互评',
          order: 4,
          theme: 'student-periwinkle',
        },
      },
      {
        path: '/student/assignments/:assignmentId/dashboard',
        component: StudentDashboardView,
        meta: {
          requiresAuth: true,
          roles: ['STUDENT'],
          menu: false,
          title: '结果看板',
          description: '查看得分、排名和评语反馈',
          order: 5,
          theme: 'student-periwinkle',
        },
      },
      {
        path: '/teacher/courses',
        component: TeacherCoursesView,
        meta: {
          requiresAuth: true,
          roles: ['TEACHER'],
          menu: true,
          title: '授课课程',
          description: '进入作业分组、评分和统计入口',
          order: 1,
        },
      },
      {
        path: '/teacher/assignments/:assignmentId/groups',
        component: TeacherGroupsView,
        meta: {
          requiresAuth: true,
          roles: ['TEACHER'],
          menu: false,
          title: '小组管理',
          description: '配置课程小组和成员归属关系',
          order: 2,
        },
      },
      {
        path: '/teacher/assignments/:assignmentId/rubric',
        component: TeacherRubricView,
        meta: {
          requiresAuth: true,
          roles: ['TEACHER'],
          menu: false,
          title: '评分规则',
          description: '调整 Rubric 维度、说明和权重',
          order: 3,
        },
      },
      {
        path: '/teacher/assignments/:assignmentId/review',
        component: TeacherReviewView,
        meta: {
          requiresAuth: true,
          roles: ['TEACHER'],
          menu: false,
          title: '评分治理',
          description: '查看提交、教师评分和异常治理',
          order: 4,
        },
      },
      {
        path: '/teacher/assignments/:assignmentId/stats',
        component: TeacherStatsView,
        meta: {
          requiresAuth: true,
          roles: ['TEACHER'],
          menu: false,
          title: '统计分析',
          description: '查看分布、排行榜和评分影响',
          order: 5,
        },
      },
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
