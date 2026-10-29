# 14 — Cài môi trường MongoDB + Redis ở local cho dự án Spring

Tới giờ project chỉ có **một** chỗ lưu dữ liệu là MySQL. Dự án thật hay dùng thêm:

- **MongoDB**: DB dạng **document** (JSON), hợp với dữ liệu có cấu trúc linh hoạt như đánh giá sản phẩm, log, catalog.
- **Redis**: kho **key → value trong RAM**, dùng làm cache, session, hàng đợi, bộ đếm.

Bài này **chỉ dựng môi trường**, chưa viết tính năng nào: thêm 2 container mới vào Docker, cho Spring Boot kết nối được tới cả hai, rồi kiểm tra kết nối bằng 3 cách. Các bài sau mới dùng tới chúng (cache sản phẩm bằng Redis, lưu đánh giá bằng MongoDB...).

Mục tiêu khi xong bài:

- `docker compose ... up` chạy **4 container**: `demo-mysql`, `demo-mongo`, `demo-redis`, app.
- `http://localhost:8093/actuator/health` báo cả `db` (MySQL), `mongo`, `redis` đều **`UP`**.
- Vào được shell của MongoDB (`mongosh`) và Redis (`redis-cli`) để tự xem dữ liệu.

> Kiến thức mới: thêm service vào `docker-compose`, **healthcheck** cho từng DB, starter `spring-boot-starter-data-mongodb` / `spring-boot-starter-data-redis`, `MongoTemplate` và `StringRedisTemplate`, xem chi tiết `/actuator/health`.

---

## Ý tưởng

### Ba DB, ba vai trò, chạy song song

MongoDB và Redis **không thay MySQL**. Cả ba cùng chạy, mỗi cái lo một việc:

```
                        ┌──────────────────────────────┐
                        │   Spring Boot app (8080)     │
                        └──┬───────────┬────────────┬──┘
              JPA          │           │            │   Spring Data Redis
         (Repository)      │           │ Spring Data│   (StringRedisTemplate)
                           ▼           ▼ MongoDB    ▼
                   ┌──────────┐ ┌───────────┐ ┌───────────┐
                   │  MySQL   │ │  MongoDB  │ │   Redis   │
                   │ "mysql"  │ │  "mongo"  │ │  "redis"  │
                   │  :3306   │ │  :27017   │ │  :6379    │
                   └──────────┘ └───────────┘ └───────────┘
  Dữ liệu chính: user,         Dữ liệu dạng       Dữ liệu tạm, cần nhanh:
  sản phẩm, danh mục           document           cache, đếm, session
```

Trong mạng Docker, app gọi các DB bằng **tên service** (`mysql`, `mongo`, `redis`), giống hệt cách đang gọi MySQL (xem lại `DB_HOST: mysql` trong `docker-compose.yml`).

### Cổng mở ra máy bạn

Giống MySQL (`3313 → 3306`), ta **đổi cổng ở phía máy bạn** để không đụng với MongoDB/Redis cài sẵn, ví dụ Redis của các dự án Laravel cũ:

| Service | Trong mạng Docker (app dùng) | Từ máy bạn (Compass, RedisInsight...) |
|---|---|---|
| MySQL | `mysql:3306` | `localhost:3313` |
| MongoDB | `mongo:27017` | `localhost:27018` |
| Redis | `redis:6379` | `localhost:6380` |

### Các file sẽ đụng tới

```
docker-compose.yml          ⬅️ SỬA  — thêm service mongo, redis
docker-compose.dev.yml      ⬅️ SỬA  — thêm y hệt
.env                        ⬅️ SỬA  — (tuỳ chọn) thêm biến MONGO_*
build.gradle                ⬅️ SỬA  — thêm 2 starter
src/main/resources/application.yml   ⬅️ SỬA  — cấu hình kết nối + health
com/example/demo/
└── nosql/
    └── NoSqlConnectionCheck.java    ⬅️ MỚI  — thử ghi/đọc lúc khởi động
```

---

## Bước 1 — Thêm MongoDB và Redis vào `docker-compose.dev.yml`

