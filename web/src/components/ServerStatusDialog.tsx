import { useEffect, useState } from 'react'
import { CircleCheck, Loader2 } from 'lucide-react'
import { checkServerHealth } from '@/lib/api'
import { useLanguage } from '@/context/LanguageContext'
import { buttonVariants } from '@/components/ui/button'
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'

const footerButton = 'cursor-pointer disabled:cursor-default disabled:opacity-70'
const pollIntervalMs = 3000
const pollLimitMs = 90_000

type Status = 'waking' | 'ready' | 'stillWaiting'

const wait = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

// dev 환경에서 앱을 열 때 서버가 대기 중이면 안내한다. 상태 확인 요청이 곧 서버를 깨우므로
// 다이얼로그가 열려 있는 동안 자동으로 다시 확인하고, 서버가 응답하면 같은 다이얼로그에서 알려 준다.
export function ServerStatusDialog() {
  const { t } = useLanguage()
  const text = t.serverStatus
  const [isOpen, setIsOpen] = useState(false)
  const [status, setStatus] = useState<Status>('waking')

  useEffect(() => {
    let ignore = false
    checkServerHealth().then(isUp => {
      if (!ignore && !isUp) setIsOpen(true)
    })
    return () => { ignore = true }
  }, [])

  useEffect(() => {
    if (!isOpen || status !== 'waking') return
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
          return
        }
      }
      setStatus('stillWaiting')
    }
    poll()
    return () => { ignore = true }
  }, [isOpen, status])

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
              <DialogTitle className="flex items-center gap-2">
                {status === 'waking' && <Loader2 size={20} className="animate-spin text-brand" />}
                {text.title}
              </DialogTitle>
              <DialogDescription>{text.description}</DialogDescription>
            </DialogHeader>
            {status === 'stillWaiting' && <p className="m-0 text-[13px] leading-[1.5] text-ink-subtle" role="status">{text.stillWaiting}</p>}
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
