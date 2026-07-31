export default defineNuxtConfig({
  modules: ['@nuxt/eslint'],

  plugins: [
    { src: '~/plugins/adminlte.client', mode: 'client' }
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
    'admin-lte/dist/css/adminlte.css'
  ],

  runtimeConfig: {
    backendUrl: 'http://localhost:8080'
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
