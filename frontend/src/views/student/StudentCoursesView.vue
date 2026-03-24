<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getStudentCourses, getStudentHome } from '../../api/student'
import {
  getSubmissionCloseAt,
  hasLateWindow,
  isInLateWindow,
  isSubmissionClosed,
  submissionTimeSummary,
} from '../../utils/assignment'

const router = useRouter()
const home = ref(null)
const page = ref({ list: [], total: 0, pageNo: 1, pageSize: 10 })

async function loadHome() {
  home.value = await getStudentHome()
}

async function loadCourses(pageNo = page.value.pageNo) {
  page.value = await getStudentCourses({ pageNo, pageSize: page.value.pageSize })
}

async function loadData() {
  await Promise.all([loadHome(), loadCourses(1)])
}

function handlePageChange(pageNo) {
  loadCourses(pageNo)
}

function goTask(task) {
  if (task.taskType === 'GO_REVIEW') {
    router.push(`/student/assignments/${task.assignmentId}/projects`)
    return
  }
  if (task.taskType === 'RESULT_AVAILABLE') {
    router.push(`/student/assignments/${task.assignmentId}/dashboard`)
    return
  }
  router.push(`/student/assignments/${task.assignmentId}/submission`)
}

function goReviewHighlight(item) {
  router.push(`/student/assignments/${item.assignmentId}/projects`)
}

function goResultHighlight(item) {
  router.push(`/student/assignments/${item.assignmentId}/dashboard`)
}

function openAssignment(assignmentId) {
  router.push(`/student/assignments/${assignmentId}`)
}

function modeLabel(mode) {
  return {
    GROUP: '小组作业',
    INDIVIDUAL: '个人作业',
  }[mode] || mode
}

function taskCategory(taskType) {
  return {
    GO_SUBMIT: '待提交',
    GO_REVIEW: '待互评',
    RESULT_AVAILABLE: '看结果',
  }[taskType] || '课程任务'
}

function taskDescription(task) {
  if (task.taskType === 'GO_REVIEW') {
    return '进入项目广场完成匿名互评'
  }
  if (task.taskType === 'RESULT_AVAILABLE') {
    return '查看结果看板和最新排名'
  }
  if (isSubmissionClosed(task)) {
    return '当前已经超过提交关闭时间，请先查看作业详情确认后续安排'
  }
  if (isInLateWindow(task)) {
    return '当前处于迟交窗口，提交会被标记为迟交'
  }
  if (hasLateWindow(task)) {
    return '优先检查提交材料，注意正常截止与迟交截止时间'
  }
  return '优先检查提交材料和截止时间'
}

function taskButtonType(task) {
  if (task.taskType === 'GO_REVIEW') {
    return 'warning'
  }
  if (task.taskType === 'RESULT_AVAILABLE') {
    return 'success'
  }
  return 'primary'
}

function cleanSegment(text) {
  return (text || '').replace(/\s+/g, ' ').trim()
}

function displayTaskTitle(title) {
  const normalized = cleanSegment(title)
  if (!normalized) {
    return '当前课程任务'
  }

  const pipeParts = normalized.split('|').map(cleanSegment).filter(Boolean)
  const base = pipeParts.length ? pipeParts[pipeParts.length - 1] : normalized
  const slashParts = base.split('/').map(cleanSegment).filter(Boolean)

  return slashParts[0] || base
}

function displayTaskSubtitle(task) {
  const title = cleanSegment(task.assignmentTitle)
  const pipeParts = title.split('|').map(cleanSegment).filter(Boolean)
  const slashParts = (pipeParts[pipeParts.length - 1] || title).split('/').map(cleanSegment).filter(Boolean)
  const detail = slashParts.length > 1 ? slashParts[slashParts.length - 1] : task.courseName
  return `${detail} · ${taskDescription(task)}`
}

function timingChips(item) {
  const closeAt = getSubmissionCloseAt(item)
  if (!closeAt) {
    return ['未设置提交关闭时间']
  }
  if (hasLateWindow(item)) {
    return [`正常截止 ${item.deadline}`, `迟交截止 ${closeAt}`]
  }
  return [`截止 ${closeAt}`]
}

