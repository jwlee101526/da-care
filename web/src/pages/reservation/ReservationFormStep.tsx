import { Check, ChevronRight } from 'lucide-react'
import { PhoneInput } from '../../components/PhoneInput'
import { useLanguage } from '../../context/LanguageContext'
import { DEVICE_CATEGORIES } from '../../lib/categories'
import {
  appendSymptom,
  getTomorrowDate,
  QUICK_SYMPTOMS_EN,
  QUICK_SYMPTOMS_KO,
  SYMPTOM_MAX_LENGTH,
  TIME_SLOTS,
  type ReservationFormValues,
  type UpdateReservationForm,
} from './reservationForm'

interface StepProps {
  form: ReservationFormValues
  update: UpdateReservationForm
}

/** 1단계: 기기, 증상, 방문 일정, 신청자 정보 입력. */
export function ReservationFormStep({ form, update, isMember, onSubmit }: StepProps & {
  isMember: boolean
  onSubmit: (event: React.FormEvent) => void
}) {
  const { lang } = useLanguage()
  return (
    <form className="order-step-form" onSubmit={onSubmit}>
      <DeviceSection form={form} update={update} />
      <SymptomSection form={form} update={update} />
      <div className="order-split-row">
        <ScheduleSection form={form} update={update} />
        <ContactSection form={form} update={update} isMember={isMember} />
      </div>

      <div className="order-action-bar">
        <button
          type="submit"
          className="button primary full-width order-primary-btn"
        >
          {lang === 'en' ? 'Review Reservation Details' : '예약 내용 확인하기'}
          <ChevronRight size={18} />
        </button>
      </div>
    </form>
  )
}

function SectionHeading({ number, title, description }: { number: number; title: string; description: string }) {
  return (
    <>
      <div className="order-section-title">
        <span className="order-step-badge">{number}</span>
        <h3>{title}</h3>
      </div>
      <p className="order-section-sub">{description}</p>
    </>
  )
}

function DeviceSection({ form, update }: StepProps) {
  const { lang } = useLanguage()
  return (
    <section className="order-section-box">
      <SectionHeading
        number={1}
        title={lang === 'en' ? 'Select Machine Category' : '수리 품목 선택 (12종)'}
        description={lang === 'en'
          ? 'Select the device or appliance that needs inspection and repair.'
          : '점검 및 수리가 필요한 전자제품 또는 가전 기기를 선택해 주세요.'}
      />

      <div className="category-select-grid">
        {DEVICE_CATEGORIES.map(cat => {
          const isSelected = form.device === cat.deviceType
          return (
            <button
              key={cat.deviceType}
              type="button"
              className={`category-select-card ${isSelected ? 'selected' : ''}`}
              onClick={() => update('device', cat.deviceType)}
            >
              <div className="cat-card-icon-wrap">
                <img src={cat.iconSrc} alt={cat.labelKo} width={36} height={36} />
              </div>
              <span className="cat-card-name">
                {lang === 'en' ? cat.labelEn : cat.labelKo}
              </span>
              <small className="cat-card-desc">
                {lang === 'en' ? cat.descEn : cat.descKo}
              </small>
              {isSelected && (
                <span className="cat-card-check">
                  <Check size={14} />
                </span>
              )}
            </button>
          )
        })}
      </div>
    </section>
  )
}

function SymptomSection({ form, update }: StepProps) {
  const { lang } = useLanguage()
  const quickSymptoms = lang === 'en' ? QUICK_SYMPTOMS_EN : QUICK_SYMPTOMS_KO
  return (
    <section className="order-section-box">
      <SectionHeading
        number={2}
        title={lang === 'en' ? 'Symptom & Description' : '고장 증상 및 요청 사항'}
        description={lang === 'en'
          ? 'Select common symptoms or describe the problem in detail.'
          : '자주 발생하는 증상을 클릭하시거나 구체적인 고장 증상을 적어주세요.'}
      />

      <div className="quick-tags-wrapper">
        <span className="quick-tags-label">{lang === 'en' ? 'Quick Add:' : '빠른 선택:'}</span>
        <div className="quick-tags-list">
          {quickSymptoms.map(s => (
            <button
              key={s}
              type="button"
              className={`quick-tag-chip ${form.symptom.includes(s) ? 'active' : ''}`}
              aria-pressed={form.symptom.includes(s)}
              onClick={() => update('symptom', appendSymptom(form.symptom, s))}
            >
              {s}
            </button>
          ))}
        </div>
      </div>

      <textarea
        className="order-textarea"
        rows={4}
        required
        maxLength={SYMPTOM_MAX_LENGTH}
        placeholder={lang === 'en' ? 'Describe symptoms, model name, or special requests...' : '기기 모델명이나 구체적인 고장 증상을 적어주시면 엔지니어가 부품을 미리 준비할 수 있습니다.'}
        value={form.symptom}
        onChange={e => update('symptom', e.target.value)}
      />
      <span className="order-textarea-count">{form.symptom.length} / {SYMPTOM_MAX_LENGTH}</span>
    </section>
  )
}

