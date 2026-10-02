import dacareLogo from '@/assets/brand/dacare-logo.svg'
import { useLanguage } from '../context/LanguageContext'
import { cn } from '@/lib/utils'
import { sectionContainer, snapCentered } from './landing'

const footerLink = 'inline-flex min-h-11 items-center hover:text-white hover:underline hover:underline-offset-[5px] focus-visible:outline-white'
const footerText = 'text-[13px] leading-[1.9] break-keep'

export function Footer() {
  const { t } = useLanguage()

  return (
    <footer className={cn('bg-[#1c293d] pt-14 pb-10 text-white max-[600px]:pb-[100px]', snapCentered)}>
      <div className={sectionContainer}>
        <div className="mb-8 grid grid-cols-[1.4fr_.7fr_1fr] gap-10 max-[600px]:grid-cols-1 max-[600px]:gap-7">
          <div>
            <a className="inline-flex shrink-0 flex-col items-start gap-1 hover:text-white hover:underline hover:underline-offset-[5px] focus-visible:outline-white max-[600px]:gap-[3px]" href="#top" aria-label={t.nav.ariaHome}>
              <img className="block h-[27px] w-auto [filter:brightness(0)_invert(1)] max-[600px]:h-[25px]" src={dacareLogo} width="304" height="100" alt="다케어" />
            </a>
            <p className="mt-5 text-[16px] font-[550]">{t.footer.tagline}</p>
            <p className={cn(footerText, 'mt-2')} style={{ whiteSpace: 'pre-line' }}>{t.footer.description}</p>
          </div>
          <nav className="flex flex-col items-start text-[13px] max-[600px]:flex-row max-[600px]:flex-wrap max-[600px]:gap-x-6" aria-label="하단 메뉴">
            <h3 className="mt-0 mb-2 text-[14px] font-semibold max-[600px]:w-full">{t.footer.menuTitle}</h3>
            <a className={footerLink} href="#service">{t.nav.service}</a>
            <a className={footerLink} href="#status">{t.nav.status}</a>
            <a className={footerLink} href="#contact">{t.contact.title}</a>
            <a className={footerLink} href="#faq">{t.nav.faq}</a>
          </nav>
          <div>
            <h3 className="mt-0 mb-4 text-[14px] font-semibold">{t.footer.guideTitle}</h3>
            <p className={footerText} style={{ whiteSpace: 'pre-line' }}>{t.footer.guide1}</p>
            <p className={cn(footerText, 'mt-3')} style={{ whiteSpace: 'pre-line' }}>{t.footer.guide2}</p>
          </div>
        </div>
        <div className="flex items-end justify-between gap-7 border-t border-[#ffffff26] pt-6 text-[12px] max-[850px]:pr-[100px] max-[600px]:flex-col max-[600px]:items-start max-[600px]:gap-4 max-[600px]:pr-0">
          <p>{t.footer.disclaimer}</p>
          <span className="text-[12px] whitespace-nowrap">{t.footer.copyright}</span>
        </div>
      </div>
    </footer>
  )
}
