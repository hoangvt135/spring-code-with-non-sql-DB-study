# 16 — Lượt xem và "Top xem nhiều" bằng Redis

Bài 14 mới dựng Redis lên và thử ghi/đọc một key. Bài này dùng Redis cho một tính năng thật:

- **Trang chi tiết sản phẩm** `GET /products/{id}`. Mỗi lần mở trang là **1 lượt xem**.
- **Cột "Lượt xem"** trong trang danh sách.
- **Khung "🔥 Xem nhiều nhất"**: 5 sản phẩm có nhiều lượt xem nhất, xếp từ cao xuống thấp.
- **"Bạn vừa xem"**: 5 sản phẩm mà *chính user đang đăng nhập* vừa mở, mới nhất đứng đầu. Sau 30 ngày không xem gì thì danh sách này tự biến mất.
- **Không viết cứng con số nào trong code**: 30 ngày, 5 sản phẩm "vừa xem", 5 sản phẩm Top đều nằm trong file cấu hình. **Mỗi module một file `.properties`** (`config/stats.properties`, `config/product.properties`, `config/upload.properties`), giống file `.ini` bên PHP, và mọi module đọc qua một class dùng chung `common/AppConfig`.

Mục tiêu khi xong bài:

- Mở chi tiết một sản phẩm vài lần: số lượt xem tăng, sản phẩm đó lên khung Top.
- `redis-cli` cho thấy dữ liệu thật: một **Sorted Set** chứa lượt xem và một **List** "vừa xem" cho mỗi user.
- Xoá sản phẩm thì sản phẩm đó cũng biến khỏi bảng xếp hạng trong Redis.
- **Tắt Redis đi, trang sản phẩm vẫn chạy**, chỉ là lượt xem hiện `0`.

> Kiến thức mới: các kiểu dữ liệu của Redis, Sorted Set (`ZINCRBY`, `ZREVRANGE`), List (`LPUSH`, `LTRIM`, `LRANGE`), TTL (`EXPIRE`), cách đặt tên key, `Principal` (lấy tên user đang đăng nhập), ghép dữ liệu từ 2 DB, xử lý khi DB phụ bị chết, cấu hình timeout, file `.properties` cho từng module, `Environment` và `EnvironmentPostProcessor` (tự nạp mọi file cấu hình của module lúc khởi động).

---

## Tư vấn trước khi code: sao không thêm cột `views` vào bảng `product`?

Nếu chỉ quen MySQL, cách nghĩ đầu tiên sẽ là:

```sql
ALTER TABLE product ADD views INT DEFAULT 0;
UPDATE product SET views = views + 1 WHERE id = 12;               -- mỗi lần xem
SELECT * FROM product ORDER BY views DESC LIMIT 5;                -- top 5
```

Cách này **chạy được**, và với project nhỏ như thế này thì cũng không sao. Nhưng nó có mấy nhược điểm, và các nhược điểm đó lộ ra ngay khi lượng truy cập tăng:

| | Cột `views` trong MySQL | Redis |
|---|---|---|
| Mỗi lượt xem | 1 câu `UPDATE`: khoá dòng, ghi xuống đĩa, ghi log transaction | 1 lệnh `ZINCRBY` trong RAM, mất vài micro giây |
| 100 người cùng xem 1 sản phẩm | 100 câu `UPDATE` phải **xếp hàng** chờ khoá cùng một dòng | Redis xử lý tuần tự cực nhanh, không có khoá dòng |
| Top 5 | `ORDER BY views` phải duyệt cả bảng, hoặc phải có index mà index thì bị sửa liên tục | Sorted Set **luôn giữ sẵn thứ tự**, lấy top 5 gần như tức thì |
| Lẫn với dữ liệu chính | Bảng `product` bị ghi liên tục chỉ vì có người *xem* | Tách hẳn ra. MySQL chỉ bị ghi khi có người *sửa* sản phẩm |
| Danh sách "vừa xem" của từng user | Phải tạo thêm bảng, tự xoá bản ghi cũ, tự dọn khi hết hạn | List + `LTRIM` + `EXPIRE`: Redis tự lo hết |

### Đánh đổi: Redis có làm mất dữ liệu không?

Redis giữ dữ liệu **trong RAM**. Ở bài 14, ta đã gắn volume `redis_data:/data`, và Redis **định kỳ chụp** dữ liệu xuống đó (gọi là *RDB snapshot*). Nhờ vậy restart container thì dữ liệu vẫn còn. Nhưng nếu Redis bị sập **giữa hai lần chụp** thì vài phút lượt xem gần nhất sẽ mất.

- Với **lượt xem**: mất vài lượt chẳng ai để ý, nên **chấp nhận được**.
- Với **đơn hàng, tiền, tồn kho**: không bao giờ chấp nhận được. Những thứ đó phải nằm ở MySQL.

### Quy tắc của bài này

**MySQL vẫn là nơi giữ dữ liệu chính. Redis chỉ giữ dữ liệu phụ: mất cũng được, sai lệch một chút cũng được.** Quy tắc này kéo theo 2 việc mà code phải làm:

1. **Redis chết thì trang không được chết theo.** Lượt xem không phải lý do để khách không xem được sản phẩm (Bước 3, `try/catch`).
2. **Hai DB không có transaction chung.** Xoá sản phẩm ở MySQL thì Redis không tự biết, nên ta phải tự dọn, giống hệt cách bài 15 tự xoá file ảnh (Bước 4).

---

## Ý tưởng

### Redis không chỉ là "key → chuỗi"

Ở bài 14 ta mới dùng kiểu đơn giản nhất: `SET key value`. Thật ra **value** trong Redis có nhiều kiểu, mỗi kiểu có bộ lệnh riêng:

| Kiểu | Giống cái gì trong Java | Lệnh hay dùng | Dùng cho |
|---|---|---|---|
| **String** | `String` / số | `SET`, `GET`, `INCR`, `SET ... NX EX` | Bộ đếm, cache, khoá tạm (bài tập 1) |
| **List** | `LinkedList<String>` | `LPUSH`, `LRANGE`, `LTRIM`, `LREM` | ⬅️ "Vừa xem", hàng đợi, log gần nhất |
| **Set** | `HashSet<String>` | `SADD`, `SISMEMBER` | Ai đã like, tag |
| **Sorted Set** (ZSet) | `TreeMap<String, Double>` **tự xếp theo value** | `ZINCRBY`, `ZREVRANGE`, `ZSCORE`, `ZREM` | ⬅️ **Bảng xếp hạng**, lượt xem |
| **Hash** | `Map<String, String>` | `HSET`, `HGET`, `HGETALL` | Lưu một object nhiều trường |

**Sorted Set** là mấu chốt của bài này. Mỗi phần tử gồm một **member** (ở đây là id sản phẩm) và một **score** (số lượt xem). Redis **luôn giữ các member theo thứ tự score**. Vì vậy chỉ một kiểu dữ liệu đã làm được cả hai việc: đếm lượt xem của từng sản phẩm, và lấy ra top 5.

```
demo:product:views   (Sorted Set)            demo:recent:admin   (List)
┌────────┬───────┐                           ┌───┬───┬───┬───┬───┐
│ member │ score │                           │ 3 │ 1 │ 7 │ 2 │ 9 │   ← mới nhất ở bên trái
├────────┼───────┤                           └───┴───┴───┴───┴───┘
│ "2"    │   1   │  ← thấp                     LPUSH chèn vào bên trái
│ "7"    │   4   │                             LTRIM 0 4 cắt bỏ phần thừa bên phải
│ "3"    │  12   │  ← cao: ZREVRANGE lấy từ đây
└────────┴───────┘
```

### Đặt tên key

Redis không có "bảng" hay "database" theo tên như MySQL. Mọi key nằm chung một chỗ. Vì thế người ta quy ước tên key bằng dấu `:` cho dễ đọc và không bị trùng:

```
demo:product:views      ← tên app : đối tượng : thông tin
demo:recent:admin       ← "vừa xem" của user admin; mỗi user một key
```

Tiền tố `demo:` (giống `demo:last-startup` ở bài 14) giúp phân biệt key của app này nếu Redis được dùng chung với app khác. Các công cụ như RedisInsight cũng dựa vào dấu `:` để hiện key thành dạng cây thư mục.

### Luồng xử lý

```
GET /products/3
  ProductViewController.detail()
   ├─ productService.findById(3)                    ──▶ MySQL   SELECT ... WHERE id = 3   (không có → 404)
   ├─ productStatsService.recordView(3, "admin")    ──▶ Redis   ZINCRBY demo:product:views 1 "3"
   │                                                            LREM / LPUSH / LTRIM / EXPIRE demo:recent:admin
   └─ productService.recentlyViewed("admin")
        ├─ productStatsService.recentProductIds()   ──▶ Redis   LRANGE demo:recent:admin 0 -1  → [3, 1, 7]
        └─ productRepository.findAllById([3,1,7])   ──▶ MySQL   SELECT ... WHERE id IN (3, 1, 7)

GET /products
  ├─ productService.findAll()                       ──▶ MySQL
  ├─ productStatsService.allViews()                 ──▶ Redis   ZRANGE demo:product:views 0 -1 WITHSCORES
  └─ productService.topViewed(5)                    ──▶ Redis   ZREVRANGE ... 0 4   →  MySQL  WHERE id IN (...)
```

Redis chỉ giữ **id**. Tên, giá, ảnh vẫn lấy từ MySQL. Nhờ vậy sửa tên sản phẩm thì khung Top hiện ngay tên mới, không bao giờ bị lệch.

### Ai biết gì

Ta làm theo đúng cách bài 15 đã làm với `FileStorageService`:

| Class | Biết gì | Không biết gì |
|---|---|---|
| `ProductStatsService` (MỚI) | Redis: tên key, kiểu dữ liệu, lệnh nào | `Product`, MySQL. Nó chỉ làm việc với **id** |
| `config/stats.properties` (MỚI) | Các con số của module stats: hạn "vừa xem", giữ mấy sản phẩm, Top mấy sản phẩm | Code Java. Đây chỉ là file giá trị |
| `common/AppConfig` (MỚI) | Cách đọc một key cấu hình và đổi nó sang `String`, `int`, `Duration` | Key đó nằm ở file nào, thuộc module nào |
| `ProductService` | MySQL + **ghép** id từ Redis thành `Product` | Lệnh Redis cụ thể |
| `ProductViewController` | Gọi 2 service, đưa dữ liệu ra template | Cả Redis lẫn MySQL |

