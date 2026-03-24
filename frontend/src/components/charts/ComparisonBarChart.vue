<script setup>
import * as echarts from 'echarts'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  items: {
    type: Array,
    default: () => [],
  },
  mineLabel: {
    type: String,
    default: '我的表现',
  },
  averageLabel: {
    type: String,
    default: '课程均值',
  },
})

const chartRef = ref(null)
let chart
let resizeObserver

function tooltipFormatter(params) {
  const current = props.items[params?.[0]?.dataIndex]
  if (!current) {
    return ''
  }

  const deltaPrefix = current.delta > 0 ? '+' : ''
  return [
    `<strong>${current.name}</strong>`,
    `${props.mineLabel}：${current.mine}`,
    `${props.averageLabel}：${current.average}`,
    `差值：${deltaPrefix}${current.delta}`,
  ].join('<br/>')
}

function renderChart() {
  if (!chartRef.value) {
    return
  }
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }

  chart.setOption({
    grid: {
      left: 24,
      right: 24,
      top: 42,
      bottom: 12,
      containLabel: true,
    },
    legend: {
      top: 0,
      right: 0,
      itemWidth: 12,
      itemHeight: 12,
      textStyle: {
        color: '#587293',
        fontSize: 12,
      },
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
        shadowStyle: {
          color: 'rgba(63, 126, 255, 0.06)',
        },
      },
      backgroundColor: '#10233f',
      borderWidth: 0,
      textStyle: { color: '#f8fbff' },
      formatter: tooltipFormatter,
    },
    xAxis: {
      type: 'value',
      min: 0,
      max: 100,
      splitNumber: 5,
      axisLabel: {
        color: '#7c95b6',
      },
      splitLine: {
        lineStyle: {
          color: '#e8f0fb',
        },
      },
      axisLine: { show: false },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'category',
      inverse: true,
      data: props.items.map((item) => item.name),
      axisTick: { show: false },
      axisLine: { show: false },
      axisLabel: {
        color: '#587293',
        fontSize: 12,
        fontWeight: 600,
      },
    },
    series: [
      {
        name: props.averageLabel,
        type: 'bar',
        data: props.items.map((item) => item.average),
        barMaxWidth: 12,
        itemStyle: {
          color: '#c9ddff',
          borderRadius: [0, 999, 999, 0],
        },
      },
      {
        name: props.mineLabel,
        type: 'bar',
        data: props.items.map((item) => item.mine),
        barMaxWidth: 12,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
            { offset: 0, color: '#2f6fe4' },
            { offset: 1, color: '#5a9cff' },
          ]),
          borderRadius: [0, 999, 999, 0],
        },
        label: {
          show: true,
          position: 'right',
          distance: 10,
          color: '#16324f',
          fontSize: 12,
          fontWeight: 700,
          formatter: ({ dataIndex }) => {
            const item = props.items[dataIndex]
            if (!item) {
              return ''
            }
            const deltaPrefix = item.delta > 0 ? '+' : ''
            return `${item.mine}  ${deltaPrefix}${item.delta}`
          },
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

watch(
  () => [props.items, props.mineLabel, props.averageLabel],
  renderChart,
  { deep: true },
)

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  window.removeEventListener('resize', renderChart)
  chart?.dispose()
})
</script>

<template>
  <div ref="chartRef" class="chart-slot chart-slot--comparison" />
</template>
