import dacareLogo from '@/assets/brand/dacare-logo.svg'
import { useLanguage } from '../context/LanguageContext'

export function Footer() {
  const { t } = useLanguage()

  return (
    <footer className="site-footer">
      <div className="container">
        <div className="footer-top">
          <div className="footer-brand">
            <a className="wordmark" href="#top" aria-label={t.nav.ariaHome}><img src={dacareLogo} width="304" height="100" alt="다케어" /></a>
            <p className="footer-tagline">{t.footer.tagline}</p>
            <p className="footer-description" style={{ whiteSpace: 'pre-line' }}>{t.footer.description}</p>
          </div>
          <nav className="footer-links" aria-label="하단 메뉴">
            <h3>{t.footer.menuTitle}</h3>
            <a href="#service">{t.nav.service}</a>
            <a href="#status">{t.nav.status}</a>
            <a href="#contact">{t.contact.title}</a>
            <a href="#faq">{t.nav.faq}</a>
          </nav>
          <div className="footer-guide">
            <h3>{t.footer.guideTitle}</h3>
            <p style={{ whiteSpace: 'pre-line' }}>{t.footer.guide1}</p>
            <p style={{ whiteSpace: 'pre-line' }}>{t.footer.guide2}</p>
          </div>
        </div>
        <div className="footer-bottom">
          <p>{t.footer.disclaimer}</p>
          <span>{t.footer.copyright}</span>
        </div>
      </div>
    </footer>
  )
}
