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

interface CategoryItem {
  id: keyof typeof import('../locales/ko').ko['services']['categories']
  iconSrc: string
  device: DeviceType
}

const categories: CategoryItem[] = [
  { id: 'phone-tablet', iconSrc: smartphoneTabletIcon, device: 'smartphone' },
  { id: 'computer', iconSrc: computerIcon, device: 'laptop' },
  { id: 'tv', iconSrc: smartTvIcon, device: 'appliance' },
  { id: 'console', iconSrc: consoleIcon, device: 'etc' },
  { id: 'aircon', iconSrc: airconIcon, device: 'appliance' },
  { id: 'washing', iconSrc: washingIcon, device: 'appliance' },
  { id: 'fridge', iconSrc: fridgeIcon, device: 'appliance' },
  { id: 'microwave', iconSrc: microwaveIcon, device: 'appliance' },
  { id: 'cleaner', iconSrc: vacuumIcon, device: 'appliance' },
  { id: 'internet', iconSrc: wifiIcon, device: 'etc' },
  { id: 'etc', iconSrc: audioIcon, device: 'etc' },
  { id: 'repair', iconSrc: emergencyAsIcon, device: 'etc' },
]

export function ServiceSection({ onSelectCategory }: { onSelectCategory: (selection: ReservationSelection) => void }) {
  const { t } = useLanguage()

  return (
    <section id="service" className="section service-section">
      <Reveal className="container">
        <div className="section-heading">
          <div>
            <span className="eyebrow">{t.services.eyebrow}</span>
            <h2>{t.services.title}</h2>
          </div>
          <p>{t.services.desc}</p>
        </div>

        <div className="category-grid">
          {categories.map(({ id, iconSrc, device }) => {
            const name = t.services.categories[id] || id
            return (
              <button
                key={id}
                type="button"
                className="category"
                onClick={() => onSelectCategory({ device, symptom: `${name} ${t.services.symptomSuffix}` })}
              >
                <span className="category-icon">
                  <img
                    src={iconSrc}
                    alt={name}
                    width={46}
                    height={46}
                    loading="lazy"
                  />
                </span>
                <span className="category-name">{name}</span>
              </button>
            )
          })}
        </div>
      </Reveal>
    </section>
  )
}
