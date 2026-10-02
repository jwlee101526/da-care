import { ArrowRight, CalendarDays, ChevronDown, ClipboardCheck, Wrench } from 'lucide-react'
import { useLanguage } from '../../context/LanguageContext'
import type { DiagnosisCard } from '../../lib/api'
import { getCategoryInfo } from '../../lib/categories'
import { formatReservationCode } from '../../lib/reservationCode'
import type { BookWithSymptom, ChatText, NavigationPage } from './types'

export function DiagnosisCards({ cards, text, onBook, onRequestTool, onNavigate }: {
  cards: DiagnosisCard[]
  text: ChatText['card']
  onBook: BookWithSymptom
  onRequestTool: (message: string, showUserMessage?: boolean) => void
  onNavigate: (page: NavigationPage) => void
}) {
  const { lang } = useLanguage()
  return (
    <div className="diagnosis-cards">
      {cards.map((card, index) => {
        if (card.type === 'inspection') return (
          <article className="diagnosis-card visit-card" key={index}>
            <div className="visit-card-heading"><strong><ClipboardCheck size={18} aria-hidden="true" />{card.title}</strong><span className="visit-recommendation">{text.manualBased}</span></div>
            <dl><div><dt>{text.device}</dt><dd>{card.deviceName}</dd></div>{card.suspectedCause && <div><dt>{text.cause}</dt><dd>{card.suspectedCause}</dd></div>}<div><dt>{text.inspection}</dt><dd>{card.inspectionDetails}</dd></div></dl>
            {card.evidence.length > 0 && (
              <details className="diagnosis-evidence">
                <summary>{text.evidence}<span>{card.evidence.length}</span><ChevronDown size={15} aria-hidden="true" /></summary>
                {card.evidence.map(source => (
                  <figure key={source.sourceId}>
                    <blockquote>{source.quote}</blockquote>
                    <figcaption>{text.source(source.sourceId)}</figcaption>
                  </figure>
                ))}
              </details>
            )}
            <CardNextAction
              title={text.nextAction}
              label={text.prepareVisit}
              detail={text.prepareVisitDetail}
              onClick={() => onRequestTool(text.prepareVisitRequest, false)}
            />
          </article>
        )
        if (card.type === 'booking') return (
          <article className="diagnosis-card visit-card" key={index}>
            <div className="visit-card-heading"><strong><CalendarDays size={17} /> {text.bookingTitle}</strong></div>
            <dl><div><dt>{text.device}</dt><dd>{getCategoryInfo(card.deviceType, lang)}</dd></div><div><dt>{text.symptom}</dt><dd>{card.symptom}</dd></div></dl>
            <small>{text.bookingDescription}</small>
            <button className="chat-booking" onClick={() => onBook({ device: card.deviceType, symptom: card.symptom })}>{text.continueBooking} <ArrowRight size={17} /></button>
          </article>
        )
        if (card.type === 'reservation_status') return (
          <article className="diagnosis-card visit-card" key={index}>
            <div className="visit-card-heading"><strong>{text.reservation(formatReservationCode(card.reservationCode))}</strong><span className={'reservation-status is-' + card.status.toLowerCase()}>{text.status[card.status]}</span></div>
            <dl><div><dt>{text.preferredAt}</dt><dd>{card.preferredAt.replace('T', ' ')}</dd></div><div><dt>{text.confirmedAt}</dt><dd>{card.confirmedAt?.replace('T', ' ') ?? text.notConfirmed}</dd></div><div><dt>{text.engineer}</dt><dd>{card.engineerName ?? text.notAssigned}</dd></div></dl>
          </article>
        )
        if (card.type === 'navigation') return (
          <article className="diagnosis-card navigation-card" key={index}>
            <div className="visit-card-heading"><strong><CalendarDays size={18} aria-hidden="true" />{card.title}</strong></div>
            <p>{card.description}</p>
            <button className="chat-booking" onClick={() => onNavigate(card.page)}>{card.actionLabel}<ArrowRight size={17} /></button>
          </article>
        )
        return null
      })}
    </div>
  )
}

function CardNextAction({ title, label, detail, onClick }: { title: string; label: string; detail: string; onClick: () => void }) {
  return (
    <div className="card-next-action">
      <div><span>{title}</span><small>{detail}</small></div>
      <button type="button" onClick={onClick}><Wrench size={14} aria-hidden="true" />{label}<ArrowRight size={14} aria-hidden="true" /></button>
    </div>
  )
}
