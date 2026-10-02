import { CheckCircle2, LoaderCircle } from 'lucide-react'
import type { DiagnosisToolProgress } from '../../lib/api'
import { isToolName, type ChatText, type ToolName } from './types'

/** 응답을 기다리는 동안 마지막으로 실행 중인 도구를 알려 준다. */
export function ToolCallingIndicator({ progress = [], text }: { progress?: DiagnosisToolProgress[]; text: ChatText['progress'] }) {
  const latest = progress.at(-1)
  const phase = latest?.status === 'started' ? text.started : text.completed
  const [title, description] = latest && latest.tool in phase
    ? phase[latest.tool as keyof typeof phase]
    : text.initial
  return (
    <div className="tool-progress" aria-label={title}>
      <LoaderCircle className="tool-spinner" size={15} aria-hidden="true" />
      <div><strong>{title}</strong><span>{description}</span></div>
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
  return <ul className="chat-tool-results" aria-label={text.aria}>
    {statuses.map(({ tool, status }) => <li key={tool} className={status === 'started' ? 'is-started' : ''}>
      {status === 'started' ? <LoaderCircle className="tool-result-spinner" size={14} aria-hidden="true" /> : <CheckCircle2 size={14} aria-hidden="true" />}
      {text.labels[tool]}<span>{status === 'started' ? text.running : text.done}</span>
    </li>)}
  </ul>
}
