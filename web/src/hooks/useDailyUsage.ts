import { useCallback, useEffect, useState } from 'react'
import { fetchUsage } from '../lib/api'
import type { DailyUsage } from '../lib/api'

/**
 * 유료 API(AI 상담, SMS)의 오늘 서비스 전체 사용량. 조회에 실패하면 이전 값을 유지한다.
 */
export function useDailyUsage() {
  const [usage, setUsage] = useState<DailyUsage | null>(null)
  const refresh = useCallback(async () => {
    try {
      const next = await fetchUsage()
      setUsage(next)
      return next
    } catch {
      return null
    }
  }, [])
  useEffect(() => {
    let active = true
    fetchUsage().then(next => { if (active) setUsage(next) }).catch(() => {})
    return () => { active = false }
  }, [])
  return { usage, refresh }
}
