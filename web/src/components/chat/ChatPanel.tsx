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
import type { ResizeDirection } from './usePanelResize'
import { cn } from '@/lib/utils'
import { messageBubble, userBubble } from './chatStyles'

const resizeHandleClass: Record<ResizeDirection, string> = {
  n: 'top-0 right-5 left-5 h-2.5 cursor-ns-resize',
  s: 'right-5 bottom-0 left-5 h-2.5 cursor-ns-resize',
  w: 'top-5 bottom-5 left-0 w-2.5 cursor-ew-resize',
  e: 'top-5 right-0 bottom-5 w-2.5 cursor-ew-resize',
  nw: 'top-0 left-0 z-[26] h-[22px] w-[22px] cursor-nwse-resize',
  ne: 'top-0 right-0 z-[26] h-[22px] w-[22px] cursor-nesw-resize',
  sw: 'bottom-0 left-0 z-[26] h-[22px] w-[22px] cursor-nesw-resize',
  se: 'right-0 bottom-0 z-[26] h-[22px] w-[22px] cursor-nwse-resize',
}
// 좁거나 낮은 화면에서는 크기 조절 없이 화면을 거의 채운다.
const compactScreen = 'max-[540px]:hidden [@media(max-height:480px)]:hidden'
const headerButton = 'inline-flex h-8 min-h-11 w-8 min-w-11 shrink-0 cursor-pointer items-center justify-center rounded-lg text-ink-subtle hover:bg-[#eef1f5] hover:text-ink'

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
    return () => { if (opener?.isConnected) opener.focus(); else document.querySelector<HTMLButtonElement>('[data-chat-launcher]')?.focus() }
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
      className={cn(
        'fixed right-6 bottom-[88px] z-40 flex h-[min(620px,calc(100dvh-120px))] max-h-[calc(100dvh-24px)] min-h-[430px] w-[min(440px,calc(100vw-32px))] max-w-[calc(100vw-24px)] min-w-[340px] touch-none resize-none flex-col overflow-hidden rounded-[28px] border border-[#e0e6ee] bg-[#f7f9fb] shadow-[0_24px_80px_#17243c30]',
        'max-[540px]:right-2 max-[540px]:bottom-2 max-[540px]:h-[min(800px,calc(100dvh-16px))] max-[540px]:min-h-0 max-[540px]:w-[calc(100vw-16px)] max-[540px]:min-w-0 max-[540px]:rounded-3xl',
        '[@media(max-height:480px)]:bottom-2 [@media(max-height:480px)]:h-[calc(100dvh-16px)]',
        isResizing && 'select-none',
        !isOpen && 'hidden',
      )}
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
          className={cn('absolute z-[25] touch-none select-none', resizeHandleClass[direction], compactScreen)}
          onPointerDown={event => handleResizeStart(direction, event)}
          aria-hidden="true"
        />
      ))}
      <div
        className={cn(
          'relative flex min-h-[76px] shrink-0 cursor-grab touch-none items-center justify-between gap-4 border-b border-[#edf0f5] bg-white px-6 py-[18px] select-none active:cursor-grabbing',
          'max-[540px]:min-h-16 max-[540px]:px-[18px] max-[540px]:py-3.5 [@media(max-height:480px)]:min-h-12 [@media(max-height:480px)]:py-1.5',
          isDragging && 'cursor-grabbing',
        )}
        ref={setActivatorNodeRef}
        {...listeners}
        {...attributes}
      >
        <div className="pointer-events-none absolute top-2 left-1/2 h-1 w-9 -translate-x-1/2 rounded-[999px] bg-line-strong" aria-hidden="true" />
        <img className="pointer-events-none block h-auto w-24 select-none" src={brandLogo} alt={t.modal.brand} />
        <div className="flex items-center">
          <button
            className={headerButton}
            type="button"
            aria-label={t.chat.newChat}
            title={t.chat.newChat}
            onClick={startNewChat}
            onPointerDown={event => event.stopPropagation()}
          >
            <RotateCcw size={18} />
          </button>
          <button
            className={headerButton}
            aria-label={t.chat.launcherCloseAria}
            onClick={onClose}
            onPointerDown={event => event.stopPropagation()}
          >
            <X size={20} />
          </button>
        </div>
      </div>
      <div className="shrink-0 px-6 pt-[22px] pb-3.5 max-[540px]:px-[18px] max-[540px]:pt-5 [@media(max-height:480px)]:py-2">
        <h2 className="m-0 text-[18px] font-extrabold tracking-[-.045em] text-[#0f172a]" id="chat-title">{t.chat.headerTitle}</h2>
        <p id="chat-notice" className="mt-1.5 mb-0 border-0 p-0 text-[13px] leading-[1.5] font-medium text-ink-body">{t.chat.notice}</p>
        {usage && <ChatUsage item={usage.diagnosis} text={t.chat.usage} />}
      </div>
      <div className="flex min-h-0 flex-1 flex-col gap-4 overflow-y-auto overscroll-contain px-6 pb-6 max-[540px]:px-[18px]" role="log" aria-label={t.chat.messagesAria} aria-live="polite" aria-relevant="additions" ref={messagesRef}>
        {messages.filter(message => !message.hidden).map(message => (
          <div key={message.id} className={cn('max-w-full shrink-0', message.sender === 'user' ? 'max-w-[88%] self-end' : 'w-full min-w-0')}>
            <div className="w-full min-w-0">
              {Boolean(message.executedTools?.length || message.toolProgress?.length) && <ToolResults tools={message.executedTools} progress={message.toolProgress} text={t.chat.tools} />}
              {message.streaming && <ToolCallingIndicator progress={message.toolProgress} text={t.chat.progress} />}
              {(message.id === 0 || message.text) && <p className={message.sender === 'user' ? cn(messageBubble, userBubble) : messageBubble} role={message.error ? 'alert' : undefined}>{message.id === 0 ? t.chat.welcome : message.text}</p>}
              {message.cards && <DiagnosisCards cards={message.cards} text={t.chat.card} onBook={onBookWithSymptom} onRequestTool={send} onNavigate={onNavigate} />}
            </div>
          </div>
        ))}
      </div>
      <div className="flex shrink-0 gap-1.5 overflow-x-auto border-0 bg-transparent px-6 py-2" aria-label={t.chat.quickQuestionsAria}>
        {t.chat.quickQuestions.map(question => (
          <button
            key={question.label}
            className="min-h-[34px] shrink-0 cursor-pointer rounded-[999px] border-[1.5px] border-line-strong bg-white px-3 py-[7px] text-[12.5px] font-semibold whitespace-nowrap text-ink transition-all duration-150 ease-[ease] enabled:hover:border-[#2455d9] enabled:hover:bg-[#eff6ff] enabled:hover:text-[#2455d9]" disabled={loading || usageExhausted} onClick={() => send(question.symptom)}>
            {question.label}
          </button>
        ))}
      </div>
      <form className="mx-5 mt-3 mb-5 flex shrink-0 items-center gap-2.5 rounded-[20px] border-[1.5px] border-line-strong bg-white py-1.5 pr-1.5 pl-[18px] shadow-[0_4px_12px_#20345b08] transition-[border-color,box-shadow] duration-200 ease-[ease] focus-within:border-[#2455d9] focus-within:shadow-[0_0_0_3px_#2455d918] max-[540px]:mx-3.5 max-[540px]:mt-2.5 max-[540px]:mb-3.5 [@media(max-height:480px)]:my-1.5" onSubmit={event => { event.preventDefault(); send(input) }}>
        <label className="sr-only" htmlFor="chat-input">{t.chat.inputLabel}</label>
        <input
          id="chat-input"
          className="h-10 min-h-10 min-w-0 flex-1 rounded-none border-0 bg-transparent p-0 text-[14.5px] text-[#0f172a] placeholder:text-[14px] placeholder:text-ink-subtle focus:shadow-none focus:outline-none focus-visible:shadow-none focus-visible:outline-none"
          ref={inputRef}
          maxLength={2000}
          value={input}
          onChange={event => setInput(event.target.value)}
          placeholder={exhaustedText || t.chat.inputPlaceholder}
          disabled={usageExhausted}
          autoComplete="off"
        />
        <button
          className="inline-flex aspect-square h-[38px] max-h-[38px] min-h-[38px] w-[38px] max-w-[38px] min-w-[38px] shrink-0 cursor-pointer items-center justify-center rounded-xl border-0 bg-[#2455d9] p-0 text-white [transition:background-color_0.15s_ease,scale_0.1s_ease,opacity_0.15s_ease] enabled:hover:scale-[1.04] enabled:hover:bg-brand-hover enabled:active:scale-[0.96] disabled:cursor-not-allowed disabled:bg-[#e9eff8] disabled:text-[#a0aec0] disabled:opacity-85"
          type="submit"
          disabled={!input.trim() || loading || usageExhausted}
          aria-label={t.chat.sendAria}
        >
          {loading ? (
            <LoaderCircle size={18} className="animate-spin" />
          ) : (
            <ArrowUp size={20} strokeWidth={2.5} />
          )}
        </button>
      </form>
    </section>
  )
}
