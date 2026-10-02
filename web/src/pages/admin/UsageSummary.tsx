import type { TotalUsage, UsageItem } from '../../lib/api'
import { cn } from '@/lib/utils'
import { adminSection, adminSectionTitle } from './adminStyles'

export function UsageSummary({ usage }: { usage: TotalUsage }) {
  return (
    <section className={adminSection}>
      <h2 className={adminSectionTitle}>이번 주 사용량</h2>
      <p className="-mt-2 mb-[18px] text-[14px] text-ink-subtle">유료 API의 서비스 전체 주간 한도입니다. 매주 월요일 0시(한국 시간)에 초기화됩니다.</p>
      <UsageMeter label="AI 상담" item={usage.diagnosis} />
      <UsageMeter className="mt-4" label="SMS 발송" item={usage.sms} />
    </section>
  )
}

function UsageMeter({ label, item, className }: { label: string; item: UsageItem; className?: string }) {
  const percent = item.limit > 0 ? Math.min(100, Math.round(item.used / item.limit * 100)) : 100
  // 다 쓰면 빨강, 80% 이상이면 주황
  const barColor = item.remaining === 0 ? 'bg-[#e11d48]' : percent >= 80 ? 'bg-warning' : 'bg-brand'
  return (
    <div className={className}>
      <div className="mb-1.5 flex justify-between text-[14px] font-semibold text-navy">
        <span>{label}</span>
        <strong>{item.used} / {item.limit}</strong>
      </div>
      <div className="h-2 overflow-hidden rounded-[999px] bg-[#eef2f7]" role="progressbar" aria-label={label}
        aria-valuemin={0} aria-valuemax={item.limit} aria-valuenow={item.used}>
        <div className={cn('h-full rounded-[inherit] transition-[width] duration-300 ease-[ease]', barColor)} style={{ width: `${percent}%` }} />
      </div>
    </div>
  )
}
