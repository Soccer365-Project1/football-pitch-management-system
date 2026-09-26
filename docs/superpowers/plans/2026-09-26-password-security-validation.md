# Kế Hoạch Triển Khai Chuẩn Hóa & Tăng Cường Bảo Mật Mật Khẩu (FPMS-119)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Khắc phục triệt để lỗi `TC_REG_20, 21` (chấp nhận mật khẩu chứa khoảng trắng), nâng cấp chính sách bảo mật mật khẩu độ dài từ **6 đến 64 ký tự**, bắt buộc có chữ cái, chữ số, ký tự đặc biệt, không chứa khoảng trắng và chặn mật khẩu mới trùng mật khẩu cũ trên toàn hệ thống (Đăng ký, Đổi mật khẩu, Đặt lại mật khẩu) ở cả Backend và Frontend.

**Biểu thức chính quy (Regex):**
`^(?=.{6,64}$)(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9\s])\S+$`
*(Tối ưu từ mẫu `^(?=\S)(?=.{6,64}$)(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9\s])(?=.*\S$).+$`: đảm bảo 6-64 ký tự, có chữ, có số, có ký tự đặc biệt, và dùng `\S+$` để chặn triệt để 100% khoảng trắng ở cả đầu, giữa và cuối chuỗi).*

**Thông báo lỗi chuẩn:**
> *"Mật khẩu phải từ 6 đến 64 ký tự, bao gồm ít nhất 1 chữ cái, 1 chữ số, 1 ký tự đặc biệt và không chứa khoảng trắng"*

**Tech Stack:** Spring Boot 3, Jakarta Validation, JUnit 5, Mockito, React 18, TypeScript.

---

