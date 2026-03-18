<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getStudentCourses, getStudentHome } from '../../api/student'

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

function taskTypeLabel(taskType) {
  return {
    DUE_SOON: '临近截止',
    TODO_SUBMIT: '待提交',
    GO_REVIEW: '去互评',
    RESULT_AVAILABLE: '结果已发布',
  }[taskType] || taskType
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
const courseTerms = computed(() => new Set(page.value.list.map((item) => item.term)).size)
</script>

<template>
  <div class="page-shell">
    <section class="page-hero">
      <div class="page-hero__card">
        <span class="page-hero__eyebrow">student hall</span>
        <h1 class="page-hero__title">在真实课程节奏里浏览待提交任务、互评热区和最近放榜结果</h1>
        <p class="page-hero__description">
          这里不是静态课程列表，而是把你当前能做的事、互评最活跃的作业，以及最近刚发布的结果，
          一起编排成学生默认大厅。
        </p>
        <div class="page-hero__meta">
          <span>{{ overview.totalCourses }} 门课程</span>
          <span>{{ overview.totalAssignments }} 个作业入口</span>
          <span>{{ overview.pendingSubmissionCount }} 个待提交任务</span>
          <span>{{ overview.availableReviewCount }} 个可评项目</span>
        </div>
      </div>

      <div class="page-hero__side">
        <span class="layout-chip">today pulse</span>
        <h3>{{ overview.reviewingAssignmentCount }} 门作业正在互评</h3>
        <p>
          当前还有 {{ overview.pendingSubmissionCount }} 个待提交任务，
          最近已有 {{ overview.publishedResultCount }} 门作业放榜，学生大厅会优先把最值得处理的入口顶上来。
        </p>
      </div>
    </section>

    <div v-if="activityBanners.length" class="hall-banner-strip">
      <article v-for="banner in activityBanners" :key="banner.title" class="hall-banner-card" :data-tone="banner.tone">
        <span class="section-eyebrow">{{ banner.tone === 'warning' ? 'priority' : (banner.tone === 'success' ? 'result' : 'review') }}</span>
        <h3>{{ banner.title }}</h3>
        <p>{{ banner.description }}</p>
      </article>
    </div>

    <div class="metric-grid" style="margin-bottom: 20px;">
      <div class="metric-card">
        <span class="muted">待提交任务</span>
        <strong>{{ overview.pendingSubmissionCount }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">互评中作业</span>
        <strong>{{ overview.reviewingAssignmentCount }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">最近放榜</span>
        <strong>{{ overview.publishedResultCount }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">可去互评项目</span>
        <strong>{{ overview.availableReviewCount }}</strong>
      </div>
    </div>

    <div class="section-card">
      <span class="section-eyebrow">today queue</span>
      <h3>今日优先完成</h3>
      <p class="section-subtitle">按临近截止、可去互评和最近放榜混排，让你一眼就知道先点哪里。</p>
      <div v-if="taskQueue.length" class="hall-task-grid">
        <article v-for="task in taskQueue" :key="`${task.taskType}-${task.assignmentId}`" class="hall-task-card">
          <div class="collection-card__header">
            <div>
              <strong>{{ task.assignmentTitle }}</strong>
              <p>{{ task.courseName }}</p>
            </div>
            <el-tag size="small" :type="task.taskType === 'RESULT_AVAILABLE' ? 'success' : (task.taskType === 'GO_REVIEW' ? 'warning' : 'info')">
              {{ task.displayStatus }}
            </el-tag>
          </div>
          <div class="hall-task-card__meta">
            <span>{{ task.mode || 'RESULT' }}</span>
            <span>{{ task.deadline || '--' }}</span>
            <span>{{ taskTypeLabel(task.taskType) }}</span>
          </div>
          <div class="toolbar" style="margin-top: 14px;">
            <el-button type="primary" @click="goTask(task)">{{ task.actionLabel }}</el-button>
            <el-button @click="openAssignment(task.assignmentId)">查看作业</el-button>
          </div>
        </article>
      </div>
      <el-empty v-else description="当前没有可排到今日队列的任务" :image-size="72" />
    </div>

    <div class="split-grid" style="margin-top: 20px; align-items: start;">
      <div class="section-card section-card--accent">
        <span class="section-eyebrow">review hot zone</span>
        <h3>互评热区</h3>
        <p class="section-subtitle">把当前最活跃、最适合直接进入广场的作业放在一起，方便快速参与互评。</p>
        <div v-if="reviewHighlights.length" class="hall-highlight-grid">
          <article v-for="item in reviewHighlights" :key="item.assignmentId" class="hall-highlight-card">
            <div class="collection-card__header">
              <div>
                <strong>{{ item.assignmentTitle }}</strong>
                <p>{{ item.courseName }}</p>
              </div>
              <el-tag type="warning" size="small">{{ item.displayStatus }}</el-tag>
            </div>
            <div class="hall-highlight-card__stats">
              <span>{{ item.totalProjects }} 个项目</span>
              <span>{{ item.reviewableProjects }} 个可评</span>
              <span>{{ item.leaderboardType === 'FINAL' ? '最终榜' : '实时榜' }}</span>
            </div>
            <div class="toolbar" style="margin-top: 14px;">
              <el-button type="primary" @click="goReviewHighlight(item)">进入项目广场</el-button>
            </div>
          </article>
        </div>
        <el-empty v-else description="当前没有互评热点作业" :image-size="72" />
      </div>

      <div class="section-card">
        <span class="section-eyebrow">recent results</span>
        <h3>最近放榜</h3>
        <p class="section-subtitle">最近刚发布结果的作业会集中展示，方便回到看板查看排名与最终得分。</p>
        <div v-if="resultHighlights.length" class="hall-result-list">
          <article v-for="item in resultHighlights" :key="item.assignmentId" class="hall-result-card">
            <div>
              <strong>{{ item.assignmentTitle }}</strong>
              <p>{{ item.courseName }}</p>
              <div class="hall-result-card__meta">
                <span>发布时间：{{ item.publishedAt || '--' }}</span>
                <span>排名：{{ item.currentRank ? `#${item.currentRank}` : '--' }}</span>
                <span>最终分：{{ item.finalScore ?? '--' }}</span>
              </div>
            </div>
            <el-button type="success" @click="goResultHighlight(item)">查看看板</el-button>
          </article>
        </div>
        <el-empty v-else description="最近还没有新的放榜结果" :image-size="72" />
      </div>
    </div>

    <div class="section-card" style="margin-top: 20px;">
      <span class="section-eyebrow">course scene</span>
      <h3>课程现场</h3>
      <p class="section-subtitle">
        分页保留课程视角，用来继续浏览全量课程与作业入口。
        当前这一页有 {{ page.total }} 门课程、本页 {{ totalAssignmentsOnPage }} 个作业入口、{{ courseTerms }} 个学期。
      </p>

      <div class="collection-grid" style="margin-top: 18px;">
        <article v-for="course in page.list" :key="course.id" class="collection-card">
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

          <div v-if="course.assignments?.length" class="mini-card-grid">
            <div v-for="assignment in course.assignments" :key="assignment.id" class="mini-card">
              <div class="collection-card__header">
                <div>
                  <strong>{{ assignment.title }}</strong>
                  <p>{{ assignment.mode }} · 截止 {{ assignment.deadline }}</p>
                </div>
                <el-tag size="small" :type="assignment.resultsPublished ? 'success' : (assignment.status === 'REVIEWING' ? 'warning' : 'info')">
                  {{ assignment.displayStatus || assignment.status }}
                </el-tag>
              </div>
              <div class="toolbar" style="margin-top: 12px;">
                <el-button type="primary" @click="openAssignment(assignment.id)">进入作业</el-button>
              </div>
            </div>
          </div>

          <div v-else class="landing-empty">
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
    </div>
  </div>
</template>
