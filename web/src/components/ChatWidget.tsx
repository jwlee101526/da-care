import { useEffect, useRef, useState } from 'react'
import {
  DndContext,
  useDraggable,
  useSensor,
  useSensors,
  PointerSensor,
  type DragEndEvent,
  type DragMoveEvent,
} from '@dnd-kit/core'
import { ArrowRight, ArrowUp, BookOpen, CalendarDays, CheckCircle2, ClipboardCheck, LoaderCircle, MessageCircle, RotateCcw, Wrench, X } from 'lucide-react'
import brandLogo from '../assets/brand/dacare-logo.svg'
import type { ReservationSelection } from '../types'
import { useLanguage } from '../context/LanguageContext'
import { ApiError, streamDiagnosis } from '../lib/api'
import type { DiagnosisCard, DiagnosisToolProgress } from '../lib/api'
import { useAuth } from '../context/AuthContext'
import { useNavigate } from 'react-router-dom'
import './ChatWidget.css'

interface Message {
  id: number
  sender: 'user' | 'bot'
  text: string
  cards?: DiagnosisCard[]
  executedTools?: string[]
  toolProgress?: DiagnosisToolProgress[]
  streaming?: boolean
  error?: boolean
}

const CHAT_SESSION_KEY = 'dacare.chat.messages.v1'

function readChatMessages(): Message[] | null {
  try {
    const value: unknown = JSON.parse(window.sessionStorage.getItem(CHAT_SESSION_KEY) ?? 'null')
    if (!Array.isArray(value) || !value.every(message => message && typeof message === 'object'
      && typeof message.id === 'number' && (message.sender === 'user' || message.sender === 'bot')
      && typeof message.text === 'string')) return null
    return value as Message[]
  } catch {
    return null
  }
}

interface ChatWidgetProps {
  isOpen: boolean
  onToggle: () => void
  onBookWithSymptom: (selection: ReservationSelection) => void
}

interface DraggableChatLauncherProps {
  isOpen: boolean
  onToggle: () => void
  currentX: number
  currentY: number
  launcherRef: React.RefObject<HTMLButtonElement | null>
}

function DraggableChatLauncher({
  isOpen,
  onToggle,
  currentX,
  currentY,
  launcherRef,
}: DraggableChatLauncherProps) {
  const { t } = useLanguage()
  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
    id: 'chat-launcher',
  })

  const handleRef = (node: HTMLButtonElement | null) => {
    setNodeRef(node)
    launcherRef.current = node
  }

  const style: React.CSSProperties = {
    transform: `translate3d(${currentX}px, ${currentY}px, 0)`,
  }

  return (
    <button
      ref={handleRef}
      style={style}
      className={`chat-launcher${isDragging ? ' is-dragging' : ''}`}
      aria-label={isOpen ? t.chat.launcherCloseAria : t.chat.launcherOpenAria}
      aria-expanded={isOpen}
      aria-controls={isOpen ? 'chat-panel' : undefined}
      onClick={onToggle}
      {...listeners}
      {...attributes}
    >
      <MessageCircle size={26} strokeWidth={2.2} />
      <span>{t.chat.launcher}</span>
    </button>
  )
}

function clampCoordinates(
  targetElement: HTMLElement | null,
  targetX: number,
  targetY: number
) {
  if (!targetElement) {
    return { x: targetX, y: targetY }
  }

  const style = window.getComputedStyle(targetElement)
  const right = parseFloat(style.right) || 24
  const bottom = parseFloat(style.bottom) || 24
  const width = targetElement.offsetWidth || 390
  const height = targetElement.offsetHeight || 580

  const minX = 12 + right + width - window.innerWidth
  const maxX = Math.max(0, right - 12)
  const minY = 12 + bottom + height - window.innerHeight
  const maxY = Math.max(0, bottom - 12)

  const safeMinX = Math.min(minX, maxX)
  const safeMaxX = Math.max(minX, maxX)
  const safeMinY = Math.min(minY, maxY)
  const safeMaxY = Math.max(minY, maxY)

  return {
    x: Math.min(Math.max(targetX, safeMinX), safeMaxX),
    y: Math.min(Math.max(targetY, safeMinY), safeMaxY),
  }
}

