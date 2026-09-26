import { useQuery } from '@tanstack/react-query';
import { pitchService } from '../../services/pitchService.ts';

export const usePitches = (pitchTypeId?: string | number) => {
  return useQuery({
    queryKey: ['pitches', pitchTypeId],
    queryFn: () => pitchService.getPitches(pitchTypeId),
  });
};

export const useTimeSlots = () => {
  return useQuery({
    queryKey: ['timeSlots'],
    queryFn: () => pitchService.getTimeSlots(),
  });
};

export const useScheduleGrid = (date: string, pitchTypeId: string | number) => {
  return useQuery({
    queryKey: ['scheduleGrid', date, pitchTypeId],
    queryFn: () => pitchService.getScheduleGrid(date, pitchTypeId),
  });
};

export const usePitchTypes = () => {
  return useQuery({
    queryKey: ['pitchTypes'],
    queryFn: () => pitchService.getPitchTypes(),
  });
};
