import { toast } from 'sonner'
import type { UsageItem } from './api'

export type UsageToastText = {
  used: (used: number, limit: number) => string
  remaining: (remaining: number) => string
  exhausted: string
}

/**
 * 사용 후 남은 한도에 따라 일반·경고(80% 이상)·소진 알림을 띄운다.
 */
export function notifyUsage(item: UsageItem, text: UsageToastText) {
  if (item.remaining === 0) {
    toast.error(text.exhausted, { description: text.used(item.used, item.limit) })
  } else if (item.used >= item.limit * 0.8) {
    toast.warning(text.used(item.used, item.limit), { description: text.remaining(item.remaining) })
  } else {
    toast.info(text.used(item.used, item.limit))
  }
}
