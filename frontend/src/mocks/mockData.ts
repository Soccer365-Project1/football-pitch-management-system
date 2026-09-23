export const mockPitches = [
  { id: '1', name: 'Sân 1 (Cỏ nhân tạo)', type: 5, status: 'ACTIVE' },
  { id: '2', name: 'Sân 2 (Cỏ nhân tạo)', type: 5, status: 'ACTIVE' },
  { id: '3', name: 'Sân 3 (Cỏ nhân tạo)', type: 5, status: 'MAINTENANCE' },
  { id: '4', name: 'Sân 4 (Cỏ tự nhiên)', type: 7, status: 'ACTIVE' },
  { id: '5', name: 'Sân 5 (Cỏ tự nhiên)', type: 7, status: 'ACTIVE' },
  { id: '6', name: 'Sân 6 (Cỏ tự nhiên)', type: 7, status: 'ACTIVE' },
  { id: '7', name: 'Sân 7 (Cỏ tự nhiên)', type: 7, status: 'ACTIVE' },
];

export const mockTimeSlots = [
  { id: 'ts1', startTime: '16:00', endTime: '17:30', basePrice: 300000, isPeak: false },
  { id: 'ts2', startTime: '17:30', endTime: '19:00', basePrice: 450000, isPeak: true },
  { id: 'ts3', startTime: '19:00', endTime: '20:30', basePrice: 450000, isPeak: true },
  { id: 'ts4', startTime: '20:30', endTime: '22:00', basePrice: 400000, isPeak: false },
];

export const mockBookings = [
  { pitchId: '1', timeSlotId: 'ts2', date: new Date().toISOString().split('T')[0], status: 'CONFIRMED' },
  { pitchId: '4', timeSlotId: 'ts3', date: new Date().toISOString().split('T')[0], status: 'CONFIRMED' },
];
