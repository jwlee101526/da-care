import { useEffect, useRef, useState } from 'react'
import { ArrowUpRight, MessageCircle, Send, X } from 'lucide-react'
import type { ReservationSelection } from '../types'
import { useLanguage } from '../context/LanguageContext'

interface Message {
  id: number
  sender: 'user' | 'bot'
  text: string
  selection?: ReservationSelection
}

interface ChatWidgetProps {
  isOpen: boolean
  onToggle: () => void
  onBookWithSymptom: (selection: ReservationSelection) => void
}

export function ChatWidget({ isOpen, onToggle, onBookWithSymptom }: ChatWidgetProps) {
  const { t } = useLanguage()

  return (
    <>
      <button
        className="chat-launcher"
        aria-label={isOpen ? t.chat.launcherCloseAria : t.chat.launcherOpenAria}
        aria-expanded={isOpen}
        aria-controls={isOpen ? 'chat-panel' : undefined}
        onClick={onToggle}
      >
        {isOpen ? <X size={22} /> : <MessageCircle size={22} />}
        <span>{t.chat.launcher}</span>
      </button>
      {isOpen && <ChatPanel onClose={onToggle} onBookWithSymptom={onBookWithSymptom} />}
    </>
  )
}

function ChatPanel({ onClose, onBookWithSymptom }: { onClose: () => void; onBookWithSymptom: ChatWidgetProps['onBookWithSymptom'] }) {
  const { t, lang } = useLanguage()
  const [messages, setMessages] = useState<Message[]>([
    { id: 0, sender: 'bot', text: t.chat.welcome }
  ])
  const [input, setInput] = useState('')
  const inputRef = useRef<HTMLInputElement>(null)
  const messagesRef = useRef<HTMLDivElement>(null)
  const nextId = useRef(1)

  // Reset welcome message on language change if no chat yet
  useEffect(() => {
    setMessages(prev => {
      if (prev.length === 1 && prev[0].id === 0) {
        return [{ id: 0, sender: 'bot', text: t.chat.welcome }]
      }
      return prev
    })
  }, [lang, t.chat.welcome])

  useEffect(() => {
    const opener = document.activeElement instanceof HTMLElement ? document.activeElement : null
    inputRef.current?.focus()
    return () => { if (opener?.isConnected) opener.focus(); else document.querySelector<HTMLButtonElement>('.chat-launcher')?.focus() }
  }, [])

  useEffect(() => {
    const list = messagesRef.current
    if (list) list.scrollTop = list.scrollHeight
  }, [messages])

  function send(text: string, selection?: ReservationSelection) {
    const symptom = text.trim()
    if (!symptom) return
    const userMessage: Message = { id: nextId.current++, sender: 'user', text: symptom }
    const reply: Message = {
      id: nextId.current++,
      sender: 'bot',
      text: t.chat.botReply,
      selection: selection ?? { device: 'etc', symptom },
    }
    setMessages(previous => [...previous, userMessage, reply])
    setInput('')
    inputRef.current?.focus()
  }

  return (
    <section id="chat-panel" className="chat-panel" role="dialog" aria-modal="false" aria-labelledby="chat-title" aria-describedby="chat-notice" onKeyDown={event => { if (event.key === 'Escape') { event.stopPropagation(); onClose() } }}>
      <div className="chat-header">
        <div>
          <span className="eyebrow">{t.modal.brand}</span>
          <h2 id="chat-title">{t.chat.headerTitle}</h2>
        </div>
        <button className="icon-button" aria-label={t.chat.launcherCloseAria} onClick={onClose}><X size={22} /></button>
      </div>
      <p id="chat-notice" className="chat-notice">{t.chat.notice}</p>
      <div className="chat-messages" role="log" aria-label="Messages" aria-live="polite" aria-relevant="additions" ref={messagesRef}>
        {messages.map(message => (
          <div key={message.id} className={'chat-message ' + message.sender}>
            <span className="message-author">
              {message.sender === 'bot' ? `${t.modal.brand} · Info` : (lang === 'ko' ? '나' : 'Me')}
            </span>
            <p style={{ whiteSpace: 'pre-line' }}>{message.text}</p>
            {message.selection && (
              <button className="chat-booking" onClick={() => onBookWithSymptom(message.selection!)}>
                {t.chat.bookAction} <ArrowUpRight size={16} />
              </button>
            )}
          </div>
        ))}
      </div>
      <div className="quick-questions" aria-label="Quick questions">
        {t.chat.quickQuestions.map(question => (
          <button key={question.label} onClick={() => send(question.symptom, { device: question.device, symptom: question.symptom })}>
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
        <button className="icon-button primary" type="submit" disabled={!input.trim()} aria-label={t.chat.sendAria}>
          <Send size={19} />
        </button>
      </form>
    </section>
  )
}
