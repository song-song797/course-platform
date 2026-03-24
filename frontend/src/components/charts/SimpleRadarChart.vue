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
let resizeObserver

function renderChart() {
  if (!chartRef.value) {
    return
  }
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }

  chart.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: '#10233f',
      borderWidth: 0,
      textStyle: { color: '#f8fbff' },
    },
    radar: {
      radius: '64%',
      indicator: props.items.map((item) => ({ name: item.name, max: 100 })),
      splitArea: {
        areaStyle: {
          color: ['rgba(63, 126, 255, 0.03)', 'rgba(63, 126, 255, 0.07)'],
        },
      },
      splitLine: { lineStyle: { color: '#d8e6fb' } },
      axisLine: { lineStyle: { color: '#d8e6fb' } },
      name: { color: '#587293' },
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: props.items.map((item) => item.value),
            areaStyle: { color: 'rgba(63, 126, 255, 0.24)' },
            lineStyle: { color: '#2f6fe4', width: 2 },
            itemStyle: { color: '#2f6fe4' },
          },
        ],
      },
    ],
  })
}

onMounted(() => {
  renderChart()
  resizeObserver = new ResizeObserver(() => chart?.resize())
  if (chartRef.value) {
    resizeObserver.observe(chartRef.value)
  }
  window.addEventListener('resize', renderChart)
})

watch(() => props.items, renderChart, { deep: true })

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  window.removeEventListener('resize', renderChart)
  chart?.dispose()
})
</script>

<template>
  <div ref="chartRef" class="chart-slot" />
</template>
