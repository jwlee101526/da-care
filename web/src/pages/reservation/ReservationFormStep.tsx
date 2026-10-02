import { Check, ChevronRight } from 'lucide-react'
import { PhoneInput } from '../../components/PhoneInput'
import { useLanguage } from '../../context/LanguageContext'
import { DEVICE_CATEGORIES } from '../../lib/categories'
import { cn } from '@/lib/utils'
import { buttonVariants } from '../../components/ui/button'
import { actionBar, fieldLabel, fieldsGrid, fullField, orderInput, orderSectionBox } from '../../components/orderForm'
import { stepBadge } from './stepBadge'
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
    <form onSubmit={onSubmit}>
      <DeviceSection form={form} update={update} />
      <SymptomSection form={form} update={update} />
      <div className="mb-5 grid grid-cols-[1fr_1fr] items-stretch gap-5 max-[860px]:grid-cols-[1fr] max-[860px]:gap-4">
        <ScheduleSection form={form} update={update} />
        <ContactSection form={form} update={update} isMember={isMember} />
      </div>

      <div className={actionBar}>
        <button
          type="submit"
          className={buttonVariants({
            variant: 'primary',
            className: 'h-[52px] w-full cursor-pointer gap-2 rounded-xl text-[16px] leading-[1.4] font-[650] shadow-[0_4px_14px_rgba(36,87,214,0.25)] enabled:hover:-translate-y-px enabled:hover:bg-brand-hover enabled:hover:shadow-[0_6px_18px_rgba(36,87,214,0.35)]',
          })}
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
      <div className="flex items-center gap-2.5">
        <span className={cn(stepBadge, 'bg-brand text-white')}>{number}</span>
        <h3 className="m-0 text-[18px] font-bold text-navy">{title}</h3>
      </div>
      <p className="mt-1.5 mb-5 text-[13px] text-ink-subtle">{description}</p>
    </>
  )
}

