const baseUrl = import.meta.env.VITE_API_BASE_URL ?? ''
/** 서버 상태 확인 주소 */
export const serverHealthUrl = `${baseUrl}/actuator/health`

export class ApiError extends Error {
  status: number
  fields: Record<string, string>
  constructor(status: number, message: string, fields: Record<string, string> = {}) { super(message); this.status = status; this.fields = fields }
}

/** code는 고객에게 보여주는 예약 번호(숫자 8자리)이고, id는 회원·관리자 API에서만 쓰는 내부 번호다. */
export type Reservation = { id: number; code: string; deviceType: import('../types').DeviceType; symptomDescription: string; visitAddress: string; preferredAt: string; confirmedAt: string | null; status: 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED'; engineerName: string | null; contactName: string | null; contactPhone: string | null }
/** 비회원 예약 응답. 순번인 id는 내려오지 않고, 조회 응답의 이름·연락처·주소는 일부 가려져 있다. */
export type GuestReservation = Omit<Reservation, 'id'>
export type Engineer = { id: number; name: string; phone: string; specialty: string; region: string }
export type DiagnosisCard =
  | { type: 'inspection'; title: string; deviceType: import('../types').DeviceType | 'etc'; deviceName: string; suspectedCause: string | null; inspectionDetails: string; evidence: { sourceId: string; quote: string; citation: string | null }[] }
  | { type: 'booking'; deviceType: import('../types').DeviceType; symptom: string; loginRequired: boolean }
  | { type: 'reservation_status'; reservationCode: string; status: Reservation['status']; preferredAt: string; confirmedAt: string | null; engineerName: string | null }
  | { type: 'navigation'; page: 'reservations' | 'reserve'; title: string; description: string; actionLabel: string }
export type UsageItem = { used: number; limit: number; remaining: number }
/** 요청자(로그인 계정 또는 비로그인 IP) 기준 이번 주 AI 상담 사용량. remaining은 서비스 전체 잔여량을 넘지 않는다. */
export type MyUsage = { periodStart: string; resetsAt: string; diagnosis: UsageItem }
/** 이번 주 AI 상담의 모델 사용량 집계. 실패·시간 초과·취소된 상담의 토큰도 포함한다. */
export type DiagnosisMetrics = {
  consultations: number
  completed: number
  failed: number
  timedOut: number
  cancelled: number
  averageDurationMs: number
  averageModelCalls: number
  inputTokens: number
  cachedInputTokens: number
  outputTokens: number
  reasoningTokens: number
}
/** 서비스 전체 이번 주 사용량(관리자 전용). */
export type TotalUsage = {
  periodStart: string
  resetsAt: string
  diagnosis: UsageItem
  sms: UsageItem
  diagnosisMetrics: DiagnosisMetrics
}
export type Diagnosis = { answer: string; cards: DiagnosisCard[]; executedTools: string[] }
export type DiagnosisToolProgress = { tool: string; status: 'started' | 'completed'; card?: DiagnosisCard | null }
export type DiagnosisStreamEvent =
  | { type: 'tool'; data: DiagnosisToolProgress }
  | { type: 'completed'; data: Diagnosis }
  | { type: 'error'; data: { code: string; message: string } }

export async function api<T>(path: string, options: RequestInit = {}, token?: string | null): Promise<T> {
  const response = await fetch(`${baseUrl}${path}`, { ...options, headers: { 'Content-Type': 'application/json', 'API-Version': '1', ...(token ? { Authorization: `Bearer ${token}` } : {}), ...options.headers } })
  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new ApiError(response.status, body.message ?? '요청 처리 중 오류가 발생했습니다.', body.fields ?? {})
  }
  return response.status === 204 ? undefined as T : response.json() as Promise<T>
}

/** 서버가 제한 시간 안에 정상 응답하는지 확인한다. 연결 실패,시간 초과,5xx는 모두 false다. */
export async function checkServerHealth() {
  try {
    const response = await fetch(serverHealthUrl, { cache: 'no-store', signal: AbortSignal.timeout(8000) })
    return response.ok
  } catch {
    return false
  }
}

export function fetchMyUsage(token?: string | null) {
  return api<MyUsage>('/api/usage', {}, token)
}

export function fetchTotalUsage(token?: string | null) {
  return api<TotalUsage>('/api/admin/usage', {}, token)
}

export async function streamDiagnosis(
  question: string,
  history: { role: 'user' | 'assistant'; text: string }[],
  token: string | null | undefined,
  signal: AbortSignal,
  onEvent: (event: DiagnosisStreamEvent) => void,
) {
  const response = await fetch(`${baseUrl}/api/diagnosis/chat/stream`, {
    method: 'POST',
    signal,
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      'API-Version': '1',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({ question, history }),
  })
  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new ApiError(response.status, body.message ?? '요청 처리 중 오류가 발생했습니다.', body.fields ?? {})
  }
  if (!response.body) throw new ApiError(500, '실시간 응답을 시작할 수 없습니다.')

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  const process = (chunk: string) => {
    let eventName = ''
    const data: string[] = []
    for (const line of chunk.split(/\r?\n/)) {
      if (line.startsWith('event:')) eventName = line.slice(6).trim()
      if (line.startsWith('data:')) data.push(line.slice(5).trim())
    }
    if (!eventName || !data.length) return
    try {
      const parsed = JSON.parse(data.join('\n'))
      if (eventName === 'tool' || eventName === 'completed' || eventName === 'error') {
        onEvent({ type: eventName, data: parsed } as DiagnosisStreamEvent)
      }
    } catch {
      throw new ApiError(500, '실시간 응답 형식을 해석할 수 없습니다.')
    }
  }

  while (true) {
    const { done, value } = await reader.read()
    if (value) buffer += decoder.decode(value, { stream: !done })
    const parts = buffer.split(/\r?\n\r?\n/)
    buffer = parts.pop() ?? ''
    parts.forEach(process)
    if (done) {
      buffer += decoder.decode()
      if (buffer.trim()) process(buffer)
      return
    }
  }
}
