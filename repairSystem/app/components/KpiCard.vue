<script setup lang="ts">
defineProps<{
  icon: string
  title: string
  value: string | number
  rate?: string
  isUp?: boolean
  sub?: string
  to?: string
}>()
</script>

<template>
  <NuxtLink
    v-if="to"
    :to="to"
    class="kpi-card kpi-card-link"
  >
    <div class="kpi-top">
      <div class="kpi-head">
        <span class="kpi-icon">
          <i :class="`bi ${icon}`" />
        </span>
        <span class="kpi-title">{{ title }}</span>
      </div>
    </div>

    <div class="kpi-bottom">
      <span class="kpi-value">{{ value }}</span>
      <span
        v-if="rate"
        class="rate-chip"
        :class="isUp ? 'rate-up' : 'rate-down'"
      >
        <i :class="`bi ${isUp ? 'bi-arrow-up-short' : 'bi-arrow-down-short'}`" />
        {{ rate }}
      </span>
      <span
        v-else-if="sub"
        class="kpi-sub"
      >{{ sub }}</span>
    </div>
  </NuxtLink>
  <div
    v-else
    class="kpi-card"
  >
    <div class="kpi-top">
      <div class="kpi-head">
        <span class="kpi-icon">
          <i :class="`bi ${icon}`" />
        </span>
        <span class="kpi-title">{{ title }}</span>
      </div>
    </div>

    <div class="kpi-bottom">
      <span class="kpi-value">{{ value }}</span>
      <span
        v-if="rate"
        class="rate-chip"
        :class="isUp ? 'rate-up' : 'rate-down'"
      >
        <i :class="`bi ${isUp ? 'bi-arrow-up-short' : 'bi-arrow-down-short'}`" />
        {{ rate }}
      </span>
      <span
        v-else-if="sub"
        class="kpi-sub"
      >{{ sub }}</span>
    </div>
  </div>
</template>