function DeviceSection({ form, update }: StepProps) {
  const { lang } = useLanguage()
  return (
    <section className={orderSectionBox}>
      <SectionHeading
        number={1}
        title={lang === 'en' ? 'Select Machine Category' : '수리 품목 선택 (12종)'}
        description={lang === 'en'
          ? 'Select the device or appliance that needs inspection and repair.'
          : '점검 및 수리가 필요한 전자제품 또는 가전 기기를 선택해 주세요.'}
      />

      <div className="grid grid-cols-[repeat(6,1fr)] gap-3 max-[1024px]:grid-cols-[repeat(4,1fr)] max-[860px]:grid-cols-[repeat(3,1fr)] max-[640px]:grid-cols-[repeat(2,1fr)] max-[640px]:gap-2">
        {DEVICE_CATEGORIES.map(cat => {
          const isSelected = form.device === cat.deviceType
          return (
            <button
              key={cat.deviceType}
              type="button"
              className={cn(
                'relative flex cursor-pointer flex-col items-center rounded-[14px] border-[1.5px] px-3 pt-[18px] pb-3.5 text-center transition-all duration-200 ease-[cubic-bezier(0.16,1,0.3,1)] hover:-translate-y-0.5',
                isSelected
                  ? 'border-brand bg-[#f4f8ff] shadow-[0_4px_16px_rgba(36,87,214,0.14)]'
                  : 'border-line-default bg-white hover:border-[#93c5fd] hover:shadow-[0_6px_16px_rgba(36,87,214,0.08)]',
              )}
              onClick={() => update('device', cat.deviceType)}
            >
              <div className={cn('mb-2.5 grid h-[52px] w-[52px] place-items-center rounded-[14px] transition-[background-color] duration-200 ease-[ease]', isSelected ? 'bg-[#e5efff]' : 'bg-surface-subtle')}>
                <img src={cat.iconSrc} alt={cat.labelKo} width={36} height={36} />
              </div>
              <span className="text-[14px] leading-[1.3] font-[650] text-navy">
                {lang === 'en' ? cat.labelEn : cat.labelKo}
              </span>
              <small className="mt-1.5 text-[11px] leading-[1.35] break-keep text-ink-subtle">
                {lang === 'en' ? cat.descEn : cat.descKo}
              </small>
              {isSelected && (
                <span className="absolute top-2.5 right-2.5 grid h-5 w-5 place-items-center rounded-full bg-brand text-white">
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
    <section className={orderSectionBox}>
      <SectionHeading
        number={2}
        title={lang === 'en' ? 'Symptom & Description' : '고장 증상 및 요청 사항'}
        description={lang === 'en'
          ? 'Select common symptoms or describe the problem in detail.'
          : '자주 발생하는 증상을 클릭하시거나 구체적인 고장 증상을 적어주세요.'}
      />

      <div className="mb-3">
        <span className="mb-2 block text-[12px] font-semibold text-ink-subtle">{lang === 'en' ? 'Quick Add:' : '빠른 선택:'}</span>
        <div className="grid grid-cols-3 gap-2 max-[640px]:grid-cols-2">
          {quickSymptoms.map(s => (
            <button
              key={s}
              type="button"
              className={cn(
                'min-h-10 cursor-pointer rounded-lg border px-3 py-2 text-left text-[13px] transition-all duration-150 ease-[ease]',
                form.symptom.includes(s)
                  ? 'border-brand bg-[#f4f8ff] font-semibold text-brand'
                  : 'border-line-strong bg-white font-medium text-ink-body hover:border-ink-faint hover:bg-surface-subtle',
              )}
              aria-pressed={form.symptom.includes(s)}
              onClick={() => update('symptom', appendSymptom(form.symptom, s))}
            >
              {s}
            </button>
          ))}
        </div>
      </div>

      <textarea
        className="box-border w-full resize-y rounded-[10px] border border-line-strong px-4 py-3.5 text-[14px] leading-[1.6] text-ink focus:border-brand focus:shadow-[0_0_0_3px_rgba(36,87,214,0.15)] focus:outline-none"
        rows={4}
        required
        maxLength={SYMPTOM_MAX_LENGTH}
        placeholder={lang === 'en' ? 'Describe symptoms, model name, or special requests...' : '기기 모델명이나 구체적인 고장 증상을 적어주시면 엔지니어가 부품을 미리 준비할 수 있습니다.'}
        value={form.symptom}
        onChange={e => update('symptom', e.target.value)}
      />
      <span className="mt-1.5 block text-right text-[12px] text-ink-faint">{form.symptom.length} / {SYMPTOM_MAX_LENGTH}</span>
    </section>
  )
}

function ScheduleSection({ form, update }: StepProps) {
  const { lang } = useLanguage()
  return (
    <section className={cn(orderSectionBox, 'mb-0')}>
      <SectionHeading
        number={3}
        title={lang === 'en' ? 'Preferred Schedule' : '방문 희망 일정'}
        description={lang === 'en'
          ? 'Select the preferred visit date and time slot.'
          : '엔지니어의 방문을 희망하시는 날짜와 시간대를 선택해 주세요.'}
      />

      <div className="grid grid-cols-[1fr] gap-[18px]">
        <div>
          <label className={fieldLabel} htmlFor="visit-date">{lang === 'en' ? 'Preferred Date' : '방문 희망일'}</label>
          <input
            id="visit-date"
            type="date"
            className={orderInput}
            required
            min={getTomorrowDate()}
            value={form.date}
            onChange={e => update('date', e.target.value)}
          />
        </div>
        <div>
          <label className={fieldLabel} htmlFor="visit-time">{lang === 'en' ? 'Preferred Time' : '방문 희망 시간'}</label>
          <div className="grid grid-cols-[repeat(3,1fr)] gap-2">
            {TIME_SLOTS.map(slot => (
              <button
                key={slot}
                type="button"
                className={cn(
                  'grid h-[42px] cursor-pointer place-items-center rounded-lg border text-[13px] transition-all duration-150 ease-[ease]',
                  form.time === slot
                    ? 'border-brand bg-brand font-semibold text-white'
                    : 'border-line-strong bg-white font-medium text-ink-body hover:border-ink-faint hover:bg-surface-subtle',
                )}
                onClick={() => update('time', slot)}
              >
                {slot}
              </button>
            ))}
          </div>
          <div className="mt-3 flex items-center gap-2.5 border-t border-dashed border-line-default pt-3">
            <span className="shrink-0 text-[12px] font-semibold text-ink-subtle">{lang === 'en' ? 'Direct Time Pick:' : '직접 시간 지정:'}</span>
            <input
              id="visit-time"
              type="time"
              className={cn(orderInput, 'max-w-[140px]')}
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
    <section className={cn(orderSectionBox, 'mb-0')}>
      <SectionHeading
        number={4}
        title={lang === 'en' ? 'Contact & Visit Location' : '신청 고객 및 방문 주소'}
        description={lang === 'en'
          ? 'Please enter contact information and exact address for the on-site technician.'
          : '엔지니어 방문 및 일정 안내를 위해 신청자 정보와 방문 주소를 정확히 입력해 주세요.'}
      />

      <div className={fieldsGrid}>
        <div className={fullField}>
          <label className={fieldLabel} htmlFor="client-name">{lang === 'en' ? 'Customer Name' : '성함 (신청자)'}</label>
          <input
            id="client-name"
            required
            className={orderInput}
            placeholder={lang === 'en' ? 'e.g. John Doe' : '홍길동'}
            value={form.name}
            onChange={e => update('name', e.target.value)}
          />
        </div>
        <div className={fullField}>
          <label className={fieldLabel} htmlFor="client-phone">{lang === 'en' ? 'Mobile Number' : '휴대전화 번호'}</label>
          <PhoneInput
            id="client-phone"
            required
            className={orderInput}
            value={form.phone}
            onChange={value => update('phone', value)}
          />
        </div>
        <div className={fullField}>
          <label className={fieldLabel} htmlFor="client-address">{lang === 'en' ? 'Visit Address' : '방문 주소'}</label>
          <input
            id="client-address"
            required
            className={orderInput}
            placeholder={lang === 'en' ? 'Enter full address including apartment / room number' : '방문 받으실 상세 주소를 입력해 주세요 (동/호수 포함)'}
            value={form.address}
            onChange={e => update('address', e.target.value)}
          />
        </div>

        {!isMember && (
          <div className={cn(fullField, 'mt-2 border-t border-dashed border-line-default pt-3.5')}>
            <label className="m-0 flex cursor-pointer items-start gap-2.5 text-[13.5px] font-normal text-ink">
              <input
                className="mt-0.5 h-4 w-4 shrink-0 cursor-pointer accent-brand"
                type="checkbox"
                checked={form.agreedPrivacy}
                onChange={e => update('agreedPrivacy', e.target.checked)}
                required
              />
              <span className="text-[13px] leading-[1.5] break-keep text-ink-secondary">
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
