<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createTeacherAssignment, createTeacherCourse, getTeacherCourses } from '../../api/teacher'
import { submissionTimeSummary } from '../../utils/assignment'

const router = useRouter()
const page = ref({ list: [], total: 0, pageNo: 1, pageSize: 10 })
const creatingCourse = ref(false)
const creatingAssignment = ref(false)
const assignmentDialogVisible = ref(false)
const activeCourse = ref(null)

const courseForm = reactive({
  code: '',
  name: '',
  term: '2026 春',
  courseDeadline: '',
})

const assignmentForm = reactive({
  title: '',
  mode: 'GROUP',
  description: '',
  deadline: '',
  allowLate: true,
  peerWeight: 40,
  teacherWeight: 60,
  status: 'SUBMITTING',
})

async function loadCourses(pageNo = page.value.pageNo) {
  page.value = await getTeacherCourses({ pageNo, pageSize: page.value.pageSize })
}

function handlePageChange(pageNo) {
  loadCourses(pageNo)
}

function openAssignment(path) {
  router.push(path)
}

function statusTagType(assignment) {
  if (assignment.resultsPublished) {
    return 'success'
  }
  if (assignment.status === 'REVIEWING') {
    return 'warning'
  }
  return 'info'
}

function modeLabel(mode) {
  return {
    GROUP: '小组作业',
    INDIVIDUAL: '个人作业',
  }[mode] || mode
}

function assignmentTimingSummary(assignment) {
  return `${modeLabel(assignment.mode)} · ${submissionTimeSummary(assignment)}`
}

function resetCourseForm() {
  courseForm.code = ''
  courseForm.name = ''
  courseForm.term = '2026 春'
  courseForm.courseDeadline = ''
}

function resetAssignmentForm() {
  assignmentForm.title = ''
  assignmentForm.mode = 'GROUP'
  assignmentForm.description = ''
  assignmentForm.deadline = ''
  assignmentForm.allowLate = true
  assignmentForm.peerWeight = 40
  assignmentForm.teacherWeight = 60
  assignmentForm.status = 'SUBMITTING'
}

function normalizeDateTime(value) {
  return value ? new Date(value.replace(' ', 'T')) : null
}

function openCreateAssignmentDialog(course) {
  activeCourse.value = course
  resetAssignmentForm()
  assignmentDialogVisible.value = true
}

async function handleCreateCourse() {
  if (!courseForm.code.trim() || !courseForm.name.trim() || !courseForm.term.trim()) {
    ElMessage.warning('请填写完整的课程编码、课程名称和学期')
    return
  }

  creatingCourse.value = true
  try {
    await createTeacherCourse({
      code: courseForm.code.trim(),
      name: courseForm.name.trim(),
      term: courseForm.term.trim(),
      courseDeadline: courseForm.courseDeadline || null,
    })
    ElMessage.success('课程已创建')
    resetCourseForm()
    await loadCourses(1)
  } finally {
    creatingCourse.value = false
  }
}

async function handleCreateAssignment() {
  if (!activeCourse.value) {
    return
  }
  if (!assignmentForm.title.trim() || !assignmentForm.description.trim() || !assignmentForm.deadline) {
    ElMessage.warning('请填写完整的作业标题、说明和截止时间')
    return
  }
  if (Number(assignmentForm.peerWeight) + Number(assignmentForm.teacherWeight) !== 100) {
    ElMessage.warning('学生互评分和教师评分之和必须等于 100')
    return
  }

  const courseDeadline = normalizeDateTime(activeCourse.value.courseDeadline)
  const assignmentDeadline = normalizeDateTime(assignmentForm.deadline)
  if (courseDeadline && assignmentDeadline && assignmentDeadline > courseDeadline) {
    ElMessage.warning('作业截止时间不能晚于课程截止时间')
    return
  }

  creatingAssignment.value = true
  try {
    await createTeacherAssignment(activeCourse.value.id, {
      title: assignmentForm.title.trim(),
      mode: assignmentForm.mode,
      description: assignmentForm.description.trim(),
      deadline: assignmentForm.deadline,
      allowLate: assignmentForm.allowLate,
      peerWeight: Number(assignmentForm.peerWeight),
      teacherWeight: Number(assignmentForm.teacherWeight),
      status: assignmentForm.status,
    })
    ElMessage.success('作业已创建')
    assignmentDialogVisible.value = false
    resetAssignmentForm()
    await loadCourses(page.value.pageNo)
  } finally {
    creatingAssignment.value = false
  }
}

onMounted(() => {
  loadCourses(1)
})

const totalAssignments = computed(() => page.value.list.reduce((sum, course) => sum + (course.assignments?.length || 0), 0))
const reviewAssignments = computed(() => page.value.list.reduce((sum, course) => (
  sum + (course.assignments?.filter((assignment) => assignment.status === 'REVIEWING').length || 0)
), 0))
const publishedAssignments = computed(() => page.value.list.reduce((sum, course) => (
  sum + (course.assignments?.filter((assignment) => assignment.resultsPublished).length || 0)
), 0))
const coursesWithDeadline = computed(() => page.value.list.filter((course) => Boolean(course.courseDeadline)).length)
</script>

