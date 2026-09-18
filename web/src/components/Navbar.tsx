import { useState } from 'react'
import { Menu, X } from 'lucide-react'
import dacareLogo from '@/assets/brand/dacare-logo.svg'
import { useLanguage } from '../context/LanguageContext'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

interface NavbarProps {
  onOpenReservation: () => void
  onOpenChat: () => void
}

export function Navbar({ onOpenReservation, onOpenChat }: NavbarProps) {
  const [isMenuOpen, setIsMenuOpen] = useState(false)
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

  const LanguageSelector = () => (
    <div className="lang-toggle" role="group" aria-label="Language selector">
      <button
        type="button"
        className={`lang-btn ${lang === 'ko' ? 'active' : ''}`}
        onClick={() => setLang('ko')}
        aria-pressed={lang === 'ko'}
      >
        KO
      </button>
      <button
        type="button"
        className={`lang-btn ${lang === 'en' ? 'active' : ''}`}
        onClick={() => setLang('en')}
        aria-pressed={lang === 'en'}
      >
        EN
      </button>
    </div>
  )

  return (
    <header className="site-header">
      <div className="container header-inner">
        <Link
          className="wordmark"
          to={homePath}
          onClick={handleLogoClick}
          aria-label={t.nav.ariaHome}
        >
          <img src={dacareLogo} width="304" height="100" alt="다케어" />
          <span>Device &amp; Appliance Care</span>
        </Link>
        <nav className="desktop-nav" aria-label={t.nav.ariaMenu}>
          <a href={`${homePath}#service`} onClick={(e) => handleNavClick(e, 'service')}>
            {t.nav.service}
          </a>
          <a href={`${homePath}#status`} onClick={(e) => handleNavClick(e, 'status')}>
            {t.nav.status}
          </a>
          <a href={`${homePath}#faq`} onClick={(e) => handleNavClick(e, 'faq')}>
            {t.nav.faq}
          </a>
          <button className="text-button" onClick={onOpenChat}>
            {t.nav.chat}
          </button>
        </nav>
        <div className="header-actions">
          <LanguageSelector />
          <Link
            className="button secondary compact"
            to={token ? (role === 'ADMIN' ? '/admin' : (lang === 'en' ? '/en/reservations' : '/reservations')) : (lang === 'en' ? '/en/login' : '/login')}
          >
            {token ? (role === 'ADMIN' ? (lang === 'en' ? 'Admin' : '관리자') : t.nav.myReservations) : t.nav.login}
          </Link>
          {token && (
            <button
              type="button"
              className="button header-logout-btn compact"
              onClick={handleLogout}
            >
              {t.nav.logout}
            </button>
          )}
          <button className="button primary compact" onClick={onOpenReservation}>
            {t.nav.reservation}
          </button>
          <button
            className="icon-button mobile-menu-toggle"
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
          className="mobile-nav container"
          aria-label={t.nav.ariaMobileMenu}
          onKeyDown={(event) => {
            if (event.key === 'Escape') {
              setIsMenuOpen(false)
              document.querySelector<HTMLButtonElement>('.mobile-menu-toggle')?.focus()
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
            className="text-button"
            onClick={() => {
              setIsMenuOpen(false)
              onOpenChat()
            }}
          >
            {t.nav.chat}
          </button>
          <hr className="mobile-nav-divider" />
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
                className="text-button mobile-logout-btn"
                onClick={handleLogout}
              >
                {t.nav.logout}
              </button>
            </>
          ) : (
            <Link
              to={lang === 'en' ? '/en/login' : '/login'}
              onClick={() => setIsMenuOpen(false)}
            >
              {t.nav.login}
            </Link>
          )}
          <div className="mobile-lang-row">
            <span className="mobile-lang-label">Language / 언어</span>
            <LanguageSelector />
          </div>
        </nav>
      )}
    </header>
  )
}
