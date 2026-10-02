import {
  getCountries,
  getCountryCallingCode,
  getExampleNumber,
  isValidPhoneNumber,
  parsePhoneNumberWithError,
  type CountryCode,
} from 'libphonenumber-js/mobile'
import examples from 'libphonenumber-js/mobile/examples'

// 전화번호 규칙은 libphonenumber를 따른다. 서버(PhoneNumber)와 같은 규칙으로 검증하고
// E.164 형식(+821012345678)으로 주고받는다. 국가번호 없이 입력한 번호는 한국 번호로 해석한다.
// 연락처로 일정 안내 문자를 보내므로 휴대전화 번호 규칙(mobile 메타데이터)만 쓴다. 이 메타데이터에서는
// 유효한 번호 = 유효한 휴대전화 번호다.
export const DEFAULT_COUNTRY: CountryCode = 'KR'

export type CountryOption = { code: CountryCode; name: string; callingCode: string }

export function isValidPhone(value: string | null | undefined): boolean {
  return !!value && isValidPhoneNumber(value, DEFAULT_COUNTRY)
}

const DIGITS = '0123456789'

// 이미 완성된 휴대전화 번호에 숫자를 더 붙일 수 있는지. 어떤 숫자를 붙여도 유효하지 않으면
// 최대 자릿수에 도달한 것으로 보고 입력 칸에서 추가 입력을 막는다.
// (예: 한국은 010-123-4567 다음에도 숫자를 더 받지만 010-1234-5678 다음은 막는다.)
export function canAppendDigit(value: string | null | undefined): boolean {
  if (!value || !isValidPhone(value)) return true
  return [...DIGITS].some(digit => isValidPhone(value + digit))
}

// 선택한 국가의 휴대전화 번호 예시(입력 칸 placeholder). 예: 한국 010-2000-0000, 미국 (201) 555-0123
export function examplePhone(country: CountryCode): string {
  return getExampleNumber(country, examples)?.formatNational() ?? ''
}

// 저장된 값(E.164)의 국가. 입력 칸의 국가번호 초기값으로 쓴다.
export function countryOf(value: string | null | undefined): CountryCode | undefined {
  if (!value) return undefined
  try {
    return parsePhoneNumberWithError(value, DEFAULT_COUNTRY).country
  } catch {
    return undefined
  }
}

// 국내 번호는 010-1234-5678, 해외 번호는 +1 201 555 0123처럼 표시한다.
export function formatPhone(value: string | null | undefined): string {
  if (!value) return ''
  try {
    const phone = parsePhoneNumberWithError(value, DEFAULT_COUNTRY)
    return phone.country === DEFAULT_COUNTRY ? phone.formatNational() : phone.formatInternational()
  } catch {
    return value
  }
}

// 국가번호 선택 목록. 기본 국가를 맨 위에 두고 나머지는 국가 이름순으로 정렬한다.
export function getCountryOptions(lang: 'ko' | 'en'): CountryOption[] {
  const names = new Intl.DisplayNames([lang], { type: 'region' })
  const options = getCountries().map(code => ({
    code,
    name: names.of(code) ?? code,
    callingCode: getCountryCallingCode(code),
  }))
  return options.sort((a, b) => {
    if (a.code === DEFAULT_COUNTRY) return -1
    if (b.code === DEFAULT_COUNTRY) return 1
    return a.name.localeCompare(b.name, lang)
  })
}
