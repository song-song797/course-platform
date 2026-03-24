<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getStudentAssignment } from '../../api/student'
import {
  getSubmissionCloseAt,
  hasLateWindow,
  isInLateWindow,
  isSubmissionClosed,
} from '../../utils/assignment'

const route = useRoute()
const router = useRouter()
const assignment = ref(null)
const now = ref(Date.now())
const submissionBlocked = computed(() => assignment.value?.mode === 'GROUP' && assignment.value?.ungroupedForGroupAssignment)

let clock = null

function modeLabel(mode) {
  return mode === 'GROUP' ? '小组作业' : '个人作业'
}

const submissionCloseAt = computed(() => getSubmissionCloseAt(assignment.value))
const lateWindowEnabled = computed(() => hasLateWindow(assignment.value))
const lateWindowActive = computed(() => isInLateWindow(assignment.value, new Date(now.value)))
const submissionClosed = computed(() => isSubmissionClosed(assignment.value, new Date(now.value)))
const submissionActionDisabled = computed(() => submissionBlocked.value || submissionClosed.value)

const statusAlert = computed(() => {
  if (!assignment.value) {
    return null
  }

  if (assignment.value.resultsPublished) {
    return {
      title: assignment.value.displayStatus || assignment.value.status,
      description: assignment.value.resultsPublishedAt
        ? `最终成绩已发布：${assignment.value.resultsPublishedAt}`
        : '最终成绩已发布，可前往结果看板查看最终得分和排名。',
      type: 'success',
    }
  }

  if (submissionClosed.value) {
    return {
      title: '提交窗口已关闭',
      description: '当前作业已截止，不能再提交或修改项目，可继续查看项目广场和结果看板。',
      type: 'error',
    }
  }

  if (lateWindowActive.value) {
    return {
      title: '当前处于迟交窗口',
      description: `你仍可在 ${submissionCloseAt.value} 前提交，但提交记录会被标记为迟交。`,
      type: 'warning',
    }
  }

  if (lateWindowEnabled.value) {
    return {
      title: assignment.value.displayStatus || assignment.value.status,
      description: `正常截止时间为 ${assignment.value.deadline}，错过后仍可在 ${submissionCloseAt.value} 前完成迟交提交。`,
      type: 'info',
    }
  }

  return {
    title: assignment.value.displayStatus || assignment.value.status,
    description: '当前仍处于提交阶段，请按截止时间完成项目材料整理与提交。',
    type: 'info',
  }
})

const nextStepDescription = computed(() => {
  if (!assignment.value) {
    return ''
  }

  if (submissionClosed.value) {
    return '提交窗口已经关闭，后续可重点关注项目广场中的互评状态和结果看板中的成绩发布。'
  }

  if (lateWindowActive.value) {
    return `当前处于迟交窗口，请在 ${submissionCloseAt.value} 前完成提交，系统会自动标记为迟交。`
  }

  if (lateWindowEnabled.value) {
    return `请先按正常截止时间 ${assignment.value.deadline} 准备提交材料；若错过，仍可在 ${submissionCloseAt.value} 前完成迟交。`
  }

  return `请在 ${submissionCloseAt.value || assignment.value.deadline || '截止时间'} 前确认仓库、演示、文档和附件是否准备完整，再进入提交页统一录入。`
})

onMounted(async () => {
  clock = window.setInterval(() => {
    now.value = Date.now()
  }, 30000)
  assignment.value = await getStudentAssignment(route.params.assignmentId)
})

onUnmounted(() => {
  if (clock) {
    window.clearInterval(clock)
  }
})
</script>

