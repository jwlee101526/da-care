import type { UsageItem } from '../../lib/api'
import type { ChatText } from './types'

/**
 * 이번 주 남은 상담 횟수. 배지 대신 문구와 얇은 막대로 남은 비율을 보여 주고, 얼마 남지 않으면 색으로 알린다.
 */
export function ChatUsage({ item, text }: { item: UsageItem; text: ChatText['usage'] }) {
  const ratio = item.limit > 0 ? item.remaining / item.limit : 0
  const level = item.remaining === 0 ? ' is-exhausted' : ratio <= 0.2 ? ' is-low' : ''
  return (
    <div className={'chat-usage' + level} aria-live="polite">
      <div className="chat-usage-text">
        <span>{text.label}</span>
        <strong>{text.count(item.remaining, item.limit)}</strong>
      </div>
      <div className="chat-usage-track" role="progressbar" aria-label={text.label}
        aria-valuemin={0} aria-valuemax={item.limit} aria-valuenow={item.remaining}>
        <div style={{ width: `${Math.round(ratio * 100)}%` }} />
      </div>
    </div>
  )
}
