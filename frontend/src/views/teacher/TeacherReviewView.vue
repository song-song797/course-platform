<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  addBlacklist,
  createTeacherScore,
  getTeacherAssignment,
  getTeacherEvaluations,
  getTeacherSubmissions,
  reviewEvaluation,
} from '../../api/teacher'

const route = useRoute()
const router = useRouter()
const assignment = ref(null)
const submissions = ref({ list: [], total: 0, pageNo: 1, pageSize: 10 })
const evaluations = ref({ list: [], total: 0, pageNo: 1, pageSize: 10 })
const activeSubmission = ref(null)
const scoreDialogVisible = ref(false)
const blacklistDialogVisible = ref(false)
const blacklistTarget = ref(null)
const blacklistEvaluatorUserId = ref(null)
const loadingEvaluations = ref(false)
const form = reactive({ itemScores: [], overallComment: '' })
const filters = reactive({
  submissionId: '',
  evaluatorUserId: '',
  reviewStatus: '',
  abnormalOnly: false,
})

const reviewStatusOptions = [
  { label: '待处理', value: 'PENDING', type: 'info' },
  { label: '已忽略', value: 'IGNORED', type: 'warning' },
  { label: '已恢复', value: 'RESTORED', type: 'success' },
]

const availableStudents = computed(() => {
  const targetMemberIds = new Set(blacklistTarget.value?.members?.map((item) => item.id) || [])
  return (assignment.value?.studentMembers || []).filter((item) => !targetMemberIds.has(item.id))
})

const evaluatorOptions = computed(() => {
  const options = new Map()
  ;(assignment.value?.studentMembers || []).forEach((item) => {
    options.set(item.id, { id: item.id, name: item.name, username: item.username })
  })
  evaluations.value.list.forEach((item) => {
    if (!options.has(item.evaluatorUserId)) {
      options.set(item.evaluatorUserId, {
        id: item.evaluatorUserId,
        name: item.evaluatorName,
        username: item.evaluatorRole,
      })
    }
  })
  return Array.from(options.values())
})

function syncFiltersFromRoute() {
  filters.abnormalOnly = route.query.abnormalOnly === 'true'
}

function evaluationQueryParams(pageNo = evaluations.value.pageNo) {
  return {
    pageNo,
    pageSize: evaluations.value.pageSize,
    submissionId: filters.submissionId || undefined,
    evaluatorUserId: filters.evaluatorUserId || undefined,
    reviewStatus: filters.reviewStatus || undefined,
    abnormalOnly: filters.abnormalOnly,
  }
}

async function loadSubmissions(pageNo = submissions.value.pageNo) {
  submissions.value = await getTeacherSubmissions(route.params.assignmentId, {
    pageNo,
    pageSize: submissions.value.pageSize,
  })
}

async function loadEvaluations(pageNo = evaluations.value.pageNo) {
  loadingEvaluations.value = true
  try {
    evaluations.value = await getTeacherEvaluations(route.params.assignmentId, evaluationQueryParams(pageNo))
  } finally {
    loadingEvaluations.value = false
  }
}

async function loadData() {
  assignment.value = await getTeacherAssignment(route.params.assignmentId)
  await loadSubmissions(1)
  await loadEvaluations(1)
}

function resetScoreForm() {
  form.itemScores = (assignment.value?.rubric || []).map((item) => ({
    rubricItemId: item.id,
    score: 8,
    comment: '',
    name: item.name,
    description: item.description,
  }))
  form.overallComment = ''
}

function openScoreDialog(submission) {
  if (assignment.value?.resultsPublished) {
    return
  }
  activeSubmission.value = submission
  resetScoreForm()
  scoreDialogVisible.value = true
}

function openBlacklistDialog(submission) {
  if (assignment.value?.resultsPublished) {
    return
  }
  blacklistTarget.value = submission
  blacklistEvaluatorUserId.value = availableStudents.value[0]?.id || null
  blacklistDialogVisible.value = true
}

async function handleScore() {
  await createTeacherScore(activeSubmission.value.id, {
    itemScores: form.itemScores.map(({ rubricItemId, score, comment }) => ({ rubricItemId, score, comment })),
    overallComment: form.overallComment,
  })
  scoreDialogVisible.value = false
  ElMessage.success('教师评分已保存')
  await loadSubmissions(submissions.value.pageNo)
  await loadEvaluations(evaluations.value.pageNo)
}

async function handleBlacklist() {
  if (!blacklistEvaluatorUserId.value) {
    ElMessage.warning('请选择要回避该项目的学生')
    return
  }
  await addBlacklist(route.params.assignmentId, blacklistEvaluatorUserId.value, blacklistTarget.value.id)
  blacklistDialogVisible.value = false
  ElMessage.success('黑名单规则已保存')
  await loadSubmissions(submissions.value.pageNo)
  await loadEvaluations(evaluations.value.pageNo)
}

