<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { uploadFile } from '../../api/files'
import { getMySubmission, getStudentAssignment, submitProject, updateProject } from '../../api/student'
import {
  getSubmissionCloseAt,
  hasLateWindow,
  isInLateWindow,
  isSubmissionClosed,
} from '../../utils/assignment'

const route = useRoute()
const assignment = ref(null)
const submissionId = ref(null)
const uploading = ref(false)
const now = ref(Date.now())

const form = reactive({
  projectName: '',
  repoUrl: '',
  videoUrl: '',
  previewUrl: '',
  docUrl: '',
  attachmentUrl: '',
  description: '',
})

let clock = null

const submissionBlocked = computed(() => assignment.value?.mode === 'GROUP' && assignment.value?.ungroupedForGroupAssignment)
const submissionCloseAt = computed(() => getSubmissionCloseAt(assignment.value))
const lateWindowEnabled = computed(() => hasLateWindow(assignment.value))
const lateWindowActive = computed(() => isInLateWindow(assignment.value, new Date(now.value)))
const submissionClosed = computed(() => isSubmissionClosed(assignment.value, new Date(now.value)))
const formDisabled = computed(() => submissionBlocked.value || submissionClosed.value)

const submissionStatusAlert = computed(() => {
  if (!assignment.value) {
    return null
  }

  if (submissionClosed.value) {
    return {
      title: '提交窗口已关闭',
      description: '当前作业已截止，不能再提交或修改项目内容。',
      type: 'error',
    }
  }

  if (lateWindowActive.value) {
    return {
      title: '当前处于迟交窗口',
      description: `你仍可在 ${submissionCloseAt.value} 前提交，系统会把本次提交标记为迟交。`,
      type: 'warning',
    }
  }

  if (lateWindowEnabled.value) {
    return {
      title: '提交窗口已开启迟交',
      description: `正常截止时间为 ${assignment.value.deadline}，如错过仍可在 ${submissionCloseAt.value} 前继续提交。`,
      type: 'info',
    }
  }

  return {
    title: '当前仍可正常提交',
    description: `请在 ${submissionCloseAt.value || assignment.value.deadline || '截止时间'} 前完成材料更新与保存。`,
    type: 'info',
  }
})

