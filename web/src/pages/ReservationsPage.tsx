import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { AlertCircle } from 'lucide-react'
import { useLanguage } from '../context/LanguageContext'
import { useAuth } from '../context/AuthContext'
import { api, ApiError } from '../lib/api'
import type { Reservation } from '../lib/api'
import { ReservationCard } from '../components/ReservationCard'

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
            {items.map(item => (
              <ReservationCard
                key={item.id}
                item={item}
                onCancel={() => handleCancel(item.id)}
                cancelling={cancellingId === item.id}
                cancelDisabled={cancellingId !== null}
              />
            ))}
          </div>
        )}
      </main>
    </div>
  )
}