Làm file dev trước vì đây là file bạn dùng hằng ngày. Thêm 2 service mới **ngay dưới service `mysql`**, cùng cấp với nó:

```yaml
  # ----- MongoDB (DB dạng document) -----
  mongo:
    image: mongo:8.0
    container_name: demo-mongo
    restart: unless-stopped
    environment:
      # Tạo user "root" của MongoDB ở lần khởi động ĐẦU TIÊN (khi volume còn trống).
      # User này nằm trong database "admin" → URI kết nối phải có ?authSource=admin (Bước 4)
      MONGO_INITDB_ROOT_USERNAME: ${MONGO_USERNAME:-demo_user}
      MONGO_INITDB_ROOT_PASSWORD: ${MONGO_PASSWORD:-demo_pass}
    ports:
      # 27018 ở máy bạn -> 27017 trong container (tránh đụng MongoDB cài sẵn nếu có)
      - "27018:27017"
    volumes:
      - mongo_data:/data/db
    healthcheck:
      # Lệnh "ping" của MongoDB, không cần đăng nhập
      test: ["CMD", "mongosh", "--quiet", "--eval", "db.adminCommand('ping')"]
      interval: 5s
      timeout: 5s
      retries: 10

  # ----- Redis (key-value trong RAM) -----
  redis:
    image: redis:7.4
    container_name: demo-redis
    restart: unless-stopped
    ports:
      # 6380 ở máy bạn -> 6379 trong container (tránh đụng Redis của dự án khác)
      - "6380:6379"
    volumes:
      # Redis định kỳ chụp dữ liệu RAM xuống /data → restart container không mất sạch
      - redis_data:/data
    healthcheck:
      # redis-cli ping → trả "PONG" là sống
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 5s
      retries: 10
```

Sửa tiếp service **`app-dev`** ở 2 chỗ.

**`depends_on`**: app chỉ khởi động khi cả 3 DB đã sẵn sàng:

```yaml
    depends_on:
      mysql:
        condition: service_healthy
      mongo:
        condition: service_healthy
      redis:
        condition: service_healthy
```

**`environment`**: thêm các biến mới, giữ nguyên các biến `DB_*` đang có:

```yaml
    environment:
      DB_HOST: mysql
      # ... (các biến DB_* và GRADLE_USER_HOME giữ nguyên)
      # "mongo", "redis" là tên service ở trên -> chính là hostname trong mạng docker
      MONGO_HOST: mongo
      MONGO_PORT: 27017
      MONGO_DB: ${MONGO_DB:-demo_mongo}
      MONGO_USERNAME: ${MONGO_USERNAME:-demo_user}
      MONGO_PASSWORD: ${MONGO_PASSWORD:-demo_pass}
      REDIS_HOST: redis
      REDIS_PORT: 6379
```

Cuối file, khai báo thêm 2 volume:

```yaml
volumes:
  mysql_data:
  gradle_cache:
  mongo_data:
  redis_data:
```

### Vì sao app lại dùng cổng `27017`, `6379` chứ không phải `27018`, `6380`?

Phần `ports: "27018:27017"` chỉ là **cửa mở ra máy bạn**. App chạy **bên trong** mạng Docker, nói chuyện thẳng với container `mongo` ở cổng gốc `27017`, không đi qua cửa đó. Đây cũng là lý do `DB_PORT` của MySQL là `3306` chứ không phải `3313`.

### Redis không có mật khẩu, có sao không?

Với máy local thì **chấp nhận được**: chỉ máy bạn kết nối được tới cổng `6380`. Trên server thật thì **bắt buộc** phải đặt mật khẩu (`--requirepass`) và không mở cổng Redis ra Internet. Rất nhiều server đã bị chiếm quyền chỉ vì để Redis trần ra ngoài.

---

## Bước 2 — Làm y hệt cho `docker-compose.yml`

Hai file compose **dùng chung container name** (bài 04), nên phải giữ chúng giống nhau:

1. Copy nguyên 2 service `mongo`, `redis` ở Bước 1 vào `docker-compose.yml`, dưới service `mysql`.
2. Trong service **`app`**, thêm `mongo`, `redis` vào `depends_on` và thêm các biến `MONGO_*`, `REDIS_*` vào `environment`, y như Bước 1.
3. Thêm `mongo_data:` và `redis_data:` vào phần `volumes:` cuối file.

