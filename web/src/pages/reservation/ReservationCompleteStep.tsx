import { Bell, Check, Clock } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
import type { GuestReservation } from '../../lib/api'
import { getCategoryInfo } from '../../lib/categories'
import { formatPhone } from '../../lib/phone'
import { formatReservationCode } from '../../lib/reservationCode'
import { cn } from '@/lib/utils'
import { buttonVariants } from '../../components/ui/button'
import type { ReservationFormValues } from './reservationForm'

const guestNoticeText = 'mx-auto mt-0 mb-7 max-w-[440px] text-[14px] leading-[1.5] text-ink-subtle'
const guideText = 'mt-1 mb-0 max-w-none text-[13px] text-ink-subtle'

/** 3단계: 접수 결과와 이후 진행 안내. 비회원에게는 예약 번호 보관과 회원가입을 안내한다. */
export function ReservationCompleteStep({ form, reservation, isMember }: {
  form: ReservationFormValues
  reservation: GuestReservation | null
  isMember: boolean
}) {
  const { lang } = useLanguage()
  const reservationsPath = lang === 'en' ? '/en/reservations' : '/reservations'
  const homePath = lang === 'en' ? '/en' : '/'
  const { name, phone, address } = form

  return (
    <div className="pt-4">
      <div className="rounded-[18px] border border-line-default bg-white px-8 py-11 text-center shadow-[0_4px_20px_rgba(24,41,70,0.05)]">
        <div className="mx-auto mt-0 mb-5 grid h-16 w-16 place-items-center rounded-full bg-brand-soft text-brand">
          <Check size={32} />
        </div>
        <h2 className="mb-2.5 text-[24px] font-bold text-navy">{lang === 'en' ? 'Reservation Successfully Received!' : '서비스 예약이 정상 접수되었습니다!'}</h2>
        <p className="mx-auto mt-0 mb-7 max-w-[440px] text-[14px] text-ink-subtle">
          {lang === 'en'
            ? 'A specialized engineer will be assigned shortly and review your request.'
            : '고객님의 예약이 등록되었으며, 담당 엔지니어가 배정된 후 방문 일정을 확정해 드립니다.'}
        </p>

        {reservation && (
          <div className="mb-8 rounded-xl border border-line-default bg-surface-subtle px-5 py-4 text-left">
            <SummaryRow label={lang === 'en' ? 'Order Number' : '예약 번호'}>{formatReservationCode(reservation.code)}</SummaryRow>
            <SummaryRow label={lang === 'en' ? 'Category' : '수리 품목'}>{getCategoryInfo(reservation.deviceType, lang)}</SummaryRow>
            <SummaryRow label={lang === 'en' ? 'Schedule' : '희망 일시'}>{reservation.preferredAt.replace('T', ' ')}</SummaryRow>
            <SummaryRow label={lang === 'en' ? 'Address' : '방문지'}>{reservation.visitAddress}</SummaryRow>
          </div>
        )}

        <NotificationGuide />

        {!isMember && (
          <div className="mb-6 rounded-xl border border-brand-soft-border bg-[#eff6ff] px-5 py-4 text-left">
            <strong className="mb-1 block text-[13px] font-bold text-[#1d4ed8]">{lang === 'en' ? 'Guest Reservation Notice' : '비회원 예약 접수 안내'}</strong>
            <p className={guestNoticeText}>
              {lang === 'en'
                ? `Your order ${formatReservationCode(reservation?.code ?? '')} is registered. Our engineer will contact you at your mobile number (${formatPhone(phone)}) prior to visit.`
                : `비회원 예약 번호 ${formatReservationCode(reservation?.code ?? '')}와 입력하신 휴대전화 번호(${formatPhone(phone)})로 정상 접수되었습니다. 담당 기사 배정 후 방문 전 유선으로 사전 연락드립니다.`}
            </p>
            <p className={cn(guestNoticeText, 'mt-1.5')}>
              {lang === 'en'
                ? 'To check or cancel later, use Guest Booking Lookup with this reservation number and your mobile number. Please keep the reservation number.'
                : '예약 확인·취소는 [비회원 예약 조회]에서 예약 번호와 휴대전화 번호로 할 수 있습니다. 예약 번호를 꼭 메모해 두세요.'}
            </p>
          </div>
        )}

        <div className="flex flex-col gap-2.5">
          {isMember ? (
            <Link to={reservationsPath} className={buttonVariants({ variant: 'primary', className: 'w-full' })}>
              {lang === 'en' ? 'View My Reservations' : '내 예약 내역 확인하기'}
            </Link>
          ) : (
            <Link
              to={lang === 'en' ? '/en/signup' : '/signup'}
              state={{ name, phone, address }}
              className={buttonVariants({ variant: 'primary', className: 'w-full' })}
            >
              {lang === 'en' ? 'Sign Up with this Info (1-Click)' : '방금 입력한 정보로 1초 회원가입'}
            </Link>
          )}
          {!isMember && reservation && (
            <Link
              to={lang === 'en' ? '/en/reservations/lookup' : '/reservations/lookup'}
              state={{ reservationCode: reservation.code, phone }}
              className={buttonVariants({ variant: 'secondary', className: 'w-full' })}
            >
              {lang === 'en' ? 'Go to Guest Booking Lookup' : '비회원 예약 조회로 이동'}
            </Link>
          )}
          <Link to={homePath} className={buttonVariants({ variant: 'secondary', className: 'w-full' })}>
            {lang === 'en' ? 'Go to Home' : '메인 홈으로 이동'}
          </Link>
        </div>
      </div>
    </div>
  )
}

