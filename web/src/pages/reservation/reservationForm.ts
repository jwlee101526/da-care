import type { Language } from '../../context/LanguageContext'
import { isValidPhone } from '../../lib/phone'
import type { DeviceType, ReservationSelection } from '../../types'

export const QUICK_SYMPTOMS_KO = [
  '전원이 켜지지 않음',
  '이상 소음 / 진동 발생',
  '화면 / 디스플레이 출력 불량',
  '발열 및 자동 꺼짐 현상',
  '주요 기능 작동 멈춤',
  '소모품 및 부품 교체 희망',
]

export const QUICK_SYMPTOMS_EN = [
  'Will not power on',
  'Abnormal noise / vibration',
  'Screen / display failure',
  'Overheating & shutdown',
  'Key functions unresponsive',
  'Part replacement requested',
]

export const SYMPTOM_MAX_LENGTH = 2000

export const TIME_SLOTS = [
  '09:00',
  '10:30',
  '13:00',
  '14:30',
  '16:00',
  '17:30',
]

export type ReservationStep = 1 | 2 | 3

export interface ReservationFormValues {
  device: DeviceType
  symptom: string
  date: string
  time: string
  name: string
  phone: string
  address: string
  agreedPrivacy: boolean
}

export type UpdateReservationForm = <K extends keyof ReservationFormValues>(
  field: K,
  value: ReservationFormValues[K],
) => void

export function getTomorrowDate(): string {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  return [
    tomorrow.getFullYear(),
    String(tomorrow.getMonth() + 1).padStart(2, '0'),
    String(tomorrow.getDate()).padStart(2, '0'),
  ].join('-')
}

export function initialReservationForm(selection: ReservationSelection | null): ReservationFormValues {
  return {
    device: selection?.device || 'computer',
    symptom: selection?.symptom || '',
    date: getTomorrowDate(),
    time: '10:30',
    name: '',
    phone: '',
    address: '',
    agreedPrivacy: true,
  }
}

/** 빠른 선택 증상을 기존 입력 뒤에 덧붙인다. 이미 들어 있으면 그대로 둔다. */
export function appendSymptom(current: string, text: string) {
  if (!current.trim()) return text
  if (current.includes(text)) return current
  return `${current}, ${text}`
}

/** 입력 단계에서 다음으로 넘어갈 수 없는 이유. 문제가 없으면 빈 문자열을 돌려준다. */
export function validateReservationForm(form: ReservationFormValues, lang: Language, isMember: boolean): string {
  const en = lang === 'en'
  if (!form.device) return en ? 'Please select a device category.' : '수리할 기기 품목을 선택해 주세요.'
  if (!form.symptom.trim()) return en ? 'Please describe the symptom.' : '고장 증상을 입력해 주세요.'
  if (!form.date) return en ? 'Please select a preferred date.' : '방문 희망 일자를 선택해 주세요.'
  if (!form.name.trim()) return en ? 'Please enter your name.' : '신청자 성함을 입력해 주세요.'
  if (!form.phone.trim()) return en ? 'Please enter your mobile number.' : '휴대전화 번호를 입력해 주세요.'
  if (!isValidPhone(form.phone)) return en ? 'Enter a valid mobile number.' : '올바른 휴대전화 번호를 입력해 주세요.'
  if (!form.address.trim()) return en ? 'Please enter the visit address.' : '방문 주소를 입력해 주세요.'
  if (!isMember && !form.agreedPrivacy) {
    return en ? 'Please agree to personal data collection for on-site service.' : '방문 수리 서비스 제공을 위한 개인정보 수집에 동의해 주세요.'
  }
  return ''
}

export function toReservationRequest(form: ReservationFormValues) {
  return {
    deviceType: form.device,
    symptomDescription: form.symptom,
    visitAddress: form.address,
    preferredAt: `${form.date}T${form.time}:00`,
    contactName: form.name,
    contactPhone: form.phone,
  }
}