(Tuỳ chọn) Thêm vào `.env` để dễ đổi sau này:

```env
MONGO_DB=demo_mongo
MONGO_USERNAME=demo_user
MONGO_PASSWORD=demo_pass
```

### Vì sao database MongoDB tên `demo_mongo` mà không dùng luôn `demo_db`?

Về kỹ thuật, đặt trùng `demo_db` với MySQL **vẫn chạy bình thường**: đây là hai server DB riêng biệt, không bao giờ đụng nhau. Ngoài thực tế cũng có nhiều team đặt trùng, theo tên ứng dụng.

Nhưng nhầm lẫn thường không xảy ra trong code (`jdbc:mysql://` và `mongodb://` nhìn là biết). Nó xảy ra ở những chỗ **chỉ còn trơ lại cái tên**:

- Khi trao đổi: "dữ liệu nằm trong `demo_db`", vậy là MySQL hay MongoDB?
- Trong file backup, tool giám sát, dashboard: chỉ hiện `demo_db`, không hiện loại DB.
- Khi `grep demo_db`: kết quả lẫn lộn cả hai.

Đặt khác tên **không tốn gì** mà bớt được một chỗ dễ nhầm cho người vào sau, kể cả chính bạn vài tháng nữa. Có 3 cách đặt hay gặp:

| Cách | Ví dụ | Nhận xét |
|---|---|---|
| Theo **công nghệ** ⬅️ bài này | `demo_db` / `demo_mongo` | Rõ nhất. Nếu sau này đổi công nghệ thì tên sẽ sai, nhưng hiếm khi xảy ra |
| Theo **mục đích** | `demo_db` / `demo_reviews` | Hay gặp ở dự án lớn. Cần biết trước MongoDB sẽ lưu gì |
| Theo **môi trường** (hậu tố) | `demo_dev`, `demo_prod` | Kết hợp được với 2 cách trên |

> **Tên biến môi trường cũng vậy.** Khi chỉ có MySQL, `DB_NAME`, `DB_HOST` là đủ rõ. Giờ có thêm MongoDB, Redis (cũng là DB) thì chữ `DB_` trở nên mơ hồ. Vì thế các biến mới trong bài này đều có tiền tố theo công nghệ (`MONGO_*`, `REDIS_*`). Đổi `DB_*` thành `MYSQL_*` cho đồng bộ là việc nên làm, nhưng phải sửa nhiều file (`application.yml`, 2 file compose, `.env`), nên để thành bài tập 6 cuối bài.

---

## Bước 3 — Thêm thư viện vào `build.gradle`

Thêm vào khối `dependencies`, cạnh dòng `spring-boot-starter-data-jpa`:

```groovy
    // MongoDB: làm việc với DB dạng document (MongoTemplate, MongoRepository)
    implementation 'org.springframework.boot:spring-boot-starter-data-mongodb'
    // Redis: làm việc với Redis (RedisTemplate, cache)
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```

Không cần ghi số phiên bản: plugin `io.spring.dependency-management` tự chọn phiên bản khớp với Spring Boot 3.4.1, giống như các starter khác.

Hai starter này **đã kèm sẵn driver**: `spring-boot-starter-data-mongodb` có MongoDB Java Driver, `spring-boot-starter-data-redis` có thư viện **Lettuce**. Không cần thêm dòng `runtimeOnly` nào như `mysql-connector-j`.

> Dùng VSCode: sau khi sửa `build.gradle`, bấm **"Reload Gradle project"** (hoặc `Ctrl+Shift+P` → `Java: Reload Projects`). Nếu không, các `import org.springframework.data.mongodb...` ở Bước 5 sẽ bị gạch đỏ dù code vẫn đúng.

---

## Bước 4 — Cấu hình kết nối trong `application.yml`

Thêm khối `data:` **bên trong `spring:`**, cùng cấp với `datasource:` và `jpa:`:

