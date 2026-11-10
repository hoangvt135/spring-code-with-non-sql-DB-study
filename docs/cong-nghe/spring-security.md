# Spring Security

> **Dùng trong project từ:** [bài 12](../study/12-dang-nhap-spring-security.md) · **Phiên bản:** Spring Security 6.4.2 (theo Spring Boot 3.4.1, qua `spring-boot-starter-security`), `thymeleaf-extras-springsecurity6` 3.1.3 · **Tài liệu chính thức:** [Spring Security 6.4 Reference](https://docs.spring.io/spring-security/reference/6.4/index.html)
>
> 🔼 **Sau [bài 16.5](../study/16.5-nang-cap-spring-boot-4.md)** (người học chưa làm): Spring Security **7.1**. `SecurityConfig` **không phải sửa**; `thymeleaf-extras-springsecurity6` (bản 3.1.5) vẫn là bản dùng cho Security 7.

## Nó là gì, giải quyết vấn đề gì

Spring Security là thư viện lo **bảo mật** cho app Spring: đăng nhập, đăng xuất, nhớ ai đang đăng nhập, chặn trang chưa được phép, mã hoá mật khẩu, chống tấn công CSRF...

Bên PHP thuần, các việc này bạn tự làm bằng tay: `password_hash()` lúc đăng ký, `password_verify()` lúc đăng nhập, lưu `$_SESSION['user_id']`, đầu mỗi trang kiểm tra `if (!isset($_SESSION['user_id'])) header('Location: /login');`, tự sinh token chống CSRF cho form. Laravel gom lại thành `Auth::attempt()`, middleware `auth`, `@csrf`. Spring Security tương đương phần đó của Laravel, nhưng khác ở hai điểm:

1. **Bật lên là khoá hết.** Chỉ cần thêm thư viện, mọi URL lập tức đòi đăng nhập. Mình khai báo ngược lại: URL nào được **mở**.
2. **Mình gần như không viết code đăng nhập.** Không có controller nào xử lý `POST /login` hay `POST /logout`. Mình chỉ trả lời Spring một câu hỏi: "user tên X trông thế nào?" (`UserDetailsService`), còn so mật khẩu, tạo session, chuyển trang Spring tự làm.

## Khái niệm cốt lõi

### Authentication và authorization

| | Authentication (xác thực) | Authorization (phân quyền) |
|---|---|---|
| Câu hỏi | **Bạn là ai?** | **Bạn được làm gì?** |
| Ví dụ | Gõ đúng `admin` / `admin123` → biết bạn là `admin` | `admin` có role `ADMIN` nên được xoá, `an` thì không |
| Trong project | ✅ Đã làm (form login + HTTP Basic) | Chỉ có mức "đã đăng nhập hay chưa". Phân quyền theo role **chưa làm** |

Hai từ này hay viết tắt là **authn** và **authz**. Lỗi trả về cũng khác nhau: chưa xác thực → **401** (hoặc chuyển về `/login`), đã xác thực mà không đủ quyền → **403**.

### Filter chain: Spring Security đứng trước controller

Spring Security không nằm trong controller mà là một **chuỗi filter** (bộ lọc) chạy **trước** khi request tới `DispatcherServlet` và controller. Giống middleware của Laravel.

```
Request
  │
  ▼
┌──────────────── SecurityFilterChain (theo thứ tự) ────────────────┐
│ SecurityContextHolderFilter      nạp "ai đang đăng nhập" từ session │
│ CsrfFilter                       POST/PUT/DELETE phải có token đúng │
│ LogoutFilter                     bắt POST /logout                   │
│ UsernamePasswordAuthenticationFilter  bắt POST /login (form)       │
│ BasicAuthenticationFilter        đọc header Authorization: Basic   │
│ AnonymousAuthenticationFilter    chưa ai đăng nhập → "anonymous"    │
│ ExceptionTranslationFilter       bị từ chối → chuyển /login hoặc 401│
│ AuthorizationFilter              so URL với luật permitAll/authenticated │
└───────────────────────────────────────────────────────────────────┘
  │ được phép
  ▼
DispatcherServlet → Controller → Service → ...
```

(Danh sách thật dài hơn, đây là các filter liên quan tới cấu hình của project.) Hệ quả quan trọng: request bị chặn thì **controller không hề chạy**, nên đặt breakpoint trong controller sẽ không dừng.

### Bean `SecurityFilterChain`: mình tự khai báo luật

Spring Boot có sẵn cấu hình mặc định: khoá mọi URL, trang login tiếng Anh tự sinh, một user tên `user` với mật khẩu ngẫu nhiên in trong log (`Using generated security password: ...`). Đó là thứ bạn thấy ở [bài 12 Bước 1](../study/12-dang-nhap-spring-security.md#bước-1--thêm-thư-viện-và-thử-khoá-cửa).

Khi mình tự khai báo:

- một bean **`SecurityFilterChain`** → Spring Boot bỏ chuỗi filter mặc định, dùng luật của mình;
- một bean **`UserDetailsService`** (ở đây là `UserService`) → Spring Boot bỏ user `user` tự sinh, hỏi `UserService` để tìm user.

Cách viết là **lambda DSL**: `http.formLogin(form -> form.loginPage("/login"))`. Cách đọc chuỗi này đã có trong [bài 12 Bước 4](../study/12-dang-nhap-spring-security.md#bước-4--securityconfig-luật-gác-cổng). Tài liệu trên mạng viết cho Spring Security 5 (`extends WebSecurityConfigurerAdapter`, `antMatchers`, `authorizeRequests`, `.and()`) **không chạy** với bản 6: các hàm đó đã bị xoá hoặc thay tên.

### SecurityContext và session: "đã đăng nhập" được nhớ ở đâu

Sau khi đăng nhập đúng, Spring tạo một object **`Authentication`** (tên user, các quyền, trạng thái "đã xác thực"), đặt vào **`SecurityContext`**, rồi lưu `SecurityContext` vào **HTTP session**. Trình duyệt giữ cookie `JSESSIONID` (giống `PHPSESSID`). Request sau gửi kèm cookie, `SecurityContextHolderFilter` lấy lại `Authentication` từ session.

- Session nằm trong **RAM của app**. Khởi động lại app (DevTools restart, chạy script reset) là mọi người bị đăng xuất.
- Lúc đăng nhập thành công, Spring **đổi mã session** (chống tấn công *session fixation*), nên giá trị `JSESSIONID` đổi.

### Form login cho trang web

```
GET /products (chưa đăng nhập)
  → Spring nhớ "đang định vào /products" → chuyển tới GET /login (trang của mình, AuthController)
POST /login  username=admin&password=admin123&_csrf=...
  → UsernamePasswordAuthenticationFilter
       ├─ userService.loadUserByUsername("admin")       ← code của mình
       ├─ passwordEncoder.matches("admin123", "$2a$10$...")
       ├─ đúng → lưu vào session → về trang đang định vào, không có thì /products
       └─ sai  → /login?error
```

- Ô nhập phải tên đúng `username` và `password` (đổi được bằng `usernameParameter(...)`, project giữ mặc định).
- Trang lưu lại trước khi bị chuyển đi có thêm `?continue` khi quay về (ví dụ `/products/new?continue`). Đây là cách Spring Security 6 đánh dấu "request này cần lấy lại trang đã lưu", không phải lỗi.
- Không tìm thấy user và sai mật khẩu đều về cùng `/login?error`. Spring cố ý không cho biết "tên này không tồn tại", để kẻ xấu không dò được ai có tài khoản.

### HTTP Basic cho REST API

```bash
curl -u admin:admin123 http://localhost:8093/api/products
# curl gửi header:  Authorization: Basic YWRtaW46YWRtaW4xMjM=
#                                         └─ "admin:admin123" mã hoá Base64
```

- Tên và mật khẩu đi kèm **từng request**, server không cần nhớ gì. Hợp với curl/Postman, vốn không giữ cookie hay đọc trang HTML.
- Base64 **không phải mã hoá bảo mật**, ai bắt được gói tin là giải ra ngay. Dự án thật bắt buộc dùng HTTPS khi dùng Basic.

Project bật **cả hai** cùng lúc trong một chuỗi filter: `.formLogin(...)` và `.httpBasic(...)`. Khi request chưa đăng nhập, Spring chọn cách phản hồi theo header `Accept` của request: trình duyệt (`Accept: text/html...`) bị chuyển về `/login`; curl (`Accept: */*`) nhận **401** kèm header `WWW-Authenticate: Basic`. Một user dùng được cả hai cách.

### `UserDetailsService`: "user tên X trông thế nào?"

Spring Security không biết bảng `users` của mình. Nó chỉ biết hỏi qua interface `UserDetailsService`, có đúng một hàm:

```java
UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
```

`UserService` làm interface này: tìm trong DB rồi đổi entity `User` của mình sang `UserDetails` của Spring (tên, mật khẩu đã mã hoá, quyền). Spring tự tìm bean này, ghép với bean `PasswordEncoder`, tạo bộ xác thực (`DaoAuthenticationProvider`). Mình không phải nối chúng với nhau.

### `PasswordEncoder` và BCrypt

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();      // cost mặc định = 10
}
```

| Hàm | Gọi ở đâu | Tương đương PHP |
|---|---|---|
| `passwordEncoder.encode("admin123")` | `UserService.register()`, `DataSeeder` | `password_hash('admin123', PASSWORD_BCRYPT)` |
| `passwordEncoder.matches(raw, hash)` | Spring tự gọi lúc đăng nhập | `password_verify($raw, $hash)` |

BCrypt là hàm băm **một chiều**, **có salt ngẫu nhiên** (cùng mật khẩu, mỗi lần encode ra chuỗi khác), và **cố ý chậm** để dò mật khẩu tốn thời gian. Giải thích chuỗi `$2a$10$...`: [bài 12](../study/12-dang-nhap-spring-security.md#bcrypt-vì-sao-không-lưu-mật-khẩu-gốc). Hàm `password_hash` của PHP cũng dùng BCrypt (chuỗi `$2y$...`), và `BCryptPasswordEncoder` đọc được cả dạng `$2y$`. BCrypt chỉ xét tối đa 72 byte đầu của mật khẩu.

Project dùng thẳng `BCryptPasswordEncoder`. Spring còn có `DelegatingPasswordEncoder` (lưu kèm tiền tố `{bcrypt}$2a$...` để sau này đổi thuật toán mà mật khẩu cũ vẫn dùng được); project chưa cần.

### CSRF: vì sao form POST phải có token

**CSRF** (Cross-Site Request Forgery): bạn đang đăng nhập app, rồi mở một trang lạ. Trang đó tự gửi `POST http://localhost:8093/categories/save` bằng form ẩn. Trình duyệt **tự gửi kèm cookie** `JSESSIONID`, server tưởng chính bạn bấm.

Cách chặn: mỗi session có một **token bí mật**. Form thật của app chứa token trong ô ẩn `_csrf`. `CsrfFilter` kiểm tra mọi request **làm thay đổi dữ liệu** (POST, PUT, PATCH, DELETE): thiếu hoặc sai token → **403**. Trang lạ không đọc được nội dung trang của bạn nên không biết token.

- **Ai chèn token?** Thymeleaf, khi form có `th:action` và `method="post"`. Viết `action="..."` thường thì không có token → 403 ([bài 12 Bước 8](../study/12-dang-nhap-spring-security.md#bước-8--csrf-vì-sao-form-cũ-vẫn-chạy-và-vì-sao-bắt-buộc-dùng-thaction)). Giống `@csrf` trong Blade, nhưng tự động.
- **Giá trị token đổi mỗi lần tải trang** (Spring Security 6 "che" token bằng một giá trị ngẫu nhiên để chống tấn công BREACH). Token gốc trong session vẫn là một.
- **GET không được kiểm tra.** CSRF chỉ bảo vệ các method thay đổi dữ liệu, vì quy ước HTTP là GET chỉ để đọc. Project đang vi phạm quy ước này: link Xoá là `GET /products/{id}/delete` và `GET /categories/{id}/delete`. Trang lạ chỉ cần dụ bạn bấm một link (hoặc chèn `<img src=".../products/5/delete">`) là xoá được. Trình duyệt mới có cơ chế cookie `SameSite` giảm bớt rủi ro, nhưng không nên trông vào. Cách sửa: [bài 12, bài tập 4](../study/12-dang-nhap-spring-security.md#bài-tập) (đổi sang form POST).
- **`/api/**` tắt CSRF**: `csrf.ignoringRequestMatchers("/api/**")`. Curl/Postman không có trang HTML để lấy token, và dùng Basic chứ không dựa vào cookie. Lưu ý: trình duyệt đã đăng nhập bằng form vẫn gửi cookie tới `/api/**`, vì project dùng chung một chuỗi filter. Rủi ro thực tế thấp (API nhận JSON và dùng PUT/DELETE, trang lạ gửi kiểu này sẽ bị trình duyệt kiểm tra CORS), nhưng dự án thật thường tách một chuỗi filter riêng cho `/api/**` không dùng session.

### `permitAll`, `authenticated` và thứ tự luật

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/login", "/register", "/css/**", "/images/**",
                     FileStorageService.URL_PREFIX + "**",     // "/uploads/**" (bài 15)
                     "/error", "/actuator/health").permitAll()
    .anyRequest().authenticated()
)
```

- Luật được xét **từ trên xuống**, gặp luật khớp đầu tiên là dừng. `anyRequest()` phải đứng **cuối**.
- `**` khớp mọi thứ phía sau (`/css/**` khớp `/css/app.css`), `*` khớp đúng một đoạn (`/products/*/delete`).
- **`/error` phải mở**: khi có lỗi (404, 500), Spring Boot chuyển nội bộ sang `/error` để vẽ trang lỗi. Request nội bộ này cũng đi qua Spring Security. Không mở thì người dùng thấy trang login thay vì trang 404.
- **`/uploads/**` mở** ([bài 15 Bước 6](../study/15-anh-va-mo-ta-san-pham.md#bước-6--securityconfig-cho-phép-xem-ảnh)): ảnh sản phẩm là nội dung công khai. Không mở thì thẻ `<img>` nhận về trang login, ảnh hiện thành ảnh vỡ.
- `.formLogin(...).permitAll()` và `.logout(...).permitAll()` tự mở các URL của đăng nhập/đăng xuất (`/login`, `/login?error`, `/logout`, `/login?logout`). `/login` vẫn ghi trong `requestMatchers` là thừa nhưng vô hại.

### Logout

```java
.logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll())
```

Spring tự xử lý `POST /logout`: huỷ session, xoá `SecurityContext`, xoá CSRF token, rồi chuyển tới `/login?logout`. Vì CSRF đang bật, **chỉ nhận POST**, nên nút Đăng xuất trong `layout.html` là một `<form th:action="@{/logout}" method="post">`. Lý do không cho đăng xuất bằng GET: [bài 12 Bước 9](../study/12-dang-nhap-spring-security.md#bước-9--nav-xin-chào-an--nút-đăng-xuất).

### `Principal`: ai đang gửi request

```java
@GetMapping("/{id}")
public String detail(@PathVariable Long id, Principal principal, Model model) {
    long views = productStatsService.recordView(id, principal.getName());
    ...
}
```

`java.security.Principal` là interface có sẵn của Java. Khai báo nó làm tham số của method controller là Spring MVC tự truyền vào. Object thật bên trong chính là `Authentication` của Spring Security, `getName()` trả về username (cái tên đã đưa vào `withUsername(...)`). Chưa đăng nhập thì `principal` là `null`; ở `/products/**` không xảy ra vì mọi trang đó đều bắt đăng nhập ([bài 16 Bước 5](../study/16-luot-xem-top-san-pham-redis.md#bước-5--productviewcontroller-trang-chi-tiết)).

Cần nhiều hơn tên thì có thể nhận `Authentication authentication` (thêm `getAuthorities()`), hoặc `@AuthenticationPrincipal UserDetails user`. Ngoài controller (trong service) thì dùng `SecurityContextHolder.getContext().getAuthentication()`. Project chỉ cần tên nên `Principal` là đủ.

### Thymeleaf: `sec:authorize`, `sec:authentication`

Thư viện `thymeleaf-extras-springsecurity6` cho template biết ai đang đăng nhập. Cần khai báo `xmlns:sec="http://www.thymeleaf.org/extras/spring-security"` ở thẻ `<html>`.

```html
<span sec:authorize="isAuthenticated()">                 <!-- như th:if: chỉ hiện khi đã đăng nhập -->
    Xin chào, <b sec:authentication="name">user</b>        <!-- như th:text: in username -->
</span>
```

Biểu thức trong `sec:authorize` dùng được `isAuthenticated()`, `isAnonymous()`, `hasRole('ADMIN')`, `hasAnyRole('ADMIN','USER')`... Lưu ý: **ẩn nút trên giao diện không phải là bảo mật**. Người dùng vẫn gõ thẳng URL được, nên luật thật phải nằm trong `SecurityConfig`. Thêm về Thymeleaf: [thymeleaf.md](thymeleaf.md).

### Role và `hasRole` (project chưa dùng)

`UserService` nạp role từ cột `users.role`:

```java
org.springframework.security.core.userdetails.User
    .withUsername(user.getUsername())
    .password(user.getPassword())
    .roles(user.getRole())          // "ADMIN" → quyền "ROLE_ADMIN"
    .build();
```

`.roles("ADMIN")` tự thêm tiền tố `ROLE_`, và `hasRole("ADMIN")` cũng tự thêm `ROLE_` khi so. Vì vậy role trong DB chỉ ghi `ADMIN` (ghi `ROLE_ADMIN` thì `.roles(...)` ném lỗi). Muốn so đúng nguyên chuỗi thì dùng `hasAuthority("ROLE_ADMIN")`.

Hiện mọi user đã đăng nhập đều làm được mọi việc. Muốn chỉ `ADMIN` được xoá thì thêm luật **trước** `anyRequest()` ([bài 12, bài tập 3](../study/12-dang-nhap-spring-security.md#bài-tập)):

```java
.requestMatchers("/products/*/delete", "/categories/*/delete").hasRole("ADMIN")
```

User không đủ quyền sẽ nhận **403**. Cách khác là phân quyền ngay trên method (`@EnableMethodSecurity` + `@PreAuthorize("hasRole('ADMIN')")`), để đọc thêm.

## Project này dùng nó thế nào

| File | Vai trò | Bài học |
|---|---|---|
| `build.gradle` | `spring-boot-starter-security`, `thymeleaf-extras-springsecurity6` | [12 Bước 1](../study/12-dang-nhap-spring-security.md#bước-1--thêm-thư-viện-và-thử-khoá-cửa) |
| `config/SecurityConfig.java` | Bean `SecurityFilterChain` (luật URL, form login, logout, HTTP Basic, CSRF) và bean `PasswordEncoder` | [12 Bước 4](../study/12-dang-nhap-spring-security.md#bước-4--securityconfig-luật-gác-cổng), [15 Bước 6](../study/15-anh-va-mo-ta-san-pham.md#bước-6--securityconfig-cho-phép-xem-ảnh) |
| `user/User.java`, `UserRepository.java` | Bảng `users`, `findByUsername`, `existsByUsername` | [12 Bước 2–3](../study/12-dang-nhap-spring-security.md#bước-2--entity-user) |
| `user/UserService.java` | `register()` (kiểm tra trùng tên, mật khẩu ≥ 6 ký tự, `encode`, ép role `USER`) và `loadUserByUsername()` | [12 Bước 5](../study/12-dang-nhap-spring-security.md#bước-5--userservice-đăng-ký--trả-lời-user-x-là-ai) |
| `user/AuthController.java` | Chỉ `GET /login`, `GET/POST /register`. **Không** có `POST /login`, `POST /logout` | [12 Bước 6](../study/12-dang-nhap-spring-security.md#bước-6--authcontroller) |
| `templates/auth/login.html`, `register.html` | Form `th:action` (có CSRF token), đọc `${param.error}`, `${param.logout}`, `${param.registered}` | [12 Bước 7](../study/12-dang-nhap-spring-security.md#bước-7--template-đăng-nhập--đăng-ký) |
| `templates/fragments/layout.html` | `sec:authorize`, `sec:authentication="name"`, form đăng xuất POST | [12 Bước 9](../study/12-dang-nhap-spring-security.md#bước-9--nav-xin-chào-an--nút-đăng-xuất) |
| `product/ProductViewController.java` | `Principal principal` ở trang chi tiết → `principal.getName()` | [16 Bước 5](../study/16-luot-xem-top-san-pham-redis.md#bước-5--productviewcontroller-trang-chi-tiết) |
| `seed/DataSeeder.java` | Tạo `admin` / `admin123` role `ADMIN`, mật khẩu qua `passwordEncoder.encode(...)` | [13 Bước 2](../study/13-du-lieu-mau-seeder.md#bước-2--tạo-user-admin) |

Toàn bộ luật nằm trong một chuỗi gọi trên `http`:

```java
http
    .authorizeHttpRequests(...)                                  // 1) URL nào mở, còn lại phải đăng nhập
    .formLogin(form -> form.loginPage("/login")
                           .defaultSuccessUrl("/products")
                           .permitAll())                         // 2) đăng nhập bằng form của mình
    .logout(logout -> logout.logoutSuccessUrl("/login?logout")
                            .permitAll())                        // 3) POST /logout
    .httpBasic(Customizer.withDefaults())                        // 4) curl -u cho REST
    .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));      // 5) bỏ CSRF cho /api/**
return http.build();
```

Spring Security còn tự thêm vài header bảo mật vào mọi response (`X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Cache-Control: no-cache...`) mà không cần cấu hình.

Mô tả chức năng đầy đủ: [Đăng nhập, đăng ký](../user/dang-nhap-dang-ky.md). Chỗ code khác bài học (chi tiết trong doc module):

- Bài 12 Bước 2 ghi `role` là `@Column(nullable = false)`, code là `nullable = true`.
- Bài 12 Bước 9 cho link Sản phẩm / Danh mục luôn hiện và có khối `sec:authorize="!isAuthenticated()"` chứa link Đăng nhập / Đăng ký. Code bọc cả link Sản phẩm / Danh mục trong `isAuthenticated()` và không có khối `!isAuthenticated()`.
- CSS `.nav-user`, `form.inline`, `.alert...` của bài 12 Bước 9 chưa có trong `app.css`.
- `/actuator/health` mở cho mọi người, mà `show-details: always` liệt kê trạng thái `db`, `mongo`, `redis`. Với project học tập thì không sao; dự án thật nên chỉ hiện chi tiết cho người đã đăng nhập.

## Tra cứu nhanh

| Annotation / Hàm | Của ai | Làm gì | Trả về / Ghi chú |
|---|---|---|---|
| `@Configuration` + `@Bean SecurityFilterChain` | Spring / Spring Security | Khai báo luật bảo mật thay cho mặc định | `http.build()` |
| `http.authorizeHttpRequests(auth -> ...)` | `HttpSecurity` | Luật URL | `HttpSecurity` |
| `auth.requestMatchers("/a", "/b/**")` | Spring Security | Chọn URL | Luật viết dở, phải nối `permitAll()`/`authenticated()`/`hasRole()` |
| `auth.anyRequest()` | Spring Security | Mọi URL còn lại | Phải đứng cuối |
| `.permitAll()` / `.authenticated()` | Spring Security | Ai cũng vào / phải đăng nhập | |
| `.hasRole("ADMIN")` / `.hasAuthority("ROLE_ADMIN")` | Spring Security | Phải có quyền | Chưa dùng |
| `http.formLogin(form -> ...)` | `HttpSecurity` | Bật đăng nhập bằng form | Spring tự xử lý `POST /login` |
| `form.loginPage("/login")` | `FormLoginConfigurer` | Dùng trang login của mình | Sai → `/login?error` |
| `form.defaultSuccessUrl("/products")` | `FormLoginConfigurer` | Trang sau khi đăng nhập (nếu không có trang đang chờ) | Thêm `, true` để luôn về đó |
| `http.logout(logout -> ...)` | `HttpSecurity` | Cấu hình `POST /logout` | |
| `http.httpBasic(Customizer.withDefaults())` | `HttpSecurity` | Bật HTTP Basic | `curl -u user:pass` |
| `csrf.ignoringRequestMatchers("/api/**")` | `CsrfConfigurer` | Không đòi token cho các URL này | |
| `new BCryptPasswordEncoder()` | Spring Security Crypto | Bộ mã hoá BCrypt, cost 10 | Khai báo kiểu `PasswordEncoder` |
| `passwordEncoder.encode(raw)` | `PasswordEncoder` | Mã hoá mật khẩu | `String` dạng `$2a$10$...` |
| `passwordEncoder.matches(raw, hash)` | `PasswordEncoder` | So mật khẩu | `boolean`. Spring tự gọi lúc đăng nhập |
| `UserDetailsService.loadUserByUsername(name)` | Spring Security (interface, mình làm) | Tìm user để Spring xác thực | `UserDetails`. Không có → ném `UsernameNotFoundException` |
| `User.withUsername(...).password(...).roles(...).build()` | `org.springframework.security.core.userdetails.User` | Tạo `UserDetails` | `.roles("X")` → quyền `ROLE_X` |
| `Principal principal` (tham số controller) | Java, Spring MVC truyền vào | Người đang đăng nhập | `getName()` → username; `null` nếu chưa đăng nhập |
| `SecurityContextHolder.getContext().getAuthentication()` | Spring Security | Lấy người đang đăng nhập ở bất kỳ đâu | `Authentication` |
| `sec:authorize="isAuthenticated()"` | `thymeleaf-extras-springsecurity6` | Hiện thẻ nếu biểu thức đúng | Không thay cho luật trong `SecurityConfig` |
| `sec:authentication="name"` | `thymeleaf-extras-springsecurity6` | In username | |
| `<form th:action="@{...}" method="post">` | Thymeleaf | Tự chèn ô ẩn `_csrf` | `action=` thường thì không có |

## Bẫy thường gặp

| Triệu chứng | Nguyên nhân | Cách sửa |
|---|---|---|
| Bấm Lưu → **403 Forbidden** | Form viết `action="..."` thay vì `th:action`, thiếu CSRF token | Dùng `th:action="@{...}"` |
| `curl -X POST /products/save ...` → 403 | Đường dẫn web (không phải `/api/**`) vẫn đòi CSRF token | Gọi API qua `/api/...` |
| Trang login không có CSS / ảnh sản phẩm vỡ | Chưa `permitAll()` cho `/css/**`, `/uploads/**` | Thêm vào `requestMatchers` |
| Đã đăng nhập, mở URL sai mà bị đẩy về `/login` thay vì trang 404 | Chưa mở `/error` | Thêm `"/error"` vào `permitAll()` |
| App không khởi động: `Can't configure requestMatchers after anyRequest` | Luật đặt sau `anyRequest()` | Đưa `anyRequest()` xuống cuối |
| Log vẫn có `Using generated security password`, trang login tiếng Anh | `UserService` thiếu `@Service` hoặc chưa `implements UserDetailsService` | Kiểm tra [bài 12 Bước 5](../study/12-dang-nhap-spring-security.md#bước-5--userservice-đăng-ký--trả-lời-user-x-là-ai) |
| Đăng nhập đúng mật khẩu vẫn `?error`, log `Encoded password does not look like BCrypt` | Mật khẩu trong DB là chữ thường (insert tay) | Đăng ký lại qua `/register`, hoặc lưu qua `passwordEncoder.encode(...)` |
| Một user cụ thể không đăng nhập được, các user khác bình thường | Cột `role` của user đó là `NULL` (insert tay; cột cho phép `NULL`) → `.roles(null)` lỗi | `UPDATE users SET role='USER' WHERE role IS NULL;` |
| Ghi role `ROLE_ADMIN` trong DB → lỗi `ROLE_ADMIN cannot start with ROLE_` | `.roles(...)` tự thêm tiền tố | Lưu `ADMIN` |
| Bấm Đăng xuất bằng link `<a href="/logout">` → 404 | Logout chỉ nhận POST | Form POST như `layout.html` |
| Sau khi đăng nhập, URL có `?continue` | Spring Security 6 đánh dấu request lấy lại trang đã lưu | Bình thường |
| Sau khi sửa code (DevTools restart) bị đăng xuất | Session nằm trong RAM của app | Bình thường khi dev |
| `curl` không có `-u` → 401; trình duyệt thì bị chuyển `/login` | Spring chọn phản hồi theo header `Accept` | Bình thường |
| Ẩn link Xoá bằng `sec:authorize` nhưng user thường vẫn xoá được | Chỉ ẩn trên giao diện, chưa có luật `hasRole` | Thêm `.requestMatchers(...).hasRole("ADMIN")` trước `anyRequest()` |
| Code mẫu trên mạng báo không tìm thấy `WebSecurityConfigurerAdapter`, `antMatchers` | Tài liệu viết cho Spring Security 5 | Dùng `SecurityFilterChain` bean và `requestMatchers` |
| `reference to User is ambiguous` | Import cả `User` của mình lẫn `User` của Spring | Ghi tên đầy đủ cho class của Spring, như `UserService` |

## Đọc thêm

- [Architecture (filter chain, `DelegatingFilterProxy`, `FilterChainProxy`)](https://docs.spring.io/spring-security/reference/6.4/servlet/architecture.html)
- [Username/Password: Form Login](https://docs.spring.io/spring-security/reference/6.4/servlet/authentication/passwords/form.html) và [Basic Authentication](https://docs.spring.io/spring-security/reference/6.4/servlet/authentication/passwords/basic.html)
- [`UserDetailsService` và `DaoAuthenticationProvider`](https://docs.spring.io/spring-security/reference/6.4/servlet/authentication/passwords/dao-authentication-provider.html)
- [Password Storage (BCrypt, `DelegatingPasswordEncoder`)](https://docs.spring.io/spring-security/reference/6.4/features/authentication/password-storage.html)
- [CSRF](https://docs.spring.io/spring-security/reference/6.4/servlet/exploits/csrf.html): vì sao GET phải "an toàn", token được lưu và kiểm tra thế nào.
- [Authorize HttpServletRequests](https://docs.spring.io/spring-security/reference/6.4/servlet/authorization/authorize-http-requests.html): `requestMatchers`, `hasRole`, thứ tự luật.
- [Method Security](https://docs.spring.io/spring-security/reference/6.4/servlet/authorization/method-security.html): `@PreAuthorize`.
- [Thymeleaf + Spring Security integration](https://github.com/thymeleaf/thymeleaf-extras-springsecurity): mọi thuộc tính `sec:`.
- Trong repo: [Đăng nhập, đăng ký](../user/dang-nhap-dang-ky.md), [spring-web-mvc.md](spring-web-mvc.md) (controller nhận tham số), [thymeleaf.md](thymeleaf.md), [mysql.md](mysql.md) (bảng `users`).
