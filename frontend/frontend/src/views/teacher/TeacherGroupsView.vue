<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createTeacherGroup,
  deleteTeacherGroup,
  getTeacherGroups,
  updateTeacherGroup,
} from '../../api/teacher'

const route = useRoute()
const router = useRouter()
const manage = ref(null)
const dialogVisible = ref(false)
const saving = ref(false)
const editingGroup = ref(null)
const form = reactive({
  groupName: '',
  memberUserIds: [],
})

const isReadonly = computed(() => Boolean(manage.value?.resultsPublished))
const canCreateGroup = computed(() => !isReadonly.value && (manage.value?.ungroupedStudents?.length || 0) > 0)
const dialogTitle = computed(() => editingGroup.value ? '编辑小组' : '创建小组')
const candidateStudents = computed(() => {
  if (!manage.value) {
    return []
  }
  const candidates = editingGroup.value
    ? [...manage.value.ungroupedStudents, ...editingGroup.value.members]
    : manage.value.ungroupedStudents
  return Array.from(new Map(candidates.map((item) => [item.id, item])).values())
})
const memberSelectDisabled = computed(() => isReadonly.value || Boolean(editingGroup.value?.memberLocked))

async function loadData() {
  manage.value = await getTeacherGroups(route.params.assignmentId)
}

function openCreateDialog() {
  editingGroup.value = null
  form.groupName = ''
  form.memberUserIds = []
  dialogVisible.value = true
}

