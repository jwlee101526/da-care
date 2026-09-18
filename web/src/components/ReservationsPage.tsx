import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { Check, Clock, CheckCircle2, AlertCircle } from 'lucide-react'
import { useLanguage } from '../context/LanguageContext'
import { useAuth } from '../context/AuthContext'
import { api, ApiError } from '../lib/api'
import type { Reservation } from '../lib/api'
import { getCategoryInfo } from '../lib/categories'

interface ReservationsPageProps {
  onOpenReservation?: () => void
}

export function ReservationsPage({ onOpenReservation }: ReservationsPageProps) {
  const { lang } = useLanguage()
  const { token } = useAuth()

  const [items, setItems] = useState<Reservation[]>([])
  const [loading, setLoading] = useState(true)
  const [cancellingId, setCancellingId] = useState<number | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!token) return
    const controller = new AbortController()
    api<Reservation[]>('/api/reservations/me', { signal: controller.signal }, token)
      .then(setItems)
      .catch(error => {
        if (!controller.signal.aborted) setError(error instanceof ApiError ? error.message : '예약 내역을 불러오지 못했습니다.')
      })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [token])

  async function handleCancel(id: number) {
    if (!token || cancellingId !== null) return
    if (!window.confirm(lang === 'en' ? 'Cancel this reservation?' : '예약을 취소하시겠습니까?')) return
    setCancellingId(id)
    setError('')
    try {
      const reservation = await api<Reservation>(`/api/reservations/${id}/cancel`, { method: 'PATCH' }, token)
      setItems(previous => previous.map(item => item.id === id ? reservation : item))
    } catch (error) {
      setError(error instanceof ApiError ? error.message : '예약 취소 처리에 실패했습니다.')
    } finally {
      setCancellingId(null)
    }
  }

  const getDeviceLabel = (type: string) => getCategoryInfo(type, lang)

  function formatDateTime(isoString?: string | null) {
    if (!isoString) return '-'
    const clean = isoString.replace('T', ' ')
    return clean.length > 16 ? clean.slice(0, 16) : clean
  }

  return (
    <div className="order-details-view">
      <main className="order-main-container">
        <div className="order-page-header">
          <div>
            <h1 className="order-page-title">{lang === 'en' ? 'Service Reservations' : '예약 내역 조회'}</h1>
            <p className="order-page-desc">
              {lang === 'en'
                ? 'Check the status and details of your repair reservations.'
                : '신청하신 수리 서비스의 진행 상태와 방문 일정을 확인할 수 있습니다.'}
            </p>
          </div>
          {onOpenReservation && (
            <button type="button" className="button primary compact" onClick={onOpenReservation}>
              {lang === 'en' ? 'New Reservation' : '새 예약 신청'}
            </button>
          )}
        </div>

        {error && (
          <div className="order-alert-error">
            <AlertCircle size={18} />
            <span>{error}</span>
          </div>
        )}

        {!token ? (
          <div className="order-empty-card">
            <h3>{lang === 'en' ? 'Sign in Required' : '로그인이 필요합니다'}</h3>
            <p>{lang === 'en' ? 'Please log in to view your reservation details.' : '예약 내역을 확인하려면 먼저 로그인해 주세요.'}</p>
            <Link className="button primary" to="/login" style={{ marginTop: '20px' }}>
              {lang === 'en' ? 'Log In' : '로그인'}
            </Link>
          </div>
        ) : loading ? (
          <div className="order-empty-card">
            <p>{lang === 'en' ? 'Loading reservation details...' : '예약 내역을 불러오는 중입니다...'}</p>
          </div>
        ) : error && items.length === 0 ? null : items.length === 0 ? (
          <div className="order-empty-card">
            <h3>{lang === 'en' ? 'No reservations found' : '조회된 예약 내역이 없습니다.'}</h3>
            <p>
              {lang === 'en'
                ? 'Book a repair service to view your live reservation details here.'
                : '다케어 서비스를 예약하시면 이곳에서 저장된 수리 진행 현황을 확인하실 수 있습니다.'}
            </p>
            {onOpenReservation ? (
              <button type="button" className="button primary" onClick={onOpenReservation} style={{ marginTop: '16px' }}>
                {lang === 'en' ? 'Book a Service' : '서비스 예약 신청하기'}
              </button>
            ) : (
              <Link to="/" className="button primary" style={{ marginTop: '16px' }}>
                {lang === 'en' ? 'Go to Home' : '홈으로 이동하기'}
              </Link>
            )}
          </div>
        ) : (
          <div className="order-list-column">
            {items.map(item => {
              const currentStep = item.status === 'PENDING' ? 1 : item.status === 'CONFIRMED' ? 2 : item.status === 'COMPLETED' ? 3 : 0

              return (
                <section className="order-card" key={item.id}>
                  {/* Card Header: Order ID */}
                  <div className="card-header-row">
                    <div>
                      <span className="order-number-label">
                        {lang === 'en' ? 'Order' : '예약 번호'} #{item.id}
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
                      <span className="value font-medium">{getDeviceLabel(item.deviceType)}</span>
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
                  {item.status === 'PENDING' && (
                    <div className="card-footer-actions">
                      <button
                        type="button"
                        className="button secondary compact cancel-btn"
                        onClick={() => handleCancel(item.id)}
                        disabled={cancellingId !== null}
                      >
                        {cancellingId === item.id ? (lang === 'en' ? 'Cancelling...' : '취소 중...') : (lang === 'en' ? 'Cancel Reservation' : '예약 취소')}
                      </button>
                    </div>
                  )}
                </section>
              )
            })}
          </div>
        )}
      </main>
    </div>
  )
}