type ResizeDirection = 'n' | 's' | 'e' | 'w' | 'nw' | 'ne' | 'sw' | 'se'

const RESIZE_HANDLES: ResizeDirection[] = ['nw', 'n', 'ne', 'w', 'e', 'sw', 's', 'se']

export function ChatWidget({ isOpen, onToggle, onBookWithSymptom }: ChatWidgetProps) {
  const navigate = useNavigate()
  const [coordinates, setCoordinates] = useState({ x: 0, y: 0 })
  const [panelSize, setPanelSize] = useState<{ width: number; height: number } | null>(null)
  const [dragDelta, setDragDelta] = useState<{ x: number; y: number } | null>(null)
  const launcherRef = useRef<HTMLButtonElement | null>(null)
  const panelRef = useRef<HTMLElement | null>(null)
  const isDraggingRef = useRef(false)

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 6,
      },
    })
  )

  useEffect(() => {
    const handleResize = () => {
      setCoordinates(prev => {
        const activeTarget = (isOpen && panelRef.current) ? panelRef.current : launcherRef.current
        return clampCoordinates(activeTarget, prev.x, prev.y)
      })
      setPanelSize(prev => {
        if (!prev) return null
        const maxW = Math.max(340, window.innerWidth - 24)
        const maxH = Math.max(430, window.innerHeight - 24)
        if (prev.width > maxW || prev.height > maxH) {
          return {
            width: Math.min(prev.width, maxW),
            height: Math.min(prev.height, maxH),
          }
        }
        return prev
      })
    }

    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [isOpen])

  useEffect(() => {
    if (isOpen) {
      const raf = requestAnimationFrame(() => {
        if (panelRef.current) {
          setCoordinates(prev => clampCoordinates(panelRef.current, prev.x, prev.y))
        }
      })
      return () => cancelAnimationFrame(raf)
    }
  }, [isOpen])

  const handleDragStart = () => {
    isDraggingRef.current = true
    setDragDelta({ x: 0, y: 0 })
  }

  const handleDragMove = (event: DragMoveEvent) => {
    const activeTarget = (isOpen && panelRef.current) ? panelRef.current : launcherRef.current
    const next = clampCoordinates(activeTarget, coordinates.x + event.delta.x, coordinates.y + event.delta.y)
    setDragDelta({ x: next.x - coordinates.x, y: next.y - coordinates.y })
  }

  const handleDragEnd = (event: DragEndEvent) => {
    setDragDelta(null)
    if (event.delta) {
      setCoordinates(prev => {
        const activeTarget = (isOpen && panelRef.current) ? panelRef.current : launcherRef.current
        return clampCoordinates(activeTarget, prev.x + event.delta.x, prev.y + event.delta.y)
      })
    }
    setTimeout(() => {
      isDraggingRef.current = false
    }, 100)
  }

  const handleDragCancel = () => {
    setDragDelta(null)
    setTimeout(() => {
      isDraggingRef.current = false
    }, 100)
  }

  const currentX = coordinates.x + (dragDelta ? dragDelta.x : 0)
  const currentY = coordinates.y + (dragDelta ? dragDelta.y : 0)

  return (
    <DndContext
      sensors={sensors}
      autoScroll={false}
      onDragStart={handleDragStart}
      onDragMove={handleDragMove}
      onDragEnd={handleDragEnd}
      onDragCancel={handleDragCancel}
    >
      <DraggableChatLauncher
        isOpen={isOpen}
        onToggle={() => {
          if (!isDraggingRef.current) {
            onToggle()
          }
        }}
        currentX={currentX}
        currentY={currentY}
        launcherRef={launcherRef}
      />
      <ChatPanel
        isOpen={isOpen}
        onClose={onToggle}
        onBookWithSymptom={onBookWithSymptom}
        onNavigate={page => { navigate(`/${page}`); onToggle() }}
        currentX={currentX}
        currentY={currentY}
        panelRef={panelRef}
        panelSize={panelSize}
        setPanelSize={setPanelSize}
        onUpdateCoordinates={coords => setCoordinates(coords)}
      />
    </DndContext>
  )
}

