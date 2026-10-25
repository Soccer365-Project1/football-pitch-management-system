export type AdminSlotStatus =
  | 'AVAILABLE'
  | 'CONFIRMED'
  | 'IN_PROGRESS'
  | 'WAITING_CANCELLATION'
  | 'WAITING_APPROVAL'
  | 'COMPLETED'
  | 'MAINTENANCE'
  | 'PENDING_HOLD'
  | (string & {});

export interface AdminTimelineSlotResponse {
  pitchId: number;
  pitchName: string;
  pitchTypeName?: string;
  timeSlotId: number;
  startTime: string;
  endTime: string;
  bookingDate: string;
  status: AdminSlotStatus;

  bookingId?: number | null;
  bookingCode?: string | null;
  guestName?: string | null;
  guestPhone?: string | null;
  totalAmount?: number | null;
  depositAmount?: number | null;
  remainingAmount?: number | null;
  customerNote?: string | null;
  cancellationReason?: string | null;
}

export interface AdminTimelineResponse {
  date: string;
  bookings: AdminTimelineSlotResponse[];
}