```yaml
spring:
  application:
    name: demo

  datasource:
    # ... (giữ nguyên)

  jpa:
    # ... (giữ nguyên)

  # Cấu hình MongoDB + Redis. Giống datasource: lấy từ biến môi trường, có giá trị mặc định.
  data:
    mongodb:
      # mongodb://user:pass@host:port/tên_database?authSource=admin
      # authSource=admin: user root do MONGO_INITDB_ROOT_* tạo ra nằm trong database "admin",
      # không nằm trong demo_mongo → thiếu tham số này là báo "Authentication failed"
      uri: mongodb://${MONGO_USERNAME:demo_user}:${MONGO_PASSWORD:demo_pass}@${MONGO_HOST:localhost}:${MONGO_PORT:27017}/${MONGO_DB:demo_mongo}?authSource=admin
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
```

Sửa tiếp khối `management:` ở cuối file để `/actuator/health` **hiện chi tiết từng DB**:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      # Mặc định chỉ trả {"status":"UP"}. "always" → liệt kê trạng thái từng thành phần: db, mongo, redis...
      show-details: always
```

### ⚠️ Đọc tài liệu cũ trên mạng thì chú ý tên thuộc tính

Từ **Spring Boot 3.0**, cấu hình Redis đã chuyển từ `spring.redis.*` sang **`spring.data.redis.*`**. Rất nhiều bài hướng dẫn cũ vẫn viết `spring.redis.host`. Nếu chép theo, Spring sẽ **lặng lẽ bỏ qua** và kết nối tới `localhost:6379` mặc định. Trong container, `localhost` là chính container app, nên kết nối thất bại.

### `show-details: always` có an toàn không?

`/actuator/health` đang được `permitAll()` trong `SecurityConfig` (bài 12), nên **ai cũng xem được** chi tiết: phiên bản MySQL, Redis... Với project học tập thì không sao. Dự án thật nên dùng `show-details: when-authorized`, chỉ hiện chi tiết cho người đã đăng nhập.

---

## Bước 5 — Thử ghi/đọc lúc khởi động: `NoSqlConnectionCheck`

### Vì sao cần bước này?

Có một điều bất ngờ: **app vẫn khởi động thành công dù MongoDB hoặc Redis đang tắt.** Khác với MySQL (Hibernate cần kết nối ngay để tạo bảng), driver MongoDB và Redis kết nối **"lười"**: chỉ thực sự kết nối khi code dùng tới lần đầu. Vì vậy cấu hình sai có thể nằm im rất lâu, đến khi có người bấm vào đúng tính năng đó mới lộ ra lỗi.

Ta dùng lại `CommandLineRunner` của bài 13 để **thử ghi và đọc lại** mỗi lần app khởi động. Sai ở đâu là biết ngay trong log.

Tạo package `nosql/` cạnh `product/`, `seed/`..., rồi tạo `nosql/NoSqlConnectionCheck.java`:

```java
package com.example.demo.nosql;

import java.time.Duration;
import java.time.LocalDateTime;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

