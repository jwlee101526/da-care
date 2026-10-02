import { Bell, Check, Clock } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
import type { GuestReservation } from '../../lib/api'
import { getCategoryInfo } from '../../lib/categories'
import { formatPhone } from '../../lib/phone'
import { formatReservationCode } from '../../lib/reservationCode'
import type { ReservationFormValues } from './reservationForm'

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
    <div className="order-complete-view">
      <div className="order-complete-card">
        <div className="complete-badge-icon">
          <Check size={32} />
        </div>
        <h2>{lang === 'en' ? 'Reservation Successfully Received!' : '서비스 예약이 정상 접수되었습니다!'}</h2>
        <p>
          {lang === 'en'
            ? 'A specialized engineer will be assigned shortly and review your request.'
            : '고객님의 예약이 등록되었으며, 담당 엔지니어가 배정된 후 방문 일정을 확정해 드립니다.'}
        </p>

        {reservation && (
          <div className="order-complete-summary">
            <SummaryRow label={lang === 'en' ? 'Order Number' : '예약 번호'}>{formatReservationCode(reservation.code)}</SummaryRow>
            <SummaryRow label={lang === 'en' ? 'Category' : '수리 품목'}>{getCategoryInfo(reservation.deviceType, lang)}</SummaryRow>
            <SummaryRow label={lang === 'en' ? 'Schedule' : '희망 일시'}>{reservation.preferredAt.replace('T', ' ')}</SummaryRow>
            <SummaryRow label={lang === 'en' ? 'Address' : '방문지'}>{reservation.visitAddress}</SummaryRow>
          </div>
        )}

        <NotificationGuide />

        {!isMember && (
          <div className="guest-complete-notice">
            <strong>{lang === 'en' ? 'Guest Reservation Notice' : '비회원 예약 접수 안내'}</strong>
            <p>
              {lang === 'en'
                ? `Your order ${formatReservationCode(reservation?.code ?? '')} is registered. Our engineer will contact you at your mobile number (${formatPhone(phone)}) prior to visit.`
                : `비회원 예약 번호 ${formatReservationCode(reservation?.code ?? '')}와 입력하신 휴대전화 번호(${formatPhone(phone)})로 정상 접수되었습니다. 담당 기사 배정 후 방문 전 유선으로 사전 연락드립니다.`}
            </p>
            <p>
              {lang === 'en'
                ? 'To check or cancel later, use Guest Booking Lookup with this reservation number and your mobile number. Please keep the reservation number.'
                : '예약 확인·취소는 [비회원 예약 조회]에서 예약 번호와 휴대전화 번호로 할 수 있습니다. 예약 번호를 꼭 메모해 두세요.'}
            </p>
          </div>
        )}

        <div className="order-complete-actions">
          {isMember ? (
            <Link to={reservationsPath} className="button primary full-width">
              {lang === 'en' ? 'View My Reservations' : '내 예약 내역 확인하기'}
            </Link>
          ) : (
            <Link
              to={lang === 'en' ? '/en/signup' : '/signup'}
              state={{ name, phone, address }}
              className="button primary full-width"
            >
              {lang === 'en' ? 'Sign Up with this Info (1-Click)' : '방금 입력한 정보로 1초 회원가입'}
            </Link>
          )}
          {!isMember && reservation && (
            <Link
              to={lang === 'en' ? '/en/reservations/lookup' : '/reservations/lookup'}
              state={{ reservationCode: reservation.code, phone }}
              className="button secondary full-width"
            >
              {lang === 'en' ? 'Go to Guest Booking Lookup' : '비회원 예약 조회로 이동'}
            </Link>
          )}
          <Link to={homePath} className="button secondary full-width">
            {lang === 'en' ? 'Go to Home' : '메인 홈으로 이동'}
          </Link>
        </div>
      </div>
    </div>
  )
}

function SummaryRow({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="summary-row">
      <span>{label}</span>
      <strong>{children}</strong>
    </div>
  )
}

function NotificationGuide() {
  const { lang } = useLanguage()
  return (
    <section className="reservation-notification-guide" aria-labelledby="notification-guide-title">
      <div className="notification-guide-heading">
        <Bell size={20} aria-hidden="true" />
        <div>
          <h3 id="notification-guide-title">{lang === 'en' ? 'What happens next' : '이후 진행 안내'}</h3>
          <p>{lang === 'en' ? 'We will keep you informed as the reservation progresses.' : '예약 진행 단계에 맞춰 필요한 내용을 안내해 드립니다.'}</p>
        </div>
      </div>
      <ol className="notification-guide-list">
        <li>
          <span className="notification-guide-marker" aria-hidden="true"><Check size={14} /></span>
          <div>
            <strong>{lang === 'en' ? 'Request received' : '예약 접수 완료'}</strong>
            <p>{lang === 'en' ? 'Our operations team reviews your requested schedule and repair details.' : '운영팀에서 희망 일정과 수리 요청 내용을 확인합니다.'}</p>
          </div>
        </li>
        <li>
          <span className="notification-guide-marker pending" aria-hidden="true"><Clock size={14} /></span>
          <div>
            <strong>{lang === 'en' ? 'Technician assigned and schedule confirmed' : '기사 배정 및 방문 일정 확정'}</strong>
            <p>{lang === 'en' ? 'Once confirmed, the assigned technician and visit time will be sent to your mobile number by SMS.' : '확정되면 담당 기사와 방문 일시를 입력하신 휴대전화 번호로 문자로 보내드립니다.'}</p>
          </div>
        </li>
      </ol>
    </section>
  )
}
