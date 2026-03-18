<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getDashboard, getLeaderboard } from '../../api/student'
import SimpleRadarChart from '../../components/charts/SimpleRadarChart.vue'

const route = useRoute()
const dashboard = ref(null)
const leaderboardPage = ref({ list: [], total: 0, pageNo: 1, pageSize: 10 })
const summary = computed(() => dashboard.value?.summary || {
  published: false,
  publishedAt: null,
  totalProjects: 0,
  currentRank: null,
  comments: [],
  radar: [],
  dimensionAverages: [],
})
const hasSubmission = computed(() => Boolean(dashboard.value?.submission))
const leaderboardTypeLabel = computed(() => dashboard.value?.leaderboardType === 'FINAL' ? '最终榜' : '实时榜')
const statusTags = computed(() => {
  if (!dashboard.value) {
    return []
  }
  return [
    { label: `作业状态：${dashboard.value.displayStatus || dashboard.value.assignmentStatus}`, type: dashboard.value.leaderboardType === 'FINAL' ? 'success' : 'info' },
    { label: `榜单类型：${leaderboardTypeLabel.value}`, type: dashboard.value.leaderboardType === 'FINAL' ? 'success' : 'warning' },
    { label: summary.value.published ? '最终成绩已发布' : '最终成绩未发布', type: summary.value.published ? 'success' : 'warning' },
  ]
})
const dimensionCards = computed(() => summary.value.dimensionAverages || [])

async function loadLeaderboard(pageNo = leaderboardPage.value.pageNo) {
  leaderboardPage.value = await getLeaderboard(route.params.assignmentId, {
    pageNo,
    pageSize: leaderboardPage.value.pageSize,
  })
}

async function loadData() {
  dashboard.value = await getDashboard(route.params.assignmentId)
  await loadLeaderboard(1)
}

function handleLeaderboardPageChange(pageNo) {
  loadLeaderboard(pageNo)
}

onMounted(loadData)

watch(
  () => route.params.assignmentId,
  async (next, previous) => {
    if (next && next !== previous) {
      await loadData()
    }
  }
)
</script>

