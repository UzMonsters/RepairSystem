<template>
  <div class="error-page">
    <div class="error-content">
      <h1 class="error-title">
        {{ error?.statusCode || 404 }}
      </h1>
      <p class="error-message">
        {{ isNotFound ? t('pageNotFound') : error?.message || t('somethingWentWrong') }}
      </p>
      <NuxtLink
        to="/admin"
        class="back-link"
      >
        {{ t('backToHome') }}
      </NuxtLink>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps({
  error: Object
})

const { t } = useLocale()

const isNotFound = computed(() => {
  return props.error?.statusCode === 404
})
</script>

<style scoped>
.error-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--rs-bg-main, #f0f4f8);
  color: var(--rs-text-primary, #1a202c);
  font-family: system-ui, -apple-system, sans-serif;
}

.error-content {
  text-align: center;
  padding: 40px;
  background: var(--rs-bg-card, #ffffff);
  border-radius: 12px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
}

.error-title {
  font-size: 72px;
  font-weight: 700;
  margin: 0 0 16px 0;
  color: var(--rs-primary-color, #3b82f6);
}

.error-message {
  font-size: 18px;
  margin: 0 0 32px 0;
  color: var(--rs-text-secondary, #4a5568);
}

.back-link {
  display: inline-block;
  padding: 12px 24px;
  background: var(--rs-primary-color, #3b82f6);
  color: #fff;
  text-decoration: none;
  border-radius: 8px;
  font-weight: 500;
  transition: opacity 0.2s;
}

.back-link:hover {
  opacity: 0.9;
}
</style>
