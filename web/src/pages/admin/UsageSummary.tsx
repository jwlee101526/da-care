import type { TotalUsage, UsageItem } from '../../lib/api'

export function UsageSummary({ usage }: { usage: TotalUsage }) {
  return (
    <section className="usage-summary">
      <h2>이번 주 사용량</h2>
      <p>유료 API의 서비스 전체 주간 한도입니다. 매주 월요일 0시(한국 시간)에 초기화됩니다.</p>
      <UsageMeter label="AI 상담" item={usage.diagnosis} />
      <UsageMeter label="SMS 발송" item={usage.sms} />
    </section>
  )
}

function UsageMeter({ label, item }: { label: string; item: UsageItem }) {
  const percent = item.limit > 0 ? Math.min(100, Math.round(item.used / item.limit * 100)) : 100
  const level = item.remaining === 0 ? ' exhausted' : percent >= 80 ? ' warning' : ''
  return (
    <div className={'usage-meter' + level}>
      <div className="usage-meter-label">
        <span>{label}</span>
        <strong>{item.used} / {item.limit}</strong>
      </div>
      <div className="usage-meter-track" role="progressbar" aria-label={label}
        aria-valuemin={0} aria-valuemax={item.limit} aria-valuenow={item.used}>
        <div style={{ width: `${percent}%` }} />
      </div>
    </div>
  )
}
