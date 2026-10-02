/** 관리자 화면 상단의 오류·안내 메시지를 바꾼다. */
export type AdminFeedback = {
  error: (message: string) => void
  notice: (message: string) => void
  clear: () => void
}

export type AdminSectionProps = {
  token: string | null
  /** 기사와 예약 목록을 다시 불러온다. */
  reload: () => Promise<void>
  feedback: AdminFeedback
}
