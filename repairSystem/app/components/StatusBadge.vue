<script setup lang="ts">
import { statusMeta } from '~/lib/statuses'
import type { RequestStatus } from '~/types'

const props = defineProps<{ status: RequestStatus }>()

const meta = computed(() => statusMeta(props.status))
</script>

<template>
  <span
    class="badge"
    :class="meta.badge"
  >
    {{ meta.label }}
  </span>
</template>
