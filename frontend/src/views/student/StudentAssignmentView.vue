<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getStudentAssignment } from '../../api/student'

const route = useRoute()
const router = useRouter()
const assignment = ref(null)
const submissionBlocked = computed(() => assignment.value?.mode === 'GROUP' && assignment.value?.ungroupedForGroupAssignment)

function modeLabel(mode) {
  return mode === 'GROUP' ? '小组作业' : '个人作业'
}

onMounted(async () => {
  assignment.value = await getStudentAssignment(route.params.assignmentId)
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
          <span>截止 {{ assignment.deadline }}</span>
        </div>
        <div class="page-head__actions">
          <el-button :disabled="submissionBlocked" @click="router.push(`/student/assignments/${assignment.id}/submission`)">提交作品</el-button>
          <el-button type="primary" @click="router.push(`/student/assignments/${assignment.id}/projects`)">项目广场</el-button>
          <el-button type="success" @click="router.push(`/student/assignments/${assignment.id}/dashboard`)">结果看板</el-button>
        </div>
      </div>

      <div class="page-head__aside">
        <span class="layout-chip">当前状态</span>
        <h3>{{ assignment.displayStatus || assignment.status }}</h3>
        <p>学生互评分占 {{ assignment.peerWeight }}%，教师评分占 {{ assignment.teacherWeight }}%，发布后会冻结最终成绩。</p>
      </div>
    </section>

    <el-alert
      :title="assignment.displayStatus || assignment.status"
      :description="assignment.resultsPublished ? `最终成绩已发布${assignment.resultsPublishedAt ? `：${assignment.resultsPublishedAt}` : ''}` : '当前阶段请以实时成绩和实时榜单为准。'"
      :type="assignment.resultsPublished ? 'success' : (assignment.status === 'REVIEWING' ? 'warning' : 'info')"
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
              <strong>先检查提交资料</strong>
              <p class="section-subtitle">确认仓库、演示、文档和附件是否准备完整，再进入提交页统一录入。</p>
            </div>
            <div class="mini-card">
              <strong>互评阶段关注项目广场</strong>
              <p class="section-subtitle">系统会根据规则过滤不可评项目，你只需要在广场中完成可评任务即可。</p>
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
