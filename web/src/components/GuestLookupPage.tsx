import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { AlertCircle } from 'lucide-react'
import { useLanguage } from '../context/LanguageContext'
import { api, ApiError, type Reservation } from '../lib/api'
import { isValidPhone } from '../lib/phone'
import { PhoneInput } from './PhoneInput'
import { ReservationCard } from './ReservationCard'

// 예약 완료 화면에서 넘어오면 예약 번호와 휴대전화 번호를 미리 채운다.
type LookupState = { reservationId?: number; phone?: string } | null

export function GuestLookupPage() {
  const { lang } = useLanguage()
  const location = useLocation()
  const initial = location.state as LookupState

  const [reservationId, setReservationId] = useState(initial?.reservationId ? String(initial.reservationId) : '')
  const [phone, setPhone] = useState(initial?.phone ?? '')
  const [guestPassword, setGuestPassword] = useState('')
  const [reservation, setReservation] = useState<Reservation | null>(null)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [cancelling, setCancelling] = useState(false)

  const loginPath = lang === 'en' ? '/en/login' : '/login'

  async function handleLookup(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    if (!reservationId) {
      setError(lang === 'en' ? 'Please enter your reservation number.' : '예약 번호를 입력해 주세요.')
      return
    }
    if (!isValidPhone(phone)) {
      setError(lang === 'en' ? 'Enter a valid mobile number.' : '올바른 휴대전화 번호를 입력해 주세요.')
      return
    }
    if (!/^\d{4}$/.test(guestPassword)) {
      setError(lang === 'en' ? 'Enter your 4-digit lookup PIN.' : '조회용 비밀번호 4자리를 입력해 주세요.')
      return
    }

    setSubmitting(true)
    try {
      setReservation(await api<Reservation>('/api/reservations/guest/lookup', {
        method: 'POST',
        body: JSON.stringify({ reservationId: Number(reservationId), contactPhone: phone, guestPassword }),
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
      setReservation(await api<Reservation>(`/api/reservations/guest/${reservation.id}/cancel`, {
        method: 'PATCH',
        body: JSON.stringify({ contactPhone: phone, guestPassword }),
      }))
    } catch (err) {
      setError(err instanceof ApiError ? err.message : (lang === 'en' ? 'Failed to cancel the reservation.' : '예약 취소 처리에 실패했습니다.'))
    } finally {
      setCancelling(false)
    }
  }

  function handleReset() {
    setReservation(null)
    setReservationId('')
    setGuestPassword('')
    setError('')
  }

  return (
    <div className="order-details-view">
      <main className="order-main-container">
        <div className="order-page-header">
          <div>
            <h1 className="order-page-title">{lang === 'en' ? 'Guest Reservation Lookup' : '비회원 예약 조회'}</h1>
            <p className="order-page-desc">
              {lang === 'en'
                ? 'Enter the reservation number, mobile number and lookup PIN you used when booking.'
                : '예약할 때 받은 예약 번호와 입력하신 휴대전화 번호, 조회용 비밀번호를 입력해 주세요.'}
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
                <label htmlFor="lookup-id">{lang === 'en' ? 'Reservation Number' : '예약 번호'}</label>
                <input
                  id="lookup-id"
                  required
                  inputMode="numeric"
                  className="order-input"
                  placeholder={lang === 'en' ? 'e.g. 128' : '예: 128'}
                  value={reservationId}
                  onChange={e => setReservationId(e.target.value.replace(/[^0-9]/g, ''))}
                />
              </div>
              <div className="customer-field full">
                <label htmlFor="lookup-phone">{lang === 'en' ? 'Mobile Number' : '휴대전화 번호'}</label>
                <PhoneInput id="lookup-phone" required value={phone} onChange={setPhone} />
              </div>
              <div className="customer-field full">
                <label htmlFor="lookup-pin">{lang === 'en' ? 'Lookup PIN (4 digits)' : '조회용 비밀번호 (4자리)'}</label>
                <input
                  id="lookup-pin"
                  required
                  type="password"
                  inputMode="numeric"
                  maxLength={4}
                  autoComplete="off"
                  className="order-input"
                  value={guestPassword}
                  onChange={e => setGuestPassword(e.target.value.replace(/[^0-9]/g, ''))}
                />
              </div>
            </div>

            <div className="order-action-bar">
              <button type="submit" className="button primary full-width" disabled={submitting}>
                {submitting ? (lang === 'en' ? 'Looking up...' : '조회 중...') : (lang === 'en' ? 'Look Up Reservation' : '예약 조회')}
              </button>
            </div>

            <p className="guest-lookup-member-hint">
              {lang === 'en' ? 'Booked as a member? ' : '회원으로 예약하셨나요? '}
              <Link to={loginPath}>{lang === 'en' ? 'Sign in to see My Reservations' : '로그인 후 내 예약에서 확인하세요'}</Link>
            </p>
          </form>
        )}
      </main>
    </div>
  )
}
