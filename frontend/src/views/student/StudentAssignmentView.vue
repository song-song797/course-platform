<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getStudentAssignment } from '../../api/student'

const route = useRoute()
const router = useRouter()
const assignment = ref(null)
const submissionBlocked = computed(() => assignment.value?.mode === 'GROUP' && assignment.value?.ungroupedForGroupAssignment)

onMounted(async () => {
  assignment.value = await getStudentAssignment(route.params.assignmentId)
})
</script>

<template>
  <div class="page-shell" v-if="assignment">
    <section class="page-hero">
      <div class="page-hero__card">
        <span class="page-hero__eyebrow">assignment overview</span>
        <h1 class="page-hero__title">{{ assignment.title }}</h1>
        <p class="page-hero__description">
          查看作业说明、评分维度和当前进度，然后继续前往项目提交、项目广场互评和个人结果看板。
        </p>
        <div class="page-hero__meta">
          <span>{{ assignment.courseName }}</span>
          <span>{{ assignment.mode }}</span>
          <span>截止 {{ assignment.deadline }}</span>
        </div>
        <div class="page-hero__actions">
          <el-button :disabled="submissionBlocked" @click="router.push(`/student/assignments/${assignment.id}/submission`)">项目提交</el-button>
          <el-button type="primary" @click="router.push(`/student/assignments/${assignment.id}/projects`)">项目广场</el-button>
          <el-button type="success" @click="router.push(`/student/assignments/${assignment.id}/dashboard`)">我的看板</el-button>
        </div>
      </div>

      <div class="page-hero__side">
        <span class="layout-chip">status</span>
        <h3>{{ assignment.displayStatus || assignment.status }}</h3>
        <p>互评分 {{ assignment.peerWeight }}%，教师评分 {{ assignment.teacherWeight }}%，作业截止后会立即开放评分窗口并在教师评分结束后自动冻结最终成绩。</p>
      </div>
    </section>

    <div class="stack">
      <el-alert
        :title="assignment.displayStatus || assignment.status"
        :description="assignment.resultsPublished ? `最终成绩已生成${assignment.resultsPublishedAt ? `：${assignment.resultsPublishedAt}` : ''}` : '当前阶段请以实时成绩和实时榜单为准；截止后学生互评会立即开放 24 小时，教师评分同步开放 48 小时。'"
        :type="assignment.resultsPublished ? 'success' : (assignment.status === 'REVIEWING' ? 'warning' : 'info')"
        :closable="false"
        show-icon
      />

      <div class="split-grid">
        <div class="section-card section-card--accent">
          <span class="section-eyebrow">brief</span>
          <h3>作业说明</h3>
          <p class="section-subtitle">{{ assignment.description }}</p>
        </div>

        <div class="section-card" v-if="assignment.mode === 'GROUP'">
          <span class="section-eyebrow">group status</span>
          <h3>我的小组</h3>
          <div v-if="assignment.myGroup" class="stack" style="gap: 8px; margin-top: 14px;">
            <strong>{{ assignment.myGroup.groupName }}</strong>
            <div class="muted">组员：{{ assignment.myGroup.members.map((item) => item.name).join(' / ') }}</div>
            <div v-if="assignment.myGroup.submission" class="muted">
              当前提交：{{ assignment.myGroup.submission.projectName }} · {{ assignment.myGroup.submission.submittedAt || '--' }}
            </div>
          </div>
          <el-alert
            v-else
            title="你暂时还没有被教师分配到小组"
            description="当前不能提交项目，但仍可进入项目广场参与互评。"
            type="warning"
            :closable="false"
            show-icon
            style="margin-top: 16px;"
          />
        </div>
      </div>

      <div class="section-card">
        <span class="section-eyebrow">rubric</span>
        <h3>评分维度</h3>
        <div style="margin-top: 16px;">
          <el-table :data="assignment.rubric">
            <el-table-column prop="name" label="维度" />
            <el-table-column prop="description" label="说明" />
            <el-table-column prop="weight" label="权重(%)" width="120" />
          </el-table>
        </div>
      </div>
    </div>
  </div>
</template>
