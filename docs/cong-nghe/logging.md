# Logging: SLF4J & Logback

> **Dùng trong project từ:** [bài 13](../study/13-du-lieu-mau-seeder.md) (logger đầu tiên, trong `DataSeeder`) · **Phiên bản:** SLF4J 2.0.16 + Logback 1.5.12 (Spring Boot 3.4.1 tự chọn, kéo vào qua `spring-boot-starter-logging`, không có dòng nào trong `build.gradle`) · **Tài liệu chính thức:** [Spring Boot Logging](https://docs.spring.io/spring-boot/3.4/reference/features/logging.html), [SLF4J Manual](https://www.slf4j.org/manual.html), [Logback Manual](https://logback.qos.ch/manual/)
>
> 🔼 **Sau [bài 16.5](../study/16.5-nang-cap-spring-boot-4.md)** (người học chưa làm): SLF4J **2.0.20**, Logback **1.5.38**; file log mặc định UTF-8. Code log không phải sửa.

## Nó là gì, giải quyết vấn đề gì

`System.out.println("đã tạo admin")` in ra chữ trơn: không biết lúc nào, ở class nào, quan trọng hay không. Cũng không tắt được khi không cần, không tự ghi ra file. **Logging** giải quyết những việc đó: mỗi dòng log có thời gian, mức độ, tên class. Bật/tắt theo mức và theo package bằng cấu hình, không phải sửa code. Ghi ra console, ra file, tự xoay vòng file cũ.

Trong Java, logging chia làm **hai lớp**. Bên PHP cũng chia y như vậy:

| Vai trò | Java (project này) | PHP / Laravel |
|---|---|---|
| **Giao diện**: code của mình gọi vào đây | **SLF4J** (`org.slf4j.Logger`) | PSR-3 (`Psr\Log\LoggerInterface`) |
| **Bản cài đặt**: thật sự định dạng và ghi log | **Logback** | Monolog |
| Cấu hình | `logging.*` trong `application.yml` | `config/logging.php` |

Code chỉ biết SLF4J. Muốn đổi Logback sang thư viện khác (ví dụ Log4j2) thì chỉ đổi thư viện trong `build.gradle`, không sửa dòng `log.info(...)` nào. Spring Boot chọn sẵn Logback làm mặc định.

Các thư viện khác trong app tự dùng những API log khác nhau: Spring dùng Commons Logging, Hibernate dùng JBoss Logging, Tomcat dùng `java.util.logging`. Spring Boot sắp sẵn "cầu nối" để **mọi** API đó đổ về cùng một chỗ là Logback: `spring-jcl` (có sẵn trong Spring) chuyển Commons Logging sang SLF4J, `jul-to-slf4j` (trong `spring-boot-starter-logging`) chuyển `java.util.logging`, còn JBoss Logging tự nhận ra Logback. Nhờ vậy chỉ một khối `logging:` trong `application.yml` điều khiển được log của cả app lẫn thư viện.

```
ProductStatsService  -- SLF4J -------------\
Spring Framework     -- Commons Logging ----+--> Logback --> console (docker compose logs)
Hibernate            -- JBoss Logging -----+             \-> logs/app.log
Tomcat               -- java.util.logging -/
```

## Khái niệm cốt lõi

### Logger mang tên class

```java
private static final Logger log = LoggerFactory.getLogger(ProductStatsService.class);
```

- `LoggerFactory.getLogger(...)` (SLF4J) trả về một `Logger` có **tên** là tên đầy đủ của class: `com.example.demo.stats.ProductStatsService`.
- `static final`: cả class dùng chung một logger, tạo một lần.
- Tên biến `log` chỉ là quy ước. Lombok `@Slf4j` cũng sinh đúng tên này ([bài 17](../study/17-lombok-slf4j-getter-setter.md#bước-2--slf4j-bỏ-dòng-khai-báo-logger), [lombok.md](lombok.md)).

Tên logger có dấu chấm nên tạo thành **cây cha-con** theo package: `com` → `com.example` → `com.example.demo` → `com.example.demo.stats.ProductStatsService`. Gốc của cây là **`root`**. Cấu hình mức cho một nút thì mọi nút con **kế thừa**, trừ khi nút con được cấu hình riêng. Vì vậy không nên dùng chung một logger cho cả app (xem [Hỏi đáp](#mỗi-class-đều-phải-tự-khai-báo-logger-có-tạo-một-logger-common-rồi-import-vào-được-không)).

### Năm mức log

Từ thấp đến cao: `TRACE < DEBUG < INFO < WARN < ERROR`. Mỗi mức có một method cùng tên: `log.trace(...)`, `log.debug(...)`, `log.info(...)`, `log.warn(...)`, `log.error(...)`.

| Mức | Khi nào dùng | Ví dụ trong project |
|---|---|---|
| `ERROR` | Một chức năng **hỏng**, cần người vào xem | `[NoSQL] không kết nối được Redis` |
| `WARN` | Bất thường nhưng app **vẫn chạy tiếp** được | `[Stats] Redis lỗi, ...`, `[Upload] không xóa được file ...` |
| `INFO` | Sự kiện bình thường, đáng biết, **ít** xảy ra | `[Seed] đã tạo admin`, `[Config] đã nạp ...` |
| `DEBUG` | Chi tiết để gỡ lỗi, xảy ra **thường xuyên** | Chưa có (bài tập 2 của [bài 17](../study/17-lombok-slf4j-getter-setter.md#bài-tập)) |
| `TRACE` | Còn chi tiết hơn | Hầu như không dùng |

Cấu hình đặt **ngưỡng**: logger đặt mức `INFO` thì in `INFO`, `WARN`, `ERROR`, bỏ `DEBUG` và `TRACE`. PSR-3 bên PHP có 8 mức (`notice`, `critical`, `alert`, `emergency`...). SLF4J chỉ có 5, và **không có `FATAL`**.

Cách chọn mức đúng ở [bài 17 Bước 5](../study/17-lombok-slf4j-getter-setter.md#bước-5--dùng-log-cho-đúng).

### `logging.level`: bật/tắt theo package

```yaml
logging:
  level:
    root: INFO               # mặc định cho mọi logger (Spring, Hibernate, Tomcat...)
    com.example.demo: DEBUG  # code của mình: in cả DEBUG
```

Key bên dưới `level` là **tên logger** (thường là tên package). Logger tìm cấu hình ở **nút gần nhất** trên cây:

| Logger | Nút gần nhất có cấu hình | Mức có hiệu lực |
|---|---|---|
| `com.example.demo.stats.ProductStatsService` | `com.example.demo` | `DEBUG` |
| `org.hibernate.SQL` | `root` | `INFO` |
| `org.springframework.web` | `root` | `INFO` |

Vài cách đổi mức khác:

- Thêm dòng cho một package thư viện, ví dụ `org.springframework.security: DEBUG` để xem Spring Security quyết định chặn hay cho qua thế nào.
- Spring Boot có sẵn 2 **nhóm** logger: `web` (các package web của Spring) và `sql` (gồm `org.hibernate.SQL`, `org.springframework.jdbc.core`). Viết `logging.level.sql: DEBUG` là in mọi câu SQL **qua hệ thống log**.
- Đặt bằng biến môi trường, không sửa file: `LOGGING_LEVEL_COM_EXAMPLE_DEMO=INFO`. Cách này chỉ dùng được cho tên package, vì biến môi trường không giữ được chữ hoa của tên class.

### Một dòng log gồm những gì

Dòng thật, lấy từ `logs/app.log` của project:

```
2026-11-10T03:05:39.757Z  INFO 283 --- [demo] [restartedMain] c.e.demo.stats.ProductStatsService       : [Stats] recent-ttl=PT720H, recent-limit=5, top-limit=5
└──────── thời gian ─────┘ └mức┘ └PID┘    └app┘ └── luồng ───┘ └──────── tên logger ────────────────┘   └──────────── nội dung ───────────────────┘
```

| Phần | Ý nghĩa |
|---|---|
| `2026-11-10T03:05:39.757Z` | Thời gian, chính xác tới mili giây. **`Z` = giờ UTC**: container Docker chạy giờ UTC, nên chậm hơn giờ Việt Nam 7 tiếng |
| `INFO` | Mức log |
| `283` | Mã tiến trình (PID) |
| `---` | Vạch ngăn: từ đây trở đi là thông tin của dòng log |
| `[demo]` | Tên app, lấy từ `spring.application.name: demo` |
| `[restartedMain]` | Tên **luồng** đang chạy. `restartedMain` là luồng khởi động khi có DevTools. Log trong lúc xử lý request có dạng `[http-nio-8080-exec-2]` (trên console, tên luồng dài bị cắt bớt còn 15 ký tự: `[nio-8080-exec-2]`) |
| `c.e.demo.stats.ProductStatsService` | Tên logger. Dài quá 40 ký tự thì package bị **viết tắt** thành chữ cái đầu (`c.e.` = `com.example.`) |
| sau dấu `:` | Câu log mình viết |

Định dạng này là mặc định của Spring Boot. Project không tự cấu hình định dạng.

### Placeholder `{}` và exception ở cuối

```java
log.warn("[Stats] Redis lỗi, không ghi được lượt xem sản phẩm {}: {}", productId, e.getMessage());
log.warn("[Upload] không xoá được file {}", filename, e);   // e ở cuối, không có {} → in cả stack trace
```

Mỗi `{}` được điền lần lượt bằng các tham số phía sau. Tham số **cuối cùng** là exception mà không có `{}` dành cho nó thì SLF4J in kèm toàn bộ stack trace. Chi tiết, kể cả các trường hợp số `{}` không khớp số tham số, ở [Hỏi đáp: dấu `{}`](#dấu--trong-loginfo--logwarn-là-gì) và [`e.getMessage()` và `e`](#egetmessage-và-e-khác-nhau-thế-nào).

### Ghi ra file và xoay vòng file

Mặc định Spring Boot chỉ in log ra **console**. Project bật thêm ghi file:

```yaml
logging:
  file:
    name: ${LOG_FILE:logs/app.log}   # đường dẫn tương đối, tính từ thư mục đang chạy app
  logback:
    rollingpolicy:
      max-file-size: 10MB
      max-history: 7
```

- `logging.file.name`: tên file. Có thêm `logging.file.path` (chỉ đặt thư mục, file sẽ tên `spring.log`). Đặt cả hai thì Spring Boot chỉ dùng `name`.
- **Rolling (xoay vòng)**: khi file đạt `max-file-size`, hoặc khi **sang ngày mới**, Logback nén file hiện tại thành dạng `app.log.2026-11-09.0.gz` rồi mở file `app.log` mới. Tên file nén theo mẫu mặc định `${LOG_FILE}.%d{yyyy-MM-dd}.%i.gz`: `%d` là ngày, `%i` là số thứ tự khi một ngày có nhiều file.
- `max-history: 7`: giữ tối đa 7 kỳ lưu trữ. Mẫu tên file theo **ngày**, nên 7 kỳ là 7 ngày. File cũ hơn bị Logback tự xoá.
- `10MB` và `7` chính là **giá trị mặc định** của Spring Boot. Project ghi ra cho rõ ý, bỏ đi thì hành vi không đổi.
- File và console có **cùng ngưỡng mức** (theo `logging.level`).

Trong thư mục `logs/` của project lúc này có đúng 3 file: `app.log` và hai file `.gz` của các ngày trước. Thư mục `logs/` nằm trong `.gitignore`.

### Log trước khi hệ thống log sẵn sàng: `DeferredLogFactory`

Spring Boot chỉ cấu hình Logback **sau khi** đọc xong `application.yml` (phải đọc thì mới biết `logging.*`). Code nào chạy **trước** thời điểm đó mà ghi log bằng SLF4J thì dòng log có thể bị mất, sai định dạng, hoặc không vào file.

`ModuleConfigLoader` (một `EnvironmentPostProcessor`, [bài 16 Bước 2](../study/16-luot-xem-top-san-pham-redis.md#bước-2--mỗi-module-một-file-properties-đọc-qua-package-common)) chính là code chạy sớm như vậy. Vì thế Spring Boot đưa vào constructor của nó một `DeferredLogFactory`:

```java
private final Log log;   // org.apache.commons.logging.Log, KHÔNG phải SLF4J

public ModuleConfigLoader(DeferredLogFactory logFactory) {
    this.log = logFactory.getLog(ModuleConfigLoader.class);
}
...
log.info("[Config] đã nạp config/" + file.getFilename() + " (" + props.size() + " key)");
```

Logger "hoãn" này **giữ lại** các dòng log trong bộ nhớ. Khi Logback đã được cấu hình xong, Spring Boot mới in chúng ra. Có thể thấy điều này trong `logs/app.log`: dòng `[Config] đã nạp ...` đứng **sau** dòng `Starting DemoApplication` và `No active profile set`, dù thực ra nó chạy trước.

Kiểu `Log` này theo API Commons Logging. Nó **không hiểu `{}`**, nên ở đây phải nối chuỗi bằng `+`. Lombok cũng không có annotation sinh ra loại logger này, nên class này giữ cách khai báo tay ([bài 17](../study/17-lombok-slf4j-getter-setter.md#vì-sao-moduleconfigloader-không-dùng-slf4j)).

### Quy ước nhãn `[Module]`

Mọi câu log trong project mở đầu bằng nhãn module: `[Seed]`, `[NoSQL]`, `[Upload]`, `[Product]`, `[Stats]`, `[Config]`. Tên logger đã cho biết **class**, còn nhãn cho biết **chức năng**. Nhờ nhãn, lọc log bằng `grep "\[Stats\]"` rất nhanh. Khi thêm module mới (ví dụ `review/` ở bài 18), đặt nhãn mới theo tên module.

## Project này dùng nó thế nào

Các class có log (kiểm tra ngày 2026-11-10, **trước** bài 17: vẫn khai báo tay bằng `LoggerFactory`):

| Class | Nhãn | Ghi gì | Bài |
|---|---|---|---|
| `seed/DataSeeder` | `[Seed]` | `info`: đã có/đã tạo admin, dữ liệu mẫu | [13](../study/13-du-lieu-mau-seeder.md) |
| `nosql/NoSqlConnectionCheck` | `[NoSQL]` | `info` khi kết nối Mongo/Redis được, `error` khi không | [14](../study/14-moi-truong-mongodb-redis.md#cách-1-đọc-log-khởi-động) |
| `storage/FileStorageService` | `[Upload]` | `info`: thư mục lưu file. `warn`: không xoá được file | [15](../study/15-anh-va-mo-ta-san-pham.md) |
| `product/ProductService` | `[Product]` | `warn`: ảnh mặc định không tồn tại | [15](../study/15-anh-va-mo-ta-san-pham.md), [16](../study/16-luot-xem-top-san-pham-redis.md) |
| `stats/ProductStatsService` | `[Stats]` | `info`: cấu hình đã đọc. `warn` (5 chỗ): Redis lỗi, dùng đường lui | [16](../study/16-luot-xem-top-san-pham-redis.md) |
| `common/ModuleConfigLoader` | `[Config]` | `info`: file cấu hình đã nạp. Dùng `DeferredLogFactory` | [16](../study/16-luot-xem-top-san-pham-redis.md) |

Mẫu chung của một chỗ "có đường lui", trích `ProductStatsService`:

```java
try {
    ...
} catch (DataAccessException e) {
    log.warn("[Stats] Redis lỗi, không đọc được top sản phẩm: {}", e.getMessage());
    return List.of();   // trang vẫn hiện, chỉ thiếu khung Top
}
```

Mức `warn` vì app vẫn chạy được. Ghi `e.getMessage()` thay vì `e` vì Redis chết thì **mỗi lần mở trang** lại có một dòng, in cả stack trace thì log ngập rất nhanh. Doc module: [lượt xem & top](../product/luot-xem-va-top-san-pham.md), [lưu file upload](../storage/luu-file-upload.md), [kiểm tra kết nối NoSQL](../nosql/kiem-tra-ket-noi-mongo-redis.md), [cấu hình module](../common/cau-hinh-module.md).

**Sau bài 17** (người học chưa làm), 5 class đầu đổi sang `@Slf4j`. Câu log và tên logger in ra giữ nguyên. `ModuleConfigLoader` giữ nguyên.

### Xem log khi chạy bằng Docker

Console của app chính là output của container:

```bash
# Chế độ dev
docker compose -f docker-compose.dev.yml logs -f app-dev                 # theo dõi liên tục
docker compose -f docker-compose.dev.yml logs --tail=100 app-dev         # 100 dòng cuối
docker compose -f docker-compose.dev.yml logs app-dev | grep "\[Stats\]" # lọc theo nhãn

# Chế độ thường (service tên app, container demo-app)
docker compose logs -f app
```

File `logs/app.log` thì khác nhau theo chế độ:

- **Dev** (`docker-compose.dev.yml`): container chạy trong `/app`, mà `/app` là thư mục project được mount từ máy bạn. File nằm ngay ở `./logs/app.log` trên máy, mở bằng VSCode hay `tail -f logs/app.log` đều được.
- **Thường** (`docker-compose.yml`): file nằm **trong container** (`/app/logs/app.log`), không có volume nào. Xoá container là mất. Muốn đọc: `docker exec demo-app tail -n 100 logs/app.log`.

## Tra cứu nhanh

| Cú pháp / hàm | Của ai | Làm gì | Ghi chú |
|---|---|---|---|
| `LoggerFactory.getLogger(X.class)` | SLF4J (`org.slf4j.LoggerFactory`) | Tạo logger mang tên class `X` | Trả `org.slf4j.Logger` |
| `log.trace/debug/info/warn/error(msg, args...)` | SLF4J (`Logger`) | Ghi một dòng ở mức tương ứng | `{}` là chỗ trống. Exception ở cuối → stack trace |
| `log.isDebugEnabled()` | SLF4J (`Logger`) | Hỏi mức `DEBUG` có đang bật không | Dùng khi **tính tham số** tốn công |
| `@Slf4j` | Lombok | Sinh dòng `getLogger` ở trên | Bài 17, xem [lombok.md](lombok.md) |
| `DeferredLogFactory.getLog(X.class)` | Spring Boot (`org.springframework.boot.logging`) | Logger hoãn cho code chạy trước khi log sẵn sàng | Trả `org.apache.commons.logging.Log`. Không hiểu `{}` |
| `logging.level.<logger>: <MỨC>` | Spring Boot | Đặt ngưỡng cho một logger và các logger con | `root` là gốc. Nhóm có sẵn: `web`, `sql` |
| `logging.file.name` | Spring Boot | Ghi thêm log ra file | Không đặt thì chỉ có console |
| `logging.logback.rollingpolicy.max-file-size` | Spring Boot | Kích thước tối đa trước khi nén file | Mặc định `10MB` |
| `logging.logback.rollingpolicy.max-history` | Spring Boot | Số kỳ lưu trữ giữ lại | Mặc định `7` |
| `logging.logback.rollingpolicy.total-size-cap` | Spring Boot | Giới hạn tổng dung lượng các file nén | Project không đặt |
| `LOGGING_LEVEL_COM_EXAMPLE_DEMO=INFO` | Spring Boot (biến môi trường) | Đổi mức mà không sửa file | Chỉ dùng được với tên package |
| `logback-spring.xml` | Logback + Spring Boot | Cấu hình nâng cao (định dạng riêng, nhiều file...) | Project **không** dùng. Nên đặt tên có `-spring`, không phải `logback.xml` |
| `docker compose -f docker-compose.dev.yml logs -f app-dev` | Docker | Xem console của app | Thêm `--tail=N`, `\| grep` |

## Bẫy thường gặp

| Triệu chứng | Nguyên nhân | Cách sửa |
|---|---|---|
| Giá trị không được in ra, không có lỗi | Thiếu `{}`: tham số dư bị bỏ qua âm thầm | Đếm `{}` khớp số tham số. [Hỏi đáp](#khi-số--và-số-tham-số-không-khớp-nhau) |
| In ra nguyên chữ `%s` | Dùng cú pháp `String.format` trong `log.info` | Dùng `{}`. [Hỏi đáp](#đừng-nhầm-với-stringformat) |
| `log.debug(...)` không in gì | Logger của class đó đang ở mức `INFO` trở lên | Kiểm tra `logging.level`. Code ngoài `com.example.demo` theo `root: INFO` |
| Bật DEBUG cho thư viện xong, log ngập hàng nghìn dòng | Đặt `root: DEBUG` | Chỉ bật cho đúng package cần xem, ví dụ `org.springframework.security: DEBUG` |
| Câu SQL (`Hibernate: select ...`) có trong `docker logs` nhưng **không có** trong `logs/app.log` | `spring.jpa.show-sql: true` in thẳng ra console bằng `System.out`, không qua Logback | Muốn SQL vào file thì dùng `logging.level.sql: DEBUG` (hoặc `org.hibernate.SQL: DEBUG`) thay cho `show-sql`. Xem [jpa-hibernate.md](jpa-hibernate.md) |
| Giờ trong log lệch 7 tiếng | Container chạy giờ UTC (đuôi `Z`) | Bình thường. Cộng 7 giờ để ra giờ Việt Nam |
| Không thấy `logs/app.log` ở chế độ thường | File nằm trong container, không được mount ra máy | `docker exec demo-app tail logs/app.log`, hoặc thêm volume |
| Stack trace dài in ra **mỗi request** | Truyền `e` vào cuối cho một lỗi lặp lại liên tục (ví dụ Redis chết) | Lỗi đã biết rõ thì ghi `e.getMessage()` |
| Lỗi in **hai lần** ở hai nơi | `log.error(...)` rồi lại `throw` cùng lỗi lên trên | Hoặc xử lý và ghi log, hoặc ném lên. Không làm cả hai |
| Log in ra mật khẩu (dù đã mã hoá) | `log.info("{}", user)` khi entity có `toString()` in mọi trường (`@ToString`, `@Data`) | Không log cả entity chứa dữ liệu nhạy cảm. Xem [lombok.md](lombok.md#bẫy-thường-gặp) |
| Log của `ModuleConfigLoader` lỗi định dạng hoặc mất khi đổi sang SLF4J | Code chạy trước khi Logback được cấu hình | Giữ `DeferredLogFactory` |
| `[Config] ...` in ra chữ `{}` | Dùng `{}` với `Log` của Commons Logging | Nối chuỗi bằng `+` (chỉ riêng class này) |
| Dòng log mang tên class khác | Copy dòng khai báo logger sang file mới mà quên sửa `X.class` | Sửa tên, hoặc dùng `@Slf4j` (bài 17) |
| Thêm `logback.xml` xong thì `logging.file.*` trong yml hết tác dụng | File XML tự viết thay cấu hình mặc định của Spring Boot | Không cần XML với nhu cầu hiện tại. Nếu cần thì đặt tên `logback-spring.xml` và tự khai báo appender file |

## Đọc thêm

- [Spring Boot — Logging](https://docs.spring.io/spring-boot/3.4/reference/features/logging.html): định dạng mặc định, `logging.level`, nhóm logger, ghi file, `logback-spring.xml`.
- [SLF4J Manual](https://www.slf4j.org/manual.html) và [SLF4J FAQ](https://www.slf4j.org/faq.html): vì sao `{}` nhanh hơn nối chuỗi, cách in exception. SLF4J 2 có thêm kiểu viết `log.atInfo().addKeyValue("id", id).log("...")`, project chưa dùng.
- [Logback Manual — Appenders](https://logback.qos.ch/manual/appenders.html): `RollingFileAppender`, `SizeAndTimeBasedRollingPolicy`, ý nghĩa `maxHistory`.
- Actuator có endpoint `/actuator/loggers` để **đổi mức log lúc app đang chạy**. Project chỉ mở `health,info` nên chưa dùng được. Xem [spring-boot.md](spring-boot.md).
- Trong repo: [lombok.md](lombok.md) (`@Slf4j`), [docker.md](docker.md) (`docker compose logs`).

## Hỏi đáp

Các câu hỏi phát sinh trong lúc học, chuyển từ sổ tay chung sang đây.

### Dấu `{}` trong `log.info(...)` / `log.warn(...)` là gì?

> Hỏi ở: [Bài 15 — Bước 4: `FileStorageService`: lưu và xoá file](../study/15-anh-va-mo-ta-san-pham.md#bước-4--filestorageservice-lưu-và-xoá-file). Cùng kiểu viết này cũng có ở [Bài 14](../study/14-moi-truong-mongodb-redis.md).

#### `{}` là chỗ trống, được điền lần lượt bằng các tham số phía sau

`log` là logger của **SLF4J** (`org.slf4j.Logger`). Tham số đầu tiên là một **mẫu câu**. Mỗi `{}` trong mẫu câu được thay bằng các tham số phụ **theo đúng thứ tự**: `{}` thứ nhất nhận tham số thứ nhất, `{}` thứ hai nhận tham số thứ hai...

```java
log.warn("[Upload] Không xoá được file {}: {}", filename, e.getMessage());
//                                     ↑   ↑      ↑          ↑
//                                     └───┼──────┘          │
//                                         └─────────────────┘
// → [Upload] Không xoá được file 3f2a...jpg: Permission denied
```

Giá trị được đổi sang chuỗi bằng `toString()`. Nếu giá trị là `null` thì in ra chữ `null`, không bị lỗi.

#### Khi số `{}` và số tham số không khớp nhau

| Trường hợp | Kết quả |
|---|---|
| Thừa `{}` (ít tham số hơn) | Các `{}` không có giá trị đi kèm sẽ in nguyên chữ `{}` |
| Thừa tham số (thiếu `{}`) | Các tham số dư bị **bỏ qua âm thầm**, không có lỗi nào báo ra |
| Tham số **cuối cùng** là exception (`e`) và không có `{}` nào dành cho nó | Trường hợp đặc biệt: SLF4J in ra **cả stack trace** của lỗi |

Trường hợp "thừa tham số" rất dễ mắc mà không biết. Ví dụ dòng sau **thiếu `{}`**, nên `value` không bao giờ được in ra:

```java
log.info("[NoSQL] Redis OK, demo:last-startup", value);       // ✗ value bị bỏ qua
log.info("[NoSQL] Redis OK, demo:last-startup = {}", value);  // ✓
```

Chỉ có **`{}` là bắt buộc**. Dấu `=` (hay `:`, `,`...) đứng trước nó chỉ là chữ thường trong câu log, được in ra nguyên như vậy và SLF4J không xử lý gì. Thêm vào là để đọc log dễ hiểu hơn. Cả 3 cách viết sau đều điền đúng `value`, chỉ khác ở câu in ra:

```java
log.info("Redis OK, last-startup = {}", value);  // → Redis OK, last-startup = 2026-10-28T...
log.info("Redis OK, last-startup: {}", value);   // → Redis OK, last-startup: 2026-10-28T...
log.info("Redis OK {}", value);                  // → Redis OK 2026-10-28T...
```

#### `e.getMessage()` và `e`: khác nhau thế nào?

```java
log.warn("Không xoá được file {}: {}", filename, e.getMessage());  // 1 dòng, chỉ có câu báo lỗi
log.warn("Không xoá được file {}", filename, e);                   // câu báo lỗi + toàn bộ stack trace
```

- Dùng `e.getMessage()` khi lỗi đã được lường trước và chỉ cần biết ngắn gọn (như xoá file thất bại).
- Truyền `e` vào cuối khi cần biết lỗi xảy ra **ở dòng code nào**, để gỡ lỗi.

#### Vì sao không nối chuỗi bằng `+` cho nhanh?

```java
log.debug("Sản phẩm: " + product);   // luôn nối chuỗi, kể cả khi mức DEBUG đang tắt
log.debug("Sản phẩm: {}", product);  // chỉ ghép chuỗi khi mức DEBUG đang bật
```

Logger có các mức `trace < debug < info < warn < error`. Mức nào bị tắt trong cấu hình thì dòng log ở mức đó không được in ra. Khi dùng `{}`, việc ghép chuỗi chỉ thực sự diễn ra lúc dòng log **sắp được in**. Còn với `+`, chuỗi luôn được ghép trước, kể cả khi sau đó dòng log bị bỏ đi, tức là làm tốn công vô ích. Thêm nữa, mẫu câu có `{}` cũng dễ đọc hơn.

Lưu ý: bản thân các tham số **vẫn được tính trước** khi gọi `log`. Ví dụ `mongoTemplate.getDb().getName()` vẫn chạy dù log có bị tắt hay không. Chỉ riêng bước ghép chuỗi là được hoãn lại.

#### Đừng nhầm với `String.format`

| | SLF4J `log.info(...)` | `String.format(...)` / `printf` |
|---|---|---|
| Chỗ trống | `{}` (không quan tâm kiểu dữ liệu) | `%s`, `%d`, `%.2f`... (phải đúng kiểu) |
| Ví dụ | `log.info("Giá: {}", price)` | `String.format("Giá: %.2f", price)` |

Viết `%s` trong `log.info` thì SLF4J không hiểu, nên sẽ in nguyên chữ `%s`.

---

### Mỗi class đều phải tự khai báo logger. Có tạo một logger "common" rồi import vào được không?

> Hỏi ở: [Bài 16](../study/16-luot-xem-top-san-pham-redis.md), khi thấy file nào cũng có dòng `private static final Logger log = LoggerFactory.getLogger(NoSqlConnectionCheck.class);`. Phần thực hành ở [**bài 17**](../study/17-lombok-slf4j-getter-setter.md).

#### Vì sao mỗi class lại có logger riêng?

Tham số `NoSqlConnectionCheck.class` là **tên của logger**, và nó được in ra trên từng dòng log:

```
INFO ... c.e.demo.nosql.NoSqlConnectionCheck : [NoSQL] MongoDB OK, database = ...
INFO ... c.e.demo.product.ProductService     : ...
```

Nhờ tên này mà bạn biết dòng log đến từ class nào. Cấu hình `logging.level.com.example.demo: DEBUG` trong `application.yml` cũng dựa vào tên đó để bật/tắt log theo package.

#### Tạo một logger chung trong `common/` được không?

Viết được, nhưng **không nên**:

```java
// ❌ common/Log.java
public class Log {
    public static final Logger log = LoggerFactory.getLogger("app");
}
```

Mọi dòng log khi đó đều mang tên `app`, nên không còn biết dòng nào của class nào, và cũng không bật/tắt theo package được nữa. Java không có cách nào "import" một biến mà biến đó **tự biết** nó đang được dùng trong class nào. Vì vậy mỗi class phải có một dòng khai báo riêng, hoặc để công cụ sinh hộ dòng đó.

#### Các cách làm gọn

| Cách | Viết trong class | Đánh giá |
|---|---|---|
| Viết tay (đang dùng) | `private static final Logger log = LoggerFactory.getLogger(ProductService.class);` | Dài. Copy sang file khác mà quên sửa tên class thì log ghi sai tên |
| Không phải gõ tên class | `private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());` | Copy-paste thoải mái, không bao giờ sai tên. Nhưng vẫn là 1 dòng, còn dài hơn |
| Interface dùng chung | `class X implements Loggable` rồi gọi `log().info(...)` | Gọn, nhưng class bị Spring bọc proxy (ví dụ `@Configuration`) sẽ in ra tên lạ kiểu `WebConfig$$SpringCGLIB$$0` |
| **Lombok `@Slf4j`** | Chỉ cần đặt `@Slf4j` trên class | **Cách phổ biến nhất.** Lúc biên dịch, Lombok tự sinh đúng dòng khai báo ở trên, với tên biến là `log`, nên code cũ dùng `log.info(...)` không phải sửa gì |
| Snippet của VSCode | Gõ tắt `logger` + Tab, VSCode tự bung ra dòng khai báo | Không thêm thư viện nào, nhưng file vẫn có dòng đó |

Ví dụ với Lombok:

```java
@Slf4j        // Lombok sinh: private static final Logger log = LoggerFactory.getLogger(ProductService.class);
@Service
public class ProductService {
    // không còn dòng khai báo log, cũng không cần import Logger / LoggerFactory
    ...
        log.warn("[Product] ...");   // dùng y như cũ
}
```

Để dùng được, phải thêm Lombok vào `build.gradle` (2 dòng `compileOnly` và `annotationProcessor`). Chi tiết ở [bài 17](../study/17-lombok-slf4j-getter-setter.md#bước-1--buildgradle-thêm-lombok).

> Hiện project không có lỗi copy-paste nào: kiểm tra ngày 2026-11-04 thì cả 5 file khai báo logger đều ghi đúng tên class của chính nó.
