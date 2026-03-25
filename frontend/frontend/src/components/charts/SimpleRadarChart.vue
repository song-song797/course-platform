<script setup>
import * as echarts from 'echarts'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { readChartTheme } from './chartTheme'

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

  const theme = readChartTheme(chartRef.value)

  chart.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: theme.tooltipBackground,
      borderWidth: 0,
      textStyle: { color: theme.tooltipText },
    },
    radar: {
      radius: '64%',
      indicator: props.items.map((item) => ({ name: item.name, max: 100 })),
      splitArea: {
        areaStyle: {
          color: [theme.radarSplitA, theme.radarSplitB],
        },
      },
      splitLine: { lineStyle: { color: theme.gridLine } },
      axisLine: { lineStyle: { color: theme.gridLine } },
      name: { color: theme.axisText },
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: props.items.map((item) => item.value),
            areaStyle: { color: theme.primaryArea },
            lineStyle: { color: theme.primary, width: 2 },
            itemStyle: { color: theme.primary },
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
