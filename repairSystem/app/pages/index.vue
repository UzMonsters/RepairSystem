<script setup lang="ts">
definePageMeta({ layout: 'public' })

const { locale, setLocale, t } = useLocale()
const languages = ['ru', 'uz', 'en'] as const
const services = computed(() => [
  { icon: 'bi-phone', title: t('phone'), text: t('landing.step2Text') },
  { icon: 'bi-laptop', title: t('technicians'), text: t('landing.servicesDescription') },
  { icon: 'bi-snow', title: t('categories'), text: t('landing.heroDescription') }
])
</script>

<template>
  <div class="landing-page">
    <header class="landing-header">
      <NuxtLink
        to="/"
        class="landing-logo"
      ><span class="landing-logo-mark"><i class="bi bi-wrench-adjustable" /></span><span>repair.system</span></NuxtLink>
      <nav class="landing-nav">
        <a href="#process">{{ t('landing.navProcess') }}</a>
        <a href="#services">{{ t('landing.navCatalog') }}</a>
        <a href="#services">{{ t('landing.navBlog') }}</a>
        <a href="#process">{{ t('landing.navFaq') }}</a>
        <NuxtLink to="/contacts">{{ t('landing.navContact') }}</NuxtLink>
        <div class="language-switcher">
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
            class="language-select"
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
        <NuxtLink
          to="/contacts"
          class="landing-cta"
        >{{ t('landing.navCta') }} <i class="bi bi-arrow-up-right" /></NuxtLink>
      </nav>
    </header>

    <main>
      <section class="landing-hero">
        <div class="landing-hero-copy">
          <span class="landing-eyebrow"><i class="bi bi-circle-fill" /> {{ t('landing.eyebrow') }}</span>
          <h1>{{ t('landing.heroTitle') }}</h1>
          <p>{{ t('landing.heroDescription') }}</p>
          <div class="landing-actions">
            <NuxtLink
              to="/contacts"
              class="landing-button"
            >{{ t('landing.consultation') }} <i class="bi bi-arrow-up-right" /></NuxtLink><a
              href="#process"
              class="landing-secondary-button"
            ><i class="bi bi-play-circle" /> {{ t('landing.howWorks') }}</a>
          </div>
          <div class="landing-stats">
            <span><strong>30%</strong>{{ t('landing.statsFast') }}</span><span><strong>1 {{ locale === 'en' ? 'day' : locale === 'uz' ? 'kun' : 'день' }}</strong>{{ t('landing.statsLaunch') }}</span><span><strong>24/7</strong>{{ t('landing.statsAccess') }}</span>
          </div>
        </div>
        <div class="landing-hero-side">
          <div class="landing-photo">
            <img
              src="/images/repair-technician.png"
              :alt="t('landing.workday')"
            ><div class="landing-photo-badge">
              <span>{{ t('landing.workday') }}</span><strong>{{ t('landing.controlled') }}</strong>
            </div>
          </div>
          <div class="landing-dashboard">
            <div class="dashboard-head">
              <span><i class="bi bi-circle-fill" /> {{ t('landing.eyebrow') }}</span><small>{{ t('landing.today') }}, 14:32</small>
            </div><div class="dashboard-body">
              <div><small>{{ t('landing.orders') }}</small><strong>48</strong><em>+12% {{ t('today') }}</em><ul><li>Приёмка <b>12</b></li><li>Диагностика <b>08</b></li><li>Ремонт <b>21</b></li></ul></div><div class="chart">
                <small>{{ t('landing.revenue') }}</small><div class="bars">
                  <i
                    v-for="n in 12"
                    :key="n"
                    :style="{ height: `${35 + (n % 5) * 13}px` }"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="landing-showcase">
        <div class="landing-showcase-image">
          <img
            src="/images/repair-categories.png"
            :alt="t('landing.servicesTitle')"
          >
          <div class="showcase-floating-card">
            <i class="bi bi-check2-circle" />
            <span><strong>98%</strong>{{ t('landing.statsAccess') }}</span>
          </div>
        </div>
        <div class="landing-showcase-copy">
          <span class="landing-eyebrow">{{ t('landing.categoriesEyebrow') }}</span>
          <h2>{{ t('landing.categoriesTitle') }}</h2>
          <p>{{ t('landing.categoriesDescription') }}</p>
          <div class="landing-category-list">
            <div><i class="bi bi-phone" /><span>{{ t('phone') }}<small>{{ t('landing.step2Text') }}</small></span><i class="bi bi-arrow-up-right" /></div>
            <div><i class="bi bi-laptop" /><span>{{ t('landing.deviceCategory') }}<small>{{ t('landing.servicesDescription') }}</small></span><i class="bi bi-arrow-up-right" /></div>
            <div><i class="bi bi-tools" /><span>{{ t('technicians') }}<small>{{ t('landing.processDescription') }}</small></span><i class="bi bi-arrow-up-right" /></div>
          </div>
        </div>
      </section>

      <section
        id="services"
        class="landing-section"
      >
        <div class="landing-section-heading">
          <div><span class="landing-eyebrow">{{ t('landing.servicesEyebrow') }}</span><h2>{{ t('landing.servicesTitle') }}</h2></div><p>{{ t('landing.servicesDescription') }}</p>
        </div><div class="landing-service-grid">
          <article
            v-for="service in services"
            :key="service.title"
            class="landing-service-card"
          >
            <i :class="`bi ${service.icon}`" /><h3>{{ service.title }}</h3><p>{{ service.text }}</p><NuxtLink to="/contacts">{{ t('landing.more') }} <i class="bi bi-arrow-up-right" /></NuxtLink>
          </article>
        </div>
      </section>

      <section
        id="process"
        class="landing-process"
      >
        <div><span class="landing-eyebrow">{{ t('landing.processEyebrow') }}</span><h2>{{ t('landing.processTitle') }}</h2><p>{{ t('landing.processDescription') }}</p></div><div class="landing-steps">
          <div><b>01</b><h3>{{ t('landing.step1Title') }}</h3><p>{{ t('landing.step1Text') }}</p></div><div><b>02</b><h3>{{ t('landing.step2Title') }}</h3><p>{{ t('landing.step2Text') }}</p></div><div><b>03</b><h3>{{ t('landing.step3Title') }}</h3><p>{{ t('landing.step3Text') }}</p></div>
        </div>
      </section>
    </main>

    <footer class="landing-footer">
      <span>© 2026 repair.system</span><NuxtLink to="/contacts">{{ t('landing.navContact') }}</NuxtLink><NuxtLink to="/admin">{{ t('landing.employees') }}</NuxtLink>
    </footer>
  </div>
