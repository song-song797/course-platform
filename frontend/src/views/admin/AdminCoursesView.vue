<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addCourseMember,
  createAssignment,
  createCourse,
  getAdminCourses,
  getCourseMembers,
  importUsers,
  removeCourseMember,
} from '../../api/admin'

const page = ref({ list: [], total: 0, pageNo: 1, pageSize: 10 })
const courseForm = reactive({ code: '', name: '', term: '2026 春' })
const assignmentForm = reactive({
  courseId: 101,
  title: '课程项目作业',
  mode: 'GROUP',
  description: '用于初始化课程项目评分流程的标准作业模板。',
  deadline: '2026-03-30T23:59:00',
  allowLate: true,
  peerWeight: 40,
  teacherWeight: 60,
  status: 'SUBMITTING',
})
const memberDialogVisible = ref(false)
const memberLoading = ref(false)
const memberSubmitting = ref(false)
const memberCourse = ref(null)
const memberState = ref({ members: [], teacherCandidates: [], studentCandidates: [] })
const memberForm = reactive({ courseRole: 'STUDENT', userId: null })

const memberCandidates = computed(() => (
  memberForm.courseRole === 'TEACHER' ? memberState.value.teacherCandidates : memberState.value.studentCandidates
))
const totalAssignments = computed(() => page.value.list.reduce((sum, item) => sum + (item.assignmentCount || 0), 0))

async function loadCourses(pageNo = page.value.pageNo) {
  page.value = await getAdminCourses({ pageNo, pageSize: page.value.pageSize })
}

function handlePageChange(pageNo) {
  loadCourses(pageNo)
}

async function handleCreateCourse() {
  await createCourse(courseForm)
  ElMessage.success('课程已创建')
  courseForm.code = ''
  courseForm.name = ''
  courseForm.term = '2026 春'
  await loadCourses()
}

async function handleCreateAssignment() {
  await createAssignment(assignmentForm.courseId, assignmentForm)
  ElMessage.success('作业已创建')
  await loadCourses()
}

async function handleImport(option) {
  const formData = new FormData()
  formData.append('file', option.file)
  await importUsers(formData)
  ElMessage.success('名单导入成功')
  await loadCourses()
}

async function openMemberDialog(course) {
  memberCourse.value = course
  memberDialogVisible.value = true
  assignmentForm.courseId = course.id
  memberForm.courseRole = 'STUDENT'
  memberForm.userId = null
  await loadMemberState()
}

async function loadMemberState() {
  if (!memberCourse.value) {
    return
  }
  memberLoading.value = true
  try {
    memberState.value = await getCourseMembers(memberCourse.value.id)
    if (!memberCandidates.value.some((item) => item.userId === memberForm.userId)) {
      memberForm.userId = memberCandidates.value[0]?.userId ?? null
    }
  } finally {
    memberLoading.value = false
  }
}

function handleRoleChange() {
  memberForm.userId = memberCandidates.value[0]?.userId ?? null
}

async function handleAddMember() {
  if (!memberCourse.value || !memberForm.userId) {
    ElMessage.warning('请选择要加入课程的成员')
    return
  }
  memberSubmitting.value = true
  try {
    await addCourseMember(memberCourse.value.id, memberForm)
    ElMessage.success('课程成员已添加')
    await loadMemberState()
    await loadCourses()
  } finally {
    memberSubmitting.value = false
  }
}

async function handleRemoveMember(member) {
  if (!memberCourse.value) {
    return
  }
  await ElMessageBox.confirm(`确认移除 ${member.displayName} 吗？`, '移除成员', {
    type: 'warning',
  })
  await removeCourseMember(memberCourse.value.id, member.userId)
  ElMessage.success('成员已移除')
  await loadMemberState()
  await loadCourses()
}

function modeLabel(mode) {
  return mode === 'GROUP' ? '小组作业' : '个人作业'
}

onMounted(loadCourses)
</script>

