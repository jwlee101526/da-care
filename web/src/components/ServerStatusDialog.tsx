import { useEffect, useState } from 'react'
import { CircleCheck, Loader2 } from 'lucide-react'
import { checkServerHealth } from '@/lib/api'
import { useLanguage } from '@/context/LanguageContext'
import { buttonVariants } from '@/components/ui/button'
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'

const footerButton = 'cursor-pointer disabled:cursor-default disabled:opacity-70'
const pollIntervalMs = 3000
const pollLimitMs = 90_000

type Status = 'idle' | 'waking' | 'ready' | 'stillWaiting'

const wait = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

// dev 환경에서 앱을 열 때 서버가 대기 중이면 안내한다. 상태 확인 요청이 곧 서버를 깨우므로 자동으로 다시 확인하고,
// 둘러보려고 다이얼로그를 닫았더라도 서버가 응답하면 다이얼로그를 다시 열어 알려 준다.
export function ServerStatusDialog() {
  const { t } = useLanguage()
  const text = t.serverStatus
  const [isOpen, setIsOpen] = useState(false)
  const [status, setStatus] = useState<Status>('idle')

  useEffect(() => {
    let ignore = false
    checkServerHealth().then(isUp => {
      if (ignore || isUp) return
      setStatus('waking')
      setIsOpen(true)
    })
    return () => { ignore = true }
  }, [])

  useEffect(() => {
    if (status !== 'waking') return
    let ignore = false
    const deadline = Date.now() + pollLimitMs
    const poll = async () => {
      while (Date.now() < deadline) {
        await wait(pollIntervalMs)
        if (ignore) return
        const isUp = await checkServerHealth()
        if (ignore) return
        if (isUp) {
          setStatus('ready')
          setIsOpen(true)
          return
        }
      }
      setStatus('stillWaiting')
    }
    poll()
    return () => { ignore = true }
  }, [status])

  return (
    <Dialog open={isOpen} onOpenChange={setIsOpen}>
      <DialogContent>
        {status === 'ready' ? (
          <>
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2">
                <CircleCheck size={20} className="text-success" />
                {text.readyTitle}
              </DialogTitle>
              <DialogDescription>{text.readyDescription}</DialogDescription>
            </DialogHeader>
            <DialogFooter>
              <DialogClose className={buttonVariants({ variant: 'primary', size: 'compact', className: footerButton })}>
                {text.start}
              </DialogClose>
            </DialogFooter>
          </>
        ) : (
          <>
            <DialogHeader>
              <DialogTitle>{text.title}</DialogTitle>
              <DialogDescription>{text.description}</DialogDescription>
            </DialogHeader>
            <p className="m-0 flex items-center gap-2 text-[13px] leading-[1.5] text-ink-subtle" role="status">
              {status === 'waking' && <Loader2 size={16} className="shrink-0 animate-spin text-brand" />}
              {status === 'waking' ? text.checking : text.stillWaiting}
            </p>
            <DialogFooter>
              <DialogClose className={buttonVariants({ variant: 'secondary', size: 'compact', className: footerButton })}>
                {text.browse}
              </DialogClose>
              {status === 'stillWaiting' && (
                <button
                  type="button"
                  className={buttonVariants({ variant: 'primary', size: 'compact', className: footerButton })}
                  onClick={() => setStatus('waking')}
                >
                  {text.retry}
                </button>
              )}
            </DialogFooter>
          </>
        )}
      </DialogContent>
    </Dialog>
  )
}
