import type { ReactNode } from 'react'
import { Check, Clock, CheckCircle2, AlertCircle, Bell } from 'lucide-react'
import { useLanguage } from '../context/LanguageContext'
import type { GuestReservation } from '../lib/api'
import { formatReservationCode } from '../lib/reservationCode'
import { getCategoryInfo } from '../lib/categories'
import { cn } from '@/lib/utils'
import { buttonVariants } from './ui/button'

interface ReservationCardProps {
  item: GuestReservation
  onCancel?: () => void
  cancelling?: boolean
  cancelDisabled?: boolean
}

function formatDateTime(isoString?: string | null) {
  if (!isoString) return '-'
  const clean = isoString.replace('T', ' ')
  return clean.length > 16 ? clean.slice(0, 16) : clean
}

type StepState = 'todo' | 'done' | 'current'

const stepIconClass: Record<StepState, string> = {
  todo: 'border-line-strong bg-white text-ink-faint',
  done: 'border-brand bg-brand text-white',
  current: 'border-brand bg-white text-brand shadow-[0_0_0_4px_rgba(36,87,214,0.15)]',
}
const stepTitleClass: Record<StepState, string> = {
  todo: 'font-medium text-ink-subtle',
  done: 'font-semibold text-brand',
  current: 'font-semibold text-navy',
}

function Step({ state, icon, title }: { state: StepState; icon: ReactNode; title: string }) {
  return (
    <div className="flex w-[90px] flex-col items-center gap-2 max-[640px]:w-[72px]">
      <div className={cn('flex h-[34px] w-[34px] items-center justify-center rounded-full border-2 transition-all duration-200 ease-[ease]', stepIconClass[state])}>
        {icon}
      </div>
      <span className={cn('text-[13px] whitespace-nowrap max-[640px]:text-[11px]', stepTitleClass[state])}>{title}</span>
    </div>
  )
}

function DetailRow({ label, children, strong = false, wide = false }: { label: string; children: ReactNode; strong?: boolean; wide?: boolean }) {
  return (
    <div className={cn('flex flex-col gap-1', wide && 'col-span-2 max-[640px]:col-span-1')}>
      <span className="text-[12px] font-medium text-ink-subtle">{label}</span>
      <span className={cn('text-[14px] break-all text-ink', strong && 'font-semibold')}>{children}</span>
    </div>
  )
}