<template>
  <div class="page-shell">
    <section class="page-head">
      <div class="page-head__main">
        <span class="page-head__eyebrow">教师总览</span>
        <h2 class="page-head__title">统一查看课程、创建作业并推进评分流程</h2>
        <p class="page-head__description">
          教师工作台现在支持直接创建课程和作业。你可以先维护课程基础信息与课程截止时间，再按课程上下文布置作业，
          继续衔接分组、评分规则、评分治理和统计分析。
        </p>
        <div class="page-head__stats">
          <span>{{ page.total }} 门课程</span>
          <span>{{ totalAssignments }} 个作业入口</span>
          <span>{{ reviewAssignments }} 个作业正在互评</span>
          <span>{{ coursesWithDeadline }} 门课程已设置截止时间</span>
        </div>
      </div>

      <div class="page-head__aside">
        <span class="layout-chip">教学配置</span>
        <h3>课程先行，作业跟随课程上下文创建</h3>
        <p>作业创建入口绑定到具体课程，不需要再手填课程 ID，也能提前校验课程截止时间约束。</p>
      </div>
    </section>

    <div class="metric-grid">
      <div class="metric-card">
        <span class="muted">授课课程</span>
        <strong>{{ page.total }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">作业总数</span>
        <strong>{{ totalAssignments }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">互评进行中</span>
        <strong>{{ reviewAssignments }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">结果已发布</span>
        <strong>{{ publishedAssignments }}</strong>
      </div>
    </div>

    <div class="content-grid">
      <div class="stack">
        <section class="section-card section-card--accent">
          <div class="panel-header">
            <div>
              <span class="section-eyebrow">课程列表</span>
              <h3 style="margin-top: 14px;">按课程进入评分工作流</h3>
            </div>
            <span class="layout-chip">当前页 {{ page.list.length }} 门</span>
          </div>

          <div class="collection-grid" style="margin-top: 18px;">
            <article v-for="course in page.list" :key="course.id" class="collection-card">
              <div class="collection-card__header">
                <div>
                  <strong>{{ course.name }}</strong>
                  <p class="section-subtitle">课程编码：{{ course.code }}</p>
                </div>
                <span class="layout-chip">{{ course.term }}</span>
              </div>

              <div class="page-head__stats" style="margin-top: 14px;">
                <span>课程截止：{{ course.courseDeadline || '未设置' }}</span>
              </div>

              <div class="landing-stats">
                <div class="landing-stat">
                  <strong>{{ course.assignments?.length || 0 }}</strong>
                  <span>作业数量</span>
                </div>
                <div class="landing-stat">
                  <strong>{{ course.assignments?.filter((item) => item.status === 'REVIEWING').length || 0 }}</strong>
                  <span>互评中</span>
                </div>
                <div class="landing-stat">
                  <strong>{{ course.assignments?.filter((item) => item.resultsPublished).length || 0 }}</strong>
                  <span>已发布</span>
                </div>
              </div>

              <div class="toolbar" style="margin-top: 16px;">
                <el-button type="primary" @click="openCreateAssignmentDialog(course)">创建作业</el-button>
              </div>

              <div v-if="course.assignments?.length" class="mini-card-grid">
                <div v-for="assignment in course.assignments" :key="assignment.id" class="mini-card">
                  <div class="collection-card__header">
                    <div>
                      <strong>{{ assignment.title }}</strong>
                      <p class="section-subtitle">{{ assignmentTimingSummary(assignment) }}</p>
                    </div>
                    <el-tag size="small" :type="statusTagType(assignment)">
                      {{ assignment.displayStatus || assignment.status }}
                    </el-tag>
                  </div>

                  <div class="page-head__stats" style="margin-top: 14px;">
                    <span>{{ assignment.resultsPublished ? '结果已发布' : '结果未发布' }}</span>
                    <span>{{ assignment.mode === 'GROUP' ? '支持分组管理' : '个人作业' }}</span>
                  </div>

                  <div class="toolbar" style="margin-top: 16px;">
                    <el-button
                      v-if="assignment.mode === 'GROUP'"
                      type="warning"
                      @click="openAssignment(`/teacher/assignments/${assignment.id}/groups`)"
                    >
                      小组管理
                    </el-button>
                    <el-button @click="openAssignment(`/teacher/assignments/${assignment.id}/review`)">评分记录</el-button>
                    <el-button type="primary" @click="openAssignment(`/teacher/assignments/${assignment.id}/rubric`)">评分规则</el-button>
                    <el-button type="success" @click="openAssignment(`/teacher/assignments/${assignment.id}/stats`)">统计分析</el-button>
                  </div>
                </div>
              </div>

              <div v-else class="landing-empty" style="margin-top: 18px;">
                <p>当前课程还没有作业，可以直接从这张课程卡片创建第一份课程项目作业。</p>
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
          <span class="section-eyebrow">新建课程</span>
          <h3 style="margin-top: 14px;">配置课程基础信息</h3>
          <p class="section-subtitle">课程截止时间可为空，但教师端建议提前设置，便于限制作业截止时间范围。</p>

          <el-form label-position="top" style="margin-top: 18px;">
            <el-form-item label="课程编码">
              <el-input v-model="courseForm.code" placeholder="例如：CS301" />
            </el-form-item>
            <el-form-item label="课程名称">
              <el-input v-model="courseForm.name" placeholder="例如：软件工程实践" />
            </el-form-item>
            <el-form-item label="学期">
              <el-input v-model="courseForm.term" placeholder="例如：2026 春" />
            </el-form-item>
            <el-form-item label="课程截止时间">
              <el-date-picker
                v-model="courseForm.courseDeadline"
                type="datetime"
                style="width: 100%;"
                placeholder="可选，建议填写"
                value-format="YYYY-MM-DDTHH:mm:ss"
                format="YYYY-MM-DD HH:mm:ss"
              />
            </el-form-item>
            <el-button type="primary" :loading="creatingCourse" @click="handleCreateCourse">创建课程</el-button>
          </el-form>
        </section>

        <section class="section-card">
          <span class="section-eyebrow">操作说明</span>
          <h3 style="margin-top: 14px;">教师端高频动作</h3>
          <div class="stack" style="margin-top: 18px; gap: 16px;">
            <div class="mini-card">
              <strong>先设置课程截止时间</strong>
              <p class="section-subtitle">如果课程设置了截止时间，后续作业不能晚于课程截止时间，有助于统一教学节奏。</p>
            </div>
            <div class="mini-card">
              <strong>作业从课程上下文创建</strong>
              <p class="section-subtitle">每张课程卡片都提供“创建作业”入口，避免手填课程 ID 带来的误操作。</p>
            </div>
            <div class="mini-card">
              <strong>创建后继续进入评分链路</strong>
              <p class="section-subtitle">作业创建完成后会自动刷新列表，你可以直接进入评分规则、评分治理和统计分析页面。</p>
            </div>
          </div>
        </section>
      </div>
    </div>

    <el-dialog
      v-model="assignmentDialogVisible"
      :title="activeCourse ? `创建作业 - ${activeCourse.name}` : '创建作业'"
      width="720px"
    >
      <div class="dialog-content">
        <div class="section-card section-card--accent">
          <span class="section-eyebrow">课程上下文</span>
          <h3 style="margin-top: 14px;">当前课程信息</h3>
          <div class="page-head__stats" style="margin-top: 18px;">
            <span>课程编码：{{ activeCourse?.code }}</span>
            <span>学期：{{ activeCourse?.term }}</span>
            <span>课程截止：{{ activeCourse?.courseDeadline || '未设置' }}</span>
          </div>
        </div>

        <div class="section-card">
          <span class="section-eyebrow">作业表单</span>
          <h3 style="margin-top: 14px;">创建课程项目作业</h3>
          <el-form label-position="top" style="margin-top: 18px;">
            <el-form-item label="作业标题">
              <el-input v-model="assignmentForm.title" placeholder="例如：阶段项目一" />
            </el-form-item>
            <el-form-item label="作业模式">
              <el-select v-model="assignmentForm.mode" style="width: 100%;">
                <el-option label="小组作业" value="GROUP" />
                <el-option label="个人作业" value="INDIVIDUAL" />
              </el-select>
            </el-form-item>
            <el-form-item label="作业说明">
              <el-input v-model="assignmentForm.description" type="textarea" :rows="4" placeholder="用于课程项目阶段验收" />
            </el-form-item>
            <el-form-item label="作业截止时间">
              <el-date-picker
                v-model="assignmentForm.deadline"
                type="datetime"
                style="width: 100%;"
                placeholder="请选择作业截止时间"
                value-format="YYYY-MM-DDTHH:mm:ss"
                format="YYYY-MM-DD HH:mm:ss"
              />
            </el-form-item>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="学生互评分占比">
                  <el-input-number v-model="assignmentForm.peerWeight" :min="0" :max="100" style="width: 100%;" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="教师评分占比">
                  <el-input-number v-model="assignmentForm.teacherWeight" :min="0" :max="100" style="width: 100%;" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="允许迟交">
              <el-switch v-model="assignmentForm.allowLate" inline-prompt active-text="允许" inactive-text="不允许" />
            </el-form-item>
            <el-alert
              :title="assignmentForm.allowLate ? '已开启迟交窗口' : '未开启迟交窗口'"
              :description="assignmentForm.allowLate
                ? '开启后，系统会在正常截止后继续保留 24 小时提交窗口；学生在此期间提交会被标记为迟交。'
                : '关闭后，提交关闭时间将与正常截止时间保持一致。'"
              :type="assignmentForm.allowLate ? 'info' : 'warning'"
              :closable="false"
              show-icon
            />
          </el-form>
        </div>
      </div>

      <template #footer>
        <el-button @click="assignmentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creatingAssignment" @click="handleCreateAssignment">创建作业</el-button>
      </template>
    </el-dialog>
  </div>
</template>
