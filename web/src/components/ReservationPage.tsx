import { useState, useEffect, useRef } from 'react'
import { ArrowLeft, Check, Clock, AlertCircle, ChevronRight } from 'lucide-react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useLanguage } from '../context/LanguageContext'
import { useAuth } from '../context/AuthContext'
import { api, ApiError, type Reservation } from '../lib/api'
import { DEVICE_CATEGORIES, getCategoryDefinition, getCategoryInfo } from '../lib/categories'
import type { DeviceType, ReservationSelection } from '../types'

const QUICK_SYMPTOMS_KO = [
  '전원이 켜지지 않음',
  '이상 소음 / 진동 발생',
  '화면 / 디스플레이 출력 불량',
  '발열 및 자동 꺼짐 현상',
  '주요 기능 작동 멈춤',
  '소모품 및 부품 교체 희망',
]

const QUICK_SYMPTOMS_EN = [
  'Will not power on',
  'Abnormal noise / vibration',
  'Screen / display failure',
  'Overheating & shutdown',
  'Key functions unresponsive',
  'Part replacement requested',
]

const TIME_SLOTS = [
  '09:00',
  '10:30',
  '13:00',
  '14:30',
  '16:00',
  '17:30',
]

function getTomorrowDate(): string {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  return [
    tomorrow.getFullYear(),
    String(tomorrow.getMonth() + 1).padStart(2, '0'),
    String(tomorrow.getDate()).padStart(2, '0'),
  ].join('-')
}

