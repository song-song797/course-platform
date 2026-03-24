<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getDashboard, getLeaderboard } from '../../api/student'
import ComparisonBarChart from '../../components/charts/ComparisonBarChart.vue'
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
    { label: summary.value.published ? '最终成绩已生成' : '最终成绩未生成', type: summary.value.published ? 'success' : 'warning' },
  ]
})
const dimensionCards = computed(() => summary.value.dimensionAverages || [])
const comparisonItems = computed(() => {
  const radarItems = summary.value.radar || []
  const averageItems = summary.value.dimensionAverages || []
  const radarMap = new Map(radarItems.map((item) => [item.name, normalizeNumber(item.value)]))
  const averageMap = new Map(averageItems.map((item) => [item.name, normalizeNumber(item.value)]))
  const orderedNames = [...new Set([...radarItems.map((item) => item.name), ...averageItems.map((item) => item.name)])]

  return orderedNames
    .map((name) => {
      const mine = roundScore(radarMap.get(name))
      const average = roundScore(averageMap.get(name))
      if (mine === null || average === null) {
        return null
      }

      return {
        name,
        mine,
        average,
        delta: roundScore(mine - average) ?? 0,
      }
    })
    .filter(Boolean)
})
const strongestDimension = computed(() => {
  if (!comparisonItems.value.length) {
    return null
  }

  return [...comparisonItems.value].sort((left, right) => {
    if (right.delta !== left.delta) {
      return right.delta - left.delta
    }
    return right.mine - left.mine
  })[0]
})
const weakestDimension = computed(() => {
  if (!comparisonItems.value.length) {
    return null
  }

  return [...comparisonItems.value].sort((left, right) => {
    if (left.delta !== right.delta) {
      return left.delta - right.delta
    }
    return left.mine - right.mine
  })[0]
})
const aboveAverageCount = computed(() => comparisonItems.value.filter((item) => item.delta >= 0).length)
const overallComparisonText = computed(() => {
  if (!comparisonItems.value.length) {
    return '提交项目并完成评分后，这里会显示你和课程均值的差距。'
  }

  const total = comparisonItems.value.length
  const count = aboveAverageCount.value
  if (count === total) {
    return `当前 ${total} 个维度都达到或高于课程均值，整体表现比较稳定。`
  }
  if (count === 0) {
    return `当前 ${total} 个维度都低于课程均值，建议优先补强短板维度。`
  }

  return `当前有 ${count} 个维度达到或高于课程均值，优势和补强点都比较清晰。`
})

function normalizeNumber(value) {
  const numericValue = Number(value)
  return Number.isFinite(numericValue) ? numericValue : null
}

function roundScore(value) {
  if (!Number.isFinite(value)) {
    return null
  }
  return Number(value.toFixed(1))
}

function formatScore(value) {
  return Number.isFinite(value) ? value.toFixed(1) : '--'
}

function describeDelta(delta) {
  if (!Number.isFinite(delta)) {
    return '--'
  }
  if (delta === 0) {
    return '与均值持平'
  }
  return delta > 0 ? `高于均值 ${delta.toFixed(1)}` : `低于均值 ${Math.abs(delta).toFixed(1)}`
}

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
  },
)
</script>

