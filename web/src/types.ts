export type DeviceType =
  | 'smartphone'
  | 'computer'
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

export interface ReservationSelection {
  device: DeviceType
  symptom: string
}

