<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTeacherAssignment, updateRubric } from '../../api/teacher'

const route = useRoute()
const assignment = ref(null)
const items = reactive([])

onMounted(async () => {
  assignment.value = await getTeacherAssignment(route.params.assignmentId)
  items.splice(0, items.length, ...assignment.value.rubric.map((item) => ({ ...item })))
})

const totalWeight = computed(() => items.reduce((sum, item) => sum + Number(item.weight || 0), 0))
const readonly = computed(() => Boolean(assignment.value?.resultsPublished))

function handleAddItem() {
  if (!readonly.value) {
    items.push({ id: null, name: '', description: '', weight: 0 })
  }
}

function handleRemoveItem(index) {
  if (!readonly.value) {
    items.splice(index, 1)
  }
}

async function handleSave() {
  if (readonly.value) {
    ElMessage.warning('最终成绩已发布，Rubric 已锁定为只读')
    return
  }
  if (totalWeight.value !== 100) {
    ElMessage.warning('Rubric 权重总和必须为 100%')
    return
  }
  await updateRubric(route.params.assignmentId, { items })
  ElMessage.success('Rubric 已更新')
  assignment.value = await getTeacherAssignment(route.params.assignmentId)
  items.splice(0, items.length, ...assignment.value.rubric.map((item) => ({ ...item })))
}
</script>

<template>
  <div class="page-shell" v-if="assignment">
    <section class="page-hero">
      <div class="page-hero__card">
        <span class="page-hero__eyebrow">rubric editor</span>
        <h1 class="page-hero__title">Shape the scoring dimensions before evaluation records are finalized</h1>
        <p class="page-hero__description">
          Rubric 会直接影响实时分与最终分的计算结果。请在成绩发布前完成评分项、说明和权重配置。
        </p>
        <div class="page-hero__meta">
          <span>{{ assignment.title }}</span>
          <span>当前权重总和 {{ totalWeight }}%</span>
          <span>{{ readonly ? '只读' : '可编辑' }}</span>
        </div>
        <div class="page-hero__actions">
          <el-button :disabled="readonly" @click="handleAddItem">新增评分项</el-button>
          <el-button type="primary" :disabled="readonly" @click="handleSave">保存 Rubric</el-button>
        </div>
      </div>

      <div class="page-hero__side">
        <span class="layout-chip">assignment status</span>
        <h3>{{ assignment.displayStatus || assignment.status }}</h3>
        <p>{{ readonly ? '最终成绩已发布，Rubric 已锁定。' : '发布前仍可继续调整维度和权重。' }}</p>
      </div>
    </section>

    <el-alert
      :title="assignment.displayStatus || assignment.status"
      :description="readonly ? `最终成绩已发布${assignment.resultsPublishedAt ? `：${assignment.resultsPublishedAt}` : ''}，Rubric 已锁定。` : 'Rubric 会直接影响实时分和最终分计算，请在发布前完成调整。'"
      :type="readonly ? 'success' : 'warning'"
      :closable="false"
      show-icon
      style="margin-bottom: 20px;"
    />

    <div class="stack">
      <div v-for="(item, index) in items" :key="item.id ?? `new-${index}`" class="section-card">
        <span class="section-eyebrow">rubric item</span>
        <h3>评分项 {{ index + 1 }}</h3>
        <el-row :gutter="20" style="margin-top: 18px;">
          <el-col :span="8"><el-input v-model="item.name" :disabled="readonly" placeholder="评分维度" /></el-col>
          <el-col :span="10"><el-input v-model="item.description" :disabled="readonly" placeholder="评分说明" /></el-col>
          <el-col :span="6"><el-input-number v-model="item.weight" :disabled="readonly" :min="0" :max="100" style="width: 100%;" /></el-col>
        </el-row>
        <div class="toolbar" style="margin-top: 14px; justify-content: flex-end;">
          <el-button type="danger" link :disabled="readonly" @click="handleRemoveItem(index)">删除</el-button>
        </div>
      </div>
    </div>
  </div>
</template>
