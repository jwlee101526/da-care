export type DeviceType = 'laptop' | 'smartphone' | 'appliance' | 'etc'

export interface ReservationSelection {
  device: DeviceType
  symptom: string
}

