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
    tooltip: { trigger: 'item' },
    radar: {
      indicator: props.items.map((item) => ({ name: item.name, max: 100 })),
      splitArea: { areaStyle: { color: ['rgba(37,99,235,0.04)', 'rgba(14,165,233,0.08)'] } },
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: props.items.map((item) => item.value),
            areaStyle: { color: 'rgba(37,99,235,0.22)' },
            lineStyle: { color: '#2563eb' },
            itemStyle: { color: '#1d4ed8' },
          },
        ],
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
