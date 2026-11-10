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
   → 16 Redis: lượt xem ✅ → 16.5 Nâng cấp Spring Boot 4.1.2 (ĐÃ VIẾT, chờ người học làm)
   → 17 Lombok @Slf4j + @Getter/@Setter (ĐÃ VIẾT, làm sau 16.5)
   → 18 MongoDB: đánh giá (CHƯA VIẾT) → 19 Redis cache (CHƯA VIẾT)
```

Lộ trình 16 → 17 → 18 do người học chốt ngày 2026-10-28: Redis trước (dễ), Mongo sau, cache để cuối (vì dễ vướng lỗi serialize). Ngày 2026-11-04 người học chèn thêm bài log làm **bài 17**, nên MongoDB dời xuống **18** và cache xuống **19**.

### Tiến độ code của người học (chụp ngày 2026-11-10, sẽ cũ dần, nên kiểm tra lại code trước khi dựa vào)

Tài liệu chi tiết từng chức năng theo code thật nằm ở `docs/<module>/` (tạo ngày 2026-11-10).

- Bài 15: **còn dở** Bước 10 (`app.css` chưa có `.preview`, `.muted` dù `form.html`, `list.html`, `detail.html` đang dùng) và Bước 11 (`.dockerignore` chưa có `uploads/`, `docker-compose.yml` chưa có volume `uploads_data`).
- Bài 15.5: đã có script ở **`script/reset-db.sh`** (bài ghi `scripts/`). `seedSampleData()` vẫn **chưa truyền mô tả** (5 sản phẩm mẫu có mô tả rỗng).
- Bài 16: **người học báo đã xong** (2026-11-10). Code có đủ **Bước 1–8** (`ProductStatsService` có đủ 5 hàm Redis, `ProductService` đã dùng `AppConfig` + `topViewed`/`recentlyViewed`, `delete()` dọn Redis, có `products/detail.html`, CSS Bước 8). Sót nhỏ: `ProductService` còn `import ...annotation.Value` thừa (bài 17 Bước 2 nhắc xoá), field `productRepository` thiếu `final`.
- Bài 16.5 (viết ngày 2026-11-10, theo yêu cầu người học): nâng Spring Boot 3.4.1 → 3.5.16 → **4.1.2**, Gradle 8.11 → **9.8.2**, sửa template gọi bean trong `th:src`, `EnvironmentPostProcessor` đổi package, 2 property đổi tên. **Chưa làm.** Đã chạy thử đủ trên bản sao. Người học làm xong thì: cập nhật dòng "Phiên bản" trong `docs/cong-nghe/*.md` và CLAUDE.md, đổi dòng ⬜ bài 16.5 trong doc module thành đã làm.
- Bài 17 (viết ngày 2026-11-10, phạm vi người học chọn: `@Slf4j` + `@Getter`/`@Setter`, có mục "dùng log cho đúng"): **chưa làm**. Doc module đã ghi dòng lịch sử ⬜ cho bài 17; làm xong thì đổi thành đã làm.
- `docker-compose.yml` (chế độ thường) chưa có `mongo`, `redis` (bài 14 Bước 2), nên lượt xem bài 16 chỉ chạy ở chế độ dev.

---

## Đang chờ

### 1. Seed thêm nhiều user thường (yêu cầu gốc của bài 15.5, chưa được đáp ứng)

- **Lên kế hoạch:** 2026-10-28. Người học muốn có seeder tạo **user + category + product** để khỏi tạo tay, nhất là vì bài 16 ("Bạn vừa xem" riêng cho từng user) cần nhiều user để thử.
- **Hiện trạng:** bài [15.5](study/15.5-reset-du-lieu-va-chay-lai-seeder.md) chỉ thêm `description` cho sản phẩm và viết script reset. Seeder vẫn chỉ tạo **một** user `admin`. Bài 13 có bài tập 2 (seed `user` / `user123` bằng hàm `createUser(username, password, role)`), nhưng người học chưa làm.
- **Việc cần làm:** hỏi người học muốn **bổ sung một bước vào bài 15.5** (khuyên chọn cách này) hay tự làm bài tập 2 của bài 13. Phạm vi gợi ý: hàm `createUser()`, mỗi user kiểm tra `existsByUsername` riêng (idempotent), seed 2–3 user role `USER` (ví dụ `user1` / `user123`, `user2` / `user123`). Sau đó bài 16 Bước 9 có thể đổi ví dụ để đăng nhập 2 user và so sánh "Bạn vừa xem".

### 2. Viết bài 18 — Đánh giá sản phẩm bằng MongoDB

- **Lên kế hoạch:** 2026-10-28. **Viết khi** người học làm xong [bài 17](study/17-lombok-slf4j-getter-setter.md), để bám theo code thật của họ. Entity `Review` (nếu là class Java) dùng `@Getter`/`@Setter` theo quy tắc Lombok trong CLAUDE.md. (Lúc lên kế hoạch đây là bài 17. Ngày 2026-11-04 dời thành 18.)
- **Phạm vi đã chốt:** mỗi sản phẩm có danh sách đánh giá (số sao 1–5, nội dung, người viết lấy từ `Principal`, thời gian) và điểm sao trung bình. Học: `@Document`, `MongoRepository` (phải `extends MongoRepository`, xem bài 14), query method kiểu `findByProductIdOrderByCreatedAtDesc`, **document lồng nhau** (danh sách trả lời nằm bên trong đánh giá), **aggregation** để tính điểm trung bình.
- **Những điểm phải dạy:** liên kết 2 DB bằng `productId` (không có JOIN, không có khoá ngoại). Xoá sản phẩm ở MySQL thì **tự xoá** đánh giá ở Mongo, giống cách bài 16 dọn Redis. Script reset của bài 15.5 đã xoá Mongo. Seeder có thể seed vài đánh giá mẫu (nối với mục 1).
- **Nơi hiển thị:** trang chi tiết `products/detail.html` của bài 16.
- **Khuôn bài:** giống bài 15/16: Tư vấn → Ý tưởng → Các bước → Chạy & kiểm tra (có `mongosh`) → Lỗi thường gặp → Bài tập. Chạy thử toàn bộ code trên một bản sao project với DB tạm riêng **trước khi** viết bài.

### 3. Viết bài 19 — Cache danh sách sản phẩm bằng Redis

- **Lên kế hoạch:** 2026-10-28. **Viết sau** bài 18. (Lúc lên kế hoạch đây là bài 18. Ngày 2026-11-04 dời thành 19.)
- **Phạm vi:** `@EnableCaching`, `@Cacheable` cho `findAll()` / `findById()`, `@CacheEvict` khi thêm/sửa/xoá. Dạy khái niệm cache và **khi nào phải xoá cache**.
- **Rủi ro cần xử lý trong bài:** serialize `Product` (có `Category` lazy, và `Category.products` có `@JsonIgnore`) sang Redis, cấu hình serializer JSON thay cho serializer mặc định của Java, đặt TTL. Bài 14 đã hứa "bài sau sẽ dùng `@Cacheable`".


### 4. Sửa các sai sót phát hiện trong bài cũ (chờ người học đồng ý)

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
  12. *(Phát hiện ngày 2026-11-10 khi viết `docs/cong-nghe/`)* Bài 09 mục 3 ghi `@RequestParam` "chưa dùng trong project", nhưng `ProductViewController.save()` đã dùng từ bài 11. Bài 15 (Bước 1 và bảng lỗi) còn ghi ảnh mặc định `.svg`, code dùng `.jpg`.
  13. Bài 05 và bài 07 (bảng lỗi) ghi "thiếu `xmlns:th` / `xmlns:layout` thì thuộc tính bị in nguyên văn". Theo tài liệu Thymeleaf, khai báo `xmlns` chỉ để IDE không báo lỗi, không ảnh hưởng xử lý. Bài 07 dùng `~{layout}`, code thật dùng `~{fragments/layout}`.
  14. Bài 12 Bước 2 ghi bảng tên `users` vì "`user` trùng từ khoá MySQL". Trong MySQL 8.4, `USER` là từ khoá nhưng **không** phải từ dành riêng (`CREATE TABLE user` vẫn chạy). Lý do đúng hơn: `user` là từ dành riêng ở PostgreSQL.
  15. Chưa bài nào nhắc: với Hibernate 6.6, `POST /api/products` gửi kèm một `"id"` không tồn tại sẽ ném `OptimisticLockException` (lỗi 500).
  16. Comment trong `Dockerfile` ghi "copy build.gradle trước để tận dụng cache", nhưng giữa 2 lệnh `COPY` không có bước `RUN` tải thư viện, nên cache không có tác dụng. Image cũng không copy `gradle.properties`.
  17. *(Đã đưa vào [bài 16.5](study/16.5-nang-cap-spring-boot-4.md) Bước 3, không còn chờ.)* 3 template sản phẩm gọi bean trong `th:src`, bị Thymeleaf 3.1.4+ cấm.
- **Khác biệt trong code của người học (chỉ để biết, không phải lỗi của bài):** `nosql/NoSqlConnectionCheck` gọi `opsForValue().set(key, value)` không có `Duration`, nên key `demo:last-startup` không bao giờ hết hạn. `fragments/layout.html` khác bài 12 Bước 9: không có khối `!isAuthenticated()` chứa link Đăng nhập / Đăng ký, nên nav ở `/login`, `/register` trống. `app.css` thiếu CSS của bài 12 (`.nav-user`, `.alert`...). Xoá danh mục còn sản phẩm thì ra 500 (khoá ngoại). `SecurityConfig` chưa có luật `hasRole` nào.
- **Còn hở:** các hàm chỉ xuất hiện trong phần **Bài tập** (ví dụ `#numbers.formatDecimal` và `RedirectAttributes.addFlashAttribute` ở bài 05) chưa có bảng tra cứu. Hỏi người học có cần thêm không.

---

## Ghi chú nhỏ (không phải kế hoạch đã chốt, chỉ là đề xuất)

- `README.md` mục "Bắt đầu học" mới liệt kê tới bài 08. Có thể đề nghị cập nhật tới bài mới nhất, nhưng **chỉ làm khi người học đồng ý**.