</template>

<style scoped>
.landing-page { min-height: 100vh; background: #fbf2e7; color: #321b12; font-family: Inter, system-ui, sans-serif; }
.landing-header, .landing-hero, .landing-section, .landing-showcase, .landing-process, .landing-footer { width: min(calc(100% - 48px), 1440px); max-width: none; margin: 0 auto; padding-left: 28px; padding-right: 28px; box-sizing: border-box; }
.landing-header { position: sticky; top: 0; z-index: 10; display: flex; align-items: center; justify-content: space-between; padding-top: 16px; padding-bottom: 16px; border-top: 2px solid #321b12; border-bottom: 1px solid #e5cdb9; background: #fbf2e7ee; backdrop-filter: blur(12px); }
.landing-logo { display: flex; align-items: center; gap: 10px; color: #321b12; font-family: Georgia, serif; font-weight: 700; text-decoration: none; } .landing-logo-mark { display: grid; width: 30px; height: 30px; place-items: center; border-radius: 50%; background: #8e3e13; color: #fff; font-family: system-ui; }
.landing-nav { display: flex; align-items: center; gap: 25px; font-size: 13px; } .landing-nav a { color: #805f4b; text-decoration: none; } .landing-nav a:hover { color: #8e3e13; }
.language-switcher { display: flex; padding: 3px; border: 1px solid #e4c8ae; border-radius: 999px; background: #fffaf4; } .language-switcher button { border: 0; border-radius: 999px; padding: 5px 9px; background: transparent; color: #805f4b; font-size: 10px; cursor: pointer; } .language-switcher button.active { background: #8e3e13; color: #fff; } .language-select { display: none; border: 0; outline: 0; background: transparent; color: #8e3e13; font-size: 11px; }
.landing-cta, .landing-button { display: inline-flex; align-items: center; gap: 8px; border-radius: 999px; background: #8e3e13; color: #fff !important; padding: 12px 18px; font-weight: 700; text-decoration: none; } .landing-cta { padding: 10px 15px; }
.landing-hero { display: grid; grid-template-columns: 1fr 1fr; gap: 70px; align-items: center; padding-top: 86px; padding-bottom: 92px; } .landing-eyebrow { color: #8e3e13; font: 600 10px/1.2 monospace; letter-spacing: .1em; text-transform: uppercase; } .landing-eyebrow i { font-size: 7px; margin-right: 7px; }
.landing-hero h1 { max-width: 570px; margin: 28px 0 22px; font-family: Georgia, serif; font-size: clamp(46px, 6vw, 78px); line-height: .98; letter-spacing: -.06em; } .landing-hero-copy > p { max-width: 545px; color: #805f4b; font-size: 17px; line-height: 1.7; }
.landing-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 18px; margin-top: 30px; } .landing-secondary-button { color: #321b12; text-decoration: none; font-size: 13px; font-weight: 600; }
.landing-stats { display: flex; gap: 35px; margin-top: 38px; padding-top: 22px; border-top: 1px solid #e5cdb9; color: #805f4b; font-size: 11px; } .landing-stats span { display: grid; gap: 5px; } .landing-stats strong { color: #321b12; font: 700 28px Georgia, serif; }
.landing-hero-side { display: grid; gap: 18px; } .landing-photo { position: relative; overflow: hidden; border: 1px solid #d9b99b; border-radius: 22px; background: #e8d3bf; } .landing-photo img { display: block; width: 100%; aspect-ratio: 1.12; object-fit: cover; } .landing-photo-badge { position: absolute; right: 16px; bottom: 16px; left: 16px; display: flex; justify-content: space-between; padding: 14px 16px; border: 1px solid #ead7c4; border-radius: 14px; background: #fffaf4e8; font: 10px monospace; } .landing-photo-badge strong { color: #8e3e13; }
.landing-dashboard { border: 1px solid #d9b99b; border-radius: 20px; background: #fffaf4; } .dashboard-head { display: flex; justify-content: space-between; padding: 16px 18px; border-bottom: 1px solid #ead7c4; color: #8e3e13; font: 10px monospace; text-transform: uppercase; } .dashboard-head i { font-size: 7px; margin-right: 7px; } .dashboard-head small { color: #977561; text-transform: none; } .dashboard-body { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; padding: 18px; } .dashboard-body small { color: #977561; } .dashboard-body strong { display: block; margin: 3px 0; font: 700 38px Georgia, serif; } .dashboard-body em { color: #a55b30; font-size: 11px; font-style: normal; } .dashboard-body ul { display: grid; gap: 8px; padding: 15px 0 0; margin: 0; list-style: none; color: #805f4b; font-size: 12px; } .dashboard-body li::before { content: '•'; color: #8e3e13; margin-right: 8px; } .dashboard-body li b { float: right; font-weight: 400; } .chart { padding: 15px; border-radius: 15px; background: #f7e9d8; } .bars { display: flex; align-items: end; gap: 6px; height: 125px; margin-top: 10px; } .bars i { flex: 1; min-height: 28px; border-radius: 7px 7px 0 0; background: #dfc3aa; } .bars i:nth-child(n+8) { background: #8e3e13; }
.landing-section { padding-top: 80px; padding-bottom: 100px; border-top: 1px solid #e5cdb9; } .landing-section-heading, .landing-process { display: grid; grid-template-columns: 1fr 1fr; gap: 50px; } .landing-section-heading h2, .landing-process h2 { max-width: 600px; margin: 14px 0 0; font: 700 46px/1.02 Georgia, serif; letter-spacing: -.05em; } .landing-section-heading p, .landing-process p { color: #805f4b; line-height: 1.7; }
.landing-service-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 15px; margin-top: 42px; } .landing-service-card { padding: 25px; border: 1px solid #e5cdb9; border-radius: 18px; background: #fffaf4; } .landing-service-card > i { color: #8e3e13; font-size: 28px; } .landing-service-card h3 { margin: 34px 0 10px; font: 700 22px Georgia, serif; } .landing-service-card p { min-height: 55px; color: #805f4b; line-height: 1.6; } .landing-service-card a { color: #8e3e13; font-size: 13px; font-weight: 700; text-decoration: none; }
.landing-showcase { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, .9fr); gap: 60px; align-items: center; padding: 84px 28px; border-top: 1px solid #e5cdb9; }
.landing-showcase-image { position: relative; overflow: hidden; min-height: 500px; border-radius: 24px; background: #e8d3bf; box-shadow: 18px 22px 0 #ead8c5; }
.landing-showcase-image img { display: block; width: 100%; height: 100%; min-height: 500px; object-fit: cover; }
.showcase-floating-card { position: absolute; right: 22px; bottom: 22px; display: flex; align-items: center; gap: 12px; padding: 13px 16px; border: 1px solid #ead7c4; border-radius: 14px; background: #fffaf4ed; color: #805f4b; font: 11px monospace; }
.showcase-floating-card i { color: #4f7d5c; font-size: 24px; } .showcase-floating-card span { display: grid; gap: 4px; } .showcase-floating-card strong { color: #321b12; font: 700 24px Georgia, serif; }
.landing-showcase h2 { max-width: 520px; margin: 14px 0 16px; font: 700 48px/1.02 Georgia, serif; letter-spacing: -.05em; } .landing-showcase-copy > p { max-width: 520px; color: #805f4b; line-height: 1.7; }
.landing-category-list { display: grid; gap: 10px; margin-top: 28px; } .landing-category-list > div { display: grid; grid-template-columns: 30px 1fr 20px; gap: 12px; align-items: center; padding: 15px 0; border-bottom: 1px solid #e5cdb9; color: #8e3e13; } .landing-category-list > div > span { display: grid; gap: 4px; color: #321b12; font-weight: 700; } .landing-category-list small { color: #977561; font-size: 11px; font-weight: 400; } .landing-category-list > div > .bi:last-child { font-size: 13px; }
.landing-process { padding-top: 80px; padding-bottom: 100px; border-top: 1px solid #e5cdb9; } .landing-steps { display: grid; gap: 14px; } .landing-steps > div { padding: 20px; border: 1px solid #e5cdb9; border-radius: 15px; background: #fffaf4; } .landing-steps b { color: #8e3e13; font: 12px monospace; } .landing-steps h3 { margin: 10px 0 4px; font: 700 20px Georgia, serif; } .landing-steps p { margin: 0; font-size: 14px; }
.landing-footer { display: flex; justify-content: space-between; padding-top: 22px; padding-bottom: 22px; border-top: 1px solid #e5cdb9; color: #805f4b; font-size: 13px; } .landing-footer a { color: inherit; text-decoration: none; }
@media (max-width: 900px) { .landing-nav > a:not(.landing-cta) { display: none; } .language-switcher button { display: none; } .language-select { display: block; } .landing-hero, .landing-section-heading, .landing-process, .landing-showcase { grid-template-columns: 1fr; gap: 35px; padding-top: 55px; padding-bottom: 65px; } .landing-service-grid { grid-template-columns: 1fr; } .landing-hero h1 { font-size: 52px; } .landing-showcase-image, .landing-showcase-image img { min-height: 360px; } } @media (max-width: 520px) { .landing-header { align-items: flex-start; gap: 15px; } .landing-nav { gap: 8px; flex-wrap: wrap; justify-content: flex-end; } .landing-cta { display: none; } .landing-stats { gap: 15px; } .landing-stats strong { font-size: 22px; } .landing-showcase { padding-left: 0; padding-right: 0; } }
</style>