onMounted(async () => {
  clock = window.setInterval(() => {
    now.value = Date.now()
  }, 30000)

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

onUnmounted(() => {
  if (clock) {
    window.clearInterval(clock)
  }
})

async function handleSubmit() {
  if (submissionBlocked.value) {
    ElMessage.warning('你还未被教师分配到小组，暂时不能提交项目')
    return
  }

  if (submissionClosed.value) {
    ElMessage.warning('当前已经超过提交关闭时间，不能再提交或修改项目')
    return
  }

  const payload = { ...form, memberUserIds: [] }
  const result = submissionId.value
    ? await updateProject(submissionId.value, payload)
    : await submitProject(route.params.assignmentId, payload)
  submissionId.value = result.id
  ElMessage.success(lateWindowActive.value ? '项目已提交，系统已标记为迟交' : '项目提交已保存')
}

async function handleFileUpload(option) {
  try {
    uploading.value = true
    const formData = new FormData()
    formData.append('file', option.file)
    const result = await uploadFile(formData)
    form.attachmentUrl = result.url || result.path || ''
    ElMessage.success('附件上传成功')
  } finally {
    uploading.value = false
  }
}

function openAttachment() {
  if (!form.attachmentUrl) {
    return
  }
  window.open(form.attachmentUrl, '_blank', 'noopener,noreferrer')
}

async function copyAttachmentUrl() {
  if (!form.attachmentUrl) {
    return
  }

  if (!navigator.clipboard?.writeText) {
    ElMessage.warning('当前浏览器不支持直接复制，请手动复制附件链接')
    return
  }

  try {
    await navigator.clipboard.writeText(form.attachmentUrl)
    ElMessage.success('附件链接已复制')
  } catch {
    ElMessage.warning('复制失败，请手动复制附件链接')
  }
}
</script>

<template>
  <div class="page-shell" v-if="assignment">
    <section class="page-head">
      <div class="page-head__main">
        <span class="page-head__eyebrow">作品提交</span>
        <h2 class="page-head__title">统一录入项目链接、附件与说明信息</h2>
        <p class="page-head__description">
          把仓库、视频、预览、文档和补充说明集中维护，保证教师评分与同学互评基于同一份项目资料展开。
        </p>
        <div class="page-head__stats">
          <span>{{ assignment.title }}</span>
          <span>{{ assignment.mode === 'GROUP' ? '小组提交' : '个人提交' }}</span>
          <span>{{ submissionId ? '已有提交记录' : '首次提交' }}</span>
        </div>
      </div>

      <div class="page-head__aside">
        <span class="layout-chip">{{ lateWindowEnabled ? '提交时间窗口' : '提交关闭时间' }}</span>
        <h3>{{ lateWindowEnabled ? '请留意正常截止与迟交截止' : (submissionCloseAt || '未设置') }}</h3>
        <div class="stack" style="margin-top: 18px; gap: 12px;">
          <div class="mini-card">
            <strong>正常截止</strong>
            <p class="section-subtitle">{{ assignment.deadline || '未设置' }}</p>
          </div>
          <div class="mini-card">
            <strong>{{ lateWindowEnabled ? '迟交截止' : '提交关闭' }}</strong>
            <p class="section-subtitle">{{ submissionCloseAt || '未设置' }}</p>
          </div>
        </div>
      </div>
    </section>

    <div class="content-grid">
      <div class="section-card section-card--accent">
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
          description="当前不能提交项目，请先联系教师完成分组。"
          type="warning"
          :closable="false"
          show-icon
          style="margin-bottom: 20px;"
        />
        <el-alert
          v-else
          title="个人作业会自动使用当前登录学生作为唯一成员"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 20px;"
        />

        <el-alert
          v-if="submissionStatusAlert"
          :title="submissionStatusAlert.title"
          :description="submissionStatusAlert.description"
          :type="submissionStatusAlert.type"
          :closable="false"
          show-icon
          style="margin-bottom: 20px;"
        />

        <span class="section-eyebrow">提交表单</span>
        <h3 style="margin-top: 14px;">项目基础信息</h3>

        <el-form :model="form" label-position="top" style="margin-top: 18px;">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="项目名称">
                <el-input v-model="form.projectName" :disabled="formDisabled" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="代码仓库链接">
                <el-input v-model="form.repoUrl" :disabled="formDisabled" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="演示视频">
                <el-input v-model="form.videoUrl" :disabled="formDisabled" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="在线预览">
                <el-input v-model="form.previewUrl" :disabled="formDisabled" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="说明文档">
                <el-input v-model="form.docUrl" :disabled="formDisabled" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="附件">
            <div class="toolbar" style="width: 100%; align-items: flex-start;">
              <el-upload :show-file-list="false" :http-request="handleFileUpload" :disabled="uploading || formDisabled">
                <el-button :loading="uploading" :disabled="formDisabled">上传附件</el-button>
              </el-upload>
              <el-input v-model="form.attachmentUrl" :disabled="formDisabled" placeholder="也可以直接填写附件 URL" />
              <el-button v-if="form.attachmentUrl" @click="openAttachment">打开附件</el-button>
              <el-button v-if="form.attachmentUrl" @click="copyAttachmentUrl">复制链接</el-button>
            </div>
          </el-form-item>

          <el-form-item label="补充说明">
            <el-input v-model="form.description" :disabled="formDisabled" type="textarea" :rows="5" />
          </el-form-item>

          <div class="toolbar">
            <el-button type="primary" :disabled="formDisabled" @click="handleSubmit">
              {{ submissionId ? '更新提交' : '保存提交' }}
            </el-button>
            <span v-if="submissionId" class="muted">当前提交 ID：{{ submissionId }}</span>
          </div>
        </el-form>
      </div>

      <div class="stack">
        <section class="section-card">
          <span class="section-eyebrow">填写建议</span>
          <h3 style="margin-top: 14px;">提交前检查清单</h3>
          <div class="stack" style="margin-top: 18px; gap: 16px;">
            <div class="mini-card">
              <strong>仓库与演示保持一致</strong>
              <p class="section-subtitle">确保视频、预览和仓库对应同一版本，便于评分时核对功能实现。</p>
            </div>
            <div class="mini-card">
              <strong>文档说明写清分工与亮点</strong>
              <p class="section-subtitle">这会直接影响教师理解项目结构和成员贡献。</p>
            </div>
            <div class="mini-card">
              <strong>附件适合作为补充材料</strong>
              <p class="section-subtitle">例如答辩 PPT、测试报告、设计说明书等，避免把核心信息只放在附件里。</p>
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>
