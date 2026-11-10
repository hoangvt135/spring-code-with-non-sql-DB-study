# Lưu file upload (FileStorageService)

> **Module:** `storage` (+ `config/WebConfig`) · **Bài học nguồn:** [bài 15](../study/15-anh-va-mo-ta-san-pham.md), [bài 16 Bước 2](../study/16-luot-xem-top-san-pham-redis.md#bước-2--mỗi-module-một-file-properties-đọc-qua-package-common), [bài 17](../study/17-lombok-slf4j-getter-setter.md) · **Cập nhật:** 2026-11-10
> **Trạng thái code:** ✅ Hoàn thành phần Java · 🔄 Bài 15 Bước 11 chưa xong (chế độ thường chưa có volume cho ảnh) — theo code thật ngày 2026-11-10

## Chức năng này làm gì

`FileStorageService` là **class duy nhất biết file upload nằm ở đâu**. Nó:

- tạo thư mục upload lúc khởi động (nếu chưa có);
- lưu file người dùng gửi lên với **tên mới ngẫu nhiên** (UUID), trả về tên file để ghi vào DB;
- xoá file theo tên, không bao giờ cho xoá ra ngoài thư mục upload.

`WebConfig` map URL `/uploads/**` tới thư mục đó, để trình duyệt xem được ảnh mà không cần controller. Muốn đổi cách lưu (S3, MinIO, GridFS...) thì chỉ viết lại class này.

## Đường dẫn & giao diện

| URL | Ai xử lý | Ghi chú |
|---|---|---|
| `GET /uploads/<tên file>` | `WebConfig` (resource handler, không qua controller) | `permitAll()` trong `SecurityConfig`, xem được khi chưa đăng nhập |

Không có trang riêng. Form upload là `products/form.html` (ô `imageFile`), thuộc [Quản lý sản phẩm](../product/quan-ly-san-pham.md).

## Luồng xử lý

```
Khởi động: FileStorageService(AppConfig)
  app.upload.dir ("uploads") → toAbsolutePath().normalize() → /app/uploads
  └─ Files.createDirectories(root)   lỗi → RuntimeException → app KHÔNG khởi động
WebConfig.addResourceHandlers: "/uploads/**" → "file:/app/uploads/"

save(file)   (gọi từ ProductService.save)
  ├─ file null hoặc rỗng                       → return null (giữ ảnh cũ)
  ├─ đuôi không thuộc jpg/jpeg/png/gif/webp    → IllegalArgumentException → trang lỗi 500
  ├─ tên mới = UUID + "." + đuôi viết thường → resolve() → transferTo()
  │     └─ IOException                         → RuntimeException("lưu file thất bại...")
  └─ return tên file mới (ghi vào product.image_path)

delete(filename)   (khi đổi ảnh / xoá sản phẩm)
  ├─ null hoặc rỗng → bỏ qua
  └─ Files.deleteIfExists(resolve(filename))
        └─ IOException → chỉ log.warn, KHÔNG ném lỗi (không làm hỏng việc chính)

resolve(filename): root.resolve(filename).normalize()
  └─ không nằm trong root (vd "../../app.jar") → IllegalArgumentException

File > 5MB (hoặc cả form > 6MB) → Tomcat từ chối trước khi tới code → thường thấy 403 (mất CSRF token)
```

## File liên quan

| File | Vai trò |
|---|---|
| [`storage/FileStorageService.java`](../../src/main/java/com/example/demo/storage/FileStorageService.java) | `save()`, `delete()`, `getRoot()`, `resolve()` (private), hằng `URL_PREFIX = "/uploads/"` |
| [`config/WebConfig.java`](../../src/main/java/com/example/demo/config/WebConfig.java) | `WebMvcConfigurer`: map `URL_PREFIX + "**"` → `"file:" + getRoot() + "/"` |
| `config/SecurityConfig.java` | Cho phép `FileStorageService.URL_PREFIX + "**"` không cần đăng nhập |
| `product/ProductService.java` | Nơi duy nhất gọi `save()` / `delete()`; tính URL ảnh bằng `URL_PREFIX` |
| [`resources/config/upload.properties`](../../src/main/resources/config/upload.properties) | `app.upload.dir` |
| [`resources/application.yml`](../../src/main/resources/application.yml) | `spring.servlet.multipart.*`: giới hạn kích thước |
| `docker-compose.dev.yml`, `docker-compose.yml`, `.gitignore`, `.dockerignore` | Thư mục `uploads/` khi chạy Docker và khi commit |

## Dữ liệu

- **Ổ đĩa:** file nằm phẳng trong thư mục upload, tên dạng `3f2a9c1e-....jpg` (đuôi viết thường).
- **MySQL:** chỉ lưu **tên file** ở cột `product.image_path` (không lưu đường dẫn, không lưu URL). `null` → hiện ảnh mặc định `static/images/product-default.jpg`.
- Không có transaction chung giữa ổ đĩa và MySQL: file và dòng DB có thể lệch nhau (file mồ côi, xem [bài 15 bài tập 5](../study/15-anh-va-mo-ta-san-pham.md#bài-tập)).

## Cấu hình

| Key / thiết lập | File | Giá trị |
|---|---|---|
| `app.upload.dir` | `config/upload.properties` | `${UPLOAD_DIR:uploads}`: tương đối thì tính từ thư mục đang chạy app |
| `spring.servlet.multipart.max-file-size` | `application.yml` | `5MB` (1 file) |
| `spring.servlet.multipart.max-request-size` | `application.yml` | `6MB` (cả form) |
| Đuôi file cho phép | hằng `ALLOWED_EXTENSION` trong code | `jpg, jpeg, png, gif, webp` (cố ý không có `svg`) |

`uploads` thực sự nằm ở đâu ([bảng đầy đủ](../study/15-anh-va-mo-ta-san-pham.md#bước-11--docker-và-git-không-làm-mất-không-commit-nhầm-ảnh)):

| Cách chạy | Thư mục | Trạng thái |
|---|---|---|
| `docker-compose.dev.yml` | `/app/uploads` = `<project>/uploads` trên máy (project được mount) | ✅ Không cần sửa |
| `docker-compose.yml` | `/app/uploads` **trong container** | ⚠️ Chưa có volume: build lại là mất ảnh |
| `./gradlew bootRun` | `<project>/uploads` | ✅ |

> ⚠️ Khác bài học: bài 15 Bước 11 thêm volume `uploads_data:/app/uploads` vào service `app` của `docker-compose.yml` và thêm `uploads/` vào `.dockerignore`. Code thật **chưa có cả hai** (`.gitignore` thì đã có `uploads/`).
>
> ⚠️ Khác bài học: bài 15 đọc cấu hình bằng `@Value("${app.upload.dir}")` trong `application.yml`. Code thật đã theo bài 16 Bước 2: đọc qua `AppConfig` từ `config/upload.properties`, `application.yml` không còn khối `app:`.
>
> ⚠️ Khác bài học: hằng trong code tên `ALLOWED_EXTENSION` (bài học: `ALLOWED_EXTENSIONS`). Message lỗi cũng khác: code báo `"ảnh không được hỗ trợ, chỉ nhận JPG, JPEG, PNG, GIF, WEBP"` và không kèm tên file gốc.

## Quy tắc & lưu ý khi sửa

- **Chỉ class này biết đường dẫn thật.** Nơi khác chỉ cầm tên file, hoặc dùng `FileStorageService.URL_PREFIX` (không gõ tay `"/uploads/"`).
- Không lưu ảnh upload vào `src/main/resources/static/` ([lý do](../study/15-anh-va-mo-ta-san-pham.md#-vì-sao-không-lưu-ảnh-upload-vào-srcmainresourcesstatic)).
- Không dùng tên file gốc của người dùng. Mọi đường dẫn từ tên file phải đi qua `resolve()`.
- `addResourceLocations` **bắt buộc** có `/` ở cuối, thiếu là 404 hết.
- Thêm đuôi mới vào danh sách cho phép thì cân nhắc an toàn (SVG chứa được JavaScript).
- Ô file trong form tên `imageFile`, **khác** trường `imagePath` của `Product`, là cố ý.
- `delete()` cố ý nuốt lỗi (chỉ log), còn `save()` thì ném lỗi: lưu hỏng thì không được ghi DB.
- Reset dữ liệu bằng `script/reset-db.sh` sẽ `rm -f uploads/*`. `down -v` ở chế độ dev **không** xoá `uploads/` (đó là thư mục trên máy, không phải volume).

## Liên quan

- [Quản lý sản phẩm](../product/quan-ly-san-pham.md): `ProductService.save()` / `delete()` / `imageUrl()`.
- [Cấu hình theo module](../common/cau-hinh-module.md): `AppConfig`, `config/upload.properties`.
- [Đăng nhập & đăng ký](../user/dang-nhap-dang-ky.md): `SecurityConfig` mở quyền `/uploads/**`.
- [Dữ liệu mẫu & reset](../seed/du-lieu-mau.md): script reset xoá ảnh.
- Sổ tay: [`catch (IOException e)` và `catch (Exception e)`](../kien-thuc/java-ngoai-le.md#catch-ioexception-e-và-catch-exception-e-khác-nhau-thế-nào), [lưu ảnh vào DB hay ra file](../cong-nghe/mongodb.md#blob-là-gì-document-của-mongodb-có-phải-là-tài-liệu-lưu-ảnh-vào-db-hay-ra-file).

## Lịch sử thay đổi

| Bài | Thay đổi |
|---|---|
| [15](../study/15-anh-va-mo-ta-san-pham.md) | Tạo `FileStorageService`, `WebConfig`; mở quyền `/uploads/**`; giới hạn multipart 5MB/6MB; `uploads/` vào `.gitignore` (Bước 11 phần Docker chưa làm) |
| [16](../study/16-luot-xem-top-san-pham-redis.md#bước-2--mỗi-module-một-file-properties-đọc-qua-package-common) | Bỏ `@Value`, đọc `app.upload.dir` qua `AppConfig` từ `config/upload.properties` |
| [17](../study/17-lombok-slf4j-getter-setter.md) | ⬜ Chưa làm. `FileStorageService` dùng `@Slf4j` thay dòng khai báo `Logger`. Bài tập 1: thay `getRoot()` viết tay bằng `@Getter` trên trường `root` |