Sau này muốn chuyển lượt xem sang chỗ khác, ví dụ một bảng MySQL riêng, thì chỉ phải viết lại `ProductStatsService`.

### Các file sẽ đụng tới

```
src/main/resources/
├── application.yml                     ⬅️ SỬA — timeout cho Redis; BỎ khối app: (chuyển sang config/)
├── config/
│   ├── stats.properties                ⬅️ MỚI — cấu hình module stats (app.stats.*)
│   ├── product.properties              ⬅️ MỚI — cấu hình module product (app.product.*, chuyển từ yml)
│   └── upload.properties               ⬅️ MỚI — cấu hình module storage (app.upload.*, chuyển từ yml)
├── META-INF/spring.factories           ⬅️ MỚI — đăng ký ModuleConfigLoader
├── templates/products/detail.html      ⬅️ MỚI — trang chi tiết
├── templates/products/list.html        ⬅️ SỬA — khung Top, cột Lượt xem, link tới chi tiết
└── static/css/app.css                  ⬅️ SỬA — style mới
com/example/demo/
├── common/
│   ├── ModuleConfigLoader.java     ⬅️ MỚI — tự nạp mọi file config/*.properties lúc khởi động
│   └── AppConfig.java              ⬅️ MỚI — hàm đọc cấu hình dùng chung cho mọi module
├── stats/
│   └── ProductStatsService.java    ⬅️ MỚI — mọi thao tác với Redis
├── storage/
│   └── FileStorageService.java     ⬅️ SỬA — đọc app.upload.dir qua AppConfig thay cho @Value
└── product/
    ├── ProductService.java         ⬅️ SỬA — top, vừa xem, dọn Redis khi xoá, đọc cấu hình qua AppConfig
    └── ProductViewController.java  ⬅️ SỬA — trang chi tiết, đưa lượt xem ra danh sách
```

Bài này **không cần** sửa `build.gradle` (đã có `spring-boot-starter-data-redis` từ bài 14) và cũng không đụng tới Docker.

---

## Bước 1 — `application.yml`: timeout cho Redis

Thêm 2 dòng `timeout` dưới `spring.data.redis`:

```yaml
  data:
    mongodb:
      uri: ...                          # giữ nguyên
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      # Redis chết → chờ tối đa 2 giây rồi báo lỗi. Mặc định chờ tới 60 giây → trang treo cả phút
      timeout: 2s
      connect-timeout: 2s
```

Vì sao cần? Ở Bước 3, ta sẽ bắt lỗi để Redis chết thì trang vẫn chạy. Nhưng "bắt được lỗi" chỉ có ích khi lỗi **đến sớm**. Mặc định, thư viện Lettuce chờ một lệnh Redis tới **60 giây** rồi mới chịu báo lỗi. Trang danh sách gọi Redis 2 lần, tức là treo 2 phút rồi mới hiện ra. Với `2s`, trang vẫn chậm (khoảng 4 giây) nhưng không bị treo.

- `connect-timeout`: chờ **kết nối** tới Redis tối đa bao lâu.
- `timeout`: chờ **kết quả của một lệnh** tối đa bao lâu.

> Nhớ là `spring.data.redis`, **không phải** `spring.redis` (tên cũ trước Spring Boot 3, xem bài 14 Bước 4). Ghi sai thì Spring lặng lẽ bỏ qua, và timeout vẫn là 60 giây.

---

## Bước 2 — Mỗi module một file `.properties`, đọc qua package `common`

### Vì sao không viết thẳng `Duration.ofDays(30)` và số `5` vào code?

Viết cứng thì chạy được, nhưng mỗi lần muốn đổi (ví dụ đặt hạn `5m` để thử TTL cho nhanh, hay muốn khung Top hiện 10 sản phẩm) là phải **sửa code rồi build lại**. Đưa ra file cấu hình thì chỉ cần sửa một dòng.

Bài 15 để cấu hình riêng của app (`app.upload.dir`, `app.product.default-image`) chung trong `application.yml`, cạnh cấu hình DB, Redis, log... Càng nhiều module thì file càng dài. Bài này tách ra theo kiểu **file `.ini` bên PHP**: **mỗi module một file**, trong đó chỉ có các dòng `key=value`:

| Module | File cấu hình | Key | Ai đọc |
|---|---|---|---|
| `stats/` | `config/stats.properties` (MỚI) | `app.stats.*` | `ProductStatsService` |
| `product/` | `config/product.properties` (chuyển từ yml) | `app.product.*` | `ProductService` |
| `storage/` | `config/upload.properties` (chuyển từ yml) | `app.upload.*` | `FileStorageService` |

`application.yml` chỉ còn giữ cấu hình **của Spring** (DB, Redis, JPA, log, actuator).

Phía Java có 2 class dùng chung, đặt trong package `common/` (cạnh `NotFoundException`):

- **`ModuleConfigLoader`**: lúc app khởi động, nó tự tìm và nạp **mọi** file `config/*.properties`. Sau này thêm module mới chỉ cần thêm file, không phải khai báo ở đâu cả.
- **`AppConfig`**: hàm đọc cấu hình dùng chung, giống hàm `config('...')` bên PHP. Module nào cần thì `import com.example.demo.common.AppConfig` rồi gọi `config.getInt("app.stats.top-limit")`.

```
Khởi động app
  ModuleConfigLoader ──▶ đọc config/product.properties, config/stats.properties, config/upload.properties
                         ──▶ đổ hết vào Environment ("kho" cấu hình của Spring, đã có sẵn application.yml)
  ProductStatsService(…, AppConfig config)
      config.getInt("app.stats.top-limit") ──▶ Environment ──▶ "5" ──▶ 5
```

**Cái gì đưa ra cấu hình, cái gì vẫn là hằng số?**

- **Đưa ra file `.properties`**: những con số *có thể muốn chỉnh* mà không sửa code, như hạn, giới hạn số lượng, đường dẫn thư mục.
- **Giữ là hằng số trong code**: tên key Redis (`demo:product:views`, `demo:recent:`). Lỡ đổi tên key thì dữ liệu cũ trong Redis bị "mồ côi", vì code không còn tìm thấy nó nữa. Tiền tố URL `/uploads/` cũng vậy: nó gắn với `SecurityConfig` và `WebConfig`, đổi là phải sửa code đi kèm.

### ① Ba file cấu hình trong `src/main/resources/config/`

Tạo thư mục `config/` **cạnh** `application.yml`, rồi tạo 3 file:

**`config/stats.properties`** (mới):

```properties
# Cấu hình module stats (bài 16): lượt xem, "Bạn vừa xem", Top
# Thời gian ghi kèm đơn vị: ms, s, m, h, d. Không viết dấu nháy.

# "Bạn vừa xem": bao lâu không xem gì thì danh sách tự biến mất
app.stats.recent-ttl=30d
# "Bạn vừa xem": giữ tối đa bao nhiêu sản phẩm
app.stats.recent-limit=5
# Khung "Xem nhiều nhất": hiện bao nhiêu sản phẩm
app.stats.top-limit=5
```

**`config/product.properties`** (chuyển từ `application.yml`):

```properties
# Cấu hình module product
# Ảnh khi sản phẩm chưa có ảnh. Tương đối so với src/main/resources/static/
app.product.default-image=images/product-default.jpg
```

**`config/upload.properties`** (chuyển từ `application.yml`):

```properties
# Cấu hình module storage: file người dùng upload (bài 15)
# Đường dẫn TƯƠNG ĐỐI → tính từ thư mục đang chạy app (Docker: /app → /app/uploads).
# Muốn để chỗ khác: đặt biến môi trường UPLOAD_DIR, ví dụ /var/lib/demo/uploads
app.upload.dir=${UPLOAD_DIR:uploads}
```

So với `.ini` bên PHP:

- **Không có `[section]`.** Mỗi dòng phải viết đủ tiền tố `app.stats.`. Nhờ vậy key trong file trùng khớp với key gọi trong Java, tìm kiếm (Ctrl+Shift+F) là ra ngay.
- **Không viết dấu nháy.** `app.stats.recent-ttl="30d"` sẽ đọc ra chuỗi `"30d"`, tính **cả dấu nháy**, và app báo lỗi lúc khởi động.
- **Comment dùng `#`**, không dùng `;`.
- `${UPLOAD_DIR:uploads}` vẫn chạy y như trong yml: có biến môi trường `UPLOAD_DIR` thì lấy biến đó, không có thì dùng `uploads`.

> ⚠️ **Thời gian luôn ghi kèm đơn vị.** Viết `app.stats.recent-ttl=30` (số trơn) thì Spring hiểu là **30 mili giây**: key "vừa xem" hết hạn gần như ngay lập tức, nên mục "Bạn vừa xem" lúc nào cũng trống. `30d` cùng một kiểu viết với `timeout: 2s` ở Bước 1.

### ② `application.yml`: bỏ khối `app:`

**Xoá hẳn** khối `app:` ở cuối file (cả `upload:` lẫn `product:`), vì giá trị của chúng đã chuyển sang `config/`.

> Lỡ để lại thì app vẫn chạy, nhưng **giá trị trong `application.yml` sẽ thắng**: `application.yml` được ưu tiên hơn các file trong `config/` (lý do ở ③). Khi đó bạn sửa `config/upload.properties` mà không thấy gì thay đổi.

### ③ `common/ModuleConfigLoader.java`: tự nạp mọi file `config/*.properties`