// Chạy 1 lần sau khi app khởi động (giống DataSeeder ở bài 13):
// thử ghi rồi đọc lại MongoDB và Redis, in kết quả ra log
@Component
public class NoSqlConnectionCheck implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(NoSqlConnectionCheck.class);

    // Cả 2 bean này do Spring Boot TỰ TẠO nhờ 2 starter ở Bước 3, ta chỉ việc xin qua constructor
    private final MongoTemplate mongoTemplate;
    private final StringRedisTemplate redisTemplate;

    public NoSqlConnectionCheck(MongoTemplate mongoTemplate, StringRedisTemplate redisTemplate) {
        this.mongoTemplate = mongoTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void run(String... args) {
        checkMongo();
        checkRedis();
    }

    private void checkMongo() {
        try {
            // Document: 1 "bản ghi" của MongoDB, dùng như một Map<String, Object>
            // _id cố định → mỗi lần chạy GHI ĐÈ lên đúng document này, không sinh thêm (idempotent, bài 13)
            Document doc = new Document("_id", "last-startup")
                    .append("startedAt", LocalDateTime.now().toString());

            // Collection "startup_log" chưa có? MongoDB TỰ TẠO khi ghi lần đầu, không cần CREATE TABLE
            mongoTemplate.save(doc, "startup_log");

            Document saved = mongoTemplate.findById("last-startup", Document.class, "startup_log");
            log.info("[NoSQL] MongoDB OK, database = {}, startup_log = {}",
                    mongoTemplate.getDb().getName(), saved.toJson());
        } catch (Exception e) {
            // Bắt lỗi để app VẪN chạy tiếp (MySQL vẫn dùng được), chỉ báo đỏ trong log
            log.error("[NoSQL] Không kết nối được MongoDB: {}", e.getMessage());
        }
    }

    private void checkRedis() {
        try {
            // SET demo:last-startup "<thời gian>" EX 600  → key tự xoá sau 10 phút
            redisTemplate.opsForValue().set("demo:last-startup",
                    LocalDateTime.now().toString(), Duration.ofMinutes(10));

            String value = redisTemplate.opsForValue().get("demo:last-startup");
            log.info("[NoSQL] Redis OK, demo:last-startup = {}", value);
        } catch (Exception e) {
            log.error("[NoSQL] Không kết nối được Redis: {}", e.getMessage());
        }
    }
}
```

### `MongoTemplate` và `StringRedisTemplate` là gì?

Là 2 object "điều khiển từ xa" mà Spring Boot tự tạo sẵn khi thấy starter tương ứng trong `build.gradle`. Mọi thao tác với MongoDB/Redis đều có thể đi qua chúng:

| Bạn muốn | MongoDB (`MongoTemplate`) | Redis (`StringRedisTemplate`) |
|---|---|---|
| Ghi | `save(doc, "collection")` | `opsForValue().set(key, value)` |
| Đọc | `findById(id, Document.class, "collection")` | `opsForValue().get(key)` |
| Tương đương lệnh tay | `db.collection.find({_id: ...})` | `GET key` |

`StringRedisTemplate` là bản chuyên cho key/value **dạng chuỗi**, nên dữ liệu trong `redis-cli` đọc được bằng mắt thường. `RedisTemplate` thường (không có chữ `String`) mặc định lưu dữ liệu dạng nhị phân của Java, xem bằng `redis-cli` chỉ thấy ký tự lạ.

Bài sau sẽ dùng cách tiện hơn: `MongoRepository` (giống hệt `JpaRepository`) và `@Cacheable`. Ở đây dùng template vì chỉ cần thử kết nối, chưa cần tạo entity.

### Vì sao `try/catch` ở đây, trong khi project quy định "Service ném `RuntimeException`"?

Đây không phải nghiệp vụ, mà là **công cụ chẩn đoán**. Nếu để exception bay ra khỏi `run()`, **cả app sẽ dừng**: chỉ vì Redis chưa bật mà không vào được cả trang sản phẩm (đang chạy bằng MySQL). Bắt lỗi rồi ghi `log.error` thì vừa biết lỗi, vừa vẫn làm việc tiếp được.

---

## Bước 6 — Chạy

Đã sửa `build.gradle` và compose nên **phải khởi động lại**: DevTools không tự tải thư viện mới được (bài 04).

```bash
# Dev
docker compose -f docker-compose.dev.yml up -d
docker compose -f docker-compose.dev.yml logs -f app-dev
```

`up -d` sẽ tự **tạo thêm** `demo-mongo`, `demo-redis`, và **tạo lại** `demo-app-dev` (vì `environment` đã đổi). MySQL không bị đụng tới, dữ liệu cũ còn nguyên. Lần đầu sẽ hơi lâu vì phải tải image `mongo:8.0`, `redis:7.4` và các thư viện Gradle mới.

Với chế độ thường thì `docker compose up --build`. Nhớ `down` chế độ kia trước, vì hai file dùng chung container name.

Kiểm tra cả 4 container đều chạy và DB đều `healthy`:

```bash
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

