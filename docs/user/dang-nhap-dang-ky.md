# Đăng nhập, đăng ký và phân quyền

> **Module:** `com.example.demo.user` + `com.example.demo.config.SecurityConfig` · **Bài học nguồn:** [bài 12](../study/12-dang-nhap-spring-security.md), [bài 13](../study/13-du-lieu-mau-seeder.md#bước-2--tạo-user-admin), [bài 15](../study/15-anh-va-mo-ta-san-pham.md#bước-6--securityconfig-cho-phép-xem-ảnh) · **Cập nhật:** 2026-11-10
> **Trạng thái code:** ✅ Hoàn thành (đăng ký, đăng nhập, đăng xuất) — theo code thật ngày 2026-11-10. Phân quyền theo role **chưa áp dụng** (bài tập 3 bài 12 chưa làm).

## Chức năng này làm gì

- **Đăng ký** tài khoản mới (`/register`), mật khẩu mã hoá bằng BCrypt, role luôn là `USER`.
- **Đăng nhập** bằng form (`/login`) cho giao diện web; **HTTP Basic** (`curl -u`) cho REST API.
- **Đăng xuất** bằng `POST /logout`.
- Chặn mọi URL khi chưa đăng nhập, trừ danh sách `permitAll`. Web bị chuyển về `/login`, REST nhận **401**.
- Nav hiện "Xin chào, *tên*" + nút Đăng xuất khi đã đăng nhập.
- Role (`USER` / `ADMIN`) được lưu và nạp vào Spring Security (`ROLE_USER`, `ROLE_ADMIN`), nhưng **chưa có luật nào dùng tới**: mọi user đăng nhập đều làm được mọi việc.

## Đường dẫn & giao diện

| Method | URL | Controller#method | Mô tả |
|---|---|---|---|
| GET | `/login` | `AuthController#loginForm` | Trang `auth/login`. Đọc cờ URL `?error`, `?logout`, `?registered` để hiện thông báo |
| POST | `/login` | *(Spring Security)* | Không có code của mình. Đúng → về trang đang chờ hoặc `/products`; sai → `/login?error` |
| GET | `/register` | `AuthController#registerForm` | Form rỗng `auth/register` |
| POST | `/register` | `AuthController#register` | Tạo user. Lỗi → hiện lại form kèm `error`; thành công → `redirect:/login?registered` |
| POST | `/logout` | *(Spring Security)* | Huỷ session → `/login?logout` |

Không cần đăng nhập: `/login`, `/register`, `/css/**`, `/images/**`, `/uploads/**` (`FileStorageService.URL_PREFIX + "**"`), `/error`, `/actuator/health`. Mọi URL khác: `anyRequest().authenticated()`.

## Luồng xử lý

```
Request bất kỳ ──► Security Filter Chain (chạy TRƯỚC controller)
   ├── URL thuộc permitAll ──► cho qua
   ├── đã đăng nhập (session) hoặc header Basic đúng ──► cho qua ──► Controller
   └── chưa đăng nhập
         ├── trình duyệt (Accept: text/html) ──► redirect /login
         └── curl / API ──► 401 Unauthorized

POST /login (username, password + CSRF token)
   └── UserService.loadUserByUsername(username)
         ├── không có user ──► UsernameNotFoundException ──► /login?error
         └── có ──► UserDetails {username, $2a$10$..., ROLE_xxx}
                   ──► passwordEncoder.matches(gõ vào, hash trong DB)
                         ├── sai ──► /login?error
                         └── đúng ──► lưu session ──► trang đang chờ, hoặc /products

POST /register (username, password)
   └── UserService.register(user)
         ├── username đã có ──► IllegalArgumentException ──┐
         ├── password null hoặc < 6 ký tự ──► IllegalArgumentException ──┤
         │      AuthController bắt lỗi ──► xoá password, trả lại auth/register + error
         └── hợp lệ ──► encode password, ép role = "USER", save ──► redirect /login?registered
```

Chi tiết: [bài 12, Spring Security đứng trước controller](../study/12-dang-nhap-spring-security.md#spring-security-đứng-trước-controller).

## File liên quan

| File | Vai trò |
|---|---|
| `src/main/java/com/example/demo/config/SecurityConfig.java` | `SecurityFilterChain` (permitAll, formLogin, logout, httpBasic, CSRF) + `@Bean PasswordEncoder` (BCrypt) |
| `src/main/java/com/example/demo/user/User.java` | Entity, bảng `users` |
| `src/main/java/com/example/demo/user/UserRepository.java` | `findByUsername`, `existsByUsername` (query method) |
| `src/main/java/com/example/demo/user/UserService.java` | `register()` + `implements UserDetailsService` (`loadUserByUsername`) |
| `src/main/java/com/example/demo/user/AuthController.java` | `GET /login`, `GET/POST /register` |
| `src/main/resources/templates/auth/login.html` | Form gửi `POST /login`, ô phải tên `username` / `password` |
| `src/main/resources/templates/auth/register.html` | Form `th:object="${user}"`, hiện `${error}` |
| `src/main/resources/templates/fragments/layout.html` | Nav dùng `sec:authorize`, `sec:authentication="name"`, form Đăng xuất |
| `src/main/java/com/example/demo/seed/DataSeeder.java` | Tạo `admin` / `admin123`, role `ADMIN` (bài 13) |
| `build.gradle` | `spring-boot-starter-security`, `thymeleaf-extras-springsecurity6` |

## Dữ liệu

Bảng `users` (tên `user` trùng từ khoá MySQL nên dùng `users`):

| Trường | Kiểu | Ràng buộc |
|---|---|---|
| `id` | `Long` | Khoá chính, tự tăng |
| `username` | `String` | `NOT NULL`, `UNIQUE` |
| `password` | `String` | `NOT NULL`. Luôn là chuỗi BCrypt `$2a$10$...`, không bao giờ là mật khẩu gốc |
| `role` | `String` | Cho phép `NULL`, mặc định `"USER"`. Giá trị dùng: `USER`, `ADMIN` |

⚠️ Khác bài học: bài 12 Bước 2 khai báo `role` là `@Column(nullable = false)`, code là `nullable = true`.

Không có quan hệ với bảng khác. Tài khoản có sẵn sau khi chạy seeder: `admin` / `admin123` (role `ADMIN`), xem [Dữ liệu mẫu](../seed/du-lieu-mau.md).

## Cấu hình

Không có file cấu hình riêng. Toàn bộ luật nằm trong code `SecurityConfig.java`:

| Phần | Giá trị |
|---|---|
| `formLogin` | `loginPage("/login")`, `defaultSuccessUrl("/products")` |
| `logout` | `logoutSuccessUrl("/login?logout")` |
| `httpBasic` | `Customizer.withDefaults()` |
| `csrf` | Bật cho web, `ignoringRequestMatchers("/api/**")` |
| `PasswordEncoder` | `new BCryptPasswordEncoder()` |

## Quy tắc & lưu ý khi sửa

- **Không viết `POST /login`, `POST /logout`** trong controller, Spring Security tự xử lý.
- **Mọi form web phải dùng `th:action="@{...}"`** để Thymeleaf tự chèn CSRF token. Viết `action="..."` thường sẽ bị **403** ([bài 12, Bước 8](../study/12-dang-nhap-spring-security.md#bước-8--csrf-vì-sao-form-cũ-vẫn-chạy-và-vì-sao-bắt-buộc-dùng-thaction)).
- **Đăng xuất phải là form POST**, không dùng `<a href="/logout">`.
- **Không tin role từ form:** `register()` luôn đặt lại `role = "USER"` ([giải thích](../study/12-dang-nhap-spring-security.md#vì-sao-register-ép-role--user)).
- **Mật khẩu luôn qua `passwordEncoder.encode(...)`** trước khi lưu, kể cả trong seeder. Insert tay bằng SQL thì đăng nhập luôn báo sai ([BCrypt](../study/12-dang-nhap-spring-security.md#bcrypt-vì-sao-không-lưu-mật-khẩu-gốc)).
- **Có hai class `User`**: trong `UserService` phải ghi tên đầy đủ `org.springframework.security.core.userdetails.User` cho class của Spring.
- **Thêm URL công khai mới** (thư mục tĩnh, trang public) thì thêm vào `requestMatchers(...).permitAll()`, đặt trước `anyRequest()`. Ví dụ bài 15 đã thêm `/images/**` và `/uploads/**`.
- Muốn phân quyền (chỉ ADMIN được xoá): thêm `.requestMatchers(...).hasRole("ADMIN")` **trước** `anyRequest()`, xem bài tập 3 của [bài 12](../study/12-dang-nhap-spring-security.md#bài-tập). Chỉ ẩn link bằng `sec:authorize` là chưa đủ.
- Nav trong `layout.html`: cả hai khối đều là `sec:authorize="isAuthenticated()"`, nên ở trang `/login`, `/register` (chưa đăng nhập) nav **trống**. Hai trang này tự có link qua lại với nhau.
  ⚠️ Khác bài học: bài 12 Bước 9 để link Sản phẩm / Danh mục luôn hiện và có thêm khối `sec:authorize="!isAuthenticated()"` chứa link Đăng nhập / Đăng ký. Code bọc link Sản phẩm / Danh mục trong `<span class="nav-user" sec:authorize="isAuthenticated()">` và **không có** khối `!isAuthenticated()`.
- ⚠️ Khác bài học: bài 12 Bước 9 thêm CSS `.nav-user`, `form.inline`, `.alert`, `.alert-error`, `.alert-ok` vào `static/css/app.css`, nhưng code **chưa có** các class này. Thông báo lỗi/thành công ở trang login/register và nút Đăng xuất vì vậy hiện không có định dạng riêng.

## Liên quan

- [Quản lý danh mục](../category/quan-ly-danh-muc.md), [Quản lý sản phẩm](../product/quan-ly-san-pham.md): mọi trang đều cần đăng nhập, REST dùng HTTP Basic.
- [Trang lỗi 404](../common/trang-loi-404.md): `/error` phải `permitAll` thì trang lỗi mới hiện được.
- [Lưu file upload](../storage/luu-file-upload.md): `/uploads/**` được mở công khai qua `FileStorageService.URL_PREFIX`.
- [Dữ liệu mẫu](../seed/du-lieu-mau.md): tạo `admin` / `admin123`.

## Lịch sử thay đổi

| Bài | Thay đổi |
|---|---|
| [12](../study/12-dang-nhap-spring-security.md) | Thêm thư viện security; tạo `User`, `UserRepository`, `UserService`, `AuthController`, `SecurityConfig`, `auth/login.html`, `auth/register.html`; nav "Xin chào" + Đăng xuất |
| [13](../study/13-du-lieu-mau-seeder.md#bước-2--tạo-user-admin) | `DataSeeder` tạo `admin` / `admin123` role `ADMIN` nếu chưa có |
| [15](../study/15-anh-va-mo-ta-san-pham.md#bước-6--securityconfig-cho-phép-xem-ảnh) | `permitAll` thêm `/images/**` và `FileStorageService.URL_PREFIX + "**"` (`/uploads/**`) |
