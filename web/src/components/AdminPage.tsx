import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { api, ApiError } from '../lib/api'
import type { Engineer, Reservation, UsageItem } from '../lib/api'
import { useDailyUsage } from '../hooks/useDailyUsage'
import { notifyUsage } from '../lib/usageToast'

const SMS_USAGE_TEXT = {
  used: (used: number, limit: number) => `오늘 SMS ${used}/${limit}회 사용`,
  remaining: (remaining: number) => `남은 발송 가능 횟수는 ${remaining}회입니다.`,
  exhausted: 'SMS 일일 한도를 모두 사용했습니다. 이후 확정 건은 문자가 발송되지 않습니다.',
}

function UsageMeter({ label, item }: { label: string; item: UsageItem }) {
  const percent = item.limit > 0 ? Math.min(100, Math.round(item.used / item.limit * 100)) : 100
  const level = item.remaining === 0 ? ' exhausted' : percent >= 80 ? ' warning' : ''
  return <div className={'usage-meter' + level}><div className="usage-meter-label"><span>{label}</span><strong>{item.used} / {item.limit}</strong></div><div className="usage-meter-track" role="progressbar" aria-label={label} aria-valuemin={0} aria-valuemax={item.limit} aria-valuenow={item.used}><div style={{ width: `${percent}%` }} /></div></div>
}

export function AdminPage() {
  const { token, logout } = useAuth(); const [engineers, setEngineers] = useState<Engineer[]>([]); const [reservations, setReservations] = useState<Reservation[]>([]); const [error, setError] = useState(''); const [notice, setNotice] = useState(''); const [submitting, setSubmitting] = useState(false); const { usage, refresh: refreshUsage } = useDailyUsage()
  const load = useCallback(() => Promise.all([api<Engineer[]>('/api/admin/engineers', {}, token), api<Reservation[]>('/api/admin/reservations', {}, token)])
    .then(([e, r]) => {
      setEngineers(e)
      setReservations(r)
      setError('')
    })
    .catch(e => setError(e instanceof ApiError ? e.message : '관리자 정보를 불러오지 못했습니다.')), [token])
  useEffect(() => { void load() }, [load])
  async function addEngineer(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const phone = String(form.get('phone') ?? '').replace(/\D/g, '')
    setSubmitting(true)
    setError('')
    setNotice('')

    if (!/^0\d{8,10}$/.test(phone)) {
      setError('연락처를 010-1234-5678 형식으로 입력해 주세요.')
      setSubmitting(false)
      return
    }

    try {
      await api('/api/admin/engineers', {
        method: 'POST',
        body: JSON.stringify({ ...Object.fromEntries(form), phone }),
      }, token)
    } catch (e) {
      setError(e instanceof ApiError ? e.message : '기사 등록에 실패했습니다.')
      setSubmitting(false)
      return
    }

    event.currentTarget.reset()
    setNotice('기사가 등록되었습니다.')
    try {
      await load()
    } catch {
      setError('기사는 등록됐지만 목록을 새로고침하지 못했습니다. 페이지를 새로고침해 확인해 주세요.')
    } finally {
      setSubmitting(false)
    }
  }
  async function confirm(id: number, form: HTMLFormElement) {
    const data = new FormData(form)
    setError('')
    setNotice('')
    try {
      await api(`/api/admin/reservations/${id}/confirmation`, { method: 'PATCH', body: JSON.stringify({ engineerId: Number(data.get('engineerId')), confirmedAt: data.get('confirmedAt') }) }, token)
      setNotice('기사 배정과 방문 일정 확정이 완료되었습니다. 고객에게 SMS 발송을 요청했습니다.')
      const nextUsage = await refreshUsage()
      if (nextUsage) notifyUsage(nextUsage.sms, SMS_USAGE_TEXT)
      await load()
    } catch (e) {
      setError(e instanceof ApiError ? e.message : '예약 확정에 실패했습니다.')
    }
  }
  async function change(id: number, action: 'cancel' | 'complete') {
    setError('')
    setNotice('')
    try {
      await api(`/api/admin/reservations/${id}/${action}`, { method: 'PATCH' }, token)
      setNotice(action === 'complete' ? '수리 완료 처리되었습니다.' : '예약이 취소되었습니다.')
      await load()
    } catch (e) {
      setError(e instanceof ApiError ? e.message : '상태 변경에 실패했습니다.')
    }
  }
  async function removeEngineer(id: number) { if (!window.confirm('기사를 삭제하시겠습니까?')) return; try { await api(`/api/admin/engineers/${id}`, { method: 'DELETE' }, token); void load() } catch (e) { setError(e instanceof ApiError ? e.message : '기사 삭제에 실패했습니다.') } }
  async function editEngineer(engineer: Engineer) { const name = window.prompt('이름', engineer.name); const phone = window.prompt('연락처', engineer.phone); const specialty = window.prompt('전문 분야', engineer.specialty); const region = window.prompt('지역', engineer.region); if (!name || !phone || !specialty || !region) return; try { await api(`/api/admin/engineers/${engineer.id}`, { method: 'PUT', body: JSON.stringify({ name, phone, specialty, region }) }, token); void load() } catch (e) { setError(e instanceof ApiError ? e.message : '기사 수정에 실패했습니다.') } }
  return <main className="dashboard"><header><Link to="/">DA-CARE 관리자</Link><button onClick={logout}>로그아웃</button></header><h1>예약 운영</h1>{error && <p className="form-error" role="alert">{error}</p>}{notice && <p className="form-success" role="status">{notice}</p>}{usage && <section className="usage-summary"><h2>오늘 사용량</h2><p>유료 API의 서비스 전체 하루 한도입니다. 한국 시간 자정에 초기화됩니다.</p><UsageMeter label="AI 상담" item={usage.diagnosis} /><UsageMeter label="SMS 발송" item={usage.sms} /></section>}<section><h2>기사 등록</h2><form className="inline-form" onSubmit={addEngineer}><input required name="name" placeholder="이름" /><input required name="phone" type="tel" placeholder="연락처" pattern="[0-9-]{9,13}" /><input required name="specialty" placeholder="전문 분야" /><input required name="region" placeholder="지역" /><button className="button primary" disabled={submitting}>{submitting ? '등록 중...' : '등록'}</button></form><ul>{engineers.map(e => <li key={e.id}>{e.name} · {e.specialty} · {e.region} <button onClick={() => editEngineer(e)}>수정</button> <button onClick={() => removeEngineer(e.id)}>삭제</button></li>)}</ul></section><section><h2>예약 목록</h2>{reservations.map(r => <article className="admin-reservation" key={r.id}><strong>#{r.id} {r.deviceType} · {r.status}</strong><p>{r.symptomDescription} / {r.visitAddress}</p><p>희망: {r.preferredAt.replace('T', ' ')}</p>{r.status === 'PENDING' && <form className="inline-form" onSubmit={e => { e.preventDefault(); void confirm(r.id, e.currentTarget) }}><select required name="engineerId" defaultValue=""><option value="" disabled>기사 선택</option>{engineers.map(x => <option value={x.id} key={x.id}>{x.name}</option>)}</select><input required name="confirmedAt" type="datetime-local" /><button className="button primary">확정</button><button type="button" className="button secondary" onClick={() => change(r.id, 'cancel')}>취소</button></form>}{r.status === 'CONFIRMED' && <><button className="button primary" onClick={() => change(r.id, 'complete')}>완료 처리</button> <button className="button secondary" onClick={() => change(r.id, 'cancel')}>취소</button></>}</article>)}</section></main>
}