function ChatPanel({
  isOpen,
  onClose,
  onBookWithSymptom,
  onNavigate,
  currentX,
  currentY,
  panelRef,
  panelSize,
  setPanelSize,
  onUpdateCoordinates,
}: {
  isOpen: boolean
  onClose: () => void
  onBookWithSymptom: ChatWidgetProps['onBookWithSymptom']
  onNavigate: (page: 'reservations') => void
  currentX: number
  currentY: number
  panelRef: React.RefObject<HTMLElement | null>
  panelSize: { width: number; height: number } | null
  setPanelSize: React.Dispatch<React.SetStateAction<{ width: number; height: number } | null>>
  onUpdateCoordinates: (coords: { x: number; y: number }) => void
}) {
  const { t } = useLanguage()
  const { token } = useAuth()
  const [messages, setMessages] = useState<Message[]>(() => readChatMessages() ?? [
    { id: 0, sender: 'bot', text: t.chat.welcome }
  ])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [isResizing, setIsResizing] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)
  const messagesRef = useRef<HTMLDivElement>(null)
  const nextId = useRef(Math.max(0, ...messages.map(message => message.id)) + 1)
  const requestRef = useRef<AbortController | null>(null)

  const { attributes, listeners, setNodeRef, setActivatorNodeRef, isDragging } = useDraggable({
    id: 'chat-panel',
  })

  const handlePanelRef = (node: HTMLElement | null) => {
    setNodeRef(node)
    panelRef.current = node
  }

  const handleResizeStart = (
    direction: ResizeDirection,
    event: React.PointerEvent<HTMLDivElement>
  ) => {
    if (event.button !== 0) return
    if (!panelRef.current) return
    event.preventDefault()
    event.stopPropagation()

    const startRect = panelRef.current.getBoundingClientRect()
    const startPointerX = event.clientX
    const startPointerY = event.clientY
    const startCoordX = currentX
    const startCoordY = currentY

    setIsResizing(true)

    const handlePointerMove = (e: PointerEvent) => {
      e.preventDefault()
      const dx = e.clientX - startPointerX
      const dy = e.clientY - startPointerY

      let newWidth = startRect.width
      let newHeight = startRect.height
      let nextX = startCoordX
      let nextY = startCoordY

      const minW = 340
      const minH = 430
      const screenMargin = 12

      // Vertical resizing
      if (direction.includes('n')) {
        const targetTop = startRect.top + dy
        const maxTop = startRect.bottom - minH
        const clampedTop = Math.min(maxTop, Math.max(screenMargin, targetTop))
        newHeight = Math.round(startRect.bottom - clampedTop)
      } else if (direction.includes('s')) {
        const targetBottom = startRect.bottom + dy
        const minBottom = startRect.top + minH
        const clampedBottom = Math.max(minBottom, Math.min(window.innerHeight - screenMargin, targetBottom))
        newHeight = Math.round(clampedBottom - startRect.top)
        nextY = Math.round(clampedBottom - (window.innerHeight - 88))
      }

      // Horizontal resizing
      if (direction.includes('w')) {
        const targetLeft = startRect.left + dx
        const maxLeft = startRect.right - minW
        const clampedLeft = Math.min(maxLeft, Math.max(screenMargin, targetLeft))
        newWidth = Math.round(startRect.right - clampedLeft)
      } else if (direction.includes('e')) {
        const targetRight = startRect.right + dx
        const minRight = startRect.left + minW
        const clampedRight = Math.max(minRight, Math.min(window.innerWidth - screenMargin, targetRight))
        newWidth = Math.round(clampedRight - startRect.left)
        nextX = Math.round(clampedRight - (window.innerWidth - 24))
      }

      setPanelSize({ width: newWidth, height: newHeight })
      if (nextX !== startCoordX || nextY !== startCoordY) {
        onUpdateCoordinates({ x: nextX, y: nextY })
      }
    }

    const handlePointerUp = () => {
      setIsResizing(false)
      window.removeEventListener('pointermove', handlePointerMove)
      window.removeEventListener('pointerup', handlePointerUp)
      window.removeEventListener('pointercancel', handlePointerUp)
    }

    window.addEventListener('pointermove', handlePointerMove)
    window.addEventListener('pointerup', handlePointerUp)
    window.addEventListener('pointercancel', handlePointerUp)
  }

  const panelStyle: React.CSSProperties = {
    transform: `translate3d(${currentX}px, ${currentY}px, 0)`,
    ...(panelSize ? { width: `${panelSize.width}px`, height: `${panelSize.height}px` } : {}),
  }

  useEffect(() => () => requestRef.current?.abort(), [])

  useEffect(() => {
    if (!isOpen) return
    const opener = document.activeElement instanceof HTMLElement ? document.activeElement : null
    inputRef.current?.focus()
    return () => { if (opener?.isConnected) opener.focus(); else document.querySelector<HTMLButtonElement>('.chat-launcher')?.focus() }
  }, [isOpen])

  useEffect(() => {
    const list = messagesRef.current
    if (list) list.scrollTop = list.scrollHeight
  }, [messages])

  useEffect(() => {
    try {
      window.sessionStorage.setItem(CHAT_SESSION_KEY, JSON.stringify(messages.slice(-20)))
    } catch {
      // 저장소를 사용할 수 없어도 현재 대화는 계속 진행합니다.
    }
  }, [messages])

  async function send(text: string, showUserMessage = true) {
    const symptom = text.trim()
    if (!symptom || requestRef.current) return
    const controller = new AbortController()
    requestRef.current = controller
    const timeout = window.setTimeout(() => controller.abort(), 90000)
    const userMessage: Message = { id: nextId.current++, sender: 'user', text: symptom }
    const responseId = nextId.current++
    const history = messages.filter(message => message.id !== 0 && !message.error).slice(-12)
      .map(message => ({ role: message.sender === 'bot' ? 'assistant' as const : 'user' as const, text: message.text.slice(0, 4000) }))
    setMessages(previous => [...previous.filter(message => message.id !== 0), ...(showUserMessage ? [userMessage] : []), {
      id: responseId, sender: 'bot', text: '', toolProgress: [], streaming: true,
    }])
    setInput('')
    setLoading(true)
    try {
      let completed = false
      let failed = false
      await streamDiagnosis(symptom, history, token, controller.signal, event => {
        if (event.type === 'tool') {
          setMessages(previous => previous.map(message => {
            if (message.id !== responseId) return message
            const progress = [...(message.toolProgress ?? [])]
            const index = progress.findIndex(item => item.tool === event.data.tool)
            if (index >= 0) progress[index] = event.data
            else progress.push(event.data)
            const cards = event.data.card
              ? [...(message.cards ?? []), event.data.card].filter((card, cardIndex, items) => items.findIndex(item => item.type === card.type) === cardIndex)
              : message.cards
            return { ...message, toolProgress: progress, cards }
          }))
          return
        }
        if (event.type === 'completed') {
          completed = true
          setMessages(previous => previous.map(message => message.id === responseId ? {
            ...message, text: event.data.answer, cards: event.data.cards,
            executedTools: event.data.executedTools, streaming: false,
          } : message))
          return
        }
        failed = true
        setMessages(previous => previous.map(message => message.id === responseId ? {
          ...message, error: true, text: event.data.message, streaming: false,
        } : message))
      })
      if (!completed && !failed) throw new ApiError(500, '실시간 응답이 예기치 않게 종료되었습니다.')
    } catch (error) {
      setMessages(previous => previous.map(message => message.id === responseId ? {
        ...message, error: true, streaming: false,
        text: error instanceof ApiError ? error.message : controller.signal.aborted
          ? '진단 응답 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.'
          : '진단 서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.',
      } : message))
    } finally {
      window.clearTimeout(timeout)
      if (requestRef.current === controller) {
        requestRef.current = null
        setLoading(false)
      }
    }
    inputRef.current?.focus()
  }

  function startNewChat() {
    requestRef.current?.abort()
    requestRef.current = null
    nextId.current = 1
    setLoading(false)
    setInput('')
    setMessages([{ id: 0, sender: 'bot', text: t.chat.welcome }])
    inputRef.current?.focus()
  }

  return (
    <section
      id="chat-panel"
      ref={handlePanelRef}
      style={panelStyle}
      className={`chat-panel${isDragging ? ' is-dragging' : ''}${isResizing ? ' is-resizing' : ''}${isOpen ? '' : ' is-closed'}`}
      role="dialog"
      aria-modal="false"
      aria-hidden={!isOpen}
      aria-labelledby="chat-title"
      aria-describedby="chat-notice"
      onKeyDown={event => {
        if (event.key === 'Escape') {
          event.stopPropagation()
          onClose()
        }
      }}
    >
      {RESIZE_HANDLES.map(direction => (
        <div
          key={direction}
          className={`chat-resize-handle chat-resize-handle-${direction}`}
          onPointerDown={event => handleResizeStart(direction, event)}
          aria-hidden="true"
        />
      ))}
      <div
        className="chat-header"
        ref={setActivatorNodeRef}
        {...listeners}
        {...attributes}
      >
        <div className="chat-header-pill" aria-hidden="true" />
        <img className="chat-brand" src={brandLogo} alt={t.modal.brand} />
        <div className="chat-header-actions">
          <button
            className="icon-button"
            type="button"
            aria-label="새 대화 시작"
            title="새 대화 시작"
            onClick={startNewChat}
            onPointerDown={event => event.stopPropagation()}
          >
            <RotateCcw size={18} />
          </button>
          <button
            className="icon-button"
            aria-label={t.chat.launcherCloseAria}
            onClick={onClose}
            onPointerDown={event => event.stopPropagation()}
          >
            <X size={20} />
          </button>
        </div>
      </div>
      <div className="chat-intro">
        <h2 id="chat-title">{t.chat.headerTitle}</h2>
        <p id="chat-notice" className="chat-notice">{t.chat.notice}</p>
      </div>
      <div className="chat-messages" role="log" aria-label="Messages" aria-live="polite" aria-relevant="additions" ref={messagesRef}>
        {messages.map(message => (
          <div key={message.id} className={'chat-message ' + message.sender}>
            <div className="chat-message-content">
              {Boolean(message.executedTools?.length || message.toolProgress?.length) && <ToolResults tools={message.executedTools} progress={message.toolProgress} />}
              {message.streaming && <ToolCallingIndicator progress={message.toolProgress} />}
              {(message.id === 0 || message.text) && <p role={message.error ? 'alert' : undefined}>{message.id === 0 ? t.chat.welcome : message.text}</p>}
              {message.cards && <DiagnosisCards cards={message.cards} onBook={onBookWithSymptom} onRequestTool={send} onNavigate={onNavigate} />}
            </div>
          </div>
        ))}
      </div>
      <div className="quick-questions" aria-label="Quick questions">
        {t.chat.quickQuestions.map(question => (
          <button key={question.label} disabled={loading} onClick={() => send(question.symptom)}>
            {question.label}
          </button>
        ))}
      </div>
      <form className="chat-form" onSubmit={event => { event.preventDefault(); send(input) }}>
        <label className="sr-only" htmlFor="chat-input">{t.modal.symptomLegend}</label>
        <input
          id="chat-input"
          ref={inputRef}
          maxLength={2000}
          value={input}
          onChange={event => setInput(event.target.value)}
          placeholder={t.chat.inputPlaceholder}
          autoComplete="off"
        />
        <button
          className="chat-send-btn"
          type="submit"
          disabled={!input.trim() || loading}
          aria-label={t.chat.sendAria}
        >
          {loading ? (
            <LoaderCircle size={18} className="chat-send-spinner" />
          ) : (
            <ArrowUp size={20} strokeWidth={2.5} />
          )}
        </button>
      </form>
    </section>
  )
}

