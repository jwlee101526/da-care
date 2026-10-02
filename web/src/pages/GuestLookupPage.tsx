import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { REGEXP_ONLY_DIGITS } from 'input-otp'
import { useLanguage } from '../context/LanguageContext'
import { api, ApiError, type GuestReservation } from '../lib/api'
import { isValidPhone } from '../lib/phone'
import { isValidReservationCode } from '../lib/reservationCode'
import { PhoneInput } from '../components/PhoneInput'
import { ReservationCard } from '../components/ReservationCard'
import { InputOTP, InputOTPGroup, InputOTPSeparator, InputOTPSlot } from '../components/ui/input-otp'
import { OrderPageLayout } from '../components/OrderPageLayout'
import { ErrorAlert } from '../components/ErrorAlert'
import { buttonVariants } from '../components/ui/button'
import { actionBar, fieldLabel, fieldsGrid, fullField, orderInput, orderSectionBox } from '../components/orderForm'
import { cn } from '@/lib/utils'

// 예약 번호 8칸 입력. 4칸씩 두 묶음을 하이픈으로 나눠 화면 표시 형식(4821-7390)과 맞춘다.
const codeSlot = 'h-12 w-auto min-w-0 flex-1 border-y-[1.5px] border-r-[1.5px] border-line-strong bg-white text-[17px] leading-[1.4285714] font-semibold text-ink first:rounded-l-[10px] first:border-l-[1.5px] last:rounded-r-[10px] data-[active=true]:border-brand data-[active=true]:shadow-[0_0_0_3px_rgba(36,87,214,0.15)] data-[active=true]:ring-0'

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
    <OrderPageLayout
      title={lang === 'en' ? 'Guest Reservation Lookup' : '비회원 예약 조회'}
      description={lang === 'en'
        ? 'Enter the reservation number and the mobile number used when booking.'
        : '예약 번호와 예약할 때 입력한 휴대전화 번호로 조회합니다.'}
      action={reservation && (
        <button type="button" className={buttonVariants({ variant: 'secondary', size: 'compact' })} onClick={handleReset}>
          {lang === 'en' ? 'Look Up Another' : '다른 예약 조회'}
        </button>
      )}
      narrow={!reservation}
    >
      {error && <ErrorAlert role="alert">{error}</ErrorAlert>}

      {reservation ? (
        <div className="flex flex-col gap-6">
          <ReservationCard
            item={reservation}
            onCancel={handleCancel}
            cancelling={cancelling}
            cancelDisabled={cancelling}
          />
        </div>
      ) : (
        <form className={orderSectionBox} onSubmit={handleLookup} noValidate>
          <div className={cn(fieldsGrid, 'mt-0')}>
            <div className={fullField}>
              <label className={fieldLabel} htmlFor="lookup-code">{lang === 'en' ? 'Reservation Number' : '예약 번호'}</label>
              {/* 기본값(one-time-code)이면 모바일 키보드가 문자로 받은 인증번호를 제안하므로 자동완성을 끈다.
                  완료 화면에서 복사한 4821-7390 형식도 붙여넣을 수 있게 숫자만 남긴다. */}
              <InputOTP
                id="lookup-code"
                maxLength={8}
                pattern={REGEXP_ONLY_DIGITS}
                inputMode="numeric"
                autoComplete="off"
                pasteTransformer={text => text.replace(/\D/g, '')}
                containerClassName="w-full gap-1.5"
                aria-describedby="lookup-code-hint"
                value={reservationCode}
                onChange={setReservationCode}
              >
                <InputOTPGroup className="min-w-0 flex-1">
                  {[0, 1, 2, 3].map(index => <InputOTPSlot key={index} index={index} className={codeSlot} />)}
                </InputOTPGroup>
                <InputOTPSeparator className="text-ink-faint" />
                <InputOTPGroup className="min-w-0 flex-1">
                  {[4, 5, 6, 7].map(index => <InputOTPSlot key={index} index={index} className={codeSlot} />)}
                </InputOTPGroup>
              </InputOTP>
              <small id="lookup-code-hint" className="mt-1.5 text-[12px] text-ink-subtle">
                {lang === 'en' ? 'The 8-digit number shown on the booking complete screen' : '예약 완료 화면에 표시된 숫자 8자리'}
              </small>
            </div>
            <div className={fullField}>
              <label className={fieldLabel} htmlFor="lookup-phone">{lang === 'en' ? 'Mobile Number' : '휴대전화 번호'}</label>
              <PhoneInput id="lookup-phone" required className={orderInput} value={phone} onChange={setPhone} />
            </div>
          </div>

          <div className={actionBar}>
            <button type="submit" className={buttonVariants({ variant: 'primary', className: 'w-full' })} disabled={submitting}>
              {submitting ? (lang === 'en' ? 'Looking up...' : '조회 중...') : (lang === 'en' ? 'Look Up Reservation' : '예약 조회')}
            </button>
          </div>

          <p className="mt-4 mb-0 text-center text-[13px] leading-[1.5] text-ink-subtle">
            {lang === 'en' ? 'Booked as a member? ' : '회원으로 예약하셨나요? '}
            <Link className="font-semibold text-brand" to={loginPath}>{lang === 'en' ? 'Sign in to see all your reservations' : '로그인하면 전체 예약 내역을 볼 수 있습니다'}</Link>
          </p>
        </form>
      )}
    </OrderPageLayout>
  )
}