```java
package com.example.demo.common;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.PropertiesLoaderUtils;

// Tự nạp MỌI file src/main/resources/config/*.properties lúc app khởi động.
// Thêm module mới chỉ cần thêm file .properties, không phải khai báo ở đâu cả.
// Không phải @Component: nó chạy TRƯỚC khi Spring tạo bean, nên được đăng ký trong META-INF/spring.factories
public class ModuleConfigLoader implements EnvironmentPostProcessor {

    private static final String PATTERN = "classpath*:config/*.properties";

    private final Log log;

    // Lúc này hệ thống log chưa sẵn sàng → Spring đưa một logger "hoãn", in ra sau khi log khởi động xong
    public ModuleConfigLoader(DeferredLogFactory logFactory) {
        this.log = logFactory.getLog(ModuleConfigLoader.class);
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        try {
            Resource[] files = new PathMatchingResourcePatternResolver().getResources(PATTERN);
            Arrays.sort(files, Comparator.comparing(Resource::getFilename));   // thứ tự cố định: product, stats, upload
            for (Resource file : files) {
                // Đọc theo UTF-8 (Spring Boot mặc định đọc .properties theo ISO-8859-1 → lỗi font tiếng Việt)
                Properties props = PropertiesLoaderUtils.loadProperties(new EncodedResource(file, StandardCharsets.UTF_8));
                // addLast = ưu tiên THẤP NHẤT: application.yml và biến môi trường vẫn ghi đè được
                environment.getPropertySources().addLast(new PropertiesPropertySource("config/" + file.getFilename(), props));
                log.info("[Config] đã nạp config/" + file.getFilename() + " (" + props.size() + " key)");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Không đọc được file cấu hình trong config/", e);
        }
    }
}
```

Rồi tạo file **`src/main/resources/META-INF/spring.factories`** (thư mục `META-INF` viết hoa), chỉ có đúng 1 dòng:

```properties
org.springframework.boot.env.EnvironmentPostProcessor=com.example.demo.common.ModuleConfigLoader
```

Mấy điểm cần để ý:

