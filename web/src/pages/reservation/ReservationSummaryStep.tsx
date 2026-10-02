import { Clock } from 'lucide-react'
import { useLanguage } from '../../context/LanguageContext'
import type { CategoryDefinition } from '../../lib/categories'
import { formatPhone } from '../../lib/phone'
import { cn } from '@/lib/utils'
import { buttonVariants } from '../../components/ui/button'
import type { ReservationFormValues } from './reservationForm'

const strongValue = 'font-semibold'
const brandValue = 'font-semibold text-brand'

/** 2단계: 입력한 내용을 확인하고 접수한다. */
export function ReservationSummaryStep({ form, category, isMember, submitting, onEdit, onConfirm }: {
  form: ReservationFormValues
  category: CategoryDefinition
  isMember: boolean
  submitting: boolean
  onEdit: () => void
  onConfirm: () => void
}) {
  const { lang } = useLanguage()
  const categoryLabel = lang === 'en' ? category.labelEn : category.labelKo
  return (
    <div className="mx-auto my-0 w-full max-w-[860px]">
      <div className="overflow-hidden rounded-[20px] border border-line-default bg-white shadow-[0_6px_24px_rgba(24,41,70,0.06)]">
        <div className="flex items-center gap-5 bg-[linear-gradient(135deg,var(--navy)_0%,#1e3a6d_100%)] px-[30px] py-[26px] text-white max-[640px]:p-5">
          <div className="grid h-[58px] w-[58px] shrink-0 place-items-center rounded-[14px] border border-[rgba(255,255,255,0.22)] bg-[rgba(255,255,255,0.12)]">
            <img className="[filter:brightness(0)_invert(1)]" src={category.iconSrc} alt={category.labelKo} width={34} height={34} />
          </div>
          <div className="min-w-0 flex-1">
            <h3 className="mt-0 mb-1.5 text-[20px] font-bold tracking-[-0.01em] text-white">{categoryLabel}</h3>
            <p className="m-0 flex items-center gap-1.5 text-[14px] text-line-strong">
              <Clock size={15} />
              <span>{form.date} {form.time}</span>
            </p>
          </div>
        </div>

        <div className="grid grid-cols-[1fr_1fr] border-b border-line-subtle max-[860px]:grid-cols-[1fr]">
          <SummarySection first title={lang === 'en' ? 'Customer Information' : '고객 정보'}>
            <SummaryItem label={lang === 'en' ? 'Name' : '성함'} valueClassName={strongValue}>{form.name}</SummaryItem>
            <SummaryItem label={lang === 'en' ? 'Mobile' : '휴대전화 번호'}>{formatPhone(form.phone)}</SummaryItem>
            <SummaryItem label={lang === 'en' ? 'Address' : '방문 주소'} valueClassName="max-w-[280px] leading-[1.5]">{form.address}</SummaryItem>
            <SummaryItem label={lang === 'en' ? 'Booking Type' : '접수 유형'} valueClassName={brandValue}>
              {isMember ? (lang === 'en' ? 'Registered Member' : '회원 예약') : (lang === 'en' ? 'Guest (Non-member)' : '비회원 간편 접수')}
            </SummaryItem>
          </SummarySection>

          <SummarySection title={lang === 'en' ? 'Repair Details & Symptom' : '수리 품목 및 증상'}>
            <SummaryItem label={lang === 'en' ? 'Category' : '품목'} valueClassName={strongValue}>{categoryLabel}</SummaryItem>
            <SummaryItem label={lang === 'en' ? 'Schedule' : '희망 일시'} valueClassName={brandValue}>{form.date} {form.time}</SummaryItem>
            <div className="mt-1 rounded-[10px] border border-line-default bg-surface-subtle px-3.5 py-3">
              <span className="mb-1.5 block text-[12px] font-semibold text-ink-subtle">{lang === 'en' ? 'Symptom Note' : '증상 메모'}</span>
              <p className="m-0 text-[13px] leading-[1.6] whitespace-pre-wrap [word-break:break-word] text-ink-body">{form.symptom}</p>
            </div>
          </SummarySection>
        </div>

        <div className="flex gap-3.5 bg-white px-[30px] py-5 max-[640px]:flex-col-reverse max-[640px]:px-5 max-[640px]:py-4">
          <button
            type="button"
            className={buttonVariants({ variant: 'secondary', className: 'min-h-[52px] flex-1 text-[15px] leading-[1.4]' })}
            onClick={onEdit}
            disabled={submitting}
          >
            {lang === 'en' ? 'Edit Details' : '내용 수정하기'}
          </button>
          <button
            type="button"
            className={buttonVariants({ variant: 'primary', className: 'min-h-[52px] flex-[2] text-[15px] leading-[1.4] font-semibold' })}
            onClick={onConfirm}
            disabled={submitting}
          >
            {submitting
              ? (lang === 'en' ? 'Processing...' : '접수 진행 중...')
              : (lang === 'en' ? 'Confirm & Submit Reservation' : '예약 접수 완료하기')}
          </button>
        </div>
      </div>
    </div>
  )
}

// 두 칸으로 나란히 놓이고, 좁은 화면에서는 위아래로 쌓인다. 칸 사이 구분선은 첫 칸이 그린다.
function SummarySection({ title, first = false, children }: { title: string; first?: boolean; children: React.ReactNode }) {
  return (
    <div className={cn('flex flex-col gap-4 px-[30px] py-7 max-[640px]:p-5', first && 'border-r border-line-subtle max-[860px]:border-r-0 max-[860px]:border-b')}>
      <h4 className="m-0 flex items-center gap-2 border-b-2 border-surface-muted pb-3 text-[14px] font-bold text-ink">{title}</h4>
      <div className="flex flex-col gap-3">{children}</div>
    </div>
  )
}

function SummaryItem({ label, valueClassName, children }: { label: string; valueClassName?: string; children: React.ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-3 text-[14px]">
      <span className="shrink-0 text-[13px] font-medium text-ink-subtle">{label}</span>
      <span className={cn('text-right [word-break:break-word] text-ink', valueClassName)}>{children}</span>
    </div>
  )
}
