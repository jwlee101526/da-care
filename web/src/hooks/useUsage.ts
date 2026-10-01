import { useCallback, useEffect, useState } from 'react'

/**
 * 유료 API 주간 사용량 조회. fetcher가 바뀌면(로그인 상태 변경 등) 다시 조회하고, 실패하면 이전 값을 유지한다.
 */
export function useUsage<T>(fetcher: () => Promise<T>) {
  const [usage, setUsage] = useState<T | null>(null)
  const refresh = useCallback(async () => {
    try {
      const next = await fetcher()
      setUsage(next)
      return next
    } catch {
      return null
    }
  }, [fetcher])
  useEffect(() => {
    let active = true
    fetcher().then(next => { if (active) setUsage(next) }).catch(() => {})
    return () => { active = false }
  }, [fetcher])
  return { usage, refresh }
}