// 예약 한 건의 진행 단계와 상세 내용. 회원 예약 내역과 비회원 예약 조회 화면이 함께 쓴다.
export function ReservationCard({ item, onCancel, cancelling = false, cancelDisabled = false }: ReservationCardProps) {
  const { lang } = useLanguage()
  const currentStep = item.status === 'PENDING' ? 1 : item.status === 'CONFIRMED' ? 2 : item.status === 'COMPLETED' ? 3 : 0
  const stepState = (step: number): StepState => (currentStep === step ? 'current' : currentStep > step ? 'done' : 'todo')

  return (
    <section className="flex flex-col gap-6 rounded-2xl border border-line-default bg-white px-8 py-7 shadow-[0_4px_20px_-2px_rgba(24,41,70,0.05)] transition-[transform,box-shadow] duration-150 ease-[ease] hover:shadow-[0_8px_28px_-4px_rgba(24,41,70,0.08)] max-[640px]:gap-[18px] max-[640px]:px-[18px] max-[640px]:py-5">
      <div className="flex items-center justify-between gap-4 border-b border-surface-muted pb-5">
        <div>
          <span className="text-[18px] font-bold tracking-[-0.01em] text-navy">
            {lang === 'en' ? 'Order' : '예약 번호'} {formatReservationCode(item.code)}
          </span>
        </div>
      </div>

      {item.status === 'CANCELLED' ? (
        <div className="flex items-start gap-3.5 rounded-xl border border-line-default bg-surface-subtle px-5 py-4 text-ink-subtle">
          <AlertCircle size={20} />
          <div>
            <h4 className="mt-0 mb-1 text-[15px] font-semibold text-ink-body">{lang === 'en' ? 'Order Cancelled' : '취소된 예약입니다.'}</h4>
            <p className="m-0 text-[13px]">{lang === 'en' ? 'This reservation has been cancelled.' : '해당 예약은 취소 처리되었습니다.'}</p>
          </div>
        </div>
      ) : (
        <div className="relative pt-3 pb-2">
          <div className="absolute top-[27px] right-9 left-9 z-[1] h-1 rounded-full bg-line-default max-[640px]:right-6 max-[640px]:left-6">
            <div
              className="h-full rounded-full bg-brand transition-[width] duration-[400ms] ease-[ease]"
              style={{
                width: currentStep === 1 ? '0%' : currentStep === 2 ? '50%' : '100%',
              }}
            />
          </div>
          <div className="relative z-[2] flex justify-between">
            <Step
              state={stepState(1)}
              icon={currentStep > 1 ? <Check size={16} /> : <Clock size={16} />}
              title={lang === 'en' ? 'Received' : '접수 완료'}
            />
            <Step
              state={stepState(2)}
              icon={currentStep > 2 ? <Check size={16} /> : <Clock size={16} />}
              title={lang === 'en' ? 'Confirmed' : '예약 확정'}
            />
            <Step
              state={stepState(3)}
              icon={<CheckCircle2 size={16} />}
              title={lang === 'en' ? 'Completed' : '수리 완료'}
            />
          </div>
        </div>
      )}

      <div className="grid grid-cols-[repeat(2,1fr)] gap-x-6 gap-y-4 rounded-xl border border-surface-muted bg-surface-subtle px-5 py-[18px] max-[640px]:grid-cols-[1fr] max-[640px]:gap-3 max-[640px]:px-4 max-[640px]:py-3.5">
        <DetailRow label={lang === 'en' ? 'Device' : '수리 품목'} strong>
          {getCategoryInfo(item.deviceType, lang)}
        </DetailRow>

        <DetailRow label={lang === 'en' ? 'Preferred Schedule' : '방문 희망 일시'}>
          {formatDateTime(item.preferredAt)}
        </DetailRow>

        {item.confirmedAt && (
          <DetailRow label={lang === 'en' ? 'Confirmed Schedule' : '확정 방문 일시'} strong>
            {formatDateTime(item.confirmedAt)}
          </DetailRow>
        )}

        <DetailRow label={lang === 'en' ? 'Address' : '방문 주소'} wide>
          {item.visitAddress}
        </DetailRow>

        <DetailRow label={lang === 'en' ? 'Technician' : '담당 엔지니어'} strong>
          {item.engineerName ? item.engineerName : (lang === 'en' ? 'Pending Assignment' : '배정 진행 중')}
        </DetailRow>
      </div>

      {item.status === 'CONFIRMED' && (
        <section className="mt-[18px] mb-0 flex items-start gap-2.5 rounded-[10px] border border-[#d8e5ff] bg-[#f3f7ff] px-4 py-3.5 text-[#234a9d]" aria-label={lang === 'en' ? 'Reservation notification' : '예약 알림 안내'}>
          <Bell size={18} aria-hidden="true" />
          <p className="m-0 text-[13px] leading-[1.55]">{lang === 'en' ? 'Your technician assignment and confirmed visit time will be delivered to your registered mobile number by SMS.' : '담당 기사 배정과 확정 방문 일시는 등록된 휴대전화 번호로 문자 안내를 받게 됩니다.'}</p>
        </section>
      )}

      {item.symptomDescription && (
        <div className="rounded-xl border border-line-default bg-white px-5 py-4">
          <div className="mb-2 text-[13px] font-semibold text-ink-secondary">
            <span>{lang === 'en' ? 'Symptom Description' : '고장 증상 및 접수 내용'}</span>
          </div>
          <p className="m-0 text-[14px] leading-[1.6] whitespace-pre-wrap text-ink">{item.symptomDescription}</p>
        </div>
      )}

      {item.status === 'PENDING' && onCancel && (
        <div className="flex justify-end border-t border-surface-muted pt-3">
          <button
            type="button"
            className={buttonVariants({
              variant: 'secondary',
              size: 'compact',
              className: 'border-line-default font-medium text-danger enabled:hover:border-danger-border enabled:hover:bg-danger-soft enabled:hover:text-danger-strong',
            })}
            onClick={onCancel}
            disabled={cancelDisabled}
          >
            {cancelling ? (lang === 'en' ? 'Cancelling...' : '취소 중...') : (lang === 'en' ? 'Cancel Reservation' : '예약 취소')}
          </button>
        </div>
      )}
    </section>
  )
}
