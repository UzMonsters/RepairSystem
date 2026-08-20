<script setup lang="ts">
const config = useRuntimeConfig()
const telegramUrl = computed(() => `https://t.me/${config.public.telegramBotUsername}`)
</script>

<template>
  <div class="public-layout">
    <slot />
    <a
      class="public-telegram-button"
      :href="telegramUrl"
      target="_blank"
      rel="noopener noreferrer"
      aria-label="Telegram"
      title="Telegram"
    >
      <i class="bi bi-telegram" />
    </a>
  </div>
</template>

<style>
html,
body,
#__nuxt {
  min-height: 100%;
}

body:has(.public-layout) {
  margin: 0;
  background: #f7f8f2;
}

.public-telegram-button {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 20;
  display: grid;
  width: 54px;
  height: 54px;
  place-items: center;
  border-radius: 50%;
  background: #8e3e13;
  color: #fff;
  box-shadow: 0 12px 28px #5a260044;
  font-size: 24px;
  text-decoration: none;
  transition: transform .2s ease, background .2s ease;
}

.public-telegram-button:hover {
  background: #a9501d;
  color: #fff;
  transform: translateY(-3px);
}

@media (max-width: 520px) {
  .public-telegram-button {
    right: 16px;
    bottom: 16px;
  }
}
</style>
