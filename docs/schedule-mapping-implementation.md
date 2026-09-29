# Hướng Dẫn Triển Khai Chức Năng Lịch Đặt Sân (Schedule Grid Mapping)

Tài liệu này mô tả giải pháp kỹ thuật để lấy và hiển thị ma trận trạng thái sân bóng (Khung giờ x Sân) trên giao diện hệ thống.

## 1. Yêu cầu Bài Toán
- Hiển thị lưới danh sách sân và khung giờ theo ngày được chọn.
- Ánh xạ các đơn đặt (booking) hiện tại vào đúng Khớp Hàng (Sân) và Khớp Cột (Khung Giờ).
- Lọc bỏ các đơn đặt đã bị hủy hoặc hoàn tiền (không được làm hiển thị "Đã đặt").
- Hỗ trợ bộ lọc tìm kiếm theo Loại sân (VD: Sân 5, Sân 7).
- Đảm bảo tối ưu hóa tốc độ (tránh Cross-Join quá mức ở SQL, chuyển logic map sang Java/React).

## 2. Triển khai ở Backend (Spring Boot)

### 2.1. Cập nhật `BookingRepository`
Tạo câu truy vấn lấy danh sách Booking trong ngày.
- **Loại bỏ** các đơn có trạng thái `CANCELLED` và `REFUNDED` (đây là các đơn đã giải phóng ca, trả lại sân trống).
- **Hỗ trợ lọc** theo `pitchTypeId` (nếu null thì lấy tất cả loại sân).

```java
@Query("SELECT b FROM Booking b WHERE b.bookingDate = :date " +
       "AND b.status NOT IN ('CANCELLED', 'REFUNDED') " +
       "AND (:pitchTypeId IS NULL OR b.pitch.pitchType.id = :pitchTypeId)")
List<Booking> findOccupyingBookings(
        @Param("date") LocalDate date, 
        @Param("pitchTypeId") Long pitchTypeId
);
```

### 2.2. Tạo DTO trả về (`ScheduleGridItemResponse`)
Để tối ưu Payload cho Frontend, ta không trả về toàn bộ Entity Booking khổng lồ mà chỉ trả về 3 dữ liệu cốt lõi: ID Sân, ID Khung Giờ và Trạng thái.

```java
@Data
@AllArgsConstructor
public class ScheduleGridItemResponse {
    private Long pitchId;
    private Long timeSlotId;
    private String status; // VD: BOOKED, MAINTENANCE, PENDING_HOLD
}
```

### 2.3. Cập nhật `BookingController`
Khai báo API public trả về mảng dữ liệu đã chuẩn hóa.

```java
@GetMapping("/public/schedule-grid")
public ResponseEntity<ApiResponse<List<ScheduleGridItemResponse>>> getScheduleGrid(
        @RequestParam LocalDate date,
        @RequestParam(required = false) Long pitchTypeId
) {
    List<Booking> bookings = bookingRepository.findOccupyingBookings(date, pitchTypeId);
    
    List<ScheduleGridItemResponse> gridItems = bookings.stream()
            .map(b -> new ScheduleGridItemResponse(
                    b.getPitch().getId(), 
                    b.getTimeSlot().getId(), 
                    b.getStatus().name()
            ))
            .collect(Collectors.toList());
            
    return ResponseEntity.ok(ApiResponse.success("Success", gridItems));
}
```

## 3. Triển khai ở Frontend (ReactJS/TypeScript)

### 3.1. Cập nhật Service (`pitchService.ts`)
Gửi thêm `pitchTypeId` qua query params để Backend lọc:

```typescript
export const pitchService = {
  getScheduleGrid: async (date: string, pitchTypeId?: string | number): Promise<GridSlot[]> => {
    const params: any = { date };
    if (pitchTypeId && pitchTypeId !== 'all') {
      params.pitchTypeId = pitchTypeId;
    }
    
    const res = await api.get<ApiResponse<GridSlot[]>>('/bookings/schedule-grid', { params });
    return res.data?.data || [];
  }
}
```

### 3.2. Cập nhật React Query (`usePitchQueries.ts`)
Bổ sung `pitchTypeId` vào `queryKey` để khi người dùng đổi loại sân, React Query sẽ tự động gọi lại API mới nhất.

```typescript
export const useScheduleGrid = (date: string, pitchTypeId: string) => {
  return useQuery({
    queryKey: ['scheduleGrid', date, pitchTypeId],
    queryFn: () => pitchService.getScheduleGrid(date, pitchTypeId),
  });
};
```

### 3.3. Thuật Toán Render tại Giao Diện (`BookPitch.tsx`)
Thuật toán bao gồm 2 vòng lặp lồng nhau (Sân -> Khung giờ). Tại mỗi toạ độ (Ô), trước hết ta kiểm tra xem bản thân **Sân có đang bảo trì không**, nếu không bảo trì thì mới dùng hàm `.find()` để dò xem API có trả về Booking nào dính với toạ độ đó không.