```
NAMES          STATUS                    PORTS
demo-app-dev   Up 40 seconds             0.0.0.0:8093->8080/tcp
demo-mongo     Up 52 seconds (healthy)   0.0.0.0:27018->27017/tcp
demo-redis     Up 52 seconds (healthy)   0.0.0.0:6380->6379/tcp
demo-mysql     Up 2 hours (healthy)      0.0.0.0:3313->3306/tcp
```

---

## Bước 7 — Kiểm tra theo 3 cách

### Cách 1: đọc log khởi động

Ngay sau dòng `Started DemoApplication` phải có:

```
Started DemoApplication in 18.204 seconds ...
... c.e.demo.nosql.NoSqlConnectionCheck : [NoSQL] MongoDB OK, database = demo_mongo, startup_log = {"_id": "last-startup", "startedAt": "2026-10-20T08:15:42.123"}
... c.e.demo.nosql.NoSqlConnectionCheck : [NoSQL] Redis OK, demo:last-startup = 2026-10-20T08:15:42.456
```

(Seeder của bài 13 vẫn in log `[Seed]` như cũ.) Thời gian là giờ **UTC** của container, sớm hơn giờ Việt Nam 7 tiếng. Đây là chuyện bình thường.

Bạn cũng sẽ thấy dòng này ở đầu log:

```
Multiple Spring Data modules found, entering strict repository configuration mode
```

**Không phải lỗi.** Project giờ có 3 "module" Spring Data (JPA, MongoDB, Redis), nên Spring phải xác định mỗi Repository thuộc về DB nào. Nó dựa vào interface cha: `ProductRepository extends JpaRepository` → MySQL. Sau này viết repository cho MongoDB thì phải `extends MongoRepository`, đừng dùng `CrudRepository` chung chung, không thì Spring không biết gán cho DB nào.

### Cách 2: `/actuator/health`

```bash
curl -s http://localhost:8093/actuator/health
```

Kết quả (đã xuống dòng cho dễ đọc, các phần không liên quan thay bằng `...`):

```json
{
  "status": "UP",
  "components": {
    "db":    { "status": "UP", "details": { "database": "MySQL", ... } },
    "mongo": { "status": "UP", "details": { "maxWireVersion": ... } },
    "redis": { "status": "UP", "details": { "version": "7.4.x" } },
    "diskSpace": { ... },
    "ping":  { "status": "UP" }
  }
}
```

Bạn **không phải viết** gì cho `mongo` và `redis` hiện ở đây: Actuator tự thêm "health indicator" khi thấy starter tương ứng.

**Thử làm hỏng một cái:**

```bash
docker stop demo-redis
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8093/actuator/health   # → 503
curl -s http://localhost:8093/actuator/health                                    # redis: DOWN, tổng: DOWN
docker start demo-redis                                                          # vài giây sau → UP, 200
```

Chỉ cần **1 thành phần `DOWN`** là cả `status` thành `DOWN`, HTTP trả **503**. Docker, Kubernetes, load balancer... đều dựa vào mã này để biết app có khoẻ không. Trong lúc Redis tắt, trang `/products` **vẫn chạy bình thường** vì chưa có tính năng nào dùng Redis.

### Cách 3: vào tận nơi xem dữ liệu

**MongoDB**: dùng `mongosh` có sẵn trong container:

```bash
docker exec -it demo-mongo mongosh -u demo_user -p demo_pass --authenticationDatabase admin demo_mongo
```

```js
show collections                     // → startup_log   (do Bước 5 tạo)
db.startup_log.find()                // → { _id: 'last-startup', startedAt: '...' }
db.startup_log.countDocuments()      // → 1   (restart bao nhiêu lần vẫn là 1, nhờ _id cố định)
exit
```

`--authenticationDatabase admin` chính là `?authSource=admin` trong URI ở Bước 4, chỉ là viết dạng tham số dòng lệnh.

**Redis**: dùng `redis-cli` có sẵn trong container:

```bash
docker exec -it demo-redis redis-cli
```

```
127.0.0.1:6379> KEYS demo:*
1) "demo:last-startup"
127.0.0.1:6379> GET demo:last-startup
"2026-10-20T08:15:42.456"
127.0.0.1:6379> TTL demo:last-startup
(integer) 587          ← còn 587 giây nữa key tự biến mất
127.0.0.1:6379> MONITOR
OK                     ← giờ lưu 1 file .java cho app restart, sẽ thấy lệnh SET/GET app gửi tới
```

