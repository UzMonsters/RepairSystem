import en from '~/locales/en'
import ru from '~/locales/ru'
import uz from '~/locales/uz'

type Locale = 'uz' | 'ru' | 'en'
type TranslationMap = Record<string, string>

const translations: Record<Locale, TranslationMap> = {
  uz: uz as TranslationMap,
  ru: ru as TranslationMap,
  en: en as TranslationMap
}

export function useLocale() {
  const locale = useState<Locale>('app:locale', () => 'uz')

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
      const stored = localStorage.getItem('repair_lang')
      setLocale(stored || 'uz')
    } catch {
      setLocale('uz')
    }
  })

  return { locale, setLocale, t }
}