- **`EnvironmentPostProcessor`** là "móc" mà Spring Boot gọi **rất sớm**: sau khi đọc xong `application.yml`, nhưng trước khi tạo bất kỳ bean nào (service, controller...). Nhờ vậy, khi `ProductStatsService` được tạo thì các key trong `config/` đã có sẵn.
- **Vì sao phải đăng ký trong `spring.factories`, không dùng `@Component`?** `@Component` chỉ có tác dụng khi Spring **quét bean**, mà lúc đó thì đã quá muộn. `spring.factories` là danh sách Spring Boot đọc ngay từ đầu để biết có những "móc" nào cần gọi. Quên file này thì `ModuleConfigLoader` không bao giờ chạy.
- **`classpath*:config/*.properties`**: tìm mọi file đuôi `.properties` trong thư mục `config/` của classpath. `src/main/resources/` sẽ được copy vào classpath, nên thư mục `src/main/resources/config/` nằm trong phạm vi tìm. Chạy được cả ở chế độ dev lẫn khi đóng gói thành `.jar`.
- **Đọc theo UTF-8.** Nếu nạp `.properties` theo cách mặc định của Spring Boot thì giá trị tiếng Việt bị lỗi font (`Xin chào` → `Xin chÃ o`), xem [sổ tay kiến thức chung](../cong-nghe/spring-boot.md#java-có-file-cấu-hình-kiểu-ini-của-php-key--value-không-có-cần-tự-viết-hàm-đọc-chung-không). Ở đây ta tự đọc nên chọn được bảng mã.
- **`addLast`** đặt các file này ở **cuối hàng ưu tiên**. Spring tìm một key từ trên xuống: tham số dòng lệnh → biến môi trường → `application.yml` → `config/*.properties`. Thấy ở đâu trước thì lấy ở đó. Vì vậy muốn ghi đè tạm mà không sửa file, chỉ cần đặt biến môi trường, ví dụ `APP_STATS_TOPLIMIT=10` (viết hoa, dấu `.` thành `_`, bỏ dấu `-`).

### ④ `common/AppConfig.java`: hàm đọc cấu hình dùng chung

```java
package com.example.demo.common;

import java.time.Duration;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

// Hàm đọc cấu hình DÙNG CHUNG cho mọi module, giống hàm config() bên PHP.
// Giá trị có thể nằm trong config/<module>.properties (do ModuleConfigLoader nạp),
// trong application.yml hay biến môi trường: nơi gọi không cần biết nó ở đâu.
@Component
public class AppConfig {

    // Environment = "kho" chứa mọi cấu hình mà Spring đã đọc được
    private final Environment env;

    public AppConfig(Environment env) {
        this.env = env;
    }

    // getRequiredProperty: không tìm thấy key → ném lỗi "Required key '...' not found"
    public String getString(String key) {
        return env.getRequiredProperty(key);
    }

    public int getInt(String key) {
        return env.getRequiredProperty(key, Integer.class);
    }

    // "30d", "10m", "2s" → Duration. Số trơn không có đơn vị thì hiểu là mili giây
    public Duration getDuration(String key) {
        return env.getRequiredProperty(key, Duration.class);
    }
}
```

- **`Environment`** là bean có sẵn của Spring, chứa **mọi** cấu hình: `application.yml`, biến môi trường, và các file `config/` mà `ModuleConfigLoader` vừa nạp. `AppConfig` chỉ là một lớp vỏ mỏng bọc quanh nó, cho gọn và có tên dễ nhớ. Thật ra `@Value("${app.upload.dir}")` của bài 15 cũng đọc từ chính `Environment` này.
- **`getRequiredProperty`**: thiếu key là **ném lỗi ngay**. Nên đọc cấu hình **trong constructor** và lưu vào field `final`. Khi đó, gõ sai tên key (trong file hay trong code) thì app **không khởi động** và báo rõ `Required key 'app.stats.recent-tll' not found`, chứ không đợi tới lúc có người mở trang mới lỗi.
- Cần kiểu khác (ví dụ `boolean`, danh sách) thì thêm hàm vào đây, mọi module đều dùng được. Xem bài tập 8.

### ⑤ `FileStorageService`: đọc qua `AppConfig`

Sửa constructor. Xoá dòng `import org.springframework.beans.factory.annotation.Value;` vì không còn dùng, rồi thêm `import com.example.demo.common.AppConfig;`:

```java
    // Trước: public FileStorageService(@Value("${app.upload.dir}") String uploadDir) {
    public FileStorageService(AppConfig config) {
        this.root = Paths.get(config.getString("app.upload.dir")).toAbsolutePath().normalize();
        // ... phần còn lại giữ nguyên
    }
```

`ProductService` cũng sẽ chuyển sang `AppConfig`, nhưng để sang **Bước 4** làm một lần, vì bước đó sửa constructor của nó để thêm `ProductStatsService`. Trong lúc chờ, `@Value("${app.product.default-image}")` cũ vẫn chạy bình thường: `@Value` cũng đọc từ `Environment`, và key này giờ nằm trong `config/product.properties`.

**Kiểm tra nhanh:** lưu file, chờ app restart. Log phải có:

```
[Config] đã nạp config/product.properties (1 key)
[Config] đã nạp config/stats.properties (3 key)
[Config] đã nạp config/upload.properties (1 key)
[Upload] thư mục lưu file: /app/uploads
```

Thêm/sửa ảnh sản phẩm vẫn chạy như cũ. Không thấy dòng `[Config]` nào thì xem lại `META-INF/spring.factories`.

### 📖 Tra cứu: các hàm có sẵn dùng trong bước này

Các hàm dưới đây **không phải do mình viết**, mà có sẵn trong Java hoặc Spring. Cách tự nhận biết và tự tra một hàm lạ: xem [sổ tay kiến thức chung](../kien-thuc/doc-bai-hoc-va-tra-cuu.md#hàm-có-sẵn-của-thư-viện-ví-dụ-opsforzset-ở-đâu-ra-làm-sao-biết-nó-làm-gì).

**Trong `ModuleConfigLoader` (③)**

| Hàm | Của ai | Làm gì | Trả về |
|---|---|---|---|
| `new PathMatchingResourcePatternResolver().getResources(PATTERN)` | Spring Core | Tìm mọi file khớp mẫu `classpath*:config/*.properties`. Mới chỉ **tìm**, chưa đọc nội dung | `Resource[]`: mỗi phần tử là một file. Không thấy file nào thì mảng rỗng |
| `file.getFilename()` | `Resource` (Spring) | Lấy tên file, không kèm đường dẫn | `String`, ví dụ `"stats.properties"` |
| `Arrays.sort(files, Comparator.comparing(Resource::getFilename))` | Java | Sắp xếp mảng **tại chỗ** (sửa luôn mảng `files`). `Comparator.comparing(...)` nghĩa là "so sánh theo tên file". `Resource::getFilename` là cách viết tắt của lambda `r -> r.getFilename()` | không trả gì (`void`) |
| `new EncodedResource(file, StandardCharsets.UTF_8)` | Spring Core | Bọc file kèm thông tin bảng mã, để lúc đọc thì đọc theo UTF-8 | object `EncodedResource` |
| `PropertiesLoaderUtils.loadProperties(...)` | Spring Core | Đọc các dòng `key=value` của file. Bỏ qua dòng comment `#` | `Properties`: giống một `Map<String, String>` |
| `props.size()` | `Properties` (Java) | Đếm số key đọc được | `int` |
| `new PropertiesPropertySource(name, props)` | Spring Core | Biến `Properties` thành một "nguồn cấu hình" có tên mà `Environment` hiểu được. Tên chỉ để nhận diện khi debug | object `PropertySource` |
| `environment.getPropertySources()` | `ConfigurableEnvironment` (Spring) | Lấy danh sách các nguồn cấu hình, xếp theo độ ưu tiên từ cao xuống thấp | `MutablePropertySources` |
| `.addLast(source)` | `MutablePropertySources` | Thêm nguồn vào **cuối** danh sách, tức ưu tiên thấp nhất. Ngược lại, `addFirst` cho ưu tiên cao nhất | `void` |
| `logFactory.getLog(ModuleConfigLoader.class)` | Spring Boot | Tạo logger "hoãn": dòng log được giữ lại, đợi hệ thống log sẵn sàng rồi mới in | `Log` |

**Trong `AppConfig` (④) và `FileStorageService` (⑤)**

| Hàm | Của ai | Làm gì | Trả về |
|---|---|---|---|
| `env.getRequiredProperty(key)` | `Environment` (Spring) | Tìm key trong mọi nguồn cấu hình. Không thấy thì ném `IllegalStateException` *"Required key '...' not found"* | `String` |
| `env.getRequiredProperty(key, Integer.class)` | `Environment` | Như trên, rồi **đổi kiểu** giá trị. `Integer.class` là cách truyền "kiểu `Integer`" vào làm tham số. Đổi không được (ví dụ `abc` sang số) thì ném lỗi | `Integer`. Java tự đổi sang `int` khi `return` |
| `env.getRequiredProperty(key, Duration.class)` | `Environment` | Đổi `"30d"`, `"2s"` sang `Duration` | `Duration` |
| `Paths.get(s).toAbsolutePath().normalize()` | Java NIO (bài 15) | Chuỗi → đường dẫn `Path`, đổi sang đường dẫn tuyệt đối, bỏ các đoạn `./`, `../` | `Path` |

### Sau này thêm module mới (ví dụ `review/` ở bài 18)

Chỉ có 2 việc:

1. Tạo `src/main/resources/config/review.properties`, ví dụ `app.review.max-length=1000`.
2. Trong service: `import com.example.demo.common.AppConfig;`, nhận `AppConfig config` qua constructor, rồi `this.maxLength = config.getInt("app.review.max-length");`.

Không phải sửa `application.yml`, `ModuleConfigLoader` hay `spring.factories`.

---

## Bước 3 — `ProductStatsService`: mọi thao tác với Redis

Tạo package `stats/` cạnh `product/`, rồi tạo `stats/ProductStatsService.java`:

```java
package com.example.demo.stats;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;

import com.example.demo.common.AppConfig;

// Class DUY NHẤT biết lượt xem nằm trong Redis, giống FileStorageService là class duy nhất biết file nằm ở đâu.
// Chỉ làm việc với id (số), không biết gì về Product hay MySQL.
@Service
public class ProductStatsService {

    private static final Logger log = LoggerFactory.getLogger(ProductStatsService.class);

    // Tên key là HẰNG SỐ, không đưa ra cấu hình: đổi tên key là mất dấu dữ liệu cũ trong Redis
    // Sorted Set: member = id sản phẩm, score = số lượt xem. Redis tự giữ thứ tự theo score.
    private static final String VIEWS_KEY = "demo:product:views";

    // List: id các sản phẩm user vừa xem, mới nhất đứng đầu. Mỗi user một key: demo:recent:admin
    private static final String RECENT_KEY_PREFIX = "demo:recent:";

    private final StringRedisTemplate redisTemplate;

    // Các con số đọc từ config/stats.properties (Bước 2)
    private final Duration recentTtl;   // "Bạn vừa xem": bao lâu không xem gì thì key tự biến mất
    private final int recentLimit;      // "Bạn vừa xem": giữ tối đa bao nhiêu sản phẩm
    private final int topLimit;         // Khung "Xem nhiều nhất": hiện bao nhiêu sản phẩm

    public ProductStatsService(StringRedisTemplate redisTemplate, AppConfig config) {
        this.redisTemplate = redisTemplate;
        // Đọc 1 lần lúc khởi động: thiếu key hay sai định dạng thì app báo lỗi ngay, không đợi tới lúc có người xem
        this.recentTtl = config.getDuration("app.stats.recent-ttl");
        this.recentLimit = config.getInt("app.stats.recent-limit");
        this.topLimit = config.getInt("app.stats.top-limit");
        log.info("[Stats] recent-ttl={}, recent-limit={}, top-limit={}", recentTtl, recentLimit, topLimit);
    }

    // Ghi nhận 1 lượt xem, trả về tổng lượt xem MỚI của sản phẩm
    public long recordView(long productId, String username) {
        String id = String.valueOf(productId);   // StringRedisTemplate chỉ làm việc với chuỗi
        try {
            // ZINCRBY demo:product:views 1 "12" → cộng 1 NGAY TRONG Redis, chưa có thì coi như 0
            Double views = redisTemplate.opsForZSet().incrementScore(VIEWS_KEY, id, 1);

            String recentKey = RECENT_KEY_PREFIX + username;
            redisTemplate.opsForList().remove(recentKey, 0, id);                 // LREM: xoá id này nếu đã có (khỏi trùng)
            redisTemplate.opsForList().leftPush(recentKey, id);                  // LPUSH: chèn lên đầu
            redisTemplate.opsForList().trim(recentKey, 0, recentLimit - 1);     // LTRIM: chỉ giữ 5 phần tử đầu
            redisTemplate.expire(recentKey, recentTtl);                          // EXPIRE: đặt lại hạn 30 ngày

            return views.longValue();
        } catch (DataAccessException e) {
            // Redis chết thì trang chi tiết vẫn phải mở được, chỉ là không đếm
            log.warn("[Stats] Redis lỗi, không ghi được lượt xem sản phẩm {}: {}", productId, e.getMessage());
            return 0;
        }
    }

    // Lượt xem của MỌI sản phẩm, lấy bằng 1 lệnh duy nhất: id → số lượt xem
    public Map<Long, Long> allViews() {
        Map<Long, Long> result = new HashMap<>();
        try {
            // ZRANGE demo:product:views 0 -1 WITHSCORES
            Set<TypedTuple<String>> tuples = redisTemplate.opsForZSet().rangeWithScores(VIEWS_KEY, 0, -1);
            for (TypedTuple<String> t : tuples) {
                result.put(Long.valueOf(t.getValue()), t.getScore().longValue());
            }
        } catch (DataAccessException e) {
            log.warn("[Stats] Redis lỗi, không đọc được lượt xem: {}", e.getMessage());
        }
        return result;
    }

    // Id của top-limit sản phẩm xem nhiều nhất, nhiều nhất đứng đầu
    public List<Long> topProductIds() {
        List<Long> ids = new ArrayList<>();
        try {
            // ZREVRANGE demo:product:views 0 4 → "REV" = score từ cao xuống thấp
            Set<String> members = redisTemplate.opsForZSet().reverseRange(VIEWS_KEY, 0, topLimit - 1);
            for (String m : members) {
                ids.add(Long.valueOf(m));
            }
        } catch (DataAccessException e) {
            log.warn("[Stats] Redis lỗi, không đọc được top sản phẩm: {}", e.getMessage());
        }
        return ids;
    }

    // Id các sản phẩm user vừa xem, mới nhất đứng đầu
    public List<Long> recentProductIds(String username) {
        List<Long> ids = new ArrayList<>();
        try {
            // LRANGE demo:recent:admin 0 -1 → cả danh sách
            List<String> members = redisTemplate.opsForList().range(RECENT_KEY_PREFIX + username, 0, -1);
            for (String m : members) {
                ids.add(Long.valueOf(m));
            }
        } catch (DataAccessException e) {
            log.warn("[Stats] Redis lỗi, không đọc được sản phẩm vừa xem của {}: {}", username, e.getMessage());
        }
        return ids;
    }

    // Xoá sản phẩm khỏi bảng xếp hạng (gọi khi xoá sản phẩm trong MySQL)
    public void removeProduct(long productId) {
        try {
            redisTemplate.opsForZSet().remove(VIEWS_KEY, String.valueOf(productId));   // ZREM
        } catch (DataAccessException e) {
            log.warn("[Stats] Redis lỗi, không xoá được lượt xem sản phẩm {}: {}", productId, e.getMessage());
        }
    }
}
```

Mấy điểm cần để ý:

- **`opsForZSet()`, `opsForList()`**: `StringRedisTemplate` chia lệnh theo kiểu dữ liệu. `opsForValue()` ở bài 14 dành cho String, `opsForZSet()` cho Sorted Set, `opsForList()` cho List. Comment cạnh mỗi dòng ghi tên lệnh Redis tương ứng, để bạn gõ thử được trong `redis-cli`. Từng hàm nhận gì, trả về gì: xem phần **📖 Tra cứu** ngay dưới.
- **Vì sao không `get` rồi `set`?** Nếu viết theo kiểu Java "đọc số cũ, cộng 1, ghi lại", và 2 người xem **cùng lúc**, thì cả hai cùng đọc được `5` rồi cùng ghi `6`, nên mất 1 lượt. `ZINCRBY` cộng **ngay bên trong Redis**, trong một bước không thể chen ngang (gọi là *atomic*), nên không bao giờ bị mất lượt.
- **Bộ ba `LREM` → `LPUSH` → `LTRIM`** là mẫu quen thuộc cho "N cái gần nhất": xoá bản cũ nếu có (để một sản phẩm không xuất hiện 2 lần), chèn lên đầu, rồi cắt bớt phần đuôi. List không bao giờ dài quá `recent-limit` (5) phần tử. `LTRIM 0 4` giữ các vị trí từ 0 tới 4, nên trong code phải là `recentLimit - 1`.
- **`expire` gọi lại mỗi lần xem**, nên hạn `recent-ttl` (30 ngày) được tính từ **lần xem cuối**. User bỏ đi 30 ngày thì key tự biến mất, Redis không bị đầy dần bởi user cũ.
- **Không còn con số nào viết cứng.** Ba giá trị đọc từ `config/stats.properties` qua `AppConfig`, còn `topProductIds()` không nhận tham số `limit` nữa: "Top mấy sản phẩm" là chuyện của module stats, nên `ProductViewController` không cần biết con số đó. Chỉ tên key vẫn là hằng số (lý do ở Bước 2).
- **Dòng log trong constructor** in ra `[Stats] recent-ttl=PT720H, recent-limit=5, top-limit=5`. `PT720H` là cách Java in một `Duration` theo chuẩn ISO-8601: `P`eriod `T`ime 720 `H`ours, tức 720 giờ = 30 ngày.
- **`catch (DataAccessException e)`**, không phải `catch (Exception e)`. Spring đổi mọi lỗi của Redis (mất kết nối, quá thời gian...) thành các lớp con của `DataAccessException`. Bắt đúng loại đó thì những lỗi do **code sai** (ví dụ `NullPointerException`) vẫn hiện ra cho bạn thấy, thay vì bị lặng lẽ nuốt mất. Xem thêm: [`catch (IOException e)` và `catch (Exception e)` khác nhau thế nào?](../kien-thuc/java-ngoai-le.md#catch-ioexception-e-và-catch-exception-e-khác-nhau-thế-nào).
- **Lỗi Redis → trả giá trị "rỗng"** (`0`, Map rỗng, List rỗng) và ghi `log.warn`. Đây chính là quy tắc 1 ở phần Tư vấn: dữ liệu phụ hỏng thì trang chính vẫn chạy. Còn nghiệp vụ chính (giá âm, không tìm thấy sản phẩm) thì vẫn ném lỗi như cũ.

> `recordView()` gửi **5 lệnh riêng lẻ** tới Redis. Nếu Redis chết đúng giữa chừng, có thể lượt xem đã được cộng mà danh sách "vừa xem" chưa được cập nhật. Với dữ liệu phụ thì chuyện này chấp nhận được. Muốn gộp 5 lệnh thành 1 lần gửi, hoặc để chúng chạy "hoặc tất cả, hoặc không gì cả", xem bài tập 6.

### 📖 Tra cứu: các hàm có sẵn dùng trong bước này

Các hàm dưới đây **không phải do mình viết**. Chúng nằm trong thư viện **Spring Data Redis** (có được nhờ dòng `spring-boot-starter-data-redis` trong `build.gradle`, bài 14) hoặc trong Java. Cách tự nhận biết và tự tra một hàm lạ: xem [sổ tay kiến thức chung](../kien-thuc/doc-bai-hoc-va-tra-cuu.md#hàm-có-sẵn-của-thư-viện-ví-dụ-opsforzset-ở-đâu-ra-làm-sao-biết-nó-làm-gì).

#### Đọc một dòng gọi nối nhau: `redisTemplate.opsForZSet().incrementScore(...)`

```java
Double views = redisTemplate.opsForZSet().incrementScore(VIEWS_KEY, id, 1);
```

Đọc từ trái sang phải. Mỗi dấu `.` là **một lần gọi hàm trên kết quả của lần gọi trước**:

1. `redisTemplate`: bean `StringRedisTemplate`, Spring tự tạo rồi truyền vào constructor.
2. `.opsForZSet()`: **chưa gửi gì** tới Redis. Hàm này chỉ trả về một object kiểu `ZSetOperations<String, String>`, có thể hiểu là "bộ lệnh dành cho Sorted Set". `<String, String>` nghĩa là key và member đều là chuỗi.
3. `.incrementScore(...)`: gọi trên object vừa nhận được. Tới đây mới thật sự gửi lệnh `ZINCRBY` tới Redis.

Viết tách thành 2 dòng thì chạy y hệt:

```java
ZSetOperations<String, String> zset = redisTemplate.opsForZSet();   // lấy "bộ lệnh Sorted Set"
Double views = zset.incrementScore(VIEWS_KEY, id, 1);               // gửi ZINCRBY
```

**Vì sao Spring thiết kế như vậy?** Redis có hơn 200 lệnh. Nếu nhét hết vào `StringRedisTemplate` thì class này sẽ có hàng trăm method, lại còn trùng tên nhau: `remove` của List (`LREM`) khác hẳn `remove` của Sorted Set (`ZREM`). Vì thế Spring chia lệnh theo kiểu dữ liệu:

| Gọi | Nhận được "bộ lệnh" | Cho kiểu Redis | Lệnh Redis tương ứng |
|---|---|---|---|
| `opsForValue()` | `ValueOperations` | String (bài 14) | `SET`, `GET`, `INCR`... |
| `opsForList()` | `ListOperations` | List | các lệnh bắt đầu bằng `L` |
| `opsForSet()` | `SetOperations` | Set | các lệnh bắt đầu bằng `S` |
| `opsForZSet()` | `ZSetOperations` | Sorted Set | các lệnh bắt đầu bằng `Z` |
| `opsForHash()` | `HashOperations` | Hash | các lệnh bắt đầu bằng `H` |

Các lệnh **dùng được cho mọi kiểu dữ liệu** (đặt hạn, xoá key, kiểm tra key có tồn tại không) thì gọi thẳng trên `redisTemplate`: `expire`, `delete`, `hasKey`.

#### Sorted Set: `redisTemplate.opsForZSet().…`

| Hàm | Lệnh Redis | Tham số | Trả về | Dùng ở |
|---|---|---|---|---|
| `incrementScore(key, member, delta)` | `ZINCRBY` | key; member cần cộng; số cộng thêm (ở đây là `1`) | `Double`: score **sau khi** cộng. Member chưa có thì Redis tạo mới với score = `delta` | `recordView` |
| `rangeWithScores(key, start, end)` | `ZRANGE … WITHSCORES` | vị trí đầu và vị trí cuối, đánh số từ `0`. `-1` là phần tử cuối cùng, nên `0, -1` là lấy hết | `Set<TypedTuple<String>>`: mỗi phần tử gồm member + score, xếp score **thấp → cao**. Key chưa có thì `Set` rỗng | `allViews` |
| `reverseRange(key, start, end)` | `ZREVRANGE` | như trên | `Set<String>`: chỉ có member, xếp score **cao → thấp** | `topProductIds` |
| `remove(key, member...)` | `ZREM` | một hoặc nhiều member | `Long`: số member đã xoá thật (member không có thì `0`) | `removeProduct` |

`TypedTuple<String>` là một **cặp** gồm member và score:

- `t.getValue()`: member, ví dụ `"12"` (`String`)
- `t.getScore()`: score, ví dụ `5.0` (`Double`)

> Kết quả trả về là `Set`, nhưng **vẫn giữ đúng thứ tự** Redis gửi về (bên trong, Spring dùng `LinkedHashSet`). Vì vậy vòng `for` qua kết quả của `reverseRange(...)` đi đúng thứ tự xếp hạng.

#### List: `redisTemplate.opsForList().…`

| Hàm | Lệnh Redis | Tham số | Trả về |
|---|---|---|---|
| `remove(key, count, value)` | `LREM` | `count`: xoá bao nhiêu lần xuất hiện của `value`. `0` nghĩa là xoá **hết** | `Long`: số phần tử đã xoá |
| `leftPush(key, value)` | `LPUSH` | phần tử cần chèn vào **đầu** list (bên trái) | `Long`: độ dài list sau khi chèn. Key chưa có thì Redis tự tạo list mới |
| `trim(key, start, end)` | `LTRIM` | giữ lại các vị trí từ `start` tới `end` (tính cả hai đầu), bỏ hết phần còn lại | không trả gì (`void`) |
| `range(key, start, end)` | `LRANGE` | `0, -1` là cả list | `List<String>`. Key không tồn tại thì list rỗng |

#### Lệnh chung: gọi thẳng trên `redisTemplate`

| Hàm | Lệnh Redis | Tham số | Trả về |
|---|---|---|---|
| `expire(key, timeout)` | `EXPIRE` | thời hạn, kiểu `Duration` | `Boolean`: `true` nếu key có tồn tại và đã được đặt hạn |

#### Hàm của Java dùng để đổi kiểu

| Hàm | Làm gì | Ví dụ |
|---|---|---|
| `String.valueOf(productId)` | số → chuỗi. Cần vì `StringRedisTemplate` chỉ nhận chuỗi | `12` → `"12"` |
| `Long.valueOf(m)` | chuỗi → `Long`. Chuỗi không phải số thì ném `NumberFormatException` | `"12"` → `12` |
| `views.longValue()` | `Double` → `long`, bỏ phần thập phân. Score trong Redis luôn là số thực nên phải đổi | `5.0` → `5` |

> Các hàm Redis trả về **wrapper** (`Double`, `Long`, `Boolean`), không phải `double`, `long`, `boolean`. Lý do là trong vài chế độ đặc biệt (pipeline, transaction, xem bài tập 6) kết quả chưa có ngay, nên chúng trả về `null`. Bài này không dùng các chế độ đó. Phân biệt kiểu viết hoa và viết thường: xem [sổ tay](../kien-thuc/java-kieu-du-lieu.md#kiểu-dữ-liệu-viết-thường-int-long-và-viết-hoa-integer-long-string-duration-khác-nhau-thế-nào).

---

## Bước 4 — `ProductService`: ghép id từ Redis thành sản phẩm

Sửa `product/ProductService.java` ở **4 chỗ**.

**① Import**: thêm vào nhóm import, và **xoá** dòng `import org.springframework.beans.factory.annotation.Value;` (không còn dùng `@Value`):

```java
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.example.demo.common.AppConfig;
import com.example.demo.stats.ProductStatsService;
```

**② Field + constructor**: nhận thêm `ProductStatsService`, và thay `@Value` bằng `AppConfig` (Bước 2):

```java
    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;
    private final ProductStatsService productStatsService;
    private final String defaultImageUrl;

    public ProductService(ProductRepository productRepository,
                          FileStorageService fileStorageService,
                          ProductStatsService productStatsService,
                          AppConfig config) {      // trước: @Value("${app.product.default-image}") String defaultImage
        this.productRepository = productRepository;
        this.fileStorageService = fileStorageService;
        this.productStatsService = productStatsService;

        String defaultImage = config.getString("app.product.default-image");   // config/product.properties
        this.defaultImageUrl = "/" + defaultImage;
        // ... phần kiểm tra ảnh mặc định giữ nguyên (vẫn dùng biến defaultImage như cũ)
    }
```

**③ `delete()`**: dọn luôn Redis:

```java
    public void delete(Long id) {
        Product product = findById(id);
        productRepository.delete(product);
        fileStorageService.delete(product.getImagePath());
        // Redis không biết MySQL vừa xoá gì → phải tự dọn, y như tự xoá file ảnh ở dòng trên
        productStatsService.removeProduct(id);
    }
```

**④ Thêm 3 phương thức** vào cuối class:

```java
    // Top sản phẩm xem nhiều nhất: Redis cho thứ tự (danh sách id), MySQL cho dữ liệu.
    // Lấy mấy sản phẩm là do app.stats.top-limit (config/stats.properties) quyết định, ProductService không cần biết
    public List<Product> topViewed() {
        return findAllInOrder(productStatsService.topProductIds());
    }

    // Các sản phẩm user vừa xem, mới nhất đứng đầu
    public List<Product> recentlyViewed(String username) {
        return findAllInOrder(productStatsService.recentProductIds(username));
    }

    // Lấy nhiều sản phẩm bằng 1 câu SQL, rồi xếp lại ĐÚNG thứ tự của danh sách id.
    // Id nào không còn trong MySQL (sản phẩm đã bị xoá) → bỏ qua.
    private List<Product> findAllInOrder(List<Long> ids) {
        // findAllById → SELECT ... WHERE id IN (3, 1, 7): MySQL KHÔNG hứa trả về theo thứ tự 3, 1, 7
        Map<Long, Product> byId = new HashMap<>();
        for (Product p : productRepository.findAllById(ids)) {
            byId.put(p.getId(), p);
        }

        List<Product> result = new ArrayList<>();
        for (Long id : ids) {
            Product p = byId.get(id);
            if (p != null) {
                result.add(p);
            }
        }
        return result;
    }
```

### 📖 Tra cứu: các hàm có sẵn dùng trong bước này

| Hàm | Của ai | Làm gì | Trả về |
|---|---|---|---|
| `productRepository.findAllById(ids)` | Spring Data JPA. `ProductRepository` có sẵn hàm này nhờ `extends JpaRepository`, mình không phải viết | Chạy **một** câu `SELECT ... WHERE id IN (...)`. Id nào không có trong DB thì bỏ qua, **không** báo lỗi | `List<Product>`, **không** đảm bảo đúng thứ tự của `ids`. `ids` rỗng thì list rỗng |
| `new HashMap<>()` | Java | Tạo `Map` rỗng. `<>` để trống vì Java tự suy ra kiểu từ vế trái `Map<Long, Product>` | `HashMap` |
| `map.put(key, value)` | Java `Map` | Ghi một cặp key → value. Key đã có thì **ghi đè** | value cũ, hoặc `null` (thường bỏ qua) |
| `map.get(key)` | Java `Map` | Tra theo key | value, hoặc **`null`** nếu không có key đó. Vì vậy mới cần `if (p != null)` |
| `new ArrayList<>()`, `list.add(x)` | Java `List` | Tạo list rỗng, thêm phần tử vào **cuối** | `add` trả `boolean` (luôn `true`, thường bỏ qua) |

Vì sao `findAllInOrder()` phải làm 2 vòng lặp?

- **Vòng 1**: `findAllById` lấy tất cả sản phẩm bằng **1 câu SQL** (`WHERE id IN (...)`), không phải 5 câu. Nhưng câu `IN` **không giữ thứ tự**: hỏi `(3, 1, 7)`, MySQL thường trả về `1, 3, 7` (theo khoá chính). Vì thế ta bỏ kết quả vào một `Map` để tra theo id.
- **Vòng 2**: đi theo **đúng thứ tự Redis đưa ra** và lấy từng sản phẩm từ `Map`. Đó chính là thứ tự xếp hạng.
- `if (p != null)`: id có trong Redis nhưng **không còn** trong MySQL thì bỏ qua. Đây là chỗ xử lý quy tắc 2 ở phần Tư vấn. `delete()` đã dọn bảng xếp hạng, nhưng **không** dọn được danh sách "vừa xem" của mọi user: muốn dọn thì phải tìm khắp `demo:recent:*`, rất tốn công. Nên ta chấp nhận để id cũ nằm lại, lúc đọc thì bỏ qua, và sau `recent-ttl` (30 ngày) key cũng tự hết hạn. Cách làm này gọi là **dọn lười** (*lazy cleanup*).

> **Chiều phụ thuộc chỉ có một hướng**: `ProductService` → `ProductStatsService`, không bao giờ ngược lại. Nếu bạn cho `ProductStatsService` nhận `ProductService` vào constructor, Spring sẽ báo lỗi *"The dependencies of some of the beans form a cycle"* (A cần B, B lại cần A, không biết tạo cái nào trước).

---

## Bước 5 — `ProductViewController`: trang chi tiết

Sửa `product/ProductViewController.java`.

**Import**: thêm:

```java
import java.security.Principal;
import java.util.List;

import com.example.demo.stats.ProductStatsService;
```

**Field + constructor**: thêm `ProductStatsService`:

```java
    private final ProductService productService;
    private final CategoryService categoryService;
    private final ProductStatsService productStatsService;

    public ProductViewController(ProductService productService,
                                 CategoryService categoryService,
                                 ProductStatsService productStatsService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.productStatsService = productStatsService;
    }
```

**`list()`**: đưa thêm lượt xem và khung Top ra template:

```java
    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", productService.findAll());
        model.addAttribute("views", productStatsService.allViews());       // Map: id → lượt xem
        model.addAttribute("topProducts", productService.topViewed());     // số lượng: app.stats.top-limit
        return "products/list";
    }
```

**Thêm `detail()`**, đặt ngay dưới `list()`:

```java
    // GET /products/12 → trang chi tiết. Mỗi lần mở là 1 lượt xem.
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Principal principal, Model model) {
        // 1) Tìm trong MySQL TRƯỚC: id không tồn tại → 404 ngay, không đếm bậy vào Redis
        Product product = productService.findById(id);

        // 2) Đếm lượt xem + ghi "vừa xem". principal.getName() = tên đăng nhập (bài 12)
        long views = productStatsService.recordView(id, principal.getName());

        // 3) "Bạn vừa xem": bỏ chính sản phẩm đang xem (nó vừa được đẩy lên đầu danh sách ở bước 2)
        List<Product> recent = productService.recentlyViewed(principal.getName());
        recent.removeIf(p -> p.getId() == product.getId());

        model.addAttribute("product", product);
        model.addAttribute("views", views);
        model.addAttribute("recent", recent);
        return "products/detail";
    }
```

Mấy điểm cần để ý:

- **`/{id}` có "nuốt" mất `/new` không?** Không. `@GetMapping("/new")` là đường dẫn **cố định**, nên Spring luôn ưu tiên nó hơn đường dẫn có biến `/{id}`. `/products/new` vẫn ra form thêm mới, còn `/products/12` mới vào `detail()`.
- **`Principal`** là "người đang đăng nhập". Chỉ cần khai báo nó làm tham số, Spring Security tự truyền vào. `getName()` trả về username, chính là cái tên hiện ở "Xin chào, ..." trên thanh menu (bài 12). Mọi trang `/products/**` đều bắt đăng nhập (`anyRequest().authenticated()`), nên ở đây `principal` không bao giờ là `null`.
- **Thứ tự 1 → 2 rất quan trọng.** Nếu đếm trước rồi mới tìm, thì ai đó gõ `/products/99999` sẽ tạo ra lượt xem cho một sản phẩm không có thật.
- **`removeIf(p -> ...)`**: xoá khỏi list mọi phần tử thoả điều kiện. `p -> p.getId() == product.getId()` là một *lambda*, giống cách viết `auth -> auth...` trong `SecurityConfig`. So sánh bằng `==` được vì `getId()` trả về `long` nguyên thuỷ.
- **Controller gọi 2 service** là bình thường, giống cách nó vẫn gọi `categoryService`. Điều bị cấm chỉ là controller gọi thẳng **Repository**.

### 📖 Tra cứu: các hàm có sẵn dùng trong bước này

| Hàm | Của ai | Làm gì | Trả về |
|---|---|---|---|
| `model.addAttribute(name, value)` | Spring MVC `Model` (bài 05) | Gửi một biến sang template. Trong template đọc bằng `${name}` | `Model` (thường bỏ qua) |
| `principal.getName()` | `java.security.Principal` (Java), do Spring Security truyền vào | Lấy tên đăng nhập của user hiện tại | `String`, ví dụ `"admin"` |
| `recent.removeIf(p -> ...)` | Java `List` | Duyệt list, xoá mọi phần tử mà lambda trả về `true`. Sửa **ngay trên** list `recent` | `boolean`: có xoá được phần tử nào không (ở đây bỏ qua) |

> `removeIf` chỉ chạy được với list **sửa được**, như `ArrayList` do `findAllInOrder()` tạo ra. List tạo bằng `List.of(...)` là list chỉ đọc: gọi `removeIf` trên nó sẽ ném `UnsupportedOperationException`.

---

## Bước 6 — `detail.html`: trang chi tiết

Tạo `templates/products/detail.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{fragments/layout}">
<head>
    <title th:text="${product.name}">Chi tiết sản phẩm</title>
</head>
<body>
    <section layout:fragment="content">
        <p><a th:href="@{/products}">← Danh sách sản phẩm</a></p>

        <div class="detail">
            <img class="preview" th:src="@{${@productService.imageUrl(product)}}" th:alt="${product.name}">
            <div>
                <h1 th:text="${product.name}">Tên sản phẩm</h1>
                <p class="price"
                   th:text="${product.price != null} ? (${#numbers.formatDecimal(product.price, 1, 'POINT', 0, 'COMMA')} + ' ₫') : '—'">0 ₫</p>
                <p>Danh mục: <b th:text="${product.category?.name} ?: 'Chưa có'">Danh mục</b></p>
                <!-- views do controller tính: lượt xem SAU KHI đã cộng lượt này -->
                <p class="views">👁 <span th:text="${views}">0</span> lượt xem</p>
                <a class="btn" th:href="@{/products/{id}/edit(id=${product.id})}">Sửa</a>
            </div>
        </div>

        <p class="description"
           th:unless="${#strings.isEmpty(product.description)}"
           th:text="${product.description}">Mô tả</p>

        <!-- Chỉ hiện khi đã xem ít nhất 1 sản phẩm KHÁC -->
        <div th:unless="${recent.isEmpty()}">
            <h2>Bạn vừa xem</h2>
            <div class="recent">
                <a th:each="r : ${recent}" th:href="@{/products/{id}(id=${r.id})}">
                    <img class="thumb" th:src="@{${@productService.imageUrl(r)}}" th:alt="${r.name}" loading="lazy">
                    <span th:text="${r.name}">Tên</span>
                </a>
            </div>
        </div>
    </section>
</body>
</html>
```

Phần lớn cú pháp đã gặp ở bài 15 (`imageUrl`, `formatDecimal`, `#strings.isEmpty`). Có 2 chỗ mới:

- **`${product.category?.name} ?: 'Chưa có'`**: toán tử `?:` (gọi là *Elvis*) có nghĩa "vế trái là `null` thì lấy vế phải". Kết hợp với `?.` (bài 06): chưa có danh mục thì `category?.name` ra `null`, nên hiện chữ "Chưa có".
- **`<title th:text="${product.name}">`**: tiêu đề tab trình duyệt là tên sản phẩm. Layout tổng sẽ lấy nó nhờ `layout:title-pattern` (bài 07).

---

## Bước 7 — `list.html`: khung Top, cột Lượt xem, link chi tiết

Phần dưới đây dựa trên `list.html` của [bài 15 Bước 10](15-anh-va-mo-ta-san-pham.md#bước-10--listhtml-và-css-bảng-có-ảnh-thumbnail). Nếu bạn chưa làm bước đó, hãy làm trước rồi quay lại đây. Thay phần `<section layout:fragment="content">`:

```html
    <section layout:fragment="content">
        <h1>Sản phẩm</h1>
        <p><a class="btn" th:href="@{/products/new}">+ Thêm sản phẩm</a></p>

        <!-- Top 5 xem nhiều nhất: thứ tự lấy từ Redis, tên lấy từ MySQL. Chưa ai xem gì → ẩn cả khung -->
        <div class="top-box" th:unless="${topProducts.isEmpty()}">
            <b>🔥 Xem nhiều nhất</b>
            <ol>
                <li th:each="t : ${topProducts}">
                    <a th:href="@{/products/{id}(id=${t.id})}" th:text="${t.name}">Tên</a>
                    <span class="views" th:text="${views.getOrDefault(t.id, 0)} + ' lượt xem'">0 lượt xem</span>
                </li>
            </ol>
        </div>

        <table>
            <thead>
                <tr><th>Ảnh</th><th>Sản phẩm</th><th>Danh mục</th><th class="right">Giá</th><th class="right">Lượt xem</th><th>Hành động</th></tr>
            </thead>
            <tbody>
                <tr th:each="p : ${products}">
                    <td>
                        <img class="thumb" th:src="@{${@productService.imageUrl(p)}}" th:alt="${p.name}" loading="lazy">
                    </td>
                    <td>
                        <!-- Tên thành link sang trang chi tiết -->
                        <b><a th:href="@{/products/{id}(id=${p.id})}" th:text="${p.name}">Tên</a></b>
                        <span class="muted"
                              th:unless="${#strings.isEmpty(p.description)}"
                              th:text="${#strings.abbreviate(p.description, 80)}">Mô tả ngắn</span>
                    </td>
                    <td th:text="${p.category?.name}">Danh mục</td>
                    <td class="right"
                        th:text="${p.price != null} ? (${#numbers.formatDecimal(p.price, 1, 'POINT', 0, 'COMMA')} + ' ₫') : '—'">0 ₫</td>
                    <!-- views là Map id → lượt xem. Sản phẩm chưa ai xem không có trong Map → 0 -->
                    <td class="right" th:text="${views.getOrDefault(p.id, 0)}">0</td>
                    <td>
                        <a th:href="@{/products/{id}/edit(id=${p.id})}">Sửa</a> |
                        <a th:href="@{/products/{id}/delete(id=${p.id})}"
                           onclick="return confirm('Xoá sản phẩm này? Ảnh của nó cũng bị xoá.')">Xoá</a>
                    </td>
                </tr>
                <tr th:if="${products.isEmpty()}">
                    <td colspan="6">Chưa có sản phẩm nào.</td>
                </tr>
            </tbody>
        </table>
    </section>
```

So với bảng của bài 15:

- Tên sản phẩm thành **link** sang `/products/{id}`.
- Thêm cột **Lượt xem**, nên `colspan` tăng từ `5` lên `6`.
- Thêm **khung Top** phía trên bảng. `th:unless="${topProducts.isEmpty()}"` ẩn cả khung khi chưa có ai xem gì.
- **`views.getOrDefault(p.id, 0)`**: `views` là `Map` id → lượt xem. Sản phẩm chưa ai xem thì không có trong Map. `get()` sẽ trả về `null` (ô trống), còn `getOrDefault(..., 0)` trả về `0`.

> **1 lệnh cho cả bảng, không phải mỗi dòng 1 lệnh.** Cũng có thể viết `${@productStatsService.viewsOf(p.id)}` ngay trong ô, giống cách gọi `imageUrl`. Nhưng như vậy bảng 50 dòng sẽ gửi **50 lệnh** tới Redis. `allViews()` lấy hết trong **1 lệnh** (`ZRANGE ... WITHSCORES`) rồi để template tra trong Map. Lỗi "mỗi dòng gọi DB một lần" rất hay gặp, và có tên riêng là **N+1 query**. Cũng vì vậy mà `findAllInOrder()` ở Bước 4 dùng `findAllById` (1 câu SQL), chứ không gọi `findById` 5 lần.

---

## Bước 8 — CSS

Thêm vào cuối `static/css/app.css`:

```css
/* ----- Bài 16: lượt xem, top, vừa xem ----- */
.top-box {
    background: #fff7ed;
    border: 1px solid #fed7aa;
    border-radius: 4px;
    padding: 8px 16px;
    margin-bottom: 16px;
}

.top-box ol {
    margin: 8px 0 0;
}

.views {
    color: #666;
    font-size: 13px;
    margin-left: 6px;
}

.detail {
    display: flex;              /* ảnh bên trái, thông tin bên phải */
    gap: 24px;
    align-items: flex-start;
}

.detail h1 {
    margin-top: 0;
}

.price {
    font-size: 20px;
    font-weight: bold;
}

.description {
    white-space: pre-line;      /* giữ nguyên các lần xuống dòng khi nhập mô tả */
}

.recent {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
}

.recent a {
    width: 80px;
    font-size: 13px;
    text-decoration: none;
}
```

`white-space: pre-line` giải quyết một chuyện hay gặp: mô tả nhập trong `<textarea>` có xuống dòng, nhưng HTML mặc định gộp mọi khoảng trắng và dấu xuống dòng thành một dấu cách, nên cả đoạn bị dồn thành một dòng.

---

## Bước 9 — Chạy & kiểm tra

```bash
docker compose -f docker-compose.dev.yml up -d
docker compose -f docker-compose.dev.yml logs -f app-dev
```

Chờ app restart xong (`Started DemoApplication`). Log có dòng `[Stats] recent-ttl=PT720H, recent-limit=5, top-limit=5` (cấu hình đã được đọc, xem Bước 3), và **không** có dòng `[Stats] Redis lỗi` nào.

**1. Trước khi xem gì:** đăng nhập, vào `http://localhost:8093/products`. Chưa có khung Top, cột Lượt xem toàn `0`.

**2. Xem chi tiết:** bấm vào tên một sản phẩm. Trang chi tiết hiện `👁 1 lượt xem`. Bấm F5 vài lần: số tăng dần. Quay lại danh sách: khung **🔥 Xem nhiều nhất** xuất hiện.

**3. Top đúng thứ tự:** mở sản phẩm A 3 lần, B 1 lần. Trong khung Top, A đứng trên B.

**4. "Bạn vừa xem":** mở lần lượt A, B, C. Ở trang C có mục **Bạn vừa xem** gồm B, A (không có C, vì đang xem chính nó). Mở lại A: thứ tự đổi thành C, B.

**5. Xem dữ liệu thật trong Redis:**

```bash
docker exec -it demo-redis redis-cli
```

```
127.0.0.1:6379> KEYS demo:*
1) "demo:last-startup"
2) "demo:product:views"
3) "demo:recent:admin"            ← tên user bạn đang đăng nhập
127.0.0.1:6379> TYPE demo:product:views
zset
127.0.0.1:6379> ZREVRANGE demo:product:views 0 -1 WITHSCORES
1) "2"                            ← id sản phẩm
2) "3"                            ← số lượt xem
3) "3"
4) "1"
127.0.0.1:6379> ZSCORE demo:product:views 2
"3"
127.0.0.1:6379> LRANGE demo:recent:admin 0 -1
1) "3"
2) "1"
3) "2"
127.0.0.1:6379> TTL demo:recent:admin
(integer) 2591998                 ← ≈ 30 ngày (tính bằng giây)
```

Thử sửa dữ liệu ngay trong Redis rồi F5 trang danh sách:

```
127.0.0.1:6379> ZINCRBY demo:product:views 100 1
"101"                             ← sản phẩm id 1 nhảy lên đầu khung Top
```

`MONITOR` (bài 14) rồi mở một trang chi tiết: bạn sẽ thấy đúng 5 lệnh `ZINCRBY`, `LREM`, `LPUSH`, `LTRIM`, `EXPIRE` mà `recordView()` gửi tới, rồi tiếp theo là `LRANGE`.

> Hai sản phẩm **bằng lượt xem** thì Redis xếp theo member, tức là theo chuỗi id, chứ không theo ai được xem trước. Đó là lý do ở ví dụ trên, `"3"` đứng trước `"2"` khi `ZREVRANGE`.

**6. Xoá sản phẩm:** xoá một sản phẩm đã có lượt xem. Trong `redis-cli`, `ZRANGE demo:product:views 0 -1` không còn id đó. `LRANGE demo:recent:admin 0 -1` **vẫn còn** id đó (dọn lười, xem Bước 4), nhưng trang chi tiết không hiện nó trong "Bạn vừa xem".

**7. Id không tồn tại:** `http://localhost:8093/products/99999` ra trang 404 (bài 10). Trong Redis không có member `"99999"`.

**8. Tắt Redis, trang vẫn chạy:**

```bash
docker stop demo-redis
```

- Vào trang danh sách: vẫn hiện bình thường sau khoảng 4 giây (2 lệnh Redis × timeout 2s). Không có khung Top, lượt xem toàn `0`.
- Vào một trang chi tiết: vẫn mở được, hiện `0 lượt xem`.
- Log có dòng: `[Stats] Redis lỗi, không ghi được lượt xem sản phẩm 2: Redis command timed out`.

```bash
docker start demo-redis
```

Lần tải đầu tiên sau đó hơi chậm vì app phải kết nối lại. Sau đó mọi thứ trở lại bình thường, và lượt xem cũ **vẫn còn** (Redis đọc lại bản chụp trong volume `redis_data`).

> Thử xoá 2 dòng `timeout` ở Bước 1 rồi lặp lại bước 8: trang danh sách sẽ treo khoảng **2 phút**. Nhớ thêm lại.

**9. Trang Sửa không tính lượt xem:** mở `/products/2/edit` rồi xem `ZSCORE`: số không đổi. Chỉ `detail()` mới gọi `recordView()`.

**10. Đổi cấu hình mà không sửa code:** sửa `config/stats.properties` thành:

```properties
app.stats.recent-ttl=5m
app.stats.recent-limit=3
app.stats.top-limit=3
```

Lưu file, chờ app restart. Log đổi thành `[Stats] recent-ttl=PT5M, recent-limit=3, top-limit=3`.

- Khung **🔥 Xem nhiều nhất** chỉ còn 3 sản phẩm.
- Mở lần lượt 4–5 sản phẩm khác nhau. `LRANGE demo:recent:admin 0 -1` chỉ còn 3 id, và trang chi tiết hiện 2 sản phẩm trong "Bạn vừa xem" (3 trừ đi sản phẩm đang xem).
- `TTL demo:recent:admin` ra khoảng `300` giây. Hạn mới chỉ áp dụng **từ lần xem tiếp theo**, vì `expire` được đặt lại mỗi lần xem. Nếu vừa restart mà chưa xem gì thì key cũ vẫn giữ hạn 30 ngày.
- Thử gõ sai tên key thành `app.stats.recent-tll=5m` rồi lưu lại: app **không khởi động** được, log báo `Required key 'app.stats.recent-ttl' not found`. Nhờ đọc cấu hình bằng `getRequiredProperty` trong constructor (Bước 2 ④), lỗi lộ ra ngay lúc khởi động.
- Thử viết `app.stats.recent-ttl="5m"` (có dấu nháy): app cũng không khởi động, báo `Failed to convert from type [java.lang.String] to type [java.time.Duration] for value ["5m"]`.
- Thử ghi đè mà không sửa file: thêm `APP_STATS_TOPLIMIT: 3` vào phần `environment:` của `app-dev` trong `docker-compose.dev.yml`, rồi `up -d` lại. Biến môi trường thắng file `config/` (Bước 2 ③). Thử xong nhớ xoá dòng đó.

Xong thì trả về `30d` / `5` / `5`.

---

## Lỗi thường gặp

| Triệu chứng | Nguyên nhân | Cách sửa |
|-------------|-------------|----------|
| Trang danh sách lỗi `EL1011E: Method call: Attempted to call method getOrDefault(...)` (hoặc `isEmpty()`) `on null context object` | Quên `model.addAttribute("views", ...)` (hoặc `"topProducts"`) trong `list()` | Xem Bước 5 |
| App không khởi động: `The dependencies of some of the beans form a cycle` | `ProductStatsService` nhận `ProductService` (hoặc `ProductRepository`) vào constructor | `ProductStatsService` chỉ được nhận `StringRedisTemplate` và `AppConfig`. Xem ghi chú cuối Bước 4 |
| Xem mãi mà khung Top không hiện | Bạn đang mở trang **Sửa** (`/edit`), không phải trang chi tiết | Bấm vào **tên** sản phẩm |
| Thứ tự "Bạn vừa xem" hoặc khung Top bị lộn xộn | Trong `findAllInOrder()`, bạn trả thẳng kết quả của `findAllById` | Phải đi theo vòng lặp `ids`, xem Bước 4 |
| Khi tắt Redis, trang treo khoảng 2 phút rồi mới hiện | Thiếu `timeout`, hoặc ghi nhầm dưới `spring.redis` | Xem Bước 1 |
| Khi tắt Redis, trang ra **500** thay vì hiện `0` | Thiếu `try/catch` ở một phương thức nào đó trong `ProductStatsService` | Mọi phương thức public đều phải có `try/catch (DataAccessException e)` |
| Trang ra 500 `NumberFormatException: For input string: "abc"` | Bạn tự `ZADD demo:product:views 5 abc` trong `redis-cli`, mà member phải là id số | `ZREM demo:product:views abc`. Lỗi này **không** bị `catch` bắt, vì nó không phải lỗi của Redis mà là dữ liệu sai, đúng như mong muốn |
| `LRANGE demo:recent:admin 0 -1` trả về rỗng | Bạn đăng nhập bằng user khác `admin` | Chạy `KEYS demo:recent:*` để xem tên key thật |
| Mô tả hiện dồn thành một dòng | Thiếu class `description` hoặc thiếu CSS `white-space: pre-line` | Xem Bước 8 |
| App không khởi động: `Required key 'app.stats.recent-ttl' not found` (hoặc `app.upload.dir`...), log **không có** dòng `[Config] đã nạp ...` nào | `ModuleConfigLoader` không chạy: thiếu `META-INF/spring.factories`, sai tên thư mục (`meta-inf`), hoặc gõ sai tên class trong file đó | Xem Bước 2 ③ |
| App không khởi động: `Required key '...' not found`, log **có** dòng `[Config]` | Tên key trong file `.properties` khác với tên gọi trong Java (`recent-tll` và `recent-ttl`), hoặc file nằm sai chỗ (phải là `src/main/resources/config/`) | So tên key ở 2 nơi. Dòng `[Config] đã nạp config/stats.properties (3 key)` cho biết file nào đã được nạp, mỗi file mấy key |
| App không khởi động: `Failed to convert from type [java.lang.String] to type [java.time.Duration] for value ["30d"]` | Viết dấu nháy như file `.ini`: `recent-ttl="30d"` | Bỏ dấu nháy: `app.stats.recent-ttl=30d` |
| App không khởi động: `Failed to convert ... to type [java.time.Duration] for value [30days]` | Sai định dạng thời gian, ví dụ `30 ngày`, `30days` | Chỉ dùng các đơn vị `ms`, `s`, `m`, `h`, `d`, viết liền: `30d` |
| "Bạn vừa xem" lúc nào cũng trống, `TTL demo:recent:admin` ra `-2` (key không tồn tại) | Viết `app.stats.recent-ttl=30`, không có đơn vị → 30 **mili giây** | Ghi kèm đơn vị: `30d`. Xem Bước 2 ① |
| Sửa `config/upload.properties` (hoặc `product.properties`) mà giá trị không đổi | Vẫn còn khối `app:` cũ trong `application.yml`, mà yml được ưu tiên hơn | Xoá khối `app:` trong yml. Xem Bước 2 ② |

---

## Bài tập

1. **Chống bơm lượt xem bằng F5**: hiện cứ F5 là +1. Hãy làm sao để mỗi user chỉ được tính 1 lượt cho mỗi sản phẩm trong 10 phút. Gợi ý: dùng kiểu **String** với `redisTemplate.opsForValue().setIfAbsent("demo:viewed:" + username + ":" + id, "1", Duration.ofMinutes(10))`. Lệnh này trả về `true` nếu key chưa có (vừa được tạo), và chỉ khi đó mới `incrementScore`. Câu hỏi: vì sao phải dùng **một** lệnh `setIfAbsent`, mà không viết `hasKey()` rồi mới `set()`? (Gợi ý: xem lại đoạn "Vì sao không `get` rồi `set`" ở Bước 3.) Đừng viết cứng 10 phút: thêm `app.stats.view-cooldown=10m` vào `config/stats.properties`, rồi đọc bằng `config.getDuration(...)` trong constructor.
2. **Lượt xem trong REST API**: thêm `GET /api/products/{id}/views` trả về số lượt xem, dùng `opsForZSet().score(...)` (lệnh `ZSCORE`). Có nên đếm lượt xem khi ai đó gọi `GET /api/products/{id}` không? Vì sao?
3. **Top trong 7 ngày gần nhất**: thay vì một key duy nhất, mỗi ngày dùng một key `demo:product:views:2026-10-28`, và đặt `EXPIRE` 8 ngày cho nó. Top 7 ngày thì gộp 7 key lại bằng `opsForZSet().unionAndStore(...)` (lệnh `ZUNIONSTORE`). Thử nghĩ xem: làm việc này bằng cột `views` trong MySQL thì sẽ khó ở chỗ nào?
4. **Nút "Xoá lịch sử vừa xem"**: thêm một nút trên trang chi tiết để xoá key `demo:recent:<user>` (`redisTemplate.delete(...)`, lệnh `DEL`). Vì sao nút này nên là `<form method="post">` chứ không phải thẻ `<a>`? (Gợi ý: nút Đăng xuất trong `layout.html`.)
5. **Thử độ bền dữ liệu**: chạy `docker restart demo-redis`. Lượt xem còn không? Sau đó chạy `docker compose -f docker-compose.dev.yml down -v` rồi `up -d` lại. Lần này thì sao? Giải thích sự khác nhau dựa vào volume `redis_data` (bài 14).
6. **Tìm hiểu**: `recordView()` gửi 5 lệnh, tức là 5 lần đi về giữa app và Redis. Tìm hiểu `redisTemplate.executePipelined(...)` (gửi nhiều lệnh trong 1 lần) và `MULTI` / `EXEC` (transaction của Redis). Hai cách này khác nhau thế nào? Khi nào thì cần đến chúng?
7. **So sánh với MySQL**: thêm thử cột `views` vào `Product` và `UPDATE product SET views = views + 1` mỗi lần xem (xem phần Tư vấn). Bật `show-sql` (đã bật sẵn), mở trang chi tiết rồi đếm xem mỗi lượt xem tạo ra bao nhiêu câu SQL. Làm xong nhớ xoá cột đi (`ddl-auto: update` không tự xoá cột, xem bài 15 phần `@Column`).
8. **Danh sách đuôi ảnh vào cấu hình**: `FileStorageService` vẫn còn viết cứng `Set.of("jpg", "jpeg", "png", "gif", "webp")`. Chuyển nó vào `config/upload.properties` thành `app.upload.allowed-extensions=jpg,jpeg,png,gif,webp`. Thêm vào `AppConfig` một hàm dùng chung `public Set<String> getStringSet(String key)`. Gợi ý: `env.getRequiredProperty(key, String[].class)` tự tách chuỗi theo dấu phẩy, rồi `Set.of(...)` mảng đó. Thử bỏ `gif` khỏi `config/upload.properties` rồi upload một ảnh `.gif`: phải bị từ chối. Câu hỏi: vì sao `URL_PREFIX = "/uploads/"` thì **không nên** đưa ra cấu hình? (Gợi ý: xem những class nào khác đang dùng nó.)

➡️ Bài sau: [**16.5 — Nâng cấp lên Spring Boot 4.1 và sửa các cú pháp đã bị khai tử**](16.5-nang-cap-spring-boot-4.md), rồi [**17 — Viết code gọn hơn bằng Lombok**](17-lombok-slf4j-getter-setter.md). Sau đó là **18 — Đánh giá sản phẩm bằng MongoDB** (`MongoRepository`, document lồng nhau, tính điểm trung bình).

➡️ Quay lại tổng quan: [../README.md](../../README.md)
