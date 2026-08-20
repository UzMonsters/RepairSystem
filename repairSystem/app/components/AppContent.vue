<script setup>
defineProps({
  title: { type: String, default: '' },
  breadcrumbs: { type: Array, default: () => [] }
})
</script>

<template>
  <div
    v-if="title || breadcrumbs.length || $slots.header"
    class="app-content-header"
  >
    <div class="container-fluid">
      <div class="row">
        <div class="col-sm-6">
          <slot name="header">
            <h3
              v-if="title"
              class="mb-0"
            >
              {{ title }}
            </h3>
          </slot>
        </div>
        <div
          v-if="breadcrumbs.length"
          class="col-sm-6"
        >
          <ol class="breadcrumb float-sm-end">
            <li
              v-for="(crumb, idx) in breadcrumbs"
              :key="idx"
              :class="['breadcrumb-item', idx === breadcrumbs.length - 1 && 'active']"
            >
              <NuxtLink
                v-if="crumb.to"
                :to="crumb.to"
              >{{ crumb.label }}</NuxtLink>
              <a
                v-else-if="crumb.href"
                :href="crumb.href"
              >{{ crumb.label }}</a>
              <template v-else>
                {{ crumb.label }}
              </template>
            </li>
          </ol>
        </div>
      </div>
    </div>
  </div>

  <div class="app-content">
    <div class="container-fluid">
      <slot />
    </div>
  </div>
</template>