function openEditDialog(group) {
  editingGroup.value = group
  form.groupName = group.groupName
  form.memberUserIds = group.members.map((item) => item.id)
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.groupName.trim()) {
    ElMessage.warning('请输入小组名称')
    return
  }
  if (!editingGroup.value && form.memberUserIds.length === 0) {
    ElMessage.warning('请至少选择 1 名组员')
    return
  }
  saving.value = true
  try {
    const payload = {
      groupName: form.groupName.trim(),
      memberUserIds: form.memberUserIds,
    }
    if (editingGroup.value) {
      await updateTeacherGroup(route.params.assignmentId, editingGroup.value.id, payload)
      ElMessage.success('小组已更新')
    } else {
      await createTeacherGroup(route.params.assignmentId, payload)
      ElMessage.success('小组已创建')
    }
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(group) {
  await ElMessageBox.confirm(`确认删除小组“${group.groupName}”吗？`, '删除小组', {
    type: 'warning',
    confirmButtonText: '确认删除',
  })
  await deleteTeacherGroup(route.params.assignmentId, group.id)
  ElMessage.success('小组已删除')
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell" v-if="manage">
    <section class="page-head">
      <div class="page-head__main">
        <span class="page-head__eyebrow">小组管理</span>
        <h2 class="page-head__title">在互评前维护课程小组关系</h2>
        <p class="page-head__description">
          教师可以在这里组织真实小组关系。已经产生提交或评分记录的小组会锁定成员，只允许继续修改组名。
        </p>
        <div class="page-head__stats">
          <span>{{ manage.assignmentTitle }}</span>
          <span>{{ manage.displayStatus || manage.assignmentStatus }}</span>
          <span>{{ manage.groups.length }} 个小组</span>
        </div>
        <div class="page-head__actions">
          <el-button @click="router.push(`/teacher/assignments/${route.params.assignmentId}/review`)">返回评分页</el-button>
          <el-button type="primary" :disabled="!canCreateGroup" @click="openCreateDialog">新建小组</el-button>
        </div>
      </div>

      <div class="page-head__aside">
        <span class="layout-chip">待分组学生</span>
        <h3>{{ manage.ungroupedStudents?.length || 0 }} 人</h3>
        <p>{{ manage.resultsPublished ? '最终成绩已发布，当前页面只读。' : '先维护小组关系，再让学生进入稳定的提交与互评流程。' }}</p>
      </div>
    </section>

    <el-alert
      :title="manage.displayStatus || manage.assignmentStatus"
      :description="manage.resultsPublished ? `最终成绩已发布${manage.resultsPublishedAt ? `：${manage.resultsPublishedAt}` : ''}，当前页面只读。` : '教师可以在这里维护真实小组关系；已经产生提交记录的小组只允许修改组名。'"
      :type="manage.resultsPublished ? 'success' : 'info'"
      :closable="false"
      show-icon
    />

    <div class="content-grid">
      <section class="section-card section-card--accent">
        <div class="panel-header">
          <div>
            <span class="section-eyebrow">小组列表</span>
            <h3 style="margin-top: 14px;">当前分组情况</h3>
          </div>
          <span class="layout-chip">{{ manage.groups.length }} 组</span>
        </div>
        <div class="data-table-wrap" style="margin-top: 18px;">
          <el-table :data="manage.groups">
            <el-table-column prop="groupName" label="小组名称" min-width="180" />
            <el-table-column label="组员" min-width="220">
              <template #default="{ row }">{{ row.members.map((item) => item.name).join(' / ') }}</template>
            </el-table-column>
            <el-table-column label="提交情况" min-width="220">
              <template #default="{ row }">
                <div v-if="row.submission">
                  <strong>{{ row.submission.projectName }}</strong>
                  <div class="muted">{{ row.submission.submittedAt || '--' }}</div>
                </div>
                <span v-else class="muted">未提交</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="140">
              <template #default="{ row }">
                <el-tag v-if="row.memberLocked" type="warning">成员已锁定</el-tag>
                <span v-else class="muted">可调整</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button type="primary" link :disabled="isReadonly" @click="openEditDialog(row)">编辑</el-button>
                <el-button type="danger" link :disabled="isReadonly || row.memberLocked" @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </section>

      <div class="stack">
        <section class="section-card">
          <span class="section-eyebrow">未分组学生</span>
          <h3 style="margin-top: 14px;">待处理名单</h3>
          <div class="data-table-wrap" style="margin-top: 18px;">
            <el-table :data="manage.ungroupedStudents" size="small">
              <el-table-column prop="name" label="姓名" />
              <el-table-column prop="username" label="账号" width="140" />
            </el-table>
          </div>
          <el-empty v-if="!manage.ungroupedStudents.length" description="当前没有未分组学生" :image-size="72" />
        </section>

        <section class="section-card">
          <span class="section-eyebrow">使用提示</span>
          <h3 style="margin-top: 14px;">分组建议</h3>
          <div class="stack" style="margin-top: 18px; gap: 16px;">
            <div class="mini-card">
              <strong>提交前尽量稳定成员</strong>
              <p class="section-subtitle">一旦小组开始提交或参与评分，就不建议再调整组员，避免数据归属混乱。</p>
            </div>
            <div class="mini-card">
              <strong>锁定后只改组名</strong>
              <p class="section-subtitle">系统会自动限制成员调整，但仍允许修正组名或展示名称。</p>
            </div>
          </div>
        </section>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px">
      <div class="dialog-content">
        <div class="section-card">
          <span class="section-eyebrow">小组编辑器</span>
          <h3 style="margin-top: 14px;">{{ dialogTitle }}</h3>
          <el-form label-position="top" style="margin-top: 18px;">
            <el-form-item label="小组名称">
              <el-input v-model="form.groupName" :disabled="isReadonly" />
            </el-form-item>
            <el-form-item label="组员">
              <el-select v-model="form.memberUserIds" multiple style="width: 100%;" :disabled="memberSelectDisabled">
                <el-option
                  v-for="student in candidateStudents"
                  :key="student.id"
                  :label="`${student.name} (${student.username})`"
                  :value="student.id"
                />
              </el-select>
            </el-form-item>
            <el-alert
              v-if="editingGroup?.memberLocked"
              title="该小组已经有提交或评分记录，当前只能修改组名，不能调整组员。"
              type="warning"
              :closable="false"
              show-icon
            />
          </el-form>
        </div>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="isReadonly" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