function assignmentSummary(assignment) {
  return `${modeLabel(assignment.mode)} · ${submissionTimeSummary(assignment)}`
}

onMounted(() => {
  loadData()
})

const overview = computed(() => home.value?.overview || {
  totalCourses: 0,
  totalAssignments: 0,
  pendingSubmissionCount: 0,
  reviewingAssignmentCount: 0,
  publishedResultCount: 0,
  availableReviewCount: 0,
})

const taskQueue = computed(() => home.value?.taskQueue || [])
const reviewHighlights = computed(() => home.value?.reviewHighlights || [])
const resultHighlights = computed(() => home.value?.resultHighlights || [])
const activityBanners = computed(() => home.value?.activityBanners || [])
const totalAssignmentsOnPage = computed(() => page.value.list.reduce((sum, course) => sum + (course.assignmentCount || 0), 0))

const primaryTask = computed(() => taskQueue.value[0] || null)
const firstReview = computed(() => reviewHighlights.value[0] || null)
const firstResult = computed(() => resultHighlights.value[0] || null)

const quickActions = computed(() => {
  const actions = []

  if (primaryTask.value) {
    actions.push({
      key: `task-${primaryTask.value.assignmentId}`,
      label: taskCategory(primaryTask.value.taskType),
      title: displayTaskTitle(primaryTask.value.assignmentTitle),
      description: displayTaskSubtitle(primaryTask.value),
      type: taskButtonType(primaryTask.value),
      actionText: primaryTask.value.actionLabel || '立即处理',
      onClick: () => goTask(primaryTask.value),
    })
  }

  if (firstReview.value) {
    actions.push({
      key: `review-${firstReview.value.assignmentId}`,
      label: '互评专区',
      title: displayTaskTitle(firstReview.value.assignmentTitle),
      description: `${firstReview.value.reviewableProjects} 个可评项目待处理`,
      type: 'warning',
      actionText: '进入互评',
      onClick: () => goReviewHighlight(firstReview.value),
    })
  }

  if (firstResult.value) {
    actions.push({
      key: `result-${firstResult.value.assignmentId}`,
      label: '结果发布',
      title: displayTaskTitle(firstResult.value.assignmentTitle),
      description: `查看排名和最终得分${firstResult.value.currentRank ? ` · #${firstResult.value.currentRank}` : ''}`,
      type: 'success',
      actionText: '查看结果',
      onClick: () => goResultHighlight(firstResult.value),
    })
  }

  return actions.slice(0, 3)
})
</script>

