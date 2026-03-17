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
  title: '新建课程作业',
  mode: 'GROUP',
  description: '用于演示管理员配置课程与作业',
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

const memberCandidates = computed(() =>
  memberForm.courseRole === 'TEACHER' ? memberState.value.teacherCandidates : memberState.value.studentCandidates,
)
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
  ElMessage.success('示例作业已创建')
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
  await ElMessageBox.confirm(`确认移除 ${member.displayName} 吗？`, '删除成员', {
    type: 'warning',
  })
  await removeCourseMember(memberCourse.value.id, member.userId)
  ElMessage.success('成员已移除')
  await loadMemberState()
  await loadCourses()
}

onMounted(loadCourses)
</script>

<template>
  <div class="page-shell">
    <section class="page-hero">
      <div class="page-hero__card">
        <span class="page-hero__eyebrow">admin workspace</span>
        <h1 class="page-hero__title">Configure courses, initialize assignments and manage members from one control center</h1>
        <p class="page-hero__description">
          管理员负责课程初始化、名单导入和成员配置。完成这些准备后，
          学生和教师页面会自动基于同一份课程数据展开工作流。
        </p>
        <div class="page-hero__meta">
          <span>共 {{ page.total }} 门课程</span>
          <span>本页 {{ totalAssignments }} 个作业</span>
          <span>名单导入与课程初始化</span>
        </div>
      </div>

      <div class="page-hero__side">
        <span class="layout-chip">admin actions</span>
        <h3>Import roster, create course and seed assignments</h3>
        <p>先完成课程和成员配置，再把作业入口同步给学生与教师角色。</p>
      </div>
    </section>

    <div class="split-grid">
      <div class="stack">
        <div class="section-card section-card--accent">
          <span class="section-eyebrow">course list</span>
          <h3>课程与成员入口</h3>
          <p class="section-subtitle">查看课程、切换当前作业课程并管理课程成员。</p>
          <div style="margin-top: 18px;">
            <el-table :data="page.list">
              <el-table-column prop="code" label="课程编码" width="120" />
              <el-table-column prop="name" label="课程名称" />
              <el-table-column prop="term" label="学期" width="120" />
              <el-table-column prop="assignmentCount" label="作业数" width="90" />
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
        </div>

        <div class="section-card">
          <span class="section-eyebrow">roster import</span>
          <h3>名单导入</h3>
          <p class="section-subtitle">上传 CSV 后，课程成员与可选教师、学生列表会一起刷新。</p>
          <div class="toolbar" style="margin-top: 18px;">
            <el-upload :show-file-list="false" :http-request="handleImport" accept=".csv">
              <el-button type="primary">上传 CSV</el-button>
            </el-upload>
          </div>
        </div>
      </div>

      <div class="stack">
        <div class="section-card">
          <span class="section-eyebrow">create course</span>
          <h3>新建课程</h3>
          <el-form :model="courseForm" label-position="top" style="margin-top: 18px;">
            <el-form-item label="课程编码"><el-input v-model="courseForm.code" /></el-form-item>
            <el-form-item label="课程名称"><el-input v-model="courseForm.name" /></el-form-item>
            <el-form-item label="学期"><el-input v-model="courseForm.term" /></el-form-item>
            <el-button type="primary" @click="handleCreateCourse">创建课程</el-button>
          </el-form>
        </div>

        <div class="section-card section-card--accent">
          <span class="section-eyebrow">seed assignment</span>
          <h3>创建示例作业</h3>
          <el-form :model="assignmentForm" label-position="top" style="margin-top: 18px;">
            <el-form-item label="课程 ID"><el-input-number v-model="assignmentForm.courseId" style="width: 100%;" /></el-form-item>
            <el-form-item label="作业标题"><el-input v-model="assignmentForm.title" /></el-form-item>
            <el-form-item label="模式">
              <el-select v-model="assignmentForm.mode" style="width: 100%;">
                <el-option label="GROUP" value="GROUP" />
                <el-option label="INDIVIDUAL" value="INDIVIDUAL" />
              </el-select>
            </el-form-item>
            <el-button type="primary" @click="handleCreateAssignment">创建作业</el-button>
          </el-form>
        </div>
      </div>
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

    <el-dialog
      v-model="memberDialogVisible"
      :title="memberCourse ? `课程成员管理 - ${memberCourse.name}` : '课程成员管理'"
      width="860px"
    >
      <div v-loading="memberLoading" class="dialog-content">
        <div class="section-card section-card--accent">
          <span class="section-eyebrow">add member</span>
          <h3>添加课程成员</h3>
          <el-form :model="memberForm" style="margin-top: 18px;">
            <div class="toolbar">
              <el-form-item label="成员角色">
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
          <span class="section-eyebrow">current members</span>
          <h3>当前成员</h3>
          <div style="margin-top: 18px;">
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
