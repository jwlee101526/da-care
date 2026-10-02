import { useRef, useState } from 'react'
import { Menu, X } from 'lucide-react'
import dacareLogo from '@/assets/brand/dacare-logo.svg'
import { useLanguage } from '../context/LanguageContext'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { buttonVariants } from './ui/button'
import { cn } from '@/lib/utils'

// 헤더 오른쪽 버튼들은 기본 버튼보다 낮고 촘촘하다.
const headerButton = 'h-9 min-h-9 px-4 py-0 text-[13px] leading-[1.4] font-semibold whitespace-nowrap rounded-lg max-[600px]:h-8 max-[600px]:min-h-8 max-[600px]:px-2.5 max-[600px]:text-[12px]'
const outlineButton = 'bg-transparent border-[#d9dfe8] text-[#5f7192] transition-all hover:bg-[#f4f6f9] hover:text-ink hover:border-line-strong'
const navLink = 'hover:text-brand'
const textButton = 'min-h-11 py-2 text-left hover:text-brand'

interface NavbarProps {
  onOpenReservation: () => void
  onOpenChat: () => void
}

interface LanguageSelectorProps {
  lang: 'ko' | 'en'
  setLang: (lang: 'ko' | 'en') => void
}

function langButton(active: boolean) {
  return cn(
    'inline-flex h-7 cursor-pointer items-center justify-center rounded-md border-0 px-2.5 text-[12px] font-semibold transition-all duration-150 ease-[ease] max-[600px]:h-[26px] max-[600px]:rounded-[4px] max-[600px]:px-[7px] max-[600px]:text-[11px]',
    active ? 'bg-white text-brand shadow-[0_1px_3px_rgba(0,0,0,0.08)]' : 'bg-transparent text-ink-subtle hover:text-navy',
  )
}

function LanguageSelector({ lang, setLang }: LanguageSelectorProps) {
  return (
    <div className="box-border inline-flex h-9 items-center gap-0.5 rounded-lg border border-[#dbe2ed] bg-[#f0f3f7] p-[3px] max-[600px]:h-8 max-[600px]:rounded-md max-[600px]:p-0.5" role="group" aria-label="Language selector">
      <button
        type="button"
        className={langButton(lang === 'ko')}
        onClick={() => setLang('ko')}
        aria-pressed={lang === 'ko'}
      >
        KO
      </button>
      <button
        type="button"
        className={langButton(lang === 'en')}
        onClick={() => setLang('en')}
        aria-pressed={lang === 'en'}
      >
        EN
      </button>
    </div>
  )
}