function ToolCallingIndicator({ progress = [] }: { progress?: DiagnosisToolProgress[] }) {
  const latest = progress.at(-1)
  const status = latest?.status === 'started'
    ? {
        searchManuals: ['매뉴얼을 검색하고 있습니다', '관련 문서를 확인하고 있습니다.'],
        showInspectionCard: ['점검 안내를 만들고 있습니다', '매뉴얼 근거를 정리하고 있습니다.'],
        prepareReservation: ['예약 안내를 준비하고 있습니다', '예약 정보를 입력하고 있습니다.'],
        getReservationStatus: ['예약 상태를 조회하고 있습니다', '실제 접수 정보를 확인하고 있습니다.'],
        navigateTo: ['이동 안내를 준비하고 있습니다', '요청한 페이지를 확인하고 있습니다.'],
      }[latest.tool]
    : latest?.tool === 'searchManuals'
      ? ['검색 결과를 분석하고 있습니다', '점검 안내 또는 다음 행동을 준비하고 있습니다.']
      : latest?.tool === 'showInspectionCard'
        ? ['답변을 정리하고 있습니다', '점검 안내 내용을 바탕으로 다음 절차를 정리하고 있습니다.']
        : latest?.tool === 'prepareReservation'
          ? ['예약 안내를 정리하고 있습니다', '다음 절차를 안내하고 있습니다.']
          : latest?.tool === 'getReservationStatus'
            ? ['조회 결과를 정리하고 있습니다', '예약 상태 안내를 작성하고 있습니다.']
            : latest?.tool === 'navigateTo'
              ? ['이동 안내를 정리하고 있습니다', '요청한 페이지로 안내하고 있습니다.']
              : ['요청을 분석하고 있습니다', '필요한 매뉴얼과 다음 행동을 확인하고 있습니다.']
  const [title, description] = status ?? ['요청을 처리하고 있습니다', '다음 단계를 준비하고 있습니다.']
  return (
    <div className="tool-progress" aria-label={title}>
      <LoaderCircle className="tool-spinner" size={15} aria-hidden="true" />
      <div><strong>{title}</strong><span>{description}</span></div>
    </div>
  )
}

