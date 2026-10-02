import type { UsageItem } from '../../lib/api'
import type { ChatText } from './types'
import { cn } from '@/lib/utils'

/**
 * 이번 주 남은 상담 횟수. 배지 대신 문구와 얇은 막대로 남은 비율을 보여 주고, 얼마 남지 않으면 색으로 알린다.
 */
export function ChatUsage({ item, text }: { item: UsageItem; text: ChatText['usage'] }) {
  const ratio = item.limit > 0 ? item.remaining / item.limit : 0
  const exhausted = item.remaining === 0
  const low = !exhausted && ratio <= 0.2
  return (
    <div className="mt-3 grid gap-1.5" aria-live="polite">
      <div className="flex items-baseline justify-between text-[12.5px] font-medium text-ink-secondary">
        <span>{text.label}</span>
        <strong className={cn('font-bold tabular-nums', exhausted ? 'text-[#be123c]' : low ? 'text-[#b45309]' : 'text-[#0f172a]')}>{text.count(item.remaining, item.limit)}</strong>
      </div>
      <div className="h-1 overflow-hidden rounded-[999px] bg-line-default" role="progressbar" aria-label={text.label}
        aria-valuemin={0} aria-valuemax={item.limit} aria-valuenow={item.remaining}>
        <div className={cn('h-full rounded-[inherit] transition-[width] duration-300 ease-[ease]', low ? 'bg-[#f59e0b]' : 'bg-[#2455d9]')} style={{ width: `${Math.round(ratio * 100)}%` }} />
      </div>
    </div>
  )
}
