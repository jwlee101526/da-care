import type { ReactNode } from 'react'
import { AlertCircle } from 'lucide-react'

// 페이지 상단 오류 안내 (예약 목록, 비회원 예약 조회, 예약 접수)
export function ErrorAlert({ children, role }: { children: ReactNode; role?: 'alert' }) {
  return (
    <div className="mb-6 flex items-center gap-2.5 rounded-[10px] border border-danger-border bg-danger-soft px-[18px] py-3.5 text-[14px] font-medium text-danger" role={role}>
      <AlertCircle size={18} />
      <span>{children}</span>
    </div>
  )
}
