import { ArrowRight, CalendarDays, ChevronDown, ClipboardCheck, Wrench } from 'lucide-react'
import { useLanguage } from '../../context/LanguageContext'
import type { DiagnosisCard } from '../../lib/api'
import { getCategoryInfo } from '../../lib/categories'
import { formatReservationCode } from '../../lib/reservationCode'
import type { BookWithSymptom, ChatText, NavigationPage } from './types'
import type { Reservation } from '../../lib/api'
import { cn } from '@/lib/utils'
import { messageBubble } from './chatStyles'

const cardClass = 'overflow-hidden rounded-2xl border border-line-strong bg-white shadow-[0_2px_6px_#182d4c14] max-[540px]:p-[15px]'
const cardHeading = 'flex items-center justify-between gap-2 border-b border-line-default bg-surface-subtle px-4 py-[13px] [&_svg]:shrink-0'
const cardTitle = 'flex items-center gap-[7px] text-[14px] leading-[1.5] font-bold tracking-[-.03em] text-[#0f172a]'
const cardButton = 'mx-[15px] mt-3.5 mb-[15px] flex min-h-[42px] w-[calc(100%-30px)] cursor-pointer items-center justify-center gap-2 rounded-[11px] border-0 bg-[#2455d9] p-[9px] text-[13px] font-bold text-white shadow-none hover:bg-brand-hover'
const statusClass: Record<Reservation['status'], string> = {
  PENDING: 'bg-surface-muted text-ink-secondary',
  CONFIRMED: 'bg-[#e8f3ff] text-[#1b64da]',
  COMPLETED: 'bg-[#e8faf0] text-[#0f7a3d]',
  CANCELLED: 'bg-[#fff0f0] text-[#c4242f]',
}

// 카드 안의 항목명-값 목록
function Facts({ children }: { children: React.ReactNode }) {
  return <dl className="mx-[15px] mt-3.5 mb-0 grid gap-0 rounded-[11px] border border-line-default bg-surface-subtle px-3.5 py-0">{children}</dl>
}

function Fact({ term, children }: { term: string; children: React.ReactNode }) {
  return (
    <div className="grid grid-cols-[72px_minmax(0,1fr)] gap-2.5 border-b border-line-default py-2.5 text-[13px] leading-[1.55] last:border-b-0">
      <dt className="font-semibold text-ink-secondary">{term}</dt>
      <dd className="m-0 font-semibold whitespace-pre-wrap text-[#0f172a] [overflow-wrap:anywhere]">{children}</dd>
    </div>
  )
}

