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

const flagClass = 'block h-3.5 w-5 flex-none rounded-[2px] object-cover shadow-[0_0_0_1px_rgba(15,23,42,0.1)]'

function Flag({ code }: { code: CountryCode }) {
  const src = FLAG_URLS[`/node_modules/country-flag-icons/3x2/${code}.svg`]
  if (!src) return <span className={flagClass} aria-hidden="true" />
  return <img className={flagClass} src={src} alt="" width={20} height={14} loading="lazy" />
}

// react-phone-number-input의 countrySelectComponent로 쓴다. 라이브러리가 넘기는 onChange 대신
// onSelectCountry를 쓰는데, 라이브러리는 국가가 바뀌면 입력한 숫자를 새 국가 번호로 바꿔 유지하지만
// 여기서는 국가를 바꾸면 입력을 비우기 때문이다.
type PhoneCountrySelectProps = {
  value?: CountryCode
  onSelectCountry: (country: CountryCode) => void
  disabled?: boolean
  readOnly?: boolean
  'aria-label'?: string
}

export function PhoneCountrySelect({
  value,
  onSelectCountry,
  disabled,
  readOnly,
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
        className="inline-flex w-auto flex-none cursor-pointer items-center gap-1.5 rounded-lg border border-line-strong bg-white px-2.5 py-0 text-[14px] font-medium text-ink transition-[border-color,box-shadow] duration-150 ease-[ease] hover:border-ink-faint focus-visible:border-brand focus-visible:shadow-[0_0_0_3px_rgba(36,87,214,0.15)] focus-visible:outline-none data-[popup-open]:border-brand data-[popup-open]:shadow-[0_0_0_3px_rgba(36,87,214,0.15)] data-[popup-open]:outline-none [&_svg]:text-ink-subtle"
        aria-label={ariaLabel}
      >
        <Flag code={selected.code} />
        <span>+{selected.callingCode}</span>
        <ChevronDown size={16} aria-hidden="true" />
      </Combobox.Trigger>
      <Combobox.Portal>
        <Combobox.Positioner className="z-[1000] outline-none" align="start" sideOffset={6}>
          <Combobox.Popup className="w-[300px] max-w-(--available-width) origin-(--transform-origin) overflow-hidden rounded-xl border border-line-default bg-white shadow-[0_12px_32px_rgba(24,41,70,0.14)] transition-[opacity,scale] duration-[120ms] ease-[ease] data-[ending-style]:scale-[0.98] data-[ending-style]:opacity-0 data-[starting-style]:scale-[0.98] data-[starting-style]:opacity-0" aria-label={ariaLabel}>
            <div className="flex items-center gap-2 border-b border-line-subtle px-3 text-ink-subtle">
              <Search size={16} aria-hidden="true" />
              <Combobox.Input className="h-11 min-w-0 flex-1 border-0 bg-transparent text-[14px] text-ink outline-none placeholder:text-ink-faint" placeholder={lang === 'en' ? 'Search country or code' : '국가 또는 국가번호 검색'} />
            </div>
            <Combobox.Empty>
              <p className="m-0 px-3 py-4 text-[13px] text-ink-subtle">{lang === 'en' ? 'No matching country.' : '검색 결과가 없습니다.'}</p>
            </Combobox.Empty>
            <Combobox.List className="max-h-[min(280px,var(--available-height))] overflow-y-auto overscroll-contain p-1.5 outline-none">
              {(option: CountryOption) => (
                <Combobox.Item key={option.code} value={option} className="grid cursor-pointer grid-cols-[20px_1fr_auto_14px] items-center gap-2.5 rounded-lg px-2.5 py-2 text-[14px] text-ink select-none data-[highlighted]:bg-surface-muted data-[selected]:font-semibold data-[selected]:text-brand">
                  <Flag code={option.code} />
                  <span className="truncate">{option.name}</span>
                  <span className="text-[13px] text-ink-subtle">+{option.callingCode}</span>
                  <Combobox.ItemIndicator className="text-brand">
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
