import { useQuery } from '@tanstack/react-query';
import { bookingService, type BookingFilterParams } from '../../services/bookingService.ts';

export const useMyBookings = (params: BookingFilterParams = { page: 1, pageSize: 10 }) => {
  return useQuery({
    queryKey: ['myBookings', params],
    queryFn: async () => {
      const res = await bookingService.getMyBookings(params);
      return res?.data;
    },
    placeholderData: (previousData) => previousData,
  });
};

