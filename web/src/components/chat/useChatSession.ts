import { useCallback, useEffect, useRef, useState } from 'react'
import { toast } from 'sonner'
import { useAuth } from '../../context/AuthContext'
import { useLanguage } from '../../context/LanguageContext'
import { useUsage } from '../../hooks/useUsage'
import { ApiError, fetchMyUsage, streamDiagnosis, type UsageItem } from '../../lib/api'
import { notifyUsage } from '../../lib/usageToast'
import { readChatMessages, saveChatMessages } from './chatStorage'
import type { ChatText, Message } from './types'

/** 서버 상담 기한(75초) 이후 도착하는 시간 초과 안내를 받을 수 있도록 여유를 둔다. */
const REQUEST_TIMEOUT_MS = 90000
const HISTORY_TURNS = 12
const HISTORY_TEXT_LENGTH = 4000

/**
 * 상담을 더 할 수 없는 이유. 개인 한도가 남았는데 막혔다면 서비스 전체 한도가 소진된 것이다.
 */
function exhaustedMessage(item: UsageItem, guest: boolean, text: ChatText['usage']) {
  if (item.used < item.limit) return text.serviceExhausted
  return guest ? text.guestExhausted : text.exhaustedPlaceholder
}

/**
 * AI 상담 대화 상태. 메시지 목록과 스트리밍 요청, 주간 사용량을 관리하고 대화를 세션 저장소에 보관한다.
 */
export function useChatSession() {
  const { t } = useLanguage()
  const { token } = useAuth()
  const [messages, setMessages] = useState<Message[]>(() => readChatMessages() ?? [
    { id: 0, sender: 'bot', text: t.chat.welcome }
  ])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)
  const nextId = useRef(Math.max(0, ...messages.map(message => message.id)) + 1)
  const requestRef = useRef<AbortController | null>(null)
  const { usage, refresh: refreshUsage } = useUsage(useCallback(() => fetchMyUsage(token), [token]))
  const usageExhausted = usage?.diagnosis.remaining === 0
  const exhaustedText = usage && usageExhausted ? exhaustedMessage(usage.diagnosis, !token, t.chat.usage) : ''

  useEffect(() => () => requestRef.current?.abort(), [])

  useEffect(() => {
    saveChatMessages(messages)
  }, [messages])

  const updateMessage = (id: number, update: (message: Message) => Message) => {
    setMessages(previous => previous.map(message => message.id === id ? update(message) : message))
  }

  async function send(text: string, showUserMessage = true) {
    const symptom = text.trim()
    if (!symptom || requestRef.current || usageExhausted) return
    const controller = new AbortController()
    requestRef.current = controller
    const timeout = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS)
    const userMessage: Message = { id: nextId.current++, sender: 'user', text: symptom, hidden: !showUserMessage }
    const responseId = nextId.current++
    const history = messages.filter(message => message.id !== 0 && !message.error).slice(-HISTORY_TURNS)
      .map(message => ({ role: message.sender === 'bot' ? 'assistant' as const : 'user' as const, text: message.text.slice(0, HISTORY_TEXT_LENGTH) }))
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
          updateMessage(responseId, message => {
            const progress = [...(message.toolProgress ?? [])]
            const index = progress.findIndex(item => item.tool === event.data.tool)
            if (index >= 0) progress[index] = event.data
            else progress.push(event.data)
            const cards = event.data.card
              ? [...(message.cards ?? []), event.data.card].filter((card, cardIndex, items) => items.findIndex(item => item.type === card.type) === cardIndex)
              : message.cards
            return { ...message, toolProgress: progress, cards }
          })
          return
        }
        if (event.type === 'completed') {
          completed = true
          updateMessage(responseId, message => ({
            ...message, text: event.data.answer, cards: event.data.cards,
            executedTools: event.data.executedTools, streaming: false,
          }))
          return
        }
        failed = true
        if (event.data.code === 'WEEKLY_LIMIT_EXCEEDED' || event.data.code === 'SERVICE_LIMIT_EXCEEDED') limitMessage = event.data.message
        updateMessage(responseId, message => ({
          ...message, error: true, text: event.data.message, streaming: false,
        }))
      })
      if (!completed && !failed) throw new ApiError(500, t.chat.errors.interrupted)
    } catch (error) {
      updateMessage(responseId, message => ({
        ...message, error: true, streaming: false,
        text: error instanceof ApiError ? error.message
          : controller.signal.aborted ? t.chat.errors.timeout : t.chat.errors.network,
      }))
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

  return { messages, input, setInput, loading, inputRef, usage, usageExhausted, exhaustedText, send, startNewChat }
}
