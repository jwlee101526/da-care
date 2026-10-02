import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useLanguage } from '../context/LanguageContext'
import { useAuth } from '../context/AuthContext'
import { api, ApiError } from '../lib/api'
import type { Reservation } from '../lib/api'
import { ReservationCard } from '../components/ReservationCard'
import { OrderPageLayout } from '../components/OrderPageLayout'
import { ErrorAlert } from '../components/ErrorAlert'
import { buttonVariants } from '../components/ui/button'

const emptyCard = 'rounded-2xl border border-line-default bg-white px-8 py-16 text-center shadow-[0_4px_16px_rgba(24,41,70,0.04)]'
const emptyTitle = 'mb-2.5 text-[20px] font-[650] text-ink'
const emptyText = 'mx-auto my-0 max-w-[420px] text-[14px] leading-[1.6] text-ink-subtle'

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
    <OrderPageLayout
      title={lang === 'en' ? 'Service Reservations' : '예약 내역 조회'}
      description={lang === 'en'
        ? 'Check the status and details of your repair reservations.'
        : '신청하신 수리 서비스의 진행 상태와 방문 일정을 확인할 수 있습니다.'}
      action={onOpenReservation && (
        <button type="button" className={buttonVariants({ variant: 'primary', size: 'compact' })} onClick={onOpenReservation}>
          {lang === 'en' ? 'New Reservation' : '새 예약 신청'}
        </button>
      )}
    >
      {error && <ErrorAlert>{error}</ErrorAlert>}

      {!token ? (
        <div className={emptyCard}>
          <h3 className={emptyTitle}>{lang === 'en' ? 'Sign in Required' : '로그인이 필요합니다'}</h3>
          <p className={emptyText}>{lang === 'en' ? 'Please log in to view your reservation details.' : '예약 내역을 확인하려면 먼저 로그인해 주세요.'}</p>
          <Link className={buttonVariants({ variant: 'primary', className: 'mt-5' })} to="/login">
            {lang === 'en' ? 'Log In' : '로그인'}
          </Link>
        </div>
      ) : loading ? (
        <div className={emptyCard}>
          <p className={emptyText}>{lang === 'en' ? 'Loading reservation details...' : '예약 내역을 불러오는 중입니다...'}</p>
        </div>
      ) : error && items.length === 0 ? null : items.length === 0 ? (
        <div className={emptyCard}>
          <h3 className={emptyTitle}>{lang === 'en' ? 'No reservations found' : '조회된 예약 내역이 없습니다.'}</h3>
          <p className={emptyText}>
            {lang === 'en'
              ? 'Book a repair service to view your live reservation details here.'
              : '다케어 서비스를 예약하시면 이곳에서 저장된 수리 진행 현황을 확인하실 수 있습니다.'}
          </p>
          {onOpenReservation ? (
            <button type="button" className={buttonVariants({ variant: 'primary', className: 'mt-4' })} onClick={onOpenReservation}>
              {lang === 'en' ? 'Book a Service' : '서비스 예약 신청하기'}
            </button>
          ) : (
            <Link to="/" className={buttonVariants({ variant: 'primary', className: 'mt-4' })}>
              {lang === 'en' ? 'Go to Home' : '홈으로 이동하기'}
            </Link>
          )}
        </div>
      ) : (
        <div className="flex flex-col gap-6">
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
    </OrderPageLayout>
  )
}