function SummaryRow({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between py-2 text-[14px] not-first:border-t not-first:border-surface-muted">
      <span className="text-ink-subtle">{label}</span>
      <strong className="text-navy">{children}</strong>
    </div>
  )
}

function NotificationGuide() {
  const { lang } = useLanguage()
  return (
    <section className="mx-0 mt-0 mb-7 border-y border-line-default p-5 text-left" aria-labelledby="notification-guide-title">
      <div className="flex items-start gap-3 text-brand">
        <Bell size={20} aria-hidden="true" />
        <div>
          <h3 className="m-0 text-[15px] text-navy" id="notification-guide-title">{lang === 'en' ? 'What happens next' : '이후 진행 안내'}</h3>
          <p className={cn(guideText, 'leading-[1.5]')}>{lang === 'en' ? 'We will keep you informed as the reservation progresses.' : '예약 진행 단계에 맞춰 필요한 내용을 안내해 드립니다.'}</p>
        </div>
      </div>
      <ol className="mx-0 mt-5 mb-0 grid list-none gap-4 p-0">
        <GuideStep
          marker={<Check size={14} />}
          title={lang === 'en' ? 'Request received' : '예약 접수 완료'}
          text={lang === 'en' ? 'Our operations team reviews your requested schedule and repair details.' : '운영팀에서 희망 일정과 수리 요청 내용을 확인합니다.'}
        />
        <GuideStep
          pending
          marker={<Clock size={14} />}
          title={lang === 'en' ? 'Technician assigned and schedule confirmed' : '기사 배정 및 방문 일정 확정'}
          text={lang === 'en' ? 'Once confirmed, the assigned technician and visit time will be sent to your mobile number by SMS.' : '확정되면 담당 기사와 방문 일시를 입력하신 휴대전화 번호로 문자로 보내드립니다.'}
        />
      </ol>
    </section>
  )
}

function GuideStep({ marker, title, text, pending = false }: { marker: React.ReactNode; title: string; text: string; pending?: boolean }) {
  return (
    <li className="grid grid-cols-[24px_1fr] items-start gap-3">
      <span className={cn('grid h-6 w-6 place-items-center rounded-full', pending ? 'bg-surface-muted text-ink-subtle' : 'bg-brand-soft text-brand')} aria-hidden="true">{marker}</span>
      <div>
        <strong className="text-[14px] text-ink">{title}</strong>
        <p className={cn(guideText, 'leading-[1.55]')}>{text}</p>
      </div>
    </li>
  )
}
