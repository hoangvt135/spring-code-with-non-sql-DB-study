# 13 — Dữ liệu có sẵn: tạo user admin và dữ liệu mẫu khi app khởi động (Seeder)

Mỗi lần `docker compose ... down -v` là DB trống trơn: phải vào `/register` tạo tài khoản, rồi tạo lại từng danh mục, từng sản phẩm bằng tay. Tệ hơn, form đăng ký **luôn tạo role `USER`** (bài 12), nên **không có cách nào tạo được tài khoản ADMIN** qua giao diện.

Mục tiêu bài này: mỗi khi app khởi động, tự động:

- Tạo user **`admin` / `admin123`** với role `ADMIN`, nếu chưa có.
- Tạo **3 danh mục + 5 sản phẩm mẫu**, nếu DB đang trống.
- Chạy lại bao nhiêu lần cũng **không tạo trùng**.

> Kiến thức mới: `CommandLineRunner` (code tự chạy lúc app khởi động), `@Component`, viết log bằng `Logger`, và khái niệm **idempotent** (chạy nhiều lần cho cùng một kết quả).

---

## "Migrate" hay "Seed"? Hai việc khác nhau

Nếu bạn từng dùng Laravel, Rails..., bạn sẽ quen hai lệnh này:

| | Migration (migrate) | Seeder (seed) |
|---|---|---|
| Lo việc gì | **Cấu trúc bảng**: tạo bảng, thêm cột, đổi kiểu cột | **Dữ liệu trong bảng**: user admin, danh mục mẫu... |
| Ví dụ | `CREATE TABLE users (...)` | `INSERT INTO users VALUES ('admin', ...)` |
| Project này đang dùng | `ddl-auto: update`: Hibernate tự tạo bảng từ entity | **Chưa có** ⬅️ bài này làm |

Phần **migrate** (tạo bảng) project đã có: Hibernate tự lo. Thứ còn thiếu là phần **seed** (tạo dữ liệu). Đó chính là việc "tạo sẵn admin, sản phẩm, danh mục mẫu" mà bài này làm.

### Có 3 cách tạo dữ liệu có sẵn trong Spring Boot

| Cách | Viết bằng | Ưu điểm | Nhược điểm với project này |
|------|-----------|---------|----------------------------|
| **`CommandLineRunner`** ⬅️ bài này | Java | Dùng lại được entity, Repository, **`PasswordEncoder`**; tự kiểm tra "có rồi thì bỏ qua" bằng `if` | Dữ liệu nằm trong code Java |
| File `data.sql` | SQL | Ngắn gọn | Phải **tự băm BCrypt** rồi dán chuỗi `$2a$10$...` vào file; chạy **mỗi lần** khởi động nên dễ bị insert trùng; phải chỉnh thêm cấu hình để nó chạy **sau** khi Hibernate tạo bảng |
| **Flyway** (migrate thật) | SQL, file đánh số `V1__...sql`, `V2__...sql` | Chuẩn dự án thật: mỗi file chỉ chạy **đúng 1 lần**, có lịch sử | Phải bỏ `ddl-auto: update` và **tự viết `CREATE TABLE` cho mọi bảng**. Là cả một bài riêng |

Bài này chọn `CommandLineRunner` vì dễ hiểu nhất, viết bằng Java như mọi thứ bạn đã học, và giải quyết gọn chuyện mật khẩu admin phải mã hoá BCrypt.

---

## Ý tưởng

```
docker compose ... up
   │
   ▼
Spring Boot khởi động
   ├── tạo các bean: Repository, Service, Controller, SecurityConfig...
   ├── Hibernate tạo/cập nhật bảng (ddl-auto: update)
   ├── Tomcat mở cổng 8080
   ├── log "Started DemoApplication in ... seconds"
   │
   └── gọi run() của MỌI bean implements CommandLineRunner   ⬅️ DataSeeder chạy ở đây
          ├── seedAdmin()       : có "admin" chưa? Chưa → tạo. Rồi → bỏ qua
          └── seedSampleData()  : bảng category trống? Trống → tạo mẫu. Có rồi → bỏ qua
```

Lúc `run()` được gọi thì **bảng đã có sẵn** (Hibernate tạo xong từ trước), nên cứ việc `save()`.

Các file sẽ đụng tới:

```
com/example/demo/
└── seed/
    └── DataSeeder.java      ⬅️ MỚI  — file duy nhất của bài
```

Không sửa entity, không sửa template, không thêm thư viện.

---

## ⚠️ Vì sao bắt buộc phải "có rồi thì bỏ qua"?

