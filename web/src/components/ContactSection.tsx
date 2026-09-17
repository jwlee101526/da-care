import { Reveal } from './Reveal'
import type { ReservationSelection } from '../types'
import { ServiceIllustration } from './ServiceIllustration'
import { useLanguage } from '../context/LanguageContext'

interface QuickLinkProps { icon: 'diagnosis' | 'reservation' | 'repair'; title: string; description: string; action: () => void }

function QuickLink({ icon, title, description, action }: QuickLinkProps) {
  return <button type="button" className="quick-link" onClick={action}>
    <span className="quick-link-icon"><ServiceIllustration kind={icon} /></span>
    <span className="quick-link-copy"><strong>{title}</strong><small>{description}</small></span>
    <svg className="quick-link-arrow" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12h13M13 6l6 6-6 6" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" /></svg>
  </button>
}

export function ContactSection({ onOpenReservation, onOpenChat }: { onOpenReservation: (selection?: ReservationSelection) => void; onOpenChat: () => void }) {
  const { t } = useLanguage()

  return <section id="contact" className="quick-section"><Reveal className="container">
    <div className="quick-heading">
      <span className="eyebrow">{t.contact.eyebrow}</span>
      <h2>{t.contact.title}</h2>
      <p>{t.contact.desc}</p>
    </div>
    <div className="quick-grid">
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
