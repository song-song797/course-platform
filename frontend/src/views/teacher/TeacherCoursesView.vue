<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getTeacherCourses } from '../../api/teacher'

const router = useRouter()
const page = ref({ list: [], total: 0, pageNo: 1, pageSize: 10 })

async function loadCourses(pageNo = page.value.pageNo) {
  page.value = await getTeacherCourses({ pageNo, pageSize: page.value.pageSize })
}

function handlePageChange(pageNo) {
  loadCourses(pageNo)
}

onMounted(() => {
  loadCourses(1)
})

const totalAssignments = computed(() => page.value.list.reduce((sum, course) => sum + (course.assignments?.length || 0), 0))
</script>

<template>
  <div class="page-shell">
    <section class="page-hero">
      <div class="page-hero__card">
        <span class="page-hero__eyebrow">teacher workspace</span>
        <h1 class="page-hero__title">Organize rubric, review records and statistics from one teaching dashboard</h1>
        <p class="page-hero__description">
          每门课程下的作业都围绕同一套真实数据展开。你可以从这里直接进入小组管理、
          Rubric 配置、教师评分和统计分析。
        </p>
        <div class="page-hero__meta">
          <span>共 {{ page.total }} 门课程</span>
          <span>本页 {{ totalAssignments }} 个作业</span>
          <span>统一教学流程</span>
        </div>
      </div>

      <div class="page-hero__side">
        <span class="layout-chip">teacher flow</span>
        <h3>组队、评分、统计在同一条链路上</h3>
        <p>教师侧页面围绕作业展开，评分治理、Rubric 和统计不会彼此割裂。</p>
      </div>
    </section>

    <div class="collection-grid">
      <article v-for="course in page.list" :key="course.id" class="collection-card">
        <div class="collection-card__header">
          <div>
            <strong>{{ course.name }}</strong>
            <p class="section-subtitle">课程编码：{{ course.code }}</p>
          </div>
          <span class="layout-chip">{{ course.term }}</span>
        </div>

        <div v-if="course.assignments?.length" class="mini-card-grid">
          <div v-for="assignment in course.assignments" :key="assignment.id" class="mini-card">
            <div class="collection-card__header">
              <div>
                <strong>{{ assignment.title }}</strong>
                <p>{{ assignment.mode }} · 截止 {{ assignment.deadline }}</p>
              </div>
              <el-tag size="small" :type="assignment.resultsPublished ? 'success' : (assignment.status === 'REVIEWING' ? 'warning' : 'info')">
                {{ assignment.displayStatus || assignment.status }}
              </el-tag>
            </div>

            <div class="toolbar" style="margin-top: 12px;">
              <el-button
                v-if="assignment.mode === 'GROUP'"
                type="warning"
                @click="router.push(`/teacher/assignments/${assignment.id}/groups`)"
              >
                小组管理
              </el-button>
              <el-button @click="router.push(`/teacher/assignments/${assignment.id}/review`)">评分记录</el-button>
              <el-button type="primary" @click="router.push(`/teacher/assignments/${assignment.id}/rubric`)">Rubric</el-button>
              <el-button type="success" @click="router.push(`/teacher/assignments/${assignment.id}/stats`)">统计</el-button>
            </div>
          </div>
        </div>

        <div v-else class="landing-empty">
          <p>当前课程还没有作业，请等待管理员初始化或稍后查看。</p>
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
  </div>
</template>
