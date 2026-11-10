# MySQL

> **Dùng trong project từ:** [bài 01](../study/01-tong-quan.md) · **Phiên bản:** MySQL 8.4 (image `mysql:8.4` trong `docker-compose.yml` và `docker-compose.dev.yml`), driver `mysql-connector-j` 9.1.0 (theo Spring Boot 3.4.1) · **Tài liệu chính thức:** [MySQL 8.4 Reference Manual](https://dev.mysql.com/doc/refman/8.4/en/)
>
> 🔼 **Sau [bài 16.5](../study/16.5-nang-cap-spring-boot-4.md)** (người học chưa làm): Driver `mysql-connector-j` **9.7.0**. MySQL server (`mysql:8.4`) và schema không đổi.

## Nó là gì, giải quyết vấn đề gì

MySQL là hệ quản trị CSDL quan hệ: dữ liệu nằm trong **bảng** (table), mỗi bảng có **cột** (column) cố định kiểu, mỗi bản ghi là một **dòng** (row), các bảng liên kết với nhau bằng **khoá ngoại**. Đây chính là MySQL bạn đã dùng với PHP, không có gì khác về bản chất.

Trong project, MySQL là **DB chính**: mọi dữ liệu "thật" (sản phẩm, danh mục, tài khoản) nằm ở đây. Hai DB còn lại là **DB phụ**:

| DB | Vai trò | Mất dữ liệu thì sao |
|---|---|---|
| **MySQL** | Sản phẩm, danh mục, user | Mất hết dữ liệu nghiệp vụ |
| Redis ([redis.md](redis.md)) | Lượt xem, "Bạn vừa xem" (bài 16) | Mất số đếm, app vẫn chạy |
| MongoDB ([mongodb.md](mongodb.md)) | Dự kiến lưu đánh giá (bài 18) | |

Khác với PHP ở chỗ: bên PHP bạn thường tự viết `CREATE TABLE`, `INSERT`, `SELECT`. Ở đây gần như **không viết SQL**: Hibernate tự tạo bảng từ entity và tự sinh câu truy vấn ([jpa-hibernate.md](jpa-hibernate.md)). Bạn chỉ vào MySQL bằng tay để **xem** dữ liệu và kiểm tra cấu trúc bảng.

Phiên bản **8.4** là bản LTS (hỗ trợ dài hạn) của MySQL. Điểm khác so với MySQL 5.7 hay gặp ở các project PHP cũ: bảng mã mặc định là `utf8mb4` (gõ tiếng Việt, emoji thoải mái), và kiểu đăng nhập mặc định là `caching_sha2_password` (xem [mục dưới](#xác-thực-caching_sha2_password-và-chuỗi-kết-nối-jdbc)).

## Khái niệm cốt lõi

### MySQL chạy trong container `demo-mysql`

Máy bạn **không cài MySQL**. MySQL chạy trong một container Docker tên `demo-mysql`, tạo từ image chính thức `mysql:8.4` ([docker.md](docker.md)). Cả `docker-compose.yml` lẫn `docker-compose.dev.yml` đều khai báo cùng service `mysql` với cùng `container_name: demo-mysql`, nên **hai chế độ dùng chung một MySQL** và không chạy cùng lúc được.

Container có `healthcheck`: cứ 5 giây chạy `mysqladmin ping`. App chỉ khởi động khi MySQL báo `healthy` (`depends_on: condition: service_healthy`), tránh lỗi "app lên trước, DB chưa sẵn sàng".

### Hai địa chỉ của cùng một MySQL: `mysql:3306` và `localhost:3313`

```yaml
ports:
  - "3313:3306"     # cổng 3313 ở máy bạn → cổng 3306 trong container
```

| Ai kết nối | Host | Cổng | Ví dụ |
|---|---|---|---|
| App chạy **trong** Docker (`app`, `app-dev`) | `mysql` (tên service) | `3306` | `DB_HOST: mysql`, `DB_PORT: 3306` trong docker-compose |
| Công cụ trên **máy bạn** (DBeaver, Workbench, IntelliJ) | `localhost` | `3313` | [bài 03](../study/03-cheatsheet.md#làm-việc-với-mysql) |
| App chạy bằng `./gradlew bootRun` trên máy bạn | `localhost` | `3313` | `DB_PORT=3313 ./gradlew bootRun` |
| Lệnh `docker exec ... mysql` | (đang đứng **bên trong** container) | không cần ghi | [Tra cứu nhanh](#tra-cứu-nhanh) |

Đổi sang `3313` để không đụng MySQL cài sẵn trên máy (thường chiếm `3306`). Trong mạng Docker, `localhost` nghĩa là "chính container này", nên app trong container phải gọi bằng tên service `mysql`.

### User, mật khẩu, database: lấy từ đâu, tạo khi nào

```yaml
environment:
  MYSQL_DATABASE: ${DB_NAME:-demo_db}
  MYSQL_USER: ${DB_USERNAME:-demo_user}
  MYSQL_PASSWORD: ${DB_PASSWORD:-demo_pass}
  MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD:-root_pass}
```

- `${DB_NAME:-demo_db}`: lấy biến `DB_NAME` trong file `.env`, không có thì dùng `demo_db`. File `.env` (không đưa lên git) được tạo từ `.env.example`.
- Image `mysql` dùng 4 biến này để tạo database `demo_db`, user `demo_user` (có toàn quyền trên `demo_db`) và mật khẩu cho `root`.
- ⚠️ Việc tạo này **chỉ chạy một lần**, khi thư mục dữ liệu còn trống (volume mới). Đổi mật khẩu trong `.env` sau đó thì MySQL **vẫn giữ mật khẩu cũ**, app báo `Access denied for user 'demo_user'`. Muốn áp dụng thì phải `down -v` để tạo lại từ đầu ([bài 15.5](../study/15.5-reset-du-lieu-va-chay-lai-seeder.md#so-sánh-down--v-và-script-reset)).

App đọc cùng các biến đó qua `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:demo_db}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
    username: ${DB_USERNAME:demo_user}
    password: ${DB_PASSWORD:demo_pass}
```

(Spring dùng dấu `:` cho giá trị mặc định, docker-compose dùng `:-`. Cùng ý nghĩa, khác cú pháp.)

### Xác thực `caching_sha2_password` và chuỗi kết nối JDBC

Chuỗi `jdbc:mysql://host:port/db?...` tương đương DSN `mysql:host=...;port=...;dbname=...` của PDO. Ba tham số phía sau:

| Tham số | Nghĩa | Vì sao cần |
|---|---|---|
| `useSSL=false` | Không mã hoá kết nối | DB nằm cùng máy/cùng mạng Docker, project học tập |
| `allowPublicKeyRetrieval=true` | Cho phép driver xin khoá công khai của server để gửi mật khẩu | MySQL 8.4 dùng `caching_sha2_password` làm mặc định (plugin cũ `mysql_native_password` bị **tắt sẵn** ở 8.4). Kiểu này, khi không có SSL, cần khoá công khai để gửi mật khẩu an toàn. Thiếu tham số → lỗi `Public Key Retrieval is not allowed` |
| `serverTimezone=UTC` | Múi giờ dùng để đổi ngày giờ giữa Java và MySQL | Tránh lệch giờ khi sau này có cột ngày giờ |

### Volume: dữ liệu nằm ở đâu

```yaml
volumes:
  - mysql_data:/var/lib/mysql      # thư mục dữ liệu của MySQL → volume tên mysql_data
```

Dữ liệu **không** nằm trong container mà trong volume Docker. Vì cả hai file compose nằm cùng thư mục, Docker coi chúng là cùng project `java_code`, nên volume thật có tên **`java_code_mysql_data`** và dùng chung cho cả hai chế độ.

| Lệnh | Container | Dữ liệu MySQL |
|---|---|---|
| `docker compose ... stop` / `restart` | Dừng / chạy lại | Giữ nguyên |
| `docker compose ... down` | Xoá | **Giữ nguyên** (volume còn) |
| `docker compose ... down -v` | Xoá | **Xoá sạch**, kể cả user `demo_user` (tạo lại ở lần `up` sau) |

`docker compose down -v` (file thường, không có `-f`) **cũng xoá dữ liệu của chế độ dev**, vì dùng chung volume.

### Bảng do Hibernate sinh

Không có file `.sql` hay migration nào. Lúc app khởi động, Hibernate (`ddl-auto: update`) đọc entity rồi tạo bảng còn thiếu. Project có 3 bảng:

| Bảng | Entity | Cột | Ghi chú |
|---|---|---|---|
| `product` | `Product` | `id`, `name`, `price`, `description`, `image_path`, `category_id` | `@Table(name="Product")` nhưng Spring Boot đổi tên bảng thành chữ thường |
| `category` | `Category` | `id`, `name` | Không có cột nào cho danh sách `products` (phía phụ của quan hệ) |
| `users` | `User` | `id`, `username`, `password`, `role` | `username` có ràng buộc `UNIQUE` |

`DESCRIBE product;` sẽ cho kết quả **dạng** như sau (thứ tự cột tuỳ lúc cột được thêm vào, vì `description`, `image_path` được thêm sau ở bài 15):

```
+-------------+---------------+------+-----+---------+----------------+
| Field       | Type          | Null | Key | Default | Extra          |
+-------------+---------------+------+-----+---------+----------------+
| id          | bigint        | NO   | PRI | NULL    | auto_increment |
| price       | double        | YES  |     | NULL    |                |
| category_id | bigint        | YES  | MUL | NULL    |                |
| name        | varchar(255)  | NO   |     | NULL    |                |
| description | varchar(2000) | YES  |     | NULL    |                |
| image_path  | varchar(255)  | YES  |     | NULL    |                |
+-------------+---------------+------+-----+---------+----------------+
```

`PRI` = khoá chính, `MUL` = cột có index không duy nhất (MySQL tự tạo index cho cột khoá ngoại).

Bảng tên `users` chứ không phải `user`: bài 12 ghi lý do là "`user` trùng từ khoá của MySQL". Chính xác hơn, `USER` có trong danh sách từ khoá của MySQL 8.4 nhưng **không phải từ khoá dành riêng** (reserved), nên `CREATE TABLE user` vẫn chạy. Dù vậy `users` vẫn là lựa chọn tốt: `user` là từ khoá dành riêng ở PostgreSQL và trong chuẩn SQL, nên đổi DB sau này không bị vướng.

### Kiểu cột thường gặp

Hibernate chọn kiểu cột theo kiểu Java của trường:

| Kiểu Java | Kiểu cột MySQL | Ví dụ trong project |
|---|---|---|
| `long` / `Long` | `BIGINT` (`long` thì thêm `NOT NULL`) | `id`, `category_id` |
| `int` / `Integer` | `INT` | |
| `Double` | `DOUBLE` (log `CREATE TABLE` của Hibernate có thể ghi `float(53)`, MySQL lưu thành `double`) | `price` |
| `BigDecimal` | `DECIMAL(p, s)` | Nên dùng cho tiền (chính xác tuyệt đối), project chưa dùng |
| `String` | `VARCHAR(255)` | `name`, `image_path`, `username` |
| `String` + `@Column(length = 2000)` | `VARCHAR(2000)` | `description` |
| `boolean` | `BIT` | |
| `LocalDateTime` | `DATETIME(6)` | |
| `byte[]` + `@Lob` | Kiểu `BLOB` | Project **không** lưu ảnh vào DB |

- **`VARCHAR(n)`**: `n` là số **ký tự**, không phải số byte. Với `utf8mb4`, mỗi ký tự tối đa 4 byte, nên chữ tiếng Việt có dấu vẫn tính là 1 ký tự.
- **`DOUBLE`** là số thực dấu phẩy động, có thể sai số nhỏ khi cộng trừ. Với giá tiền ở dự án thật nên dùng `DECIMAL`. Project giữ `Double` cho đơn giản.
- **`BLOB`** là kiểu cột chứa dữ liệu nhị phân (nội dung file ảnh). Project chỉ lưu **tên file** vào `image_path`, còn file nằm trong `uploads/`. Lý do: [BLOB là gì? Lưu ảnh vào DB hay ra file?](mongodb.md#blob-là-gì-document-của-mongodb-có-phải-là-tài-liệu-lưu-ảnh-vào-db-hay-ra-file) và [bài 15](../study/15-anh-va-mo-ta-san-pham.md).
- **Đổi kiểu hoặc độ dài sau khi bảng đã có**: `ddl-auto: update` không làm, phải tự `ALTER TABLE` hoặc reset (xem [Hỏi đáp `@Column`](jpa-hibernate.md#column-vì-sao-có-trường-khai-báo-có-trường-không)).

### Khoá ngoại `category_id` và chuyện xoá danh mục

`@ManyToOne` + `@JoinColumn(name = "category_id")` trong `Product` làm Hibernate tạo:

```sql
alter table product
  add constraint FK...            -- tên tự sinh, dạng FK + chuỗi ký tự ngẫu nhiên
  foreign key (category_id) references category (id);
```

Ràng buộc này **không** có `ON DELETE`, nên MySQL dùng mặc định: **chặn xoá** dòng cha khi còn dòng con trỏ tới. Hệ quả trong project:

| Thao tác | Kết quả |
|---|---|
| Thêm sản phẩm với `category_id = 99` (không tồn tại) | MySQL từ chối: `Cannot add or update a child row: a foreign key constraint fails` |
| Thêm sản phẩm không có danh mục (`category_id = NULL`) | ✅ Được, vì cột cho phép `NULL` |
| Xoá danh mục **chưa** có sản phẩm nào | ✅ Được |
| Xoá danh mục **còn** sản phẩm | ❌ MySQL báo lỗi 1451 `Cannot delete or update a parent row: a foreign key constraint fails (...)`. Spring đổi thành `DataIntegrityViolationException` → trang lỗi 500 |

Khoá ngoại là **lưới an toàn cuối cùng** ở DB: nhờ nó, không bao giờ có sản phẩm trỏ tới danh mục "ma". Nhưng để người dùng thấy thông báo dễ hiểu thì phải kiểm tra trước ở service: bài tập 2 của [bài 08](../study/08-giao-dien-category.md#bài-tập) (`category.getProducts().isEmpty()`). Các hướng khác (ví dụ khi xoá danh mục thì gán `category_id = NULL` cho sản phẩm của nó) đều phải làm có chủ đích, và `ddl-auto: update` không sửa được ràng buộc khoá ngoại đã có. Hiện trạng code: [Quản lý danh mục](../category/quan-ly-danh-muc.md).

### Transaction

**Transaction** là một nhóm câu lệnh "được ăn cả, ngã về không": hoặc tất cả thành công (`COMMIT`), hoặc tất cả bị huỷ (`ROLLBACK`). Cả 3 bảng đều dùng engine **InnoDB** (Hibernate tạo bảng với `engine=InnoDB`), engine có hỗ trợ transaction.

```sql
START TRANSACTION;
UPDATE product SET price = price - 1000 WHERE id = 1;
DELETE FROM product WHERE id = 2;
ROLLBACK;     -- huỷ cả 2 câu; COMMIT thì lưu cả 2
```

Những điều cần biết:

- MySQL mặc định **`autocommit = 1`**: mỗi câu lệnh lẻ là một transaction riêng, chạy xong là lưu luôn. Gõ `DELETE` trong `mysql` shell là mất thật, không có "Ctrl+Z".
- Câu lệnh đổi cấu trúc (`CREATE`, `ALTER`, `DROP`) **tự `COMMIT`** và không rollback được.
- Mức cô lập mặc định của InnoDB là `REPEATABLE READ`: trong một transaction, đọc lại cùng dòng sẽ thấy cùng dữ liệu.
- Transaction chỉ có tác dụng **trong MySQL**. File trong `uploads/`, dữ liệu Redis/MongoDB không quay lui theo. Vì thế `ProductService.delete()` phải tự xoá file ảnh và dọn Redis.

Phía Java điều khiển transaction thế nào (`@Transactional`, vì sao project hiện mỗi lần gọi repository là một transaction riêng): xem [jpa-hibernate.md](jpa-hibernate.md#transactional-gom-nhiều-thao-tác-thành-một).

## Project này dùng nó thế nào

| Chỗ | Dùng gì | Bài học / doc |
|---|---|---|
| `docker-compose.yml`, `docker-compose.dev.yml` | Service `mysql`: image `mysql:8.4`, container `demo-mysql`, cổng `3313:3306`, volume `mysql_data`, healthcheck `mysqladmin ping` | [bài 01](../study/01-tong-quan.md), [docker.md](docker.md) |
| `.env.example` → `.env` | `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `DB_ROOT_PASSWORD` | |
| `application.yml` | `spring.datasource.*` (chuỗi kết nối), `spring.jpa.hibernate.ddl-auto: update` | [bài 02](../study/02-thuc-hanh-crud.md) |
| Entity `Product`, `Category`, `User` | Hibernate sinh 3 bảng `product`, `category`, `users` | [bài 02](../study/02-thuc-hanh-crud.md), [bài 06](../study/06-quan-he-model.md), [bài 12](../study/12-dang-nhap-spring-security.md#bước-2--entity-user) |
| `seed/DataSeeder.java` | App khởi động trên DB trống → tạo `admin` / `admin123`, 3 danh mục, 5 sản phẩm | [bài 13](../study/13-du-lieu-mau-seeder.md), [Dữ liệu mẫu](../seed/du-lieu-mau.md) |
| `script/reset-db.sh` | `DROP DATABASE` + `CREATE DATABASE`, xoá MongoDB, Redis, `uploads/`, rồi chạy lại app cho seeder tạo dữ liệu | [bài 15.5](../study/15.5-reset-du-lieu-va-chay-lai-seeder.md) |
| `management.endpoint.health.show-details: always` | `/actuator/health` có mục `db` cho biết MySQL còn sống không | [spring-boot.md](spring-boot.md) |

Đoạn script reset chạm vào MySQL:

```bash
docker exec -e MYSQL_PWD="$DB_PASSWORD" demo-mysql \
    mysql -u "$DB_USERNAME" -e "DROP DATABASE IF EXISTS $DB_NAME; CREATE DATABASE $DB_NAME;"
```

- `MYSQL_PWD` truyền mật khẩu qua biến môi trường, để client không in cảnh báo `Using a password on the command line interface can be insecure`.
- `DROP DATABASE` xoá cả bảng lẫn bộ đếm `AUTO_INCREMENT`, nên dữ liệu mẫu mới lại có id 1, 2, 3... Đó là lý do phải xoá Redis cùng lúc ([bài 15.5](../study/15.5-reset-du-lieu-va-chay-lai-seeder.md#vì-sao-phải-xoá-cả-mongodb-và-redis-không-chỉ-mysql)).
- `demo_user` xoá và tạo lại được `demo_db` vì image `mysql` cấp cho `MYSQL_USER` toàn quyền trên database `MYSQL_DATABASE`.

Chỗ code/tài liệu khác nhau:

- Bài 15.5 và comment trong chính script ghi `./scripts/reset-db.sh`, nhưng file thật nằm ở **`script/reset-db.sh`** (không có `s`). Chạy bằng `./script/reset-db.sh`. Script vẫn đúng vì dòng `cd "$(dirname "$0")/.."` luôn đưa về thư mục gốc.
- Bài 03 (phần Gradle và bảng sự cố) còn ghi cổng `3307`. Cổng thật là **`3313`**: chạy app trên máy thì dùng `DB_PORT=3313` (như bài 14).

## Tra cứu nhanh

Các lệnh dưới đây chạy ở terminal máy bạn, khi `demo-mysql` đang chạy.

| Lệnh | Của ai | Làm gì | Ghi chú |
|---|---|---|---|
| `docker exec -it demo-mysql mysql -u demo_user -pdemo_pass demo_db` | Docker + client `mysql` trong container | Mở MySQL shell, vào sẵn `demo_db` | `-p` dính liền mật khẩu, không có dấu cách. Thoát: `exit` |
| `... demo_db -e "SHOW TABLES;"` | client `mysql` | Chạy một câu rồi thoát | Bỏ `-it` nếu dùng trong script |
| `... --default-character-set=utf8mb4` | client `mysql` | Hiện đúng tiếng Việt | Khi thấy `?i?n tho?i` |
| `docker exec -it demo-mysql mysql -u root -proot_pass` | | Vào bằng `root` | Mật khẩu theo `DB_ROOT_PASSWORD` |
| `SHOW DATABASES;` / `USE demo_db;` | SQL | Liệt kê / chọn database | |
| `SHOW TABLES;` | SQL | Liệt kê bảng | Mong đợi: `category`, `product`, `users` |
| `DESCRIBE product;` (hoặc `SHOW COLUMNS FROM product;`) | SQL | Xem cột, kiểu, `NULL`, khoá | Kiểm tra sau khi sửa entity |
| `SHOW CREATE TABLE product\G` | SQL | Xem nguyên câu `CREATE TABLE`, gồm khoá ngoại, engine, charset | `\G` in dọc cho dễ đọc |
| `SHOW INDEX FROM users;` | SQL | Xem index (khoá chính, `UNIQUE`) | |
| `SELECT p.id, p.name, c.name FROM product p LEFT JOIN category c ON p.category_id = c.id;` | SQL | Sản phẩm kèm tên danh mục | Câu của [bài 06](../study/06-quan-he-model.md) |
| `SELECT username, role, password FROM users;` | SQL | Xem tài khoản | Mật khẩu là chuỗi BCrypt `$2a$10$...` |
| `ALTER TABLE product MODIFY description VARCHAR(2000);` | SQL | Sửa cột mà `ddl-auto: update` bỏ qua | |
| `SELECT VERSION();` | SQL | Xem phiên bản MySQL | Mong đợi `8.4.x` |
| `docker compose -f docker-compose.dev.yml logs -f mysql` | Docker | Xem log MySQL | |
| `docker volume ls` | Docker | Thấy volume `java_code_mysql_data` | |
| `./script/reset-db.sh` | script của project | Reset dữ liệu và chạy lại seeder | `-y` để không hỏi lại |
| `docker compose -f docker-compose.dev.yml down -v` | Docker | Xoá luôn volume, MySQL khởi tạo lại từ đầu | Dùng khi đổi `.env` |

Kết nối từ công cụ đồ hoạ: host `localhost`, port `3313`, database `demo_db`, user `demo_user`, mật khẩu `demo_pass`. Nếu công cụ báo `Public Key Retrieval is not allowed`, bật tuỳ chọn `allowPublicKeyRetrieval=true` trong phần cấu hình driver.

## Bẫy thường gặp

| Triệu chứng | Nguyên nhân | Cách sửa |
|---|---|---|
| `Access denied for user 'demo_user'` sau khi sửa `.env` | User/mật khẩu chỉ được tạo lúc volume còn trống | `down -v` rồi `up` lại |
| Chạy dev xong chạy thường (hoặc ngược lại) báo trùng tên container `demo-mysql` / cổng `3313` | Hai file compose dùng chung tên container và cổng | `down` chế độ đang chạy trước |
| `docker compose down -v` (file thường) mà dữ liệu chế độ dev cũng mất | Hai file chung project `java_code`, chung volume `java_code_mysql_data` | Bình thường. Seeder tạo lại dữ liệu mẫu khi app khởi động |
| `dependency mysql failed to start` / `container demo-mysql is unhealthy` ngay sau `down -v` | MySQL khởi tạo lần đầu chậm hơn thời gian chờ của healthcheck | Chờ khoảng 30 giây rồi `up -d` lại |
| App chạy bằng Gradle trên máy báo `Communications link failure` | Mặc định `DB_HOST=localhost`, `DB_PORT=3306`, nhưng MySQL mở ở `3313` | `DB_PORT=3313 ./gradlew bootRun` |
| `Public Key Retrieval is not allowed` | Thiếu `allowPublicKeyRetrieval=true` với `caching_sha2_password` | Thêm tham số vào chuỗi kết nối |
| `Data too long for column 'description'` | Cột được tạo trước khi thêm `length = 2000` | `ALTER TABLE ... MODIFY ...` hoặc reset |
| Xoá danh mục → trang 500, log `foreign key constraint fails` | Còn sản phẩm trỏ tới danh mục | Chuyển sản phẩm sang danh mục khác trước, hoặc làm bài tập "xoá an toàn" bài 08 |
| `Duplicate entry 'an' for key ...` | Ràng buộc `UNIQUE` của `users.username` | Bình thường. `UserService.register()` đã kiểm tra trước bằng `existsByUsername` |
| `SELECT` thấy `?i?n tho?i` | Client `mysql` hiển thị sai bảng mã. Dữ liệu không hỏng | Thêm `--default-character-set=utf8mb4` |
| Insert user bằng SQL rồi đăng nhập luôn báo sai | Mật khẩu lưu dạng chữ thường, Spring so bằng BCrypt | Đăng ký qua `/register`, hoặc dùng `admin` do seeder tạo ([spring-security.md](spring-security.md)) |
| `./scripts/reset-db.sh: No such file or directory` | Thư mục thật là `script/` | `./script/reset-db.sh` |

## Đọc thêm

- [MySQL 8.4 — Data Types](https://dev.mysql.com/doc/refman/8.4/en/data-types.html): đầy đủ các kiểu cột.
- [MySQL 8.4 — FOREIGN KEY Constraints](https://dev.mysql.com/doc/refman/8.4/en/create-table-foreign-keys.html): `ON DELETE CASCADE`, `SET NULL`, `RESTRICT`.
- [MySQL 8.4 — Transactional and Locking Statements](https://dev.mysql.com/doc/refman/8.4/en/sql-transactional-statements.html): `START TRANSACTION`, `COMMIT`, `ROLLBACK`, autocommit.
- [MySQL 8.4 — Keywords and Reserved Words](https://dev.mysql.com/doc/refman/8.4/en/keywords.html): từ nào phải đặt trong dấu `` ` ``.
- [MySQL 8.4 — Caching SHA-2 Pluggable Authentication](https://dev.mysql.com/doc/refman/8.4/en/caching-sha2-pluggable-authentication.html)
- [Image `mysql` trên Docker Hub](https://hub.docker.com/_/mysql): ý nghĩa các biến `MYSQL_*`.
- Trong repo: [jpa-hibernate.md](jpa-hibernate.md) (entity → bảng, `ddl-auto`, `@Transactional`), [docker.md](docker.md) (container, volume, network), [bài 15.5](../study/15.5-reset-du-lieu-va-chay-lai-seeder.md) (reset dữ liệu).
