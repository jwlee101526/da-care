import { useEffect, useRef } from 'react'
import { useDraggable } from '@dnd-kit/core'
import { ArrowUp, LoaderCircle, RotateCcw, X } from 'lucide-react'
import brandLogo from '../../assets/brand/dacare-logo.svg'
import { useLanguage } from '../../context/LanguageContext'
import { ChatUsage } from './ChatUsage'
import { DiagnosisCards } from './DiagnosisCards'
import { ToolCallingIndicator, ToolResults } from './ToolStatus'
import { useChatSession } from './useChatSession'
import { RESIZE_HANDLES, usePanelResize } from './usePanelResize'
import type { BookWithSymptom, Coordinates, NavigationPage, PanelSize } from './types'

interface ChatPanelProps {
  isOpen: boolean
  onClose: () => void
  onBookWithSymptom: BookWithSymptom
  onNavigate: (page: NavigationPage) => void
  coordinates: Coordinates
  panelRef: React.RefObject<HTMLElement | null>
  panelSize: PanelSize | null
  setPanelSize: (size: PanelSize) => void
  onUpdateCoordinates: (coords: Coordinates) => void
}

export function ChatPanel({
  isOpen,
  onClose,
  onBookWithSymptom,
  onNavigate,
  coordinates,
  panelRef,
  panelSize,
  setPanelSize,
  onUpdateCoordinates,
}: ChatPanelProps) {
  const { t } = useLanguage()
  const { messages, input, setInput, loading, inputRef, usage, usageExhausted, exhaustedText, send, startNewChat } = useChatSession()
  const { isResizing, handleResizeStart } = usePanelResize({ panelRef, coordinates, setPanelSize, onUpdateCoordinates })
  const messagesRef = useRef<HTMLDivElement>(null)

  const { attributes, listeners, setNodeRef, setActivatorNodeRef, isDragging } = useDraggable({
    id: 'chat-panel',
  })

  const handlePanelRef = (node: HTMLElement | null) => {
    setNodeRef(node)
    panelRef.current = node
  }

  const panelStyle: React.CSSProperties = {
    transform: `translate3d(${coordinates.x}px, ${coordinates.y}px, 0)`,
    ...(panelSize ? { width: `${panelSize.width}px`, height: `${panelSize.height}px` } : {}),
  }

  useEffect(() => {
    if (!isOpen) return
    const opener = document.activeElement instanceof HTMLElement ? document.activeElement : null
    inputRef.current?.focus()
    return () => { if (opener?.isConnected) opener.focus(); else document.querySelector<HTMLButtonElement>('.chat-launcher')?.focus() }
  }, [isOpen, inputRef])

  useEffect(() => {
    const list = messagesRef.current
    if (list) list.scrollTop = list.scrollHeight
  }, [messages])

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
