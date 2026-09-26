/**
 * =========================================================================================
 * FILE TIỆN ÍCH THỜI GIAN (timeUtils.ts)
 * =========================================================================================
 * Tập hợp các hàm xử lý và tính toán thời gian dùng chung cho các màn hình quản trị.
 * Viết theo phong cách thuần túy (pure functions), đơn giản và dễ hiểu cho intern.
 */

/**
 * Hàm kiểm tra chuỗi thời gian có đúng định dạng 24 giờ chuẩn "HH:mm" hay không.
 * - Ví dụ hợp lệ: "06:00", "07:30", "18:45", "23:59"
 * - Ví dụ không hợp lệ: "6:00" (thiếu số 0), "24:00" (quá 23h), "12:60" (quá 59p), "abc"
 * 
 * @param time Chuỗi giờ cần kiểm tra
 * @returns true nếu hợp lệ từ 00:00 đến 23:59, ngược lại false
 */
export const isValidTimeFormat = (time: string): boolean => {
  if (!time) return false;
  // Biểu thức chính quy: Giờ từ 00-19 hoặc 20-23, Phút từ 00-59
  return /^([01]\d|2[0-3]):([0-5]\d)$/.test(time.trim());
};

/**
 * Hàm tính khoảng cách thời gian giữa Giờ bắt đầu và Giờ kết thúc thành đơn vị phút.
 * - Công thức cơ bản: (Giờ kết thúc * 60 + Phút kết thúc) - (Giờ bắt đầu * 60 + Phút bắt đầu)
 * - Ví dụ: "06:00" đến "07:30" -> (7*60 + 30) - (6*60 + 0) = 450 - 360 = 90 phút.
 * 
 * @param start Giờ bắt đầu (định dạng "HH:mm")
 * @param end Giờ kết thúc (định dạng "HH:mm")
 * @returns Số phút chênh lệch. Nếu giờ không đúng định dạng thì trả về 0.
 */
export const calculateDurationMinutes = (start: string, end: string): number => {
  if (!start || !end || !isValidTimeFormat(start) || !isValidTimeFormat(end)) {
    return 0;
  }

  // Tách chuỗi giờ và phút thành số nguyên
  const [startH, startM] = start.split(':').map(Number);
  const [endH, endM] = end.split(':').map(Number);

  const startTotalMinutes = startH * 60 + startM;
  const endTotalMinutes = endH * 60 + endM;

  return endTotalMinutes - startTotalMinutes;
};

/**
 * Hàm chuyển đổi số phút thành chuỗi mô tả thân thiện bằng tiếng Việt để hiển thị trên UI.
 * - Ví dụ: 60 phút -> "1 giờ"
 * - Ví dụ: 90 phút -> "1 giờ 30 phút"
 * - Ví dụ: 120 phút -> "2 giờ"
 * 
 * @param minutes Số phút cần định dạng
 * @returns Chuỗi văn bản mô tả thời lượng
 */
export const formatDuration = (minutes: number): string => {
  if (minutes <= 0) return '0 phút';

  const hours = Math.floor(minutes / 60);
  const remainingMins = minutes % 60;

  if (hours > 0 && remainingMins > 0) {
    return `${hours} giờ ${remainingMins} phút`;
  }
  if (hours > 0) {
    return `${hours} giờ`;
  }
  return `${remainingMins} phút`;
};
