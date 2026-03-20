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
    grid: {
      left: 28,
      right: 12,
      top: 20,
      bottom: 24,
      containLabel: true,
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#10233f',
      borderWidth: 0,
      textStyle: { color: '#f8fbff' },
    },
    xAxis: {
      type: 'category',
      data: props.items.map((item) => item.name),
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#d8e6fb' } },
      axisLabel: { color: '#587293', fontSize: 12 },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#e8f0fb' } },
      axisLine: { show: false },
      axisLabel: { color: '#7c95b6' },
    },
    series: [
      {
        data: props.items.map((item) => item.value),
        type: 'bar',
        barMaxWidth: 28,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#5a9cff' },
            { offset: 1, color: '#2f6fe4' },
          ]),
          borderRadius: [10, 10, 0, 0],
        },
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
