import api from './api.ts';

export interface BookingCreationRequest {
  pitchId: number;
  timeSlotId: number;
  bookingDate: string;
  guestName: string;
  guestPhone: string;
  customerNote?: string;
}

export const bookingService = {
  createBooking: async (data: BookingCreationRequest) => {
    const response = await api.post('/bookings', data);
    return response.data;
  }
};
