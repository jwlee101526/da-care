import { useState, useEffect, useRef } from 'react'
import { ArrowLeft, Check, AlertCircle } from 'lucide-react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
import { useAuth } from '../../context/AuthContext'
import { api, ApiError, type GuestReservation } from '../../lib/api'
import { DEVICE_CATEGORIES, getCategoryDefinition } from '../../lib/categories'
import type { ReservationSelection } from '../../types'
import {
  initialReservationForm,
  toReservationRequest,
  validateReservationForm,
  type ReservationFormValues,
  type ReservationStep,
  type UpdateReservationForm,
} from './reservationForm'
import { ReservationFormStep } from './ReservationFormStep'
import { ReservationSummaryStep } from './ReservationSummaryStep'
import { ReservationCompleteStep } from './ReservationCompleteStep'

export function ReservationPage() {
  const { lang } = useLanguage()
  const { token } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const isMember = Boolean(token)

  const [step, setStep] = useState<ReservationStep>(1)
  const [form, setForm] = useState<ReservationFormValues>(
    () => initialReservationForm(location.state as ReservationSelection | null))
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [savedReservation, setSavedReservation] = useState<GuestReservation | null>(null)

  const topRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    topRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [step])

  useEffect(() => {
    if (error) topRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [error])

  const update: UpdateReservationForm = (field, value) => setForm(previous => ({ ...previous, [field]: value }))
  const selectedCategory = getCategoryDefinition(form.device) || DEVICE_CATEGORIES[0]

  const handleFormSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const message = validateReservationForm(form, lang, isMember)
    setError(message)
    if (!message) setStep(2)
  }

  const handleConfirmReservation = async () => {
    setSubmitting(true)
    setError('')

    try {
      // 회원 예약은 계정의 고객 정보와 연결되고, 비회원 예약은 예약 번호와 휴대전화 번호로만 조회할 수 있다.
      const reservation = await api<GuestReservation>(isMember ? '/api/reservations' : '/api/reservations/guest', {
        method: 'POST',
        body: JSON.stringify(toReservationRequest(form)),
      }, token)
      setSavedReservation(reservation)
      setStep(3)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : (lang === 'en' ? 'Failed to submit reservation. Please try again.' : '예약 접수 중 오류가 발생했습니다. 다시 시도해 주세요.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="order-flow-view" ref={topRef}>
      <main className="order-flow-container">
        <header className="order-flow-header">
          <button
            type="button"
            className="order-back-btn"
            onClick={() => {
              if (step === 2) setStep(1)
              else navigate(-1)
            }}
            aria-label="뒤로가기"
          >
            <ArrowLeft size={20} />
          </button>
          <h1 className="order-flow-title">
            {step === 3
              ? (lang === 'en' ? 'Booking Confirmed' : '접수 완료')
              : step === 2
              ? (lang === 'en' ? 'Confirm Reservation' : '예약 내용 확인')
              : (lang === 'en' ? 'Book Repair Service' : '서비스 예약 신청')}
          </h1>
          <div aria-hidden="true" />
        </header>

        <ReservationProgress step={step} />

        {error && (
          <div className="order-alert-error" role="alert">
            <AlertCircle size={18} />
            <span>{error}</span>
          </div>
        )}

        {step === 1 && (
          <ReservationFormStep form={form} update={update} isMember={isMember} onSubmit={handleFormSubmit} />
        )}
        {step === 2 && (
          <ReservationSummaryStep
            form={form}
            category={selectedCategory}
            isMember={isMember}
            submitting={submitting}
            onEdit={() => setStep(1)}
            onConfirm={handleConfirmReservation}
          />
        )}
        {step === 3 && (
          <ReservationCompleteStep form={form} reservation={savedReservation} isMember={isMember} />
        )}
      </main>
    </div>
  )
}

function ReservationProgress({ step }: { step: ReservationStep }) {
  const { lang } = useLanguage()
  const labels = lang === 'en'
    ? ['Details', 'Review', 'Complete']
    : ['정보 입력', '내용 확인', '접수 완료']
  return (
    <ol className="order-progress" aria-label={lang === 'en' ? 'Reservation steps' : '예약 진행 단계'}>
      {labels.map((label, index) => {
        const stepNumber = index + 1
        const state = stepNumber < step || step === 3 ? 'done' : stepNumber === step ? 'current' : ''
        return (
          <li key={label} className={state} aria-current={state === 'current' ? 'step' : undefined}>
            <span className="order-step-badge">
              {state === 'done' ? <Check size={13} /> : stepNumber}
            </span>
            {label}
          </li>
        )
      })}
    </ol>
  )
}
