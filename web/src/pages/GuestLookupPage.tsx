import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { AlertCircle } from 'lucide-react'
import { REGEXP_ONLY_DIGITS } from 'input-otp'
import { useLanguage } from '../context/LanguageContext'
import { api, ApiError, type GuestReservation } from '../lib/api'
import { isValidPhone } from '../lib/phone'
import { isValidReservationCode } from '../lib/reservationCode'
import { PhoneInput } from '../components/PhoneInput'
import { ReservationCard } from '../components/ReservationCard'
import { InputOTP, InputOTPGroup, InputOTPSeparator, InputOTPSlot } from '../components/ui/input-otp'

// 예약 완료 화면에서 넘어오면 예약 번호와 휴대전화 번호를 미리 채운다.
type LookupState = { reservationCode?: string; phone?: string } | null

export function GuestLookupPage() {
  const { lang } = useLanguage()
  const location = useLocation()
  const initial = location.state as LookupState

  const [reservationCode, setReservationCode] = useState(initial?.reservationCode ?? '')
  const [phone, setPhone] = useState(initial?.phone ?? '')
  const [reservation, setReservation] = useState<GuestReservation | null>(null)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [cancelling, setCancelling] = useState(false)

  const loginPath = lang === 'en' ? '/en/login' : '/login'

  async function handleLookup(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    if (!isValidReservationCode(reservationCode)) {
      setError(lang === 'en' ? 'Enter the 8-digit reservation number.' : '예약 번호 8자리를 입력해 주세요.')
      return
    }
    if (!isValidPhone(phone)) {
      setError(lang === 'en' ? 'Enter a valid mobile number.' : '올바른 휴대전화 번호를 입력해 주세요.')
      return
    }

    setSubmitting(true)
    try {
      setReservation(await api<GuestReservation>('/api/reservations/guest/lookup', {
        method: 'POST',
        body: JSON.stringify({ reservationCode, contactPhone: phone }),
      }))
    } catch (err) {
      setError(err instanceof ApiError ? err.message : (lang === 'en' ? 'Failed to look up the reservation.' : '예약 조회에 실패했습니다.'))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleCancel() {
    if (!reservation || cancelling) return
    if (!window.confirm(lang === 'en' ? 'Cancel this reservation?' : '예약을 취소하시겠습니까?')) return
    setCancelling(true)
    setError('')
    try {
      setReservation(await api<GuestReservation>(`/api/reservations/guest/${reservation.code}/cancel`, {
        method: 'PATCH',
        body: JSON.stringify({ contactPhone: phone }),
      }))
    } catch (err) {
      setError(err instanceof ApiError ? err.message : (lang === 'en' ? 'Failed to cancel the reservation.' : '예약 취소 처리에 실패했습니다.'))
    } finally {
      setCancelling(false)
    }
  }

  function handleReset() {
    setReservation(null)
    setReservationCode('')
    setError('')
  }

  return (
    <div className="order-details-view">
      <main className={`order-main-container${reservation ? '' : ' guest-lookup-container'}`}>
        <div className="order-page-header">
          <div>
            <h1 className="order-page-title">{lang === 'en' ? 'Guest Reservation Lookup' : '비회원 예약 조회'}</h1>
            <p className="order-page-desc">
              {lang === 'en'
                ? 'Enter the reservation number and the mobile number used when booking.'
                : '예약 번호와 예약할 때 입력한 휴대전화 번호로 조회합니다.'}
            </p>
          </div>
          {reservation && (
            <button type="button" className="button secondary compact" onClick={handleReset}>
              {lang === 'en' ? 'Look Up Another' : '다른 예약 조회'}
            </button>
          )}
        </div>

        {error && (
          <div className="order-alert-error" role="alert">
            <AlertCircle size={18} />
            <span>{error}</span>
          </div>
        )}

        {reservation ? (
          <div className="order-list-column">
            <ReservationCard
              item={reservation}
              onCancel={handleCancel}
              cancelling={cancelling}
              cancelDisabled={cancelling}
            />
          </div>
        ) : (
          <form className="order-section-box" onSubmit={handleLookup} noValidate>
            <div className="customer-fields-grid">
              <div className="customer-field full">
                <label htmlFor="lookup-code">{lang === 'en' ? 'Reservation Number' : '예약 번호'}</label>
                {/* 기본값(one-time-code)이면 모바일 키보드가 문자로 받은 인증번호를 제안하므로 자동완성을 끈다.
                    완료 화면에서 복사한 4821-7390 형식도 붙여넣을 수 있게 숫자만 남긴다. */}
                <InputOTP
                  id="lookup-code"
                  maxLength={8}
                  pattern={REGEXP_ONLY_DIGITS}
                  inputMode="numeric"
                  autoComplete="off"
                  pasteTransformer={text => text.replace(/\D/g, '')}
                  containerClassName="reservation-code-input"
                  aria-describedby="lookup-code-hint"
                  value={reservationCode}
                  onChange={setReservationCode}
                >
                  <InputOTPGroup>
                    {[0, 1, 2, 3].map(index => <InputOTPSlot key={index} index={index} />)}
                  </InputOTPGroup>
                  <InputOTPSeparator />
                  <InputOTPGroup>
                    {[4, 5, 6, 7].map(index => <InputOTPSlot key={index} index={index} />)}
                  </InputOTPGroup>
                </InputOTP>
                <small id="lookup-code-hint" className="reservation-code-hint">
                  {lang === 'en' ? 'The 8-digit number shown on the booking complete screen' : '예약 완료 화면에 표시된 숫자 8자리'}
                </small>
              </div>
              <div className="customer-field full">
                <label htmlFor="lookup-phone">{lang === 'en' ? 'Mobile Number' : '휴대전화 번호'}</label>
                <PhoneInput id="lookup-phone" required className="order-input" value={phone} onChange={setPhone} />
              </div>
            </div>

            <div className="order-action-bar">
              <button type="submit" className="button primary full-width" disabled={submitting}>
                {submitting ? (lang === 'en' ? 'Looking up...' : '조회 중...') : (lang === 'en' ? 'Look Up Reservation' : '예약 조회')}
              </button>
            </div>

            <p className="guest-lookup-notes">
              {lang === 'en' ? 'Booked as a member? ' : '회원으로 예약하셨나요? '}
              <Link to={loginPath}>{lang === 'en' ? 'Sign in to see all your reservations' : '로그인하면 전체 예약 내역을 볼 수 있습니다'}</Link>
            </p>
          </form>
        )}
      </main>
    </div>
  )
}