<template>
  <div class="page-shell student-workbench">
    <section class="page-head student-workbench__hero">
      <div class="page-head__main">
        <span class="page-head__eyebrow">学生工作台</span>
        <h2 class="page-head__title">今天优先处理这些任务</h2>
        <p class="page-head__description">
          先完成提交，再处理互评和结果查看，重要入口都放在这一屏。
        </p>

        <div v-if="primaryTask" class="focus-task">
          <div class="focus-task__main">
            <span class="layout-chip">{{ taskCategory(primaryTask.taskType) }}</span>
            <h3>{{ displayTaskTitle(primaryTask.assignmentTitle) }}</h3>
            <p>{{ displayTaskSubtitle(primaryTask) }}</p>
            <div class="focus-task__meta">
              <span>{{ modeLabel(primaryTask.mode) }}</span>
              <span v-for="chip in timingChips(primaryTask)" :key="`${primaryTask.assignmentId}-${chip}`">{{ chip }}</span>
              <span>{{ primaryTask.displayStatus }}</span>
            </div>
          </div>
          <div class="focus-task__actions">
            <el-button :type="taskButtonType(primaryTask)" @click="goTask(primaryTask)">
              {{ primaryTask.actionLabel || '立即处理' }}
            </el-button>
            <el-button @click="openAssignment(primaryTask.assignmentId)">查看作业</el-button>
          </div>
        </div>

        <div v-else class="landing-empty" style="margin-top: 22px;">
          <p>当前没有需要马上处理的任务，可以继续查看课程、互评和结果入口。</p>
        </div>
      </div>

      <div class="page-head__aside student-workbench__summary">
        <span class="layout-chip">今日清单</span>
        <h3>{{ overview.pendingSubmissionCount + overview.availableReviewCount }} 项待关注</h3>
        <div class="summary-list">
          <div class="summary-item">
            <span>待提交任务</span>
            <strong>{{ overview.pendingSubmissionCount }}</strong>
          </div>
          <div class="summary-item">
            <span>可互评项目</span>
            <strong>{{ overview.availableReviewCount }}</strong>
          </div>
          <div class="summary-item">
            <span>最近放榜</span>
            <strong>{{ overview.publishedResultCount }}</strong>
          </div>
        </div>
      </div>
    </section>

    <div class="student-workbench__signals">
      <div class="metric-card signal-card">
        <span class="muted">课程数</span>
        <strong>{{ overview.totalCourses }}</strong>
      </div>
      <div class="metric-card signal-card">
        <span class="muted">作业入口</span>
        <strong>{{ overview.totalAssignments }}</strong>
      </div>
      <div class="metric-card signal-card">
        <span class="muted">互评中作业</span>
        <strong>{{ overview.reviewingAssignmentCount }}</strong>
      </div>
    </div>

    <div class="content-grid student-workbench__main">
      <div class="stack">
        <section class="section-card section-card--accent">
          <div class="panel-header">
            <div>
              <span class="section-eyebrow">待办清单</span>
              <h3 style="margin-top: 14px;">按优先级处理</h3>
            </div>
            <span class="layout-chip">{{ taskQueue.length }} 项</span>
          </div>

          <div v-if="taskQueue.length" class="task-list">
            <article v-for="task in taskQueue" :key="`${task.taskType}-${task.assignmentId}`" class="task-row">
              <div class="task-row__main">
                <div class="task-row__header">
                  <div>
                    <strong>{{ displayTaskTitle(task.assignmentTitle) }}</strong>
                    <p>{{ displayTaskSubtitle(task) }}</p>
                  </div>
                  <el-tag size="small" :type="taskButtonType(task)">{{ task.displayStatus }}</el-tag>
                </div>
                <div class="task-row__meta">
                  <span>{{ taskCategory(task.taskType) }}</span>
                  <span>{{ modeLabel(task.mode) }}</span>
                  <span v-for="chip in timingChips(task)" :key="`${task.assignmentId}-${task.taskType}-${chip}`">{{ chip }}</span>
                </div>
              </div>
              <div class="task-row__side">
                <p>{{ task.courseName }}</p>
                <div class="toolbar">
                  <el-button :type="taskButtonType(task)" @click="goTask(task)">{{ task.actionLabel || '立即处理' }}</el-button>
                  <el-button @click="openAssignment(task.assignmentId)">查看作业</el-button>
                </div>
              </div>
            </article>
          </div>
          <el-empty v-else description="当前没有需要优先处理的课程任务" :image-size="72" />
        </section>

        <section class="section-card">
          <div class="panel-header">
            <div>
              <span class="section-eyebrow">我的课程</span>
              <h3 style="margin-top: 14px;">课程与作业入口</h3>
            </div>
            <span class="layout-chip">{{ page.total }} 门课程 · {{ totalAssignmentsOnPage }} 个入口</span>
          </div>

          <div class="course-grid">
            <article v-for="course in page.list" :key="course.id" class="collection-card course-card">
              <div class="collection-card__header">
                <div>
                  <strong>{{ course.name }}</strong>
                  <p class="section-subtitle">课程编码：{{ course.code }}</p>
                </div>
                <span class="layout-chip">{{ course.term }}</span>
              </div>

              <div class="landing-stats">
                <div class="landing-stat">
                  <strong>{{ course.assignmentCount }}</strong>
                  <span>作业数量</span>
                </div>
                <div class="landing-stat">
                  <strong>{{ course.assignments?.filter((item) => item.status === 'REVIEWING').length || 0 }}</strong>
                  <span>互评中</span>
                </div>
                <div class="landing-stat">
                  <strong>{{ course.assignments?.filter((item) => item.resultsPublished).length || 0 }}</strong>
                  <span>已放榜</span>
                </div>
              </div>

              <div v-if="course.assignments?.length" class="assignment-stack">
                <article v-for="assignment in course.assignments" :key="assignment.id" class="assignment-row">
                  <div>
                    <strong>{{ displayTaskTitle(assignment.title) }}</strong>
                    <p>{{ assignmentSummary(assignment) }}</p>
                  </div>
                  <div class="assignment-row__actions">
                    <el-tag size="small" :type="assignment.resultsPublished ? 'success' : (assignment.status === 'REVIEWING' ? 'warning' : 'info')">
                      {{ assignment.displayStatus || assignment.status }}
                    </el-tag>
                    <el-button type="primary" @click="openAssignment(assignment.id)">进入作业</el-button>
                  </div>
                </article>
              </div>

              <div v-else class="landing-empty" style="margin-top: 18px;">
                <p>当前课程还没有可进入的作业。</p>
              </div>
            </article>
          </div>

          <div v-if="page.total > page.pageSize" class="pagination-bar">
            <el-pagination
              background
              layout="prev, pager, next"
              :current-page="page.pageNo"
              :page-size="page.pageSize"
              :total="page.total"
              @current-change="handlePageChange"
            />
          </div>
        </section>
      </div>

      <div class="stack">
        <section class="section-card">
          <div class="panel-header">
            <div>
              <span class="section-eyebrow">快捷入口</span>
              <h3 style="margin-top: 14px;">直接去处理</h3>
            </div>
          </div>

          <div v-if="quickActions.length" class="quick-action-list">
            <article v-for="action in quickActions" :key="action.key" class="quick-action-card">
              <span class="layout-chip">{{ action.label }}</span>
              <strong>{{ action.title }}</strong>
              <p>{{ action.description }}</p>
              <el-button :type="action.type" @click="action.onClick">{{ action.actionText }}</el-button>
            </article>
          </div>
          <el-empty v-else description="当前没有可用的快捷处理入口" :image-size="72" />
        </section>

        <section v-if="activityBanners.length" class="section-card">
          <div class="panel-header">
            <div>
              <span class="section-eyebrow">课程提醒</span>
              <h3 style="margin-top: 14px;">近期通知</h3>
            </div>
          </div>
          <div class="notice-list">
            <article v-for="banner in activityBanners" :key="banner.title" class="notice-card">
              <strong>{{ banner.title }}</strong>
              <p>{{ banner.description }}</p>
            </article>
          </div>
        </section>

        <section class="section-card">
          <div class="panel-header">
            <div>
              <span class="section-eyebrow">结果与互评</span>
              <h3 style="margin-top: 14px;">最新动态</h3>
            </div>
          </div>

          <div class="stack" style="gap: 14px; margin-top: 18px;">
            <article v-if="firstReview" class="mini-card status-card">
              <span class="layout-chip">互评热点</span>
              <strong>{{ displayTaskTitle(firstReview.assignmentTitle) }}</strong>
              <p>{{ firstReview.courseName }} · 当前有 {{ firstReview.reviewableProjects }} 个可评项目</p>
              <el-button type="warning" @click="goReviewHighlight(firstReview)">进入项目广场</el-button>
            </article>

            <article v-if="firstResult" class="mini-card status-card">
              <span class="layout-chip">最近放榜</span>
              <strong>{{ displayTaskTitle(firstResult.assignmentTitle) }}</strong>
              <p>{{ firstResult.courseName }} · 可直接查看排名和最终得分</p>
              <el-button type="success" @click="goResultHighlight(firstResult)">查看结果看板</el-button>
            </article>

            <el-empty v-if="!firstReview && !firstResult" description="最近还没有新的互评或结果动态" :image-size="72" />
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.student-workbench :deep(.page-head__title) {
  font-size: 32px;
  line-height: 1.16;
  letter-spacing: -0.03em;
}

