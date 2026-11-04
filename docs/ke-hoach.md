# Kế hoạch học tập — việc đã lên kế hoạch nhưng chưa làm

File này giữ lại những việc **đã bàn và chốt** nhưng **chưa làm xong**, để khi mở session mới vẫn làm tiếp được mà không bỏ sót.

Quy ước (xem thêm mục "Kế hoạch đang chờ" trong [CLAUDE.md](../CLAUDE.md)):

- Mỗi việc là một mục `###` nằm dưới **Đang chờ**, ghi rõ: ngày lên kế hoạch, làm khi nào (điều kiện), phạm vi đã chốt, và những gì còn phải hỏi người học.
- Làm xong việc nào thì **xoá hẳn mục đó**. Không đánh dấu ✅ rồi để lại, vì để lại sẽ dễ nhầm với kế hoạch sau này.
- Hết việc thì phần **Đang chờ** chỉ còn chữ *(Không có việc nào đang chờ)*.

---

## Lộ trình hiện tại

```
13 Seeder ✅ → 14 Mongo + Redis ✅ → 15 Ảnh & mô tả (còn Bước 10) → 15.5 Reset & seeder (đang làm)
   → 16 Redis: lượt xem (đã viết, mới làm Bước 1) → 17 MongoDB: đánh giá (CHƯA VIẾT) → 18 Redis cache (CHƯA VIẾT)
```

Lộ trình 16 → 17 → 18 do người học chốt ngày 2026-10-28: Redis trước (dễ), Mongo sau, cache để cuối (vì dễ vướng lỗi serialize).

### Tiến độ code của người học (chụp ngày 2026-11-04, sẽ cũ dần, nên kiểm tra lại code trước khi dựa vào)

- Bài 15: **chưa làm Bước 10**. `products/list.html` vẫn là bản cũ: còn cột ID, giá chưa định dạng, `colspan="4"`. Bài 16 Bước 6 cần bước này.
- Bài 15.5: `DataSeeder` đã có overload `createProduct(..., description)` nhưng `seedSampleData()` **chưa truyền mô tả**. **Chưa có** `scripts/reset-db.sh` (Bước 2–4).
- Bài 16: mới xong Bước 1 (`timeout: 2s` trong `application.yml`). Chưa có `stats/ProductStatsService`, `products/detail.html`.

---

## Đang chờ

### 1. Seed thêm nhiều user thường (yêu cầu gốc của bài 15.5, chưa được đáp ứng)

- **Lên kế hoạch:** 2026-10-28. Người học muốn có seeder tạo **user + category + product** để khỏi tạo tay, nhất là vì bài 16 ("Bạn vừa xem" riêng cho từng user) cần nhiều user để thử.
- **Hiện trạng:** bài [15.5](15.5-reset-du-lieu-va-chay-lai-seeder.md) chỉ thêm `description` cho sản phẩm và viết script reset. Seeder vẫn chỉ tạo **một** user `admin`. Bài 13 có bài tập 2 (seed `user` / `user123` bằng hàm `createUser(username, password, role)`), nhưng người học chưa làm.
- **Việc cần làm:** hỏi người học muốn **bổ sung một bước vào bài 15.5** (khuyên chọn cách này) hay tự làm bài tập 2 của bài 13. Phạm vi gợi ý: hàm `createUser()`, mỗi user kiểm tra `existsByUsername` riêng (idempotent), seed 2–3 user role `USER` (ví dụ `user1` / `user123`, `user2` / `user123`). Sau đó bài 16 Bước 8 có thể đổi ví dụ để đăng nhập 2 user và so sánh "Bạn vừa xem".

### 2. Viết bài 17 — Đánh giá sản phẩm bằng MongoDB

- **Lên kế hoạch:** 2026-10-28. **Viết khi** người học làm xong bài 16, để bám theo code thật của họ.
- **Phạm vi đã chốt:** mỗi sản phẩm có danh sách đánh giá (số sao 1–5, nội dung, người viết lấy từ `Principal`, thời gian) và điểm sao trung bình. Học: `@Document`, `MongoRepository` (phải `extends MongoRepository`, xem bài 14), query method kiểu `findByProductIdOrderByCreatedAtDesc`, **document lồng nhau** (danh sách trả lời nằm bên trong đánh giá), **aggregation** để tính điểm trung bình.
- **Những điểm phải dạy:** liên kết 2 DB bằng `productId` (không có JOIN, không có khoá ngoại). Xoá sản phẩm ở MySQL thì **tự xoá** đánh giá ở Mongo, giống cách bài 16 dọn Redis. Script reset của bài 15.5 đã xoá Mongo. Seeder có thể seed vài đánh giá mẫu (nối với mục 1).
- **Nơi hiển thị:** trang chi tiết `products/detail.html` của bài 16.
- **Khuôn bài:** giống bài 15/16: Tư vấn → Ý tưởng → Các bước → Chạy & kiểm tra (có `mongosh`) → Lỗi thường gặp → Bài tập. Chạy thử toàn bộ code trên một bản sao project với DB tạm riêng **trước khi** viết bài.

### 3. Viết bài 18 — Cache danh sách sản phẩm bằng Redis

- **Lên kế hoạch:** 2026-10-28. **Viết sau** bài 17.
- **Phạm vi:** `@EnableCaching`, `@Cacheable` cho `findAll()` / `findById()`, `@CacheEvict` khi thêm/sửa/xoá. Dạy khái niệm cache và **khi nào phải xoá cache**.
- **Rủi ro cần xử lý trong bài:** serialize `Product` (có `Category` lazy, và `Category.products` có `@JsonIgnore`) sang Redis, cấu hình serializer JSON thay cho serializer mặc định của Java, đặt TTL. Bài 14 đã hứa "bài sau sẽ dùng `@Cacheable`".

---

## Ghi chú nhỏ (không phải kế hoạch đã chốt, chỉ là đề xuất)

- `README.md` mục "Bắt đầu học" mới liệt kê tới bài 08. Có thể đề nghị cập nhật tới bài mới nhất, nhưng **chỉ làm khi người học đồng ý**.
