import { useState, type ComponentPropsWithoutRef } from 'react'
import PhoneInputWithCountry from 'react-phone-number-input/mobile'
import type { CountryCode } from 'libphonenumber-js/mobile'
import { useLanguage } from '../context/LanguageContext'
import { DEFAULT_COUNTRY, canAppendDigit, countryOf, examplePhone, isValidPhone } from '../lib/phone'
import { PhoneCountrySelect } from './PhoneCountrySelect'
import './PhoneInput.css'

// placeholder는 선택한 국가의 예시 번호로 자동으로 채운다.
type PhoneInputProps = Omit<ComponentPropsWithoutRef<'input'>, 'value' | 'defaultValue' | 'onChange' | 'type' | 'placeholder'> & {
  value: string
  onChange: (value: string) => void
}

// 휴대전화 번호 입력. 국가번호는 국기와 함께 검색형 목록에서 고르고, 번호 칸에는 숫자만 입력받는다.
// 입력한 숫자는 선택한 국가의 형식으로 하이픈이 붙고, 국가별 휴대전화 번호 최대 자릿수를 넘겨 입력할 수 없다.
// 값은 E.164 형식(+821012345678)으로 전달된다.
export function PhoneInput({ value, onChange, className, ...props }: PhoneInputProps) {
  const { lang } = useLanguage()
  const [touched, setTouched] = useState(false)
  const [country, setCountry] = useState<CountryCode>(() => countryOf(value) ?? DEFAULT_COUNTRY)
  const invalid = touched && !!value && !isValidPhone(value)

  return (
    <div className="phone-field">
      <PhoneInputWithCountry
        {...props}
        className="phone-input"
        numberInputProps={{
          className,
          inputMode: 'numeric',
          'aria-invalid': invalid || undefined,
          // 숫자 외 문자는 입력 단계에서 막는다. '+'를 허용하면 국가번호 직접 입력 모드로 바뀌므로 함께 막는다.
          // 휴대전화 번호 최대 자릿수에 도달하면 숫자도 더 받지 않는다.
          // 붙여넣기는 라이브러리가 해석하므로 +국가번호가 포함된 번호를 붙여넣으면 국가가 자동으로 바뀐다.
          onBeforeInput: (event: InputEvent) => {
            if (!event.data) return
            if (/\D/.test(event.data) || !canAppendDigit(value)) event.preventDefault()
          },
        }}
        countrySelectComponent={PhoneCountrySelect}
        countrySelectProps={{
          className,
          onSelectCountry: (next: CountryCode) => {
            if (next === country) return
            setCountry(next)
            setTouched(false)
            onChange('')
          },
        }}
        labels={{ country: lang === 'en' ? 'Country code' : '국가번호' }}
        defaultCountry={country}
        placeholder={examplePhone(country)}
        addInternationalOption={false}
        limitMaxLength
        autoComplete={props.autoComplete ?? 'tel-national'}
        value={value || undefined}
        onChange={next => onChange(next ?? '')}
        onBlur={() => setTouched(true)}
      />
      {invalid && (
        <small className="phone-input-error" role="alert">
          {lang === 'en' ? 'Please check the mobile number.' : '휴대전화 번호를 다시 확인해 주세요.'}
        </small>
      )}
    </div>
  )
}
