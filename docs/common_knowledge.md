# Sổ tay kiến thức chung (mục lục)

Trang này là **cửa vào** của mọi tài liệu kiến thức trong `docs/`. Nội dung chi tiết nằm ở các file riêng, mỗi file tập trung vào **một** chủ đề, để câu hỏi mới về cùng chủ đề được ghi tiếp vào đúng chỗ thay vì nối dài một file.

| Thư mục | Chứa gì |
|---|---|
| [`study/`](study/) | Bài học, làm theo thứ tự 01 → 17 |
| `cong-nghe/` | Giới thiệu chi tiết từng **công nghệ** project dùng (Spring, Redis, MongoDB...), cuối mỗi file có mục **Hỏi đáp** |
| `kien-thuc/` | Kiến thức **chung**, không gắn riêng công nghệ nào (cú pháp Java, cách đọc bài học...) |
| `<module>/` (`product/`, `category/`...) | Tài liệu từng **chức năng** đã làm trong project |
| [`ke-hoach.md`](ke-hoach.md) | Kế hoạch đang chờ |

## Công nghệ

| Tài liệu | Nội dung |
|---|---|
| [Spring Boot](cong-nghe/spring-boot.md) | Nền tảng của cả app: bean, dependency injection, auto-configuration, starter, cấu hình, DevTools, Actuator |
| [Spring Web MVC](cong-nghe/spring-web-mvc.md) | Controller trang web và REST API, mapping URL, nhận tham số, form, upload file, JSON |
| [Spring Data JPA & Hibernate](cong-nghe/jpa-hibernate.md) | Entity, Repository, quan hệ giữa bảng, `ddl-auto`, lazy loading |
| [Spring Security](cong-nghe/spring-security.md) | Đăng nhập form và HTTP Basic, mã hoá mật khẩu, CSRF, phân quyền |
| [Thymeleaf](cong-nghe/thymeleaf.md) | Template HTML phía server, layout dialect, form, gọi bean trong template |
| [MySQL](cong-nghe/mysql.md) | DB chính: bảng, khoá ngoại, kiểu cột, xem dữ liệu bằng dòng lệnh |
| [Redis](cong-nghe/redis.md) | DB phụ trong RAM: kiểu dữ liệu, TTL, lượt xem và "vừa xem" (bài 16) |
| [MongoDB](cong-nghe/mongodb.md) | DB phụ dạng document: collection, document, so sánh với MySQL |
| [Docker & Docker Compose](cong-nghe/docker.md) | Image, container, volume, network, 2 chế độ chạy của project |
| [Gradle](cong-nghe/gradle.md) | Build Java, khai báo thư viện, các task hay dùng, hot reload |
| [Logging: SLF4J & Logback](cong-nghe/logging.md) | Ghi log, mức log, cấu hình log ra file |
| [Lombok](cong-nghe/lombok.md) | Sinh code lúc biên dịch: `@Slf4j`, `@Getter`, `@Setter` (bài 17) |

## Kiến thức chung

| Tài liệu | Nội dung |
|---|---|
| [Đọc bài học và tự tra cứu](kien-thuc/doc-bai-hoc-va-tra-cuu.md) | Nhãn khối code trong bài học, cách tự tra một hàm có sẵn của thư viện |
| [Java: hàm (method) và tham số](kien-thuc/java-ham-method.md) | Varargs, nạp chồng (overload), ghi đè (`@Override`) |
| [Java: kiểu dữ liệu và generic](kien-thuc/java-kieu-du-lieu.md) | Kiểu nguyên thuỷ và wrapper, generic `<>` |
| [Java: ngoại lệ (exception)](kien-thuc/java-ngoai-le.md) | Bắt lỗi rộng hay hẹp, checked exception |
| [Java: toán tử](kien-thuc/java-toan-tu.md) | `&&`, `\|\|` và cơ chế đoản mạch |

## Tra cứu theo câu hỏi

Mọi câu hỏi đã hỏi trong lúc học, xếp theo tài liệu chứa câu trả lời.

**Spring Boot**

