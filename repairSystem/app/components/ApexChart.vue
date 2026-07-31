<script setup>
const props = defineProps({
  type: { type: String, default: 'line' },
  series: { type: Array, required: true },
  options: { type: Object, default: () => ({}) },
  height: { type: [Number, String], default: 350 },
  width: { type: [Number, String], default: undefined },
  deepWatch: { type: Boolean, default: false }
})

const el = ref(null)
let chart = null

function buildOptions() {
  const base = props.options ?? {}
  return {
    ...base,
    chart: {
      ...(base.chart ?? {}),
      type: props.type,
      height: props.height,
      width: props.width
    },
    series: props.series
  }
}

onMounted(async () => {
  if (!el.value) return
  const { default: ApexCharts } = await import('apexcharts')
  if (!el.value) return
  chart = new ApexCharts(el.value, buildOptions())
  await chart.render()
})

watch(
  () => props.series,
  series => chart?.updateSeries(series),
  { deep: props.deepWatch }
)

watch(
  () => [props.options, props.type, props.height, props.width],
  () => chart?.updateOptions(buildOptions()),
  { deep: true }
)

onBeforeUnmount(() => {
  chart?.destroy()
  chart = null
})
</script>

<template>
  <div ref="el" />
</template>