Bấm `Ctrl + C` để thoát `MONITOR`, gõ `exit` để ra.

> `KEYS` quét **toàn bộ** key, nên trên server thật có hàng triệu key thì lệnh này có thể làm Redis đứng hình. Chỉ dùng ở local. Trên production người ta dùng `SCAN`.

**Công cụ có giao diện** (cài trên máy bạn, kết nối qua cổng đã mở ra ngoài):

| Công cụ | Chuỗi kết nối |
|---|---|
| **MongoDB Compass** | `mongodb://demo_user:demo_pass@localhost:27018/?authSource=admin` |
| **RedisInsight** | Host `localhost`, port `6380`, không có mật khẩu |

---

## Chạy app ngay trên máy (không Docker) thì sao?

Nếu chạy `./gradlew bootRun` trên máy có JDK 21, app sẽ dùng **giá trị mặc định** trong `application.yml` (`localhost:27017`, `localhost:6379`). Nhưng cổng mở ra máy bạn là `27018` / `6380`, nên phải truyền biến môi trường:

```bash
DB_PORT=3313 MONGO_PORT=27018 REDIS_PORT=6380 ./gradlew bootRun
```

Cách làm giống hệt MySQL (`DB_PORT=3313`), không có gì mới.

---

## Làm lại từ đầu

```bash
docker compose -f docker-compose.dev.yml down -v     # ⚠️ xoá SẠCH dữ liệu của CẢ 3 DB
docker compose -f docker-compose.dev.yml up -d
```

Chỉ muốn xoá dữ liệu MongoDB mà giữ MySQL (ví dụ khi đổi `MONGO_PASSWORD`, xem bảng lỗi bên dưới):

```bash
docker compose -f docker-compose.dev.yml rm -sf mongo
docker volume ls | grep mongo                  # tìm đúng tên, thường là java_code_mongo_data
docker volume rm java_code_mongo_data
docker compose -f docker-compose.dev.yml up -d
```

Tên volume có tiền tố là **tên thư mục project** (`java_code`), do Docker Compose tự thêm.

---

## Lỗi thường gặp

| Triệu chứng | Nguyên nhân | Cách sửa |
|-------------|-------------|----------|
| Log `[NoSQL] Không kết nối được MongoDB: ... Authentication failed` | Thiếu `?authSource=admin` trong `uri` | Thêm lại như Bước 4 |
| Vẫn `Authentication failed` dù URI đúng | Volume `mongo_data` đã được tạo **từ trước** với user/mật khẩu khác. `MONGO_INITDB_ROOT_*` **chỉ có tác dụng ở lần đầu**, khi volume còn trống | Xoá volume MongoDB (mục "Làm lại từ đầu") |
| Log `Exception in monitor thread while connecting to server localhost:27017` | App đang đọc giá trị mặc định `localhost`: thiếu `MONGO_HOST: mongo` trong `environment` | Thêm biến ở Bước 1/2, rồi `up -d` lại |
| Log `Unable to connect to Redis` / `Connection refused: localhost/...:6379` | Thiếu `REDIS_HOST: redis`, **hoặc** viết `spring.redis.host` (tên cũ) thay vì `spring.data.redis.host` | Kiểm tra Bước 1 và Bước 4 |
| App khởi động OK nhưng không có dòng log `[NoSQL]` nào | Thiếu `@Component` hoặc `implements CommandLineRunner` | Kiểm tra Bước 5 |
| `import org.springframework.data.mongodb...` bị gạch đỏ trong VSCode | VSCode chưa đọc lại `build.gradle` | `Ctrl+Shift+P` → `Java: Reload Projects` |
| Log `NoSuchBeanDefinitionException: ... MongoTemplate` | Thêm dependency vào `build.gradle` nhưng chưa khởi động lại container `app-dev` | `docker compose -f docker-compose.dev.yml up -d` (hoặc `restart app-dev`) |
| `up` báo `Bind for 0.0.0.0:6380 failed: port is already allocated` | Một chương trình khác đang chiếm cổng | Đổi cổng bên trái, ví dụ `"6381:6379"`. App **không cần sửa** vì nó dùng cổng `6379` bên trong |
| `demo-mongo` liên tục restart, log có `Illegal instruction` hoặc exit code `132` | CPU quá cũ, không có tập lệnh AVX mà MongoDB 5.0+ yêu cầu | Đổi `image: mongo:4.4`, **kèm** đổi `mongosh` thành `mongo` trong healthcheck và lệnh `docker exec` (bản 4.4 chưa có `mongosh`) |
| `/actuator/health` chỉ trả `{"status":"UP"}`, không có `components` | Thiếu `show-details: always`, hoặc đặt sai chỗ (phải nằm dưới `management.endpoint.health`, **không có** chữ `s`) | Kiểm tra Bước 4 |
| `docker compose up` báo `container name "/demo-mongo" is already in use` | Chế độ kia (dev/thường) vẫn đang chạy | `down` file compose kia trước |

