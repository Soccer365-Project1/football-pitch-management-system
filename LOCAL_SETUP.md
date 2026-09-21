# Hướng dẫn chạy project local

## 1. Clone project
```bash
git clone https://github.com/Soccer365-Project1/football-pitch-management-system.git
cd football-pitch-management-system
```
Chuyển sang branch `develop`:
```bash
git checkout develop
git pull origin develop
```

---

## 2. Khởi động PostgreSQL và Redis
Đảm bảo **Docker Desktop** đang chạy.
Tại thư mục gốc project, chạy:
```bash
docker compose up -d
```
Kiểm tra container:
```bash
docker compose ps
```
Cần thấy:
- `football-postgres` (Up)
- `football-redis` (Up)

**Thông tin Database local:**
- **Host:** `localhost`
- **Port:** `5432`
- **Database:** `fpms`
- **Username:** `postgres`
- **Password:** `postgres`

> [!NOTE]
> Không cần cài PostgreSQL riêng vì project đã sử dụng PostgreSQL thông qua Docker.

---

## 3. Chạy Backend
Mở terminal mới:
```bash
cd backend
```
Trên Windows:
```powershell
.\mvnw.cmd spring-boot:run
```
Backend chạy tại: http://localhost:8080

**Kiểm tra Backend:**
Mở trình duyệt: http://localhost:8080/swagger-ui.html
Nếu Swagger UI hiển thị thì Backend đã chạy thành công.
*Giữ terminal Backend đang chạy.*

---

## 4. Chạy Frontend
Mở terminal mới:
```bash
cd frontend
```
Cài dependencies:
```bash
npm install
```
Chạy Frontend:
```bash
npm run dev
```
Sau khi chạy thành công, mở: http://localhost:5173

---

## 5. Kiểm tra hệ thống
Sau khi Frontend và Backend đều đang chạy:
1. Truy cập: http://localhost:5173
2. Vào trang Đăng nhập.
3. Sử dụng tài khoản Admin mặc định:
   - **Email:** `admin@soccer365.vn`
   - **Mật khẩu:** `123456`
4. Nếu đăng nhập thành công thì hệ thống local đã chạy hoàn chỉnh.

---

## 6. Các terminal cần mở
- **Terminal 1 — Docker:** `docker compose up -d`
- **Terminal 2 — Backend:** `cd backend` sau đó chạy `.\mvnw.cmd spring-boot:run`
- **Terminal 3 — Frontend:** `cd frontend` sau đó chạy `npm run dev`

---

## 7. Dừng project
Dừng Frontend và Backend: Nhấn `Ctrl + C` tại cửa sổ dòng lệnh.

Dừng PostgreSQL và Redis:
```bash
docker compose down
```
Lần sau chạy lại chỉ cần gõ:
```bash
docker compose up -d
```

---

## 8. Nếu gặp lỗi PostgreSQL port 5432
Nếu Docker báo port `5432` đã được sử dụng, kiểm tra bằng lệnh sau:
```cmd
netstat -ano | findstr :5432
```
Nếu máy đã cài PostgreSQL trực tiếp trên Windows và đang chạy, cần dừng PostgreSQL Windows trước khi chạy PostgreSQL bằng Docker.

Kiểm tra PostgreSQL Service:
```powershell
Get-Service | Where-Object {$_.Name -like "*postgres*"}
```
Ví dụ nếu service là `postgresql-x64-18`, mở PowerShell bằng quyền **Run as Administrator**, sau đó gõ:
```powershell
Stop-Service -Name "postgresql-x64-18"
```
Sau đó chạy lại Docker:
```bash
docker compose up -d
```

> [!WARNING]
> Không sử dụng `docker compose down -v` nếu không muốn xóa dữ liệu Database local.