`run()` chạy ở **mỗi lần app khởi động**, mà ở chế độ dev, app khởi động lại **mỗi lần bạn lưu một file `.java`** (DevTools, bài 04). Một buổi code có thể restart hàng chục lần.

Nếu seeder không kiểm tra trước:

- Lần restart thứ 2: insert `admin` lần nữa → cột `username` có `unique = true` → **lỗi, app không khởi động được**.
- Danh mục thì không có `unique`, nên mỗi lần restart lại có thêm **3 danh mục "Điện thoại", "Bàn phím", "Chuột" trùng nhau**.

Code mà chạy 1 lần hay 100 lần đều cho **cùng một kết quả** được gọi là **idempotent**. Seeder nào cũng phải idempotent.

---

## Bước 1 — Khung class `DataSeeder`

Tạo package `seed/` cạnh `product/`, `category/`, `user/`, rồi tạo `seed/DataSeeder.java`:

```java
package com.example.demo.seed;

import com.example.demo.category.Category;
import com.example.demo.category.CategoryRepository;
import com.example.demo.product.Product;
import com.example.demo.product.ProductRepository;
import com.example.demo.user.User;
import com.example.demo.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// @Component: "Spring ơi, tạo giùm 1 object của class này" (giống @Service, nhưng không mang nghĩa "nghiệp vụ")
// implements CommandLineRunner: "và gọi run() của nó sau khi app khởi động xong"
@Component
public class DataSeeder implements CommandLineRunner {

    // Logger: ghi log ra console (hiện trong `docker compose ... logs -f app-dev`)
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;   // @Bean trong SecurityConfig (bài 12)

    // Inject qua constructor như mọi class khác trong project
    public DataSeeder(UserRepository userRepository,
                      CategoryRepository categoryRepository,
                      ProductRepository productRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Spring tự gọi hàm này 1 lần, ngay sau khi app khởi động xong
    @Override
    public void run(String... args) {
        seedAdmin();
        seedSampleData();
    }
}
```

### Vì sao `@Component` mà không phải `@Service`?

Cả hai đều bảo Spring "tạo và quản lý object này". `@Service` thực chất là `@Component` được **đặt tên cho dễ đọc**: "đây là logic nghiệp vụ". Seeder không phải nghiệp vụ (không controller nào gọi nó), nên dùng `@Component` chung chung là hợp nghĩa nhất.

### Vì sao seeder dùng thẳng Repository?

Quy tắc của project là **Controller** không gọi Repository. Seeder không phải controller: nó đứng ngang hàng với Service. Lý do chính không đi qua `UserService.register()`: hàm đó **cố ý ép role về `USER`** (bài 12, để chặn người dùng tự phong admin). Seeder cần tạo `ADMIN`, nên phải tự `save()`.

### `Logger` thay cho `System.out.println`

```java
log.info("[Seed] Đã tạo user admin");
```

Dòng log sẽ có đủ thời gian, mức độ (`INFO`), tên class, giống mọi dòng log khác của Spring. `System.out.println` cũng chạy, nhưng chỉ in ra chữ trơn. Chữ `[Seed]` ở đầu là để dễ tìm bằng `grep`.

---

## Bước 2 — Tạo user admin

Thêm method này vào **trong** class `DataSeeder`, dưới `run()`:

```java
    private void seedAdmin() {
        // Có rồi thì thôi (idempotent)
        if (userRepository.existsByUsername("admin")) {
            log.info("[Seed] Đã có user admin → bỏ qua");
            return;
        }

        User admin = new User();
        admin.setUsername("admin");
        // BẮT BUỘC mã hoá: lưu "admin123" trần thì đăng nhập luôn báo sai (bài 12)
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole("ADMIN");
        userRepository.save(admin);

        log.info("[Seed] Đã tạo user admin / admin123");
    }
```

- `existsByUsername` là query method bạn đã viết ở bài 12, giờ dùng lại.
- Kiểm tra **theo đúng user `admin`**, không phải "bảng users có trống không". Nhờ vậy dù đã có người đăng ký trước, admin vẫn được tạo.

> ⚠️ Mật khẩu `admin123` nằm ngay trong code là **chấp nhận được với project học tập**, không bao giờ làm vậy với dự án thật. Bài tập 1 chỉ cách lấy mật khẩu từ biến môi trường.

---

## Bước 3 — Danh mục và sản phẩm mẫu

Thêm tiếp 3 method:

