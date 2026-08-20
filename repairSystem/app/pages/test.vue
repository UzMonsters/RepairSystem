<template>
  <div>
    <NuxtLink
      to="/admin/requests"
      class="btn btn-sm btn-primary"
    >
      static class
    </NuxtLink>
    <NuxtLink
      :to="'/admin/requests'"
      class="btn btn-sm btn-primary"
    >
      bound-to static class
    </NuxtLink>
    <NuxtLink
      to="/admin/requests"
      :class="['btn btn-sm btn-primary']"
    >
      bound class
    </NuxtLink>
    <NuxtLink to="/admin/requests">
      no class
    </NuxtLink>
    <NuxtLink
      to="/admin/requests"
      class="btn btn-sm btn-primary"
      custom
    >
      custom slot
    </NuxtLink>
  </div>
</template>