## Global Constraints
- Tuân thủ quy tắc comment tiếng Việt chi tiết cho mọi thay đổi Frontend (theo [GEMINI.md](file:///f:/Working/JavaBackend/football-pitch-management-system/GEMINI.md)).
- Đảm bảo 100% unit tests trong backend pass (cập nhật các mock data trong test sang định dạng thỏa mãn regex: ví dụ `Password@123`).
- Thông báo lỗi phía BE và FE phải thống nhất, rõ ràng, thân thiện với người dùng.

---

### Task 1: Backend - Cập nhật Regex bảo mật cho DTOs & Bổ sung ErrorCode

**Files:**
- Modify: `backend/src/main/java/com/fpms/exception/ErrorCode.java`
- Modify: `backend/src/main/java/com/fpms/dto/request/RegisterRequest.java`
- Modify: `backend/src/main/java/com/fpms/dto/request/ChangePasswordRequest.java`
- Modify: `backend/src/main/java/com/fpms/dto/request/ResetPasswordRequest.java`

- [ ] **Step 1: Bổ sung mã lỗi NEW_PASSWORD_SAME_AS_OLD trong `ErrorCode.java`**
  - Thêm `NEW_PASSWORD_SAME_AS_OLD(2017, HttpStatus.BAD_REQUEST, "Mật khẩu mới không được trùng với mật khẩu hiện tại")`.

- [ ] **Step 2: Cập nhật validation trong `RegisterRequest.java`**
  - Thay `@Size(min = 6)` bằng:
    ```java
    @NotBlank(message = "Mật khẩu không được để trống")
    @Pattern(
        regexp = "^(?=.{6,64}$)(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S+$",
        message = "Mật khẩu phải từ 6 đến 64 ký tự, bao gồm ít nhất 1 chữ cái, 1 chữ số, 1 ký tự đặc biệt và không chứa khoảng trắng"
    )
    private String password;
    ```

- [ ] **Step 3: Cập nhật validation trong `ChangePasswordRequest.java` và `ResetPasswordRequest.java`**
  - Áp dụng cùng annotation `@Pattern` trên cho trường `newPassword` với thông báo tương ứng cho mật khẩu mới.

- [ ] **Step 4: Kiểm tra biên dịch backend**
  - Chạy `./mvnw test-compile` để kiểm tra cú pháp.

---

### Task 2: Backend - Bổ sung nghiệp vụ chặn trùng mật khẩu cũ & Đồng bộ Unit Tests

**Files:**
- Modify: `backend/src/main/java/com/fpms/service/impl/UserServiceImpl.java`
- Modify: `backend/src/test/java/com/fpms/service/UserServiceTest.java`
- Modify: `backend/src/test/java/com/fpms/service/AuthServiceTest.java`
- Modify: `backend/src/test/java/com/fpms/controller/AuthControllerTest.java`
- Modify: `backend/src/test/java/com/fpms/controller/UserControllerTest.java` (nếu có)

- [ ] **Step 1: Thêm logic chặn mật khẩu mới trùng mật khẩu cũ trong `UserServiceImpl.java`**
  - Trong `changePassword()`, kiểm tra:
    ```java
    if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
        throw new AppException(ErrorCode.NEW_PASSWORD_SAME_AS_OLD);
    }
    ```

- [ ] **Step 2: Thêm unit test kiểm tra nghiệp vụ chặn trùng mật khẩu cũ trong `UserServiceTest.java`**
  - Test case: `changePassword_NewPasswordSameAsOld_ThrowsException()`.

- [ ] **Step 3: Cập nhật password mock trong các bài test backend sang chuẩn mới (`Password@123`)**
  - Thay các chuỗi `password123` và `newPassword123` trong `AuthControllerTest.java` và `AuthServiceTest.java` thành `Password@123` / `NewPassword@123`.

- [ ] **Step 4: Bổ sung test cases kiểm tra validation regex trong `AuthControllerTest.java`**
  - Kiểm tra mật khẩu chứa khoảng trắng ở giữa (`Pass@ 123`) -> 400 Bad Request.
  - Kiểm tra mật khẩu thiếu ký tự đặc biệt (`Password123`) -> 400 Bad Request.
  - Kiểm tra mật khẩu dưới 6 ký tự (`P@1a`) -> 400 Bad Request.

- [ ] **Step 5: Chạy test suite backend**
  - Chạy `./mvnw test` để đảm bảo 100% test pass.

---

### Task 3: Frontend - Cập nhật validation mật khẩu tại trang Đăng ký (`Register.tsx`)

**Files:**
- Modify: `frontend/src/pages/Register.tsx`

- [ ] **Step 1: Định nghĩa hằng số Regex và hàm validate mật khẩu bảo mật**
  - Khai báo regex chuẩn:
    ```typescript
    const PASSWORD_REGEX = /^(?=.{6,64}$)(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9\s])\S+$/;
    ```
  - Trong `validateForm()`:
    ```typescript
    if (!formData.password) {
      newErrors.password = 'Mật khẩu không được để trống';
    } else if (/\s/.test(formData.password)) {
      newErrors.password = 'Mật khẩu không được chứa khoảng trắng';
    } else if (formData.password.length < 6 || formData.password.length > 64) {
      newErrors.password = 'Mật khẩu phải từ 6 đến 64 ký tự';
    } else if (!PASSWORD_REGEX.test(formData.password)) {
      newErrors.password = 'Mật khẩu phải bao gồm ít nhất 1 chữ cái, 1 chữ số và 1 ký tự đặc biệt';
    }
    ```
  - Thêm chú thích gợi ý yêu cầu mật khẩu dưới ô input để người dùng dễ theo dõi.
  - Viết comment tiếng Việt chi tiết cho toàn bộ code Frontend theo đúng quy tắc [GEMINI.md](file:///f:/Working/JavaBackend/football-pitch-management-system/GEMINI.md).

---

### Task 4: Frontend - Cập nhật validation mật khẩu tại `Profile.tsx` & `ForgotPassword.tsx`

**Files:**
- Modify: `frontend/src/pages/Profile.tsx`
- Modify: `frontend/src/pages/ForgotPassword.tsx`

- [ ] **Step 1: Cập nhật hàm `handleChangePassword` trong `Profile.tsx`**
  - Kiểm tra rỗng, khoảng trắng `/\s/`, độ dài 6-64 ký tự, regex đầy đủ `PASSWORD_REGEX`.
  - Kiểm tra mật khẩu mới không được trùng mật khẩu hiện tại:
    ```typescript
    if (newPassword && currentPassword && newPassword === currentPassword) {
      errors.newPassword = 'Mật khẩu mới không được trùng với mật khẩu hiện tại';
    }
    ```
  - Xử lý mã lỗi `2017` từ server map vào `passwordErrors.newPassword`.
  - Viết comment tiếng Việt chi tiết theo [GEMINI.md](file:///f:/Working/JavaBackend/football-pitch-management-system/GEMINI.md).

- [ ] **Step 2: Cập nhật hàm `handleResetPassword` trong `ForgotPassword.tsx`**
  - Áp dụng các điều kiện tương tự: rỗng, khoảng trắng, độ dài 6-64 ký tự, `PASSWORD_REGEX`.
  - Viết comment tiếng Việt chi tiết theo [GEMINI.md](file:///f:/Working/JavaBackend/football-pitch-management-system/GEMINI.md).

---

### Task 5: Kiểm thử hồi quy và xác nhận toàn diện (Verification)

- [ ] **Step 1: Chạy build frontend**
  - Chạy `npm run build` trong `frontend/` để đảm bảo TypeScript không có lỗi.

- [ ] **Step 2: Chạy toàn bộ test suite backend**
  - Chạy `./mvnw test` trong `backend/`.

- [ ] **Step 3: Kiểm tra trực tiếp các kịch bản test case**
  - `TC_REG_20`: Mật khẩu có khoảng trắng ở đầu, giữa, cuối -> Bị chặn với thông báo "Mật khẩu không được chứa khoảng trắng".
  - `TC_REG_21`: Mật khẩu toàn khoảng trắng -> Bị chặn.
  - Mật khẩu 6 ký tự hợp lệ có chữ + số + ký tự đặc biệt (ví dụ `Aa1@bc`) -> Cho phép đăng ký / đổi mật khẩu thành công.
  - Đổi mật khẩu mới trùng mật khẩu hiện tại -> Bị chặn với thông báo "Mật khẩu mới không được trùng với mật khẩu hiện tại".
