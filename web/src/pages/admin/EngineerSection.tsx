import { useState, type FormEvent } from 'react'
import { PhoneInput } from '../../components/PhoneInput'
import { api, ApiError, type Engineer } from '../../lib/api'
import { formatPhone, isValidPhone } from '../../lib/phone'
import type { AdminSectionProps } from './types'

/** 기사 등록·수정·삭제. */
export function EngineerSection({ engineers, token, reload, feedback }: AdminSectionProps & { engineers: Engineer[] }) {
  const [submitting, setSubmitting] = useState(false)
  const [engineerPhone, setEngineerPhone] = useState('')

  async function addEngineer(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const formElement = event.currentTarget
    const form = new FormData(formElement)
    setSubmitting(true)
    feedback.clear()

    if (!isValidPhone(engineerPhone)) {
      feedback.error('올바른 휴대전화 번호를 입력해 주세요.')
      setSubmitting(false)
      return
    }

    try {
      await api('/api/admin/engineers', {
        method: 'POST',
        body: JSON.stringify({ ...Object.fromEntries(form), phone: engineerPhone }),
      }, token)
    } catch (e) {
      feedback.error(e instanceof ApiError ? e.message : '기사 등록에 실패했습니다.')
      setSubmitting(false)
      return
    }

    formElement.reset()
    setEngineerPhone('')
    feedback.notice('기사가 등록되었습니다.')
    try {
      await reload()
    } catch {
      feedback.error('기사는 등록됐지만 목록을 새로고침하지 못했습니다. 페이지를 새로고침해 확인해 주세요.')
    } finally {
      setSubmitting(false)
    }
  }

  async function editEngineer(engineer: Engineer) {
    const name = window.prompt('이름', engineer.name)
    const phone = window.prompt('휴대전화 번호', formatPhone(engineer.phone))
    const specialty = window.prompt('전문 분야', engineer.specialty)
    const region = window.prompt('지역', engineer.region)
    if (!name || !phone || !specialty || !region) return
    try {
      await api(`/api/admin/engineers/${engineer.id}`, {
        method: 'PUT',
        body: JSON.stringify({ name, phone, specialty, region }),
      }, token)
      void reload()
    } catch (e) {
      feedback.error(e instanceof ApiError ? e.message : '기사 수정에 실패했습니다.')
    }
  }

  async function removeEngineer(id: number) {
    if (!window.confirm('기사를 삭제하시겠습니까?')) return
    try {
      await api(`/api/admin/engineers/${id}`, { method: 'DELETE' }, token)
      void reload()
    } catch (e) {
      feedback.error(e instanceof ApiError ? e.message : '기사 삭제에 실패했습니다.')
    }
  }

  return (
    <section>
      <h2>기사 등록</h2>
      <form className="inline-form" onSubmit={addEngineer}>
        <input required name="name" placeholder="이름" />
        <PhoneInput required aria-label="휴대전화 번호" value={engineerPhone} onChange={setEngineerPhone} />
        <input required name="specialty" placeholder="전문 분야" />
        <input required name="region" placeholder="지역" />
        <button className="button primary" disabled={submitting}>{submitting ? '등록 중...' : '등록'}</button>
      </form>
      <ul>
        {engineers.map(e => (
          <li key={e.id}>
            {e.name} · {formatPhone(e.phone)} · {e.specialty} · {e.region}{' '}
            <button onClick={() => editEngineer(e)}>수정</button>{' '}
            <button onClick={() => removeEngineer(e.id)}>삭제</button>
          </li>
        ))}
      </ul>
    </section>
  )
}