- [`management.endpoints` và `management.endpoint` (có/không có chữ `s`) khác nhau thế nào?](cong-nghe/spring-boot.md#managementendpoints-và-managementendpoint-cókhông-có-chữ-s-khác-nhau-thế-nào)
- [Đưa hằng số (ví dụ `RECENT_TTL` 30 ngày) sang `application.yml` được không? `@Value` có đọc được `Duration` không?](cong-nghe/spring-boot.md#đưa-hằng-số-ví-dụ-recent_ttl-30-ngày-sang-applicationyml-được-không-value-có-đọc-được-duration-không)
- [Mỗi module một class cấu hình riêng (`StatsProperties`, `ProductProperties`) được không? `@ConfigurationProperties` là gì?](cong-nghe/spring-boot.md#mỗi-module-một-class-cấu-hình-riêng-statsproperties-productproperties-được-không-configurationproperties-là-gì)
- [Java có file cấu hình kiểu `.ini` của PHP (`key = "value"`) không? Có cần tự viết hàm đọc chung không?](cong-nghe/spring-boot.md#java-có-file-cấu-hình-kiểu-ini-của-php-key--value-không-có-cần-tự-viết-hàm-đọc-chung-không)

**Spring Data JPA & Hibernate**

- [`@Column`: vì sao có trường khai báo, có trường không?](cong-nghe/jpa-hibernate.md#column-vì-sao-có-trường-khai-báo-có-trường-không)

**MongoDB**

- [BLOB là gì? "Document" của MongoDB có phải là "tài liệu"? Lưu ảnh vào DB hay ra file?](cong-nghe/mongodb.md#blob-là-gì-document-của-mongodb-có-phải-là-tài-liệu-lưu-ảnh-vào-db-hay-ra-file)

**Logging: SLF4J & Logback**

- [Dấu `{}` trong `log.info(...)` / `log.warn(...)` là gì?](cong-nghe/logging.md#dấu--trong-loginfo--logwarn-là-gì)
- [Mỗi class đều phải tự khai báo logger. Có tạo một logger "common" rồi import vào được không?](cong-nghe/logging.md#mỗi-class-đều-phải-tự-khai-báo-logger-có-tạo-một-logger-common-rồi-import-vào-được-không)

**Đọc bài học và tự tra cứu**

- [Chữ `groovy` (`yaml`, `java`...) sau dấu ``` trong bài học là gì? Groovy là gì?](kien-thuc/doc-bai-hoc-va-tra-cuu.md#chữ-groovy-yaml-java-sau-dấu--trong-bài-học-là-gì-groovy-là-gì)
- [Hàm có sẵn của thư viện (ví dụ `opsForZSet`) ở đâu ra? Làm sao biết nó làm gì?](kien-thuc/doc-bai-hoc-va-tra-cuu.md#hàm-có-sẵn-của-thư-viện-ví-dụ-opsforzset-ở-đâu-ra-làm-sao-biết-nó-làm-gì)

**Java: hàm (method) và tham số**

- [`String... args` (3 dấu chấm) trong `run()` là gì?](kien-thuc/java-ham-method.md#string-args-3-dấu-chấm-trong-run-là-gì)
- [Java có giá trị mặc định cho tham số (`String description = ""`) không? Nạp chồng (overload) là gì?](kien-thuc/java-ham-method.md#java-có-giá-trị-mặc-định-cho-tham-số-string-description---không-nạp-chồng-overload-là-gì)
- [`@Override` là gì? Bỏ đi có sao không? Khác gì nạp chồng (overloading)?](kien-thuc/java-ham-method.md#override-là-gì-bỏ-đi-có-sao-không-khác-gì-nạp-chồng-overloading)

**Java: kiểu dữ liệu và generic**

- [Kiểu dữ liệu viết thường (`int`, `long`) và viết hoa (`Integer`, `Long`, `String`, `Duration`) khác nhau thế nào?](kien-thuc/java-kieu-du-lieu.md#kiểu-dữ-liệu-viết-thường-int-long-và-viết-hoa-integer-long-string-duration-khác-nhau-thế-nào)
- [Dấu `()` và dấu `<>` khác nhau thế nào? (`List<Long>`, `new HashMap<>()`, generic)](kien-thuc/java-kieu-du-lieu.md#dấu--và-dấu--khác-nhau-thế-nào-listlong-new-hashmap-generic)

**Java: ngoại lệ (exception)**

- [`catch (IOException e)` và `catch (Exception e)` khác nhau thế nào?](kien-thuc/java-ngoai-le.md#catch-ioexception-e-và-catch-exception-e-khác-nhau-thế-nào)

**Java: toán tử**

- [`return a() && b() && c();` hoạt động thế nào? (toán tử `&&` và cơ chế "đoản mạch")](kien-thuc/java-toan-tu.md#return-a--b--c-hoạt-động-thế-nào-toán-tử--và-cơ-chế-đoản-mạch)

---

## Cách ghi câu hỏi mới

Khi người học hỏi một kiến thức trong bài (quy tắc đầy đủ ở mục **📒 Sổ tay kiến thức** của [CLAUDE.md](../CLAUDE.md)):

1. Chọn **một** file tập trung đúng chủ đề: công nghệ → `cong-nghe/<công-nghệ>.md` (mục **Hỏi đáp**), kiến thức chung → `kien-thuc/<chủ-đề>.md`. Chủ đề đã có mục liên quan thì **bổ sung vào mục đó**. Chưa có file phù hợp thì tạo file chủ đề mới.
2. Thêm một dòng vào **Tra cứu theo câu hỏi** ở trang này (và vào bảng ở trên nếu là file mới).
3. Không viết nội dung chi tiết vào trang này.
