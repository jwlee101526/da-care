import { Check, Clock, CheckCircle2, AlertCircle, Bell } from 'lucide-react'
import { useLanguage } from '../context/LanguageContext'
import type { GuestReservation } from '../lib/api'
import { formatReservationCode } from '../lib/reservationCode'
import { getCategoryInfo } from '../lib/categories'

interface ReservationCardProps {
  item: GuestReservation
  onCancel?: () => void
  cancelling?: boolean
  cancelDisabled?: boolean
}

function formatDateTime(isoString?: string | null) {
  if (!isoString) return '-'
  const clean = isoString.replace('T', ' ')
  return clean.length > 16 ? clean.slice(0, 16) : clean
}

// 예약 한 건의 진행 단계와 상세 내용. 회원 예약 내역과 비회원 예약 조회 화면이 함께 쓴다.
export function ReservationCard({ item, onCancel, cancelling = false, cancelDisabled = false }: ReservationCardProps) {
  const { lang } = useLanguage()
  const currentStep = item.status === 'PENDING' ? 1 : item.status === 'CONFIRMED' ? 2 : item.status === 'COMPLETED' ? 3 : 0

  return (
    <section className="order-card">
      {/* Card Header: Order ID */}
      <div className="card-header-row">
        <div>
          <span className="order-number-label">
            {lang === 'en' ? 'Order' : '예약 번호'} {formatReservationCode(item.code)}
          </span>
        </div>
      </div>

      {/* Stepper Bar (style01-3 inspired, clean & no emojis) */}
      {item.status === 'CANCELLED' ? (
        <div className="cancelled-box">
          <AlertCircle size={20} />
          <div>
            <h4>{lang === 'en' ? 'Order Cancelled' : '취소된 예약입니다.'}</h4>
            <p>{lang === 'en' ? 'This reservation has been cancelled.' : '해당 예약은 취소 처리되었습니다.'}</p>
          </div>
        </div>
      ) : (
        <div className="progress-stepper">
          <div className="stepper-track">
            <div
              className="stepper-fill"
              style={{
                width: currentStep === 1 ? '0%' : currentStep === 2 ? '50%' : '100%',
              }}
            />
          </div>
          <div className="stepper-steps">
            <div className={`step-item ${currentStep >= 1 ? 'completed' : ''} ${currentStep === 1 ? 'active' : ''}`}>
              <div className="step-icon-wrapper">
                {currentStep > 1 ? <Check size={16} /> : <Clock size={16} />}
              </div>
              <span className="step-title">{lang === 'en' ? 'Received' : '접수 완료'}</span>
            </div>

            <div className={`step-item ${currentStep >= 2 ? 'completed' : ''} ${currentStep === 2 ? 'active' : ''}`}>
              <div className="step-icon-wrapper">
                {currentStep > 2 ? <Check size={16} /> : <Clock size={16} />}
              </div>
              <span className="step-title">{lang === 'en' ? 'Confirmed' : '예약 확정'}</span>
            </div>

            <div className={`step-item ${currentStep >= 3 ? 'completed' : ''} ${currentStep === 3 ? 'active' : ''}`}>
              <div className="step-icon-wrapper">
                <CheckCircle2 size={16} />
              </div>
              <span className="step-title">{lang === 'en' ? 'Completed' : '수리 완료'}</span>
            </div>
          </div>
        </div>
      )}

      {/* Detail Key-Values */}
      <div className="detail-rows">
        <div className="detail-row">
          <span className="label">{lang === 'en' ? 'Device' : '수리 품목'}</span>
          <span className="value font-medium">{getCategoryInfo(item.deviceType, lang)}</span>
        </div>

        <div className="detail-row">
          <span className="label">{lang === 'en' ? 'Preferred Schedule' : '방문 희망 일시'}</span>
          <span className="value">{formatDateTime(item.preferredAt)}</span>
        </div>

        {item.confirmedAt && (
          <div className="detail-row">
            <span className="label">{lang === 'en' ? 'Confirmed Schedule' : '확정 방문 일시'}</span>
            <span className="value font-medium">{formatDateTime(item.confirmedAt)}</span>
          </div>
        )}

        <div className="detail-row">
          <span className="label">{lang === 'en' ? 'Address' : '방문 주소'}</span>
          <span className="value address-val">{item.visitAddress}</span>
        </div>

        <div className="detail-row">
          <span className="label">{lang === 'en' ? 'Technician' : '담당 엔지니어'}</span>
          <span className="value font-medium">
            {item.engineerName ? item.engineerName : (lang === 'en' ? 'Pending Assignment' : '배정 진행 중')}
          </span>
        </div>
      </div>

      {item.status === 'CONFIRMED' && (
        <section className="reservation-confirmed-notice" aria-label={lang === 'en' ? 'Reservation notification' : '예약 알림 안내'}>
          <Bell size={18} aria-hidden="true" />
          <p>{lang === 'en' ? 'Your technician assignment and confirmed visit time will be delivered to your registered mobile number by SMS.' : '담당 기사 배정과 확정 방문 일시는 등록된 휴대전화 번호로 문자 안내를 받게 됩니다.'}</p>
        </section>
      )}

      {/* Symptom Note */}
      {item.symptomDescription && (
        <div className="memo-box">
          <div className="memo-header">
            <span>{lang === 'en' ? 'Symptom Description' : '고장 증상 및 접수 내용'}</span>
          </div>
          <p className="memo-text">{item.symptomDescription}</p>
        </div>
      )}

      {/* Bottom Action */}
      {item.status === 'PENDING' && onCancel && (
        <div className="card-footer-actions">
          <button
            type="button"
            className="button secondary compact cancel-btn"
            onClick={onCancel}
            disabled={cancelDisabled}
          >
            {cancelling ? (lang === 'en' ? 'Cancelling...' : '취소 중...') : (lang === 'en' ? 'Cancel Reservation' : '예약 취소')}
          </button>
        </div>
      )}
    </section>
  )
}
