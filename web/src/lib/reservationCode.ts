// 고객에게 보여주는 예약 번호(숫자 8자리). 서버는 숫자만 저장하고, 화면에서는 4821-7390처럼 4자리씩 끊어 보여준다.
const CODE_LENGTH = 8

// 숫자만 남겨 8자리까지 받고, 5번째 숫자부터 하이픈을 붙인다. 입력 중인 값과 저장된 값 모두에 쓴다.
export function formatReservationCode(value: string): string {
  const digits = value.replace(/\D/g, '').slice(0, CODE_LENGTH)
  return digits.length > 4 ? `${digits.slice(0, 4)}-${digits.slice(4)}` : digits
}

export function isValidReservationCode(value: string): boolean {
  return value.replace(/\D/g, '').length === CODE_LENGTH
}