## Bài tập

1. **Đặt mật khẩu cho Redis**: thêm vào service `redis`:
   ```yaml
   command: ["redis-server", "--requirepass", "${REDIS_PASSWORD:-redis_pass}"]
   ```
   rồi sửa healthcheck thành `["CMD", "redis-cli", "-a", "${REDIS_PASSWORD:-redis_pass}", "ping"]`. Thêm `REDIS_PASSWORD` vào `environment` của app và `password: ${REDIS_PASSWORD:}` dưới `spring.data.redis` trong `application.yml`. Chạy lại: lúc chưa thêm `password` vào `application.yml`, log `[NoSQL]` báo lỗi gì?
2. **Redis bền hơn**: mặc định Redis chỉ chụp dữ liệu xuống đĩa **định kỳ**, nên tắt đột ngột có thể mất vài phút dữ liệu cuối. Thêm `--appendonly yes` vào `command`, rồi vào container xem thư mục `/data` có thêm gì (`docker exec demo-redis ls -R /data`). Tìm hiểu: RDB và AOF khác nhau thế nào?
3. **Đếm số lần khởi động**: trong `checkRedis()`, thêm `redisTemplate.opsForValue().increment("demo:startup-count")` và in giá trị ra log. Lưu file vài lần cho app restart, xem số tăng dần. Tương đương lệnh tay nào trong `redis-cli`? Vì sao key này **không** nên đặt TTL?
4. **Lưu lịch sử khởi động**: sửa `checkMongo()` để mỗi lần khởi động ghi **một document mới** (bỏ `_id` cố định, để MongoDB tự sinh `ObjectId`). Restart vài lần, rồi `db.startup_log.find().sort({ startedAt: -1 }).limit(3)` trong `mongosh`. So sánh với bài 13: vì sao ở đây **không** cần idempotent mà vẫn ổn?
5. **Tìm hiểu**: Laravel cấu hình Redis bằng `REDIS_HOST`, `CACHE_STORE=redis` trong `.env`. Ánh xạ từng dòng đó sang file nào, thuộc tính nào của project Spring này? Thuộc tính nào trong Laravel **chưa có** tương đương ở đây (gợi ý: cache, session, queue)?
6. **Đặt lại tên biến cho MySQL**: đổi `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `DB_ROOT_PASSWORD` thành `MYSQL_HOST`, `MYSQL_PORT`... ở **mọi chỗ**: `application.yml`, `docker-compose.yml`, `docker-compose.dev.yml`, `.env`. Dùng `grep -rn "DB_" --include="*.yml" --include=".env" .` để không sót. Sau đó `up -d` lại và kiểm tra `/actuator/health` vẫn `UP`. Hỏi: nếu trong `application.yml` sót dòng `${DB_PASSWORD:demo_pass}` (chưa đổi thành `MYSQL_PASSWORD`), vì sao app **vẫn chạy bình thường**, không báo lỗi gì? Lỗi đó sẽ chỉ lộ ra khi nào? Còn nếu sót `${DB_HOST:localhost}` thì sao?

➡️ Quay lại tổng quan: [../README.md](../README.md)
