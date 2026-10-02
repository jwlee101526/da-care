import { api, ApiError, type Engineer, type Reservation } from '../../lib/api'
import { formatPhone } from '../../lib/phone'
import { formatReservationCode } from '../../lib/reservationCode'
import type { AdminSectionProps } from './types'
import { buttonVariants } from '../../components/ui/button'
import { cn } from '@/lib/utils'
import { adminSection, adminSectionTitle, inlineForm, inlineInput } from './adminStyles'

const detail = 'mb-1.5 text-[14px] text-ink-subtle'

/** 예약 목록과 상태별 처리(확정·완료·취소). */
export function ReservationSection({ reservations, engineers, token, reload, feedback, onConfirmed }: AdminSectionProps & {
  reservations: Reservation[]
  engineers: Engineer[]
  onConfirmed: () => Promise<void>
}) {
  async function confirm(id: number, form: HTMLFormElement) {
    const data = new FormData(form)
    feedback.clear()
    try {
      await api(`/api/admin/reservations/${id}/confirmation`, {
        method: 'PATCH',
        body: JSON.stringify({ engineerId: Number(data.get('engineerId')), confirmedAt: data.get('confirmedAt') }),
      }, token)
      feedback.notice('기사 배정과 방문 일정 확정이 완료되었습니다. 고객에게 SMS 발송을 요청했습니다.')
      await onConfirmed()
      await reload()
    } catch (e) {
      feedback.error(e instanceof ApiError ? e.message : '예약 확정에 실패했습니다.')
    }
  }

  async function change(id: number, action: 'cancel' | 'complete') {
    feedback.clear()
    try {
      await api(`/api/admin/reservations/${id}/${action}`, { method: 'PATCH' }, token)
      feedback.notice(action === 'complete' ? '수리 완료 처리되었습니다.' : '예약이 취소되었습니다.')
      await reload()
    } catch (e) {
      feedback.error(e instanceof ApiError ? e.message : '상태 변경에 실패했습니다.')
    }
  }

  return (
    <section className={adminSection}>
      <h2 className={adminSectionTitle}>예약 목록</h2>
      {reservations.map(r => (
        <article className="mb-4 rounded-xl border border-line-default bg-white p-5" key={r.id}>
          <strong className="mb-2 block text-[16px] text-navy">{formatReservationCode(r.code)} {r.deviceType} · {r.status}</strong>
          <p className={detail}>{r.symptomDescription} / {r.visitAddress}</p>
          {r.contactName && <p className={detail}>고객: {r.contactName}{r.contactPhone && ` · ${formatPhone(r.contactPhone)}`}</p>}
          <p className={detail}>희망: {r.preferredAt.replace('T', ' ')}</p>
          {r.status === 'PENDING' && (
            <form className={cn(inlineForm, 'mt-3.5 mb-0')} onSubmit={e => { e.preventDefault(); void confirm(r.id, e.currentTarget) }}>
              <select className={inlineInput} required name="engineerId" defaultValue="">
                <option value="" disabled>기사 선택</option>
                {engineers.map(x => <option value={x.id} key={x.id}>{x.name}</option>)}
              </select>
              <input className={inlineInput} required name="confirmedAt" type="datetime-local" />
              <button className={buttonVariants({ variant: 'primary' })}>확정</button>
              <button type="button" className={buttonVariants({ variant: 'secondary' })} onClick={() => change(r.id, 'cancel')}>취소</button>
            </form>
          )}
          {r.status === 'CONFIRMED' && (
            <>
              <button className={buttonVariants({ variant: 'primary' })} onClick={() => change(r.id, 'complete')}>완료 처리</button>{' '}
              <button className={buttonVariants({ variant: 'secondary' })} onClick={() => change(r.id, 'cancel')}>취소</button>
            </>
          )}
        </article>
      ))}
    </section>
  )
}
