<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTeacherCourses, getTeacherStats, removeBlacklist } from '../../api/teacher'
import SimpleBarChart from '../../components/charts/SimpleBarChart.vue'

const route = useRoute()
const router = useRouter()
const stats = ref(null)
const teacherCourses = ref([])
const selectedCourseId = ref(null)
const selectedAssignmentId = ref(null)

const selectedCourse = computed(() => teacherCourses.value.find((item) => item.id === selectedCourseId.value) || null)
const assignmentOptions = computed(() => selectedCourse.value?.assignments || [])

async function loadData() {
  teacherCourses.value = (await getTeacherCourses({ pageNo: 1, pageSize: 50 })).list
  stats.value = await getTeacherStats(route.params.assignmentId)
  selectedCourseId.value = stats.value.courseId
  selectedAssignmentId.value = stats.value.assignmentId
}

async function handleRemoveBlacklist(rule) {
  try {
    await ElMessageBox.confirm(
      `确认移除“${rule.evaluatorName} -> ${rule.targetProjectName}”这条回避规则吗？`,
      '移除回避规则',
      {
        confirmButtonText: '确认移除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    await removeBlacklist(route.params.assignmentId, rule.evaluatorUserId, rule.targetSubmissionId)
    await loadData()
    ElMessage.success('回避规则已移除')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      throw error
    }
  }
}

function handleCourseChange(courseId) {
  const course = teacherCourses.value.find((item) => item.id === courseId)
  const nextAssignmentId = course?.assignments?.[0]?.id
  if (nextAssignmentId) {
    selectedAssignmentId.value = nextAssignmentId
    router.push(`/teacher/assignments/${nextAssignmentId}/stats`)
  }
}

function handleAssignmentChange(assignmentId) {
  if (assignmentId) {
    router.push(`/teacher/assignments/${assignmentId}/stats`)
  }
}

function goToAbnormalReview() {
  router.push({
    path: `/teacher/assignments/${route.params.assignmentId}/review`,
    query: { abnormalOnly: 'true' },
  })
}

onMounted(async () => {
  await loadData()
})

watch(() => route.params.assignmentId, async (next, previous) => {
  if (next && next !== previous) {
    await loadData()
  }
})
</script>

<template>
  <div class="page-shell" v-if="stats">
    <section class="page-head">
      <div class="page-head__main">
        <span class="page-head__eyebrow">统计分析</span>
        <h2 class="page-head__title">从分布、榜单与异常处理影响复核作业结果</h2>
        <p class="page-head__description">
          统计页会把提交量、评分完成率、异常评价处理和最终成绩影响汇总成统一的数据面板。
          最终成绩会在评分窗口结束后由系统自动生成，并自动冻结最终榜单。
        </p>
        <div class="page-head__stats">
          <span>{{ stats.courseName }}</span>
          <span>{{ stats.assignmentTitle }}</span>
          <span>{{ stats.leaderboardType === 'FINAL' ? '最终榜' : '实时榜' }}</span>
        </div>
      </div>

      <div class="page-head__aside">
        <span class="layout-chip">结果状态</span>
        <h3>{{ stats.resultsPublished ? '已自动生成' : '自动生成中' }}</h3>
        <p>{{ stats.resultsPublished ? '学生端已看到最终成绩，排行榜已自动冻结。' : '系统会在评分窗口结束后自动生成最终成绩并冻结最终榜单。' }}</p>
      </div>
    </section>

    <div class="filter-strip">
      <div class="toolbar">
        <el-select v-model="selectedCourseId" style="width: 220px;" @change="handleCourseChange">
          <el-option v-for="course in teacherCourses" :key="course.id" :label="course.name" :value="course.id" />
        </el-select>
        <el-select v-model="selectedAssignmentId" style="width: 280px;" @change="handleAssignmentChange">
          <el-option
            v-for="assignment in assignmentOptions"
            :key="assignment.id"
            :label="`${assignment.title} · ${assignment.displayStatus || assignment.status}`"
            :value="assignment.id"
          />
        </el-select>
        <span class="workspace-toolbar__meta">最终成绩自动生成</span>
        <el-button type="warning" @click="goToAbnormalReview">查看异常治理</el-button>
      </div>
    </div>

    <el-alert
      :title="stats.displayStatus || stats.assignmentStatus"
      :description="stats.resultsPublished
        ? `最终成绩已自动生成${stats.resultsPublishedAt ? `：${stats.resultsPublishedAt}` : ''}，当前榜单已冻结为最终榜。`
        : '系统会自动标记异常评价，并在评分窗口结束后自动生成最终成绩与最终榜单。'"
      :type="stats.resultsPublished ? 'success' : (stats.assignmentStatus === 'REVIEWING' ? 'warning' : 'info')"
      :closable="false"
      show-icon
    />

    <div class="metric-grid">
      <div class="metric-card">
        <span class="muted">提交数</span>
        <strong>{{ stats.totalSubmissions }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">迟交数</span>
        <strong>{{ stats.lateSubmissions }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">评分记录</span>
        <strong>{{ stats.totalEvaluations }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">异常评价</span>
        <strong>{{ stats.abnormalEvaluations }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">待处理异常</span>
        <strong>{{ stats.abnormalPendingCount }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">完成率</span>
        <strong>{{ stats.completionRate }}%</strong>
      </div>
    </div>

    <div class="content-grid">
      <section class="section-card section-card--accent">
        <span class="section-eyebrow">项目成绩分布</span>
        <h3 style="margin-top: 14px;">成绩区间</h3>
        <p class="section-subtitle" style="margin-top: 10px;">按项目当前成绩统计，未发布时展示实时综合分，已发布时展示最终分。</p>
        <div style="margin-top: 18px;">
          <SimpleBarChart :items="stats.scoreDistribution" />
        </div>
      </section>

      <section class="section-card">
        <span class="section-eyebrow">排行榜</span>
        <h3 style="margin-top: 14px;">当前榜单</h3>
        <p class="section-subtitle">当前显示：{{ stats.leaderboardType === 'FINAL' ? '最终榜' : '实时榜' }}</p>
        <div class="data-table-wrap" style="margin-top: 18px;">
          <el-table :data="stats.leaderboard">
            <el-table-column prop="rank" label="排名" width="80" />
            <el-table-column prop="projectName" label="项目" />
            <el-table-column prop="finalScore" label="得分" width="100" />
          </el-table>
        </div>
      </section>
    </div>

    <div class="content-grid">
      <section class="section-card">
        <span class="section-eyebrow">维度均分</span>
        <h3 style="margin-top: 14px;">各维度平均分</h3>
        <div style="margin-top: 18px;">
          <SimpleBarChart :items="stats.dimensionAverages" />
        </div>
      </section>

      <section class="section-card section-card--accent">
        <span class="section-eyebrow">项目得分</span>
        <h3 style="margin-top: 14px;">项目成绩对比</h3>
        <div class="data-table-wrap" style="margin-top: 18px;">
          <el-table :data="stats.projectScores">
            <el-table-column prop="projectName" label="项目" />
            <el-table-column prop="peerScore" label="互评分" width="100" />
            <el-table-column prop="teacherScore" label="教师分" width="100" />
            <el-table-column prop="realtimeFinalScore" label="实时综合分" width="120" />
            <el-table-column prop="finalScore" label="最终分" width="100" />
          </el-table>
        </div>
      </section>
    </div>

    <div class="content-grid">
      <div class="stack">
        <section class="section-card">
          <span class="section-eyebrow">互评进度</span>
          <h3 style="margin-top: 14px;">学生完成率</h3>
          <div class="data-table-wrap" style="margin-top: 18px;">
            <el-table :data="stats.reviewProgressByStudent" size="small">
              <el-table-column prop="displayName" label="学生" />
              <el-table-column label="完成 / 应评">
                <template #default="{ row }">{{ row.completedCount }} / {{ row.totalCount }}</template>
              </el-table-column>
              <el-table-column prop="completionRate" label="完成率(%)" width="120" />
            </el-table>
          </div>
        </section>

        <section class="section-card">
          <span class="section-eyebrow">异常提示</span>
          <h3 style="margin-top: 14px;">异常评分概览</h3>
          <div class="data-table-wrap" style="margin-top: 18px;">
            <el-table :data="stats.abnormalHints" size="small">
              <el-table-column prop="projectName" label="项目" />
              <el-table-column prop="evaluatorName" label="评分人" width="120" />
              <el-table-column prop="totalScore" label="分数" width="80" />
              <el-table-column prop="reason" label="原因" />
              <el-table-column label="计分状态" width="100">
                <template #default="{ row }">
                  <el-tag v-if="row.excluded" type="warning">已忽略</el-tag>
                  <span v-else class="muted">参与计分</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </section>
      </div>

      <section class="section-card">
        <span class="section-eyebrow">回避规则</span>
        <h3 style="margin-top: 14px;">黑名单列表</h3>
        <div class="data-table-wrap" style="margin-top: 18px;">
          <el-table :data="stats.blacklistRules" size="small">
            <el-table-column prop="evaluatorName" label="学生" width="140" />
            <el-table-column prop="targetProjectName" label="回避项目" />
            <el-table-column prop="createdAt" label="创建时间" width="180" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button
                  type="danger"
                  link
                  :disabled="stats.resultsPublished"
                  @click="handleRemoveBlacklist(row)"
                >
                  移除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <el-empty v-if="!stats.blacklistRules.length" description="当前还没有配置回避规则" :image-size="72" />
      </section>
    </div>

    <section class="section-card">
      <span class="section-eyebrow">影响分析</span>
      <h3 style="margin-top: 14px;">异常处理影响表</h3>
      <div class="data-table-wrap" style="margin-top: 18px;">
        <el-table :data="stats.abnormalImpacts">
          <el-table-column prop="projectName" label="项目" min-width="180" />
          <el-table-column prop="currentPeerScore" label="当前互评分" width="110" />
          <el-table-column prop="rawPeerScore" label="未治理互评分" width="120" />
          <el-table-column prop="peerScoreDelta" label="互评分差值" width="110" />
          <el-table-column prop="currentFinalScore" label="当前最终分" width="110" />
          <el-table-column prop="rawFinalScore" label="未治理最终分" width="120" />
          <el-table-column prop="finalScoreDelta" label="最终分差值" width="110" />
        </el-table>
      </div>
      <el-empty v-if="!stats.abnormalImpacts.length" description="当前没有因异常评价治理产生分数变化的项目" :image-size="72" />
    </section>
  </div>
</template>
