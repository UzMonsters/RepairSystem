<script setup lang="ts">
const props = defineProps<{ rating: number }>()

const stars = computed(() => [1, 2, 3, 4, 5].map(n => ({ n, full: n <= Math.round(props.rating) })))
</script>

<template>
  <span
    class="text-warning"
    :title="`${rating} / 5`"
  >
    <i
      v-for="s in stars"
      :key="s.n"
      :class="s.full ? 'bi bi-star-fill' : 'bi bi-star'"
    />
  </span>
</template>
