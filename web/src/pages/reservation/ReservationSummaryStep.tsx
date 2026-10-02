import { Clock } from 'lucide-react'
import { useLanguage } from '../../context/LanguageContext'
import type { CategoryDefinition } from '../../lib/categories'
import { formatPhone } from '../../lib/phone'
import type { ReservationFormValues } from './reservationForm'

/** 2단계: 입력한 내용을 확인하고 접수한다. */
export function ReservationSummaryStep({ form, category, isMember, submitting, onEdit, onConfirm }: {
  form: ReservationFormValues
  category: CategoryDefinition
  isMember: boolean
  submitting: boolean
  onEdit: () => void
  onConfirm: () => void
}) {
  const { lang } = useLanguage()
  const categoryLabel = lang === 'en' ? category.labelEn : category.labelKo
  return (
    <div className="order-summary-container">
      <div className="order-summary-main-card">
        <div className="summary-banner">
          <div className="summary-banner-icon">
            <img src={category.iconSrc} alt={category.labelKo} width={34} height={34} />
          </div>
          <div className="summary-banner-text">
            <h3>{categoryLabel}</h3>
            <p className="summary-banner-time">
              <Clock size={15} />
              <span>{form.date} {form.time}</span>
            </p>
          </div>
        </div>

        <div className="summary-sections-grid">
          <div className="summary-section">
            <h4 className="summary-section-title">{lang === 'en' ? 'Customer Information' : '고객 정보'}</h4>
            <div className="summary-kv-list">
              <SummaryItem label={lang === 'en' ? 'Name' : '성함'} valueClassName="font-bold">{form.name}</SummaryItem>
              <SummaryItem label={lang === 'en' ? 'Mobile' : '휴대전화 번호'}>{formatPhone(form.phone)}</SummaryItem>
              <SummaryItem label={lang === 'en' ? 'Address' : '방문 주소'} valueClassName="address">{form.address}</SummaryItem>
              <SummaryItem label={lang === 'en' ? 'Booking Type' : '접수 유형'} valueClassName="font-bold text-brand">
                {isMember ? (lang === 'en' ? 'Registered Member' : '회원 예약') : (lang === 'en' ? 'Guest (Non-member)' : '비회원 간편 접수')}
              </SummaryItem>
            </div>
          </div>

          <div className="summary-section">
            <h4 className="summary-section-title">{lang === 'en' ? 'Repair Details & Symptom' : '수리 품목 및 증상'}</h4>
            <div className="summary-kv-list">
              <SummaryItem label={lang === 'en' ? 'Category' : '품목'} valueClassName="font-bold">{categoryLabel}</SummaryItem>
              <SummaryItem label={lang === 'en' ? 'Schedule' : '희망 일시'} valueClassName="font-bold text-brand">{form.date} {form.time}</SummaryItem>
              <div className="summary-memo-block">
                <span className="kv-label">{lang === 'en' ? 'Symptom Note' : '증상 메모'}</span>
                <p className="summary-memo-content">{form.symptom}</p>
              </div>
            </div>
          </div>
        </div>

        <div className="summary-actions">
          <button
            type="button"
            className="button secondary summary-back-btn"
            onClick={onEdit}
            disabled={submitting}
          >
            {lang === 'en' ? 'Edit Details' : '내용 수정하기'}
          </button>
          <button
            type="button"
            className="button primary summary-confirm-btn"
            onClick={onConfirm}
            disabled={submitting}
          >
            {submitting
              ? (lang === 'en' ? 'Processing...' : '접수 진행 중...')
              : (lang === 'en' ? 'Confirm & Submit Reservation' : '예약 접수 완료하기')}
          </button>
        </div>
      </div>
    </div>
  )
}

function SummaryItem({ label, valueClassName, children }: { label: string; valueClassName?: string; children: React.ReactNode }) {
  return (
    <div className="summary-kv-item">
      <span className="kv-label">{label}</span>
      <span className={valueClassName ? `kv-val ${valueClassName}` : 'kv-val'}>{children}</span>
    </div>
  )
}
