import { Reveal } from './Reveal'
import type { ReservationSelection } from '../types'
import { ServiceIllustration } from './ServiceIllustration'
import { useLanguage } from '../context/LanguageContext'
import { cn } from '@/lib/utils'
import { eyebrow, sectionContainer, snapCentered } from './landing'

interface QuickLinkProps { icon: 'diagnosis' | 'reservation' | 'repair'; title: string; description: string; action: () => void }

function QuickLink({ icon, title, description, action }: QuickLinkProps) {
  return <button type="button" className="group relative flex min-h-[205px] border-r border-b border-[#dfe5ed] bg-white p-[30px] text-left transition-[background] duration-200 ease-[ease] hover:bg-[#f6faff] max-[600px]:min-h-[156px] max-[600px]:p-[18px]" onClick={action}>
    <span className="absolute right-[22px] bottom-5 grid h-[92px] w-[92px] place-items-center max-[600px]:right-[17px] max-[600px]:bottom-[17px] max-[600px]:h-12 max-[600px]:w-12 max-[600px]:rounded-2xl [&>svg]:h-full [&>svg]:w-full"><ServiceIllustration kind={icon} /></span>
    <span className="flex max-w-[205px] flex-col gap-3 max-[600px]:max-w-[120px] max-[600px]:gap-[7px]">
      <strong className="text-[20px] font-bold tracking-[-.5px] text-[#172536] max-[600px]:text-[14px]">{title}</strong>
      <small className="text-[13px] leading-[1.55] text-muted max-[600px]:text-[11px] max-[600px]:leading-[1.4]">{description}</small>
    </span>
    <svg className="absolute bottom-7 left-[30px] h-[18px] w-[18px] text-brand transition-transform duration-200 ease-[ease] group-hover:translate-x-[5px] max-[600px]:bottom-4 max-[600px]:left-[18px] max-[600px]:w-4" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12h13M13 6l6 6-6 6" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" /></svg>
  </button>
}

export function ContactSection({ onOpenReservation, onOpenChat }: { onOpenReservation: (selection?: ReservationSelection) => void; onOpenChat: () => void }) {
  const { t } = useLanguage()

  return <section id="contact" className={cn('bg-white py-[108px] max-[600px]:py-16', snapCentered)}><Reveal className={sectionContainer}>
    <div className="mb-[54px] text-center max-[600px]:mb-8 max-[600px]:text-left">
      <span className={eyebrow}>{t.contact.eyebrow}</span>
      <h2 className="my-3 text-[32px] tracking-[-.9px] max-[600px]:text-[26px] landing-wide:text-[44px]">{t.contact.title}</h2>
      <p className="text-[14px] text-muted">{t.contact.desc}</p>
    </div>
    <div className="grid grid-cols-3 border-t border-l border-[#dfe5ed] max-[600px]:grid-cols-2">
      <QuickLink
        icon="diagnosis"
        title={t.contact.links.diagnosis.title}
        description={t.contact.links.diagnosis.description}
        action={onOpenChat}
      />
      <QuickLink
        icon="reservation"
        title={t.contact.links.reservation.title}
        description={t.contact.links.reservation.description}
        action={() => onOpenReservation()}
      />
      <QuickLink
        icon="repair"
        title={t.contact.links.repair.title}
        description={t.contact.links.repair.description}
        action={() => document.querySelector('#service')?.scrollIntoView({ behavior: 'smooth' })}
      />
    </div>
  </Reveal></section>
}
