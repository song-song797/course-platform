<script setup>
import * as echarts from 'echarts'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  items: {
    type: Array,
    default: () => [],
  },
})

const chartRef = ref(null)
let chart

function renderChart() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: props.items.map((item) => item.name),
      axisLabel: { color: '#475569' },
    },
    yAxis: { type: 'value' },
    series: [
      {
        data: props.items.map((item) => item.value),
        type: 'bar',
        itemStyle: {
          color: '#2563eb',
          borderRadius: [8, 8, 0, 0],
        },
      },
    ],
  })
}

onMounted(renderChart)
watch(() => props.items, renderChart, { deep: true })
onBeforeUnmount(() => chart?.dispose())
</script>

<template>
  <div ref="chartRef" style="height: 320px;" />
</template>
