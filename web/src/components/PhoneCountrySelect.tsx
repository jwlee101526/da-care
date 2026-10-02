import { useMemo, useRef } from 'react'
import { Combobox } from '@base-ui/react/combobox'
import { Check, ChevronDown, Search } from 'lucide-react'
import type { CountryCode } from 'libphonenumber-js/mobile'
import { useLanguage } from '../context/LanguageContext'
import { DEFAULT_COUNTRY, getCountryOptions, type CountryOption } from '../lib/phone'

// 국기 SVG(country-flag-icons)는 국가마다 별도 파일로 내보내고 화면에 보일 때만 불러온다.
// 작은 파일도 JS에 인라인되지 않도록 no-inline으로 가져온다.
const FLAG_URLS = import.meta.glob<string>('/node_modules/country-flag-icons/3x2/*.svg', {
  query: '?no-inline',
  import: 'default',
  eager: true,
})

function Flag({ code }: { code: CountryCode }) {
  const src = FLAG_URLS[`/node_modules/country-flag-icons/3x2/${code}.svg`]
  if (!src) return <span className="phone-flag" aria-hidden="true" />
  return <img className="phone-flag" src={src} alt="" width={20} height={14} loading="lazy" />
}

// react-phone-number-input의 countrySelectComponent로 쓴다. 라이브러리가 넘기는 onChange 대신
// onSelectCountry를 쓰는데, 라이브러리는 국가가 바뀌면 입력한 숫자를 새 국가 번호로 바꿔 유지하지만
// 여기서는 국가를 바꾸면 입력을 비우기 때문이다.
type PhoneCountrySelectProps = {
  value?: CountryCode
  onSelectCountry: (country: CountryCode) => void
  disabled?: boolean
  readOnly?: boolean
  className?: string
  'aria-label'?: string
}

export function PhoneCountrySelect({
  value,
  onSelectCountry,
  disabled,
  readOnly,
  className,
  'aria-label': ariaLabel,
}: PhoneCountrySelectProps) {
  const { lang } = useLanguage()
  const options = useMemo(() => getCountryOptions(lang), [lang])
  const selected = options.find(option => option.code === (value ?? DEFAULT_COUNTRY)) ?? options[0]
  const triggerRef = useRef<HTMLButtonElement>(null)
  // 국가를 골라 닫히면 번호 칸으로, Esc 등으로 그냥 닫히면 선택 버튼으로 포커스를 돌려준다.
  const pickedRef = useRef(false)
  const { contains } = Combobox.useFilter()

  return (
    <Combobox.Root
      items={options}
      value={selected}
      onValueChange={next => {
        if (!next) return
        pickedRef.current = true
        onSelectCountry(next.code)
      }}
      onOpenChange={open => {
        if (open) pickedRef.current = false
      }}
      onOpenChangeComplete={open => {
        // 국가를 고르면 같은 입력 묶음의 번호 칸으로 포커스를 옮긴다. 목록이 닫히며 포커스를
        // 선택 버튼으로 되돌린 뒤에 실행해야 하므로 닫힘 애니메이션이 끝난 시점에 처리한다.
        if (!open && pickedRef.current) {
          triggerRef.current?.parentElement?.querySelector<HTMLInputElement>('input[type="tel"]')?.focus()
        }
      }}
      // 국가 이름 또는 국가번호(82, +82)로 찾는다.
      filter={(option, query) => {
        const code = query.trim().replace(/^\+/, '')
        return contains(option.name, query) || (code !== '' && option.callingCode.startsWith(code))
      }}
      isItemEqualToValue={(item, current) => item.code === current.code}
      itemToStringLabel={option => `${option.name} +${option.callingCode}`}
      disabled={disabled}
      readOnly={readOnly}
      autoHighlight
    >
      <Combobox.Trigger
        ref={triggerRef}
        className={`phone-country-trigger ${className ?? ''}`}
        aria-label={ariaLabel}
      >
        <Flag code={selected.code} />
        <span>+{selected.callingCode}</span>
        <ChevronDown size={16} aria-hidden="true" />
      </Combobox.Trigger>
      <Combobox.Portal>
        <Combobox.Positioner className="phone-country-positioner" align="start" sideOffset={6}>
          <Combobox.Popup className="phone-country-popup" aria-label={ariaLabel}>
            <div className="phone-country-search">
              <Search size={16} aria-hidden="true" />
              <Combobox.Input placeholder={lang === 'en' ? 'Search country or code' : '국가 또는 국가번호 검색'} />
            </div>
            <Combobox.Empty>
              <p className="phone-country-empty">{lang === 'en' ? 'No matching country.' : '검색 결과가 없습니다.'}</p>
            </Combobox.Empty>
            <Combobox.List className="phone-country-list">
              {(option: CountryOption) => (
                <Combobox.Item key={option.code} value={option} className="phone-country-item">
                  <Flag code={option.code} />
                  <span className="phone-country-name">{option.name}</span>
                  <span className="phone-country-code">+{option.callingCode}</span>
                  <Combobox.ItemIndicator className="phone-country-check">
                    <Check size={14} />
                  </Combobox.ItemIndicator>
                </Combobox.Item>
              )}
            </Combobox.List>
          </Combobox.Popup>
        </Combobox.Positioner>
      </Combobox.Portal>
    </Combobox.Root>
  )
}
