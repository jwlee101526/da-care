import { useState } from 'react'
import { ArrowUpRight, Menu, X } from 'lucide-react'
import dacareLogo from '@/assets/brand/dacare-logo.svg'
import { useLanguage } from '../context/LanguageContext'

interface NavbarProps {
  onOpenReservation: () => void
  onOpenChat: () => void
}

export function Navbar({ onOpenReservation, onOpenChat }: NavbarProps) {
  const [isMenuOpen, setIsMenuOpen] = useState(false)
  const { lang, setLang, t } = useLanguage()

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
        <a className="wordmark" href="#top" aria-label={t.nav.ariaHome}>
          <img src={dacareLogo} width="304" height="100" alt="다케어" />
          <span>Device &amp; Appliance Care</span>
        </a>
        <nav className="desktop-nav" aria-label={t.nav.ariaMenu}>
          <a href="#service">{t.nav.service}</a>
          <a href="#status">{t.nav.status}</a>
          <a href="#faq">{t.nav.faq}</a>
          <button className="text-button" onClick={onOpenChat}>{t.nav.chat}</button>
        </nav>
        <div className="header-actions">
          <LanguageSelector />
          <button className="button primary compact" onClick={onOpenReservation}>
            {t.nav.reservation} <ArrowUpRight size={16} />
          </button>
          <button className="icon-button mobile-menu-toggle" aria-label={isMenuOpen ? t.nav.ariaCloseMenu : t.nav.ariaOpenMenu} aria-expanded={isMenuOpen} aria-controls="mobile-nav" onClick={() => setIsMenuOpen(!isMenuOpen)}>
            {isMenuOpen ? <X size={22} /> : <Menu size={22} />}
          </button>
        </div>
      </div>
      {isMenuOpen && <nav id="mobile-nav" className="mobile-nav container" aria-label={t.nav.ariaMobileMenu} onKeyDown={event => { if (event.key === 'Escape') { setIsMenuOpen(false); document.querySelector<HTMLButtonElement>('.mobile-menu-toggle')?.focus() } }}>
        <a href="#service" onClick={() => setIsMenuOpen(false)}>{t.nav.service}</a>
        <a href="#status" onClick={() => setIsMenuOpen(false)}>{t.nav.status}</a>
        <a href="#faq" onClick={() => setIsMenuOpen(false)}>{t.nav.faq}</a>
        <button className="text-button" onClick={() => { setIsMenuOpen(false); onOpenChat() }}>{t.nav.chat}</button>
        <div className="mobile-lang-row">
          <span className="mobile-lang-label">Language / 언어</span>
          <LanguageSelector />
        </div>
      </nav>}
    </header>
  )
}
