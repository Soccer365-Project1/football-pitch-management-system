import api from './api.ts';
import type { ApiResponse } from '../types/common.ts';
import type { Pitch, TimeSlot, GridSlot } from '../types/pitch.ts';

export const pitchService = {
  // Lấy danh sách sân hoạt động
  getPitches: async (pitchTypeId?: number | string): Promise<Pitch[]> => {
    const res = await api.get<ApiResponse<Pitch[]>>('/pitches', { 
      params: { pitchTypeId: pitchTypeId && pitchTypeId !== 'all' ? pitchTypeId : undefined } 
    });
    return res.data?.data || [];
  },

  // Lấy danh sách khung giờ hoạt động
  getTimeSlots: async (): Promise<TimeSlot[]> => {
    const res = await api.get<ApiResponse<TimeSlot[]>>('/timeslots');
    return res.data?.data || [];
  },

  // Lấy lưới trạng thái đặt sân theo ngày
  getScheduleGrid: async (date: string): Promise<GridSlot[]> => {
    const res = await api.get<ApiResponse<GridSlot[]>>('/bookings/schedule-grid', { 
      params: { date } 
    });
    return res.data?.data || [];
  },

  // Lấy danh sách loại sân
  getPitchTypes: async (): Promise<PitchType[]> => {
    const res = await api.get<ApiResponse<PitchType[]>>('/pitches/types');
    return res.data?.data || [];
  }
};
