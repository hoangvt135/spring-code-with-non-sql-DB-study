# Redis

> **Dùng trong project từ:** [bài 14](../study/14-moi-truong-mongodb-redis.md) (dựng môi trường), [bài 16](../study/16-luot-xem-top-san-pham-redis.md) (tính năng thật đầu tiên) · **Phiên bản:** `redis:7.4` (theo `docker-compose.dev.yml`), Spring Data Redis đi kèm Spring Boot 3.4.1, driver Lettuce · **Tài liệu chính thức:** [redis.io/docs](https://redis.io/docs/latest/) · [danh sách lệnh](https://redis.io/docs/latest/commands/) · [Spring Data Redis](https://docs.spring.io/spring-data/redis/reference/)
>
> 🔼 **Sau [bài 16.5](../study/16.5-nang-cap-spring-boot-4.md)** (người học chưa làm): Lettuce **7.5**. Property `spring.data.redis.*` **không** đổi; code Redis không phải sửa.

## Nó là gì, giải quyết vấn đề gì

**Redis là một kho dữ liệu dạng key → value, giữ toàn bộ dữ liệu trong RAM.** Mỗi lệnh chỉ mất vài micro giây, nhanh hơn MySQL rất nhiều, vì không phải đọc ghi đĩa ở mỗi câu lệnh.

Nếu so với những thứ đã quen bên PHP:

| Bên PHP / MySQL | Redis |
|---|---|
| Mảng `$_SESSION`, APCu, Memcached | Gần giống, nhưng dùng chung được cho nhiều server và có nhiều kiểu dữ liệu hơn |
| Một bảng MySQL có 2 cột `key`, `value` | Ý tưởng giống, nhưng không có bảng, không có SQL, không có JOIN |
| `CACHE_STORE=redis`, `SESSION_DRIVER=redis`, `QUEUE_CONNECTION=redis` của Laravel | Đúng là Redis này. Laravel dùng nó cho cache, session, queue |

Redis **không thay MySQL**. Nó giải quyết những việc mà MySQL làm được nhưng làm chậm hoặc làm vất vả:

| Việc | Vì sao hợp với Redis |
|---|---|
| **Đếm** (lượt xem, lượt like, số lần đăng nhập sai) | `INCR`, `ZINCRBY` cộng ngay trong Redis, không khoá dòng như `UPDATE ... SET views = views + 1` |
| **Xếp hạng** (top xem nhiều, bảng điểm) | Sorted Set luôn giữ sẵn thứ tự, lấy top N gần như tức thì |
| **Dữ liệu tự hết hạn** (mã OTP, "vừa xem", khoá tạm) | Đặt TTL cho key, Redis tự xoá, không cần cron dọn rác |
| **Cache** (kết quả truy vấn nặng) | Đọc từ RAM thay vì chạy lại câu SQL. Sẽ học ở [bài 19](#sắp-học-ở-bài-19-cache-bằng-cacheable) |
| **Session** dùng chung cho nhiều server | Server nào cũng đọc được session trong Redis |
| **Hàng đợi** đơn giản | List với `LPUSH` / `RPOP` |

Đổi lại, Redis có 2 điểm yếu cần nhớ suốt:

1. **Dữ liệu nằm trong RAM**, nên dung lượng bị giới hạn bởi RAM, và có thể mất vài phút dữ liệu cuối nếu Redis sập đột ngột (xem [Độ bền dữ liệu](#độ-bền-dữ-liệu-rdb-snapshot-và-volume-redis_data)).
2. **Không có quan hệ, không có truy vấn phức tạp.** Không thể hỏi "các sản phẩm giá dưới 100 nghìn thuộc danh mục X". Chỉ hỏi được theo **key**.

Vì vậy quy tắc của project (đặt ra ở [bài 16](../study/16-luot-xem-top-san-pham-redis.md#quy-tắc-của-bài-này)): **MySQL giữ dữ liệu chính. Redis chỉ giữ dữ liệu phụ, mất hoặc lệch một chút cũng được.**

## Khái niệm cốt lõi

### Key và value

Mọi thứ trong Redis là một cặp **key → value**. Key là một chuỗi, value có một **kiểu** (String, List, Set, Sorted Set, Hash...). Kiểu được quyết định bởi lệnh đầu tiên ghi vào key đó: `SET` tạo String, `LPUSH` tạo List, `ZADD`/`ZINCRBY` tạo Sorted Set.

Không cần "tạo bảng" trước. Ghi vào một key chưa có thì Redis tự tạo. Một List hay Sorted Set bị xoá hết phần tử thì key đó cũng tự biến mất.

Dùng sai kiểu thì Redis báo lỗi:

```
127.0.0.1:6379> SET demo:x "hello"
OK
127.0.0.1:6379> LPUSH demo:x "a"
(error) WRONGTYPE Operation against a key holding the wrong kind of value
```

Lệnh `TYPE <key>` cho biết key đang thuộc kiểu nào.

### Redis chạy lệnh tuần tự, nên mỗi lệnh là "atomic"

Redis xử lý lệnh **lần lượt từng lệnh một** (một luồng chính). Hai người cùng gửi `ZINCRBY` vào cùng một lúc thì Redis vẫn chạy lệnh này xong rồi mới tới lệnh kia. Vì vậy một lệnh không bao giờ bị lệnh khác chen vào giữa. Tính chất này gọi là **atomic**.

Đây là lý do [bài 16](../study/16-luot-xem-top-san-pham-redis.md#bước-3--productstatsservice-mọi-thao-tác-với-redis) dùng `incrementScore` (một lệnh `ZINCRBY`) chứ không "đọc số cũ, cộng 1, ghi lại" bằng Java: cách sau là 2 lệnh, giữa hai lệnh người khác có thể chen vào, làm mất lượt đếm.

Lưu ý: atomic chỉ đúng cho **từng lệnh**. `recordView()` gửi 5 lệnh liên tiếp, và 5 lệnh đó **không** atomic khi gộp lại. Với lượt xem thì không sao. Muốn gộp nhiều lệnh thành một khối thì Redis có `MULTI`/`EXEC` hoặc script Lua, project chưa dùng (bài 16, bài tập 6).

### 5 kiểu dữ liệu chính

| Kiểu | Giống gì trong Java | Dùng cho | Project dùng ở |
|---|---|---|---|
| **String** | `String`, hoặc một con số | Giá trị đơn, bộ đếm, cache, khoá tạm | `demo:last-startup` (bài 14) |
| **List** | `LinkedList<String>`, có thứ tự, cho trùng | "Vừa xem", hàng đợi, log gần nhất | `demo:recent:<user>` (bài 16) |
| **Set** | `HashSet<String>`, không thứ tự, không trùng | Ai đã like, tag, "đã xem chưa" | Chưa dùng |
| **Sorted Set** (ZSet) | Mỗi phần tử có một điểm số, luôn xếp theo điểm | Bảng xếp hạng, lượt xem | `demo:product:views` (bài 16) |
| **Hash** | `Map<String, String>` | Một object nhiều trường | Chưa dùng |

Value của mọi kiểu đều là **chuỗi**. Con số cũng lưu dạng chuỗi (`"12"`), Redis tự hiểu là số khi gặp lệnh như `INCR`. Vì thế code Java phải tự đổi `long` ↔ `String` (`String.valueOf`, `Long.valueOf`).

Trong Spring, mỗi kiểu có một "bộ lệnh" riêng, lấy qua `opsForXxx()`:

| Kiểu | Gọi | Nhận được |
|---|---|---|
| String | `redisTemplate.opsForValue()` | `ValueOperations` |
| List | `redisTemplate.opsForList()` | `ListOperations` |
| Set | `redisTemplate.opsForSet()` | `SetOperations` |
| Sorted Set | `redisTemplate.opsForZSet()` | `ZSetOperations` |
| Hash | `redisTemplate.opsForHash()` | `HashOperations` |

`opsForXxx()` **chưa gửi gì** tới Redis, nó chỉ trả về bộ lệnh. Hàm gọi tiếp theo (`set`, `leftPush`, `incrementScore`...) mới thật sự gửi lệnh. Cách đọc dòng gọi nối nhau `a.b().c()`: xem [bài 14 Bước 5](../study/14-moi-truong-mongodb-redis.md#bước-5--thử-ghiđọc-lúc-khởi-động-nosqlconnectioncheck) và [sổ tay](../kien-thuc/doc-bai-hoc-va-tra-cuu.md#hàm-có-sẵn-của-thư-viện-ví-dụ-opsforzset-ở-đâu-ra-làm-sao-biết-nó-làm-gì).

Lệnh từng kiểu và hàm Java tương ứng nằm ở [Tra cứu nhanh](#tra-cứu-nhanh). Dưới đây là ví dụ để hình dung.

#### String

```
SET demo:greeting "xin chào"         → OK
GET demo:greeting                    → "xin chào"
INCR demo:counter                    → (integer) 1   (key chưa có thì coi như 0)
SET demo:otp:admin 123456 EX 300     → tự xoá sau 300 giây
SET demo:lock:42 1 NX EX 10          → chỉ ghi nếu key CHƯA có (NX). Trả nil nếu đã có
```

Một String chứa được tối đa 512 MB, nhưng thực tế chỉ nên để giá trị nhỏ.

#### List

Có hai đầu: trái (`L`) và phải (`R`). Vị trí đánh số từ `0`, số âm đếm từ cuối (`-1` là phần tử cuối).

```
LPUSH demo:recent:admin 3            → chèn "3" vào đầu trái
LRANGE demo:recent:admin 0 -1        → toàn bộ list, từ trái sang phải
LTRIM demo:recent:admin 0 4          → chỉ giữ 5 phần tử đầu, bỏ phần còn lại
LREM demo:recent:admin 0 3           → xoá MỌI phần tử bằng "3"
```

Bộ ba `LREM` → `LPUSH` → `LTRIM` của bài 16 tạo ra danh sách "vừa xem" không trùng, mới nhất đứng đầu, dài tối đa N.

#### Set và Hash

Project chưa dùng hai kiểu này. Set giống `HashSet` (`SADD`, `SISMEMBER`, `SMEMBERS`), hợp cho câu hỏi "user này đã like chưa". Hash giống một object nhiều trường (`HSET demo:product:3 name "Áo thun" price 150000`, `HGET`, `HGETALL`). Lệnh và hàm Java tương ứng nằm ở [Tra cứu nhanh](#tra-cứu-nhanh).

### TTL và EXPIRE: key tự hết hạn

Mỗi key có thể có **thời hạn sống** (TTL, *time to live*). Hết hạn thì Redis tự xoá key, không cần ai dọn.

```
EXPIRE demo:recent:admin 2592000     → đặt hạn 30 ngày (tính bằng giây)
TTL demo:recent:admin                → còn bao nhiêu giây
PERSIST demo:recent:admin            → bỏ hạn, key sống mãi
```

Ý nghĩa giá trị trả về của `TTL`:

| `TTL` trả về | Nghĩa |
|---|---|
| Số dương | Còn bấy nhiêu giây |
| `-1` | Key có tồn tại nhưng **không có hạn** |
| `-2` | Key **không tồn tại** (chưa ghi bao giờ, hoặc đã hết hạn) |

Ba điều hay làm người mới bất ngờ:

- **`SET` không kèm `EX` sẽ xoá hạn cũ.** Ghi đè một key đang có hạn bằng `SET key value` là key thành sống mãi. Đây chính là chuyện đang xảy ra với `demo:last-startup` trong code thật (xem [Project dùng thế nào](#nosqlconnectioncheck-bài-14)).
- **`EXPIRE` gọi lại thì hạn được đặt lại từ đầu.** Bài 16 lợi dụng điều này: mỗi lần xem sản phẩm, `expire(recentKey, recentTtl)` đẩy hạn lùi thêm 30 ngày. Nghĩa là "30 ngày **không xem gì** thì mất", không phải "30 ngày kể từ lần xem đầu".
- **Các lệnh sửa nội dung List/Set/ZSet (`LPUSH`, `ZINCRBY`...) không đụng tới hạn.** Chỉ những lệnh ghi đè cả key (`SET`, `DEL`, `RENAME`...) mới làm mất hạn.

Không phải key nào cũng nên có hạn. `demo:product:views` **không** có TTL, vì lượt xem phải giữ lâu dài.

### Đặt tên key

Redis không có bảng hay schema. Mọi key nằm chung một không gian phẳng. Quy ước phổ biến là dùng dấu `:` để chia tầng:

```
<app>:<đối tượng>:<thông tin>[:<id>]
demo:product:views          ← lượt xem của mọi sản phẩm (1 Sorted Set)
demo:recent:admin           ← "vừa xem" của user admin (mỗi user 1 List)
demo:last-startup           ← thời điểm app khởi động gần nhất (String)
```

- Tiền tố `demo:` giúp tách key của app này nếu Redis dùng chung với app khác.
- RedisInsight và nhiều công cụ khác hiện key thành cây thư mục theo dấu `:`.
- Trong code, tên key là **hằng số** (`VIEWS_KEY`, `RECENT_KEY_PREFIX` trong `ProductStatsService`), không viết rải rác, để khỏi gõ sai.

Chi tiết lý do: [bài 16 — Đặt tên key](../study/16-luot-xem-top-san-pham-redis.md#đặt-tên-key).

### Độ bền dữ liệu: RDB snapshot và volume `redis_data`

Redis giữ dữ liệu trong RAM, nhưng có 2 cách ghi xuống đĩa để khởi động lại không bị mất sạch:

| Cách | Làm gì | Mất tối đa khi sập đột ngột | Project dùng? |
|---|---|---|---|
| **RDB** (snapshot) | Định kỳ chụp **toàn bộ** dữ liệu ra một file `dump.rdb` | Mọi thay đổi kể từ lần chụp cuối (có thể vài phút) | ✅ Mặc định của image |
| **AOF** (append-only file) | Ghi lại **từng lệnh ghi** vào file, lúc khởi động chạy lại | Khoảng 1 giây (mặc định `appendfsync everysec`) | ❌ Mặc định tắt |

Image `redis:7.4` chạy Redis **không kèm file cấu hình**, nên dùng cấu hình mặc định của Redis 7: RDB bật, lưu vào thư mục `/data`, AOF tắt. Lịch chụp mặc định là `save 3600 1 300 100 60 10000`, nghĩa là chụp khi: sau 1 giờ có ít nhất 1 thay đổi, hoặc sau 5 phút có ít nhất 100 thay đổi, hoặc sau 60 giây có ít nhất 10000 thay đổi. Tự kiểm tra bằng `CONFIG GET save` và `CONFIG GET appendonly` trong `redis-cli`.

Khi container dừng **bình thường** (`docker stop`, `docker compose down`), Redis chụp một lần cuối trước khi tắt, nên không mất gì. Chỉ khi bị giết đột ngột mới mất phần chưa chụp.

Thư mục `/data` được gắn vào **volume `redis_data`** (khai báo ở [bài 14 Bước 1](../study/14-moi-truong-mongodb-redis.md#bước-1--thêm-mongodb-và-redis-vào-docker-composedevyml)):

| Thao tác | Lượt xem còn không? |
|---|---|
| `docker restart demo-redis`, `docker compose ... down` rồi `up` | Còn: file `dump.rdb` vẫn nằm trong volume |
| `docker compose -f docker-compose.dev.yml down -v` | **Mất**: `-v` xoá luôn volume |
| `./script/reset-db.sh` | **Mất**: script gọi `FLUSHDB` (cố ý, xem [bài 15.5](../study/15.5-reset-du-lieu-va-chay-lai-seeder.md#vì-sao-phải-xoá-cả-mongodb-và-redis-không-chỉ-mysql)) |

Về volume nói chung: [Docker & Docker Compose](docker.md). Muốn bật AOF: bài 14, bài tập 2.

### Database số 0–15 và `FLUSHDB`

Một Redis server có sẵn **16 database đánh số 0–15** (không có tên). Mặc định mọi kết nối dùng database `0`, Spring cũng vậy (`spring.data.redis.database`, project không đặt). `redis-cli` cũng vào database `0`.

- `FLUSHDB`: xoá mọi key của **database đang dùng**. Script reset dùng lệnh này.
- `FLUSHALL`: xoá mọi key của **cả 16 database**. Đừng gõ nhầm trên server dùng chung.

### Redis không có transaction chung với MySQL

`ProductService.delete()` xoá sản phẩm ở MySQL rồi gọi `productStatsService.removeProduct(id)` để `ZREM` bên Redis. Hai việc này **không** nằm trong cùng một transaction:

- MySQL xoá xong mà Redis lỗi → MySQL **không** rollback. Trong Redis còn sót id của sản phẩm đã xoá. Code chịu được: lúc ghép id thành `Product`, id nào không còn trong MySQL thì bị bỏ qua ("dọn lười", [bài 16 Bước 4](../study/16-luot-xem-top-san-pham-redis.md#bước-4--productservice-ghép-id-từ-redis-thành-sản-phẩm)).
- `@Transactional` của Spring chỉ áp dụng cho MySQL (JPA). Nó không "kéo" được Redis vào cùng.
- List "vừa xem" của từng user thì **không** được dọn khi xoá sản phẩm, cũng nhờ dọn lười mà vẫn hiện đúng.

Đây là lý do Redis chỉ được giữ dữ liệu "lệch một chút cũng được". Với dữ liệu không được phép lệch (tiền, tồn kho), không đặt ở Redis.

### Redis chết thì app phải sống: timeout + `DataAccessException`

Driver Lettuce kết nối **lười**: app vẫn khởi động được khi Redis đang tắt ([bài 14 Bước 5](../study/14-moi-truong-mongodb-redis.md#bước-5--thử-ghiđọc-lúc-khởi-động-nosqlconnectioncheck)). Lỗi chỉ lộ ra khi code gọi Redis lần đầu. Vì vậy phải lo 2 việc:

1. **Lỗi phải đến sớm.** Không cấu hình gì thì Lettuce chờ một lệnh tới 60 giây mới báo lỗi. Project đặt `timeout: 2s` và `connect-timeout: 2s` ([bài 16 Bước 1](../study/16-luot-xem-top-san-pham-redis.md#bước-1--applicationyml-timeout-cho-redis)).
2. **Bắt đúng lỗi.** Spring đổi mọi lỗi Redis (mất kết nối: `RedisConnectionFailureException`, quá giờ: `QueryTimeoutException`...) thành lớp con của `org.springframework.dao.DataAccessException`. `ProductStatsService` bắt đúng lớp này, ghi `log.warn`, rồi trả giá trị "rỗng" (`0`, `Map` rỗng, `List` rỗng). Lỗi do code sai (`NullPointerException`, `NumberFormatException`) **không** bị bắt, nên vẫn hiện ra để sửa. So sánh bắt rộng và bắt hẹp: [sổ tay](../kien-thuc/java-ngoai-le.md#catch-ioexception-e-và-catch-exception-e-khác-nhau-thế-nào).

`depends_on: condition: service_healthy` trong compose chỉ giúp **lúc khởi động**. Redis chết giữa chừng thì compose không làm gì cho app cả, nên `try/catch` là bắt buộc.

## Project này dùng nó thế nào

### Môi trường: container `demo-redis` (bài 14)

Trong [`docker-compose.dev.yml`](../../docker-compose.dev.yml):

```yaml
  redis:
    image: redis:7.4
    container_name: demo-redis
    ports:
      - "6380:6379"          # máy bạn: 6380 → trong container: 6379
    volumes:
      - redis_data:/data     # nơi Redis ghi dump.rdb
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
```

| Ai kết nối | Host:cổng | Ghi chú |
|---|---|---|
| App trong Docker | `redis:6379` | `REDIS_HOST: redis` (tên service), cổng gốc |
| `redis-cli` trong container | `docker exec -it demo-redis redis-cli` | Không cần cổng, chạy ngay trong container |
| RedisInsight / app chạy trên máy (`./gradlew bootRun`) | `localhost:6380` | `REDIS_PORT=6380` khi chạy ngoài Docker |

Redis **không có mật khẩu**. Ở máy local thì chấp nhận được, lên server thật thì bắt buộc đặt (xem [bài 14](../study/14-moi-truong-mongodb-redis.md#redis-không-có-mật-khẩu-có-sao-không), bài tập 1).

> ⚠️ Chỉ `docker-compose.dev.yml` có service `redis`. `docker-compose.yml` (chế độ thường) **chưa có** (bài 14 Bước 2 chưa làm). Chạy chế độ thường thì app tìm Redis ở `localhost:6379` trong chính container app, không thấy: log `[NoSQL]` báo lỗi, `/actuator/health` trả 503, lượt xem luôn `0`.

### Kết nối: `application.yml`

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      timeout: 2s            # chờ kết quả 1 lệnh tối đa 2 giây
      connect-timeout: 2s    # chờ kết nối tối đa 2 giây
```

Thuộc tính nằm dưới **`spring.data.redis`**. Từ Spring Boot 3.0, tên cũ `spring.redis.*` không còn tác dụng, và Spring **không báo lỗi** khi gặp tên cũ.

Starter `spring-boot-starter-data-redis` (trong `build.gradle`) mang theo Spring Data Redis và Lettuce. Spring Boot tự tạo sẵn 2 bean: `redisTemplate` (kiểu `RedisTemplate<Object, Object>`) và `stringRedisTemplate` (kiểu `StringRedisTemplate`). Project chỉ dùng cái thứ hai.

### Vì sao `StringRedisTemplate` chứ không phải `RedisTemplate`

| | `StringRedisTemplate` ⬅️ project dùng | `RedisTemplate` mặc định |
|---|---|---|
| Key, value lưu dạng | Chuỗi UTF-8 | Byte do Java tự đóng gói (JDK serialization) |
| Xem bằng `redis-cli` | Đọc được: `"12"`, `"demo:product:views"` | Ký tự lạ kiểu `"\xac\xed\x00\x05t\x00..."` |
| Lệnh gõ tay khớp với code | Có | Không, vì key cũng bị đóng gói |
| Lưu được object Java | Không, phải tự đổi ra chuỗi | Có, nhưng class phải `implements Serializable` |

Lượt xem chỉ là id và con số, nên `StringRedisTemplate` là đủ và dễ kiểm tra nhất. Bài 19 (cache object `Product`) mới cần tới chuyện serialize.

### `NoSqlConnectionCheck` (bài 14)

[`nosql/NoSqlConnectionCheck.java`](../../src/main/java/com/example/demo/nosql/NoSqlConnectionCheck.java) thử ghi rồi đọc một key lúc khởi động:

```java
redisTemplate.opsForValue().set("demo:last-startup", LocalDateTime.now().toString());
String value = redisTemplate.opsForValue().get("demo:last-startup");
```

> ⚠️ Khác bài học: bài 14 gọi `set(key, value, Duration.ofMinutes(10))`. Code thật không truyền `Duration`, nên key **không bao giờ hết hạn**: `TTL demo:last-startup` trả `-1`, không phải số giây. Xem [doc module](../nosql/kiem-tra-ket-noi-mongo-redis.md).

### `ProductStatsService` (bài 16)

[`stats/ProductStatsService.java`](../../src/main/java/com/example/demo/stats/ProductStatsService.java) là **class duy nhất** làm việc với Redis cho lượt xem. Nó chỉ biết id, không biết `Product` hay MySQL. Cấu hình (`recent-ttl=30d`, `recent-limit=5`, `top-limit=5`) đọc từ [`config/stats.properties`](../../src/main/resources/config/stats.properties) qua `AppConfig`.

```java
private static final String VIEWS_KEY = "demo:product:views";
private static final String RECENT_KEY_PREFIX = "demo:recent:";

public long recordView(long productId, String username) {
    String id = String.valueOf(productId);
    try {
        Double views = redisTemplate.opsForZSet().incrementScore(VIEWS_KEY, id, 1);   // ZINCRBY
        String recentKey = RECENT_KEY_PREFIX + username;
        redisTemplate.opsForList().remove(recentKey, 0, id);                           // LREM
        redisTemplate.opsForList().leftPush(recentKey, id);                            // LPUSH
        redisTemplate.opsForList().trim(recentKey, 0, recentLimit - 1);                // LTRIM
        redisTemplate.expire(recentKey, recentTtl);                                    // EXPIRE
        return views.longValue();
    } catch (DataAccessException e) {
        log.warn("[Stats] Redis lỗi, không ghi được lượt xem sản phẩm {}: {}", productId, e.getMessage());
        return 0;
    }
}
```

| Hàm | Lệnh Redis | Key |
|---|---|---|
| `recordView(id, user)` | `ZINCRBY`, `LREM`, `LPUSH`, `LTRIM`, `EXPIRE` | `demo:product:views`, `demo:recent:<user>` |
| `allViews()` | `ZRANGE ... WITHSCORES` | `demo:product:views` |
| `topProductIds()` | `ZREVRANGE 0 <top-limit - 1>` | `demo:product:views` |
| `recentProductIds(user)` | `LRANGE 0 -1` | `demo:recent:<user>` |
| `removeProduct(id)` | `ZREM` | `demo:product:views` |

Cả 5 hàm đều có `try/catch (DataAccessException e)`. Luồng đầy đủ và cách `ProductService` ghép id thành sản phẩm: [bài 16 — Luồng xử lý](../study/16-luot-xem-top-san-pham-redis.md#luồng-xử-lý), [doc module lượt xem](../product/luot-xem-va-top-san-pham.md).

### Xem dữ liệu bằng `redis-cli`

```bash
docker exec -it demo-redis redis-cli
```

```
127.0.0.1:6379> KEYS demo:*                                  # chỉ dùng ở local, xem bẫy bên dưới
127.0.0.1:6379> TYPE demo:product:views                      # → zset
127.0.0.1:6379> ZREVRANGE demo:product:views 0 -1 WITHSCORES
127.0.0.1:6379> LRANGE demo:recent:admin 0 -1
127.0.0.1:6379> TTL demo:recent:admin
127.0.0.1:6379> MONITOR                                      # xem trực tiếp mọi lệnh app gửi tới. Ctrl+C để thoát
```

Chạy một lệnh rồi thoát luôn: `docker exec demo-redis redis-cli ZSCORE demo:product:views 3`. Kịch bản kiểm tra đầy đủ: [bài 16 Bước 9](../study/16-luot-xem-top-san-pham-redis.md#bước-9--chạy--kiểm-tra).

## Tra cứu nhanh

Cột "Của ai": **Redis** là lệnh gõ trong `redis-cli`. Các hàm Java đều thuộc **Spring Data Redis**: `opsForXxx()` gọi trên bean `StringRedisTemplate`, hàm `.xxx(...)` gọi trên bộ lệnh vừa lấy được. Hàm Redis của Spring trả về kiểu wrapper (`Long`, `Double`, `Boolean`), không phải `long`, `double` (lý do: [bài 16](../study/16-luot-xem-top-san-pham-redis.md#bước-3--productstatsservice-mọi-thao-tác-với-redis), mục Tra cứu).

### Lệnh chung (mọi kiểu)

| Lệnh hoặc Hàm | Của ai | Làm gì | Trả về / Ghi chú |
|---|---|---|---|
| `PING` | Redis | Kiểm tra Redis sống | `PONG`. Healthcheck của compose dùng lệnh này |
| `TYPE key` | Redis | Xem kiểu của key | `string`, `list`, `set`, `zset`, `hash`, hoặc `none` |
| `EXPIRE key giây` / `expire(key, Duration)` | Redis / `StringRedisTemplate` | Đặt hạn cho key | `Boolean`: `true` nếu key tồn tại |
| `TTL key` / `getExpire(key)` | Redis / `StringRedisTemplate` | Còn sống bao nhiêu giây | `Long`. `-1` không có hạn, `-2` không tồn tại |
| `DEL key` / `delete(key)` | Redis / `StringRedisTemplate` | Xoá key | `Boolean`: `true` nếu đã xoá thật |
| `EXISTS key` / `hasKey(key)` | Redis / `StringRedisTemplate` | Key có tồn tại không | `Boolean` |
| `KEYS pattern` | Redis | Liệt kê key khớp mẫu, ví dụ `demo:*` | Quét **toàn bộ** key. Chỉ dùng ở local, production dùng `SCAN` |
| `SCAN 0 MATCH demo:* COUNT 100` | Redis | Liệt kê từng đợt, không làm Redis đứng | Trả con trỏ cho lần gọi sau. Hết khi con trỏ về `0` |
| `MONITOR` | Redis | In mọi lệnh server nhận được | Chỉ để debug, rất tốn tài nguyên |
| `FLUSHDB` | Redis | Xoá mọi key của database đang dùng | Script reset dùng. Không hỏi lại |
| `CONFIG GET save` | Redis | Xem lịch chụp RDB | Cũng dùng được với `appendonly`, `maxmemory`... |

### String: `redisTemplate.opsForValue()`

| Lệnh hoặc Hàm | Của ai | Làm gì | Trả về / Ghi chú |
|---|---|---|---|
| `SET key value` / `set(key, value)` | Redis / `ValueOperations` | Ghi giá trị | `void`. Ghi đè và **xoá hạn cũ** |
| `SET key value EX giây` / `set(key, value, Duration)` | Redis / `ValueOperations` | Ghi kèm thời hạn | `void` |
| `GET key` / `get(key)` | Redis / `ValueOperations` | Đọc giá trị | `String`, hoặc `null` nếu không có |
| `INCR key` / `increment(key)` | Redis / `ValueOperations` | Cộng 1 (atomic) | `Long`: giá trị sau khi cộng. Key chưa có thì coi là 0 |
| `INCRBY key n` / `increment(key, n)` | Redis / `ValueOperations` | Cộng n | `Long` |
| `SET key value NX EX giây` / `setIfAbsent(key, value, Duration)` | Redis / `ValueOperations` | Chỉ ghi nếu key **chưa có** | `Boolean`: `true` nếu vừa ghi. Dùng làm khoá tạm (bài 16, bài tập 1) |

### List: `redisTemplate.opsForList()`

| Lệnh hoặc Hàm | Của ai | Làm gì | Trả về / Ghi chú |
|---|---|---|---|
| `LPUSH key v` / `leftPush(key, v)` | Redis / `ListOperations` | Chèn vào đầu trái | `Long`: độ dài mới |
| `RPUSH key v` / `rightPush(key, v)` | Redis / `ListOperations` | Chèn vào cuối phải | `Long` |
| `LPOP key` / `leftPop(key)` | Redis / `ListOperations` | Lấy và xoá phần tử đầu | `String`, hoặc `null` nếu rỗng |
| `LRANGE key start end` / `range(key, start, end)` | Redis / `ListOperations` | Đọc một đoạn. `0, -1` là cả list | `List<String>`, rỗng nếu key không có |
| `LTRIM key start end` / `trim(key, start, end)` | Redis / `ListOperations` | Chỉ giữ đoạn `start..end` | `void` |
| `LREM key count v` / `remove(key, count, v)` | Redis / `ListOperations` | Xoá phần tử bằng `v`. `count = 0` là xoá hết | `Long`: số phần tử đã xoá |
| `LLEN key` / `size(key)` | Redis / `ListOperations` | Độ dài list | `Long` |

### Set: `redisTemplate.opsForSet()`

| Lệnh hoặc Hàm | Của ai | Làm gì | Trả về / Ghi chú |
|---|---|---|---|
| `SADD key v...` / `add(key, v...)` | Redis / `SetOperations` | Thêm phần tử (trùng thì bỏ qua) | `Long`: số phần tử mới thêm |
| `SISMEMBER key v` / `isMember(key, v)` | Redis / `SetOperations` | Có trong set không | `Boolean` |
| `SMEMBERS key` / `members(key)` | Redis / `SetOperations` | Mọi phần tử | `Set<String>`, không có thứ tự |
| `SREM key v...` / `remove(key, v...)` | Redis / `SetOperations` | Xoá phần tử | `Long` |
| `SCARD key` / `size(key)` | Redis / `SetOperations` | Số phần tử | `Long` |

### Sorted Set: `redisTemplate.opsForZSet()`

| Lệnh hoặc Hàm | Của ai | Làm gì | Trả về / Ghi chú |
|---|---|---|---|
| `ZADD key score m` / `add(key, m, score)` | Redis / `ZSetOperations` | Thêm member với điểm (ghi đè điểm cũ) | `Boolean`: `true` nếu là member mới. Chú ý thứ tự tham số Java: member trước, score sau |
| `ZINCRBY key n m` / `incrementScore(key, m, n)` | Redis / `ZSetOperations` | Cộng điểm (atomic) | `Double`: điểm mới |
| `ZSCORE key m` / `score(key, m)` | Redis / `ZSetOperations` | Điểm của một member | `Double`, hoặc `null` nếu không có |
| `ZRANGE key s e WITHSCORES` / `rangeWithScores(key, s, e)` | Redis / `ZSetOperations` | Đoạn theo thứ tự điểm thấp → cao, kèm điểm | `Set<TypedTuple<String>>`: `getValue()` là member, `getScore()` là điểm. Giữ đúng thứ tự |
| `ZREVRANGE key s e` / `reverseRange(key, s, e)` | Redis / `ZSetOperations` | Đoạn theo thứ tự điểm cao → thấp | `Set<String>`, giữ đúng thứ tự |
| `ZREVRANK key m` / `reverseRank(key, m)` | Redis / `ZSetOperations` | Hạng của member (0 = cao nhất) | `Long`, hoặc `null` |
| `ZREM key m...` / `remove(key, m...)` | Redis / `ZSetOperations` | Xoá member | `Long` |
| `ZCARD key` / `zCard(key)` | Redis / `ZSetOperations` | Số member | `Long` |

### Hash: `redisTemplate.opsForHash()`

| Lệnh hoặc Hàm | Của ai | Làm gì | Trả về / Ghi chú |
|---|---|---|---|
| `HSET key f v` / `put(key, f, v)` | Redis / `HashOperations` | Ghi một trường | `void` |
| `HGET key f` / `get(key, f)` | Redis / `HashOperations` | Đọc một trường | Giá trị, hoặc `null`. Kiểu trả về là generic, thường khai báo `HashOperations<String, String, String> h = redisTemplate.opsForHash();` để nhận `String` |
| `HGETALL key` / `entries(key)` | Redis / `HashOperations` | Mọi cặp trường → giá trị | `Map` |
| `HINCRBY key f n` / `increment(key, f, n)` | Redis / `HashOperations` | Cộng vào một trường | `Long` |
| `HDEL key f...` / `delete(key, f...)` | Redis / `HashOperations` | Xoá trường | `Long` |

### Cấu hình Spring (`application.yml`)

| Thuộc tính | Project đặt | Ghi chú |
|---|---|---|
| `spring.data.redis.host` / `port` | `${REDIS_HOST:localhost}` / `${REDIS_PORT:6379}` | Trong Docker: `redis` / `6379` |
| `spring.data.redis.timeout` | `2s` | Chờ kết quả một lệnh. Không đặt thì Lettuce chờ 60 giây |
| `spring.data.redis.connect-timeout` | `2s` | Chờ kết nối |
| `spring.data.redis.password` | Không đặt | Cần khi Redis có `--requirepass` (bài 14, bài tập 1) |
| `spring.data.redis.database` | Không đặt (= `0`) | Database số mấy trong 0–15 |

## Bẫy thường gặp

| Triệu chứng | Nguyên nhân | Cách sửa |
|---|---|---|
| Log `Unable to connect to Redis` / `Connection refused: localhost/...:6379` khi chạy Docker | Thiếu `REDIS_HOST: redis` trong `environment`, **hoặc** viết `spring.redis.host` (tên cũ) | Kiểm tra compose và `application.yml`. Xem [bài 14](../study/14-moi-truong-mongodb-redis.md#lỗi-thường-gặp) |
| Chạy `docker compose up --build` (chế độ thường) thì lượt xem luôn `0`, `/actuator/health` trả 503 | `docker-compose.yml` chưa có service `redis` | Dùng chế độ dev, hoặc làm bài 14 Bước 2 |
| Tắt Redis thì trang treo khoảng 2 phút | Thiếu `timeout`, hoặc đặt sai dưới `spring.redis` | Thêm `spring.data.redis.timeout: 2s` |
| Tắt Redis thì trang ra 500 | Một hàm trong `ProductStatsService` thiếu `try/catch (DataAccessException e)` | Mọi hàm public gọi Redis đều phải bắt |
| `redis-cli` thấy key/value toàn ký tự lạ `\xac\xed\x00...` | Dùng `RedisTemplate` mặc định (JDK serialization) thay vì `StringRedisTemplate` | Dùng `StringRedisTemplate`, hoặc cấu hình serializer |
| `(error) WRONGTYPE Operation against a key holding the wrong kind of value` | Dùng lệnh List lên key String (hoặc ngược lại) | `TYPE key` để xem kiểu, đổi tên key hoặc `DEL` key cũ |
| `TTL` ra `-1` dù tưởng đã đặt hạn | Ghi lại bằng `SET`/`set(key, value)` không kèm thời hạn, hạn cũ bị xoá | Dùng `set(key, value, Duration)`, hoặc gọi `expire` sau khi ghi |
| `TTL` ra `-2`, "Bạn vừa xem" luôn trống | Key đã hết hạn rất nhanh: `recent-ttl=30` không có đơn vị = 30 **mili giây** | Ghi kèm đơn vị: `30d` |
| `NumberFormatException` khi đọc top | Tự `ZADD` một member không phải số trong `redis-cli` | `ZREM` member đó. Member phải là id số |
| Sản phẩm mới sau khi reset MySQL "thừa hưởng" lượt xem của sản phẩm cũ cùng id | Xoá MySQL mà không xoá Redis | Dùng `./script/reset-db.sh` (có `FLUSHDB`) |
| `down -v` xong thì lượt xem mất sạch | `-v` xoá volume `redis_data` | Bình thường. Chỉ dùng `down` (không `-v`) nếu muốn giữ |
| Redis production chậm hẳn hoặc đứng vài giây | Gọi `KEYS *` trên Redis có rất nhiều key | Dùng `SCAN` |
| Hai member bằng điểm, thứ tự top "lạ" | Bằng điểm thì Redis xếp theo chuỗi member, không theo ai được xem trước | Chấp nhận, hoặc cộng thêm phần lẻ thời gian vào score |

## Đọc thêm

- Bài học: [14 — Cài MongoDB + Redis](../study/14-moi-truong-mongodb-redis.md), [16 — Lượt xem và Top bằng Redis](../study/16-luot-xem-top-san-pham-redis.md), [15.5 — Reset dữ liệu](../study/15.5-reset-du-lieu-va-chay-lai-seeder.md).
- Doc module: [Lượt xem và top sản phẩm](../product/luot-xem-va-top-san-pham.md), [Kiểm tra kết nối Mongo/Redis](../nosql/kiem-tra-ket-noi-mongo-redis.md), [Dữ liệu mẫu & reset](../seed/du-lieu-mau.md).
- Công nghệ liên quan: [Docker & Docker Compose](docker.md) (volume, cổng, healthcheck), [MongoDB](mongodb.md) (DB phụ còn lại), [MySQL](mysql.md) (DB chính), [Spring Boot](spring-boot.md) (cấu hình, Actuator health).
- Tài liệu chính thức:
  - [Redis data types](https://redis.io/docs/latest/develop/data-types/): giải thích từng kiểu dữ liệu.
  - [Redis commands](https://redis.io/docs/latest/commands/): tra cứu từng lệnh, có độ phức tạp và giá trị trả về.
  - [Redis persistence](https://redis.io/docs/latest/operate/oss_and_stack/management/persistence/): RDB và AOF.
  - [Spring Data Redis reference](https://docs.spring.io/spring-data/redis/reference/): `RedisTemplate`, serializer, cache.
  - [Spring Boot: Working with NoSQL — Redis](https://docs.spring.io/spring-boot/reference/data/nosql.html#data.nosql.redis): auto-configuration và thuộc tính `spring.data.redis.*`.
  - [Image `redis` trên Docker Hub](https://hub.docker.com/_/redis): cách image chạy Redis, thư mục `/data`.

### Sắp học ở bài 19: cache bằng `@Cacheable`

Bài 16 dùng Redis như **một DB phụ**: tự gọi từng lệnh, tự chọn kiểu dữ liệu. Bài 19 dùng Redis theo kiểu khác: làm **cache**, và gần như không gọi lệnh Redis nào bằng tay.

**Cache là gì:** lưu tạm kết quả của một việc tốn thời gian (ví dụ `SELECT` danh sách sản phẩm) để lần sau trả luôn, khỏi làm lại. Khó nhất của cache không phải là lưu, mà là **biết khi nào phải xoá**: sản phẩm vừa sửa mà cache còn bản cũ thì người dùng thấy dữ liệu sai.

Spring có sẵn một lớp trừu tượng cho cache, điều khiển bằng annotation:

```java
@Cacheable("products")                           // lần đầu: chạy hàm rồi lưu kết quả vào cache. Lần sau: trả từ cache
public Product findById(long id) { ... }

@CacheEvict(value = "products", allEntries = true)   // thêm/sửa/xoá thì xoá cache
public Product save(...) { ... }
```

Bật bằng `@EnableCaching`. Khi có Redis, Spring Boot tự dùng Redis làm nơi chứa cache: mỗi mục là một key dạng `products::3`. Những điểm bài 19 sẽ phải xử lý: mặc định cache lưu object bằng JDK serialization (class phải `Serializable`, và xem bằng `redis-cli` không đọc được), nên cần cấu hình serializer JSON. `Product` có quan hệ `Category` lazy nên dễ lỗi khi serialize. Và cần đặt thời hạn cho cache (`spring.cache.redis.time-to-live`). Tài liệu: [Spring Boot Caching](https://docs.spring.io/spring-boot/reference/io/caching.html).
