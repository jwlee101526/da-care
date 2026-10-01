import { useCallback, useEffect, useRef, useState } from 'react'
import {
  DndContext,
  useDraggable,
  useSensor,
  useSensors,
  PointerSensor,
  type DragEndEvent,
  type DragMoveEvent,
} from '@dnd-kit/core'
import { ArrowRight, ArrowUp, CalendarDays, CheckCircle2, ChevronDown, ClipboardCheck, LoaderCircle, MessageCircle, RotateCcw, Wrench, X } from 'lucide-react'
import brandLogo from '../assets/brand/dacare-logo.svg'
import type { ReservationSelection } from '../types'
import { useLanguage } from '../context/LanguageContext'
import { ApiError, fetchMyUsage, streamDiagnosis } from '../lib/api'
import type { DiagnosisCard, DiagnosisToolProgress, UsageItem } from '../lib/api'
import { getCategoryInfo } from '../lib/categories'
import { useAuth } from '../context/AuthContext'
import { useUsage } from '../hooks/useUsage'
import { notifyUsage } from '../lib/usageToast'
import type { LocaleDict } from '../locales/ko'
import { toast } from 'sonner'
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
  /** 카드 버튼으로 보낸 요청. 화면에는 표시하지 않지만 다음 요청의 대화 이력에는 포함한다. */
  hidden?: boolean
}

type ChatText = LocaleDict['chat']
type ToolName = keyof ChatText['tools']['labels']
type NavigationPage = Extract<DiagnosisCard, { type: 'navigation' }>['page']

function isToolName(tool: string, labels: ChatText['tools']['labels']): tool is ToolName {
  return tool in labels
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
  onNavigate: (page: NavigationPage) => void
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
  const { usage, refresh: refreshUsage } = useUsage(useCallback(() => fetchMyUsage(token), [token]))
  const usageExhausted = usage?.diagnosis.remaining === 0
  const exhaustedText = usage && usageExhausted ? exhaustedMessage(usage.diagnosis, !token, t.chat.usage) : ''

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
    if (!symptom || requestRef.current || usageExhausted) return
    const controller = new AbortController()
    requestRef.current = controller
    // 서버 상담 기한(75초) 이후 도착하는 시간 초과 안내를 받을 수 있도록 여유를 둔다.
    const timeout = window.setTimeout(() => controller.abort(), 90000)
    const userMessage: Message = { id: nextId.current++, sender: 'user', text: symptom, hidden: !showUserMessage }
    const responseId = nextId.current++
    const history = messages.filter(message => message.id !== 0 && !message.error).slice(-12)
      .map(message => ({ role: message.sender === 'bot' ? 'assistant' as const : 'user' as const, text: message.text.slice(0, 4000) }))
    setMessages(previous => [...previous.filter(message => message.id !== 0), userMessage, {
      id: responseId, sender: 'bot', text: '', toolProgress: [], streaming: true,
    }])
    setInput('')
    setLoading(true)
    let completed = false
    let limitMessage = ''
    try {
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
        if (event.data.code === 'WEEKLY_LIMIT_EXCEEDED' || event.data.code === 'SERVICE_LIMIT_EXCEEDED') limitMessage = event.data.message
        setMessages(previous => previous.map(message => message.id === responseId ? {
          ...message, error: true, text: event.data.message, streaming: false,
        } : message))
      })
      if (!completed && !failed) throw new ApiError(500, t.chat.errors.interrupted)
    } catch (error) {
      setMessages(previous => previous.map(message => message.id === responseId ? {
        ...message, error: true, streaming: false,
        text: error instanceof ApiError ? error.message
          : controller.signal.aborted ? t.chat.errors.timeout : t.chat.errors.network,
      } : message))
    } finally {
      window.clearTimeout(timeout)
      if (requestRef.current === controller) {
        requestRef.current = null
        setLoading(false)
      }
    }
    // 상담 1건이 끝나면 이번 주 사용량을 다시 조회해 남은 횟수 표시와 알림에 반영한다.
    const nextUsage = await refreshUsage()
    if (limitMessage) toast.error(t.chat.usage.exhausted, { description: limitMessage })
    else if (completed && nextUsage) notifyUsage(nextUsage.diagnosis, t.chat.usage)
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
            aria-label={t.chat.newChat}
            title={t.chat.newChat}
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
        {usage && <ChatUsage item={usage.diagnosis} text={t.chat.usage} />}
      </div>
      <div className="chat-messages" role="log" aria-label={t.chat.messagesAria} aria-live="polite" aria-relevant="additions" ref={messagesRef}>
        {messages.filter(message => !message.hidden).map(message => (
          <div key={message.id} className={'chat-message ' + message.sender}>
            <div className="chat-message-content">
              {Boolean(message.executedTools?.length || message.toolProgress?.length) && <ToolResults tools={message.executedTools} progress={message.toolProgress} text={t.chat.tools} />}
              {message.streaming && <ToolCallingIndicator progress={message.toolProgress} text={t.chat.progress} />}
              {(message.id === 0 || message.text) && <p role={message.error ? 'alert' : undefined}>{message.id === 0 ? t.chat.welcome : message.text}</p>}
              {message.cards && <DiagnosisCards cards={message.cards} text={t.chat.card} onBook={onBookWithSymptom} onRequestTool={send} onNavigate={onNavigate} />}
            </div>
          </div>
        ))}
      </div>
      <div className="quick-questions" aria-label={t.chat.quickQuestionsAria}>
        {t.chat.quickQuestions.map(question => (
          <button key={question.label} disabled={loading || usageExhausted} onClick={() => send(question.symptom)}>
            {question.label}
          </button>
        ))}
      </div>
      <form className="chat-form" onSubmit={event => { event.preventDefault(); send(input) }}>
        <label className="sr-only" htmlFor="chat-input">{t.chat.inputLabel}</label>
        <input
          id="chat-input"
          ref={inputRef}
          maxLength={2000}
          value={input}
          onChange={event => setInput(event.target.value)}
          placeholder={exhaustedText || t.chat.inputPlaceholder}
          disabled={usageExhausted}
          autoComplete="off"
        />
        <button
          className="chat-send-btn"
          type="submit"
          disabled={!input.trim() || loading || usageExhausted}
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

