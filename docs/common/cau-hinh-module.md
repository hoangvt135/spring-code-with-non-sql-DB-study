# Cấu hình theo module (`config/<module>.properties` + AppConfig)

> **Module:** `common` (`ModuleConfigLoader`, `AppConfig`) · **Bài học nguồn:** [bài 16 Bước 2](../study/16-luot-xem-top-san-pham-redis.md#bước-2--mỗi-module-một-file-properties-đọc-qua-package-common) · **Cập nhật:** 2026-11-10
> **Trạng thái code:** ✅ Hoàn thành — theo code thật ngày 2026-11-10

## Chức năng này làm gì

Cấu hình riêng của app (`app.*`) không nằm trong `application.yml` mà tách thành **mỗi module một file** `src/main/resources/config/<module>.properties`, giống file `.ini` bên PHP.

- **`ModuleConfigLoader`** (`EnvironmentPostProcessor`): lúc khởi động, tự tìm và nạp **mọi** file `config/*.properties` theo **UTF-8** vào `Environment` của Spring, với **độ ưu tiên thấp nhất**.
- **`AppConfig`** (`@Component`): hàm đọc cấu hình dùng chung, giống `config('...')` của PHP: `getString` / `getInt` / `getDuration`. Thiếu key thì ném lỗi ngay.

`application.yml` chỉ giữ cấu hình **của Spring** (DB, Redis, JPA, multipart, log, actuator).

## Đường dẫn & giao diện

Không có — chạy lúc khởi động.

## Luồng xử lý

```
Spring Boot khởi động
  ├─ đọc META-INF/spring.factories → biết có ModuleConfigLoader
  ├─ nạp application.yml, biến môi trường...
  ├─ ModuleConfigLoader.postProcessEnvironment()      (TRƯỚC khi tạo bất kỳ bean nào)
  │    ├─ getResources("classpath*:config/*.properties") → sắp theo tên file
  │    ├─ mỗi file: loadProperties(UTF-8) → addLast(PropertiesPropertySource "config/<tên file>")
  │    │            → log "[Config] đã nạp config/stats.properties (3 key)"
  │    └─ IOException → UncheckedIOException → app KHÔNG khởi động
  └─ tạo bean: FileStorageService(AppConfig), ProductService(..., AppConfig), ProductStatsService(..., AppConfig)
        └─ trong constructor: config.getXxx("app....") → lưu vào field final
              ├─ thiếu key        → IllegalStateException "Required key '...' not found" → app KHÔNG khởi động
              └─ sai kiểu ("abc" cho getInt, "30" hiểu là 30ms cho getDuration) → lỗi chuyển kiểu / giá trị sai

Tìm một key (cao → thấp): tham số dòng lệnh → biến môi trường → application.yml → config/*.properties
```

## File liên quan

| File | Vai trò |
|---|---|
| [`common/ModuleConfigLoader.java`](../../src/main/java/com/example/demo/common/ModuleConfigLoader.java) | Nạp `config/*.properties` (UTF-8, `addLast`). Log bằng `DeferredLogFactory` vì lúc này hệ thống log chưa sẵn sàng |
| [`resources/META-INF/spring.factories`](../../src/main/resources/META-INF/spring.factories) | 1 dòng đăng ký `ModuleConfigLoader`. Thiếu file này thì loader không bao giờ chạy |
| [`common/AppConfig.java`](../../src/main/java/com/example/demo/common/AppConfig.java) | Bọc `Environment.getRequiredProperty`: `getString`, `getInt`, `getDuration` |
| [`resources/config/upload.properties`](../../src/main/resources/config/upload.properties) | `app.upload.dir` → `storage/FileStorageService` |
| [`resources/config/product.properties`](../../src/main/resources/config/product.properties) | `app.product.default-image` → `product/ProductService` |
| [`resources/config/stats.properties`](../../src/main/resources/config/stats.properties) | `app.stats.recent-ttl`, `recent-limit`, `top-limit` → `stats/ProductStatsService` |

## Dữ liệu

Không lưu gì. Các key hiện có:

| Key | Giá trị | Hàm đọc | Ai đọc |
|---|---|---|---|
| `app.upload.dir` | `${UPLOAD_DIR:uploads}` | `getString` | `FileStorageService` |
| `app.product.default-image` | `images/product-default.jpg` (tương đối so với `static/`) | `getString` | `ProductService` |
| `app.stats.recent-ttl` | `30d` | `getDuration` | `ProductStatsService` |
| `app.stats.recent-limit` | `5` | `getInt` | `ProductStatsService` |
| `app.stats.top-limit` | `5` | `getInt` | `ProductStatsService` |

## Cấu hình

**Thêm cấu hình mới cho một module** ([bài 16](../study/16-luot-xem-top-san-pham-redis.md#sau-này-thêm-module-mới-ví-dụ-review-ở-bài-18)), chỉ 2 việc:

1. Thêm dòng vào `config/<module>.properties` (tạo file mới nếu module chưa có), ví dụ `app.review.max-length=1000`.
2. Trong service: nhận `AppConfig config` qua constructor, gọi `this.maxLength = config.getInt("app.review.max-length");` và lưu vào field `final`.

Không phải sửa `application.yml`, `ModuleConfigLoader` hay `spring.factories`.

Cú pháp file `.properties`:

- Mỗi dòng ghi **đủ tiền tố** (`app.stats.top-limit`), không có `[section]`.
- **Không viết dấu nháy**: `"30d"` sẽ đọc ra cả dấu nháy.
- Comment bằng `#`. Placeholder `${BIEN:mac_dinh}` dùng được như trong yml.
- **Thời gian luôn kèm đơn vị** (`ms`, `s`, `m`, `h`, `d`). Số trơn = mili giây.
- Ghi đè tạm không cần sửa file: đặt biến môi trường (xem [bài 16 ③](../study/16-luot-xem-top-san-pham-redis.md#bước-2--mỗi-module-một-file-properties-đọc-qua-package-common)).

## Quy tắc & lưu ý khi sửa

- **Không** dùng `@Value`, **không** dùng `@ConfigurationProperties`, **không** viết cứng con số có thể muốn chỉnh. Đọc qua `AppConfig`, **trong constructor**, lưu vào field `final` (sai tên key là app báo lỗi ngay lúc khởi động).
- Những gì đổi là phải sửa code đi kèm thì **giữ làm hằng số**: tên key Redis (`demo:product:views`), tiền tố URL `/uploads/`.
- Đừng để lại khối `app:` trong `application.yml`: nó **thắng** file trong `config/`, sửa file `.properties` sẽ không thấy tác dụng.
- Hai file cùng một key: file có tên đứng trước theo bảng chữ cái thắng (được `addLast` trước). Tránh trùng bằng cách mỗi module chỉ dùng tiền tố `app.<module>.` của mình.
- Cần kiểu mới (`boolean`, danh sách...) thì thêm hàm vào `AppConfig` cho mọi module dùng chung.
- `ModuleConfigLoader` **không** phải `@Component` và không đổi được sang `@Slf4j` (dùng `DeferredLogFactory`), xem kế hoạch bài 17 trong [ke-hoach.md](../ke-hoach.md).
- Không thấy dòng log `[Config]` nào → kiểm tra `META-INF/spring.factories` (thư mục `META-INF` viết hoa).

> ⚠️ Khác bài học: không có điểm khác về logic. Code thật thiếu các comment giải thích mà bài 16 có (ví dụ ở `addLast`, `getDuration`). `ProductService` đã chuyển sang `AppConfig` (bài 16 Bước 4) nhưng còn sót dòng `import org.springframework.beans.factory.annotation.Value;` không dùng.

## Liên quan

- [Lưu file upload](../storage/luu-file-upload.md): `app.upload.dir`.
- [Quản lý sản phẩm](../product/quan-ly-san-pham.md): `app.product.default-image`.
- [Lượt xem và top sản phẩm](../product/luot-xem-va-top-san-pham.md): `app.stats.*`.
- Sổ tay: [Java có file cấu hình kiểu `.ini` không?](../common_knowledge.md#java-có-file-cấu-hình-kiểu-ini-của-php-key--value-không-có-cần-tự-viết-hàm-đọc-chung-không), [`@ConfigurationProperties` là gì? (cách đã bỏ)](../common_knowledge.md#mỗi-module-một-class-cấu-hình-riêng-statsproperties-productproperties-được-không-configurationproperties-là-gì), [`@Value` có đọc được `Duration` không?](../common_knowledge.md#đưa-hằng-số-ví-dụ-recent_ttl-30-ngày-sang-applicationyml-được-không-value-có-đọc-được-duration-không).

## Lịch sử thay đổi

| Bài | Thay đổi |
|---|---|
| [15](../study/15-anh-va-mo-ta-san-pham.md#bước-1--cấu-hình-trong-applicationyml) | Cấu hình `app.upload.dir`, `app.product.default-image` nằm trong khối `app:` của `application.yml`, đọc bằng `@Value` |
| [16 Bước 2](../study/16-luot-xem-top-san-pham-redis.md#bước-2--mỗi-module-một-file-properties-đọc-qua-package-common) | Tách ra `config/{upload,product,stats}.properties`; thêm `ModuleConfigLoader`, `spring.factories`, `AppConfig`; bỏ khối `app:`; xoá `stats/StatsProperties.java` |
