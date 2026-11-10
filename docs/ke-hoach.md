# Kế hoạch học tập — việc đã lên kế hoạch nhưng chưa làm

File này giữ lại những việc **đã bàn và chốt** nhưng **chưa làm xong**, để khi mở session mới vẫn làm tiếp được mà không bỏ sót.

Quy ước (xem thêm mục "Kế hoạch đang chờ" trong [CLAUDE.md](../CLAUDE.md)):

- Mỗi việc là một mục `###` nằm dưới **Đang chờ**, ghi rõ: ngày lên kế hoạch, làm khi nào (điều kiện), phạm vi đã chốt, và những gì còn phải hỏi người học.
- Làm xong việc nào thì **xoá hẳn mục đó**. Không đánh dấu ✅ rồi để lại, vì để lại sẽ dễ nhầm với kế hoạch sau này.
- Hết việc thì phần **Đang chờ** chỉ còn chữ *(Không có việc nào đang chờ)*.

---

## Lộ trình hiện tại

```
13 Seeder ✅ → 14 Mongo + Redis ✅ → 15 Ảnh & mô tả (Bước 10 đang dở) → 15.5 Reset & seeder (còn seed mô tả)
   → 16 Redis: lượt xem (đã viết, code xong Bước 1–8, còn Bước 9) → 17 Log gọn bằng Lombok @Slf4j (CHƯA VIẾT)
   → 18 MongoDB: đánh giá (CHƯA VIẾT) → 19 Redis cache (CHƯA VIẾT)
```

Lộ trình 16 → 17 → 18 do người học chốt ngày 2026-10-28: Redis trước (dễ), Mongo sau, cache để cuối (vì dễ vướng lỗi serialize). Ngày 2026-11-04 người học chèn thêm bài log làm **bài 17**, nên MongoDB dời xuống **18** và cache xuống **19**.

### Tiến độ code của người học (chụp ngày 2026-11-10, sẽ cũ dần, nên kiểm tra lại code trước khi dựa vào)

Tài liệu chi tiết từng chức năng theo code thật nằm ở `docs/<module>/` (tạo ngày 2026-11-10).

- Bài 15: **còn dở** Bước 10 (`app.css` chưa có `.preview`, `.muted` dù `form.html`, `list.html`, `detail.html` đang dùng) và Bước 11 (`.dockerignore` chưa có `uploads/`, `docker-compose.yml` chưa có volume `uploads_data`).
- Bài 15.5: đã có script ở **`script/reset-db.sh`** (bài ghi `scripts/`). `seedSampleData()` vẫn **chưa truyền mô tả** (5 sản phẩm mẫu có mô tả rỗng).
- Bài 16: code đã có đủ **Bước 1–8** (`ProductStatsService` có đủ 5 hàm Redis, `ProductService` đã dùng `AppConfig` + `topViewed`/`recentlyViewed`, `delete()` dọn Redis, có `products/detail.html`, CSS Bước 8). **Bước 9 (chạy & kiểm tra)** chưa có bằng chứng đã làm. Sót nhỏ: `ProductService` còn `import ...annotation.Value` thừa, field `productRepository` thiếu `final`.
- `docker-compose.yml` (chế độ thường) chưa có `mongo`, `redis` (bài 14 Bước 2), nên lượt xem bài 16 chỉ chạy ở chế độ dev.

---

## Đang chờ

### 1. Seed thêm nhiều user thường (yêu cầu gốc của bài 15.5, chưa được đáp ứng)