function ToolResults({ tools = [], progress = [] }: { tools?: string[]; progress?: DiagnosisToolProgress[] }) {
  const labels: Record<string, string> = {
    searchManuals: '매뉴얼 검색 완료',
    showInspectionCard: '점검 안내 생성 완료',
    prepareReservation: '방문 점검 신청 안내 완료',
    getReservationStatus: '예약 상태 조회 완료',
    navigateTo: '페이지 이동 안내 완료',
  }
  const statuses = progress.filter(item => labels[item.tool]).map(item => ({ tool: item.tool, status: item.status }))
  for (const tool of [...new Set(tools)].filter(tool => labels[tool] && !statuses.some(item => item.tool === tool))) {
    statuses.push({ tool, status: 'completed' })
  }
  if (!statuses.length) return null
  return <ul className="chat-tool-results" aria-label="실행된 도구">
    {statuses.map(({ tool, status }) => <li key={tool} className={status === 'started' ? 'is-started' : ''}>
      {status === 'started' ? <LoaderCircle className="tool-result-spinner" size={14} aria-hidden="true" /> : <CheckCircle2 size={14} aria-hidden="true" />}
      {labels[tool]}<span>{status === 'started' ? '처리 중' : '완료'}</span>
    </li>)}
  </ul>
}

function DiagnosisCards({ cards, onBook, onRequestTool, onNavigate }: {
  cards: DiagnosisCard[]
  onBook: ChatWidgetProps['onBookWithSymptom']
  onRequestTool: (message: string, showUserMessage?: boolean) => void
  onNavigate: (page: 'reservations') => void
}) {
  const { t } = useLanguage()
  const statusLabels = { PENDING: '접수 대기', CONFIRMED: '예약 확정', COMPLETED: '점검 완료', CANCELLED: '취소됨' }
  return (
    <div className="diagnosis-cards">
      {cards.map((card, index) => {
        if (card.type === 'inspection') return (
          <article className="diagnosis-card visit-card" key={index}>
            <div className="visit-card-heading"><strong><ClipboardCheck size={18} aria-hidden="true" />{card.title}</strong><span className="visit-recommendation">매뉴얼 기반</span></div>
            <dl><div><dt>대상 기기</dt><dd>{card.deviceName}</dd></div>{card.suspectedCause && <div><dt>추정 원인</dt><dd>{card.suspectedCause}</dd></div>}<div><dt>점검 내용</dt><dd>{card.inspectionDetails}</dd></div></dl>
            <details className="diagnosis-evidence"><summary><BookOpen size={15} aria-hidden="true" />참고 매뉴얼 <span>{card.evidence.length}건</span></summary>{card.evidence.map((source, sourceIndex) => <div className="diagnosis-source" key={source.sourceId}><strong>근거 {sourceIndex + 1}</strong><blockquote>{source.quote}</blockquote><small>문서 {source.sourceId}</small></div>)}</details>
            <CardNextAction
              label="방문 점검 준비"
              detail="예약 정보를 미리 입력하고 다음 절차를 안내합니다."
              onClick={() => onRequestTool('방문 점검 예약을 준비해 주세요.', false)}
            />
          </article>
        )
        if (card.type === 'booking') return (
          <article className="diagnosis-card visit-card" key={index}>
            <div className="visit-card-heading"><strong><CalendarDays size={17} /> 방문 점검 신청</strong></div>
            <dl><div><dt>대상 기기</dt><dd>{(t.modal.devices as Record<string, string>)[card.deviceType] || card.deviceType}</dd></div><div><dt>접수 증상</dt><dd>{card.symptom}</dd></div></dl>
            <small>선택하신 기기와 증상은 다음 단계에 미리 입력해 두겠습니다. 방문을 원하는 날짜와 장소를 입력하면 예약을 신청할 수 있습니다.</small>
            <button className="chat-booking" onClick={() => onBook({ device: card.deviceType, symptom: card.symptom })}>예약 계속하기 <ArrowRight size={17} /></button>
          </article>
        )
        if (card.type === 'reservation_status') return (
          <article className="diagnosis-card visit-card" key={index}>
            <div className="visit-card-heading"><strong>예약 #{card.reservationId}</strong><span className="ready-badge">{statusLabels[card.status]}</span></div>
            <dl><div><dt>희망 일시</dt><dd>{card.preferredAt.replace('T', ' ')}</dd></div><div><dt>확정 일시</dt><dd>{card.confirmedAt?.replace('T', ' ') ?? '미확정'}</dd></div><div><dt>엔지니어</dt><dd>{card.engineerName ?? '미배정'}</dd></div></dl>
          </article>
        )
        if (card.type === 'navigation') return (
          <article className="diagnosis-card navigation-card" key={index}>
            <div className="visit-card-heading"><strong><CalendarDays size={18} aria-hidden="true" />{card.title}</strong></div>
            <p>{card.description}</p>
            <button className="chat-booking" onClick={() => onNavigate(card.page)}>{card.actionLabel}<ArrowRight size={17} /></button>
          </article>
        )
        return null
      })}
    </div>
  )
}

function CardNextAction({ label, detail, onClick }: { label: string; detail: string; onClick: () => void }) {
  return (
    <div className="card-next-action">
      <div><span>다음 행동</span><small>{detail}</small></div>
      <button type="button" onClick={onClick}><Wrench size={14} aria-hidden="true" />{label}<ArrowRight size={14} aria-hidden="true" /></button>
    </div>
  )
}
