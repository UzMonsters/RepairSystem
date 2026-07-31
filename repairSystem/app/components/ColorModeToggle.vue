<script setup>
const modes = [
  { value: 'light', icon: 'bi-sun-fill', label: 'Light' },
  { value: 'dark', icon: 'bi-moon-fill', label: 'Dark' },
  { value: 'auto', icon: 'bi-circle-half', label: 'Auto' }
]

const colorMode = ref('light')

const triggerIcon = computed(() => {
  if (colorMode.value === 'light') return 'bi-sun-fill'
  if (colorMode.value === 'dark') return 'bi-moon-fill'
  return 'bi-circle-half'
})

function resolve(mode) {
  if (mode === 'auto') return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  return mode
}

function setColorMode(mode) {
  colorMode.value = mode
  document.documentElement.setAttribute('data-bs-theme', resolve(mode))
  try {
    localStorage.setItem('lte-theme', mode)
  } catch (e) {
    void e
  }
}

onMounted(() => {
  try {
    colorMode.value = localStorage.getItem('lte-theme') || 'light'
  } catch (e) {
    void e
  }
  document.documentElement.setAttribute('data-bs-theme', resolve(colorMode.value))
})
</script>

<template>
  <li class="nav-item dropdown">
    <button
      id="bd-theme"
      class="nav-link"
      type="button"
      aria-label="Toggle color scheme"
      data-bs-toggle="dropdown"
      aria-expanded="false"
    >
      <i :class="['bi', triggerIcon]" />
    </button>
    <ul
      class="dropdown-menu dropdown-menu-end"
      aria-labelledby="bd-theme"
    >
      <li
        v-for="mode in modes"
        :key="mode.value"
      >
        <button
          type="button"
          :class="['dropdown-item d-flex align-items-center', colorMode === mode.value && 'active']"
          @click="setColorMode(mode.value)"
        >
          <i :class="['bi', mode.icon, 'me-2']" />
          {{ mode.label }}
          <i
            v-if="colorMode === mode.value"
            class="bi bi-check-lg ms-auto"
          />
        </button>
      </li>
    </ul>
  </li>
</template>
