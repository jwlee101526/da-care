import type { DiagnosisCard, DiagnosisToolProgress } from '../../lib/api'
import type { LocaleDict } from '../../locales/ko'
import type { ReservationSelection } from '../../types'

export interface Message {
  id: number
  sender: 'user' | 'bot'
  text: string
  cards?: DiagnosisCard[]
  executedTools?: string[]
  toolProgress?: DiagnosisToolProgress[]
  streaming?: boolean
  error?: boolean
  /** 카드 버튼으로 보낸 요청. 화면에는 표시하지 않지만 다음 요청의 대화 이력에는 포함한다. */
  hidden?: boolean
}

export type ChatText = LocaleDict['chat']
export type ToolName = keyof ChatText['tools']['labels']
export type NavigationPage = Extract<DiagnosisCard, { type: 'navigation' }>['page']
export type BookWithSymptom = (selection: ReservationSelection) => void
export type PanelSize = { width: number; height: number }
export type Coordinates = { x: number; y: number }

export function isToolName(tool: string, labels: ChatText['tools']['labels']): tool is ToolName {
  return tool in labels
}
