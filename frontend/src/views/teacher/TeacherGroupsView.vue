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
    <section class="page-hero">
      <div class="page-hero__card">
        <span class="page-hero__eyebrow">group management</span>
        <h1 class="page-hero__title">Build and maintain course groups before students move into review</h1>
        <p class="page-hero__description">
          教师可以在这里组织真实小组关系。已有提交或评分记录的小组会锁定成员，只允许继续修改组名。
        </p>
        <div class="page-hero__meta">
          <span>{{ manage.assignmentTitle }}</span>
          <span>{{ manage.displayStatus || manage.assignmentStatus }}</span>
          <span>{{ manage.groups.length }} 个小组</span>
        </div>
        <div class="page-hero__actions">
          <el-button @click="router.push(`/teacher/assignments/${route.params.assignmentId}/review`)">返回评分页</el-button>
          <el-button type="primary" :disabled="!canCreateGroup" @click="openCreateDialog">新建小组</el-button>
        </div>
      </div>

      <div class="page-hero__side">
        <span class="layout-chip">ungrouped</span>
        <h3>{{ manage.ungroupedStudents?.length || 0 }} 人待分组</h3>
        <p>{{ manage.resultsPublished ? '最终成绩已生成，当前页面只读。' : '先完成小组关系维护，再让学生进入稳定的提交与互评流程。' }}</p>
      </div>
    </section>

    <el-alert
      :title="manage.displayStatus || manage.assignmentStatus"
      :description="manage.resultsPublished ? `最终成绩已生成${manage.resultsPublishedAt ? `：${manage.resultsPublishedAt}` : ''}，当前页面只读。` : '教师可在这里维护真实小组关系；已有提交的小组仅允许改组名。'"
      :type="manage.resultsPublished ? 'success' : 'info'"
      :closable="false"
      show-icon
      style="margin-bottom: 20px;"
    />

    <div class="split-grid">
      <div class="section-card section-card--accent">
        <span class="section-eyebrow">group list</span>
        <h3>小组列表</h3>
        <div style="margin-top: 18px;">
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
                <el-tag v-if="row.memberLocked" type="warning">成员已冻结</el-tag>
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
      </div>

      <div class="section-card">
        <span class="section-eyebrow">ungrouped</span>
        <h3>未分组学生</h3>
        <div style="margin-top: 18px;">
          <el-table :data="manage.ungroupedStudents" size="small">
            <el-table-column prop="name" label="姓名" />
            <el-table-column prop="username" label="账号" width="140" />
          </el-table>
          <el-empty v-if="!manage.ungroupedStudents.length" description="当前没有未分组学生" :image-size="72" />
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px">
      <div class="dialog-content">
        <div class="section-card">
          <span class="section-eyebrow">group editor</span>
          <h3>{{ dialogTitle }}</h3>
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
              title="该小组已有提交或评分记录，当前只能修改组名，不能调整组员。"
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
