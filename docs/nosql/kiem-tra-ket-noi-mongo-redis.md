# Kiểm tra kết nối MongoDB và Redis (NoSqlConnectionCheck)

> **Module:** `nosql` · **Bài học nguồn:** [bài 14](../study/14-moi-truong-mongodb-redis.md), [bài 16 Bước 1](../study/16-luot-xem-top-san-pham-redis.md#bước-1--applicationyml-timeout-cho-redis) (timeout Redis), [bài 16.5](../study/16.5-nang-cap-spring-boot-4.md), [bài 17](../study/17-lombok-slf4j-getter-setter.md) · **Cập nhật:** 2026-11-10
> **Trạng thái code:** ✅ Hoàn thành ở chế độ dev · 🔄 Bài 14 Bước 2 chưa làm (`docker-compose.yml` chưa có Mongo/Redis) — theo code thật ngày 2026-11-10

## Chức năng này làm gì

MongoDB và Redis là **DB phụ**, chạy song song với MySQL. Driver của chúng kết nối "lười": app vẫn khởi động được dù hai DB này đang tắt hoặc cấu hình sai. `NoSqlConnectionCheck` là một `CommandLineRunner` **thử ghi rồi đọc lại** mỗi DB một lần lúc khởi động, in kết quả ra log, để phát hiện lỗi cấu hình ngay.

Đây là công cụ chẩn đoán: lỗi chỉ ghi `log.error`, **không** làm app dừng.

## Đường dẫn & giao diện

Không có — chạy lúc khởi động. Kiểm tra thêm bằng `GET /actuator/health` (không cần đăng nhập): có `components.mongo` và `components.redis`; một DB `DOWN` thì cả `status` là `DOWN`, HTTP **503**.

## Luồng xử lý

```
App khởi động → "Started DemoApplication" → mọi CommandLineRunner chạy (cùng lúc với DataSeeder)
  NoSqlConnectionCheck.run()
    ├─ checkMongo()
    │    ├─ save(Document{_id:"last-startup", startedAt:now}, "startup_log")   (_id cố định → ghi đè, luôn 1 document)
    │    ├─ findById("last-startup", Document.class, "startup_log")
    │    ├─ OK  → log.info "[NoSQL] MongoDB OK, database = ..., startup_log = {...}"
    │    └─ Exception → log.error "[NoSQL] không kết nối được mongoDB: ..."   (app chạy tiếp)
    └─ checkRedis()
         ├─ opsForValue().set("demo:last-startup", now)      ⚠️ không có TTL
         ├─ opsForValue().get("demo:last-startup")
         ├─ OK  → log.info "[NoSQL] Redis OK, demo:last-startup = ..."
         └─ Exception (chờ tối đa 2s nhờ timeout) → log.error "[NoSQL] không kết nối được Redis: ..."
```

> ⚠️ Khác bài học: bài 14 Bước 5 gọi `set(key, value, Duration.ofMinutes(10))`, key tự xoá sau 10 phút. Code thật gọi `set(key, value)` **không có `Duration`** (dù vẫn `import java.time.Duration`), nên key `demo:last-startup` **không bao giờ hết hạn**: `TTL demo:last-startup` trả `-1`, không phải số giây như bài học.

## File liên quan

| File | Vai trò |
|---|---|
| [`nosql/NoSqlConnectionCheck.java`](../../src/main/java/com/example/demo/nosql/NoSqlConnectionCheck.java) | `@Component` + `CommandLineRunner`: `checkMongo()`, `checkRedis()` |
| [`docker-compose.dev.yml`](../../docker-compose.dev.yml) | Service `mongo` (`mongo:8.0`), `redis` (`redis:7.4`), healthcheck, volume `mongo_data`, `redis_data`; `app-dev` chờ cả 3 DB healthy |
| [`docker-compose.yml`](../../docker-compose.yml) | **Chưa có** `mongo`, `redis` |
| [`application.yml`](../../src/main/resources/application.yml) | `spring.data.mongodb.uri`, `spring.data.redis.*`, `management.endpoint.health.show-details: always` |
| `build.gradle` | `spring-boot-starter-data-mongodb`, `spring-boot-starter-data-redis` (đã kèm driver) |

## Dữ liệu

| DB | Nơi | Nội dung |
|---|---|---|
| MongoDB | collection `startup_log` | Đúng 1 document `{_id: "last-startup", startedAt: "<giờ UTC>"}` |
| Redis | key `demo:last-startup` (String) | Thời điểm khởi động gần nhất, không TTL |

Cổng và host ([bảng gốc](../study/14-moi-truong-mongodb-redis.md#cổng-mở-ra-máy-bạn)):

| Service | Container | App dùng (trong mạng Docker) | Từ máy bạn |
|---|---|---|---|
| MySQL | `demo-mysql` | `mysql:3306` | `localhost:3313` |
| MongoDB | `demo-mongo` | `mongo:27017` | `localhost:27018` |
| Redis | `demo-redis` | `redis:6379` | `localhost:6380` (không mật khẩu) |

Xem tận nơi:

```bash
docker exec -it demo-mongo mongosh -u demo_user -p demo_pass --authenticationDatabase admin demo_db
docker exec -it demo-redis redis-cli      # GET demo:last-startup
```

## Cấu hình

```yaml
spring.data.mongodb.uri: mongodb://${MONGO_USERNAME:demo_user}:${MONGO_PASSWORD:demo_pass}@${MONGO_HOST:localhost}:${MONGO_PORT:27017}/${MONGO_DB:demo_mongo}?authSource=admin
spring.data.redis: host ${REDIS_HOST:localhost}, port ${REDIS_PORT:6379}, timeout 2s, connect-timeout 2s
```

Biến môi trường của `app-dev`: `MONGO_HOST=mongo`, `MONGO_PORT=27017`, `MONGO_DB=${MONGO_DB:-demo_db}`, `MONGO_USERNAME`, `MONGO_PASSWORD` (mặc định `demo_user` / `demo_pass`), `REDIS_HOST=redis`, `REDIS_PORT=6379`. User Mongo do `MONGO_INITDB_ROOT_*` tạo ở **lần đầu** khởi tạo volume, nằm trong database `admin` → URI bắt buộc có `?authSource=admin`.

> ⚠️ Khác bài học: bài 14 đặt tên database MongoDB là **`demo_mongo`** (`MONGO_DB: ${MONGO_DB:-demo_mongo}`). Trong `docker-compose.dev.yml` thật, mặc định là **`demo_db`**, và `.env` không khai báo `MONGO_DB`. Vì vậy khi chạy Docker dev, app dùng database Mongo **`demo_db`** (log in `database = demo_db`). Chỉ khi chạy ngoài Docker mới rơi về `demo_mongo` của `application.yml`. Script `script/reset-db.sh` cũng mặc định `demo_db`, khớp với compose.
>
> ⚠️ Khác bài học: bài 14 Bước 2 thêm `mongo`, `redis` vào cả `docker-compose.yml`. Code thật chưa làm, nên chế độ thường (`docker compose up --build`) **không có** Mongo/Redis: log `[NoSQL]` báo lỗi, `/actuator/health` trả 503, và các tính năng Redis của bài 16 không chạy.

Chạy app ngay trên máy (không Docker): `DB_PORT=3313 MONGO_PORT=27018 REDIS_PORT=6380 ./gradlew bootRun`.

## Quy tắc & lưu ý khi sửa

- Redis: thuộc tính là `spring.data.redis.*`, **không phải** `spring.redis.*` (tên cũ, bị bỏ qua lặng lẽ).
- Trong Docker, host là **tên service** (`mongo`, `redis`), cổng là cổng **trong** container (27017, 6379), không phải cổng mở ra máy.
- `try/catch` ở đây là cố ý (chẩn đoán, không phải nghiệp vụ), xem [bài 14](../study/14-moi-truong-mongodb-redis.md#vì-sao-trycatch-ở-đây-trong-khi-project-quy-định-service-ném-runtimeexception). Code nghiệp vụ vẫn theo quy ước ném `RuntimeException`.
- Đổi `MONGO_USERNAME` / `MONGO_PASSWORD` sau khi volume đã tạo → `Authentication failed`: phải xoá volume Mongo ([làm lại từ đầu](../study/14-moi-truong-mongodb-redis.md#làm-lại-từ-đầu)).
- Không có transaction chung giữa MySQL và Mongo/Redis: xoá dữ liệu ở MySQL thì phải tự dọn bên kia.
- Repository cho Mongo sau này phải `extends MongoRepository` (project có 3 module Spring Data, chạy ở chế độ "strict").
- Key Redis của project dùng tiền tố `demo:`. Khi thêm `docker-compose.yml`, giữ hai file compose giống nhau (dùng chung container name).

## Liên quan

- [Lượt xem và top sản phẩm](../product/luot-xem-va-top-san-pham.md): tính năng đầu tiên dùng Redis thật (`stats/ProductStatsService`).
- [Dữ liệu mẫu & reset](../seed/du-lieu-mau.md): script reset `dropDatabase()` Mongo và `FLUSHDB` Redis.
- Sổ tay: [`management.endpoints` và `management.endpoint`](../cong-nghe/spring-boot.md#managementendpoints-và-managementendpoint-cókhông-có-chữ-s-khác-nhau-thế-nào).

## Lịch sử thay đổi

| Bài | Thay đổi |
|---|---|
| [14](../study/14-moi-truong-mongodb-redis.md) | Thêm `mongo`, `redis` vào `docker-compose.dev.yml` (chưa thêm vào `docker-compose.yml`); 2 starter; cấu hình kết nối + health; tạo `NoSqlConnectionCheck` |
| [16 Bước 1](../study/16-luot-xem-top-san-pham-redis.md#bước-1--applicationyml-timeout-cho-redis) | Thêm `timeout: 2s`, `connect-timeout: 2s` cho Redis |
| [16.5](../study/16.5-nang-cap-spring-boot-4.md) | ⬜ Chưa làm. Spring Boot 4: `spring.data.mongodb.uri` → `spring.mongodb.uri` (tên cũ bị bỏ qua → kết nối nhầm `localhost:27017`). Redis giữ `spring.data.redis.*` |
| [17](../study/17-lombok-slf4j-getter-setter.md) | ⬜ Chưa làm. `NoSqlConnectionCheck` dùng `@Slf4j` thay dòng khai báo `Logger` |
