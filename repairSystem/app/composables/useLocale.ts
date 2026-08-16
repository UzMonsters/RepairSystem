import en from '~/locales/en'
import ru from '~/locales/ru'
import uz from '~/locales/uz'

type Locale = 'uz' | 'ru' | 'en'
type TranslationMap = Record<string, string>

function getStoredLocale(): Locale | null {
  if (!import.meta.client) return null

  try {
    const stored = localStorage.getItem('repair_lang')
    return stored === 'ru' || stored === 'en' || stored === 'uz' ? stored : null
  } catch {
    return null
  }
}

const translations: Record<Locale, TranslationMap> = {
  uz: uz as TranslationMap,
  ru: ru as TranslationMap,
  en: en as TranslationMap
}

export function useLocale() {
  const locale = useState<Locale>('app:locale', () => getStoredLocale() || 'uz')

  function setLocale(lang: string) {
    locale.value = lang === 'ru' || lang === 'en' ? lang : 'uz'
    if (!import.meta.client) return
    try {
      localStorage.setItem('repair_lang', locale.value)
    } catch {
      // Local storage can be unavailable in private browsing.
    }
    document.documentElement.setAttribute('lang', locale.value)
  }

  const t = (key: string) => {
    const dictionary = translations[locale.value]
    return dictionary[key] || (en as TranslationMap)[key] || (uz as TranslationMap)[key] || key
  }

  onMounted(() => {
    if (!import.meta.client) return
    try {
      setLocale(getStoredLocale() || 'uz')
    } catch {
      setLocale('uz')
    }
  })

  return { locale, setLocale, t }
}
