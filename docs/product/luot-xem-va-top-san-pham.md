# Lượt xem sản phẩm, Top xem nhiều và "Bạn vừa xem"

> **Module:** `product` (code ở package `stats`) · **Bài học nguồn:** [bài 16](../study/16-luot-xem-top-san-pham-redis.md), [bài 17](../study/17-lombok-slf4j-getter-setter.md) · **Cập nhật:** 2026-11-10
> **Trạng thái code:** 🔄 Đang làm (bài 16 Bước 9 — chạy & kiểm tra) — theo code thật ngày 2026-11-10: code của Bước 1–8 đã có đủ, Bước 9 chưa xác nhận đã chạy thử

## Chức năng này làm gì

- Mỗi lần mở **trang chi tiết** `GET /products/{id}` là **+1 lượt xem** cho sản phẩm đó.
- Trang danh sách có cột **Lượt xem** và khung **🔥 Xem nhiều nhất** (top N sản phẩm, xếp từ cao xuống thấp).
- Trang chi tiết có mục **"Bạn vừa xem"**: các sản phẩm *user đang đăng nhập* vừa mở, mới nhất đứng đầu, không tính sản phẩm đang xem. Không xem gì trong `recent-ttl` (30 ngày) thì danh sách tự biến mất.
- Dữ liệu nằm trong **Redis**, không thêm cột nào vào bảng `product`. Lý do chọn Redis thay vì cột `views`: xem [bài 16 — Tư vấn](../study/16-luot-xem-top-san-pham-redis.md#tư-vấn-trước-khi-code-sao-không-thêm-cột-views-vào-bảng-product).
- Redis chỉ giữ **dữ liệu phụ**: mất hay lệch một chút cũng được. **Redis chết thì trang vẫn chạy**, lượt xem hiện `0` ([Quy tắc của bài](../study/16-luot-xem-top-san-pham-redis.md#quy-tắc-của-bài-này)).
- Chức năng **không đổi luồng lưu sản phẩm** (thêm/sửa). Nó chỉ thêm việc dọn Redis khi xoá sản phẩm.

## Đường dẫn & giao diện

| Method | URL | Controller#method | Mô tả |
|---|---|---|---|
| GET | `/products` | `ProductViewController#list` | Danh sách + cột Lượt xem + khung Top (template `products/list`) |
| GET | `/products/{id}` | `ProductViewController#detail` | Trang chi tiết, **ghi 1 lượt xem**, hiện "Bạn vừa xem" (template `products/detail`) |
| GET | `/products/{id}/delete` | `ProductViewController#delete` | Xoá sản phẩm, gọi `productStatsService.removeProduct(id)` qua `ProductService.delete` |
| DELETE | `/api/products/{id}` | `ProductController#delete` | REST, cũng đi qua `ProductService.delete` nên cũng dọn Redis |

- Mọi đường dẫn trên đều bắt đăng nhập (`anyRequest().authenticated()`), nên `Principal` trong `detail()` không bao giờ `null`.
- `/products/new` **không** bị `/{id}` "nuốt": đường dẫn cố định được ưu tiên ([Bước 5](../study/16-luot-xem-top-san-pham-redis.md#bước-5--productviewcontroller-trang-chi-tiết)).
- **Không** tính lượt xem: trang Sửa `/products/{id}/edit`, REST `GET /api/products/{id}`.
- Giao diện: [Bước 6 `detail.html`](../study/16-luot-xem-top-san-pham-redis.md#bước-6--detailhtml-trang-chi-tiết), [Bước 7 `list.html`](../study/16-luot-xem-top-san-pham-redis.md#bước-7--listhtml-khung-top-cột-lượt-xem-link-chi-tiết), [Bước 8 CSS](../study/16-luot-xem-top-san-pham-redis.md#bước-8--css).

## Luồng xử lý

Chi tiết từng lệnh: [bài 16 — Luồng xử lý](../study/16-luot-xem-top-san-pham-redis.md#luồng-xử-lý).

```
GET /products/{id}   (ProductViewController.detail)
  ├─ 1) productService.findById(id)              ──▶ MySQL  SELECT ... WHERE id = ?
  │       └─ không có → NotFoundException → 404 (DỪNG, không đếm gì vào Redis)
  ├─ 2) productStatsService.recordView(id, user) ──▶ Redis  ZINCRBY demo:product:views 1 "<id>"
  │       │                                                 LREM / LPUSH / LTRIM / EXPIRE demo:recent:<user>
  │       └─ Redis lỗi (DataAccessException) → log.warn, trả 0
  ├─ 3) productService.recentlyViewed(user)
  │       ├─ productStatsService.recentProductIds()  ──▶ Redis  LRANGE demo:recent:<user> 0 -1
  │       │     └─ Redis lỗi → log.warn, List rỗng
  │       └─ findAllInOrder(ids)                     ──▶ MySQL  SELECT ... WHERE id IN (...)
  │             └─ id không còn trong MySQL → bỏ qua (dọn lười)
  ├─ 4) recent.removeIf(p là sản phẩm đang xem)
  └─ 5) model: product, views, recent → "products/detail"

GET /products   (ProductViewController.list)
  ├─ productService.findAll()                    ──▶ MySQL
  ├─ productStatsService.allViews()              ──▶ Redis  ZRANGE demo:product:views 0 -1 WITHSCORES
  │       └─ Redis lỗi → Map rỗng → cột Lượt xem toàn 0
  └─ productService.topViewed()                  ──▶ Redis  ZREVRANGE demo:product:views 0 <top-limit - 1>
          │                                       ──▶ MySQL  WHERE id IN (...), xếp lại đúng thứ tự Redis
          └─ Redis lỗi → List rỗng → ẩn khung Top

ProductService.delete(id)
  ├─ productRepository.delete(product)           ──▶ MySQL
  ├─ fileStorageService.delete(imagePath)        ──▶ thư mục uploads/
  └─ productStatsService.removeProduct(id)       ──▶ Redis  ZREM demo:product:views "<id>"
          └─ Redis lỗi → log.warn, MySQL vẫn đã xoá (không rollback)
```

Redis chết hẳn: mỗi lệnh chờ tối đa `timeout: 2s` rồi mới báo lỗi, nên trang danh sách chậm khoảng 4 giây (2 lệnh) chứ không treo ([Bước 1](../study/16-luot-xem-top-san-pham-redis.md#bước-1--applicationyml-timeout-cho-redis)).

## File liên quan

| File | Vai trò | Trạng thái |
|---|---|---|
| `src/main/java/com/example/demo/stats/ProductStatsService.java` | Class **duy nhất** làm việc với Redis cho lượt xem. Chỉ biết id, không biết `Product`/MySQL. Đủ 5 hàm: `recordView`, `allViews`, `topProductIds`, `recentProductIds`, `removeProduct`, mỗi hàm có `try/catch (DataAccessException)` | ✅ |
| `src/main/java/com/example/demo/product/ProductService.java` | Ghép id từ Redis thành `Product`: `topViewed()`, `recentlyViewed(username)`, `findAllInOrder(ids)` (private). `delete()` gọi `removeProduct()` | ✅ (còn sót import `@Value`, xem cuối mục) |
| `src/main/java/com/example/demo/product/ProductViewController.java` | `list()` đưa `views` + `topProducts` ra template. `detail()` mới | ✅ |
| `src/main/resources/templates/products/detail.html` | Trang chi tiết: ảnh, giá, danh mục, 👁 lượt xem, mô tả, "Bạn vừa xem" | ✅ |
| `src/main/resources/templates/products/list.html` | Khung Top, cột Lượt xem (`views.getOrDefault(p.id, 0)`), tên là link sang chi tiết, `colspan="6"` | ✅ |
| `src/main/resources/static/css/app.css` | `.top-box`, `.views`, `.detail`, `.price`, `.description`, `.recent` | ✅ |
| `src/main/resources/config/stats.properties` | 3 key `app.stats.*` | ✅ |
| `src/main/resources/application.yml` | `spring.data.redis.timeout` / `connect-timeout: 2s` | ✅ |
| `common/AppConfig.java`, `common/ModuleConfigLoader.java`, `META-INF/spring.factories` | Nạp và đọc `config/*.properties`. Xem [cấu hình module](../common/cau-hinh-module.md) | ✅ |
| `docker-compose.dev.yml` (service `redis`, `REDIS_HOST: redis`) | Redis 7.4, cổng `6380` → 6379, volume `redis_data` | ✅ |
| `docker-compose.yml` (chế độ thường) | Chưa có service `redis` → chạy chế độ này thì lượt xem luôn `0` (log `[Stats] Redis lỗi`) | ⬜ |
| Bước 9 — chạy & kiểm tra (`redis-cli`, tắt Redis, đổi cấu hình) | App đã khởi động lại với code mới (log có `[Stats] recent-ttl=PT720H, recent-limit=5, top-limit=5`), chưa xác nhận đã thử đủ 10 mục | 🔄 |
| Bài tập 1–8 (chống F5, `GET /api/products/{id}/views`, top 7 ngày, nút xoá lịch sử...) | Chưa làm | ⬜ |

Khác với bài học (không ảnh hưởng chức năng):

- `ProductService` còn dòng `import org.springframework.beans.factory.annotation.Value;` không dùng nữa. Bài 16 Bước 4 ① bảo xoá dòng này.
- `ProductService.productRepository` khai báo thiếu `final` (bài ghi `private final ProductRepository productRepository;`).
- `ProductStatsService` không có các dòng comment giải thích như trong bài. Logic giống hệt.

## Dữ liệu

| Redis key | Kiểu | TTL | Ý nghĩa |
|---|---|---|---|
| `demo:product:views` | Sorted Set (ZSet) | Không hết hạn | member = id sản phẩm (chuỗi), score = tổng lượt xem. Redis tự giữ thứ tự theo score → lấy Top bằng `ZREVRANGE` |
| `demo:recent:<username>` | List | `app.stats.recent-ttl` (30 ngày), **đặt lại mỗi lần xem** | id sản phẩm user vừa xem, mới nhất bên trái, tối đa `recent-limit` phần tử. Mỗi user một key |

- Tên key là **hằng số** trong `ProductStatsService` (`VIEWS_KEY`, `RECENT_KEY_PREFIX`), cố ý không đưa ra cấu hình: đổi tên key là mất dấu dữ liệu cũ. Quy ước đặt tên: [Đặt tên key](../study/16-luot-xem-top-san-pham-redis.md#đặt-tên-key).
- **Liên kết với MySQL qua `productId`** (bảng `product`). Redis chỉ giữ id, tên/giá/ảnh luôn lấy từ MySQL, nên sửa sản phẩm thì Top hiện ngay tên mới.
- **Không có transaction chung** giữa MySQL và Redis. Xoá sản phẩm thì tự `ZREM` khỏi bảng xếp hạng. Riêng các List `demo:recent:*` thì **dọn lười**: id cũ nằm lại, lúc đọc `findAllInOrder()` bỏ qua, hết TTL thì key tự mất ([Bước 4](../study/16-luot-xem-top-san-pham-redis.md#bước-4--productservice-ghép-id-từ-redis-thành-sản-phẩm)).
- Độ bền: Redis chụp RDB xuống volume `redis_data`. `docker restart demo-redis` không mất, `down -v` thì mất sạch. Sập giữa 2 lần chụp có thể mất vài lượt xem gần nhất ([Đánh đổi](../study/16-luot-xem-top-san-pham-redis.md#đánh-đổi-redis-có-làm-mất-dữ-liệu-không)).
- Seeder **không** tạo lượt xem mẫu. Reset MySQL không xoá Redis, nên id cũ trong ZSet có thể trỏ vào sản phẩm không còn (bị bỏ qua khi hiện Top). Xem [dữ liệu mẫu](../seed/du-lieu-mau.md).
- Xem dữ liệu thật: `docker exec -it demo-redis redis-cli`, rồi `ZREVRANGE demo:product:views 0 -1 WITHSCORES`, `LRANGE demo:recent:admin 0 -1`, `TTL demo:recent:admin`.

## Cấu hình

File `src/main/resources/config/stats.properties`, đọc **một lần** trong constructor `ProductStatsService` qua `AppConfig`:

| Key | Giá trị hiện tại | Đọc bằng | Ý nghĩa |
|---|---|---|---|
| `app.stats.recent-ttl` | `30d` | `getDuration` | "Bạn vừa xem": bao lâu không xem gì thì key tự hết hạn |
| `app.stats.recent-limit` | `5` | `getInt` | "Bạn vừa xem": giữ tối đa mấy sản phẩm (`LTRIM 0 recentLimit-1`) |
| `app.stats.top-limit` | `5` | `getInt` | Khung "Xem nhiều nhất": hiện mấy sản phẩm |

- Thời gian ghi **liền đơn vị**, không dấu nháy (`30d`, `5m`). Ghi `30` không đơn vị là 30 **mili giây**.
- Thiếu key hoặc sai định dạng → app **không khởi động** (`getRequiredProperty`). Biến môi trường (ví dụ `APP_STATS_TOPLIMIT`) thắng file `config/`.
- Timeout Redis nằm ở `application.yml` (`spring.data.redis.timeout`, `connect-timeout`), **không phải** `spring.redis`.
- Cơ chế nạp file: [cấu hình module](../common/cau-hinh-module.md) và [bài 16 Bước 2](../study/16-luot-xem-top-san-pham-redis.md#bước-2--mỗi-module-một-file-properties-đọc-qua-package-common).

## Quy tắc & lưu ý khi sửa

- **Chỉ `ProductStatsService` được gọi Redis** cho lượt xem. Đổi nơi lưu (ví dụ sang bảng MySQL) thì chỉ viết lại class này.
- **Chiều phụ thuộc một hướng**: `ProductService` → `ProductStatsService`. `ProductStatsService` chỉ nhận `StringRedisTemplate` + `AppConfig`; cho nó nhận `ProductService`/`ProductRepository` sẽ lỗi vòng phụ thuộc (*beans form a cycle*).
- **Mọi hàm public** trong `ProductStatsService` phải `try/catch (DataAccessException e)` và trả giá trị rỗng (`0`, Map/List rỗng). Không bắt `Exception` chung, để lỗi do code sai vẫn lộ ra.
- Trong `detail()`: **tìm MySQL trước, đếm sau**, nếu không id rác (`/products/99999`) sẽ thành lượt xem.
- Đếm bằng `incrementScore` (`ZINCRBY`, atomic), **không** `get` rồi `set` (mất lượt khi 2 người xem cùng lúc).
- `findAllInOrder()` phải đi theo thứ tự danh sách id, không trả thẳng kết quả `findAllById` (MySQL không giữ thứ tự `IN (...)`).
- Tránh **N+1**: không gọi Redis cho từng dòng trong template. Lấy hết bằng `allViews()` một lệnh, rồi tra Map.
- Thêm số mới cho module (ví dụ cooldown của bài tập 1) thì thêm vào `config/stats.properties` và đọc bằng `AppConfig`. Không viết cứng, không `@Value`.
- Thêm nơi xoá sản phẩm mới thì phải đi qua `ProductService.delete()` để Redis được dọn.
- `recordView()` gửi 5 lệnh riêng lẻ, không atomic với nhau. Chấp nhận được với dữ liệu phụ (bài tập 6).
- Lỗi hay gặp: [bài 16 — Lỗi thường gặp](../study/16-luot-xem-top-san-pham-redis.md#lỗi-thường-gặp).

## Liên quan

- [Quản lý sản phẩm](quan-ly-san-pham.md): CRUD, ảnh, luồng lưu sản phẩm (`ProductService.save`, `delete`).
- [Cấu hình module](../common/cau-hinh-module.md): `AppConfig`, `ModuleConfigLoader`, `config/*.properties`.
- [Kiểm tra kết nối Mongo/Redis](../nosql/kiem-tra-ket-noi-mongo-redis.md): `NoSqlConnectionCheck`, key `demo:last-startup`.
- [Dữ liệu mẫu](../seed/du-lieu-mau.md): seeder, reset dữ liệu.
- Bài học: [bài 14 — Môi trường MongoDB + Redis](../study/14-moi-truong-mongodb-redis.md) (nền), [bài 16](../study/16-luot-xem-top-san-pham-redis.md) (chức năng này), [bài 12](../study/12-dang-nhap-spring-security.md) (`Principal`, đăng nhập).

## Lịch sử thay đổi

| Bài | Thay đổi |
|---|---|
| [14](../study/14-moi-truong-mongodb-redis.md) | Thêm service `redis` vào `docker-compose.dev.yml`, `spring-boot-starter-data-redis`, cấu hình `spring.data.redis` |
| [16](../study/16-luot-xem-top-san-pham-redis.md) | Tạo chức năng: timeout Redis (Bước 1), `config/stats.properties` + `AppConfig` (Bước 2), `ProductStatsService` (Bước 3), `topViewed` / `recentlyViewed` / dọn Redis khi xoá (Bước 4), `detail()` (Bước 5), `detail.html` (Bước 6), khung Top + cột Lượt xem (Bước 7), CSS (Bước 8) |
| [17](../study/17-lombok-slf4j-getter-setter.md) | ⬜ Chưa làm. `ProductStatsService` dùng `@Slf4j` thay dòng khai báo `Logger`. Không đổi logic |
