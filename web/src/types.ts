export type DeviceType =
  | 'smartphone'
  | 'computer'
  | 'laptop'
  | 'tv'
  | 'console'
  | 'aircon'
  | 'washing'
  | 'fridge'
  | 'microwave'
  | 'cleaner'
  | 'internet'
  | 'audio'
  | 'repair'
  | 'appliance'
  | 'etc'

export interface ReservationSelection {
  device: DeviceType
  symptom: string
}

