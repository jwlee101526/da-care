import type { DiagnosisMetrics, TotalUsage, UsageItem } from '../../lib/api'
import { cn } from '@/lib/utils'
import { adminSection, adminSectionTitle } from './adminStyles'

export function UsageSummary({ usage }: { usage: TotalUsage }) {
  return (
    <section className={adminSection}>
      <h2 className={adminSectionTitle}>이번 주 사용량</h2>
      <p className="-mt-2 mb-[18px] text-[14px] text-ink-subtle">유료 API의 서비스 전체 주간 한도입니다. 매주 월요일 0시(한국 시간)에 초기화됩니다.</p>
      <UsageMeter label="AI 상담" item={usage.diagnosis} />
      <UsageMeter className="mt-4" label="SMS 발송" item={usage.sms} />
      <DiagnosisMetricsGrid metrics={usage.diagnosisMetrics} />
    </section>
  )
}

const numberFormat = new Intl.NumberFormat('ko-KR')

function DiagnosisMetricsGrid({ metrics }: { metrics: DiagnosisMetrics }) {
  const title = <h3 className="mb-3 text-[15px] font-semibold text-navy">AI 상담 모델 사용량</h3>
  if (metrics.consultations === 0) {
    return (
      <div className="mt-6 border-t border-line-default pt-5">
        {title}
        <p className="text-[14px] text-ink-subtle">이번 주 상담 기록이 없습니다.</p>
      </div>
    )
  }
  const unfinished = metrics.failed + metrics.timedOut + metrics.cancelled
  const cacheRate = metrics.inputTokens > 0 ? Math.round(metrics.cachedInputTokens / metrics.inputTokens * 100) : 0
  const items = [
    { label: '상담', value: `${numberFormat.format(metrics.consultations)}건`, detail: `완료 ${metrics.completed} · 실패 ${metrics.failed} · 시간 초과 ${metrics.timedOut} · 취소 ${metrics.cancelled}`, alert: unfinished > 0 },
    { label: '평균 소요 시간', value: `${(metrics.averageDurationMs / 1000).toFixed(1)}초`, detail: '대기열 시간 포함' },
    { label: '평균 모델 호출', value: `${metrics.averageModelCalls.toFixed(1)}회`, detail: '도구 호출마다 모델을 다시 호출' },
    { label: '입력 토큰', value: numberFormat.format(metrics.inputTokens), detail: `캐시 적중 ${cacheRate}%` },
    { label: '출력 토큰', value: numberFormat.format(metrics.outputTokens), detail: `추론 ${numberFormat.format(metrics.reasoningTokens)}` },
  ]
  return (
    <div className="mt-6 border-t border-line-default pt-5">
      {title}
      <dl className="grid grid-cols-3 gap-2.5 max-[850px]:grid-cols-2 max-[600px]:grid-cols-1">
        {items.map(item => (
          <div key={item.label} className="rounded-lg border border-surface-muted bg-surface-subtle px-4 py-3">
            <dt className="text-[13px] font-medium text-ink-subtle">{item.label}</dt>
            <dd className="mt-1 text-[20px] font-bold text-navy">{item.value}</dd>
            <dd className={cn('mt-0.5 text-[13px] text-ink-subtle', item.alert && 'text-warning')}>{item.detail}</dd>
          </div>
        ))}
      </dl>
    </div>
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