<template>
  <div class="page-shell" v-if="assignment">
    <section class="page-head">
      <div class="page-head__main">
        <span class="page-head__eyebrow">作业详情</span>
        <h2 class="page-head__title">{{ assignment.title }}</h2>
        <p class="page-head__description">
          在这里查看作业说明、评分维度和当前阶段状态，然后继续前往作品提交、项目广场互评和结果看板。
        </p>
        <div class="page-head__stats">
          <span>{{ assignment.courseName }}</span>
          <span>{{ modeLabel(assignment.mode) }}</span>
          <span>正常截止 {{ assignment.deadline || '未设置' }}</span>
          <span v-if="lateWindowEnabled">迟交截止 {{ submissionCloseAt || '未设置' }}</span>
          <span v-else>提交关闭 {{ submissionCloseAt || '未设置' }}</span>
        </div>
        <div class="page-head__actions">
          <el-button :disabled="submissionActionDisabled" @click="router.push(`/student/assignments/${assignment.id}/submission`)">
            提交作品
          </el-button>
          <el-button type="primary" @click="router.push(`/student/assignments/${assignment.id}/projects`)">项目广场</el-button>
          <el-button type="success" @click="router.push(`/student/assignments/${assignment.id}/dashboard`)">结果看板</el-button>
        </div>
      </div>

      <div class="page-head__aside">
        <span class="layout-chip">当前状态</span>
        <h3>{{ assignment.displayStatus || assignment.status }}</h3>
        <p>学生互评分占 {{ assignment.peerWeight }}%，教师评分占 {{ assignment.teacherWeight }}%，结果发布后会冻结为最终成绩。</p>
      </div>
    </section>

    <el-alert
      v-if="statusAlert"
      :title="statusAlert.title"
      :description="statusAlert.description"
      :type="statusAlert.type"
      :closable="false"
      show-icon
    />

    <div class="content-grid">
      <div class="stack">
        <section class="section-card section-card--accent">
          <span class="section-eyebrow">作业说明</span>
          <h3 style="margin-top: 14px;">任务目标与提交要求</h3>
          <p class="section-subtitle" style="margin-top: 18px;">{{ assignment.description }}</p>
        </section>

        <section class="section-card">
          <span class="section-eyebrow">评分规则</span>
          <h3 style="margin-top: 14px;">Rubric 维度</h3>
          <div class="data-table-wrap" style="margin-top: 18px;">
            <el-table :data="assignment.rubric">
              <el-table-column prop="name" label="维度" />
              <el-table-column prop="description" label="说明" />
              <el-table-column prop="weight" label="权重(%)" width="120" />
            </el-table>
          </div>
        </section>
      </div>

      <div class="stack">
        <section v-if="assignment.mode === 'GROUP'" class="section-card">
          <span class="section-eyebrow">分组状态</span>
          <h3 style="margin-top: 14px;">我的小组</h3>
          <div v-if="assignment.myGroup" class="stack" style="margin-top: 18px; gap: 12px;">
            <div class="mini-card">
              <strong>{{ assignment.myGroup.groupName }}</strong>
              <p class="section-subtitle">组员：{{ assignment.myGroup.members.map((item) => item.name).join(' / ') }}</p>
            </div>
            <div v-if="assignment.myGroup.submission" class="mini-card">
              <strong>{{ assignment.myGroup.submission.projectName }}</strong>
              <p class="section-subtitle">当前提交时间：{{ assignment.myGroup.submission.submittedAt || '--' }}</p>
            </div>
          </div>
          <el-alert
            v-else
            title="你暂时还没有被分配到小组"
            description="当前不能提交作品，但仍可进入项目广场查看互评信息。"
            type="warning"
            :closable="false"
            show-icon
            style="margin-top: 18px;"
          />
        </section>

        <section class="section-card">
          <span class="section-eyebrow">下一步</span>
          <h3 style="margin-top: 14px;">推荐操作</h3>
          <div class="stack" style="margin-top: 18px; gap: 16px;">
            <div class="mini-card">
              <strong>{{ lateWindowActive ? '尽快完成迟交提交' : '先检查提交材料' }}</strong>
              <p class="section-subtitle">{{ nextStepDescription }}</p>
            </div>
            <div class="mini-card">
              <strong>互评阶段关注项目广场</strong>
              <p class="section-subtitle">项目广场会保留全量项目列表，并标记当前不可评的项目及原因；你只需要处理“可评价”的项目。</p>
            </div>
            <div class="mini-card">
              <strong>结果发布后查看看板</strong>
              <p class="section-subtitle">结果看板会汇总排名、维度画像和匿名评语，方便复盘项目表现。</p>
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>
