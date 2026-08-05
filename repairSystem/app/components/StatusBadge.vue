<script setup lang="ts">
import { statusMeta } from '~/lib/statuses'
import type { RequestStatus } from '~/types'

const { t } = useLocale()
const props = defineProps<{ status: RequestStatus }>()

const meta = computed(() => statusMeta(props.status))
const label = computed(() => t(`status.${props.status}`))
</script>

<template>
  <span
    class="status-chip"
    :class="meta.badge"
  >
    <span class="status-dot" />
    {{ label }}
  </span>
</template>
