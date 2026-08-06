<template>
  <div>
    <NuxtLink
      to="/requests"
      class="btn btn-sm btn-primary"
    >
      static class
    </NuxtLink>
    <NuxtLink
      :to="'/requests'"
      class="btn btn-sm btn-primary"
    >
      bound-to static class
    </NuxtLink>
    <NuxtLink
      to="/requests"
      :class="['btn btn-sm btn-primary']"
    >
      bound class
    </NuxtLink>
    <NuxtLink to="/requests">
      no class
    </NuxtLink>
    <NuxtLink
      to="/requests"
      class="btn btn-sm btn-primary"
      custom
    >
      custom slot
    </NuxtLink>
  </div>
</template>
