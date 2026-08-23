export default defineNuxtConfig({
  modules: ['@nuxt/eslint'],

  plugins: [
    { src: '~/plugins/adminlte.client', mode: 'client' },
    { src: '~/plugins/web-push.client', mode: 'client' },
    { src: '~/plugins/realtime.client', mode: 'client' }
  ],

  devtools: {
    enabled: true
  },

  app: {
    head: {
      htmlAttrs: { lang: 'en' },
      title: 'Repair System',
      meta: [
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'description', content: 'Repair Service CRM' }
      ],
      link: [
        { rel: 'icon', type: 'image/png', href: '/assets/img/AdminLTELogo.png' },
        { rel: 'preconnect', href: 'https://fonts.googleapis.com' },
        { rel: 'preconnect', href: 'https://fonts.gstatic.com', crossorigin: '' },
        {
          rel: 'stylesheet',
          href: 'https://fonts.googleapis.com/css2?family=Source+Sans+3:ital,wght@0,300;0,400;0,500;0,600;0,700;1,400&display=swap'
        }
      ],
      script: [
        {
          key: 'lte-theme-init',
          tagPosition: 'head',
          innerHTML: `(function(){try{var k=localStorage.getItem('lte-theme')||'light';var d=k==='auto'?(window.matchMedia('(prefers-color-scheme: dark)').matches?'dark':'light'):k;document.documentElement.setAttribute('data-bs-theme',d);}catch(e){}})();`
        }
      ]
    }
  },

  css: [
    'bootstrap-icons/font/bootstrap-icons.css',
    'overlayscrollbars/overlayscrollbars.css',
    'jsvectormap/dist/jsvectormap.css',
    'admin-lte/dist/css/adminlte.css',
    '~/assets/css/admin-theme.css',
    '~/assets/css/light-theme.css'
  ],

  runtimeConfig: {
    public: {
      realtimeUrl: process.env.NUXT_PUBLIC_REALTIME_URL || '',
      telegramBotUsername: process.env.NUXT_PUBLIC_TELEGRAM_BOT_USERNAME || 'repairauto_bot',
      firebaseApiKey: process.env.NUXT_PUBLIC_FIREBASE_API_KEY,
      firebaseAuthDomain: process.env.NUXT_PUBLIC_FIREBASE_AUTH_DOMAIN,
      firebaseProjectId: process.env.NUXT_PUBLIC_FIREBASE_PROJECT_ID,
      firebaseStorageBucket: process.env.NUXT_PUBLIC_FIREBASE_STORAGE_BUCKET,
      firebaseMessagingSenderId: process.env.NUXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID,
      firebaseAppId: process.env.NUXT_PUBLIC_FIREBASE_APP_ID,
      firebaseVapidKey: process.env.NUXT_PUBLIC_FIREBASE_VAPID_KEY
    },
    backendUrl: process.env.NUXT_BACKEND_URL
      || (process.env.NUXT_BACKEND_HOSTPORT ? `http://${process.env.NUXT_BACKEND_HOSTPORT}` : undefined)
      || 'http://localhost:8080'
  },

  compatibilityDate: '2026-06-30',

  eslint: {
    config: {
      stylistic: {
        commaDangle: 'never',
        braceStyle: '1tbs'
      }
    }
  }
})
