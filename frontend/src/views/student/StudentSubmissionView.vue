<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { uploadFile } from '../../api/files'
import { getMySubmission, getStudentAssignment, submitProject, updateProject } from '../../api/student'

const route = useRoute()
const assignment = ref(null)
const submissionId = ref(null)
const uploading = ref(false)

const form = reactive({
  projectName: '',
  repoUrl: '',
  videoUrl: '',
  previewUrl: '',
  docUrl: '',
  attachmentUrl: '',
  description: '',
})
const submissionBlocked = computed(() => assignment.value?.mode === 'GROUP' && assignment.value?.ungroupedForGroupAssignment)

onMounted(async () => {
  assignment.value = await getStudentAssignment(route.params.assignmentId)
  const submission = await getMySubmission(route.params.assignmentId)
  if (submission) {
    submissionId.value = submission.id
    Object.assign(form, {
      projectName: submission.projectName,
      repoUrl: submission.repoUrl,
      videoUrl: submission.videoUrl || '',
      previewUrl: submission.previewUrl || '',
      docUrl: submission.docUrl || '',
      attachmentUrl: submission.attachmentUrl || '',
      description: submission.description || '',
    })
  }
})

async function handleSubmit() {
  if (submissionBlocked.value) {
    ElMessage.warning('你还未被教师分配到小组，暂时不能提交项目')
    return
  }
  const payload = { ...form, memberUserIds: [] }
  const result = submissionId.value
    ? await updateProject(submissionId.value, payload)
    : await submitProject(route.params.assignmentId, payload)
  submissionId.value = result.id
  ElMessage.success('项目提交已保存')
}

async function handleFileUpload(option) {
  try {
    uploading.value = true
    const formData = new FormData()
    formData.append('file', option.file)
    const result = await uploadFile(formData)
    form.attachmentUrl = result.url
    ElMessage.success('附件上传成功')
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <div class="page-shell" v-if="assignment">
    <section class="page-hero">
      <div class="page-hero__card">
        <span class="page-hero__eyebrow">project submission</span>
        <h1 class="page-hero__title">Prepare your project links, assets and description in one submission form</h1>
        <p class="page-hero__description">
          统一填写仓库、视频、预览、文档和附件链接，让教师评分与同学互评都能基于同一份项目资料展开。
        </p>
        <div class="page-hero__meta">
          <span>{{ assignment.title }}</span>
          <span>{{ assignment.mode }}</span>
          <span>{{ submissionId ? '已存在提交记录' : '首次提交' }}</span>
        </div>
      </div>

      <div class="page-hero__side">
        <span class="layout-chip">deadline</span>
        <h3>{{ assignment.deadline }}</h3>
        <p>保存后可随时更新。若还未分组，系统会提醒你先联系教师完成分组。</p>
      </div>
    </section>

    <div class="section-card">
      <el-alert
        v-if="assignment.mode === 'GROUP' && assignment.myGroup"
        :title="`当前小组：${assignment.myGroup.groupName}`"
        :description="`组员：${assignment.myGroup.members.map((item) => item.name).join(' / ')}`"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 20px;"
      />
      <el-alert
        v-else-if="submissionBlocked"
        title="你还未被教师分配到小组"
        description="当前不能提交项目，请联系教师先完成分组。"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 20px;"
      />
      <el-alert
        v-else
        title="个人作业将自动使用当前登录学生作为唯一成员"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 20px;"
      />

      <el-form :model="form" label-position="top">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="项目名称">
              <el-input v-model="form.projectName" :disabled="submissionBlocked" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="GitHub / Gitee 链接">
              <el-input v-model="form.repoUrl" :disabled="submissionBlocked" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8"><el-form-item label="演示视频"><el-input v-model="form.videoUrl" :disabled="submissionBlocked" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="在线预览"><el-input v-model="form.previewUrl" :disabled="submissionBlocked" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="说明文档"><el-input v-model="form.docUrl" :disabled="submissionBlocked" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="附件">
          <div class="toolbar" style="width: 100%;">
            <el-upload :show-file-list="false" :http-request="handleFileUpload" :disabled="uploading || submissionBlocked">
              <el-button :loading="uploading">上传附件</el-button>
            </el-upload>
            <el-input v-model="form.attachmentUrl" :disabled="submissionBlocked" placeholder="也可以直接填写附件 URL" />
          </div>
        </el-form-item>
        <el-form-item label="补充说明">
          <el-input v-model="form.description" :disabled="submissionBlocked" type="textarea" :rows="4" />
        </el-form-item>
        <div class="toolbar">
          <el-button type="primary" :disabled="submissionBlocked" @click="handleSubmit">{{ submissionId ? '更新提交' : '保存提交' }}</el-button>
          <span class="muted" v-if="submissionId">当前提交 ID：{{ submissionId }}</span>
        </div>
      </el-form>
    </div>
  </div>
</template>
