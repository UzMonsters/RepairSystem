<script setup lang="ts">
definePageMeta({ layout: 'public' })

const { locale, setLocale, t } = useLocale()
const languages = ['ru', 'uz', 'en'] as const
const sent = ref(false)
const form = reactive({ name: '', phone: '', message: '' })

function submit() {
  sent.value = true
}
</script>

<template>
  <div class="contact-page">
    <header class="contact-header">
      <NuxtLink
        to="/"
        class="contact-logo"
      ><span class="contact-logo-mark"><i class="bi bi-wrench-adjustable" /></span><span>repair.system</span></NuxtLink>
      <NuxtLink
        to="/"
        class="contact-back"
      ><i class="bi bi-arrow-left" /> {{ t('home') }}</NuxtLink>
      <div class="contact-language-switcher">
        <button
          v-for="lang in languages"
          :key="lang"
          type="button"
          :class="{ active: locale === lang }"
          @click="setLocale(lang)"
        >
          {{ lang.toUpperCase() }}
        </button>
        <select
          class="contact-language-select"
          :value="locale"
          aria-label="Language"
          @change="setLocale(($event.target as HTMLSelectElement).value)"
        >
          <option
            v-for="lang in languages"
            :key="lang"
            :value="lang"
          >
            {{ lang.toUpperCase() }}
          </option>
        </select>
      </div>
    </header>

    <main class="contact-content">
      <section class="contact-intro">
        <h1>{{ t('landing.navContact') }}</h1><p>{{ t('landing.heroDescription') }}</p>
      </section>

      <section class="contact-grid">
        <div class="contact-card">
          <h2>{{ t('landing.consultation') }}</h2>
          <p>{{ t('landing.processDescription') }}</p>
          <div
            v-if="sent"
            class="contact-success"
          >
            <i class="bi bi-check-circle" /> {{ t('savedSuccessfully') }}
          </div>
          <form
            v-else
            @submit.prevent="submit"
          >
            <label>{{ t('name') }}<input
              v-model="form.name"
              required
              :placeholder="t('name')"
            ></label>
            <label>{{ t('phone') }}<input
              v-model="form.phone"
              required
              type="tel"
              :placeholder="t('phone')"
            ></label>
            <label>{{ t('description') }}<textarea
              v-model="form.message"
              required
              rows="5"
              :placeholder="t('description')"
            /></label>
            <button
              type="submit"
              class="contact-submit"
            >
              {{ t('landing.consultation') }} <i class="bi bi-arrow-up-right" />
            </button>
          </form>
        </div>

        <div class="contact-details">
          <div class="map-card">
            <iframe
              title="Repair System location"
              src="https://www.openstreetmap.org/export/embed.html?bbox=69.210%2C41.275%2C69.300%2C41.335&amp;layer=mapnik&amp;marker=41.311%2C69.279"
              loading="lazy"
            />
          </div>
          <div class="contact-info">
            <div><i class="bi bi-geo-alt" /><span>Asia/Tashkent<br><small>Service center</small></span></div><div><i class="bi bi-clock" /><span>24/7<br><small>{{ t('landing.statsAccess') }}</small></span></div><div><i class="bi bi-telegram" /><span>Telegram<br><small>{{ t('landing.navContact') }}</small></span></div>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.contact-page { min-height: 100vh; background: #fbf2e7; color: #321b12; } .contact-header, .contact-content { width: min(calc(100% - 48px), 1440px); max-width: none; margin: 0 auto; box-sizing: border-box; } .contact-header { display: flex; align-items: center; justify-content: space-between; padding: 16px 28px; border-top: 2px solid #321b12; border-bottom: 1px solid #e5cdb9; }
.contact-logo { display: flex; order: 1; align-items: center; gap: 10px; color: #321b12; font-family: Georgia, serif; font-weight: 700; text-decoration: none; } .contact-logo-mark { display: grid; width: 30px; height: 30px; place-items: center; border-radius: 50%; background: #8e3e13; color: #fff; font-family: system-ui; } .contact-back { order: 3; color: #805f4b; font-size: 13px; text-decoration: none; } .contact-language-switcher { display: flex; order: 2; padding: 3px; border: 1px solid #e4c8ae; border-radius: 999px; background: #fffaf4; } .contact-language-switcher button { border: 0; border-radius: 999px; padding: 5px 9px; background: transparent; color: #805f4b; font-size: 10px; cursor: pointer; } .contact-language-switcher button.active { background: #8e3e13; color: #fff; } .contact-language-select { display: none; border: 0; background: transparent; color: #8e3e13; }
.contact-content { padding: 78px 28px 100px; } .contact-intro { max-width: 760px; } .contact-eyebrow { color: #8e3e13; font: 600 10px monospace; letter-spacing: .1em; text-transform: uppercase; } .contact-eyebrow i { font-size: 7px; margin-right: 7px; } .contact-intro h1 { margin: 20px 0 15px; font: 700 clamp(48px, 7vw, 88px)/.95 Georgia, serif; letter-spacing: -.06em; } .contact-intro p, .contact-card > p { color: #805f4b; font-size: 17px; line-height: 1.7; }
.contact-grid { display: grid; grid-template-columns: minmax(300px, .85fr) minmax(420px, 1.15fr); gap: 24px; margin-top: 60px; } .contact-card, .map-card, .contact-info { border: 1px solid #d9b99b; border-radius: 20px; background: #fffaf4; } .contact-card { padding: 30px; } .contact-card h2 { margin: 0; font: 700 32px Georgia, serif; } form { display: grid; gap: 16px; margin-top: 28px; } label { display: grid; gap: 7px; color: #805f4b; font-size: 12px; } input, textarea { width: 100%; box-sizing: border-box; padding: 13px 14px; border: 1px solid #e5cdb9; border-radius: 10px; background: #fbf2e7; color: #321b12; font: inherit; } textarea { resize: vertical; } .contact-submit { width: fit-content; border: 0; border-radius: 999px; padding: 13px 19px; background: #8e3e13; color: #fff; cursor: pointer; font-weight: 700; } .contact-success { display: flex; gap: 9px; margin-top: 28px; padding: 14px; border-radius: 10px; background: #f4e3d1; color: #8e3e13; }
.contact-details { display: grid; gap: 18px; } .map-card { min-height: 380px; overflow: hidden; } iframe { width: 100%; height: 100%; min-height: 380px; border: 0; filter: sepia(.25) saturate(.8); } .contact-info { display: grid; grid-template-columns: repeat(3, 1fr); gap: 15px; padding: 20px; } .contact-info div { display: flex; gap: 10px; color: #8e3e13; font-size: 13px; } .contact-info i { font-size: 19px; } .contact-info small { color: #977561; }
@media (max-width: 800px) { .contact-grid { grid-template-columns: 1fr; } .contact-content { padding-top: 55px; } .contact-header { gap: 12px; } .contact-language-switcher button { display: none; } .contact-language-select { display: block; } } @media (max-width: 520px) { .contact-header, .contact-content { width: calc(100% - 28px); padding-left: 14px; padding-right: 14px; } .contact-header { flex-wrap: wrap; } .contact-info { grid-template-columns: 1fr; } }
</style>