.student-workbench :deep(.page-head__description) {
  font-size: 16px;
}

.student-workbench__hero {
  grid-template-columns: minmax(0, 1.45fr) 320px;
}

.student-workbench__summary {
  align-content: start;
}

.focus-task {
  margin-top: 20px;
  padding: 22px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--surface-border-strong);
  background:
    radial-gradient(circle at top right, rgba(98, 160, 255, 0.14) 0%, rgba(98, 160, 255, 0) 30%),
    linear-gradient(180deg, #ffffff 0%, #f4f8ff 100%);
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
}

.focus-task h3 {
  margin: 12px 0 0;
  font-size: 22px;
  line-height: 1.35;
}

.focus-task p {
  margin: 8px 0 0;
  color: var(--text-body);
  line-height: 1.65;
  font-size: 15px;
}

.focus-task__meta,
.task-row__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.focus-task__meta span,
.task-row__meta span {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid var(--surface-border);
  color: #537098;
  font-size: 13px;
  font-weight: 700;
}

.focus-task__actions {
  display: grid;
  gap: 10px;
}

.student-workbench__signals {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.signal-card {
  min-height: 118px;
}

.signal-card strong {
  font-size: 26px;
}

.summary-list {
  display: grid;
  gap: 12px;
}

.summary-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-radius: var(--radius-md);
  background: var(--surface-soft);
}

