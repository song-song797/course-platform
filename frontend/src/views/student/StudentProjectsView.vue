<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createEvaluation, getProjects, getStudentAssignment } from '../../api/student'

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
    evaluated: allProjects.filter((item) => item.evaluated).length,
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
  <div class="page-shell">
    <section v-if="assignment" class="page-hero">
      <div class="page-hero__card">
        <span class="page-hero__eyebrow">project plaza</span>
        <h1 class="page-hero__title">Review projects in a shared plaza and contribute structured peer feedback</h1>
        <p class="page-hero__description">
          项目广场会自动屏蔽自己、自己组以及黑名单项目。你可以直接查看当前得分状态，并进入评分弹窗完成互评。
        </p>
        <div class="page-hero__meta">
          <span>共 {{ projectSummary.total }} 个项目</span>
          <span>共 {{ projectSummary.reviewable }} 个可评项目</span>
          <span>已完成 {{ projectSummary.evaluated }} 个评分</span>
          <span>本页 {{ availableCount }} 个可评项目</span>
          <span>{{ assignment.displayStatus || assignment.status }}</span>
        </div>
      </div>

      <div class="page-hero__side">
        <span class="layout-chip">peer review</span>
        <h3>{{ assignment.resultsPublished ? '成绩已冻结' : '当前展示实时得分' }}</h3>
        <p>教师发布最终成绩前，这里的项目分数与排行榜都会保持实时更新。</p>
      </div>
    </section>

    <el-alert
      v-if="assignment"
      :title="assignment.displayStatus || assignment.status"
      :description="assignment.resultsPublished ? '当前排行榜和项目得分已冻结为最终结果。' : '当前展示为实时成绩，教师发布后会冻结为最终成绩。'"
      :type="assignment.resultsPublished ? 'success' : (assignment.status === 'REVIEWING' ? 'warning' : 'info')"
      :closable="false"
      show-icon
      style="margin-bottom: 20px;"
    />

    <div class="metric-grid" style="margin-bottom: 20px;">
      <div class="metric-card">
        <span class="muted">本作业项目数</span>
        <strong>{{ projectSummary.total }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">可评项目数</span>
        <strong>{{ projectSummary.reviewable }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">已评分项目数</span>
        <strong>{{ projectSummary.evaluated }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">当前榜单</span>
        <strong>{{ assignment?.resultsPublished ? '最终榜' : '实时榜' }}</strong>
      </div>
    </div>

    <div class="section-card">
      <span class="section-eyebrow">project list</span>
      <h3>可浏览与可评价项目</h3>
      <div style="margin-top: 18px;">
        <el-table :data="page.list">
          <el-table-column prop="projectName" label="项目名称" />
          <el-table-column label="成员">
            <template #default="{ row }">{{ row.memberNames.join(' / ') }}</template>
          </el-table-column>
          <el-table-column label="当前得分" width="140">
            <template #default="{ row }">
              <strong>{{ row.finalScore }}</strong>
              <div class="muted" style="font-size: 12px;">{{ row.scoreType === 'FINAL' ? '最终分' : '实时分' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="140">
            <template #default="{ row }">
              <el-tag v-if="row.evaluated" type="success">已评分</el-tag>
              <el-tag v-else-if="row.canEvaluate" type="primary">可评价</el-tag>
              <el-tag v-else type="info">不可评价</el-tag>
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
    </div>

    <el-dialog v-model="dialogVisible" width="760px" title="提交互评">
      <div class="dialog-content">
        <el-card v-for="item in form.itemScores" :key="item.rubricItemId" shadow="never">
          <template #header>{{ item.name }}</template>
          <el-slider v-model="item.score" :min="0" :max="10" :step="0.5" />
          <el-input v-model="item.comment" placeholder="单项评论（可选）" />
        </el-card>
        <el-input v-model="form.overallComment" type="textarea" :rows="4" placeholder="总体评价" />
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!scoreReady" @click="handleScore">提交评分</el-button>
      </template>
    </el-dialog>
  </div>
</template>
