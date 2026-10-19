import { useEffect, useState } from 'react'
import { CircleCheck, Loader2 } from 'lucide-react'
import { checkServerHealth, serverHealthUrl } from '@/lib/api'
import { useLanguage } from '@/context/LanguageContext'
import { buttonVariants } from '@/components/ui/button'
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'

const footerButton = 'cursor-pointer disabled:cursor-default disabled:opacity-70'

// dev 환경에서 앱을 열 때 서버가 대기 중이면 깨우는 링크와 함께 안내한다. 재확인은 버튼을 누를 때만 하고,
// 서버가 응답하면 같은 다이얼로그에서 다시 작동한다고 알려 준다.
export function ServerStatusDialog() {
  const { t } = useLanguage()
  const text = t.serverStatus
  const [isOpen, setIsOpen] = useState(false)
  const [isReady, setIsReady] = useState(false)
  const [isChecking, setIsChecking] = useState(false)
  const [isStillWaiting, setIsStillWaiting] = useState(false)

  useEffect(() => {
    let ignore = false
    checkServerHealth().then(isUp => {
      if (!ignore && !isUp) setIsOpen(true)
    })
    return () => { ignore = true }
  }, [])

  const retry = async () => {
    setIsChecking(true)
    const isUp = await checkServerHealth()
    setIsChecking(false)
    setIsReady(isUp)
    setIsStillWaiting(!isUp)
  }

  return (
    <Dialog open={isOpen} onOpenChange={setIsOpen}>
      <DialogContent>
        {isReady ? (
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
            <a className="w-fit text-[14px] leading-[1.5] font-semibold text-brand underline underline-offset-3 hover:text-brand-hover" href={serverHealthUrl} target="_blank" rel="noreferrer">
              {text.wake}
            </a>
            {isStillWaiting && <p className="m-0 text-[13px] leading-[1.5] text-ink-subtle" role="status">{text.stillWaiting}</p>}
            <DialogFooter>
              <DialogClose className={buttonVariants({ variant: 'secondary', size: 'compact', className: footerButton })}>
                {text.browse}
              </DialogClose>
              <button
                type="button"
                className={buttonVariants({ variant: 'primary', size: 'compact', className: footerButton })}
                onClick={retry}
                disabled={isChecking}
              >
                {isChecking && <Loader2 size={16} className="animate-spin" />}
                {isChecking ? text.checking : text.retry}
              </button>
            </DialogFooter>
          </>
        )}
      </DialogContent>
    </Dialog>
  )
}