export function DiagnosisCards({ cards, text, onBook, onRequestTool, onNavigate }: {
  cards: DiagnosisCard[]
  text: ChatText['card']
  onBook: BookWithSymptom
  onRequestTool: (message: string, showUserMessage?: boolean) => void
  onNavigate: (page: NavigationPage) => void
}) {
  const { lang } = useLanguage()
  return (
    <div className="mt-3 grid gap-3 empty:hidden">
      {cards.map((card, index) => {
        if (card.type === 'inspection') return (
          <article className={cardClass} key={index}>
            <div className={cardHeading}>
              <strong className={cardTitle}><ClipboardCheck size={18} aria-hidden="true" />{card.title}</strong>
              <span className="shrink-0 rounded-[999px] bg-[#e0ecff] px-2 py-[3px] text-[11.5px] font-bold text-[#1d4ed8] max-[540px]:text-[11px]">{text.manualBased}</span>
            </div>
            <Facts>
              <Fact term={text.device}>{card.deviceName}</Fact>
              {card.suspectedCause && <Fact term={text.cause}>{card.suspectedCause}</Fact>}
              <Fact term={text.inspection}>{card.inspectionDetails}</Fact>
            </Facts>
            {card.evidence.length > 0 && (
              <details className="group mx-[15px] mt-3 mb-0 border-t border-surface-muted pt-2.5">
                <summary className="flex cursor-pointer list-none items-center gap-1 text-[13px] text-ink-subtle [&::-webkit-details-marker]:hidden">
                  {text.evidence}<span className="font-semibold text-[#2455d9]">{card.evidence.length}</span>
                  <ChevronDown className="ml-auto transition-transform duration-150 ease-[ease] group-open:rotate-180" size={15} aria-hidden="true" />
                </summary>
                {card.evidence.map(source => (
                  <figure className="mx-0 mt-2 mb-0 rounded-[10px] bg-surface-subtle p-3" key={source.sourceId}>
                    <blockquote className="m-0 text-[13px] leading-[1.6] text-ink-body [overflow-wrap:anywhere]">{source.quote}</blockquote>
                    <figcaption className="mt-1.5 text-[12px] text-ink-subtle">{source.citation ?? text.source(source.sourceId)}</figcaption>
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
          <article className={cardClass} key={index}>
            <div className={cardHeading}><strong className={cardTitle}><CalendarDays size={17} /> {text.bookingTitle}</strong></div>
            <Facts>
              <Fact term={text.device}>{getCategoryInfo(card.deviceType, lang)}</Fact>
              <Fact term={text.symptom}>{card.symptom}</Fact>
            </Facts>
            <small className="mx-[15px] mt-3 mb-0 block text-[12.5px] leading-[1.5] font-medium text-ink-body">{text.bookingDescription}</small>
            <button className={cardButton} onClick={() => onBook({ device: card.deviceType, symptom: card.symptom })}>{text.continueBooking} <ArrowRight size={17} /></button>
          </article>
        )
        if (card.type === 'reservation_status') return (
          <article className={cardClass} key={index}>
            <div className={cardHeading}>
              <strong className={cardTitle}>{text.reservation(formatReservationCode(card.reservationCode))}</strong>
              <span className={cn('shrink-0 rounded-md px-2 py-[3px] text-[12px] font-semibold', statusClass[card.status])}>{text.status[card.status]}</span>
            </div>
            <Facts>
              <Fact term={text.preferredAt}>{card.preferredAt.replace('T', ' ')}</Fact>
              <Fact term={text.confirmedAt}>{card.confirmedAt?.replace('T', ' ') ?? text.notConfirmed}</Fact>
              <Fact term={text.engineer}>{card.engineerName ?? text.notAssigned}</Fact>
            </Facts>
          </article>
        )
        if (card.type === 'navigation') return (
          <article className={cardClass} key={index}>
            <div className={cardHeading}><strong className={cardTitle}><CalendarDays size={18} aria-hidden="true" />{card.title}</strong></div>
            {/* 원래 CSS에서 메시지 말풍선 스타일이 이 문단에도 적용돼 있어 그대로 맞춘다. */}
            <p className={cn(messageBubble, 'mx-[15px] mt-3.5 mb-0 text-[13.5px] leading-[1.6] font-medium text-ink')}>{card.description}</p>
            <button className={cardButton} onClick={() => onNavigate(card.page)}>{card.actionLabel}<ArrowRight size={17} /></button>
          </article>
        )
        return null
      })}
    </div>
  )
}

function CardNextAction({ title, label, detail, onClick }: { title: string; label: string; detail: string; onClick: () => void }) {
  return (
    <div className="mt-3.5 flex items-center justify-between gap-3 border-t border-[#e4e8ee] bg-surface-subtle px-[15px] py-3">
      <div className="grid min-w-0 gap-0.5">
        <span className="text-[12.5px] font-extrabold text-[#0f172a]">{title}</span>
        <small className="text-[12px] leading-[1.4] font-medium text-ink-secondary">{detail}</small>
      </div>
      <button type="button" className="inline-flex min-h-[34px] flex-[0_0_auto] cursor-pointer items-center justify-center gap-1.5 rounded-[9px] border-0 bg-[#2455d9] px-[11px] py-[7px] text-[12px] font-bold text-white hover:bg-brand-hover" onClick={onClick}><Wrench size={14} aria-hidden="true" />{label}<ArrowRight size={14} aria-hidden="true" /></button>
    </div>
  )
}
