import type { ReactNode } from 'react'
import { cn } from '@/lib/utils'

interface OrderPageLayoutProps {
  title: string
  description: string
  /** 제목 오른쪽 버튼 */
  action?: ReactNode
  /** 입력 폼처럼 좁은 본문 */
  narrow?: boolean
  children: ReactNode
}

// 예약 목록형 페이지(내 예약, 비회원 예약 조회)의 배경·본문 폭·제목 영역
export function OrderPageLayout({ title, description, action, narrow = false, children }: OrderPageLayoutProps) {
  return (
    <div className="box-border min-h-[calc(100dvh-var(--site-header-height))] bg-surface-subtle px-5 pt-10 pb-20 max-[640px]:px-4 max-[640px]:pt-6 max-[640px]:pb-16">
      <main className={cn('mx-auto', narrow ? 'w-[min(520px,100%)]' : 'w-[min(860px,100%)]')}>
        <div className="mb-8 flex flex-wrap items-end justify-between gap-5">
          <div>
            <h1 className="mb-1.5 text-[28px] font-bold tracking-[-0.03em] text-navy max-[640px]:text-[24px]">{title}</h1>
            <p className="m-0 text-[15px] text-ink-subtle">{description}</p>
          </div>
          {action}
        </div>
        {children}
      </main>
    </div>
  )
}
