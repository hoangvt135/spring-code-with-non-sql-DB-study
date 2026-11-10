# Dữ liệu mẫu (DataSeeder) và reset dữ liệu

> **Module:** `seed` (+ script `script/reset-db.sh`) · **Bài học nguồn:** [bài 13](../study/13-du-lieu-mau-seeder.md), [bài 15.5](../study/15.5-reset-du-lieu-va-chay-lai-seeder.md), [bài 17](../study/17-lombok-slf4j-getter-setter.md) · **Cập nhật:** 2026-11-10
> **Trạng thái code:** 🔄 Đang làm (bài 15.5 Bước 1: `seedSampleData()` chưa truyền mô tả) — theo code thật ngày 2026-11-10

## Chức năng này làm gì

- **`DataSeeder`**: mỗi lần app khởi động, tự tạo user `admin` / `admin123` (role `ADMIN`) và 3 danh mục + 5 sản phẩm mẫu. Chạy lại bao nhiêu lần cũng **không nhân đôi** dữ liệu (idempotent).
- **`script/reset-db.sh`**: xoá sạch MySQL, MongoDB, Redis và ảnh trong `uploads/`, rồi khởi động lại app để Hibernate tạo lại bảng và seeder chèn lại dữ liệu mẫu. Giống `php artisan migrate:fresh --seed`.