- **Lên kế hoạch:** 2026-10-28. Người học muốn có seeder tạo **user + category + product** để khỏi tạo tay, nhất là vì bài 16 ("Bạn vừa xem" riêng cho từng user) cần nhiều user để thử.
- **Hiện trạng:** bài [15.5](study/15.5-reset-du-lieu-va-chay-lai-seeder.md) chỉ thêm `description` cho sản phẩm và viết script reset. Seeder vẫn chỉ tạo **một** user `admin`. Bài 13 có bài tập 2 (seed `user` / `user123` bằng hàm `createUser(username, password, role)`), nhưng người học chưa làm.
- **Việc cần làm:** hỏi người học muốn **bổ sung một bước vào bài 15.5** (khuyên chọn cách này) hay tự làm bài tập 2 của bài 13. Phạm vi gợi ý: hàm `createUser()`, mỗi user kiểm tra `existsByUsername` riêng (idempotent), seed 2–3 user role `USER` (ví dụ `user1` / `user123`, `user2` / `user123`). Sau đó bài 16 Bước 9 có thể đổi ví dụ để đăng nhập 2 user và so sánh "Bạn vừa xem".

### 2. Viết bài 17 — Khai báo log gọn hơn bằng Lombok `@Slf4j`

- **Lên kế hoạch:** 2026-11-04. **Viết khi** người học làm xong bài 16, trước bài MongoDB.
- **Câu hỏi gốc:** class nào có log cũng phải tự gõ `private static final Logger log = LoggerFactory.getLogger(XXX.class);`. Có tạo được một cái "common" rồi import vào là xong không? Câu trả lời ngắn đã ghi trong [common_knowledge.md](common_knowledge.md#mỗi-class-đều-phải-tự-khai-báo-logger-có-tạo-một-logger-common-rồi-import-vào-được-không).
- **Phạm vi đề xuất:**
  - *Tư vấn trước khi code:* vì sao không dùng chung một logger được. Logger cần **tên class** để log in ra `c.e.demo.product.ProductService`, và `logging.level.com.example.demo: DEBUG` cũng dựa vào tên đó. So sánh 5 cách: (a) một logger `static` chung trong `common/`: sai tên class, không nên dùng; (b) interface `Loggable` có hàm `default log()` dùng `getClass()`: class `@Configuration` bị Spring bọc proxy nên tên in ra thành `WebConfig$$SpringCGLIB$$0`, và mỗi lần gọi lại phải tra logger; (c) `LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())`: copy-paste giữa các file không bị sai tên class, nhưng vẫn là 1 dòng; (d) **Lombok `@Slf4j`** (khuyên dùng); (e) snippet của VSCode: gõ tắt rồi tự bung ra dòng khai báo.
  - *Thực hành:* thêm Lombok vào `build.gradle` (`compileOnly` + `annotationProcessor`, phiên bản do Spring Boot quản lý). Thay `@Slf4j` vào các file đang khai báo logger tay. Ngày 2026-11-04 có 5 file: `nosql/NoSqlConnectionCheck`, `seed/DataSeeder`, `product/ProductService`, `storage/FileStorageService`, `stats/ProductStatsService`, cộng thêm `common/ModuleConfigLoader` của bài 16. Riêng class này dùng `DeferredLogFactory`, **không** thay bằng `@Slf4j` được: phải giải thích vì sao.
  - *Chạy & kiểm tra:* log in ra giống hệt trước (đúng tên class), build trong Docker (`docker-compose.dev.yml`) chạy được, VSCode không gạch đỏ `log` (extension Java của Red Hat đã có sẵn hỗ trợ Lombok).
  - *Lỗi thường gặp:* thiếu dòng `annotationProcessor` thì báo `cannot find symbol: variable log`. Build chạy được mà VSCode vẫn gạch đỏ thì dùng "Java: Clean Java Language Server Workspace". Vẫn còn dòng khai báo cũ thì báo trùng biến `log`.
  - Có thể kèm một phần ngắn "dùng log cho đúng": khi nào dùng `debug` / `info` / `warn` / `error`, và `log.warn("...", e)` để in cả stack trace. Phần `{}` đã có trong common_knowledge, chỉ cần link tới.
- **Phải hỏi người học trước khi viết:**
  1. Chỉ dùng **`@Slf4j`**, hay dùng thêm `@Getter` / `@Setter` của Lombok? [CLAUDE.md](../CLAUDE.md) đang ghi getter/setter viết tay là **cố ý**. Nếu dùng thêm thì đây là refactor lớn (entity, `XxxProperties`...), phải sửa cả CLAUDE.md.
  2. Có gộp phần "dùng log cho đúng" vào bài này không?
- **Khi viết xong:** sửa dòng "Đừng tự ý refactor sang ... Lombok" trong CLAUDE.md, thêm ngoại lệ cho `@Slf4j`. Kiểm tra lại link "Bài sau" ở cuối bài 16. Khuôn bài giống bài 15/16. Chạy thử trên bản sao project **trước khi** viết bài.

### 3. Viết bài 18 — Đánh giá sản phẩm bằng MongoDB

- **Lên kế hoạch:** 2026-10-28. **Viết khi** người học làm xong bài 17, để bám theo code thật của họ. (Lúc lên kế hoạch đây là bài 17. Ngày 2026-11-04 dời thành 18.)
- **Phạm vi đã chốt:** mỗi sản phẩm có danh sách đánh giá (số sao 1–5, nội dung, người viết lấy từ `Principal`, thời gian) và điểm sao trung bình. Học: `@Document`, `MongoRepository` (phải `extends MongoRepository`, xem bài 14), query method kiểu `findByProductIdOrderByCreatedAtDesc`, **document lồng nhau** (danh sách trả lời nằm bên trong đánh giá), **aggregation** để tính điểm trung bình.
- **Những điểm phải dạy:** liên kết 2 DB bằng `productId` (không có JOIN, không có khoá ngoại). Xoá sản phẩm ở MySQL thì **tự xoá** đánh giá ở Mongo, giống cách bài 16 dọn Redis. Script reset của bài 15.5 đã xoá Mongo. Seeder có thể seed vài đánh giá mẫu (nối với mục 1).
- **Nơi hiển thị:** trang chi tiết `products/detail.html` của bài 16.
- **Khuôn bài:** giống bài 15/16: Tư vấn → Ý tưởng → Các bước → Chạy & kiểm tra (có `mongosh`) → Lỗi thường gặp → Bài tập. Chạy thử toàn bộ code trên một bản sao project với DB tạm riêng **trước khi** viết bài.

### 4. Viết bài 19 — Cache danh sách sản phẩm bằng Redis

- **Lên kế hoạch:** 2026-10-28. **Viết sau** bài 18. (Lúc lên kế hoạch đây là bài 18. Ngày 2026-11-04 dời thành 19.)
- **Phạm vi:** `@EnableCaching`, `@Cacheable` cho `findAll()` / `findById()`, `@CacheEvict` khi thêm/sửa/xoá. Dạy khái niệm cache và **khi nào phải xoá cache**.
- **Rủi ro cần xử lý trong bài:** serialize `Product` (có `Category` lazy, và `Category.products` có `@JsonIgnore`) sang Redis, cấu hình serializer JSON thay cho serializer mặc định của Java, đặt TTL. Bài 14 đã hứa "bài sau sẽ dùng `@Cacheable`".


### 5. Sửa các sai sót phát hiện trong bài cũ (chờ người học đồng ý)

- **Lên kế hoạch:** 2026-11-09. Phát hiện trong lúc bổ sung phần "📖 Tra cứu: các hàm có sẵn" cho bài 02 → 15 (việc bổ sung đó **đã xong**; quy ước cho bài mới nằm trong CLAUDE.md). Chưa sửa gì, cần hỏi người học có muốn sửa không.
- **Sai sót trong tài liệu:**
  1. Bài 02 khai báo `private Long id`, nhưng bài 05 dùng `product.getId() == 0` và `${product.id == 0}`. Nếu vẫn để `Long` thì sản phẩm mới bị NPE khi lưu, và form hiện "Sửa" thay vì "Thêm". Bài 06 Bước 2 tự đổi sang `long id` mà không giải thích. (Code thật: `Product.id` là `long`, `Category.id` và `User.id` là `Long`.)
  2. Bài 11, cuối Bước 2 và bảng Lỗi thường gặp: nói quên `addAttribute("categories", ...)` thì lỗi 500. Sai: Thymeleaf 3.1 coi `null` trong `th:each` như list rỗng, nên chỉ ra dropdown trống. Trong bài có một dòng khác nói đúng, nên hai chỗ đang mâu thuẫn.
  3. Bài 13, bảng Lỗi thường gặp, dòng "Sản phẩm mẫu có cột Danh mục trống": gán `Category` chưa `save` thì nhiều khả năng Hibernate ném `TransientPropertyValueException`, chứ không phải để cột trống. **Chưa chạy thử để xác nhận.**
  4. Bài 14, mục "MongoTemplate và StringRedisTemplate là gì?": câu "Bài sau sẽ dùng `MongoRepository` … và `@Cacheable`" đã cũ, vì giờ là bài 18 và 19.
  5. Bài 03 (cheatsheet) còn ghi cổng MySQL `3307` (dòng 37 và 70), thực tế là `3313`.
  6. Lỗi nhỏ: bài 10, message ví dụ không thống nhất giữa phần Ý tưởng và Bước 3. Bài 06 ghi `@Table(name = "Product")` viết hoa, còn bài 02 ghi `"product"`. Bài 12 Bước 3, comment `SELECT COUNT(*) > 0` là cách viết đơn giản hoá (Spring Data thực tế sinh `select ... limit 1`). Bài 12 Bước 4, `.logoutSuccessUrl("/login?logout")` thừa nhưng vô hại.
  7. *(Phát hiện thêm ngày 2026-11-10 khi viết doc module)* Bài 05 dùng URL `/products/edit/{id}`, `/products/delete/{id}`. Từ bài 07, template trong bài đã dùng `/products/{id}/edit`, `/{id}/delete`, nhưng không bài nào có bước sửa mapping trong controller (code thật dùng kiểu mới).
  8. Bài 06 Bước 5 thêm `GET /api/products/by-category/{categoryId}` + `ProductRepository.findByCategoryId`. Code thật không có. Cần hỏi: người học bỏ qua bước này, hay bài nên bỏ?
  9. Bài 15.5 và comment trong `script/reset-db.sh` ghi `scripts/`, trong khi thư mục thật là `script/`.
  10. Bài 14: database Mongo trong bài là `demo_mongo`, nhưng `docker-compose.dev.yml` mặc định `MONGO_DB=demo_db`. Vì vậy lệnh `mongosh ... demo_mongo` trong bài trỏ vào database trống.
  11. Bài 10 ghi message `"Không tìm thấy danh mục id=..."`, code ghi `"không tìm thấy category id ..."`. Bài 12 khai báo `User.role` là `nullable = false`, code để `nullable = true`.
- **Khác biệt trong code của người học (chỉ để biết, không phải lỗi của bài):** `nosql/NoSqlConnectionCheck` gọi `opsForValue().set(key, value)` không có `Duration`, nên key `demo:last-startup` không bao giờ hết hạn. `fragments/layout.html` khác bài 12 Bước 9: không có khối `!isAuthenticated()` chứa link Đăng nhập / Đăng ký, nên nav ở `/login`, `/register` trống. `app.css` thiếu CSS của bài 12 (`.nav-user`, `.alert`...). Xoá danh mục còn sản phẩm thì ra 500 (khoá ngoại). `SecurityConfig` chưa có luật `hasRole` nào.
- **Còn hở:** các hàm chỉ xuất hiện trong phần **Bài tập** (ví dụ `#numbers.formatDecimal` và `RedirectAttributes.addFlashAttribute` ở bài 05) chưa có bảng tra cứu. Hỏi người học có cần thêm không.

---

## Ghi chú nhỏ (không phải kế hoạch đã chốt, chỉ là đề xuất)

- `README.md` mục "Bắt đầu học" mới liệt kê tới bài 08. Có thể đề nghị cập nhật tới bài mới nhất, nhưng **chỉ làm khi người học đồng ý**.
