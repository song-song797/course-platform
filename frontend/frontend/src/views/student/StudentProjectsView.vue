<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createEvaluation, getProjects, getStudentAssignment } from '../../api/student'
import { ineligibleReasonLabel, ineligibleReasonTagType } from '../../utils/assignment'

const route = useRoute()
const assignment = ref(null)
const page = ref({ list: [], total: 0, pageNo: 1, pageSize: 10 })
const projectSummary = ref({ total: 0, reviewable: 0, evaluated: 0 })
const scoringProject = ref(null)
const dialogVisible = ref(false)
const form = reactive({
  itemScores: [],
  overallComment: '',
})

async function loadProjects(pageNo = page.value.pageNo) {
  page.value = await getProjects(route.params.assignmentId, { pageNo, pageSize: page.value.pageSize })
}

function handlePageChange(pageNo) {
  loadProjects(pageNo)
}

async function loadProjectSummary() {
  const pageSize = 100
  let pageNo = 1
  let total = 0
  const allProjects = []

  do {
    const result = await getProjects(route.params.assignmentId, { pageNo, pageSize })
    total = result.total || 0
    allProjects.push(...result.list)
    pageNo += 1
  } while (allProjects.length < total)

  projectSummary.value = {
    total,
    reviewable: allProjects.filter((item) => item.canEvaluate).length,
    evaluated: allProjects.filter((item) => item.ineligibleReason === 'ALREADY_EVALUATED' || item.evaluated).length,
  }
}

async function loadData() {
  assignment.value = await getStudentAssignment(route.params.assignmentId)
  await Promise.all([loadProjects(1), loadProjectSummary()])
}

onMounted(() => {
  loadData()
})

const scoreReady = computed(() => form.itemScores.every((item) => Number.isFinite(Number(item.score))))
const availableCount = computed(() => page.value.list.filter((item) => item.canEvaluate).length)

function projectStatusLabel(project) {
  if (project.canEvaluate) {
    return '可评价'
  }
  return ineligibleReasonLabel(project.ineligibleReason || (project.evaluated ? 'ALREADY_EVALUATED' : null))
}

function projectStatusType(project) {
  if (project.canEvaluate) {
    return 'primary'
  }
  return ineligibleReasonTagType(project.ineligibleReason || (project.evaluated ? 'ALREADY_EVALUATED' : null))
}

function openScoreDialog(project) {
  scoringProject.value = project
  form.itemScores = assignment.value.rubric.map((item) => ({
    rubricItemId: item.id,
    score: 8,
    comment: '',
    name: item.name,
  }))
  form.overallComment = ''
  dialogVisible.value = true
}

async function handleScore() {
  await createEvaluation(scoringProject.value.id, {
    itemScores: form.itemScores.map(({ rubricItemId, score, comment }) => ({ rubricItemId, score, comment })),
    overallComment: form.overallComment,
  })
  dialogVisible.value = false
  await Promise.all([loadProjects(page.value.pageNo), loadProjectSummary()])
  ElMessage.success('互评已提交')
}

watch(() => route.params.assignmentId, async (next, previous) => {
  if (next && next !== previous) {
    await loadData()
  }
})
</script>

