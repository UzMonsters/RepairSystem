<script setup lang="ts">
import { ref, watch } from 'vue'

const { t } = useLocale()

const props = defineProps<{
  page: number
  size: number
  total: number
  totalPages?: number
  pageSizes?: number[]
}>()

const emit = defineEmits<{
  'update:page': [page: number]
  'update:size': [size: number]
}>()

const selectedSize = ref(props.size)

watch(() => props.size, (v) => {
  selectedSize.value = v
})

const pageCount = computed(() => {
  const count = props.totalPages ?? Math.max(1, Math.ceil(props.total / props.size))
  return Math.max(1, count)
})

const current = computed(() => Math.min(Math.max(1, props.page), pageCount.value))

const startIndex = computed(() => props.total === 0 ? 0 : (current.value - 1) * props.size + 1)
const endIndex = computed(() => Math.min(current.value * props.size, props.total))

const visiblePages = computed(() => {
  const count = pageCount.value
  const currentPage = current.value
  const pages: Array<number | 'ellipsis-start' | 'ellipsis-end'> = []
  const windowSize = 5
  if (count <= windowSize + 2) {
    for (let i = 1; i <= count; i++) pages.push(i)
  } else {
    const start = Math.max(2, currentPage - 1)
    const end = Math.min(count - 1, currentPage + 1)
    pages.push(1)
    if (start > 2) pages.push('ellipsis-start')
    for (let i = start; i <= end; i++) pages.push(i)
    if (end < count - 1) pages.push('ellipsis-end')
    pages.push(count)
  }
  return pages
})

function goTo(target: number) {
  if (target < 1 || target > pageCount.value || target === current.value) return
  emit('update:page', target)
}

function changeSize() {
  emit('update:size', selectedSize.value)
  emit('update:page', 1)
}
</script>

<template>
  <div class="pagination-bar">
    <span class="pagination-info">
      {{ t('showing') }} {{ startIndex }}–{{ endIndex }} {{ t('of') }} {{ total }}
    </span>

    <div class="pagination-actions">
      <div
        v-if="pageSizes?.length"
        class="pagination-size-wrap"
      >
        <span class="pagination-size-label">{{ t('rowsPerPage') }}</span>
        <select
          v-model="selectedSize"
          class="pagination-size"
          @change="changeSize"
        >
          <option
            v-for="s in pageSizes"
            :key="s"
            :value="s"
          >
            {{ s }}
          </option>
        </select>
      </div>

      <div class="pagination-controls">
        <button
          type="button"
          class="page-btn"
          :disabled="current <= 1"
          aria-label="Previous page"
          @click="goTo(current - 1)"
        >
          <i class="bi bi-chevron-left" />
        </button>

        <template v-for="(p, idx) in visiblePages" :key="`${p}-${idx}`">
          <span
            v-if="typeof p === 'string'"
            class="page-ellipsis"
          >…</span>
          <button
            v-else
            type="button"
            class="page-btn"
            :class="{ active: p === current }"
            @click="goTo(p)"
          >
            {{ p }}
          </button>
        </template>

        <button
          type="button"
          class="page-btn"
          :disabled="current >= pageCount"
          aria-label="Next page"
          @click="goTo(current + 1)"
        >
          <i class="bi bi-chevron-right" />
        </button>
      </div>
    </div>
  </div>
</template>
