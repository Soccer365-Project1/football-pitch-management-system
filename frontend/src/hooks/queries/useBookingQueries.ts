import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
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

export const useAdminTimeline = (date: string, pitchTypeId?: string | number) => {
  return useQuery({
    queryKey: ['adminTimeline', date, pitchTypeId],
    queryFn: async () => {
      const res = await bookingService.getAdminTimeline(date, pitchTypeId);
      return res;
    },
    refetchOnWindowFocus: true,
  });
};

export const useApproveBooking = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (bookingId: number) => bookingService.approveBooking(bookingId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminTimeline'] });
    },
  });
};

export const useCheckInBooking = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (bookingId: number) => bookingService.checkInBooking(bookingId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminTimeline'] });
    },
  });
};
