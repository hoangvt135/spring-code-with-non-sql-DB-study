# 12 — Đăng ký, đăng nhập, đăng xuất với Spring Security

Hiện tại **ai mở được trình duyệt là sửa/xoá được mọi sản phẩm, danh mục**. App chưa biết "người đang dùng là ai".

Mục tiêu bài này:

- Có trang **Đăng ký** (tạo tài khoản, mật khẩu được mã hoá trước khi lưu DB).
- Có trang **Đăng nhập** / nút **Đăng xuất**.
- Chưa đăng nhập thì **không vào được** `/products`, `/categories` → tự bị đẩy về `/login`.
- Thanh nav hiện "Xin chào, **an**" khi đã đăng nhập.
- REST API `/api/**` cũng phải có tài khoản mới gọi được.

> Kiến thức mới: **Spring Security** (filter chặn trước controller), `UserDetailsService`, mã hoá mật khẩu bằng **BCrypt**, **CSRF token**, và `sec:authorize` trong Thymeleaf.

---

## Ý tưởng

### Hai từ hay bị nhầm

| | Authentication (xác thực) | Authorization (phân quyền) |
|---|---|---|
| Câu hỏi | **Bạn là ai?** | **Bạn được làm gì?** |
| Ví dụ | Nhập đúng user/mật khẩu → biết bạn là `an` | `an` là USER thì không được xoá, ADMIN thì được |
| Bài này | ✅ Làm chính | Chỉ chạm nhẹ (phần bài tập) |

### Spring Security đứng TRƯỚC controller

Tới giờ, một request đi thẳng vào controller (xem bài 01). Khi thêm Spring Security, mọi request phải đi qua **một chuỗi filter** (security filter chain) trước:

```
Trình duyệt
   │  GET /products
   ▼
┌────────────── Security Filter Chain ──────────────┐
│  Đã đăng nhập chưa?                               │
│    ├── Chưa → redirect /login   (controller KHÔNG chạy)
│    └── Rồi  → cho đi tiếp ─────────────────────┐  │
└────────────────────────────────────────────────┼──┘
                                                 ▼
                         ProductViewController → ProductService → ...
```

Điểm quan trọng: **ta không viết code xử lý `POST /login`**. Spring Security tự làm:

```
POST /login  (username=an, password=123456)
   │
   ▼
Filter đăng nhập của Spring Security
   ├── gọi userService.loadUserByUsername("an")   ⬅️ phần DUY NHẤT ta phải viết
   │       → tìm trong bảng users, trả về {an, $2a$10$..., ROLE_USER}
   ├── so mật khẩu: passwordEncoder.matches("123456", "$2a$10$...")
   ├── Đúng → lưu "an đã đăng nhập" vào session → redirect /products
   └── Sai  → redirect /login?error
```

Ta chỉ cần trả lời Spring câu hỏi "**user tên X trông như thế nào?**". Việc so mật khẩu, tạo session, cookie… Spring lo hết.

### Các file sẽ đụng tới

```
build.gradle                         ✏️ SỬA  — thêm 2 thư viện security

com/example/demo/
├── config/
│   └── SecurityConfig.java          ⬅️ MỚI  — luật: trang nào cần login, trang login ở đâu...
└── user/                            ⬅️ MỚI  — feature mới, đủ khuôn Entity → Repository → Service → Controller
    ├── User.java                    — entity, bảng "users"
    ├── UserRepository.java          — findByUsername, existsByUsername
    ├── UserService.java             — đăng ký + loadUserByUsername
    └── AuthController.java          — GET /login, GET/POST /register

templates/
├── auth/
│   ├── login.html                   ⬅️ MỚI
│   └── register.html                ⬅️ MỚI
└── fragments/layout.html            ✏️ SỬA  — nav: "Xin chào, an" + nút Đăng xuất

static/css/app.css                   ✏️ SỬA  — vài dòng CSS cho thông báo
```

---

## Bước 1 — Thêm thư viện và thử "khoá cửa"

Mở `build.gradle`, thêm vào khối `dependencies`:

