import type { Message } from './types'

const CHAT_SESSION_KEY = 'dacare.chat.messages.v1'
const STORED_MESSAGE_COUNT = 20

export function readChatMessages(): Message[] | null {
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

export function saveChatMessages(messages: Message[]) {
  try {
    window.sessionStorage.setItem(CHAT_SESSION_KEY, JSON.stringify(messages.slice(-STORED_MESSAGE_COUNT)))
  } catch {
    // 저장소를 사용할 수 없어도 현재 대화는 계속 진행합니다.
  }
}
