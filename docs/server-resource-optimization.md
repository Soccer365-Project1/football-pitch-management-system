# Tối ưu tài nguyên server

## Frontend trên Vercel

Vercel CDN tự động nén HTML/CSS/JS theo `Accept-Encoding` của trình duyệt,
ưu tiên Brotli khi được hỗ trợ và hỗ trợ Gzip. Không tự gán `Content-Encoding`
trong vercel.json vì header phải khớp với dữ liệu thực sự được nén.
Nguồn: https://vercel.com/docs/how-vercel-cdn-works/compression

`frontend/vercel.json` cho phép trình duyệt giữ file `/assets/*` một năm.
Vite đặt hash trong tên file build, nên bản build mới có URL mới.
Giữ nguyên rewrite về index.html cho các đường dẫn React Router.

Sau khi deploy, mở DevTools > Network, chọn file JS/CSS và kiểm tra:
`Content-Encoding: br` hoặc `gzip`, cùng `Cache-Control` cho assets.
Ví dụ: `curl -I -H "Accept-Encoding: br, gzip" https://<domain>/assets/<file>.js`.
Local Vite dev không dùng CDN Vercel nên không chứng minh được nén production.

## Redis cache backend

Spring Cache dùng RedisCacheManager; cache lưu DTO, không lưu JPA entity.
Các DTO được serialize bằng JDK serializer mặc định của Spring Data Redis.
Redis phải nằm trong mạng tin cậy, không mở công khai.

| Cache | Nội dung | Xóa khi |
| --- | --- | --- |
| pitches | Danh sách theo bộ lọc/phân trang, danh sách public, chi tiết sân | Tạo/sửa/xóa sân, đổi trạng thái |
| pitchTypes | Danh sách loại sân | Hết TTL (hiện chưa có API sửa loại sân) |
| prices | Bảng giá theo loại sân, toàn bộ bảng giá, chi tiết giá | Lưu/xóa giá |
| timeSlots | Danh sách và chi tiết khung giờ, khung giờ public | Tạo/sửa/xóa khung giờ hoặc thay đổi giá |

Lần đọc đầu truy vấn database và ghi Redis. Các lần đọc cùng tham số lấy DTO
từ Redis, bỏ qua thân hàm service và các truy vấn repository bên trong.
Key bao gồm tên phương thức và các tham số để phân biệt DTO, bộ lọc và trang.
Prefix `fpms:catalog:v1:` tách cache danh mục khỏi Redis blacklist JWT.
TTL mặc định 15 phút; cấu hình `app.cache.catalog-ttl=PT15M` để thay đổi.
Cache manager transaction-aware chỉ ghi/xóa sau commit thành công.
Thay đổi giá cũng xóa cache khung giờ vì DTO khung giờ chứa giá.

Không cache lịch trống, booking hoặc bước tính giá/kiểm tra khi đặt sân:
các bước này tiếp tục đọc database để xử lý dữ liệu giao dịch hiện thời.
Thay đổi trực tiếp bằng SQL chỉ được phản ánh khi TTL hết hoặc xóa cache.
Redis lỗi sẽ được báo lỗi theo mặc định; cấu hình này không tự chuyển sang DB.
Khi triển khai bản thay đổi cấu trúc DTO, tăng phiên bản prefix để tránh đọc
dữ liệu serialize không tương thích từ bản cũ.

## Kiểm tra

1. Chạy PostgreSQL/Redis và khởi động lại backend để nạp cấu hình cache.
2. Gọi cùng API danh mục hai lần: lần đầu có SQL SELECT, lần sau không có
   SELECT cho phương thức đã cache. Kiểm tra Redis bằng SCAN với pattern
   `fpms:catalog:v1:*` và TTL của key (không dùng KEYS trên production).
3. Sửa giá qua API, đọc lại giá và khung giờ để xác nhận giá mới; thử thao tác
   bị rollback để xác nhận không xóa cache do giao dịch thất bại.
4. Không dùng tỷ lệ giảm tải giả định: đo số SELECT, thời gian phản hồi và
   hit/miss trong cùng một kịch bản trước/sau để báo cáo kết quả thực tế.