```gradle
    // Spring Security: đăng nhập, đăng xuất, phân quyền
    implementation 'org.springframework.boot:spring-boot-starter-security'
    // Cho phép dùng sec:authorize, sec:authentication trong Thymeleaf
    implementation 'org.thymeleaf.extras:thymeleaf-extras-springsecurity6'
```

Không cần ghi số phiên bản: Spring Boot tự chọn bản khớp (giống các `spring-boot-starter-*` khác).

> ⚠️ **Thêm thư viện thì DevTools không tự nhận.** Hot reload (bài 04) chỉ nạp lại *code của bạn*, còn danh sách thư viện được Gradle đọc **lúc khởi động**. Phải khởi động lại container:
>
> ```bash
> docker compose -f docker-compose.dev.yml restart app-dev
> docker compose -f docker-compose.dev.yml logs -f app-dev
> ```

### 🧪 Thí nghiệm: chưa viết gì cả, chỉ thêm thư viện

Khởi động lại xong, mở http://localhost:8093/products. Bạn sẽ bị đẩy sang một **trang đăng nhập có sẵn** (tiếng Anh, giao diện của Spring). Trong log có dòng:

```
Using generated security password: 8f3c1a2e-....
```

Đăng nhập bằng user `user` + mật khẩu đó là vào được. Đây là hành vi mặc định của Spring Boot: **có Spring Security là khoá hết mọi trang**, tạo sẵn 1 user tạm trong RAM.

Phần còn lại của bài là thay "user tạm trong RAM" bằng **user thật trong bảng MySQL**, và thay trang login tiếng Anh bằng trang của mình.

---

## Bước 2 — Entity `User`

Tạo package `user/` cạnh `product/`, `category/`. Tạo `user/User.java`:

```java
package com.example.demo.user;

import jakarta.persistence.*;

@Entity
@Table(name = "users")   // "user" trùng từ khoá của MySQL → đặt tên bảng là "users" cho an toàn
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // unique = true: DB cũng chặn 2 người trùng tên đăng nhập
    @Column(nullable = false, unique = true)
    private String username;

    // Lưu mật khẩu ĐÃ MÃ HOÁ (chuỗi dạng $2a$10$...), không bao giờ lưu mật khẩu gốc
    @Column(nullable = false)
    private String password;

    // Vai trò: "USER" hoặc "ADMIN". Mặc định là USER
    @Column(nullable = false)
    private String role = "USER";

    public User() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
```

Nhờ `ddl-auto: update`, khởi động lại là Hibernate tự tạo bảng `users`.

---

## Bước 3 — `UserRepository`

Tạo `user/UserRepository.java`:

```java
package com.example.demo.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Spring Data tự hiểu tên method → SELECT * FROM users WHERE username = ?
    Optional<User> findByUsername(String username);

    // → SELECT COUNT(*) > 0 FROM users WHERE username = ?
    boolean existsByUsername(String username);
}
```

Đây là lần đầu repo này dùng **query method**: chỉ cần **đặt tên method đúng quy tắc** (`findBy` + tên thuộc tính), Spring Data tự sinh câu SQL. Tên thuộc tính phải đúng chữ hoa/thường: `findByUsername` ứng với `private String username`.

---

## Bước 4 — `SecurityConfig`: luật gác cổng

Tạo package `config/`, rồi tạo `config/SecurityConfig.java`:

```java
package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

// @Configuration: class chứa các @Bean, Spring đọc khi khởi động
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1) Trang nào ai cũng vào được, trang nào phải đăng nhập
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register", "/css/**", "/error", "/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            // 2) Đăng nhập bằng form, dùng trang login của mình
            .formLogin(form -> form
                .loginPage("/login")              // GET /login → trang của mình (AuthController)
                .defaultSuccessUrl("/products")   // login xong mà không có trang nào đang chờ → về /products
                .permitAll()
            )
            // 3) Đăng xuất: POST /logout (Spring tự xử lý)
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            // 4) Cho REST API: gửi user/mật khẩu kèm mỗi request (curl -u)
            .httpBasic(Customizer.withDefaults())
            // 5) Tắt CSRF cho /api/** (giải thích ở Bước 8)
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));

        return http.build();
    }

    // Bộ mã hoá mật khẩu. Dùng chung cho: lúc đăng ký (encode) và lúc đăng nhập (matches)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

Đọc từng phần:

| Dòng | Ý nghĩa |
|------|---------|
| `requestMatchers(...).permitAll()` | Danh sách **không cần đăng nhập**. Thứ tự quan trọng: Spring xét từ trên xuống, khớp luật nào trước thì dùng luật đó |
| `/login`, `/register` | Hiển nhiên: chưa login thì phải vào được trang login |
| `/css/**` | Không mở thì trang login **mất hết CSS** (file CSS cũng bị chặn!) |
| `/error` | Không mở thì trang 404 ở bài 10 cũng bị chặn, người chưa login thấy lỗi là bị đẩy về `/login` |
| `/actuator/health` | Để công cụ giám sát kiểm tra app sống mà không cần tài khoản |
| `anyRequest().authenticated()` | **Mọi thứ còn lại** phải đăng nhập. Luật này luôn đặt **cuối cùng** |

### `@Bean` là gì?

`@Bean` trên một method = "Spring ơi, gọi method này lúc khởi động, giữ lại object nó trả về, ai cần thì đưa cho họ". Giống `@Service` nhưng dùng cho object **ta không tự viết class** (như `BCryptPasswordEncoder` của thư viện). Nhờ vậy ở Bước 5, `UserService` nhận được `PasswordEncoder` qua constructor.

### BCrypt: vì sao không lưu mật khẩu gốc?

Nếu DB bị lộ (backup rơi ra ngoài, nhân viên tò mò...), người ta đọc được mật khẩu của mọi người, và vì nhiều người dùng chung một mật khẩu cho nhiều web, họ đăng nhập được cả Facebook, email... của nạn nhân.

BCrypt biến `123456` thành chuỗi như:

```
$2a$10$2F6w7aYEkpQuFijlb3zsy.yAdNIOoqjOLEuLQ0q0/SRM/eRzkTV12
 │  │  └─ salt (ngẫu nhiên) + kết quả băm
 │  └──── "cost" = 10: độ chậm (cố tình chậm để dò mật khẩu tốn thời gian)
 └─────── phiên bản thuật toán
```

- **Một chiều**: không có hàm "giải mã" ngược lại.
- **Mỗi lần encode ra chuỗi khác nhau** (vì salt ngẫu nhiên), kể cả cùng `123456`. Nên không thể so bằng `equals`, phải dùng `passwordEncoder.matches(matKhauGoc, chuoiTrongDB)`. Việc này Spring tự làm lúc đăng nhập.

---

## Bước 5 — `UserService`: đăng ký + trả lời "user X là ai?"

Tạo `user/UserService.java`:

```java
package com.example.demo.user;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// implements UserDetailsService: "tôi là nơi Spring Security hỏi thông tin user"
@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;   // chính là @Bean ở SecurityConfig

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ===== Đăng ký =====
    public User register(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập '" + user.getUsername() + "' đã có người dùng");
        }
        if (user.getPassword() == null || user.getPassword().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự");
        }
        // Mã hoá TRƯỚC khi lưu
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // Ép về USER: không tin giá trị role từ form gửi lên (xem giải thích bên dưới)
        user.setRole("USER");
        return userRepository.save(user);
    }

    // ===== Spring Security gọi hàm này khi có người bấm Đăng nhập =====
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không có user " + username));

        // Đổi User (của mình) → UserDetails (của Spring Security)
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())   // chuỗi $2a$10$... trong DB
                .roles(user.getRole())          // "USER" → Spring lưu thành quyền "ROLE_USER"
                .build();
    }
}
```

### Vì sao phải viết `org.springframework.security.core.userdetails.User` dài thế?

Vì có **hai class cùng tên `User`**: class của mình (`com.example.demo.user.User`) và class của Spring Security. Trong file này, chữ `User` ngắn gọn đã là của mình, nên muốn dùng class của Spring phải ghi **tên đầy đủ kèm package**. Nếu import cả hai, Java báo lỗi không biết `User` là cái nào.

### Vì sao `register()` ép `role = "USER"`?

`@ModelAttribute User user` ở controller tự điền **mọi** thuộc tính có trong request, không chỉ những ô có trên form. Kẻ xấu chỉ cần sửa HTML (F12), thêm `<input name="role" value="ADMIN">` là tự phong mình làm admin. Service luôn đặt lại `USER` để chặn trò này.

> Bài học chung: **dữ liệu từ form là dữ liệu người dùng tự gõ**, kể cả ô ẩn, kể cả ô không có trên form. Thứ gì quan trọng thì server tự quyết.

### Vì sao ném `IllegalArgumentException` mà không phải `RuntimeException`?

`IllegalArgumentException` **là một loại** `RuntimeException` (class con), nên vẫn giữ đúng kiểu "Service ném lỗi" như các bài trước. Dùng loại cụ thể để controller **chỉ bắt đúng lỗi "dữ liệu không hợp lệ"**, còn lỗi khác (mất kết nối DB...) vẫn bay ra thành trang lỗi bình thường.

---

## Bước 6 — `AuthController`

Tạo `user/AuthController.java`:

```java
package com.example.demo.user;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // GET /login → chỉ hiện trang. POST /login do Spring Security xử lý, KHÔNG viết ở đây
    @GetMapping("/login")
    public String loginForm() {
        return "auth/login";
    }

    // GET /register → form rỗng
    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("user", new User());
        return "auth/register";
    }

    // POST /register → tạo tài khoản
    @PostMapping("/register")
    public String register(@ModelAttribute User user, Model model) {
        try {
            userService.register(user);
        } catch (IllegalArgumentException e) {
            // Lỗi → hiện lại form kèm thông báo, giữ nguyên tên đã gõ
            model.addAttribute("error", e.getMessage());
            user.setPassword("");   // không gửi mật khẩu ngược về trình duyệt
            return "auth/register";
        }
        return "redirect:/login?registered";
    }
}
```

So với `ProductViewController`:
- Không có `@RequestMapping` ở đầu class, vì `/login` và `/register` không chung tiền tố.
- Lỗi thì **không redirect** mà `return "auth/register"` luôn, để giữ được `user` (tên đã gõ) và `error` trong `Model`. Redirect là trình duyệt gửi request mới, model cũ mất sạch.
- `?registered` chỉ là một "cờ" trên URL để trang login biết vừa đăng ký xong (xem Bước 7).

---

## Bước 7 — Template đăng nhập & đăng ký

### `templates/auth/login.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{fragments/layout}">
<head>
    <title>Đăng nhập</title>
</head>
<body>
    <section layout:fragment="content">
        <h1>Đăng nhập</h1>

        <!-- ${param.xxx}: đọc tham số trên URL. /login?error → param.error có giá trị -->
        <p th:if="${param.error}" class="alert alert-error">Sai tên đăng nhập hoặc mật khẩu.</p>
        <p th:if="${param.logout}" class="alert alert-ok">Bạn đã đăng xuất.</p>
        <p th:if="${param.registered}" class="alert alert-ok">Đăng ký thành công, mời đăng nhập.</p>

        <!-- Gửi tới POST /login: Spring Security nhận, không phải controller của mình -->
        <form th:action="@{/login}" method="post">
            <!-- name PHẢI là "username" và "password": Spring Security đọc đúng 2 tên này -->
            <label>Tên đăng nhập</label>
            <input type="text" name="username" required autofocus>

            <label>Mật khẩu</label>
            <input type="password" name="password" required>

            <button type="submit">Đăng nhập</button>
        </form>

        <p>Chưa có tài khoản? <a th:href="@{/register}">Đăng ký</a></p>
    </section>
</body>
</html>
```

Form login **không dùng `th:object`/`th:field`** vì không có object Java nào ở đây, dữ liệu đi thẳng vào filter của Spring Security.

Vì sao báo chung "Sai tên đăng nhập **hoặc** mật khẩu" mà không nói rõ cái nào sai? Nói "tên đăng nhập không tồn tại" là vô tình giúp kẻ xấu dò ra ai có tài khoản.

### `templates/auth/register.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{fragments/layout}">
<head>
    <title>Đăng ký</title>
</head>
<body>
    <section layout:fragment="content">
        <h1>Đăng ký</h1>

        <!-- ${error}: do AuthController.register() đưa vào model khi có lỗi -->
        <p th:if="${error}" th:text="${error}" class="alert alert-error">Lỗi</p>

        <form th:action="@{/register}" th:object="${user}" method="post">
            <label>Tên đăng nhập</label>
            <input type="text" th:field="*{username}" required>

            <label>Mật khẩu (ít nhất 6 ký tự)</label>
            <input type="password" th:field="*{password}" required>

            <button type="submit">Tạo tài khoản</button>
        </form>

        <p>Đã có tài khoản? <a th:href="@{/login}">Đăng nhập</a></p>
    </section>
</body>
</html>
```

---

## Bước 8 — CSRF: vì sao form cũ vẫn chạy, và vì sao bắt buộc dùng `th:action`

Mở trang đăng nhập, bấm **View Page Source** (Ctrl+U), bạn sẽ thấy một ô ẩn mà mình **không hề viết**:

```html
<form action="/login" method="post">
    <input type="hidden" name="_csrf" value="B3dFAN8_76Kx..."/>   ⬅️ tự xuất hiện
    ...
```

Đây là **CSRF token**. Bật Spring Security là mặc định **mọi request POST phải kèm token này**, thiếu là bị **403 Forbidden**.

### CSRF là kiểu tấn công gì?

Bạn đang đăng nhập ở app của mình. Bạn mở một trang web lạ, trong đó có form ẩn tự gửi `POST http://localhost:8093/categories/save`. Trình duyệt **tự gửi kèm cookie đăng nhập của bạn**, nên server tưởng chính bạn bấm. Token chặn được trò này vì trang lạ **không đọc được** token nằm trong trang của bạn.

### Ai chèn token vào?

**Thymeleaf**, khi thấy form có `th:action` + `method="post"`. Vì thế:

| Viết | Kết quả |
|------|---------|
| `<form th:action="@{/categories/save}" method="post">` | ✅ Có token, chạy bình thường |
| `<form action="/categories/save" method="post">` | ❌ Không có token → **403** |

Các form ở bài 05, 08, 11 đều dùng `th:action` nên **không phải sửa gì**.

### Vì sao tắt CSRF cho `/api/**`?

REST API được gọi bằng curl/Postman, không có trang HTML nào để lấy token. API dùng `httpBasic` (gửi user/mật khẩu **trong từng request**, không dựa vào cookie), nên kiểu tấn công trên không áp dụng được. Vì vậy `csrf.ignoringRequestMatchers("/api/**")` an toàn ở đây.

---

## Bước 9 — Nav: "Xin chào, an" + nút Đăng xuất

Sửa `templates/fragments/layout.html`. Thêm namespace `sec` ở thẻ `<html>`, và thêm 2 khối vào `<nav>`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">   <!-- ⬅️ thêm -->
<head>
    ... (giữ nguyên)
</head>
<body>
    <header>
        <nav>
            <a th:href="@{/products}">Sản phẩm</a>
            <a th:href="@{/categories}">Danh mục</a>

            <!-- ⬇️ thêm: chỉ hiện khi ĐÃ đăng nhập -->
            <span class="nav-user" sec:authorize="isAuthenticated()">
                Xin chào, <b sec:authentication="name">user</b>
                <!-- Đăng xuất phải là POST (có CSRF token), không dùng thẻ <a> được -->
                <form th:action="@{/logout}" method="post" class="inline">
                    <button type="submit">Đăng xuất</button>
                </form>
            </span>

            <!-- ⬇️ thêm: chỉ hiện khi CHƯA đăng nhập -->
            <span class="nav-user" sec:authorize="!isAuthenticated()">
                <a th:href="@{/login}">Đăng nhập</a>
                <a th:href="@{/register}">Đăng ký</a>
            </span>
        </nav>
        <hr>
    </header>
    ... (giữ nguyên phần còn lại)
```

| Thuộc tính | Ý nghĩa |
|-----------|---------|
| `sec:authorize="isAuthenticated()"` | Giống `th:if`, nhưng điều kiện là "đã đăng nhập chưa" |
| `sec:authentication="name"` | Giống `th:text`, in ra tên người đang đăng nhập |

### Vì sao đăng xuất không dùng `<a href="/logout">`?

Thẻ `<a>` gửi **GET**, không mang CSRF token. Nếu đăng xuất bằng GET được, trang lạ chỉ cần chèn `<img src="http://localhost:8093/logout">` là đá bạn ra. Spring Security mặc định **chỉ nhận `POST /logout`**, nên phải dùng form.

Thêm vào cuối `static/css/app.css`:

```css
/* ===== Bài 12: đăng nhập ===== */
.nav-user { float: right; }
form.inline { display: inline; }
.alert { padding: 8px 12px; border-radius: 4px; }
.alert-error { background: #fee2e2; color: #991b1b; }
.alert-ok { background: #dcfce7; color: #166534; }
```

---

## Bước 10 — Chạy & kiểm tra

```bash
docker compose -f docker-compose.dev.yml restart app-dev     # đã thêm thư viện ở Bước 1
docker compose -f docker-compose.dev.yml logs -f app-dev
```

Trong log phải thấy dòng này (nghĩa là Spring đã dùng `UserService` của mình, **không** còn dòng "generated security password" nữa):

```
Global AuthenticationManager configured with UserDetailsService bean with name userService
```

### Trên trình duyệt

1. Mở http://localhost:8093/products → bị đẩy sang `/login` (trang của mình, có nav, có CSS).
2. Bấm **Đăng ký** → nhập mật khẩu `123` → báo "Mật khẩu phải có ít nhất 6 ký tự", tên vừa gõ vẫn còn.
3. Đăng ký `an` / `123456` → về trang login với thông báo xanh "Đăng ký thành công".
4. Đăng ký lại `an` → báo "Tên đăng nhập 'an' đã có người dùng".
5. Đăng nhập sai mật khẩu → "Sai tên đăng nhập hoặc mật khẩu".
6. Đăng nhập đúng → vào `/products`, góc phải nav hiện "Xin chào, **an**" + nút Đăng xuất.
7. Thêm/sửa sản phẩm, danh mục → vẫn chạy bình thường (nhờ `th:action` tự gắn CSRF).
8. Bấm **Đăng xuất** → về `/login?logout`, báo "Bạn đã đăng xuất". Mở lại `/products` → bị chặn.
9. Chưa đăng nhập, mở thẳng http://localhost:8093/products/new → bị đẩy về login. Đăng nhập xong, Spring **đưa bạn quay lại đúng `/products/new`** (URL có thêm `?continue`, đó là dấu hiệu của Spring Security, không sao cả).

### Xem mật khẩu trong DB

```bash
docker exec -it demo-mysql mysql -u demo_user -pdemo_pass demo_db \
  -e "SELECT id, username, password, role FROM users;"
```

```
+----+----------+--------------------------------------------------------------+------+
| id | username | password                                                     | role |
+----+----------+--------------------------------------------------------------+------+
|  1 | an       | $2a$10$2F6w7aYEkpQuFijlb3zsy.yAdNIOoqjOLEuLQ0q0/SRM/eRzkTV12 | USER |
+----+----------+--------------------------------------------------------------+------+
```

Thử đăng ký thêm `binh` / `123456` (cùng mật khẩu với `an`) → cột `password` ra **chuỗi khác hẳn**. Đó là tác dụng của salt.

### REST API

```bash
# Không gửi tài khoản → 401 Unauthorized
curl -i http://localhost:8093/api/categories

# Gửi kèm user:mật khẩu (HTTP Basic) → 200
curl -u an:123456 http://localhost:8093/api/categories

# POST cũng được (đã tắt CSRF cho /api/**)
curl -u an:123456 -H 'Content-Type: application/json' \
     -d '{"name":"Chuột"}' http://localhost:8093/api/categories
```

> Vì sao curl nhận **401** còn trình duyệt bị **redirect về /login**? Spring nhìn header `Accept` của request: trình duyệt xin `text/html` → trả trang login; curl xin `*/*` → trả 401 đúng kiểu API.

---

## Lỗi thường gặp

| Triệu chứng | Nguyên nhân | Cách sửa |
|-------------|-------------|----------|
| Vẫn thấy trang login tiếng Anh, log vẫn có "generated security password" | Chưa có `UserService implements UserDetailsService`, hoặc thiếu `@Service` | Kiểm tra Bước 5 |
| Log báo `cannot find symbol ... springframework.security` | Thêm thư viện nhưng chưa restart container | `docker compose -f docker-compose.dev.yml restart app-dev` |
| Trang login **không có CSS** | Quên `permitAll()` cho `/css/**` | Thêm vào `requestMatchers(...)` |
| Bấm Lưu sản phẩm/danh mục → **403 Forbidden** | Form viết `action="..."` thay vì `th:action` → thiếu CSRF token | Đổi sang `th:action="@{...}"` |
| Đăng nhập đúng mật khẩu vẫn báo sai | Mật khẩu trong DB là chữ thường (insert tay bằng SQL), không phải chuỗi BCrypt | Xoá user đó, đăng ký lại qua `/register` |
| App không khởi động, log báo `required a bean of type 'PasswordEncoder' that could not be found` | Thiếu `@Bean PasswordEncoder`, hoặc thiếu `@Configuration` trên `SecurityConfig` | Kiểm tra Bước 4 |
| Đăng nhập luôn báo sai dù user có trong DB | Ô input đặt tên khác `username` / `password` | Sửa `name="username"`, `name="password"` |
| Lỗi `reference to User is ambiguous` | Import cả `com.example.demo.user.User` lẫn `...userdetails.User` | Chỉ dùng tên đầy đủ cho class của Spring như Bước 5 |
| Chưa login mở `/abc` → bị đẩy về `/login` thay vì trang 404 | Đúng như cấu hình: mọi URL lạ cũng thuộc `anyRequest().authenticated()` | Bình thường. Login rồi mở lại sẽ thấy 404. Nhưng nếu **đã login** mà 404 vẫn về `/login` → quên `permitAll()` cho `/error` |
| Nav không hiện "Xin chào", thẻ `sec:` còn nguyên trong HTML | Thiếu thư viện `thymeleaf-extras-springsecurity6` hoặc thiếu `xmlns:sec` | Kiểm tra Bước 1 và Bước 9 |
| Bấm Đăng xuất → 404 / không có gì xảy ra | Đăng xuất bằng `<a href="/logout">` (GET) | Dùng form `POST` như Bước 9 |

## Bài tập

1. **Nhập lại mật khẩu**: thêm ô "Nhập lại mật khẩu" (`name="confirmPassword"`) vào form đăng ký. Nhận bằng `@RequestParam String confirmPassword` ở `AuthController` (giống cách nhận `categoryId` ở bài 11), khác `password` thì báo lỗi.
2. **Tạo sẵn tài khoản admin**: viết một `@Bean CommandLineRunner` (tìm hiểu: chạy 1 lần lúc app khởi động) trong `SecurityConfig` hoặc class mới, nếu chưa có user `admin` thì tạo `admin` / `admin123` với `role = "ADMIN"`. Nhớ phải `passwordEncoder.encode(...)`.
3. **Chỉ ADMIN được xoá**: trong `SecurityConfig`, thêm **trước** dòng `anyRequest()`:
   ```java
   .requestMatchers("/products/*/delete", "/categories/*/delete").hasRole("ADMIN")
   ```
   Đăng nhập bằng `an` rồi bấm Xoá → 403. Sau đó dùng `sec:authorize="hasRole('ADMIN')"` để **ẩn luôn** link Xoá với người không phải admin. Câu hỏi: nếu chỉ ẩn link mà không có dòng `hasRole` trong `SecurityConfig` thì có an toàn không? (Gợi ý: gõ thẳng URL.)
4. **Xoá bằng POST**: link Xoá hiện là `GET /products/{id}/delete`, nên CSRF **không bảo vệ** nó (giống lý do ở phần đăng xuất). Đổi thành một form `POST` nhỏ có nút Xoá, và `@GetMapping` → `@PostMapping` trong controller.
5. **Đã đăng nhập thì khỏi vào trang login**: trong `loginForm()`, nhận thêm tham số `java.security.Principal principal`. Nếu `principal != null` (đã đăng nhập) thì `return "redirect:/products"`.

➡️ Quay lại tổng quan: [../README.md](../README.md)