<template>
  <div class="page-shell">
    <section class="page-head">
      <div class="page-head__main">
        <span class="page-head__eyebrow">课程配置中心</span>
        <h2 class="page-head__title">统一维护课程、作业与成员基础数据</h2>
        <p class="page-head__description">
          管理端负责初始化课程项目评分平台的基础资源。完成课程创建、名单导入和作业配置后，
          学生端与教师端会自动基于同一套数据进入提交、评分和统计流程。
        </p>
        <div class="page-head__stats">
          <span>{{ page.total }} 门课程</span>
          <span>{{ totalAssignments }} 个作业</span>
          <span>CSV 名单导入</span>
          <span>课程成员统一维护</span>
        </div>
      </div>

      <div class="page-head__aside">
        <span class="layout-chip">初始化步骤</span>
        <h3>先建课程，再导成员，最后布置作业</h3>
        <p>管理员面板只保留课程初始化相关高频操作，帮助你用最短路径完成教学环境准备。</p>
      </div>
    </section>

    <div class="metric-grid">
      <div class="metric-card">
        <span class="muted">课程总数</span>
        <strong>{{ page.total }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">作业总数</span>
        <strong>{{ totalAssignments }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">当前选中课程</span>
        <strong>{{ assignmentForm.courseId }}</strong>
      </div>
      <div class="metric-card">
        <span class="muted">默认模式</span>
        <strong>{{ modeLabel(assignmentForm.mode) }}</strong>
      </div>
    </div>

    <div class="content-grid">
      <div class="stack">
        <section class="section-card section-card--accent">
          <div class="panel-header">
            <div>
              <span class="section-eyebrow">课程清单</span>
              <h3 style="margin-top: 14px;">课程与成员入口</h3>
            </div>
            <span class="layout-chip">当前页 {{ page.list.length }} 门</span>
          </div>
          <p class="section-subtitle">从这里选定当前课程、进入成员管理，并确认每门课的作业配置进度。</p>

          <div class="data-table-wrap" style="margin-top: 18px;">
            <el-table :data="page.list">
              <el-table-column prop="code" label="课程编码" width="140" />
              <el-table-column prop="name" label="课程名称" min-width="220" />
              <el-table-column prop="term" label="学期" width="120" />
              <el-table-column prop="assignmentCount" label="作业数" width="100" />
              <el-table-column label="操作" width="260">
                <template #default="{ row }">
                  <div class="toolbar">
                    <el-button size="small" @click="assignmentForm.courseId = row.id">设为当前课程</el-button>
                    <el-button size="small" type="primary" @click="openMemberDialog(row)">成员管理</el-button>
                  </div>
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

        <section class="section-card">
          <div class="panel-header">
            <div>
              <span class="section-eyebrow">名单导入</span>
              <h3 style="margin-top: 14px;">批量更新平台用户</h3>
            </div>
          </div>
          <p class="section-subtitle">上传 CSV 后，教师候选列表和学生候选列表会同步刷新，可直接用于课程成员配置。</p>
          <div class="toolbar" style="margin-top: 18px;">
            <el-upload :show-file-list="false" :http-request="handleImport" accept=".csv">
              <el-button type="primary">上传 CSV</el-button>
            </el-upload>
          </div>
        </section>
      </div>

      <div class="stack">
        <section class="section-card">
          <span class="section-eyebrow">新建课程</span>
          <h3 style="margin-top: 14px;">录入课程基础信息</h3>
          <el-form :model="courseForm" label-position="top" style="margin-top: 18px;">
            <el-form-item label="课程编码">
              <el-input v-model="courseForm.code" />
            </el-form-item>
            <el-form-item label="课程名称">
              <el-input v-model="courseForm.name" />
            </el-form-item>
            <el-form-item label="学期">
              <el-input v-model="courseForm.term" />
            </el-form-item>
            <el-button type="primary" @click="handleCreateCourse">创建课程</el-button>
          </el-form>
        </section>

        <section class="section-card section-card--accent">
          <span class="section-eyebrow">创建作业</span>
          <h3 style="margin-top: 14px;">为当前课程布置评分任务</h3>
          <el-form :model="assignmentForm" label-position="top" style="margin-top: 18px;">
            <el-form-item label="课程 ID">
              <el-input-number v-model="assignmentForm.courseId" style="width: 100%;" />
            </el-form-item>
            <el-form-item label="作业标题">
              <el-input v-model="assignmentForm.title" />
            </el-form-item>
            <el-form-item label="作业模式">
              <el-select v-model="assignmentForm.mode" style="width: 100%;">
                <el-option label="小组作业" value="GROUP" />
                <el-option label="个人作业" value="INDIVIDUAL" />
              </el-select>
            </el-form-item>
            <el-form-item label="作业说明">
              <el-input v-model="assignmentForm.description" type="textarea" :rows="3" />
            </el-form-item>
            <el-button type="primary" @click="handleCreateAssignment">创建作业</el-button>
          </el-form>
        </section>
      </div>
    </div>

    <el-dialog
      v-model="memberDialogVisible"
      :title="memberCourse ? `课程成员管理 - ${memberCourse.name}` : '课程成员管理'"
      width="860px"
    >
      <div v-loading="memberLoading" class="dialog-content">
        <div class="section-card section-card--accent">
          <span class="section-eyebrow">添加成员</span>
          <h3 style="margin-top: 14px;">加入课程角色</h3>
          <el-form :model="memberForm" label-position="top" style="margin-top: 18px;">
            <div class="toolbar" style="align-items: flex-end;">
              <el-form-item label="课程角色">
                <el-select v-model="memberForm.courseRole" style="width: 160px;" @change="handleRoleChange">
                  <el-option label="教师" value="TEACHER" />
                  <el-option label="学生" value="STUDENT" />
                </el-select>
              </el-form-item>
              <el-form-item label="选择用户">
                <el-select v-model="memberForm.userId" style="width: 280px;" filterable>
                  <el-option
                    v-for="item in memberCandidates"
                    :key="item.userId"
                    :label="`${item.displayName} (${item.username})`"
                    :value="item.userId"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label=" ">
                <el-button type="primary" :loading="memberSubmitting" @click="handleAddMember">加入课程</el-button>
              </el-form-item>
            </div>
          </el-form>
        </div>

        <div class="section-card">
          <span class="section-eyebrow">当前成员</span>
          <h3 style="margin-top: 14px;">课程成员列表</h3>
          <div class="data-table-wrap" style="margin-top: 18px;">
            <el-table :data="memberState.members" empty-text="当前课程还没有成员">
              <el-table-column prop="displayName" label="姓名" />
              <el-table-column prop="username" label="账号" width="160" />
              <el-table-column prop="courseRole" label="课程角色" width="120" />
              <el-table-column label="操作" width="120">
                <template #default="{ row }">
                  <el-button size="small" type="danger" link @click="handleRemoveMember(row)">移除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>
