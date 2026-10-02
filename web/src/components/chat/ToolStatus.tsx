import { CheckCircle2, LoaderCircle } from 'lucide-react'
import type { DiagnosisToolProgress } from '../../lib/api'
import { isToolName, type ChatText, type ToolName } from './types'
import { cn } from '@/lib/utils'

/** 응답을 기다리는 동안 마지막으로 실행 중인 도구를 알려 준다. */
export function ToolCallingIndicator({ progress = [], text }: { progress?: DiagnosisToolProgress[]; text: ChatText['progress'] }) {
  const latest = progress.at(-1)
  const phase = latest?.status === 'started' ? text.started : text.completed
  const [title, description] = latest && latest.tool in phase
    ? phase[latest.tool as keyof typeof phase]
    : text.initial
  return (
    <div className="mx-0.5 mt-2 mb-2.5 flex items-start gap-2 border-l-2 border-[#2455d9] py-0.5 pr-0 pl-2.5 text-[#43546a]" aria-label={title}>
      <LoaderCircle className="mt-0.5 flex-[0_0_auto] animate-spin text-[#2455d9] motion-reduce:animate-none" size={15} aria-hidden="true" />
      <div className="grid min-w-0 gap-0.5">
        <strong className="text-[12px] leading-[1.4] font-[650] text-[#34455c]">{title}</strong>
        <span className="text-[11px] leading-[1.45] text-[#62728a]">{description}</span>
      </div>
    </div>
  )
}

/** 이번 응답에서 실행된 도구 목록. */
export function ToolResults({ tools = [], progress = [], text }: { tools?: string[]; progress?: DiagnosisToolProgress[]; text: ChatText['tools'] }) {
  const statuses: { tool: ToolName; status: DiagnosisToolProgress['status'] }[] = []
  for (const item of progress) {
    if (isToolName(item.tool, text.labels)) statuses.push({ tool: item.tool, status: item.status })
  }
  for (const tool of new Set(tools)) {
    if (isToolName(tool, text.labels) && !statuses.some(item => item.tool === tool)) statuses.push({ tool, status: 'completed' })
  }
  if (!statuses.length) return null
  return <ul className="mx-0 mt-0 mb-2.5 grid list-none gap-1.5 p-0" aria-label={text.aria}>
    {statuses.map(({ tool, status }) => <li
      key={tool}
      className={cn(
        'flex items-center gap-2 rounded-[10px] border px-3 py-2 text-[12.5px] font-semibold',
        status === 'started' ? 'border-[#93c5fd] bg-[#eff6ff] text-[#1d4ed8] [&>svg]:text-[#2455d9]' : 'border-[#d9e2ec] bg-surface-subtle text-ink [&>svg]:text-success',
      )}
    >
      {status === 'started' ? <LoaderCircle className="animate-spin motion-reduce:animate-none" size={14} aria-hidden="true" /> : <CheckCircle2 size={14} aria-hidden="true" />}
      {text.labels[tool]}<span className="ml-auto text-[11.5px] font-semibold text-ink-secondary">{status === 'started' ? text.running : text.done}</span>
    </li>)}
  </ul>
}