<template>
  <div class="page-shell" v-if="dashboard">
    <section class="page-head">
      <div class="page-head__main">
        <span class="page-head__eyebrow">结果看板</span>
        <h2 class="page-head__title">集中跟踪得分、排名与匿名反馈</h2>
        <p class="page-head__description">
          从互评分、教师评分到最终成绩发布，所有关键结果都会汇总在这里，帮助你快速理解项目当前表现。
        </p>
        <div class="page-head__stats">
          <span>{{ dashboard.courseName }}</span>
          <span>{{ dashboard.assignmentTitle }}</span>
          <span>{{ leaderboardTypeLabel }}</span>
        </div>
      </div>

      <div class="page-head__aside">
        <span class="layout-chip">当前排名</span>
        <h3>{{ summary.currentRank ? `#${summary.currentRank}` : '--' }}</h3>
        <p>{{ summary.published ? '最终成绩已自动生成，可查看冻结后的排名和结果。' : '当前仍处于实时成绩阶段，系统自动生成后会更新最终结果。' }}</p>
      </div>
    </section>

    <div class="toolbar" style="gap: 10px;">
      <el-tag v-for="tag in statusTags" :key="tag.label" :type="tag.type" effect="light">
        {{ tag.label }}
      </el-tag>
      <el-tag v-if="summary.publishedAt" type="success" effect="plain">
        生成时间：{{ summary.publishedAt }}
      </el-tag>
    </div>

    <el-alert
      :title="dashboard.displayStatus || dashboard.assignmentStatus"
      :description="summary.published ? `最终成绩已自动生成${summary.publishedAt ? `：${summary.publishedAt}` : ''}` : '当前仅展示实时成绩，系统自动生成后会显示最终成绩并冻结排行榜。'"
      :type="summary.published ? 'success' : (dashboard.assignmentStatus === 'REVIEWING' ? 'warning' : 'info')"
      :closable="false"
      show-icon
    />

    <div class="metric-grid">
      <div class="metric-card">
        <span class="muted">学生互评分</span>
        <strong>{{ hasSubmission ? (summary.peerScore ?? '--') : '待提交' }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">教师评分</span>
        <strong>{{ hasSubmission ? (summary.teacherScore ?? '--') : '待提交' }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">实时综合分</span>
        <strong>{{ hasSubmission ? (summary.realtimeFinalScore ?? '--') : '待提交' }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">最终得分</span>
        <strong>{{ !hasSubmission ? '待提交' : (summary.published ? (summary.finalScore ?? '--') : '未发布') }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">当前排名</span>
        <strong>{{ summary.currentRank ? `#${summary.currentRank}` : '--' }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">项目总数</span>
        <strong>{{ summary.totalProjects ?? 0 }}</strong>
      </div>
    </div>

    <div class="content-grid dashboard-analysis-grid">
      <div class="dashboard-analysis-column">
        <section class="section-card">
          <span class="section-eyebrow">维度画像</span>
          <h3 style="margin-top: 14px;">雷达图分析</h3>
          <p class="section-subtitle">先看整体轮廓，快速判断四个评分维度是否均衡。</p>
          <div style="margin-top: 18px;">
            <SimpleRadarChart v-if="summary.radar?.length" :items="summary.radar" />
            <el-empty v-else description="提交项目并完成评分后，这里会显示你的维度画像" :image-size="72" />
          </div>
        </section>

        <section class="section-card section-card--accent dashboard-panel--comparison">
          <span class="section-eyebrow">维度对比</span>
          <h3 style="margin-top: 14px;">我的表现 vs 课程均值</h3>
          <p class="section-subtitle">横向比较每个评分维度与课程均值的差距，更容易看清当前优势和下一步该补强什么。</p>
          <div class="dashboard-chart-wrap" style="margin-top: 18px;">
            <ComparisonBarChart
              v-if="comparisonItems.length"
              :items="comparisonItems"
              mine-label="我的得分"
              average-label="课程均值"
            />
            <el-empty v-else description="暂无足够数据进行维度对比" :image-size="72" />
          </div>
        </section>
      </div>

      <div class="dashboard-analysis-column">
        <section class="section-card section-card--accent">
          <span class="section-eyebrow">维度均分</span>
          <h3 style="margin-top: 14px;">各维度表现</h3>
          <p class="section-subtitle">右侧卡片给出课程均值，方便你和自己的项目表现一起看。</p>
          <div v-if="dimensionCards.length" class="metric-grid" style="margin-top: 18px;">
            <div v-for="item in dimensionCards" :key="item.name" class="metric-card">
              <span class="muted">{{ item.name }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
          <el-empty v-else description="暂无可展示的维度均分" :image-size="72" />
        </section>

        <section class="section-card dashboard-panel--comments">
          <span class="section-eyebrow">匿名评语</span>
          <h3 style="margin-top: 14px;">反馈摘要（{{ summary.comments?.length || 0 }}）</h3>
          <div style="margin-top: 18px;">
            <el-timeline v-if="summary.comments?.length">
              <el-timeline-item v-for="(comment, index) in summary.comments" :key="index" :timestamp="comment.authorRole">
                {{ comment.content }}
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else :description="hasSubmission ? '暂时还没有匿名评语' : '提交项目后，这里会汇总匿名评语'" :image-size="72" />
          </div>
        </section>
      </div>
    </div>

    <div class="content-grid">
      <div class="stack">
        <section v-if="dashboard.submission" class="section-card">
          <span class="section-eyebrow">我的提交</span>
          <h3 style="margin-top: 14px;">作品信息</h3>
          <div class="stack" style="margin-top: 18px; gap: 12px;">
            <div class="mini-card">
              <strong>{{ dashboard.submission.projectName }}</strong>
              <p class="section-subtitle">仓库：{{ dashboard.submission.repoUrl }}</p>
            </div>
            <div class="mini-card">
              <strong>成员</strong>
              <p class="section-subtitle">{{ dashboard.submission.members.map((item) => item.name).join(' / ') }}</p>
            </div>
            <div class="mini-card">
              <strong>提交时间</strong>
              <p class="section-subtitle">{{ dashboard.submission.submittedAt || '--' }}</p>
            </div>
          </div>
        </section>
      </div>

      <div class="stack">
        <section class="section-card">
          <span class="section-eyebrow">排行榜</span>
          <h3 style="margin-top: 14px;">当前榜单</h3>
          <p class="section-subtitle">当前展示：{{ leaderboardTypeLabel }}</p>
          <div class="data-table-wrap" style="margin-top: 18px;">
            <el-table :data="leaderboardPage.list" size="small">
              <el-table-column prop="rank" label="排名" width="80" />
              <el-table-column prop="projectName" label="项目" />
              <el-table-column prop="finalScore" label="得分" width="100" />
            </el-table>
          </div>
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
        </section>

        <section v-if="comparisonItems.length" class="section-card section-card--accent">
          <span class="section-eyebrow">结果解读</span>
          <h3 style="margin-top: 14px;">当前表现摘要</h3>
          <p class="section-subtitle">把维度对比结果收敛成几条最重要的判断，方便快速理解当前状态。</p>
          <div class="dashboard-insight-grid">
            <div class="mini-card dashboard-insight-card">
              <span class="dashboard-insight-card__label">当前最稳</span>
              <strong>{{ strongestDimension?.name || '--' }}</strong>
              <p class="section-subtitle">
                得分 {{ formatScore(strongestDimension?.mine) }}，{{ describeDelta(strongestDimension?.delta) }}
              </p>
            </div>
            <div class="mini-card dashboard-insight-card dashboard-insight-card--warning">
              <span class="dashboard-insight-card__label">优先补强</span>
              <strong>{{ weakestDimension?.name || '--' }}</strong>
              <p class="section-subtitle">
                得分 {{ formatScore(weakestDimension?.mine) }}，{{ describeDelta(weakestDimension?.delta) }}
              </p>
            </div>
            <div class="mini-card dashboard-insight-card dashboard-insight-card--summary">
              <span class="dashboard-insight-card__label">整体状态</span>
              <strong>{{ aboveAverageCount }}/{{ comparisonItems.length }}</strong>
              <p class="section-subtitle">{{ overallComparisonText }}</p>
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>