function ScheduleSection({ form, update }: StepProps) {
  const { lang } = useLanguage()
  return (
    <section className="order-section-box">
      <SectionHeading
        number={3}
        title={lang === 'en' ? 'Preferred Schedule' : '방문 희망 일정'}
        description={lang === 'en'
          ? 'Select the preferred visit date and time slot.'
          : '엔지니어의 방문을 희망하시는 날짜와 시간대를 선택해 주세요.'}
      />

      <div className="schedule-picker-row">
        <div className="schedule-date-col">
          <label htmlFor="visit-date">{lang === 'en' ? 'Preferred Date' : '방문 희망일'}</label>
          <input
            id="visit-date"
            type="date"
            className="order-input"
            required
            min={getTomorrowDate()}
            value={form.date}
            onChange={e => update('date', e.target.value)}
          />
        </div>
        <div className="schedule-time-col">
          <label htmlFor="visit-time">{lang === 'en' ? 'Preferred Time' : '방문 희망 시간'}</label>
          <div className="time-chips-grid">
            {TIME_SLOTS.map(slot => (
              <button
                key={slot}
                type="button"
                className={`time-chip ${form.time === slot ? 'active' : ''}`}
                onClick={() => update('time', slot)}
              >
                {slot}
              </button>
            ))}
          </div>
          <div className="time-custom-row">
            <span className="time-custom-label">{lang === 'en' ? 'Direct Time Pick:' : '직접 시간 지정:'}</span>
            <input
              id="visit-time"
              type="time"
              className="order-input time-custom-input"
              value={form.time}
              onChange={e => update('time', e.target.value)}
            />
          </div>
        </div>
      </div>
    </section>
  )
}

function ContactSection({ form, update, isMember }: StepProps & { isMember: boolean }) {
  const { lang } = useLanguage()
  return (
    <section className="order-section-box">
      <SectionHeading
        number={4}
        title={lang === 'en' ? 'Contact & Visit Location' : '신청 고객 및 방문 주소'}
        description={lang === 'en'
          ? 'Please enter contact information and exact address for the on-site technician.'
          : '엔지니어 방문 및 일정 안내를 위해 신청자 정보와 방문 주소를 정확히 입력해 주세요.'}
      />

      <div className="customer-fields-grid">
        <div className="customer-field full">
          <label htmlFor="client-name">{lang === 'en' ? 'Customer Name' : '성함 (신청자)'}</label>
          <input
            id="client-name"
            required
            className="order-input"
            placeholder={lang === 'en' ? 'e.g. John Doe' : '홍길동'}
            value={form.name}
            onChange={e => update('name', e.target.value)}
          />
        </div>
        <div className="customer-field full">
          <label htmlFor="client-phone">{lang === 'en' ? 'Mobile Number' : '휴대전화 번호'}</label>
          <PhoneInput
            id="client-phone"
            required
            className="order-input"
            value={form.phone}
            onChange={value => update('phone', value)}
          />
        </div>
        <div className="customer-field full">
          <label htmlFor="client-address">{lang === 'en' ? 'Visit Address' : '방문 주소'}</label>
          <input
            id="client-address"
            required
            className="order-input"
            placeholder={lang === 'en' ? 'Enter full address including apartment / room number' : '방문 받으실 상세 주소를 입력해 주세요 (동/호수 포함)'}
            value={form.address}
            onChange={e => update('address', e.target.value)}
          />
        </div>

        {!isMember && (
          <div className="customer-field full guest-privacy-box">
            <label className="guest-privacy-check">
              <input
                type="checkbox"
                checked={form.agreedPrivacy}
                onChange={e => update('agreedPrivacy', e.target.checked)}
                required
              />
              <span>
                {lang === 'en'
                  ? '[Required] I consent to personal contact & address collection for dispatching visit service.'
                  : '[필수] 전담 엔지니어 배정 및 방문 수리 서비스 제공을 위한 개인정보(성함, 휴대전화 번호, 주소) 수집·이용에 동의합니다.'}
              </span>
            </label>
          </div>
        )}
      </div>
    </section>
  )
}
