# MongoDB

> **Dùng trong project từ:** [bài 14](../study/14-moi-truong-mongodb-redis.md) (dựng môi trường), bài 18 (đánh giá sản phẩm, chưa viết) · **Phiên bản:** `mongo:8.0` (theo `docker-compose.dev.yml`), Spring Data MongoDB đi kèm Spring Boot 3.4.1 · **Tài liệu chính thức:** [mongodb.com/docs/manual](https://www.mongodb.com/docs/manual/) · [mongosh](https://www.mongodb.com/docs/mongodb-shell/) · [Spring Data MongoDB](https://docs.spring.io/spring-data/mongodb/reference/)
>
> 🔼 **Sau [bài 16.5](../study/16.5-nang-cap-spring-boot-4.md)** (người học chưa làm): Driver Mongo **5.8**. Property kết nối đổi `spring.data.mongodb.uri` → **`spring.mongodb.uri`**. Quên đổi thì app lặng lẽ kết nối `localhost:27017` (Bước 7).

## Nó là gì, giải quyết vấn đề gì

**MongoDB là một DB dạng document: mỗi bản ghi là một object kiểu JSON, có thể lồng object và mảng bên trong.** Nó không có bảng với cột cố định như MySQL.

Ví dụ một đánh giá sản phẩm (kiểu dữ liệu bài 18 sẽ làm), viết theo cách MongoDB lưu:

```js
{
  _id: ObjectId("6730f1c2a1b2c3d4e5f60718"),
  productId: 3,
  username: "admin",
  stars: 5,
  content: "Áo đẹp, vải mát",
  createdAt: ISODate("2026-11-10T08:15:42Z"),
  replies: [                                   // mảng object nằm NGAY TRONG đánh giá
    { username: "shop", content: "Cảm ơn bạn!", createdAt: ISODate("2026-11-10T09:00:00Z") }
  ]
}
```

Làm việc này bằng MySQL thì cần 2 bảng (`review`, `review_reply`), khoá ngoại, và một câu `JOIN` mỗi lần hiển thị. Với MongoDB, một lần đọc là lấy đủ cả đánh giá lẫn các câu trả lời.

MongoDB hợp với dữ liệu:

| Đặc điểm dữ liệu | Ví dụ |
|---|---|
| Luôn được đọc **cả cụm**, ít khi cần ghép với thứ khác | Đánh giá kèm trả lời, bài viết kèm bình luận |
| Mỗi bản ghi có **trường khác nhau** | Catalog: áo có `size`, điện thoại có `ram`, sách có `isbn` |
| Ghi nhiều, cấu trúc hay thay đổi | Log, sự kiện, lịch sử thao tác |

MongoDB **không hợp** với dữ liệu có nhiều quan hệ chéo và cần ràng buộc chặt: đơn hàng, tiền, tồn kho, user và phân quyền. Những thứ đó vẫn ở MySQL.

Trong project, MongoDB là **DB phụ**, chạy song song với MySQL và Redis ([bài 14 — Ý tưởng](../study/14-moi-truong-mongodb-redis.md)). Hiện mới chỉ có một collection để thử kết nối. Bài 18 mới dùng thật.

## Khái niệm cốt lõi

### Database, collection, document: so với MySQL

| MySQL | MongoDB | Ghi chú |
|---|---|---|
| Server | Server | Một server chứa nhiều database |
| Database (`demo_db`) | Database | Giống nhau |
| Bảng (`product`) | **Collection** (`startup_log`) | Không có danh sách cột cố định |
| Dòng (row) | **Document** | Một object JSON. Chữ "document" ở đây **không** có nghĩa là "tài liệu" kiểu Word, xem [Hỏi đáp](#blob-là-gì-document-của-mongodb-có-phải-là-tài-liệu-lưu-ảnh-vào-db-hay-ra-file) |
| Cột (column) | **Field** (trường) | Mỗi document tự có trường của nó |
| Khoá chính `id AUTO_INCREMENT` | `_id`, thường là `ObjectId` | Bắt buộc có, không trùng trong một collection |
| `JOIN` | Không có (gần đúng: `$lookup` trong aggregation) | Thiết kế để khỏi cần JOIN |
| Khoá ngoại | **Không có** | Không ai chặn bạn trỏ tới id không tồn tại |
| `CREATE TABLE` | Không cần | Ghi vào collection chưa có thì MongoDB tự tạo |

Database và collection được tạo **lười**: `use demo_db` chưa tạo gì cả. Chỉ khi ghi document đầu tiên thì chúng mới xuất hiện. Vì vậy `show dbs` không liệt kê database chưa có dữ liệu.

### BSON: JSON có thêm kiểu

Nhìn qua thì document giống JSON, nhưng MongoDB lưu bằng **BSON** (*Binary JSON*). BSON có nhiều kiểu hơn JSON, nên phân biệt được những thứ JSON không phân biệt được:

| Kiểu BSON | Ví dụ trong `mongosh` | Ứng với Java |
|---|---|---|
| String | `"Áo thun"` | `String` |
| Int32 / Int64 | `5`, `NumberLong(3)` | `int` / `long` |
| Double | `4.5` | `double` |
| Decimal128 | `NumberDecimal("150000.00")` | `BigDecimal`. Dùng cho tiền, vì Double làm tròn sai |
| Boolean | `true` | `boolean` |
| Date | `ISODate("2026-11-10T08:15:42Z")` | `Date`, `Instant`, `LocalDateTime` (Spring tự đổi) |
| ObjectId | `ObjectId("6730f1c2...")` | `ObjectId`, hoặc `String` (Spring tự đổi) |
| Array | `[1, 2, 3]` | `List` |
| Document lồng | `{ city: "HN" }` | Một class khác, hoặc `Map` |
| Null | `null` | `null` |
| Binary | `BinData(...)` | `byte[]` |

Hệ quả thực tế: `"5"` (chuỗi) và `5` (số) là **khác nhau**. Tìm `{ productId: 3 }` sẽ không ra document lưu `productId: "3"`. Ở MySQL, cột có kiểu cố định nên chuyện này không xảy ra. Ở MongoDB phải tự giữ cho thống nhất, và dùng class Java có kiểu rõ ràng (bài 18) là cách đơn giản nhất.

Một document tối đa **16 MB**. Mảng lồng nhau không được lớn vô hạn.

### `_id` và ObjectId

Mỗi document **bắt buộc** có trường `_id`, không trùng trong collection. `_id` có thể là bất cứ kiểu gì (trừ mảng). Không tự đặt thì MongoDB (hoặc driver) sinh một **ObjectId**:

```
ObjectId("6730f1c2 a1b2c3d4e5 f60718")
          └──────┘ └────────┘ └────┘
          4 byte   5 byte      3 byte
          thời điểm giá trị    bộ đếm
          tạo (giây) ngẫu nhiên tăng dần
```

- 12 byte, viết ra thành 24 ký tự hex.
- Sinh ra **ở phía client** được, không cần hỏi server như `AUTO_INCREMENT`. Nhiều máy cùng sinh mà gần như không bao giờ trùng.
- 4 byte đầu là thời điểm tạo, nên ObjectId **gần như tăng theo thời gian**, và `sort({ _id: -1 })` xấp xỉ "mới nhất trước".

`NoSqlConnectionCheck` cố ý **tự đặt** `_id` là chuỗi `"last-startup"`. Nhờ vậy mỗi lần khởi động là ghi đè đúng một document, collection luôn chỉ có 1 bản ghi (bài 14). Bỏ `_id` cố định đi thì mỗi lần khởi động sinh thêm một document mới (bài 14, bài tập 4).

### Schema linh hoạt (không có nghĩa là không có schema)

MongoDB **không bắt** các document trong một collection phải giống nhau. Hai document sau nằm chung một collection vẫn hợp lệ:

```js
{ _id: 1, name: "Áo thun", size: "L" }
{ _id: 2, name: "Điện thoại", ram: "8GB", colors: ["đen", "trắng"] }
```

Lợi ích: thêm trường mới không cần `ALTER TABLE`, document cũ không có trường đó thì coi như `null`. So với project: MySQL ở đây dùng `ddl-auto: update` để Hibernate tự thêm cột. Với MongoDB thì không có gì để "update" cả.

Cái giá phải trả: **schema chuyển từ DB sang code.** Không ai chặn bạn ghi `stars: "năm"` thay vì `stars: 5`, hay gõ nhầm `usename`. Vì vậy trong thực tế người ta vẫn giữ một cấu trúc thống nhất, thường bằng một class Java có `@Document` (bài 18). MongoDB cũng có tính năng *schema validation* (`$jsonSchema`) để bắt buộc cấu trúc, project chưa dùng.

### Lồng nhau (embed) hay tham chiếu (reference)

Đây là quyết định thiết kế quan trọng nhất khi dùng MongoDB, tương đương với việc chia bảng ở MySQL.

| | **Lồng** (embed): đặt dữ liệu con ngay trong document cha | **Tham chiếu** (reference): chỉ lưu id, dữ liệu ở chỗ khác |
|---|---|---|
| Ví dụ | `replies: [ {...}, {...} ]` trong đánh giá | `productId: 3` trong đánh giá |
| Đọc | Một lần là đủ | Phải đọc thêm lần nữa ở chỗ khác |
| Ghi | Sửa cha và con cùng lúc, atomic (cùng 1 document) | Hai lần ghi riêng |
| Hợp khi | Con **thuộc về** cha, luôn đọc cùng cha, số lượng có giới hạn | Con được dùng ở nhiều nơi, sửa độc lập, hoặc số lượng không giới hạn |
| Rủi ro | Document phình to (giới hạn 16 MB) | Dữ liệu lệch nhau vì không có khoá ngoại |

Áp vào bài 18: **câu trả lời** của đánh giá thì lồng vào đánh giá (thuộc về nó, ít). **Sản phẩm** thì chỉ lưu `productId` (sản phẩm nằm ở MySQL, sửa độc lập). **Người viết** chỉ lưu `username`.

### Không có JOIN, khoá ngoại hay transaction chung với MySQL

Đánh giá ở MongoDB trỏ tới sản phẩm ở MySQL bằng `productId`. Giữa hai DB:

- **Không có JOIN.** Muốn hiện đánh giá kèm tên sản phẩm thì code Java phải đọc 2 lần (MySQL rồi MongoDB) và tự ghép, giống cách bài 16 ghép id từ Redis thành `Product`.
- **Không có khoá ngoại.** Xoá sản phẩm ở MySQL thì MongoDB không biết. Code phải **tự** xoá đánh giá của sản phẩm đó, giống cách bài 16 tự `ZREM` bên Redis và bài 15 tự xoá file ảnh. Quên bước này thì sau khi reset MySQL, sản phẩm mới cùng id sẽ "nhận" đánh giá của sản phẩm cũ ([bài 15.5](../study/15.5-reset-du-lieu-va-chay-lai-seeder.md#vì-sao-phải-xoá-cả-mongodb-và-redis-không-chỉ-mysql)).
- **Không có transaction chung.** `@Transactional` của project chỉ áp dụng cho MySQL. MySQL xoá xong mà MongoDB lỗi thì MySQL không rollback.

Bên trong riêng MongoDB:

- Mọi thao tác trên **một document** luôn atomic. Đây là lý do nên lồng dữ liệu con vào cha: sửa một lần, không sợ lệch.
- Transaction nhiều document có tồn tại, nhưng chỉ chạy trên **replica set** hoặc cluster. Container `demo-mongo` của project là **một server đơn lẻ** (standalone), nên không dùng được transaction.
- `$lookup` trong aggregation làm được việc gần giống `LEFT JOIN` giữa 2 collection. Dùng được, nhưng nếu phải `$lookup` liên tục thì nên xem lại thiết kế.

### Khi nào chọn MongoDB, khi nào ở lại MySQL

| Câu hỏi | Nghiêng về MySQL | Nghiêng về MongoDB |
|---|---|---|
| Dữ liệu có nhiều quan hệ chéo, cần JOIN nhiều? | ✅ | |
| Cần ràng buộc chặt (khoá ngoại, `NOT NULL`, `UNIQUE` nhiều cột)? | ✅ | |
| Cần transaction nhiều bảng (tiền, tồn kho)? | ✅ | |
| Dữ liệu luôn đọc thành cụm cha + con? | | ✅ |
| Mỗi bản ghi có trường khác nhau, hay đổi cấu trúc? | | ✅ |
| Ghi rất nhiều, ít sửa (log, sự kiện)? | | ✅ |
| Chưa chắc? | ✅ Mặc định chọn MySQL | |

Project giữ user, sản phẩm, danh mục ở MySQL. Đánh giá (bài 18) sang MongoDB một phần vì hợp, một phần vì **mục đích học**: dự án nhỏ thật thì để đánh giá ở MySQL cũng hoàn toàn ổn.

### Index

Giống MySQL: không có index thì mỗi truy vấn phải quét cả collection. `_id` luôn có index sẵn. Truy vấn hay dùng (ví dụ `productId` ở bài 18) thì nên tạo thêm:

```js
db.reviews.createIndex({ productId: 1, createdAt: -1 })   // 1 = tăng dần, -1 = giảm dần
db.reviews.getIndexes()
```

Trong Spring có annotation `@Indexed`, nhưng từ Spring Data MongoDB 3.0 việc **tự tạo index lúc khởi động bị tắt mặc định** (`spring.data.mongodb.auto-index-creation: false`). Đặt `@Indexed` mà không bật thuộc tính này thì index không được tạo.

## Project này dùng nó thế nào

### Môi trường: container `demo-mongo` (bài 14)

Trong [`docker-compose.dev.yml`](../../docker-compose.dev.yml):

```yaml
  mongo:
    image: mongo:8.0
    container_name: demo-mongo
    environment:
      MONGO_INITDB_ROOT_USERNAME: ${MONGO_USERNAME:-demo_user}   # tạo user ở LẦN ĐẦU, khi volume còn trống
      MONGO_INITDB_ROOT_PASSWORD: ${MONGO_PASSWORD:-demo_pass}
    ports:
      - "27018:27017"
    volumes:
      - mongo_data:/data/db
    healthcheck:
      test: ["CMD", "mongosh", "--quiet", "--eval", "db.adminCommand('ping')"]
```

| Ai kết nối | Host:cổng | Ghi chú |
|---|---|---|
| App trong Docker | `mongo:27017` | `MONGO_HOST: mongo` (tên service), cổng gốc |
| `mongosh` trong container | `docker exec -it demo-mongo mongosh ...` | Image `mongo:8.0` có sẵn `mongosh` |
| MongoDB Compass / app chạy trên máy | `localhost:27018` | `mongodb://demo_user:demo_pass@localhost:27018/?authSource=admin` |

`MONGO_INITDB_ROOT_*` **chỉ có tác dụng ở lần khởi động đầu tiên**, khi volume `mongo_data` còn trống. Đổi mật khẩu sau đó thì MongoDB vẫn giữ mật khẩu cũ, phải xoá volume ([bài 14 — Làm lại từ đầu](../study/14-moi-truong-mongodb-redis.md#làm-lại-từ-đầu)).

> ⚠️ Chỉ `docker-compose.dev.yml` có service `mongo`. `docker-compose.yml` (chế độ thường) **chưa có** (bài 14 Bước 2 chưa làm).

### URI kết nối và `authSource=admin`

Trong `application.yml`:

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://${MONGO_USERNAME:demo_user}:${MONGO_PASSWORD:demo_pass}@${MONGO_HOST:localhost}:${MONGO_PORT:27017}/${MONGO_DB:demo_mongo}?authSource=admin
```

Đọc từng phần:

```
mongodb://demo_user:demo_pass@mongo:27017/demo_db?authSource=admin
          └───────┘ └───────┘ └───┘ └───┘ └─────┘ └──────────────┘
          user      mật khẩu  host  cổng  database tham số: user nằm ở database "admin"
                                          app làm việc
```

**Vì sao cần `authSource=admin`?** Trong MongoDB, mỗi user "thuộc về" một database. User do `MONGO_INITDB_ROOT_*` tạo ra là user **root**, thuộc database `admin`. Nếu không ghi `authSource`, driver sẽ tìm user trong database ở đường dẫn (`demo_db`), không thấy, và báo `Authentication failed`. Tham số này chỉ nói "**đăng nhập** ở database `admin`", còn dữ liệu vẫn ghi vào `demo_db`. Ở `mongosh`, tham số tương ứng là `--authenticationDatabase admin`.

Mật khẩu có ký tự đặc biệt (`@`, `:`, `/`, `%`) thì phải mã hoá kiểu URL trong URI (`@` thành `%40`), nếu không driver sẽ hiểu sai URI.

### Tên database thật là `demo_db`, không phải `demo_mongo`

> ⚠️ Khác bài học: bài 14 đặt tên database MongoDB là `demo_mongo` ([giải thích lý do](../study/14-moi-truong-mongodb-redis.md#vì-sao-database-mongodb-tên-demo_mongo-mà-không-dùng-luôn-demo_db)). Nhưng trong `docker-compose.dev.yml` thật, `app-dev` có `MONGO_DB: ${MONGO_DB:-demo_db}`, và `.env` không khai báo `MONGO_DB`. Biến môi trường thắng giá trị mặc định trong `application.yml`, nên:

| Cách chạy | Database MongoDB app dùng |
|---|---|
| `docker compose -f docker-compose.dev.yml up` | **`demo_db`** (log in `database = demo_db`) |
| `./gradlew bootRun` trên máy, không đặt `MONGO_DB` | `demo_mongo` (mặc định của `application.yml`) |
| `script/reset-db.sh` | `demo_db` (khớp với compose) |

Vì vậy lệnh `mongosh ... demo_mongo` trong bài 14 mở ra một database **trống**. Dùng `demo_db` khi chạy Docker dev. Không có xung đột gì với database `demo_db` của MySQL: đó là hai server khác nhau.

### `MongoTemplate` trong `NoSqlConnectionCheck` (bài 14)

Starter `spring-boot-starter-data-mongodb` mang theo Spring Data MongoDB và MongoDB Java Driver. Spring Boot tự tạo bean `MongoTemplate`, kết nối lười (app vẫn khởi động khi MongoDB tắt). [`nosql/NoSqlConnectionCheck.java`](../../src/main/java/com/example/demo/nosql/NoSqlConnectionCheck.java) dùng nó để thử ghi và đọc lại:

```java
Document doc = new Document("_id", "last-startup").append("startedAt", LocalDateTime.now().toString());
mongoTemplate.save(doc, "startup_log");                                   // upsert theo _id
Document saved = mongoTemplate.findById("last-startup", Document.class, "startup_log");
log.info("[NoSQL] MongoDB OK, database = {}, startup_log = {}", mongoTemplate.getDb().getName(), saved.toJson());
```

- `org.bson.Document` là một `Map<String, Object>`, dùng khi chưa có class Java riêng.
- `save` ghi đè nếu đã có document cùng `_id`, chưa có thì thêm (upsert).
- `startedAt` được lưu là **chuỗi** (vì gọi `toString()`), không phải kiểu Date của BSON. Muốn sắp xếp, lọc theo thời gian cho đúng thì nên lưu thẳng kiểu ngày giờ (bài 18 sẽ làm).

Giải thích từng hàm: [bài 14 Bước 5](../study/14-moi-truong-mongodb-redis.md#bước-5--thử-ghiđọc-lúc-khởi-động-nosqlconnectioncheck). Luồng và trạng thái code: [doc module](../nosql/kiem-tra-ket-noi-mongo-redis.md).

### Sẽ học ở bài 18: `@Document` và `MongoRepository`

`MongoTemplate` là cách "gõ lệnh bằng tay". Bài 18 dùng cách quen thuộc hơn, giống hệt JPA:

| JPA (MySQL), đã học | Spring Data MongoDB, bài 18 |
|---|---|
| `@Entity` | `@Document(collection = "reviews")` |
| `@Id @GeneratedValue Long id` | `@Id String id`: để `null` khi tạo, MongoDB sinh ObjectId và Spring đổi ra chuỗi hex |
| `@Column(name = "...")` | `@Field("...")` (thường không cần) |
| `interface ProductRepository extends JpaRepository<Product, Long>` | `interface ReviewRepository extends MongoRepository<Review, String>` |
| `findByCategoryId(...)` (query method) | `findByProductIdOrderByCreatedAtDesc(long productId)`: cùng quy tắc đặt tên |
| `@OneToMany` sang bảng khác | Trường `List<Reply>` lồng thẳng trong document, không cần annotation quan hệ |

Ví dụ phác thảo (chưa phải code của bài 18):

```java
@Document(collection = "reviews")
public class Review {
    @Id
    private String id;
    private long productId;      // tham chiếu sang MySQL, KHÔNG có khoá ngoại
    private String username;
    private int stars;
    private String content;
    private LocalDateTime createdAt;
    private List<Reply> replies = new ArrayList<>();   // lồng nhau
}
```

Hai lưu ý đã nhắc từ bài 14:

- Project có 3 module Spring Data (JPA, MongoDB, Redis) nên chạy ở chế độ **strict**. Repository cho MongoDB **phải** `extends MongoRepository`, không dùng `CrudRepository` chung chung, nếu không Spring không biết gán cho DB nào.
- Document ghi qua class có `@Document` sẽ có thêm trường **`_class`** (tên đầy đủ của class Java), do Spring tự thêm để biết đọc ra thành class nào. Thấy nó trong `mongosh` là bình thường.

### Xem dữ liệu bằng `mongosh`

```bash
docker exec -it demo-mongo mongosh -u demo_user -p demo_pass --authenticationDatabase admin demo_db
```

```js
show dbs                               // các database đang có dữ liệu
show collections                       // → startup_log
db.startup_log.find()                  // → { _id: 'last-startup', startedAt: '...' }
db.startup_log.countDocuments()        // → 1
exit
```

Chạy một lệnh rồi thoát luôn: thêm `--quiet --eval "db.startup_log.find()"` vào cuối lệnh `docker exec` (bỏ `-it`). Script reset làm đúng như vậy với `db.dropDatabase()`.

## Tra cứu nhanh

### Lệnh `mongosh`

| Lệnh | Của ai | Làm gì | Trả về / Ghi chú |
|---|---|---|---|
| `show dbs` | mongosh | Liệt kê database có dữ liệu | Database trống không hiện |
| `use demo_db` | mongosh | Chuyển sang database | Chưa tạo gì cho tới khi ghi |
| `show collections` | mongosh | Liệt kê collection | |
| `db.c.insertOne({...})` | mongosh | Thêm 1 document | `insertedId`. Collection chưa có thì tự tạo |
| `db.c.find({ productId: 3 })` | mongosh | Tìm theo điều kiện. `{}` là lấy hết | Con trỏ, `mongosh` in 20 document đầu |
| `db.c.findOne({ _id: "last-startup" })` | mongosh | Tìm 1 document | Document hoặc `null` |
| `db.c.find().sort({ createdAt: -1 }).limit(5)` | mongosh | Sắp xếp, giới hạn | Giống `ORDER BY ... DESC LIMIT 5` |
| `db.c.find({ stars: { $gte: 4 } })` | mongosh | So sánh: `$gt`, `$gte`, `$lt`, `$lte`, `$ne`, `$in` | Giống `WHERE stars >= 4` |
| `db.c.countDocuments({...})` | mongosh | Đếm | Số |
| `db.c.updateOne({ _id: x }, { $set: { stars: 4 } })` | mongosh | Sửa trường. Còn có `$inc` (cộng), `$push` (thêm vào mảng) | `matchedCount`, `modifiedCount`. Quên `$set` thì **thay cả document** |
| `db.c.deleteMany({ productId: 3 })` | mongosh | Xoá theo điều kiện | `deletedCount` |
| `db.c.createIndex({ productId: 1 })` | mongosh | Tạo index | Tên index |
| `db.c.drop()` | mongosh | Xoá cả collection | |
| `db.dropDatabase()` | mongosh | Xoá database đang dùng | Script reset dùng |
| `db.adminCommand('ping')` | mongosh | Kiểm tra server sống | `{ ok: 1 }`. Healthcheck dùng |

### Hàm Java

| Hàm | Của ai | Làm gì | Trả về / Ghi chú |
|---|---|---|---|
| `new Document("k", v).append("k2", v2)` | `org.bson.Document` (BSON) | Tạo document dạng `Map` | `Document` |
| `doc.toJson()` | `org.bson.Document` | Đổi ra chuỗi JSON | `String` |
| `mongoTemplate.save(obj, "collection")` | `MongoTemplate` | Ghi, có `_id` trùng thì thay thế (upsert) | Object đã lưu |
| `mongoTemplate.insert(obj, "collection")` | `MongoTemplate` | Chỉ thêm mới, `_id` trùng thì lỗi | Object đã lưu |
| `mongoTemplate.findById(id, Xxx.class, "collection")` | `MongoTemplate` | Tìm theo `_id` | Object, hoặc **`null`** (không phải `Optional`) |
| `mongoTemplate.find(query, Xxx.class)` | `MongoTemplate` | Tìm theo điều kiện | `List` |
| `Query.query(Criteria.where("productId").is(3))` | Spring Data MongoDB | Tạo điều kiện tìm | `Query`, truyền vào `find`, `remove`, `count` |
| `mongoTemplate.remove(query, Xxx.class)` | `MongoTemplate` | Xoá theo điều kiện | `DeleteResult` |
| `mongoTemplate.getDb().getName()` | `MongoTemplate` + driver | Tên database đang dùng | `String` |
| `repository.save(entity)` | `MongoRepository` | Thêm hoặc sửa | Entity đã lưu (có `id`) |
| `repository.findById(id)` | `MongoRepository` | Tìm theo id | `Optional`, giống JPA |
| `repository.findAll()`, `count()`, `deleteById(id)` | `MongoRepository` | Như JPA | |
| `findByProductIdOrderByCreatedAtDesc(...)` | Query method tự khai báo | Spring sinh truy vấn từ tên hàm | `List<Review>` |

### Cấu hình Spring (`application.yml`)

| Thuộc tính | Project đặt | Ghi chú |
|---|---|---|
| `spring.data.mongodb.uri` | URI ở trên | Đã dùng `uri` thì không đặt thêm `host`, `port`, `username`, `password` riêng lẻ. Tên database lấy từ đường dẫn trong URI |
| `spring.data.mongodb.auto-index-creation` | Không đặt (= `false`) | Bật lên thì `@Indexed` mới tự tạo index |

## Bẫy thường gặp

| Triệu chứng | Nguyên nhân | Cách sửa |
|---|---|---|
| Log `[NoSQL] không kết nối được mongoDB: ... Authentication failed` | Thiếu `?authSource=admin` trong URI | Thêm lại tham số |
| Vẫn `Authentication failed` dù URI đúng | Volume `mongo_data` được tạo từ trước với user/mật khẩu khác. `MONGO_INITDB_ROOT_*` chỉ chạy lần đầu | Xoá volume Mongo ([bài 14](../study/14-moi-truong-mongodb-redis.md#làm-lại-từ-đầu)) |
| `mongosh ... demo_mongo` rồi `show collections` thấy trống | Trong Docker dev, app dùng database `demo_db` | Mở `demo_db`. Xem [mục tên database](#tên-database-thật-là-demo_db-không-phải-demo_mongo) |
| Log `Exception in monitor thread while connecting to server localhost:27017` | Thiếu `MONGO_HOST: mongo`, app đang dùng mặc định `localhost` | Thêm biến vào `environment` của `app-dev`, rồi `up -d` |
| App khởi động bình thường dù MongoDB tắt | Driver kết nối lười | Đúng như thiết kế. Xem log `[NoSQL]` và `/actuator/health` |
| `find({ productId: 3 })` không ra, dù thấy document có `productId` | Document lưu `productId: "3"` (chuỗi), tìm bằng số | Giữ kiểu thống nhất, dùng class Java có kiểu rõ ràng |
| Gõ nhầm tên trường (`usename`) nhưng không báo lỗi gì | Schema linh hoạt: MongoDB nhận mọi trường | Dùng class `@Document` thay cho `Document` thủ công |
| `updateOne({...}, { stars: 4 })` làm mất hết các trường khác | Thiếu `$set`: document bị **thay thế toàn bộ** | Luôn dùng `{ $set: { ... } }` |
| Thấy trường `_class` lạ trong document | Spring tự thêm khi ghi qua class `@Document` | Bình thường, đừng xoá |
| Đặt `@Indexed` mà `getIndexes()` không có index đó | Tự tạo index đang tắt mặc định | `spring.data.mongodb.auto-index-creation: true`, hoặc `createIndex` bằng tay |
| Repository Mongo không được Spring nhận, hoặc bị gán nhầm sang JPA | Project ở chế độ strict, repository lại `extends CrudRepository` | `extends MongoRepository` |
| Xoá sản phẩm rồi mà đánh giá vẫn còn trong MongoDB | Không có khoá ngoại, không có cascade giữa 2 DB | Tự xoá trong `ProductService.delete()` (bài 18) |
| Dùng `@Transactional` cho thao tác MongoDB nhưng không có tác dụng, hoặc lỗi transaction | Transaction của project chỉ cho JPA. MongoDB standalone không hỗ trợ transaction | Thiết kế để mỗi thao tác chỉ sửa **một** document |
| `demo-mongo` restart liên tục, exit code `132` | CPU không có AVX (MongoDB 5.0+ yêu cầu) | Xem [bài 14 — Lỗi thường gặp](../study/14-moi-truong-mongodb-redis.md#lỗi-thường-gặp) |

## Đọc thêm

- Bài học: [14 — Cài MongoDB + Redis](../study/14-moi-truong-mongodb-redis.md), [15 — Ảnh và mô tả](../study/15-anh-va-mo-ta-san-pham.md#tư-vấn-trước-khi-code-lưu-ảnh-ở-đâu-file-vật-lý-hay-blob) (vì sao không lưu ảnh vào MongoDB), [15.5 — Reset dữ liệu](../study/15.5-reset-du-lieu-va-chay-lai-seeder.md). Bài 18 (đánh giá sản phẩm) chưa viết, phạm vi nằm trong [kế hoạch](../ke-hoach.md).
- Doc module: [Kiểm tra kết nối Mongo/Redis](../nosql/kiem-tra-ket-noi-mongo-redis.md), [Dữ liệu mẫu & reset](../seed/du-lieu-mau.md).
- Công nghệ liên quan: [MySQL](mysql.md) (để so sánh), [Redis](redis.md) (DB phụ còn lại), [Docker & Docker Compose](docker.md), [Spring Data JPA & Hibernate](jpa-hibernate.md) (repository kiểu JPA, bài 18 dùng y hệt).
- Tài liệu chính thức:
  - [MongoDB CRUD Operations](https://www.mongodb.com/docs/manual/crud/): thêm, tìm, sửa, xoá.
  - [Data Modeling](https://www.mongodb.com/docs/manual/data-modeling/): chọn lồng nhau hay tham chiếu.
  - [BSON Types](https://www.mongodb.com/docs/manual/reference/bson-types/), [ObjectId](https://www.mongodb.com/docs/manual/reference/method/ObjectId/).
  - [Connection String](https://www.mongodb.com/docs/manual/reference/connection-string/): ý nghĩa `authSource` và các tham số khác.
  - [Aggregation](https://www.mongodb.com/docs/manual/aggregation/).
  - [Spring Data MongoDB reference](https://docs.spring.io/spring-data/mongodb/reference/): `MongoTemplate`, repository, mapping.
  - [Image `mongo` trên Docker Hub](https://hub.docker.com/_/mongo): biến `MONGO_INITDB_*`, thư mục `/data/db`.

### Sắp học ở bài 18: aggregation

Muốn tính **điểm sao trung bình** của một sản phẩm, MySQL viết `SELECT AVG(stars), COUNT(*) FROM review WHERE product_id = 3`. MongoDB làm việc này bằng **aggregation pipeline**: một mảng các "công đoạn", dữ liệu chảy qua từng công đoạn theo thứ tự.

```js
db.reviews.aggregate([
  { $match: { productId: 3 } },                         // WHERE product_id = 3
  { $group: {                                           // GROUP BY product_id
      _id: "$productId",
      avgStars: { $avg: "$stars" },                     // AVG(stars)
      total:    { $sum: 1 }                             // COUNT(*)
  } }
])
// → [ { _id: 3, avgStars: 4.5, total: 2 } ]
```

| Công đoạn | Giống SQL |
|---|---|
| `$match` | `WHERE` |
| `$group` | `GROUP BY` + hàm gộp (`$avg`, `$sum`, `$max`...) |
| `$sort`, `$limit` | `ORDER BY`, `LIMIT` |
| `$project` | Chọn cột trong `SELECT` |
| `$lookup` | Gần giống `LEFT JOIN` giữa 2 collection |

Trong Java, `MongoTemplate.aggregate(...)` nhận một pipeline dựng bằng `Aggregation.newAggregation(match(...), group(...)...)`. Bài 18 sẽ giải thích từng hàm.

## Hỏi đáp

Các câu hỏi phát sinh trong lúc học, chuyển từ sổ tay chung sang đây.

### BLOB là gì? "Document" của MongoDB có phải là "tài liệu"? Lưu ảnh vào DB hay ra file?

> Hỏi khi chuẩn bị [Bài 15 — Ảnh và mô tả cho sản phẩm](../study/15-anh-va-mo-ta-san-pham.md#tư-vấn-trước-khi-code-lưu-ảnh-ở-đâu-file-vật-lý-hay-blob), liên quan tới MongoDB ở [Bài 14](../study/14-moi-truong-mongodb-redis.md). Bảng so sánh đầy đủ nằm trong bài 15.

- **BLOB** (*Binary Large Object*) là cách gọi chung cho "một cục byte lớn lưu thẳng trong DB": ảnh, PDF, zip... Đây **không phải tính năng riêng của MongoDB**. MySQL có kiểu cột `BLOB` / `MEDIUMBLOB` (16MB) / `LONGBLOB` (4GB). MongoDB có kiểu `BinData`, nhưng cả document tối đa 16MB. File lớn hơn thì dùng **GridFS**: cắt file thành mảnh 255KB rồi lưu vào 2 collection `fs.files` + `fs.chunks`.
- **"Document" trong MongoDB = một bản ghi dạng JSON**, tương đương một dòng trong bảng MySQL. Nó **không** có nghĩa là "tài liệu" kiểu Word/PDF. MongoDB không có ưu thế đặc biệt nào trong việc lưu file so với MySQL.
- **Lưu ở đâu?** Hầu hết dự án làm theo kiểu: **file để một nơi** (ổ đĩa, hoặc S3/MinIO khi chạy production), **DB chỉ lưu tên file**. Lý do là hiển thị nhanh, DB nhẹ, backup nhanh. BLOB chỉ đáng chọn khi file nhỏ, ít, và bắt buộc phải nằm chung transaction với bản ghi.
- Với project này, lưu ảnh vào MongoDB GridFS còn kém hơn nữa: sản phẩm nằm ở MySQL, ảnh lại ở MongoDB, nên xoá sản phẩm và xoá ảnh không cùng một transaction. Như vậy mất luôn ưu điểm "nhất quán" là lý do chính để chọn BLOB.