/**
 * 오늘 남은 상담 횟수. 배지 대신 문구와 얇은 막대로 남은 비율을 보여 주고, 얼마 남지 않으면 색으로 알린다.
 */
/**
 * 상담을 더 할 수 없는 이유. 개인 한도가 남았는데 막혔다면 서비스 전체 한도가 소진된 것이다.
 */
function exhaustedMessage(item: UsageItem, guest: boolean, text: ChatText['usage']) {
  if (item.used < item.limit) return text.serviceExhausted
  return guest ? text.guestExhausted : text.exhaustedPlaceholder
}

function ChatUsage({ item, text }: { item: UsageItem; text: ChatText['usage'] }) {
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

function ToolCallingIndicator({ progress = [], text }: { progress?: DiagnosisToolProgress[]; text: ChatText['progress'] }) {
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

function ToolResults({ tools = [], progress = [], text }: { tools?: string[]; progress?: DiagnosisToolProgress[]; text: ChatText['tools'] }) {
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

function DiagnosisCards({ cards, text, onBook, onRequestTool, onNavigate }: {
  cards: DiagnosisCard[]
  text: ChatText['card']
  onBook: ChatWidgetProps['onBookWithSymptom']
  onRequestTool: (message: string, showUserMessage?: boolean) => void
  onNavigate: (page: NavigationPage) => void
}) {
  const { lang } = useLanguage()
  return (
    <div className="diagnosis-cards">
      {cards.map((card, index) => {
        if (card.type === 'inspection') return (
          <article className="diagnosis-card visit-card" key={index}>
            <div className="visit-card-heading"><strong><ClipboardCheck size={18} aria-hidden="true" />{card.title}</strong><span className="visit-recommendation">{text.manualBased}</span></div>
            <dl><div><dt>{text.device}</dt><dd>{card.deviceName}</dd></div>{card.suspectedCause && <div><dt>{text.cause}</dt><dd>{card.suspectedCause}</dd></div>}<div><dt>{text.inspection}</dt><dd>{card.inspectionDetails}</dd></div></dl>
            {card.evidence.length > 0 && (
              <details className="diagnosis-evidence">
                <summary>{text.evidence}<span>{card.evidence.length}</span><ChevronDown size={15} aria-hidden="true" /></summary>
                {card.evidence.map(source => (
                  <figure key={source.sourceId}>
                    <blockquote>{source.quote}</blockquote>
                    <figcaption>{text.source(source.sourceId)}</figcaption>
                  </figure>
                ))}
              </details>
            )}
            <CardNextAction
              title={text.nextAction}
              label={text.prepareVisit}
              detail={text.prepareVisitDetail}
              onClick={() => onRequestTool(text.prepareVisitRequest, false)}
            />
          </article>
        )
        if (card.type === 'booking') return (
          <article className="diagnosis-card visit-card" key={index}>
            <div className="visit-card-heading"><strong><CalendarDays size={17} /> {text.bookingTitle}</strong></div>
            <dl><div><dt>{text.device}</dt><dd>{getCategoryInfo(card.deviceType, lang)}</dd></div><div><dt>{text.symptom}</dt><dd>{card.symptom}</dd></div></dl>
            <small>{text.bookingDescription}</small>
            <button className="chat-booking" onClick={() => onBook({ device: card.deviceType, symptom: card.symptom })}>{text.continueBooking} <ArrowRight size={17} /></button>
          </article>
        )
        if (card.type === 'reservation_status') return (
          <article className="diagnosis-card visit-card" key={index}>
            <div className="visit-card-heading"><strong>{text.reservation(card.reservationId)}</strong><span className={'reservation-status is-' + card.status.toLowerCase()}>{text.status[card.status]}</span></div>
            <dl><div><dt>{text.preferredAt}</dt><dd>{card.preferredAt.replace('T', ' ')}</dd></div><div><dt>{text.confirmedAt}</dt><dd>{card.confirmedAt?.replace('T', ' ') ?? text.notConfirmed}</dd></div><div><dt>{text.engineer}</dt><dd>{card.engineerName ?? text.notAssigned}</dd></div></dl>
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

function CardNextAction({ title, label, detail, onClick }: { title: string; label: string; detail: string; onClick: () => void }) {
  return (
    <div className="card-next-action">
      <div><span>{title}</span><small>{detail}</small></div>
      <button type="button" onClick={onClick}><Wrench size={14} aria-hidden="true" />{label}<ArrowRight size={14} aria-hidden="true" /></button>
    </div>
  )
}