.summary-item span {
  color: var(--text-body);
  font-size: 15px;
}

.summary-item strong {
  font-size: 24px;
  color: var(--text-strong);
}

.task-list,
.course-grid,
.quick-action-list,
.notice-list,
.assignment-stack {
  display: grid;
  gap: 16px;
}

.task-list,
.course-grid,
.quick-action-list,
.notice-list {
  margin-top: 18px;
}

.task-row,
.quick-action-card,
.notice-card,
.assignment-row {
  border-radius: var(--radius-lg);
  border: 1px solid var(--surface-border);
  background: rgba(255, 255, 255, 0.9);
}

.task-row {
  padding: 18px;
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(240px, 0.8fr);
  gap: 18px;
}

.task-row__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.task-row__header strong,
.quick-action-card strong,
.notice-card strong,
.assignment-row strong {
  display: block;
  margin: 0;
}

.task-row__header strong {
  font-size: 20px;
  line-height: 1.4;
}

.task-row__header p,
.task-row__side p,
.quick-action-card p,
.notice-card p,
.assignment-row p {
  margin: 8px 0 0;
  color: var(--text-body);
  line-height: 1.7;
}

.task-row__header p,
.task-row__side p,
.quick-action-card p,
.notice-card p,
.assignment-row p,
.status-card p {
  font-size: 14px;
}

.task-row__side {
  display: grid;
  align-content: space-between;
  gap: 12px;
}

.course-card {
  padding: 22px;
}

.assignment-row {
  padding: 16px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.assignment-row strong {
  font-size: 18px;
  line-height: 1.45;
}

.assignment-row__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.quick-action-card,
.notice-card,
.status-card {
  padding: 18px;
}

.quick-action-card strong,
.status-card strong {
  margin-top: 12px;
  font-size: 18px;
  line-height: 1.45;
}

.quick-action-card button,
.status-card button {
  margin-top: 14px;
}

.status-card p {
  margin: 10px 0 0;
  color: var(--text-body);
  line-height: 1.7;
}

@media (max-width: 1280px) {
  .student-workbench__signals {
    grid-template-columns: 1fr 1fr 1fr;
  }

  .task-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1080px) {
  .student-workbench__hero,
  .student-workbench__signals {
    grid-template-columns: 1fr;
  }

  .focus-task {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .student-workbench :deep(.page-head__title) {
    font-size: 28px;
  }

  .assignment-row,
  .assignment-row__actions {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
