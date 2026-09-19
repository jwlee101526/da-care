import { createContext, useContext, useEffect, type ReactNode } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { ko, type LocaleDict } from '../locales/ko'
import { en } from '../locales/en'

export type Language = 'ko' | 'en'

interface LanguageContextValue {
  lang: Language
  setLang: (lang: Language) => void
  toggleLang: () => void
  t: LocaleDict
}

const LanguageContext = createContext<LanguageContextValue | null>(null)

export function LanguageProvider({ children }: { children: ReactNode }) {
  const location = useLocation()
  const navigate = useNavigate()

  // URL 경로(/en)를 단일 진실의 원천(Single Source of Truth)으로 사용
  const isEnglish = location.pathname === '/en' || location.pathname.startsWith('/en/')
  const lang: Language = isEnglish ? 'en' : 'ko'

  const setLang = (nextLang: Language) => {
    if (nextLang === lang) return

    let targetPath = location.pathname
    if (nextLang === 'en') {
      if (!targetPath.startsWith('/en')) {
        targetPath = '/en' + (targetPath === '/' ? '' : targetPath)
      }
    } else {
      if (targetPath.startsWith('/en')) {
        targetPath = targetPath.replace(/^\/en(\/|$)/, '/') || '/'
      }
    }

    navigate(
      { pathname: targetPath, search: location.search, hash: location.hash },
      { replace: true }
    )
  }

  const toggleLang = () => {
    setLang(lang === 'ko' ? 'en' : 'ko')
  }

  useEffect(() => {
    document.documentElement.lang = lang
  }, [lang])

  const t = lang === 'ko' ? ko : en

  return (
    <LanguageContext.Provider value={{ lang, setLang, toggleLang, t }}>
      {children}
    </LanguageContext.Provider>
  )
}

// 컨텍스트 hook은 Provider와 동일 모듈에서 공개한다.
// eslint-disable-next-line react-refresh/only-export-components
export function useLanguage(): LanguageContextValue {
  const context = useContext(LanguageContext)
  if (!context) {
    throw new Error('useLanguage must be used within a LanguageProvider')
  }
  return context
}
