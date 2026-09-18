import type { DeviceType } from '../types'
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

export interface CategoryDefinition {
  deviceType: DeviceType
  labelKo: string
  labelEn: string
  descKo: string
  descEn: string
  iconSrc: string
}

export const DEVICE_CATEGORIES: CategoryDefinition[] = [
  {
    deviceType: 'smartphone',
    labelKo: '스마트폰 · 태블릿',
    labelEn: 'Smartphone / Tablet',
    descKo: '액정 파손, 배터리 교체, 침수, 충전 불량',
    descEn: 'Screen repair, battery replacement, water damage',
    iconSrc: smartphoneTabletIcon,
  },
  {
    deviceType: 'computer',
    labelKo: '데스크탑 · PC / 노트북',
    labelEn: 'Laptop / PC',
    descKo: '전원 불량, 부팅 오류, 팬 소음, 부품 업그레이드',
    descEn: 'Power failure, booting issues, upgrade',
    iconSrc: computerIcon,
  },
  {
    deviceType: 'tv',
    labelKo: '스마트 TV',
    labelEn: 'Smart TV',
    descKo: '화면 꺼짐, 백라이트 불량, 사운드 출력 오류',
    descEn: 'Display issue, backlight problem, sound issue',
    iconSrc: smartTvIcon,
  },
  {
    deviceType: 'aircon',
    labelKo: '에어컨',
    labelEn: 'Air Conditioner',
    descKo: '냉방 약함, 가스 누출, 냄새 / 소음, 필터 점검',
    descEn: 'Cooling issue, gas leak, noise / filter',
    iconSrc: airconIcon,
  },
  {
    deviceType: 'washing',
    labelKo: '세탁기 · 건조기',
    labelEn: 'Washer / Dryer',
    descKo: '탈수 불량, 급수 / 배수 에러, 심한 진동',
    descEn: 'Spinning failure, water supply/drain error',
    iconSrc: washingIcon,
  },
  {
    deviceType: 'fridge',
    labelKo: '냉장고',
    labelEn: 'Refrigerator',
    descKo: '냉기 약함, 컴프레서 소음, 도어 패킹 점검',
    descEn: 'Cooling performance, compressor noise',
    iconSrc: fridgeIcon,
  },
  {
    deviceType: 'microwave',
    labelKo: '전자레인지 · 인덕션',
    labelEn: 'Microwave / Induction',
    descKo: '가열 불량, 작동 중 꺼짐, 조작 패널 오류',
    descEn: 'Heating failure, unexpected shutdown',
    iconSrc: microwaveIcon,
  },
  {
    deviceType: 'cleaner',
    labelKo: '청소기',
    labelEn: 'Vacuum Cleaner',
    descKo: '흡입력 저하, 모터 이상음, 배터리 방전',
    descEn: 'Suction loss, motor issue, battery failure',
    iconSrc: vacuumIcon,
  },
  {
    deviceType: 'console',
    labelKo: '게임 콘솔',
    labelEn: 'Gaming Console',
    descKo: '발열 / 팬 굉음, HDMI 출력 불가, 드라이브 인식',
    descEn: 'Overheating, HDMI output failure, drive recognition',
    iconSrc: consoleIcon,
  },
  {
    deviceType: 'internet',
    labelKo: '네트워크 · 공유기',
    labelEn: 'Network / Router',
    descKo: '인터넷 끊김, 와이파이 신호 미약, 공유기 설정',
    descEn: 'Connection drop, weak Wi-Fi signal',
    iconSrc: wifiIcon,
  },
  {
    deviceType: 'audio',
    labelKo: '음향 기기 · 오디오',
    labelEn: 'Audio System',
    descKo: '노이즈 발생, 스피커 단선, 블루투스 연결 장애',
    descEn: 'Noise, disconnection, Bluetooth issue',
    iconSrc: audioIcon,
  },
  {
    deviceType: 'repair',
    labelKo: '긴급 출장 A/S',
    labelEn: 'Emergency Service',
    descKo: '당일 빠른 방문, 종합 가전 긴급 현장 점검',
    descEn: 'Same-day urgent on-site inspection',
    iconSrc: emergencyAsIcon,
  },
]

export function getCategoryInfo(deviceType: string, lang: 'ko' | 'en' = 'ko'): string {
  const normalized = deviceType.toLowerCase()
  if (normalized === 'laptop') return lang === 'en' ? 'Laptop / PC' : '데스크탑 · PC / 노트북'
  if (normalized === 'appliance') return lang === 'en' ? 'Home Appliance' : '생활 가전'
  if (normalized === 'etc') return lang === 'en' ? 'Other Device' : '기타 기기'
  const match = DEVICE_CATEGORIES.find(c => c.deviceType === normalized)
  if (match) return lang === 'en' ? match.labelEn : match.labelKo
  return deviceType
}

export function getCategoryDefinition(deviceType: string): CategoryDefinition | undefined {
  const normalized = deviceType.toLowerCase()
  if (normalized === 'laptop') return DEVICE_CATEGORIES.find(c => c.deviceType === 'computer')
  return DEVICE_CATEGORIES.find(c => c.deviceType === normalized)
}
