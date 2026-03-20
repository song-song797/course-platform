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
    ElMessage.warning('最终成绩已发布，评分规则已锁定为只读')
    return
  }
  if (totalWeight.value !== 100) {
    ElMessage.warning('评分规则总权重必须等于 100%')
    return
  }
  await updateRubric(route.params.assignmentId, { items })
  ElMessage.success('评分规则已更新')
  assignment.value = await getTeacherAssignment(route.params.assignmentId)
  items.splice(0, items.length, ...assignment.value.rubric.map((item) => ({ ...item })))
}
</script>

<template>
  <div class="page-shell" v-if="assignment">
    <section class="page-head">
      <div class="page-head__main">
        <span class="page-head__eyebrow">评分规则</span>
        <h2 class="page-head__title">在评分记录固化前配置 Rubric 维度</h2>
        <p class="page-head__description">
          评分规则会直接影响实时分与最终分的计算结果。请在成绩发布前完成评分项、说明和权重配置。
        </p>
        <div class="page-head__stats">
          <span>{{ assignment.title }}</span>
          <span>当前权重总和 {{ totalWeight }}%</span>
          <span>{{ readonly ? '只读状态' : '可编辑状态' }}</span>
        </div>
        <div class="page-head__actions">
          <el-button :disabled="readonly" @click="handleAddItem">新增评分项</el-button>
          <el-button type="primary" :disabled="readonly" @click="handleSave">保存评分规则</el-button>
        </div>
      </div>

      <div class="page-head__aside">
        <span class="layout-chip">作业状态</span>
        <h3>{{ assignment.displayStatus || assignment.status }}</h3>
        <p>{{ readonly ? '最终成绩已发布，评分规则已经锁定。' : '发布前仍可调整维度与权重。' }}</p>
      </div>
    </section>

    <el-alert
      :title="assignment.displayStatus || assignment.status"
      :description="readonly ? `最终成绩已发布${assignment.resultsPublishedAt ? `：${assignment.resultsPublishedAt}` : ''}，评分规则已锁定。` : '评分规则会直接影响实时分与最终分的计算，请在发布前完成校验。'"
      :type="readonly ? 'success' : 'warning'"
      :closable="false"
      show-icon
    />

    <div class="stack">
      <section v-for="(item, index) in items" :key="item.id ?? `new-${index}`" class="section-card">
        <div class="panel-header">
          <div>
            <span class="section-eyebrow">评分项 {{ index + 1 }}</span>
            <h3 style="margin-top: 14px;">Rubric 配置</h3>
          </div>
          <el-button type="danger" link :disabled="readonly" @click="handleRemoveItem(index)">删除</el-button>
        </div>

        <el-row :gutter="20" style="margin-top: 18px;">
          <el-col :span="8">
            <el-input v-model="item.name" :disabled="readonly" placeholder="评分维度" />
          </el-col>
          <el-col :span="10">
            <el-input v-model="item.description" :disabled="readonly" placeholder="评分说明" />
          </el-col>
          <el-col :span="6">
            <el-input-number v-model="item.weight" :disabled="readonly" :min="0" :max="100" style="width: 100%;" />
          </el-col>
        </el-row>
      </section>
    </div>
  </div>
</template>
