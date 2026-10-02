import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useUsage } from '../../hooks/useUsage'
import { api, ApiError, fetchTotalUsage } from '../../lib/api'
import type { Engineer, Reservation } from '../../lib/api'
import { notifyUsage, type UsageToastText } from '../../lib/usageToast'
import { EngineerSection } from './EngineerSection'
import { ReservationSection } from './ReservationSection'
import { UsageSummary } from './UsageSummary'
import type { AdminFeedback } from './types'
import './AdminPage.css'

const SMS_USAGE_TEXT: UsageToastText = {
  used: (used: number, limit: number) => `이번 주 SMS ${used}/${limit}회 사용`,
  remaining: (remaining: number) => `남은 발송 가능 횟수는 ${remaining}회입니다.`,
  exhausted: 'SMS 주간 한도를 모두 사용했습니다. 이후 확정 건은 문자가 발송되지 않습니다.',
}

export function AdminPage() {
  const { token, logout } = useAuth()
  const [engineers, setEngineers] = useState<Engineer[]>([])
  const [reservations, setReservations] = useState<Reservation[]>([])
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const { usage, refresh: refreshUsage } = useUsage(useCallback(() => fetchTotalUsage(token), [token]))

  const load = useCallback(() => Promise.all([
    api<Engineer[]>('/api/admin/engineers', {}, token),
    api<Reservation[]>('/api/admin/reservations', {}, token),
  ])
    .then(([e, r]) => {
      setEngineers(e)
      setReservations(r)
      setError('')
    })
    .catch(e => setError(e instanceof ApiError ? e.message : '관리자 정보를 불러오지 못했습니다.')), [token])

  useEffect(() => { void load() }, [load])

  const feedback: AdminFeedback = {
    error: setError,
    notice: setNotice,
    clear: () => {
      setError('')
      setNotice('')
    },
  }

  // 예약을 확정하면 SMS가 발송되므로 이번 주 SMS 사용량을 다시 조회해 알린다.
  const notifySmsUsage = async () => {
    const nextUsage = await refreshUsage()
    if (nextUsage) notifyUsage(nextUsage.sms, SMS_USAGE_TEXT)
  }

  return (
    <main className="dashboard">
      <header>
        <Link to="/">DA-CARE 관리자</Link>
        <button onClick={logout}>로그아웃</button>
      </header>
      <h1>예약 운영</h1>
      {error && <p className="form-error" role="alert">{error}</p>}
      {notice && <p className="form-success" role="status">{notice}</p>}
      {usage && <UsageSummary usage={usage} />}
      <EngineerSection engineers={engineers} token={token} reload={load} feedback={feedback} />
      <ReservationSection
        reservations={reservations}
        engineers={engineers}
        token={token}
        reload={load}
        feedback={feedback}
        onConfirmed={notifySmsUsage}
      />
    </main>
  )
}
