<script setup>
const props = defineProps({
  map: { type: String, default: 'world' },
  options: { type: Object, default: () => ({}) },
  height: { type: String, default: '400px' }
})

const el = ref(null)
let instance = null

onMounted(async () => {
  if (!el.value) return
  const mod = await import('jsvectormap')
  const JsVectorMap = mod.default ?? mod
  if (!el.value) return
  instance = new JsVectorMap({
    selector: el.value,
    map: props.map,
    ...props.options
  })
})

onBeforeUnmount(() => {
  instance?.destroy?.()
  instance = null
})
</script>

<template>
  <div
    ref="el"
    :style="{ height }"
  />
</template>
