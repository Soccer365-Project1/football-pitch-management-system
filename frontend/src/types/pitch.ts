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

export interface GridSlot {
  pitchId: number;
  pitchName: string;
  timeSlotId: number;
  startTime: string;
  endTime: string;
  isPeakHour: boolean;
  status: 'AVAILABLE' | 'BOOKED' | 'MAINTENANCE';
  bookingId?: number;
}