export function ReservationPage() {
  const { lang } = useLanguage()
  const { token } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const quickSymptoms = lang === 'en' ? QUICK_SYMPTOMS_EN : QUICK_SYMPTOMS_KO
  const reservationsPath = lang === 'en' ? '/en/reservations' : '/reservations'
  const homePath = lang === 'en' ? '/en' : '/'

  const initialSelection = (location.state as ReservationSelection | null)

  const [step, setStep] = useState<1 | 2 | 3>(1)
  const [selectedDevice, setSelectedDevice] = useState<DeviceType>(initialSelection?.device || 'computer')
  const [symptom, setSymptom] = useState(initialSelection?.symptom || '')
  const [date, setDate] = useState(getTomorrowDate())
  const [time, setTime] = useState('10:30')
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [address, setAddress] = useState('')
  const [guestPassword, setGuestPassword] = useState('1234')
  const [agreedPrivacy, setAgreedPrivacy] = useState(true)

  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [savedReservation, setSavedReservation] = useState<Reservation | null>(null)

  const topRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (initialSelection) {
      if (initialSelection.device) setSelectedDevice(initialSelection.device)
      if (initialSelection.symptom) setSymptom(initialSelection.symptom)
    }
  }, [initialSelection])

  useEffect(() => {
    topRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [step])

  const selectedCategory = getCategoryDefinition(selectedDevice) || DEVICE_CATEGORIES[0]

  const handleQuickSymptom = (text: string) => {
    setSymptom(prev => {
      if (!prev.trim()) return text
      if (prev.includes(text)) return prev
      return `${prev}, ${text}`
    })
  }

  const handleStep1Submit = (e: React.FormEvent) => {
    e.preventDefault()
    setError('')

    if (!selectedDevice) {
      setError(lang === 'en' ? 'Please select a device category.' : '수리할 기기 품목을 선택해 주세요.')
      return
    }
    if (!symptom.trim()) {
      setError(lang === 'en' ? 'Please describe the symptom.' : '고장 증상을 입력해 주세요.')
      return
    }
    if (!date) {
      setError(lang === 'en' ? 'Please select a preferred date.' : '방문 희망 일자를 선택해 주세요.')
      return
    }
    if (!name.trim()) {
      setError(lang === 'en' ? 'Please enter your name.' : '신청자 성함을 입력해 주세요.')
      return
    }
    if (!phone.trim()) {
      setError(lang === 'en' ? 'Please enter your phone number.' : '연락처를 입력해 주세요.')
      return
    }
    if (!address.trim()) {
      setError(lang === 'en' ? 'Please enter the visit address.' : '방문 주소를 입력해 주세요.')
      return
    }
    if (!token && !agreedPrivacy) {
      setError(lang === 'en' ? 'Please agree to personal data collection for on-site service.' : '방문 수리 서비스 제공을 위한 개인정보 수집에 동의해 주세요.')
      return
    }

    setStep(2)
  }

  const handleConfirmReservation = async () => {
    setSubmitting(true)
    setError('')

    try {
      let reservation: Reservation
      if (token) {
        reservation = await api<Reservation>('/api/reservations', {
          method: 'POST',
          body: JSON.stringify({
            deviceType: selectedDevice,
            symptomDescription: symptom,
            contactName: name,
            contactPhone: phone,
            visitAddress: address,
            preferredAt: `${date}T${time}:00`,
          }),
        }, token)
      } else {
        reservation = await api<Reservation>('/api/reservations/guest', {
          method: 'POST',
          body: JSON.stringify({
            deviceType: selectedDevice,
            symptomDescription: symptom,
            visitAddress: address,
            preferredAt: `${date}T${time}:00`,
            contactName: name,
            contactPhone: phone,
            guestPassword: guestPassword || '1234',
          }),
        })
      }

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
        {/* Top App-like Header */}
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
          <div style={{ width: 36 }} /> {/* Balance Spacer */}
        </header>

        {/* Global Error Banner */}
        {error && (
          <div className="order-alert-error" role="alert">
            <AlertCircle size={18} />
            <span>{error}</span>
          </div>
        )}

        {/* Step 1: Input Form */}
        {step === 1 && (
          <form className="order-step-form" onSubmit={handleStep1Submit}>
            {/* Section 1: Device Category Selection (12 Categories) */}
            <section className="order-section-box">
              <div className="order-section-title">
                <span className="order-step-badge">1</span>
                <h3>{lang === 'en' ? 'Select Machine Category' : '수리 품목 선택 (12종)'}</h3>
              </div>
              <p className="order-section-sub">
                {lang === 'en'
                  ? 'Select the device or appliance that needs inspection and repair.'
                  : '점검 및 수리가 필요한 전자제품 또는 가전 기기를 선택해 주세요.'}
              </p>

              <div className="category-select-grid">
                {DEVICE_CATEGORIES.map(cat => {
                  const isSelected = selectedDevice === cat.deviceType
                  return (
                    <button
                      key={cat.deviceType}
                      type="button"
                      className={`category-select-card ${isSelected ? 'selected' : ''}`}
                      onClick={() => setSelectedDevice(cat.deviceType)}
                    >
                      <div className="cat-card-icon-wrap">
                        <img src={cat.iconSrc} alt={cat.labelKo} width={36} height={36} />
                      </div>
                      <span className="cat-card-name">
                        {lang === 'en' ? cat.labelEn : cat.labelKo}
                      </span>
                      <small className="cat-card-desc">
                        {lang === 'en' ? cat.descEn : cat.descKo}
                      </small>
                      {isSelected && (
                        <span className="cat-card-check">
                          <Check size={14} />
                        </span>
                      )}
                    </button>
                  )
                })}
              </div>
            </section>

            {/* Section 2: Symptoms and Notes */}
            <section className="order-section-box">
              <div className="order-section-title">
                <span className="order-step-badge">2</span>
                <h3>{lang === 'en' ? 'Symptom & Description' : '고장 증상 및 요청 사항'}</h3>
              </div>
              <p className="order-section-sub">
                {lang === 'en'
                  ? 'Select common symptoms or describe the problem in detail.'
                  : '자주 발생하는 증상을 클릭하시거나 구체적인 고장 증상을 적어주세요.'}
              </p>

              <div className="quick-tags-wrapper">
                <span className="quick-tags-label">{lang === 'en' ? 'Quick Add:' : '빠른 선택:'}</span>
                <div className="quick-tags-list">
                  {quickSymptoms.map(s => (
                    <button
                      key={s}
                      type="button"
                      className="quick-tag-chip"
                      onClick={() => handleQuickSymptom(s)}
                    >
                      + {s}
                    </button>
                  ))}
                </div>
              </div>

              <textarea
                className="order-textarea"
                rows={4}
                required
                placeholder={lang === 'en' ? 'Describe symptoms, model name, or special requests...' : '기기 모델명이나 구체적인 고장 증상을 적어주시면 엔지니어가 부품을 미리 준비할 수 있습니다.'}
                value={symptom}
                onChange={e => setSymptom(e.target.value)}
              />
            </section>

            {/* Split Row for Schedule and Customer Info */}
            <div className="order-split-row">
              {/* Section 3: Preferred Date & Time */}
              <section className="order-section-box">
                <div className="order-section-title">
                  <span className="order-step-badge">3</span>
                  <h3>{lang === 'en' ? 'Preferred Schedule' : '방문 희망 일정'}</h3>
                </div>
                <p className="order-section-sub">
                  {lang === 'en'
                    ? 'Select the preferred visit date and time slot.'
                    : '엔지니어의 방문을 희망하시는 날짜와 시간대를 선택해 주세요.'}
                </p>

                <div className="schedule-picker-row">
                  <div className="schedule-date-col">
                    <label htmlFor="visit-date">{lang === 'en' ? 'Preferred Date' : '방문 희망일'}</label>
                    <input
                      id="visit-date"
                      type="date"
                      className="order-input"
                      required
                      min={getTomorrowDate()}
                      value={date}
                      onChange={e => setDate(e.target.value)}
                    />
                  </div>
                  <div className="schedule-time-col">
                    <label htmlFor="visit-time">{lang === 'en' ? 'Preferred Time' : '방문 희망 시간'}</label>
                    <div className="time-chips-grid">
                      {TIME_SLOTS.map(slot => (
                        <button
                          key={slot}
                          type="button"
                          className={`time-chip ${time === slot ? 'active' : ''}`}
                          onClick={() => setTime(slot)}
                        >
                          {slot}
                        </button>
                      ))}
                    </div>
                    <div className="time-custom-row">
                      <span className="time-custom-label">{lang === 'en' ? 'Direct Time Pick:' : '직접 시간 지정:'}</span>
                      <input
                        id="visit-time"
                        type="time"
                        className="order-input time-custom-input"
                        value={time}
                        onChange={e => setTime(e.target.value)}
                      />
                    </div>
                  </div>
                </div>
              </section>

              {/* Section 4: Customer Information */}
              <section className="order-section-box">
                <div className="order-section-title">
                  <span className="order-step-badge">4</span>
                  <h3>{lang === 'en' ? 'Contact & Visit Location' : '신청 고객 및 방문 주소'}</h3>
                </div>
                <p className="order-section-sub">
                  {lang === 'en'
                    ? 'Please enter contact information and exact address for the on-site technician.'
                    : '엔지니어 방문 및 일정 안내를 위해 신청자 정보와 방문 주소를 정확히 입력해 주세요.'}
                </p>

                <div className="customer-fields-grid">
                  <div className="customer-field">
                    <label htmlFor="client-name">{lang === 'en' ? 'Customer Name' : '성함 (신청자)'}</label>
                    <input
                      id="client-name"
                      required
                      className="order-input"
                      placeholder={lang === 'en' ? 'e.g. John Doe' : '홍길동'}
                      value={name}
                      onChange={e => setName(e.target.value)}
                    />
                  </div>
                  <div className="customer-field">
                    <label htmlFor="client-phone">{lang === 'en' ? 'Phone Number' : '연락처'}</label>
                    <input
                      id="client-phone"
                      required
                      type="tel"
                      pattern="[0-9-]{9,13}"
                      className="order-input"
                      placeholder="010-1234-5678"
                      value={phone}
                      onChange={e => setPhone(e.target.value)}
                    />
                  </div>
                  <div className="customer-field full">
                    <label htmlFor="client-address">{lang === 'en' ? 'Visit Address' : '방문 주소'}</label>
                    <input
                      id="client-address"
                      required
                      className="order-input"
                      placeholder={lang === 'en' ? 'Enter full address including apartment / room number' : '방문 받으실 상세 주소를 입력해 주세요 (동/호수 포함)'}
                      value={address}
                      onChange={e => setAddress(e.target.value)}
                    />
                  </div>

                  {!token && (
                    <div className="customer-field">
                      <label htmlFor="guest-pwd">{lang === 'en' ? 'Lookup PIN (4 digits)' : '조회용 비밀번호 (4자리)'}</label>
                      <input
                        id="guest-pwd"
                        type="password"
                        maxLength={4}
                        className="order-input"
                        placeholder="1234"
                        value={guestPassword}
                        onChange={e => setGuestPassword(e.target.value.replace(/[^0-9]/g, ''))}
                      />
                    </div>
                  )}

                  {!token && (
                    <div className="customer-field full guest-privacy-box">
                      <label className="guest-privacy-check">
                        <input
                          type="checkbox"
                          checked={agreedPrivacy}
                          onChange={e => setAgreedPrivacy(e.target.checked)}
                          required
                        />
                        <span>
                          {lang === 'en'
                            ? '[Required] I consent to personal contact & address collection for dispatching visit service.'
                            : '[필수] 전담 엔지니어 배정 및 방문 수리 서비스 제공을 위한 개인정보(성함, 연락처, 주소) 수집·이용에 동의합니다.'}
                        </span>
                      </label>
                    </div>
                  )}
                </div>
              </section>
            </div>

            {/* Submit Action */}
            <div className="order-action-bar">
              <button
                type="submit"
                className="button primary full-width order-primary-btn"
              >
                {lang === 'en' ? 'Review Reservation Details' : '예약 내용 확인하기'}
                <ChevronRight size={18} />
              </button>
            </div>
          </form>
        )}

        {/* Step 2: Order Summary View (unified balanced card) */}
        {step === 2 && (
          <div className="order-summary-container">
            <div className="order-summary-main-card">
              {/* Top Banner: Device & Schedule Highlight */}
              <div className="summary-banner">
                <div className="summary-banner-icon">
                  <img src={selectedCategory.iconSrc} alt={selectedCategory.labelKo} width={34} height={34} />
                </div>
                <div className="summary-banner-text">
                  <h3>{lang === 'en' ? selectedCategory.labelEn : selectedCategory.labelKo}</h3>
                  <p className="summary-banner-time">
                    <Clock size={15} />
                    <span>{date} {time}</span>
                  </p>
                </div>
              </div>

              {/* 2-Column Balanced Grid */}
              <div className="summary-sections-grid">
                {/* Column 1: Customer Information */}
                <div className="summary-section">
                  <h4 className="summary-section-title">{lang === 'en' ? 'Customer Information' : '고객 정보'}</h4>
                  <div className="summary-kv-list">
                    <div className="summary-kv-item">
                      <span className="kv-label">{lang === 'en' ? 'Name' : '성함'}</span>
                      <span className="kv-val font-bold">{name}</span>
                    </div>
                    <div className="summary-kv-item">
                      <span className="kv-label">{lang === 'en' ? 'Contact' : '연락처'}</span>
                      <span className="kv-val">{phone}</span>
                    </div>
                    <div className="summary-kv-item">
                      <span className="kv-label">{lang === 'en' ? 'Address' : '방문 주소'}</span>
                      <span className="kv-val address">{address}</span>
                    </div>
                    <div className="summary-kv-item">
                      <span className="kv-label">{lang === 'en' ? 'Booking Type' : '접수 유형'}</span>
                      <span className="kv-val font-bold text-brand">
                        {token ? (lang === 'en' ? 'Registered Member' : '회원 예약') : (lang === 'en' ? 'Guest (Non-member)' : '비회원 간편 접수')}
                      </span>
                    </div>
                  </div>
                </div>

                {/* Column 2: Repair Details & Memo */}
                <div className="summary-section">
                  <h4 className="summary-section-title">{lang === 'en' ? 'Repair Details & Symptom' : '수리 품목 및 증상'}</h4>
                  <div className="summary-kv-list">
                    <div className="summary-kv-item">
                      <span className="kv-label">{lang === 'en' ? 'Category' : '품목'}</span>
                      <span className="kv-val font-bold">{lang === 'en' ? selectedCategory.labelEn : selectedCategory.labelKo}</span>
                    </div>
                    <div className="summary-kv-item">
                      <span className="kv-label">{lang === 'en' ? 'Schedule' : '희망 일시'}</span>
                      <span className="kv-val font-bold text-brand">{date} {time}</span>
                    </div>
                    <div className="summary-memo-block">
                      <span className="kv-label">{lang === 'en' ? 'Symptom Note' : '증상 메모'}</span>
                      <p className="summary-memo-content">{symptom}</p>
                    </div>
                  </div>
                </div>
              </div>

              {/* Bottom Action Buttons */}
              <div className="summary-actions">
                <button
                  type="button"
                  className="button secondary summary-back-btn"
                  onClick={() => setStep(1)}
                  disabled={submitting}
                >
                  {lang === 'en' ? 'Edit Details' : '내용 수정하기'}
                </button>
                <button
                  type="button"
                  className="button primary summary-confirm-btn"
                  onClick={handleConfirmReservation}
                  disabled={submitting}
                >
                  {submitting
                    ? (lang === 'en' ? 'Processing...' : '접수 진행 중...')
                    : (lang === 'en' ? 'Confirm & Submit Reservation' : '예약 접수 완료하기')}
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Step 3: Complete View */}
        {step === 3 && (
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

              {savedReservation && (
                <div className="order-complete-summary">
                  <div className="summary-row">
                    <span>{lang === 'en' ? 'Order Number' : '예약 번호'}</span>
                    <strong>#{savedReservation.id}</strong>
                  </div>
                  <div className="summary-row">
                    <span>{lang === 'en' ? 'Category' : '수리 품목'}</span>
                    <strong>{getCategoryInfo(savedReservation.deviceType, lang)}</strong>
                  </div>
                  <div className="summary-row">
                    <span>{lang === 'en' ? 'Schedule' : '희망 일시'}</span>
                    <strong>{savedReservation.preferredAt.replace('T', ' ')}</strong>
                  </div>
                  <div className="summary-row">
                    <span>{lang === 'en' ? 'Address' : '방문지'}</span>
                    <strong>{savedReservation.visitAddress}</strong>
                  </div>
                </div>
              )}

              {!token && (
                <div className="guest-complete-notice">
                  <strong>{lang === 'en' ? 'Guest Reservation Notice' : '비회원 예약 접수 안내'}</strong>
                  <p>
                    {lang === 'en'
                      ? `Your order #${savedReservation?.id} is registered. Our engineer will contact you via phone (${phone}) prior to visit.`
                      : `비회원 예약 번호 #${savedReservation?.id}와 입력하신 연락처(${phone})로 정상 접수되었습니다. 담당 기사 배정 후 방문 전 유선으로 사전 연락드립니다.`}
                  </p>
                </div>
              )}

              <div className="order-complete-actions">
                {token ? (
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
                <Link to={homePath} className="button secondary full-width">
                  {lang === 'en' ? 'Go to Home' : '메인 홈으로 이동'}
                </Link>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  )
}