```tsx
const { data: gridData = [] } = useScheduleGrid(selectedDate, selectedPitchType);

const getSlotStatus = (pitch: Pitch, timeSlotId: number) => {
  // 1. Ưu tiên kiểm tra trạng thái bảo trì của sân trước
  if (pitch.status === 'MAINTENANCE') {
     return 'maintenance'; // Chặn toàn bộ khung giờ của sân này
  }

  // 2. Điểm mấu chốt: Map cả hàng (pitch) và cột (timeslot)
  const gridItem = gridData.find(g => g.pitchId === pitch.id && g.timeSlotId === timeSlotId);
  
  if (!gridItem) return 'available';
  
  // 3. Xử lý trường hợp "Đang chờ cọc" (Giữ chỗ 10 phút)
  if (gridItem.status === 'PENDING_HOLD') return 'pending_hold';
  
  return 'booked'; // Các trạng thái còn lại (CONFIRMED, IN_PROGRESS, COMPLETED...)
};

// ... trong JSX:
<tbody>
  {pitches.map(pitch => (
    <tr key={pitch.id}>
      <td>{pitch.name}</td>
      {timeSlots.map(slot => {
         // Chú ý: Truyền toàn bộ object pitch vào hàm thay vì chỉ truyền pitch.id
         const status = getSlotStatus(pitch, slot.id);
         return (
            <td key={slot.id}>
               {status === 'maintenance' && <div>Bảo Trì</div>}
               {status === 'pending_hold' && <div className="text-orange-500">Chờ Cọc</div>}
               {status === 'booked' && <div className="text-red-500">Đã Đặt</div>}
               {status === 'available' && <div className="text-green-500">Trống</div>}
            </td>
         )
      })}
    </tr>
  ))}
</tbody>
```

## 4. Tích hợp Bảng Giá (Price Matrix) Động Theo Ngày

Bảng giá phụ thuộc vào 3 yếu tố: `Loại ngày (DayType)`, `Loại sân (PitchType)` và `Khung giờ (TimeSlot)`. Vì các ô trống cũng cần hiển thị giá, API `getScheduleGrid` cần trả về thêm cấu hình giá của ngày hôm đó.

### 4.1. Xác định `DayType` (Trong Service)
Kiểm tra xem ngày đang chọn là Ngày lễ, Cuối tuần hay Ngày thường.

```java
import java.time.DayOfWeek;
import java.time.LocalDate;

public DayType determineDayType(LocalDate date) {
    if (holidayRepository.existsByHolidayDate(date)) {
        return DayType.HOLIDAY;
    }
    DayOfWeek dayOfWeek = date.getDayOfWeek();
    if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
        return DayType.WEEKEND;
    }
    return DayType.WEEKDAY;
}
```

### 4.2. Cập nhật DTO trả về cho Controller
Đóng gói cả `Bookings` và `Prices` vào 1 Object để Frontend chỉ cần gọi API 1 lần.

```java
@Data
@AllArgsConstructor
public class PriceItemResponse {
    private Long pitchTypeId; 
    private Boolean isPeakHour; // Thay timeSlotId bằng isPeakHour
    private BigDecimal price;
}

@Data
public class ScheduleGridResponse {
    private List<ScheduleGridItemResponse> bookings; 
    private List<PriceItemResponse> prices;          
}
```

### 4.3. Cập nhật `BookingController`
Lấy thêm bảng giá dựa vào `DayType` và gán vào Response.

```java
@GetMapping("/public/schedule-grid")
public ResponseEntity<ApiResponse<ScheduleGridResponse>> getScheduleGrid(@RequestParam LocalDate date) {
    ScheduleGridResponse response = new ScheduleGridResponse();

    // 1. Dữ liệu Bookings
    List<Booking> bookings = bookingRepository.findOccupyingBookings(date, null);
    // ... map ra List<ScheduleGridItemResponse>
    response.setBookings(gridItems);

    // 2. Dữ liệu Bảng giá
    DayType currentDayType = determineDayType(date);
    List<PriceMatrix> matrices = priceMatrixRepository.findByDayType(currentDayType);
    
    List<PriceItemResponse> priceItems = matrices.stream()
            .map(m -> new PriceItemResponse(m.getPitchType().getId(), m.getIsPeakHour(), m.getPrice()))
            .collect(Collectors.toList());
            
    response.setPrices(priceItems);

    return ResponseEntity.ok(ApiResponse.success("Success", response));
}
```

### 4.4. Cập nhật Map Giá ở Frontend (`BookPitch.tsx`)
Từ biến `gridData.prices`, ta tra cứu giá cho mỗi ô dựa vào ID Loại Sân và thuộc tính `isPeakHour` của Khung Giờ.

```tsx
const getPrice = (pitchTypeId: number, isPeakHour: boolean) => {
   const priceItem = gridData.prices?.find(p => p.pitchTypeId === pitchTypeId && p.isPeakHour === isPeakHour);
   return priceItem ? priceItem.price : 0;
};

// ... trong vòng lặp map:
{timeSlots.map(slot => {
   const status = getSlotStatus(pitch, slot.id);
   // Truyền boolean isPeakHour thay vì slot.id
   const currentPrice = getPrice(pitch.pitchType.id, slot.isPeakHour);
   
   return (
      <td key={slot.id} className="matrix-cell">
         <span className="font-bold text-sm">{formatPrice(currentPrice)}</span>
         {status === 'maintenance' && <div>Bảo Trì</div>}
         {status === 'pending_hold' && <div className="text-orange-500">Chờ Cọc</div>}
         {status === 'booked' && <div className="text-red-500">Đã Đặt</div>}
         {status === 'available' && <div className="text-green-500">Trống</div>}
      </td>
   )
})}
```

## 5. Tổng kết
Cách triển khai này đảm bảo Frontend không bị lỗi logic khi có các đơn hủy nằm chồng chéo trong Database, đồng thời tăng hiệu năng mạnh mẽ vì mọi tính toán map dữ liệu phức tạp đều được đưa ra khỏi tầng SQL (Database) và đưa về xử lý nhanh gọn tại Java/JS. Kèm theo đó, bảng giá được load động linh hoạt theo từng sự kiện (Ngày lễ, cuối tuần).