async function handleExclude(evaluation, excluded = true) {
  await reviewEvaluation(evaluation.id, excluded)
  ElMessage.success(excluded ? '该评分已忽略，不再参与计分' : '该评分已恢复，将重新参与计分')
  await loadEvaluations(evaluations.value.pageNo)
}

function handleFilter() {
  void loadEvaluations(1)
}

function resetFilters() {
  filters.submissionId = ''
  filters.evaluatorUserId = ''
  filters.reviewStatus = ''
  filters.abnormalOnly = false
  void loadEvaluations(1)
}

function handleSubmissionPageChange(pageNo) {
  void loadSubmissions(pageNo)
}

function handleEvaluationPageChange(pageNo) {
  void loadEvaluations(pageNo)
}

function reviewStatusMeta(status) {
  return reviewStatusOptions.find((item) => item.value === status) || reviewStatusOptions[0]
}

onMounted(async () => {
  syncFiltersFromRoute()
  await loadData()
})

watch(
  () => `${route.params.assignmentId}|${route.query.abnormalOnly || ''}`,
  async (next, previous) => {
    if (next !== previous) {
      syncFiltersFromRoute()
      await loadData()
    }
  }
)
</script>

<template>
  <div class="page-shell" v-if="assignment">
    <section class="page-hero">
      <div class="page-hero__card">
        <span class="page-hero__eyebrow">review records</span>
        <h1 class="page-hero__title">Review submissions, score with rubric and govern abnormal evaluations from one page</h1>
        <p class="page-hero__description">
          提交列表、教师评分和异常评分治理都围绕当前作业展开。最终成绩会在教师评分窗口结束后自动生成。
        </p>
        <div class="page-hero__meta">
          <span>{{ assignment.title }}</span>
          <span>{{ assignment.displayStatus || assignment.status }}</span>
          <span>Rubric {{ assignment.rubric.length }} 项</span>
        </div>
        <div class="page-hero__actions">
          <el-button v-if="assignment.mode === 'GROUP'" type="warning" @click="router.push(`/teacher/assignments/${route.params.assignmentId}/groups`)">小组管理</el-button>
          <el-button :disabled="assignment.resultsPublished" @click="router.push(`/teacher/assignments/${route.params.assignmentId}/rubric`)">编辑 Rubric</el-button>
          <el-button type="primary" @click="router.push(`/teacher/assignments/${route.params.assignmentId}/stats`)">查看统计</el-button>
        </div>
      </div>

      <div class="page-hero__side">
        <span class="layout-chip">submission count</span>
        <h3>{{ submissions.total }}</h3>
        <p>{{ assignment.resultsPublished ? '教师评分已结束，当前页面进入只读状态。' : '异常评分会被标记，但只有教师明确忽略后才会退出聚合。' }}</p>
      </div>
    </section>

    <el-alert
      :title="assignment.displayStatus || assignment.status"
      :description="assignment.resultsPublished ? `最终成绩已生成${assignment.resultsPublishedAt ? `：${assignment.resultsPublishedAt}` : ''}，当前页面进入只读状态。` : '学生互评在截止后立即开放 24 小时，教师评分同步开放 48 小时。教师窗口结束后系统会自动冻结最终成绩。'"
      :type="assignment.resultsPublished ? 'success' : (assignment.status === 'REVIEWING' ? 'warning' : 'info')"
      :closable="false"
      show-icon
      style="margin-bottom: 20px;"
    />

    <div class="split-grid">
      <div class="section-card section-card--accent">
        <span class="section-eyebrow">submissions</span>
        <h3>项目提交列表</h3>
        <div style="margin-top: 18px;">
          <el-table :data="submissions.list">
            <el-table-column prop="projectName" label="项目名称" />
            <el-table-column label="成员">
              <template #default="{ row }">{{ row.members.map((item) => item.name).join(' / ') }}</template>
            </el-table-column>
            <el-table-column prop="repoUrl" label="仓库链接" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.late" type="warning">迟交</el-tag>
                <span v-else class="muted">正常</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220">
              <template #default="{ row }">
                <el-button type="primary" link :disabled="assignment.resultsPublished" @click="openScoreDialog(row)">教师评分</el-button>
                <el-button type="danger" link :disabled="assignment.resultsPublished" @click="openBlacklistDialog(row)">拉黑名单</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="submissions.total > submissions.pageSize" class="pagination-bar">
            <el-pagination
              background
              layout="prev, pager, next"
              :current-page="submissions.pageNo"
              :page-size="submissions.pageSize"
              :total="submissions.total"
              @current-change="handleSubmissionPageChange"
            />
          </div>
        </div>
      </div>

      <div class="section-card">
        <span class="section-eyebrow">review governance</span>
        <h3>异常评分治理</h3>
        <div class="toolbar" style="margin-top: 18px;">
          <el-select v-model="filters.submissionId" clearable placeholder="按项目筛选" style="width: 160px;">
            <el-option v-for="submission in submissions.list" :key="submission.id" :label="submission.projectName" :value="submission.id" />
          </el-select>
          <el-select v-model="filters.evaluatorUserId" clearable placeholder="按评分人筛选" style="width: 180px;">
            <el-option v-for="evaluator in evaluatorOptions" :key="evaluator.id" :label="`${evaluator.name} (${evaluator.username})`" :value="evaluator.id" />
          </el-select>
          <el-select v-model="filters.reviewStatus" clearable placeholder="按处理状态筛选" style="width: 160px;">
            <el-option v-for="item in reviewStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-switch v-model="filters.abnormalOnly" inline-prompt active-text="仅异常" inactive-text="全部" />
          <el-button type="primary" :loading="loadingEvaluations" @click="handleFilter">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </div>

        <div style="margin-top: 18px;">
          <el-table :data="evaluations.list" v-loading="loadingEvaluations">
            <el-table-column type="expand">
              <template #default="{ row }">
                <div class="stack" style="padding: 8px 0;">
                  <strong>评分项详情</strong>
                  <el-table :data="row.itemScores" size="small" border>
                    <el-table-column prop="rubricItemName" label="评分项" />
                    <el-table-column prop="score" label="分数" width="100" />
                    <el-table-column prop="comment" label="评论" />
                  </el-table>
                  <el-empty v-if="!row.itemScores.length" description="暂无评分项详情" :image-size="56" />
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="projectName" label="项目" min-width="140" />
            <el-table-column prop="evaluatorName" label="评分人" min-width="120" />
            <el-table-column prop="evaluatorRole" label="角色" width="100" />
            <el-table-column prop="totalScore" label="总分" width="100" />
            <el-table-column label="异常原因" min-width="220">
              <template #default="{ row }">
                <el-tag v-if="row.abnormal" type="danger">异常</el-tag>
                <span v-if="row.abnormalReason" style="margin-left: 8px;">{{ row.abnormalReason }}</span>
                <span v-if="!row.abnormal" class="muted">正常</span>
              </template>
            </el-table-column>
            <el-table-column label="治理状态" width="120">
              <template #default="{ row }">
                <el-tag :type="reviewStatusMeta(row.reviewStatus).type">{{ reviewStatusMeta(row.reviewStatus).label }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="计分状态" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.excluded" type="warning">已忽略</el-tag>
                <span v-else class="muted">参与计分</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button v-if="!row.excluded && row.evaluatorRole === 'STUDENT'" type="danger" link :disabled="assignment.resultsPublished" @click="handleExclude(row, true)">忽略</el-button>
                <el-button v-else-if="row.evaluatorRole === 'STUDENT'" type="primary" link :disabled="assignment.resultsPublished" @click="handleExclude(row, false)">恢复</el-button>
                <span v-else class="muted">不适用</span>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="evaluations.total > evaluations.pageSize" class="pagination-bar">
            <el-pagination
              background
              layout="prev, pager, next"
              :current-page="evaluations.pageNo"
              :page-size="evaluations.pageSize"
              :total="evaluations.total"
              @current-change="handleEvaluationPageChange"
            />
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="scoreDialogVisible" width="760px" title="教师评分">
      <div class="dialog-content">
        <el-card v-for="item in form.itemScores" :key="item.rubricItemId" shadow="never">
          <template #header>
            <div style="display: flex; justify-content: space-between; gap: 16px;">
              <strong>{{ item.name }}</strong>
              <span class="muted">{{ item.description }}</span>
            </div>
          </template>
          <el-slider v-model="item.score" :min="0" :max="10" :step="0.5" />
          <el-input v-model="item.comment" placeholder="单项评论（可选）" />
        </el-card>
        <el-input v-model="form.overallComment" type="textarea" :rows="4" placeholder="总体评语" />
      </div>
      <template #footer>
        <el-button @click="scoreDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="assignment.resultsPublished" @click="handleScore">提交教师评分</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="blacklistDialogVisible" width="560px" title="添加黑名单规则">
      <div class="dialog-content">
        <el-alert :title="`项目：${blacklistTarget?.projectName || ''}`" type="warning" :closable="false" show-icon />
        <el-form label-position="top">
          <el-form-item label="选择需要回避该项目的学生">
            <el-select v-model="blacklistEvaluatorUserId" style="width: 100%;">
              <el-option v-for="student in availableStudents" :key="student.id" :label="`${student.name} (${student.username})`" :value="student.id" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="blacklistDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="assignment.resultsPublished" @click="handleBlacklist">确认添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>
