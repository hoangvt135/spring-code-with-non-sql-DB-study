# 04 — Hot reload: sửa code không cần build lại image

## Vì sao Java cần build, còn PHP thì không?

- **PHP** là ngôn ngữ *thông dịch*: web server đọc thẳng file `.php` mỗi request. Sửa file → chạy code mới ngay.
- **Java** là ngôn ngữ *biên dịch*: `.java` phải được dịch thành `.class` rồi đóng thành `.jar`. Container chạy file `.jar` đã đóng gói, nó không biết tới `.java` gốc.

```
PHP:   sửa index.php ─────────────────────► request chạy code mới ngay
Java:  sửa Product.java ─► compile ─► .jar ─► request chạy code mới
```

Đổi lại, Java bắt lỗi sớm (lúc build) và chạy nhanh hơn khi vận hành.

## Hai chế độ chạy trong project này

| | **Prod** (`docker-compose.yml`) | **Dev hot-reload** (`docker-compose.dev.yml`) |
|---|---|---|
| Cách chạy app | Build image `.jar` rồi chạy | Mount code + chạy bằng Gradle |
| Khi sửa code | Phải `up --build` lại | Tự biên dịch & restart trong vài giây |
| Tốc độ khởi động lần đầu | Nhanh | Chậm hơn (tải thư viện vào cache) |
| Dùng khi nào | Giống môi trường thật, deploy | Lúc đang code, thử nhanh |

## Cách chạy chế độ dev

```bash
# Bật (chạy nền)
docker compose -f docker-compose.dev.yml up -d

# Xem log để thấy app restart mỗi khi bạn lưu file
docker compose -f docker-compose.dev.yml logs -f app-dev

# Dừng
docker compose -f docker-compose.dev.yml down
```

Mở http://localhost:8093/ để kiểm tra.

## Thử hot reload

1. Bật dev mode và mở log (`logs -f app-dev`).
2. Sửa `src/main/java/com/example/demo/controller/HelloController.java`, đổi dòng chữ trong `home()`.
3. **Lưu file.** Trong ~2-5 giây, log sẽ hiện app khởi động lại (dòng có `Restarting` / `Started DemoApplication`).
4. Tải lại http://localhost:8093/ → thấy nội dung mới, **không cần** build lại image.

## Cơ chế hoạt động (bên trong)

Container dev chạy **2 tiến trình Gradle song song**:

1. `gradle classes --continuous` — canh mọi thay đổi rồi tự: biên dịch lại `.java` → `.class` **và** copy lại template/resource (task `classes` = `compileJava` + `processResources`). Nhờ bước copy resource này mà sửa file `.html` (Thymeleaf) cũng được nhận.
2. `gradle bootRun` — chạy app. **Spring Boot DevTools** (đã thêm vào `build.gradle`) theo dõi thư mục classpath; thấy file mới thì khởi động lại app rất nhanh (chỉ nạp lại code của bạn, không nạp lại toàn bộ thư viện).

> ⚠️ Nếu chỉ dùng `compileJava --continuous` thì **chỉ Java** được nhận, còn sửa template `.html` sẽ KHÔNG cập nhật (vì resource không được copy). Vì vậy phải dùng task `classes`.

## Nếu hot reload không tự kích hoạt

Trên một số hệ thống (đặc biệt Windows/WSL2), sự kiện thay đổi file đôi khi không truyền được vào container. Cách xử lý:

- Kiểm tra bạn đang sửa file trong đúng thư mục project đã mount.
- Khởi động lại dev mode: `docker compose -f docker-compose.dev.yml restart app-dev`.
- Hoặc dùng cách thay thế: chỉ chạy DB bằng Docker, chạy app bằng IDE/`./gradlew bootRun` trên máy (IDE tự biên dịch nên DevTools restart ngay).
