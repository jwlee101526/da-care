import type { DeviceType, ReservationSelection } from '../types'
import { Reveal } from './Reveal'
import { useLanguage } from '../context/LanguageContext'

import smartphoneTabletIcon from '../assets/icons/smartphone-tablet.svg'
import computerIcon from '../assets/icons/computer.svg'
import smartTvIcon from '../assets/icons/smart-tv.svg'
import consoleIcon from '../assets/icons/console.svg'
import airconIcon from '../assets/icons/aircon.svg'
import washingIcon from '../assets/icons/washing-machine.svg'
import fridgeIcon from '../assets/icons/fridge.svg'
import microwaveIcon from '../assets/icons/microwave.svg'
import vacuumIcon from '../assets/icons/vacuum.svg'
import wifiIcon from '../assets/icons/wifi.svg'
import audioIcon from '../assets/icons/audio.svg'
import emergencyAsIcon from '../assets/icons/emergency-as.svg'
import { cn } from '@/lib/utils'
import { eyebrow, sectionContainer, sectionHeading, sectionHeadingDesc, sectionPadding, sectionTitle, snapCentered } from './landing'

interface CategoryItem {
  id: keyof typeof import('../locales/ko').ko['services']['categories']
  iconSrc: string
  device: DeviceType
}

const categories: CategoryItem[] = [
  { id: 'phone-tablet', iconSrc: smartphoneTabletIcon, device: 'smartphone' },
  { id: 'computer', iconSrc: computerIcon, device: 'computer' },
  { id: 'tv', iconSrc: smartTvIcon, device: 'tv' },
  { id: 'console', iconSrc: consoleIcon, device: 'console' },
  { id: 'aircon', iconSrc: airconIcon, device: 'aircon' },
  { id: 'washing', iconSrc: washingIcon, device: 'washing' },
  { id: 'fridge', iconSrc: fridgeIcon, device: 'fridge' },
  { id: 'microwave', iconSrc: microwaveIcon, device: 'microwave' },
  { id: 'cleaner', iconSrc: vacuumIcon, device: 'cleaner' },
  { id: 'internet', iconSrc: wifiIcon, device: 'internet' },
  { id: 'etc', iconSrc: audioIcon, device: 'audio' },
  { id: 'repair', iconSrc: emergencyAsIcon, device: 'repair' },
]

export function ServiceSection({ onSelectCategory }: { onSelectCategory: (selection: ReservationSelection) => void }) {
  const { t } = useLanguage()

  return (
    <section id="service" className={cn(sectionPadding, snapCentered)}>
      <Reveal className={sectionContainer}>
        <div className={sectionHeading}>
          <div>
            <span className={eyebrow}>{t.services.eyebrow}</span>
            <h2 className={sectionTitle}>{t.services.title}</h2>
          </div>
          <p className={sectionHeadingDesc}>{t.services.desc}</p>
        </div>

        <div className="grid grid-cols-4 gap-x-4 gap-y-7 pt-8 md:gap-x-6 md:gap-y-8 lg:grid-cols-6 lg:gap-y-9 max-[850px]:grid-cols-4 max-[600px]:gap-x-1 max-[600px]:gap-y-3 max-[600px]:pt-5 landing-wide:gap-x-7 landing-wide:gap-y-8">
          {categories.map(({ id, iconSrc, device }) => {
            const name = t.services.categories[id] || id
            return (
              <button
                key={id}
                type="button"
                className="group flex cursor-pointer flex-col items-center gap-3 rounded-[20px] border border-transparent bg-white px-1.5 py-3.5 text-[14px] leading-[1.4] font-[550] break-keep text-[#20252c] transition-all duration-200 ease-[cubic-bezier(0.16,1,0.3,1)] hover:-translate-y-1 hover:border-[#dbeafe] hover:bg-[#fafcff] hover:shadow-[0_12px_24px_-6px_rgba(22,89,185,0.12)] max-[600px]:gap-2.5 max-[600px]:px-0 max-[600px]:py-3 max-[600px]:text-[12px] landing-wide:gap-3.5 landing-wide:px-2 landing-wide:py-4"
                onClick={() => onSelectCategory({ device, symptom: `${name} ${t.services.symptomSuffix}` })}
              >
                <span className="grid h-[76px] w-[76px] place-items-center rounded-[24px] border border-[#eef2f7] bg-[#f6f8fb] transition-all duration-[250ms] ease-[ease] group-hover:border-brand-soft-border group-hover:bg-brand-soft max-[600px]:h-[54px] max-[600px]:w-[54px] max-[600px]:rounded-[14px] landing-wide:h-[88px] landing-wide:w-[88px]">
                  <img
                    className="h-12 w-12 object-contain [filter:drop-shadow(0_4px_10px_rgba(0,0,0,0.06))] transition-transform duration-[250ms] ease-[cubic-bezier(0.34,1.56,0.64,1)] group-hover:scale-110 landing-wide:h-14 landing-wide:w-14"
                    src={iconSrc}
                    alt={name}
                    width={46}
                    height={46}
                    loading="lazy"
                  />
                </span>
                <span className="text-[14px] font-semibold tracking-[-0.02em] text-[#2d3748] transition-colors duration-150 ease-[ease] group-hover:text-brand landing-wide:text-[15px]">{name}</span>
              </button>
            )
          })}
        </div>
      </Reveal>
    </section>
  )
}