<template>
  <div class="page-shell" v-if="dashboard">
    <section class="page-hero">
      <div class="page-hero__card">
        <span class="page-hero__eyebrow">result dashboard</span>
        <h1 class="page-hero__title">Track your score, ranking and qualitative feedback in one learning dashboard</h1>
        <p class="page-hero__description">
          从互评分、教师评分到最终成绩发布，所有关键结果都会汇总在这里，帮助你快速理解项目当前表现。
        </p>
        <div class="page-hero__meta">
          <span>{{ dashboard.courseName }}</span>
          <span>{{ dashboard.assignmentTitle }}</span>
          <span>{{ leaderboardTypeLabel }}</span>
        </div>
      </div>

      <div class="page-hero__side">
        <span class="layout-chip">current rank</span>
        <h3>{{ summary.currentRank ? `#${summary.currentRank}` : '--' }}</h3>
        <p>{{ summary.published ? '最终成绩已发布，可查看冻结后的排名与结果。' : '当前仍处于实时成绩阶段，教师发布后会更新最终结果。' }}</p>
      </div>
    </section>

    <div class="toolbar" style="margin-bottom: 16px; gap: 10px; flex-wrap: wrap;">
      <el-tag v-for="tag in statusTags" :key="tag.label" :type="tag.type" effect="light">
        {{ tag.label }}
      </el-tag>
      <el-tag v-if="summary.publishedAt" type="success" effect="plain">
        发布时间：{{ summary.publishedAt }}
      </el-tag>
    </div>

    <el-alert
      :title="dashboard.displayStatus || dashboard.assignmentStatus"
      :description="summary.published ? `最终成绩已发布${summary.publishedAt ? `：${summary.publishedAt}` : ''}` : '当前仅展示实时成绩，教师发布后会显示最终成绩并冻结排行榜。'"
      :type="summary.published ? 'success' : (dashboard.assignmentStatus === 'REVIEWING' ? 'warning' : 'info')"
      :closable="false"
      show-icon
      style="margin-bottom: 20px;"
    />

    <div class="metric-grid">
      <div class="metric-card"><span class="muted">学生互评分</span><strong>{{ hasSubmission ? (summary.peerScore ?? '--') : '待提交' }}</strong></div>
      <div class="metric-card"><span class="muted">教师评分</span><strong>{{ hasSubmission ? (summary.teacherScore ?? '--') : '待提交' }}</strong></div>
      <div class="metric-card"><span class="muted">实时综合分</span><strong>{{ hasSubmission ? (summary.realtimeFinalScore ?? '--') : '待提交' }}</strong></div>
      <div class="metric-card"><span class="muted">最终得分</span><strong>{{ !hasSubmission ? '待提交' : (summary.published ? (summary.finalScore ?? '--') : '未发布') }}</strong></div>
      <div class="metric-card"><span class="muted">当前排名</span><strong>{{ summary.currentRank ? `#${summary.currentRank}` : '--' }}</strong></div>
      <div class="metric-card"><span class="muted">总项目数</span><strong>{{ summary.totalProjects ?? 0 }}</strong></div>
    </div>

    <div class="split-grid" style="margin-top: 20px; align-items: stretch;">
      <div class="section-card">
        <span class="section-eyebrow">radar</span>
        <h3>维度雷达图</h3>
        <div style="margin-top: 18px;">
          <SimpleRadarChart v-if="summary.radar?.length" :items="summary.radar" />
          <el-empty v-else description="提交项目并完成评分后，这里会显示你的维度画像" :image-size="72" />
        </div>
      </div>

      <div class="stack">
        <div class="section-card section-card--accent">
          <span class="section-eyebrow">dimensions</span>
          <h3>各维度均分</h3>
          <div v-if="dimensionCards.length" class="metric-grid" style="margin-top: 18px;">
            <div v-for="item in dimensionCards" :key="item.name" class="metric-card">
              <span class="muted">{{ item.name }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
          <el-empty v-else description="暂无可展示的维度均分" :image-size="72" />
        </div>

        <div class="section-card">
          <span class="section-eyebrow">comments</span>
          <h3>匿名评语（{{ summary.comments?.length || 0 }}）</h3>
          <div style="margin-top: 18px;">
            <el-timeline v-if="summary.comments?.length">
              <el-timeline-item v-for="(comment, index) in summary.comments" :key="index" :timestamp="comment.authorRole">
                {{ comment.content }}
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else :description="hasSubmission ? '暂时还没有匿名评语' : '提交项目后，这里会汇总匿名评语'" :image-size="72" />
          </div>
        </div>

        <div v-if="dashboard.submission" class="section-card">
          <span class="section-eyebrow">my submission</span>
          <h3>我的提交</h3>
          <div class="stack" style="gap: 8px; margin-top: 14px;">
            <div><strong>{{ dashboard.submission.projectName }}</strong></div>
            <div class="muted">仓库：{{ dashboard.submission.repoUrl }}</div>
            <div class="muted">成员：{{ dashboard.submission.members.map((item) => item.name).join(' / ') }}</div>
            <div class="muted">提交时间：{{ dashboard.submission.submittedAt || '--' }}</div>
          </div>
        </div>

        <div class="section-card">
          <span class="section-eyebrow">leaderboard</span>
          <h3>排行榜</h3>
          <div class="muted" style="margin: 10px 0 12px;">当前展示：{{ leaderboardTypeLabel }}</div>
          <el-table :data="leaderboardPage.list" size="small">
            <el-table-column prop="rank" label="排名" width="80" />
            <el-table-column prop="projectName" label="项目" />
            <el-table-column prop="finalScore" label="得分" width="100" />
          </el-table>
          <div v-if="leaderboardPage.total > leaderboardPage.pageSize" class="pagination-bar">
            <el-pagination
              background
              layout="prev, pager, next"
              :current-page="leaderboardPage.pageNo"
              :page-size="leaderboardPage.pageSize"
              :total="leaderboardPage.total"
              @current-change="handleLeaderboardPageChange"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
