export interface PitchType {
  id: number;
  name: string;
}

export interface Pitch {
  id: number;
  name: string;
  pitchType: PitchType;
  status: 'ACTIVE' | 'MAINTENANCE';
  description?: string;
}

export interface TimeSlot {
  id: number;
  startTime: string;
  endTime: string;
  isPeakHour: boolean;
}

export interface ScheduleGridItemResponse {
  pitchId: number;
  timeSlotId: number;
  status: string;
}

export interface PriceItemResponse {
  pitchTypeId: number;
  isPeakHour: boolean;
  price: number;
}

export interface ScheduleGridResponse {
  bookings: ScheduleGridItemResponse[];
  prices: PriceItemResponse[];
}