<template>
  <div class="page-shell detail-page detail-page--projects">
    <section v-if="assignment" class="page-head">
      <div class="page-head__main">
        <span class="page-head__eyebrow">项目广场</span>
        <h2 class="page-head__title">结构化互评</h2>
        <p class="page-head__description">
          项目广场会展示全部项目，并标记当前不可评的项目及原因
        </p>
        <div class="page-head__stats">
          <span>共 {{ projectSummary.total }} 个项目</span>
          <span>共 {{ projectSummary.reviewable }} 个可评项目</span>
          <span>已完成 {{ projectSummary.evaluated }} 次互评</span>
          <span>本页 {{ availableCount }} 个可评项目</span>
          <span>{{ assignment.displayStatus || assignment.status }}</span>
        </div>
      </div>

      <div class="page-head__aside">
        <span class="layout-chip">互评状态</span>
        <h3>{{ assignment.resultsPublished ? '成绩已冻结' : '实时分数展示中' }}</h3>
        <p>项目分数和排名实时更新。</p>
      </div>
    </section>

    <el-alert
      v-if="assignment"
      :title="assignment.displayStatus || assignment.status"
      :description="assignment.resultsPublished ? '当前排行榜和项目得分已冻结为最终结果。' : '当前展示的是实时成绩；互评结束并完成汇总后，系统会生成最终结果。'"
      :type="assignment.resultsPublished ? 'success' : (assignment.status === 'REVIEWING' ? 'warning' : 'info')"
      :closable="false"
      show-icon
    />

    <div class="metric-grid">
      <div class="metric-card">
        <span class="muted">项目总数</span>
        <strong>{{ projectSummary.total }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">可评项目</span>
        <strong>{{ projectSummary.reviewable }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">已评项目</span>
        <strong>{{ projectSummary.evaluated }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">榜单类型</span>
        <strong>{{ assignment?.resultsPublished ? '最终榜' : '实时榜' }}</strong>
      </div>
    </div>

    <div class="content-grid">
      <section class="section-card section-card--accent">
        <div class="panel-header">
          <div>
            <span class="section-eyebrow">项目列表</span>
            <h3 style="margin-top: 14px;">可浏览与可评价项目</h3>
          </div>
          <span class="layout-chip">{{ page.total }} 个项目</span>
        </div>

        <div class="data-table-wrap" style="margin-top: 18px;">
          <el-table :data="page.list">
            <el-table-column prop="projectName" label="项目名称" min-width="180" />
            <el-table-column label="成员" min-width="220">
              <template #default="{ row }">{{ row.memberNames.join(' / ') }}</template>
            </el-table-column>
            <el-table-column label="当前得分" width="140">
              <template #default="{ row }">
                <strong>{{ row.finalScore }}</strong>
                <div class="muted" style="font-size: 12px;">{{ row.scoreType === 'FINAL' ? '最终分' : '实时分' }}</div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="180">
              <template #default="{ row }">
                <el-tag :type="projectStatusType(row)">{{ projectStatusLabel(row) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button type="primary" link :disabled="!row.canEvaluate" @click="openScoreDialog(row)">去评分</el-button>
              </template>
            </el-table-column>
          </el-table>
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

      <div class="stack">
        <section class="section-card">
          <span class="section-eyebrow">互评说明</span>
          <h3 style="margin-top: 14px;">评分建议</h3>
          <div class="stack" style="margin-top: 18px; gap: 16px;">
            <div class="mini-card">
              <strong>按维度独立打分</strong>
              <p class="section-subtitle">每个维度都对应 Rubric 项，建议依据项目材料逐项判断，不要只给总评。</p>
            </div>
            <div class="mini-card">
              <strong>评论尽量可执行</strong>
              <p class="section-subtitle">好的互评应说明优点、问题和可改进方向，帮助对方有效复盘。</p>
            </div>
            <div class="mini-card">
              <strong>关注可评标识</strong>
              <p class="section-subtitle">系统会保留全量项目列表，并标记当前不可评原因；只有“可评价”的项目需要你处理。</p>
            </div>
          </div>
        </section>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" width="760px" title="提交互评">
      <div class="dialog-content">
        <el-card v-for="item in form.itemScores" :key="item.rubricItemId" shadow="never">
          <template #header>{{ item.name }}</template>
          <el-slider v-model="item.score" :min="0" :max="10" :step="0.5" />
          <el-input v-model="item.comment" placeholder="单项评语，可选填写" />
        </el-card>
        <el-input v-model="form.overallComment" type="textarea" :rows="4" placeholder="整体评价" />
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!scoreReady" @click="handleScore">提交评分</el-button>
      </template>
    </el-dialog>
  </div>
</template>
