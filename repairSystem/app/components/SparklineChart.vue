<script setup>
const props = defineProps({
  data: { type: Array, required: true },
  type: { type: String, default: 'area' },
  color: { type: String, default: undefined },
  height: { type: [Number, String], default: 60 }
})

const options = computed(() => ({
  chart: { sparkline: { enabled: true } },
  stroke: { curve: 'smooth', width: 2 },
  fill: { opacity: 0.3 },
  colors: props.color ? [props.color] : undefined,
  tooltip: { enabled: true }
}))
</script>

<template>
  <ApexChart
    :type="type"
    :height="height"
    :series="[{ data }]"
    :options="options"
  />
</template>
