# Kế hoạch Tích hợp API cho trang Đặt Sân (BookPitch.tsx)

Việc chuyển từ dữ liệu giả lập (mock data) sang gọi API thực tế để lấy lịch theo ngày và loại sân từ cơ sở dữ liệu **sẽ không làm thay đổi quá nhiều về mặt giao diện (UI) hoặc cấu trúc HTML/CSS hiện tại**, nhưng **sẽ thay đổi đáng kể về mặt logic (Logic & State Management)** bên trong component.

Dưới đây là phân tích chi tiết và kế hoạch triển khai:

## 1. Đánh giá mức độ thay đổi

### Những phần KHÔNG thay đổi nhiều (UI/UX)
- Giao diện lưới (Matrix Gantt) trên Desktop và danh sách thẻ (Mobile Cards) vẫn giữ nguyên cấu trúc.
- Bộ lọc (Filter) ngày và loại sân vẫn giữ nguyên.
- Các CSS class, style, hiệu ứng hover, màu sắc (trống, giờ vàng, đã đặt, bảo trì) không bị ảnh hưởng.

### Những phần SẼ thay đổi (Logic & State)
- **Xóa bỏ Mock Data**: Không còn import `mockPitches`, `mockTimeSlots`, `mockBookings`.
- **Thêm State Management**: Cần thêm các `useState` để lưu trữ dữ liệu trả về từ API (`pitches`, `timeSlots`, `bookings`, `pitchTypes`).
- **Thêm Loading State**: Cần thêm state `isLoading`, `error` để hiển thị spinner hoặc thông báo lỗi trong lúc chờ API phản hồi.
- **Thêm Effect Hooks**: Dùng `useEffect` để gọi API mỗi khi component mount hoặc khi `selectedDate`, `selectedPitchType` thay đổi.
- **Cập nhật hàm `getSlotStatus`**: Hàm này sẽ dựa vào state `bookings` và `pitches` thực tế thay vì mock data.

---

## 2. Kế hoạch triển khai chi tiết (Implementation Plan)

### Bước 1: Định nghĩa các TypeScript Interfaces (Models)
Tạo file `src/types/pitch.ts` (hoặc định nghĩa trực tiếp) cho các dữ liệu trả về từ API để đảm bảo type-safety:
```typescript
export interface PitchType {
  id: string; // hoặc number
  name: string; // VD: "Sân 5 người"
  capacity: number; // VD: 5
}

export interface Pitch {
  id: string;
  name: string;
  typeId: string; // Tham chiếu đến PitchType
  capacity: number;
  status: 'AVAILABLE' | 'MAINTENANCE';
}

export interface TimeSlot {
  id: string;
  startTime: string;
  endTime: string;
  basePrice: number;
  isPeak: boolean;
}

export interface Booking {
  id: string;
  pitchId: string;
  timeSlotId: string;
  date: string;
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED';
}
```

### Bước 2: Tạo các API Services
Cập nhật hoặc tạo mới file `src/services/api.ts` (hoặc dùng axios/fetch trực tiếp):
- `getPitchTypes()`: Gọi API `/api/pitch-types` để đổ dữ liệu vào dropdown "Loại sân".
- `getPitches(typeId?)`: Gọi API `/api/pitches` để lấy danh sách sân bóng (có thể lọc theo loại).
- `getTimeSlots()`: Gọi API `/api/timeslots` để lấy danh sách khung giờ cố định.
- `getBookingsByDate(date)`: Gọi API `/api/bookings?date=YYYY-MM-DD` để lấy lịch đã đặt trong ngày cụ thể.

### Bước 3: Cập nhật component `BookPitch.tsx`

**A. Thêm State:**
```tsx
const [pitches, setPitches] = useState<Pitch[]>([]);
const [pitchTypes, setPitchTypes] = useState<PitchType[]>([]);
const [timeSlots, setTimeSlots] = useState<TimeSlot[]>([]);
const [bookings, setBookings] = useState<Booking[]>([]);

const [isLoading, setIsLoading] = useState<boolean>(true);
const [error, setError] = useState<string | null>(null);
```

**B. Sử dụng `useEffect` để fetch dữ liệu:**
```tsx
// Lấy dữ liệu tĩnh ban đầu (Loại sân, Sân, Khung giờ)
useEffect(() => {
  const fetchInitialData = async () => {
    try {
      setIsLoading(true);
      const [typesRes, pitchesRes, slotsRes] = await Promise.all([
        getPitchTypes(),
        getPitches(),
        getTimeSlots()
      ]);
      setPitchTypes(typesRes.data);
      setPitches(pitchesRes.data);
      setTimeSlots(slotsRes.data);
    } catch (err) {
      setError("Không thể tải dữ liệu sân bóng");
    } finally {
      setIsLoading(false);
    }
  };
  fetchInitialData();
}, []);

// Lấy dữ liệu booking mỗi khi ngày thay đổi
useEffect(() => {
  const fetchBookings = async () => {
    try {
      setIsLoading(true);
      const res = await getBookingsByDate(selectedDate);
      setBookings(res.data);
    } catch (err) {
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };
  fetchBookings();
}, [selectedDate]);
```

**C. Cập nhật hàm xử lý logic và UI:**
- Hàm `getSlotStatus` sẽ tham chiếu biến `bookings` (từ API) thay vì `mockBookings`.
- Option của thẻ `<select>` Loại Sân sẽ được map từ state `pitchTypes`.
- Bổ sung UI Loading: Bọc phần hiển thị danh sách sân bằng một điều kiện kiểm tra `if (isLoading) return <LoadingSpinner />`.