export function Navbar({ onOpenReservation, onOpenChat }: NavbarProps) {
  const [isMenuOpen, setIsMenuOpen] = useState(false)
  const menuToggleRef = useRef<HTMLButtonElement>(null)
  const { lang, setLang, t } = useLanguage()
  const { token, role, logout } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()

  const homePath = lang === 'en' ? '/en' : '/'

  const handleLogoClick = (e: React.MouseEvent) => {
    const isLanding = location.pathname === '/' || location.pathname === '/en'
    if (isLanding) {
      e.preventDefault()
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
  }

  const handleNavClick = (e: React.MouseEvent, sectionId: string) => {
    setIsMenuOpen(false)
    const isLanding = location.pathname === '/' || location.pathname === '/en'
    if (isLanding) {
      e.preventDefault()
      const element = document.getElementById(sectionId)
      if (element) {
        element.scrollIntoView({ behavior: 'smooth' })
      }
    } else {
      e.preventDefault()
      navigate(`${homePath}#${sectionId}`)
    }
  }

  const handleLogout = () => {
    logout()
    setIsMenuOpen(false)
    navigate(homePath)
  }

  return (
    <header className="sticky top-0 z-30 border-b border-line bg-white">
      <div className="container mx-auto flex h-20 w-[min(1360px,calc(100%-48px))] items-center justify-between gap-7 max-[600px]:h-[68px] max-[600px]:gap-3">
        <Link
          className="inline-flex shrink-0 flex-row items-center gap-3.5"
          to={homePath}
          onClick={handleLogoClick}
          aria-label={t.nav.ariaHome}
        >
          <img className="block h-[27px] w-auto max-[600px]:h-[25px]" src={dacareLogo} width="304" height="100" alt="다케어" />
          <span className="whitespace-nowrap border-l border-[#d9e0ea] pl-3.5 text-[10px] font-semibold leading-none tracking-[.055em] text-[#5f7192] max-[600px]:hidden">Device &amp; Appliance Care</span>
        </Link>
        <nav className="ml-9 flex items-center gap-7 text-[14px] max-[1050px]:gap-5 max-[850px]:hidden" aria-label={t.nav.ariaMenu}>
          <a className={navLink} href={`${homePath}#service`} onClick={(e) => handleNavClick(e, 'service')}>
            {t.nav.service}
          </a>
          <a className={navLink} href={`${homePath}#status`} onClick={(e) => handleNavClick(e, 'status')}>
            {t.nav.status}
          </a>
          <a className={navLink} href={`${homePath}#faq`} onClick={(e) => handleNavClick(e, 'faq')}>
            {t.nav.faq}
          </a>
          <button className={textButton} onClick={onOpenChat}>
            {t.nav.chat}
          </button>
        </nav>
        <div className="ml-auto flex items-center gap-2 max-[600px]:gap-1.5">
          <LanguageSelector lang={lang} setLang={setLang} />
          <Link
            className={buttonVariants({ variant: 'secondary', size: 'compact', className: headerButton })}
            to={token ? (role === 'ADMIN' ? '/admin' : (lang === 'en' ? '/en/reservations' : '/reservations')) : (lang === 'en' ? '/en/login' : '/login')}
          >
            {token ? (role === 'ADMIN' ? (lang === 'en' ? 'Admin' : '관리자') : t.nav.myReservations) : t.nav.login}
          </Link>
          {!token && (
            <Link
              className={buttonVariants({ size: 'compact', className: cn(headerButton, outlineButton, 'max-[850px]:hidden') })}
              to={lang === 'en' ? '/en/reservations/lookup' : '/reservations/lookup'}
              aria-label={t.nav.guestLookup}
            >
              {t.nav.lookup}
            </Link>
          )}
          {token && (
            <button
              type="button"
              className={buttonVariants({ size: 'compact', className: cn(headerButton, outlineButton) })}
              onClick={handleLogout}
            >
              {t.nav.logout}
            </button>
          )}
          <button className={buttonVariants({ variant: 'primary', size: 'compact', className: headerButton })} onClick={onOpenReservation}>
            {t.nav.reservation}
          </button>
          <button
            ref={menuToggleRef}
            className="hidden min-h-11 min-w-11 shrink-0 items-center justify-center rounded-lg hover:bg-[#eef1f5] max-[850px]:inline-flex"
            aria-label={isMenuOpen ? t.nav.ariaCloseMenu : t.nav.ariaOpenMenu}
            aria-expanded={isMenuOpen}
            aria-controls="mobile-nav"
            onClick={() => setIsMenuOpen(!isMenuOpen)}
          >
            {isMenuOpen ? <X size={22} /> : <Menu size={22} />}
          </button>
        </div>
      </div>
      {isMenuOpen && (
        <nav
          id="mobile-nav"
          className="container mx-auto hidden w-[min(1120px,calc(100%-64px))] max-[850px]:flex max-[850px]:flex-col max-[850px]:pt-2 max-[850px]:pb-5 max-[600px]:w-[calc(100%-40px)] [&_a]:flex [&_a]:min-h-11 [&_a]:items-center [&_a]:text-[15px] [&_a:hover]:text-brand"
          aria-label={t.nav.ariaMobileMenu}
          onKeyDown={(event) => {
            if (event.key === 'Escape') {
              setIsMenuOpen(false)
              menuToggleRef.current?.focus()
            }
          }}
        >
          <a href={`${homePath}#service`} onClick={(e) => handleNavClick(e, 'service')}>
            {t.nav.service}
          </a>
          <a href={`${homePath}#status`} onClick={(e) => handleNavClick(e, 'status')}>
            {t.nav.status}
          </a>
          <a href={`${homePath}#faq`} onClick={(e) => handleNavClick(e, 'faq')}>
            {t.nav.faq}
          </a>
          <button
            className={cn(textButton, 'text-[15px]')}
            onClick={() => {
              setIsMenuOpen(false)
              onOpenChat()
            }}
          >
            {t.nav.chat}
          </button>
          <hr className="mt-3 mb-1.5 border-0 border-t border-line" />
          {token ? (
            <>
              <Link
                to={role === 'ADMIN' ? '/admin' : (lang === 'en' ? '/en/reservations' : '/reservations')}
                onClick={() => setIsMenuOpen(false)}
              >
                {role === 'ADMIN' ? (lang === 'en' ? 'Admin' : '관리자 페이지') : t.nav.myReservations}
              </Link>
              <button
                type="button"
                className="min-h-11 cursor-pointer py-2 text-left text-[15px] text-danger hover:text-danger-strong"
                onClick={handleLogout}
              >
                {t.nav.logout}
              </button>
            </>
          ) : (
            <>
              <Link
                to={lang === 'en' ? '/en/login' : '/login'}
                onClick={() => setIsMenuOpen(false)}
              >
                {t.nav.login}
              </Link>
              <Link
                to={lang === 'en' ? '/en/reservations/lookup' : '/reservations/lookup'}
                onClick={() => setIsMenuOpen(false)}
              >
                {t.nav.guestLookup}
              </Link>
            </>
          )}
          <div className="mt-3 flex items-center justify-between border-t border-line pt-4">
            <span className="text-[13px] font-medium text-muted">Language / 언어</span>
            <LanguageSelector lang={lang} setLang={setLang} />
          </div>
        </nav>
      )}
    </header>
  )
}