Project không có lệnh "chạy seeder" riêng: **chạy seeder = khởi động lại app** ([bài 15.5](../study/15.5-reset-du-lieu-va-chay-lai-seeder.md#chạy-seeder-nghĩa-là-gì-trong-project-này)).

## Đường dẫn & giao diện

Không có. `DataSeeder` chạy lúc khởi động. Script chạy từ terminal, ở thư mục gốc project:

```bash
./script/reset-db.sh       # hỏi lại trước khi xoá
./script/reset-db.sh -y    # không hỏi
```

> ⚠️ Khác bài học: bài 15.5 (và comment ở đầu chính file script) ghi `./scripts/reset-db.sh`, nhưng thư mục thật là **`script/`** (không có `s`). Script vẫn chạy đúng vì dòng `cd "$(dirname "$0")/.."` luôn đưa về thư mục gốc.

## Luồng xử lý

```
App khởi động → Hibernate tạo/cập nhật bảng → "Started DemoApplication"
  └─ DataSeeder.run()   (CommandLineRunner)
       ├─ seedAdmin()
       │    ├─ existsByUsername("admin") = true  → log "[Seed] đã có admin", bỏ qua
       │    └─ chưa có → encode("admin123") → save → log "[Seed] đã tạo admin"
       └─ seedSampleData()
            ├─ categoryRepository.count() > 0     → log "...bỏ qua dữ liệu mẫu", bỏ qua
            └─ bảng trống → tạo 3 Category (save trước, lấy object có id)
                          → tạo 5 Product gán category → log "[Seed] Đã tạo 3 danh mục + 5 sản phẩm mẫu"
  Lỗi trong run() (ví dụ thiếu bean PasswordEncoder, trùng username) → app KHÔNG khởi động

./script/reset-db.sh
  [hỏi y/n, trừ khi có -y]  → khác "y" thì "Đã huỷ.", exit 0
  [1] up -d --wait mysql mongo redis   [2] stop app-dev
  [3] MySQL: DROP + CREATE DATABASE    [4] Mongo: db.dropDatabase()   Redis: FLUSHDB
  [5] rm -f uploads/*                  [6] up -d app-dev, đọc log từ mốc "since" mỗi 5 giây
        ├─ thấy "[Seed]"                         → ✅ in log seeder, exit 0
        ├─ thấy "APPLICATION FAILED TO START"    → ❌ exit 1
        ├─ đã "Started" quá 1 phút mà không có [Seed] → ⚠️ exit 1
        └─ quá 5 phút                            → ⏳ exit 1
  Bất kỳ lệnh nào lỗi giữa chừng → dừng ngay (set -e)
```

## File liên quan

| File | Vai trò |
|---|---|
| [`seed/DataSeeder.java`](../../src/main/java/com/example/demo/seed/DataSeeder.java) | `@Component` + `CommandLineRunner`: `seedAdmin()`, `seedSampleData()`, `createCategory()`, 2 overload `createProduct()` |
| [`script/reset-db.sh`](../../script/reset-db.sh) | Script reset 6 bước, chỉ dùng cho chế độ dev (`docker-compose.dev.yml`) |
| `user/UserRepository`, `category/CategoryRepository`, `product/ProductRepository` | Seeder gọi thẳng Repository (không qua Service) |
| `config/SecurityConfig.java` | Cung cấp bean `PasswordEncoder` (BCrypt) mà seeder cần |

## Dữ liệu

| Bảng | Seeder tạo | Điều kiện bỏ qua |
|---|---|---|
| `users` | `admin` / BCrypt(`admin123`) / `ADMIN` | Đã có username `admin` |
| `category` | Điện thoại, Bàn phím, Chuột | Bảng `category` có ít nhất 1 dòng |
| `product` | iPhone 15, Samsung Galaxy S24 (Điện thoại); Keychron K2, Logitech MX Keys (Bàn phím); Logitech G102 (Chuột). `imagePath` = `null` → hiện ảnh mặc định | Cùng điều kiện với `category` |

> ⚠️ Khác bài học: bài 15.5 Bước 1 truyền mô tả cho từng sản phẩm (và cố ý để `null` cho G102). Code thật đã có overload `createProduct(name, price, category, description)`, nhưng `seedSampleData()` vẫn gọi bản 3 tham số, bản này truyền `""`. Kết quả: **cả 5 sản phẩm mẫu có `description` là chuỗi rỗng**, không có trường hợp `null`.

Script xoá: database MySQL `DB_NAME` (mặc định `demo_db`), database MongoDB `MONGO_DB` (mặc định `demo_db`, giống `docker-compose.dev.yml`), **mọi key** Redis, mọi file trong `uploads/`. User đã đăng ký và phiên đăng nhập đều mất.

## Cấu hình

- Seeder **không có cấu hình**: tên, mật khẩu admin và dữ liệu mẫu viết cứng trong code (chấp nhận được với project học tập, xem [bài 13 Bước 2](../study/13-du-lieu-mau-seeder.md#bước-2--tạo-user-admin)).
- Script đọc `.env` nếu có, rồi dùng mặc định: `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `MONGO_DB`, `MONGO_USERNAME`, `MONGO_PASSWORD`. Tên container cứng: `demo-mysql`, `demo-mongo`, `demo-redis`, `demo-app-dev`.

## Quy tắc & lưu ý khi sửa

- **Mỗi lần thêm trường vào entity thì cập nhật seeder**, có cả giá trị mẫu lẫn trường hợp `null` ([quy trình](../study/15.5-reset-du-lieu-va-chay-lai-seeder.md#quy-trình-mỗi-lần-đổi-cấu-trúc-bảng)).
- Mọi phần seed mới phải **idempotent**: kiểm tra "có rồi thì bỏ qua" trước khi `save` ([lý do](../study/13-du-lieu-mau-seeder.md#-vì-sao-bắt-buộc-phải-có-rồi-thì-bỏ-qua)).
- Tạo `Category` **trước**, dùng object do `save()` trả về để gán cho `Product`.
- Admin kiểm tra theo **tên**; dữ liệu mẫu kiểm tra theo **bảng trống** (`count() > 0`). Sửa seeder mà DB đã có danh mục thì sẽ không thấy dữ liệu mới: phải reset.
- Chỉ **thêm** trường thì `ddl-auto: update` tự lo; **sửa, đổi tên, xoá** trường thì cần reset ([bảng](../study/15.5-reset-du-lieu-va-chay-lai-seeder.md#khi-nào-cần-reset)).
- Dữ liệu ở MySQL, MongoDB, Redis trỏ tới nhau bằng id. Reset thì phải reset **cả 3**, nếu không sản phẩm mới id `3` sẽ "thừa hưởng" lượt xem của sản phẩm cũ id `3`.
- Script nhận diện seeder xong nhờ chữ `[Seed]` trong log. Đổi nội dung `log.info` thì giữ tiền tố `[Seed]`.
- Đổi mật khẩu/user DB trong `.env` thì script không giúp được: dùng `docker compose -f docker-compose.dev.yml down -v`.

## Liên quan

- [Quản lý sản phẩm](../product/quan-ly-san-pham.md), [Quản lý danh mục](../category/quan-ly-danh-muc.md): dữ liệu mà seeder tạo.
- [Đăng nhập & đăng ký](../user/dang-nhap-dang-ky.md): tài khoản `admin`, `PasswordEncoder`.
- [Lượt xem và top sản phẩm](../product/luot-xem-va-top-san-pham.md): dữ liệu Redis bị `FLUSHDB` khi reset.
- [Lưu file upload](../storage/luu-file-upload.md): thư mục `uploads/` bị xoá khi reset.
- [Kiểm tra kết nối Mongo/Redis](../nosql/kiem-tra-ket-noi-mongo-redis.md): cũng là `CommandLineRunner`, chạy cùng lúc khởi động.
- Việc đang chờ: seed thêm nhiều user thường, xem [ke-hoach.md](../ke-hoach.md).

## Lịch sử thay đổi

| Bài | Thay đổi |
|---|---|
| [13](../study/13-du-lieu-mau-seeder.md) | Tạo `DataSeeder`: admin + 3 danh mục + 5 sản phẩm, idempotent |
| [15.5](../study/15.5-reset-du-lieu-va-chay-lai-seeder.md) | Thêm overload `createProduct(..., description)` (chưa dùng trong `seedSampleData()`); thêm script reset (đặt ở `script/`) |
| [17](../study/17-lombok-slf4j-getter-setter.md) | ⬜ Chưa làm. `DataSeeder` dùng `@Slf4j` thay dòng khai báo `Logger` |