```java
    private void seedSampleData() {
        // Bảng category đã có dữ liệu → coi như DB không còn trống, không chèn mẫu nữa
        if (categoryRepository.count() > 0) {
            log.info("[Seed] Bảng category đã có dữ liệu → bỏ qua dữ liệu mẫu");
            return;
        }

        // Tạo danh mục TRƯỚC, giữ lại object để gán cho sản phẩm
        Category phone = createCategory("Điện thoại");
        Category keyboard = createCategory("Bàn phím");
        Category mouse = createCategory("Chuột");

        createProduct("iPhone 15", 22990000.0, phone);
        createProduct("Samsung Galaxy S24", 19990000.0, phone);
        createProduct("Keychron K2", 1890000.0, keyboard);
        createProduct("Logitech MX Keys", 2490000.0, keyboard);
        createProduct("Logitech G102", 390000.0, mouse);

        log.info("[Seed] Đã tạo 3 danh mục + 5 sản phẩm mẫu");
    }

    // Hàm phụ: đỡ phải lặp 3 dòng new / set / save cho mỗi danh mục
    private Category createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        return categoryRepository.save(category);   // trả về object ĐÃ CÓ id
    }

    private void createProduct(String name, double price, Category category) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(price);
        product.setCategory(category);   // gán quan hệ như bài 06/11
        productRepository.save(product);
    }
```

### Vì sao phải tạo danh mục trước?

Bảng `product` lưu **id** của danh mục ở cột `category_id` (bài 06). Một `Category` mới tạo bằng `new` thì chưa có id. Sau khi `categoryRepository.save(...)`, MySQL sinh id và Hibernate **gán ngược id vào object**. Vì thế ta giữ lại object được `save` trả về (`phone`, `keyboard`...) để gán cho sản phẩm.

### Vì sao điều kiện là `count() > 0` mà không kiểm tra từng tên?

Dữ liệu mẫu chỉ để **DB mới tinh có cái mà xem**. Nếu bạn đã tự tạo danh mục của mình, chèn thêm "Điện thoại", "Bàn phím"... sẽ làm lẫn lộn dữ liệu thật. Quy tắc đơn giản: **bảng trống thì chèn, có gì rồi thì thôi**.

Ngược lại với admin: tài khoản admin thì **lúc nào cũng cần có**, nên kiểm tra theo đúng tên `admin`.

---

## Bước 4 — Chạy & kiểm tra

Không thêm thư viện nên **không cần restart**: lưu file là DevTools tự khởi động lại app.

```bash
docker compose -f docker-compose.dev.yml logs -f app-dev
```

### Lần chạy đầu

Nếu DB đang **chưa có danh mục nào**, log sẽ có (ngay **sau** dòng `Started DemoApplication`):

```
Started DemoApplication in 15.648 seconds ...
... c.example.demo.seed.DataSeeder : [Seed] Đã tạo user admin / admin123
... c.example.demo.seed.DataSeeder : [Seed] Đã tạo 3 danh mục + 5 sản phẩm mẫu
```

Nếu DB **đã có danh mục bạn tự tạo**, dòng thứ hai sẽ là "bỏ qua dữ liệu mẫu". Đó là đúng như thiết kế. Muốn thấy dữ liệu mẫu thì xem mục "Làm lại từ đầu" bên dưới.

### Lần chạy thứ 2 trở đi

Sửa bất kỳ file `.java` nào rồi lưu (thêm 1 dấu cách cũng được). App restart, log phải ghi:

```
[Seed] Đã có user admin → bỏ qua
[Seed] Bảng category đã có dữ liệu → bỏ qua dữ liệu mẫu
```

Và số dòng trong DB **không tăng**:

```bash
docker exec -it demo-mysql mysql --default-character-set=utf8mb4 -u demo_user -pdemo_pass demo_db -e "
  SELECT id, username, role FROM users;
  SELECT p.id, p.name, p.price, c.name AS category
  FROM product p LEFT JOIN category c ON c.id = p.category_id;"
```

> **`--default-character-set=utf8mb4` là gì?** Thiếu nó, client `mysql` hiển thị tiếng Việt thành `?i?n tho?i`. Dữ liệu **không hỏng**: mở `/categories` trên trình duyệt vẫn thấy đúng "Điện thoại". Chỉ là client dòng lệnh đọc sai bảng mã.

### Trên trình duyệt

1. Mở http://localhost:8093/login → đăng nhập **`admin` / `admin123`** → vào được `/products`.
2. Trang sản phẩm có 5 sản phẩm, cột Danh mục đã điền sẵn (nhờ bài 11).
3. Trang danh mục có 3 danh mục.

### Làm lại từ đầu

```bash
docker compose -f docker-compose.dev.yml down -v     # ⚠️ xoá SẠCH dữ liệu
docker compose -f docker-compose.dev.yml up -d
```

App khởi động trên DB trống → seeder tạo lại admin + dữ liệu mẫu. Từ giờ `down -v` **không còn đáng sợ**: vài chục giây sau là có lại đủ dữ liệu để làm việc.

> Nhắc lại: sau `down -v`, MySQL khởi tạo lần đầu có thể chậm và báo `dependency mysql failed to start`. Chờ một lát rồi chạy lại `up -d` là được.

---

## Lỗi thường gặp

| Triệu chứng | Nguyên nhân | Cách sửa |
|-------------|-------------|----------|
| Không thấy dòng log `[Seed]` nào | Thiếu `@Component`, hoặc quên `implements CommandLineRunner` | Kiểm tra Bước 1 |
| App không khởi động, log có `Duplicate entry 'admin' for key` | Thiếu đoạn `if (existsByUsername(...)) return;` | Thêm lại kiểm tra ở Bước 2 |
| Mỗi lần restart lại có thêm 3 danh mục trùng tên | Thiếu `if (categoryRepository.count() > 0) return;` | Thêm lại kiểm tra; xoá bớt dòng trùng hoặc `down -v` |
| Đăng nhập `admin` / `admin123` báo sai | Quên `passwordEncoder.encode(...)`, lưu mật khẩu trần | Sửa code, rồi xoá user cũ: `DELETE FROM users WHERE username='admin';` và restart |
| Log "Đã có user admin" nhưng vẫn đăng nhập sai | User `admin` được tạo **trước** bằng cách khác (đăng ký tay, sai mật khẩu) | Xoá user đó như dòng trên, restart để seeder tạo lại |
| Log "bỏ qua dữ liệu mẫu" dù trang sản phẩm trống | Bảng `category` có dữ liệu, chỉ `product` trống | Đúng thiết kế. Muốn có mẫu thì `down -v`, hoặc xoá hết danh mục |
| Sản phẩm mẫu có cột Danh mục trống | Gán category **trước khi** `save` danh mục (object chưa có id) | Dùng object do `categoryRepository.save(...)` trả về, như Bước 3 |
| App không khởi động, log có `DataSeeder required a bean of type 'PasswordEncoder' that could not be found` | Chưa làm bài 12 (chưa có `@Bean PasswordEncoder`) | Làm bài 12 trước |
| `SELECT` thấy `?i?n tho?i` | Client `mysql` sai bảng mã | Thêm `--default-character-set=utf8mb4` |

## Bài tập

1. **Mật khẩu admin từ biến môi trường**: thêm tham số vào constructor của `DataSeeder`:
   ```java
   @Value("${ADMIN_PASSWORD:admin123}") String adminPassword
   ```
   (import `org.springframework.beans.factory.annotation.Value`). Cú pháp `${TEN:mac_dinh}` giống hệt cách `application.yml` đang đọc `DB_PASSWORD`. Thêm `ADMIN_PASSWORD: ...` vào phần `environment` của `app-dev` trong `docker-compose.dev.yml`, `down -v` rồi `up` lại, thử đăng nhập bằng mật khẩu mới.
2. **Thêm user thường**: seed thêm `user` / `user123` với role `USER`. Viết hàm phụ `createUser(username, password, role)` để `seedAdmin()` và phần mới dùng chung, giống `createCategory()`.
3. **Kết hợp bài 12 (phân quyền)**: làm bài tập 3 của bài 12 (chỉ ADMIN được xoá). Đăng nhập lần lượt bằng `admin` và `user` để thấy khác biệt. Giờ đã có sẵn cả hai tài khoản, không phải tạo tay nữa.
4. **Bật/tắt dữ liệu mẫu bằng cấu hình**: thêm vào `application.yml`
   ```yaml
   app:
     seed-sample-data: true
   ```
   rồi nhận bằng `@Value("${app.seed-sample-data:true}") boolean seedSampleData`. Trong `run()`, chỉ gọi `seedSampleData()` khi giá trị là `true`. Admin thì vẫn luôn được tạo. Hỏi: vì sao dự án thật thường **tắt** dữ liệu mẫu ở môi trường production?
5. **Tìm hiểu**: đọc qua cách Flyway đặt tên file `V1__create_tables.sql`, `V2__seed_admin.sql` và bảng `flyway_schema_history`. So sánh: Flyway biết "file này đã chạy rồi" bằng cách nào, còn seeder của bài này biết bằng cách nào?

➡️ Quay lại tổng quan: [../README.md](../README.md)
