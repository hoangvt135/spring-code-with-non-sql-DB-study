# Spring Boot

> **Dùng trong project từ:** [bài 01](../study/01-tong-quan.md) · **Phiên bản:** Spring Boot 3.4.1 (kéo theo Spring Framework 6.2.1, Tomcat nhúng 10.1.34), Java 21, theo `build.gradle` · **Tài liệu chính thức:** [Spring Boot 3.4 Reference](https://docs.spring.io/spring-boot/3.4/index.html)
>
> 🔼 **Sau [bài 16.5](../study/16.5-nang-cap-spring-boot-4.md)** (người học chưa làm): Spring Boot **4.1.2** (Spring Framework 7.0). Đổi: `EnvironmentPostProcessor` sang package `org.springframework.boot` (Bước 6), `spring.data.mongodb.*` → `spring.mongodb.*`, `server.error.*` → `spring.web.error.*` (Bước 7), Actuator bật sẵn liveness/readiness.

## Nó là gì, giải quyết vấn đề gì

**Spring Framework** là bộ khung lõi: một "nhà máy" tạo và nối các object với nhau, cộng thêm các mảng web (MVC), truy cập DB, bảo mật... **Spring Boot** đứng trên Spring Framework và lo phần "lắp ráp": tự chọn phiên bản thư viện khớp nhau, tự cấu hình theo những gì có trong `build.gradle`, và nhúng sẵn web server vào app. Nhờ vậy project này chỉ có một hàm `main()` mà đã có web, DB, Redis, MongoDB, đăng nhập.

Nếu bạn quen PHP (Laravel), có thể đối chiếu như sau:

| Việc | PHP / Laravel | Spring Boot (project này) |
|---|---|---|
| Chạy web | Nginx/Apache + PHP-FPM. **Mỗi request** chạy script từ đầu, xong là dọn sạch | **Một tiến trình Java chạy liên tục**, Tomcat nằm sẵn bên trong, nghe cổng `8080` |
| Điểm khởi động | `public/index.php` → bootstrap | `DemoApplication.main()` → `SpringApplication.run(...)` |
| Nơi tạo và giữ object dùng chung | Service container: `app()->make()`, `bind`, `singleton` | **IoC container**: các object gọi là **bean** |
| Thêm một thư viện lớn | `composer require` + service provider | Thêm **starter** vào `build.gradle`, **auto-configuration** tự lo phần còn lại |
| Cấu hình | `.env` + `config/*.php`, đọc bằng `config('...')` | `application.yml` + biến môi trường + `config/*.properties`, đọc bằng `AppConfig` |
| Việc chạy một lần sau khi khởi động | `php artisan db:seed` | `CommandLineRunner` (`DataSeeder`) |

Khác biệt quan trọng nhất là dòng đầu tiên. App PHP "sống" trong một request, còn app Spring Boot sống suốt từ lúc khởi động tới lúc tắt. Vì vậy các object như `ProductService` chỉ được tạo **một lần** lúc khởi động, rồi **mọi request dùng chung**. Khởi động thì chậm (vài giây), nhưng request sau đó rất nhanh vì không phải dựng lại gì.

## Khái niệm cốt lõi

### IoC container và bean

Trong PHP thuần, muốn dùng một class thì bạn tự `new`. Với Spring thì ngược lại: bạn **khai báo** class nào cần được quản lý, còn việc tạo object, truyền các object phụ thuộc vào, giữ nó suốt đời app là việc của Spring. Đảo ngược quyền "ai tạo object" như vậy gọi là **Inversion of Control (IoC)**. Phần của Spring làm việc này gọi là **IoC container** (hay `ApplicationContext`), mỗi object nó quản lý gọi là một **bean**.

Mặc định mỗi bean là **singleton**: cả app chỉ có đúng một object `ProductService`. Hệ quả: **không lưu dữ liệu của một request vào field** của bean, vì request khác cũng đọc thấy field đó. Field của bean chỉ nên là các bean khác hoặc giá trị cấu hình, và nên để `final`.

### Đánh dấu bean: `@Component` và các "biệt danh"

Đặt một trong các annotation sau lên class thì Spring sẽ tạo bean cho class đó:

| Annotation | Ý nghĩa | Trong project |
|---|---|---|
| `@Component` | Bean chung chung, không thuộc tầng nào | `DataSeeder`, `NoSqlConnectionCheck`, `AppConfig` |
| `@Service` | Bean chứa logic nghiệp vụ. Thực chất là `@Component` có tên dễ đọc hơn | `ProductService`, `CategoryService`, `UserService`, `FileStorageService`, `ProductStatsService` |
| `@Repository` | Bean truy cập dữ liệu | Không dùng trực tiếp: `ProductRepository` là **interface** `extends JpaRepository`, Spring Data tự sinh class và tự tạo bean (xem [jpa-hibernate.md](jpa-hibernate.md)) |
| `@Controller` / `@RestController` | Bean nhận request HTTP | 6 controller, xem [spring-web-mvc.md](spring-web-mvc.md) |
| `@Configuration` | Class chứa các hàm `@Bean` hoặc tuỳ chỉnh framework | `SecurityConfig`, `WebConfig` |

Vì sao seeder dùng `@Component` mà không phải `@Service`: xem [bài 13](../study/13-du-lieu-mau-seeder.md#vì-sao-component-mà-không-phải-service).

Mỗi bean có một **tên**, mặc định là tên class viết thường chữ đầu: `ProductService` → `productService`. Thymeleaf gọi bean bằng chính tên này: `${@productService.imageUrl(p)}` ([bài 15](../study/15-anh-va-mo-ta-san-pham.md)).

### `@Configuration` + `@Bean`: tạo bean từ class không phải của mình

`BCryptPasswordEncoder` là class của Spring Security, bạn không sửa được nó để gắn `@Component`. Cách làm là viết một **hàm** trả về object, đánh dấu `@Bean`, đặt trong class `@Configuration`. Spring gọi hàm đó **một lần** và giữ kết quả làm bean:

```java
@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();      // bean tên "passwordEncoder", kiểu PasswordEncoder
    }
}
```

Từ đó, class nào cần `PasswordEncoder` (`UserService`, `DataSeeder`) chỉ việc khai báo trong constructor. Tham số của hàm `@Bean` cũng được Spring tự truyền vào, ví dụ `securityFilterChain(HttpSecurity http)`. Xem thêm [bài 12 — `@Bean` là gì?](../study/12-dang-nhap-spring-security.md#bean-là-gì).

### Dependency injection qua constructor

Bean cần bean khác thì **xin** qua constructor, Spring tự tìm bean đúng **kiểu** rồi truyền vào:

```java
@Service
public class ProductStatsService {
    private final StringRedisTemplate redisTemplate;   // bean do auto-configuration tạo
    private final Duration recentTtl;

    public ProductStatsService(StringRedisTemplate redisTemplate, AppConfig config) {
        this.redisTemplate = redisTemplate;
        this.recentTtl = config.getDuration("app.stats.recent-ttl");
    }
}
```

- Class chỉ có **một** constructor thì không cần `@Autowired`. Có từ 2 constructor trở lên thì phải gắn `@Autowired` lên cái Spring cần dùng.
- Ngoài kia hay gặp `@Autowired` đặt thẳng trên field. Project này **chỉ dùng constructor** (quy ước trong `CLAUDE.md`): field được `final`, nhìn constructor là biết class cần gì, và test được bằng `new ProductService(...)` mà không cần Spring.
- Lombok `@RequiredArgsConstructor` sinh constructor hộ, nhưng bài 17 cố ý không dùng, xem [lombok.md](lombok.md).
- Spring tạo bean **theo thứ tự phụ thuộc**: `AppConfig` và `StringRedisTemplate` có trước, rồi mới tới `ProductStatsService`, rồi `ProductService`, rồi `ProductViewController`. Hai bean xin lẫn nhau (A cần B, B cần A) thì app không khởi động được (xem Bẫy).

### `@SpringBootApplication` và component scan

```java
@SpringBootApplication          // = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

- `@ComponentScan`: quét package chứa `DemoApplication` (`com.example.demo`) **và mọi package con** để tìm class có `@Component`, `@Service`... Class đặt ngoài cây này (ví dụ `com.example.other`) sẽ **không** thành bean.
- `@EnableAutoConfiguration`: bật auto-configuration (mục dưới).
- `@SpringBootConfiguration`: một dạng `@Configuration`, nên `DemoApplication` cũng được phép có hàm `@Bean`.

### Starter

Starter là một gói thư viện **không có code**, chỉ gom sẵn các thư viện hay đi cùng nhau. Ví dụ `spring-boot-starter-web` kéo theo Spring MVC, Tomcat nhúng, Jackson (JSON). Project có các starter: `web`, `data-jpa`, `validation`, `actuator`, `thymeleaf`, `security`, `data-mongodb`, `data-redis`, `test`. Cách khai báo và chuyện không phải ghi số phiên bản: xem [gradle.md](gradle.md).

### Auto-configuration

Bên trong Spring Boot có hàng trăm class cấu hình sẵn. Mỗi class kèm **điều kiện** và chỉ chạy khi điều kiện đúng:

| Điều kiện | Nghĩa | Ví dụ trong project |
|---|---|---|
| `@ConditionalOnClass` | Có thư viện X trong classpath | Có Lettuce (từ `starter-data-redis`) → tạo `StringRedisTemplate`, đọc `spring.data.redis.*` |
| `@ConditionalOnMissingBean` | **Bạn chưa tự tạo** bean loại này | Bạn có `UserService implements UserDetailsService` → Spring Boot **không** tạo user mặc định với mật khẩu in ra log nữa |
| `@ConditionalOnProperty` | Có (hoặc không có) một thuộc tính cấu hình | Các `management.*`, `spring.jpa.*` bật/tắt từng phần |

Đây là lý do thêm một dòng starter vào `build.gradle` là có ngay `MongoTemplate`, `StringRedisTemplate`, `DataSource` để inject ([bài 14](../study/14-moi-truong-mongodb-redis.md)). Và cũng là lý do **bean bạn tự viết luôn thắng**: bạn khai báo `SecurityFilterChain` trong `SecurityConfig` thì cấu hình bảo mật mặc định của Spring Boot tự lùi lại.

Muốn biết auto-configuration nào đã chạy và vì sao: chạy app với `debug: true` trong `application.yml` (hoặc tham số `--debug`), log sẽ in **CONDITIONS EVALUATION REPORT**, liệt kê từng class cấu hình là *matched* hay *did not match* kèm lý do.

### Vòng đời khởi động

Khi gọi `SpringApplication.run(...)`, mọi thứ diễn ra theo thứ tự sau. Các dòng `[Config]`, `[Upload]`, `[Seed]`... trong log của project xuất hiện đúng theo thứ tự này:

```
SpringApplication.run(DemoApplication.class, args)
 │
 ├─ 1. Dựng Environment ("kho" cấu hình)
 │      biến môi trường, tham số dòng lệnh, application.yml
 │      └─ gọi các EnvironmentPostProcessor (khai trong META-INF/spring.factories)
 │           └─ ModuleConfigLoader nạp config/*.properties   → log "[Config] đã nạp ..."
 │
 ├─ 2. In banner, tạo ApplicationContext
 ├─ 3. Component scan + auto-configuration → danh sách bean cần tạo
 ├─ 4. Tạo từng bean (chạy constructor)
 │      AppConfig → FileStorageService ("[Upload] thư mục lưu file") → ProductStatsService ("[Stats] ...")
 │      → ProductService → các controller ...
 ├─ 5. Khởi động Tomcat nhúng                                  → "Tomcat started on port 8080"
 ├─ 6. "Started DemoApplication in x.xxx seconds"
 └─ 7. Gọi mọi bean CommandLineRunner / ApplicationRunner
        DataSeeder ("[Seed] ..."), NoSqlConnectionCheck ("[NoSQL] ...")
```

Lỗi ở bước 1–5 (thiếu key cấu hình, thiếu bean, không kết nối được MySQL, trùng cổng) làm app **dừng hẳn**, log in khung `APPLICATION FAILED TO START`. Lỗi ném ra từ `run()` ở bước 7 cũng làm app dừng. Đó là lý do `NoSqlConnectionCheck` tự `try/catch`: Mongo/Redis chết thì chỉ ghi log, app vẫn chạy ([bài 14](../study/14-moi-truong-mongodb-redis.md)).

### `CommandLineRunner`: chạy việc ngay sau khi khởi động

Một bean `implements CommandLineRunner` sẽ được Spring Boot gọi hàm `run(String... args)` **đúng một lần**, sau khi mọi bean đã sẵn sàng và Tomcat đã chạy. Project dùng nó cho seeder và cho việc thử kết nối NoSQL.

```java
@Component
public class DataSeeder implements CommandLineRunner {
    @Override
    public void run(String... args) {
        seedAdmin();          // có rồi thì bỏ qua, vì run() chạy lại ở MỖI lần khởi động
        seedSampleData();
    }
}
```

- `ApplicationRunner` là anh em của nó, chỉ khác kiểu tham số (`ApplicationArguments` thay vì `String...`).
- Có nhiều runner thì thứ tự giữa chúng **không được hứa trước**. Cần chắc chắn thứ tự thì gắn `@Order(1)`, `@Order(2)`... Hiện `DataSeeder` và `NoSqlConnectionCheck` không phụ thuộc nhau nên không cần.
- Ở chế độ dev, mỗi lần DevTools restart app thì runner **chạy lại**. Vì vậy seeder phải "có rồi thì bỏ qua", xem [bài 13](../study/13-du-lieu-mau-seeder.md) và [seed/du-lieu-mau.md](../seed/du-lieu-mau.md).

### `EnvironmentPostProcessor`: móc vào trước khi có bean

`CommandLineRunner` chạy **muộn nhất**. Ngược lại, `EnvironmentPostProcessor` chạy **sớm nhất**: lúc cấu hình vừa đọc xong và chưa có bean nào. Đây là chỗ duy nhất có thể thêm nguồn cấu hình mà mọi bean, kể cả bean auto-configuration, đều thấy.

Vì chạy trước khi có bean, nó **không** dùng được `@Component` hay constructor injection. Spring Boot tìm nó qua file `META-INF/spring.factories`:

```properties
# src/main/resources/META-INF/spring.factories
org.springframework.boot.env.EnvironmentPostProcessor=com.example.demo.common.ModuleConfigLoader
```

Constructor của nó chỉ nhận được vài thứ Spring Boot cho phép, như `DeferredLogFactory` (logger "hoãn", vì hệ thống log chưa sẵn sàng). Chi tiết từng dòng: [bài 16 Bước 2](../study/16-luot-xem-top-san-pham-redis.md#bước-2--mỗi-module-một-file-properties-đọc-qua-package-common).

### Cấu hình: `Environment` và thứ tự ưu tiên

Mọi giá trị cấu hình đều được gom vào **`Environment`**: một danh sách các **nguồn** (property source) xếp theo độ ưu tiên. Khi đọc một key, Spring hỏi từ nguồn cao nhất xuống, thấy ở đâu trước thì lấy ở đó. Với project này, thứ tự thực tế là (cao → thấp):

| # | Nguồn | Ví dụ |
|---|---|---|
| 1 | Tham số dòng lệnh | `java -jar app.jar --server.port=9090` |
| 2 | Java system properties | `-Dserver.port=9090` |
| 3 | Biến môi trường | `DB_HOST=mysql` (docker-compose truyền vào), `APP_STATS_TOPLIMIT=10` |
| 4 | `application.yml` | Cấu hình của Spring: DB, JPA, Redis, log, actuator |
| 5 | `config/*.properties` | Cấu hình riêng từng module, do `ModuleConfigLoader` nạp bằng `addLast` |

(Danh sách đầy đủ 15 mức, kể cả các mức dành cho test: [Externalized Configuration](https://docs.spring.io/spring-boot/3.4/reference/features/external-config.html).)

Có **hai cách** để biến môi trường tác động tới cấu hình, project dùng cả hai:

- **Placeholder** trong file: `url: jdbc:mysql://${DB_HOST:localhost}:...` nghĩa là "lấy biến `DB_HOST`, không có thì dùng `localhost`". Tên biến do mình tự đặt.
- **Ghi đè thẳng một key** bằng biến môi trường cùng tên, theo luật: dấu `.` → `_`, bỏ dấu `-`, viết hoa. `app.stats.top-limit` → `APP_STATS_TOPLIMIT`, `spring.datasource.url` → `SPRING_DATASOURCE_URL`. Spring tự khớp, gọi là *relaxed binding*.

**Ba cách đọc cấu hình trong code:**

| Cách | Ví dụ | Project |
|---|---|---|
| `@Value("${key}")` trên tham số constructor | `@Value("${app.upload.dir}") String dir` | Từng dùng ở bài 15, bỏ từ bài 16 |
| `@ConfigurationProperties(prefix = "app.stats")` | Class `StatsProperties` có getter/setter | Không dùng (người học đã chọn bỏ) |
| `Environment` (bọc trong `AppConfig`) | `config.getInt("app.stats.top-limit")` | **Đang dùng**, gọi trong constructor, lưu vào field `final` |

Cả ba cùng đọc từ `Environment` nên kết quả như nhau, kể cả việc đổi `"30d"` thành `Duration`. So sánh chi tiết và lý do chọn `AppConfig`: các mục [Đưa hằng số sang `application.yml`](#đưa-hằng-số-ví-dụ-recent_ttl-30-ngày-sang-applicationyml-được-không-value-có-đọc-được-duration-không), [`@ConfigurationProperties`](#mỗi-module-một-class-cấu-hình-riêng-statsproperties-productproperties-được-không-configurationproperties-là-gì) và [file `.ini` của PHP](#java-có-file-cấu-hình-kiểu-ini-của-php-key--value-không-có-cần-tự-viết-hàm-đọc-chung-không) trong phần Hỏi đáp bên dưới.

> Ngoài kia hay gặp **profile** (`application-dev.yml`, `spring.profiles.active=dev`) để tách cấu hình theo môi trường. Project này chưa dùng: khác biệt giữa dev và Docker được truyền bằng biến môi trường trong 2 file compose.

### DevTools: tự khởi động lại khi sửa code

`spring-boot-devtools` (khai báo `developmentOnly` trong `build.gradle`) làm 2 việc chính:

1. **Restart nhanh.** DevTools chia classpath làm 2 phần: thư viện (`.jar`, ít đổi) nạp bằng một *classloader* cố định, còn code của bạn (`build/classes`, `build/resources`) nạp bằng một classloader riêng. Thấy file trong phần thứ hai đổi, nó chỉ vứt classloader đó đi và dựng lại, nên nhanh hơn nhiều so với tắt hẳn rồi chạy lại. Ở chế độ dev, `gradle classes --continuous` là thứ biên dịch lại và copy file vào `build/`, còn DevTools là thứ thấy file mới và restart. Xem [bài 04](../study/04-hot-reload.md#cơ-chế-hoạt-động-bên-trong) và [gradle.md](gradle.md).
2. **Đổi vài mặc định cho tiện lúc dev**, ví dụ tắt cache của Thymeleaf, bật `server.error.include-message`. Vì thế [bài 10](../study/10-trang-404.md#bước-4--cho-phép-hiện-message-lỗi) ghi rõ `include-message: always` trong `application.yml` để chế độ thường (không có DevTools) cũng hiện message.

Hai điều hay thắc mắc:

- Sửa file trong `templates/` hoặc `static/` thì **không** restart: DevTools mặc định bỏ qua các thư mục này, và vì cache Thymeleaf đã tắt nên tải lại trang là thấy ngay. Sửa `.java`, `application.yml`, `config/*.properties` thì restart.
- Sửa `build.gradle` thì DevTools **không** giúp được: thư viện mới phải được tải và đưa vào classpath từ đầu, nên phải restart container `app-dev`.

DevTools tự tắt khi app chạy từ file `.jar` đóng gói (chế độ `docker compose up --build`). Thực ra nó còn không có mặt trong `.jar`, vì `developmentOnly` bị loại khỏi `bootJar`.

### Actuator: app tự báo tình trạng

`spring-boot-starter-actuator` thêm các đường dẫn `/actuator/...` để theo dõi app. Project mở 2 endpoint qua web:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always
```

`/actuator/health` gom kết quả của nhiều **health indicator** mà auto-configuration tự đăng ký theo thư viện có mặt: `db` (MySQL), `mongo`, `redis`, `diskSpace`, `ping`... Chỉ cần **một** cái `DOWN` thì tổng là `DOWN` và HTTP trả **503**. Endpoint này được mở cho người chưa đăng nhập trong `SecurityConfig` (`/actuator/health`), còn `/actuator/info` thì phải đăng nhập.

Hai tầng `endpoints` / `endpoint` khác nhau thế nào: xem [Hỏi đáp](#managementendpoints-và-managementendpoint-cókhông-có-chữ-s-khác-nhau-thế-nào). Có nên `show-details: always`: xem [bài 14](../study/14-moi-truong-mongodb-redis.md#show-details-always-có-an-toàn-không).

## Project này dùng nó thế nào

| File | Khái niệm Spring Boot | Bài học / doc module |
|---|---|---|
| `DemoApplication.java` | `@SpringBootApplication`, `SpringApplication.run` | [01](../study/01-tong-quan.md) |
| `product/`, `category/`, `user/` | `@Service`, `@Controller`, constructor injection | [02](../study/02-thuc-hanh-crud.md), [09](../study/09-kien-thuc-controller.md) · [product](../product/quan-ly-san-pham.md), [category](../category/quan-ly-danh-muc.md), [user](../user/dang-nhap-dang-ky.md) |
| `config/SecurityConfig.java` | `@Configuration` + `@Bean` | [12](../study/12-dang-nhap-spring-security.md), [spring-security.md](spring-security.md) |
| `config/WebConfig.java` | `@Configuration` + `WebMvcConfigurer` | [15](../study/15-anh-va-mo-ta-san-pham.md), [spring-web-mvc.md](spring-web-mvc.md) |
| `seed/DataSeeder.java`, `nosql/NoSqlConnectionCheck.java` | `@Component` + `CommandLineRunner` | [13](../study/13-du-lieu-mau-seeder.md), [14](../study/14-moi-truong-mongodb-redis.md) · [seed](../seed/du-lieu-mau.md), [nosql](../nosql/kiem-tra-ket-noi-mongo-redis.md) |
| `common/ModuleConfigLoader.java` + `META-INF/spring.factories` | `EnvironmentPostProcessor` | [16 Bước 2](../study/16-luot-xem-top-san-pham-redis.md#bước-2--mỗi-module-một-file-properties-đọc-qua-package-common) · [common/cau-hinh-module.md](../common/cau-hinh-module.md) |
| `common/AppConfig.java` | Bean bọc `Environment` | như trên |
| `application.yml` | Cấu hình của Spring, placeholder `${...:...}`, Actuator | [04](../study/04-hot-reload.md), [10](../study/10-trang-404.md), [14](../study/14-moi-truong-mongodb-redis.md), [16 Bước 1](../study/16-luot-xem-top-san-pham-redis.md#bước-1--applicationyml-timeout-cho-redis) |
| `config/stats.properties`, `product.properties`, `upload.properties` | Nguồn cấu hình ưu tiên thấp nhất | [16 Bước 2](../study/16-luot-xem-top-san-pham-redis.md#bước-2--mỗi-module-một-file-properties-đọc-qua-package-common) |

Một ví dụ đi trọn từ cấu hình tới bean, lấy từ `storage/FileStorageService.java`:

```java
@Service
public class FileStorageService {
    private final Path root;

    public FileStorageService(AppConfig config) {           // ① Spring truyền bean AppConfig vào
        this.root = Paths.get(config.getString("app.upload.dir"))   // ② đọc key trong config/upload.properties
                         .toAbsolutePath().normalize();      //    giá trị ${UPLOAD_DIR:uploads} → "uploads"
        // ... tạo thư mục, ghi log "[Upload] thư mục lưu file: /app/uploads"
    }
}
```

```properties
# config/upload.properties
app.upload.dir=${UPLOAD_DIR:uploads}
```

Thiếu key `app.upload.dir` thì `getRequiredProperty` ném lỗi ngay ở bước ②, tức là lúc **tạo bean**, nên app không khởi động được và log ghi rõ `Required key 'app.upload.dir' not found`. Đây là điều project muốn: cấu hình sai thì biết ngay, không đợi tới lúc có người upload ảnh.

## Tra cứu nhanh

| Annotation / Hàm | Của ai | Làm gì | Ghi chú |
|---|---|---|---|
| `@SpringBootApplication` | Spring Boot | Bật component scan + auto-configuration | Đặt ở package gốc `com.example.demo` |
| `SpringApplication.run(Class, args)` | Spring Boot | Khởi động toàn bộ app | Trả về `ConfigurableApplicationContext` (project bỏ qua) |
| `@Component` | Spring Framework | Đánh dấu class là bean | |
| `@Service` / `@Repository` / `@Controller` | Spring Framework | Như `@Component`, kèm ý nghĩa tầng | `@RestController` = `@Controller` + `@ResponseBody` |
| `@Configuration` | Spring Framework | Class cấu hình, chứa hàm `@Bean` | |
| `@Bean` | Spring Framework | Giá trị trả về của hàm thành bean | Tên bean = tên hàm |
| `@Autowired` | Spring Framework | Chỉ định constructor/field cần inject | Không cần khi chỉ có 1 constructor. Project không dùng |
| `@Order(n)` | Spring Framework | Thứ tự giữa các bean cùng loại (runner...) | Số nhỏ chạy trước |
| `CommandLineRunner.run(String... args)` | Spring Boot | Chạy 1 lần sau khi khởi động xong | `void`, khai báo `throws Exception` |
| `EnvironmentPostProcessor.postProcessEnvironment(env, app)` | Spring Boot | Sửa `Environment` trước khi tạo bean | Đăng ký trong `META-INF/spring.factories` |
| `env.getRequiredProperty(key, Type.class)` | Spring Framework (`Environment`) | Đọc key và đổi kiểu, thiếu thì ném lỗi | `IllegalStateException` |
| `env.getProperty(key, Type.class, macDinh)` | Spring Framework (`Environment`) | Đọc key, thiếu thì trả giá trị mặc định | Project không dùng, vì muốn thiếu key là báo lỗi |
| `${TEN_BIEN:mac_dinh}` | Spring (placeholder) | Lấy biến/key, không có thì dùng mặc định | Dùng được trong `.yml` và `.properties` |
| `debug: true` | Spring Boot | In báo cáo auto-configuration lúc khởi động | Chỉ bật khi cần điều tra |
| `management.endpoints.web.exposure.include` | Actuator | Endpoint nào được mở qua web | Mặc định chỉ `health` |

## Bẫy thường gặp

| Triệu chứng | Nguyên nhân | Cách sửa |
|---|---|---|
| `Parameter 0 of constructor in ... required a bean of type '...' that could not be found` | Class được xin chưa phải bean: quên `@Service`/`@Component`, hoặc nằm ngoài package `com.example.demo` | Gắn annotation; đặt class trong cây `com.example.demo.*` |
| Như trên, nhưng là bean của thư viện (`MongoTemplate`, `StringRedisTemplate`) | Vừa thêm starter mà chưa restart container, nên auto-configuration chưa thấy thư viện | `docker compose -f docker-compose.dev.yml restart app-dev` |
| `The dependencies of some of the beans in the application context form a cycle` | A xin B qua constructor, B lại xin A. Từ Spring Boot 2.6, vòng tròn bị cấm mặc định | Tách phần dùng chung ra class thứ ba, hoặc bỏ bớt một chiều |
| `No qualifying bean of type '...' available: expected single matching bean but found 2` | Có 2 bean cùng kiểu, Spring không biết chọn cái nào | Bỏ bớt một bean, hoặc dùng `@Primary` / `@Qualifier` |
| `Required key 'app.xxx' not found` lúc khởi động | Gõ sai key trong code hoặc trong `config/*.properties`; hoặc file không được nạp (không thấy dòng `[Config] đã nạp`) | So tên key 2 bên; kiểm tra `META-INF/spring.factories` |
| Sửa `config/upload.properties` mà giá trị không đổi | Key đó còn nằm trong `application.yml` hoặc biến môi trường, mà 2 nguồn này ưu tiên cao hơn | Xoá key ở nguồn cao hơn (xem bảng thứ tự ưu tiên) |
| Đặt `management.endpoints.health.show-details` (thừa `s`) mà không có tác dụng, cũng không báo lỗi | Spring Boot lặng lẽ bỏ qua key không biết | Xem [Hỏi đáp](#managementendpoints-và-managementendpoint-cókhông-có-chữ-s-khác-nhau-thế-nào) |
| Làm theo bài trên mạng thì gặp `javax.persistence...` không tồn tại, `spring.redis.*` không có tác dụng, `WebSecurityConfigurerAdapter` không có | Bài viết cho Spring Boot 2. Từ bản 3: `javax.*` → `jakarta.*`, `spring.redis` → `spring.data.redis`, Security viết kiểu `SecurityFilterChain` | Luôn kiểm tra bài viết dùng Spring Boot 3.x |
| `Web server failed to start. Port 8080 was already in use.` | Có tiến trình khác đang giữ cổng (thường là chạy `./gradlew bootRun` trên host trong khi đã có app khác) | Tắt tiến trình kia, hoặc chạy với `--server.port=8081` |
| Log seeder chạy lại sau mỗi lần lưu file | DevTools restart app, runner chạy lại | Bình thường. Seeder phải "có rồi thì bỏ qua" |
| `/actuator/health` trả 503 dù trang web vẫn chạy | Một thành phần (thường là Redis/Mongo) `DOWN`, kéo tổng thành `DOWN` | Xem chi tiết trong JSON trả về, bật lại dịch vụ đó |
| Tạo file `src/main/resources/config/application.properties` thì cấu hình "lạ" | `classpath:/config/` là chỗ Spring Boot **tự** đọc `application.*`, ưu tiên cao hơn `application.yml` ở gốc. `ModuleConfigLoader` cũng nạp nó thêm lần nữa | Đặt tên file theo module (`review.properties`), đừng đặt `application.*` trong `config/` |

## Đọc thêm

- Tài liệu chính thức: [Spring Boot 3.4 Reference](https://docs.spring.io/spring-boot/3.4/index.html), đặc biệt các mục [SpringApplication](https://docs.spring.io/spring-boot/3.4/reference/features/spring-application.html), [Externalized Configuration](https://docs.spring.io/spring-boot/3.4/reference/features/external-config.html), [Auto-configuration](https://docs.spring.io/spring-boot/3.4/reference/using/auto-configuration.html), [Developer Tools](https://docs.spring.io/spring-boot/3.4/reference/using/devtools.html), [Actuator Endpoints](https://docs.spring.io/spring-boot/3.4/reference/actuator/endpoints.html).
- IoC container và bean (Spring Framework): [The IoC Container](https://docs.spring.io/spring-framework/reference/6.2/core/beans.html).
- Bài học: [01 Tổng quan](../study/01-tong-quan.md) · [04 Hot reload](../study/04-hot-reload.md) · [09 Controller (constructor injection)](../study/09-kien-thuc-controller.md) · [13 Seeder (`CommandLineRunner`)](../study/13-du-lieu-mau-seeder.md) · [16 Bước 2 (cấu hình theo module)](../study/16-luot-xem-top-san-pham-redis.md#bước-2--mỗi-module-một-file-properties-đọc-qua-package-common).
- Tài liệu liên quan: [spring-web-mvc.md](spring-web-mvc.md) · [gradle.md](gradle.md) · [logging.md](logging.md) · [common/cau-hinh-module.md](../common/cau-hinh-module.md).

## Hỏi đáp

Các câu hỏi phát sinh trong lúc học, chuyển từ sổ tay chung sang đây.

### `management.endpoints` và `management.endpoint` (có/không có chữ `s`) khác nhau thế nào?

> Hỏi ở: [Bài 14 — Bước 4: Cấu hình kết nối trong `application.yml`](../study/14-moi-truong-mongodb-redis.md#bước-4--cấu-hình-kết-nối-trong-applicationyml)

Cả hai đều là cấu hình của **Spring Boot Actuator**. Actuator là thư viện sinh sẵn các đường dẫn `/actuator/...` để theo dõi app. Mỗi đường dẫn như vậy gọi là một **endpoint**: `health`, `info`, `metrics`, `env`, `beans`...

| | `management.endpoints` (số nhiều) | `management.endpoint.<tên>` (số ít) |
|---|---|---|
| Phạm vi | **Tất cả** endpoint, nhìn chung | **Một** endpoint cụ thể, có tên ngay sau |
| Trả lời câu hỏi | Endpoint **nào** được mở ra ngoài? Mở qua đâu, ở đường dẫn nào? | Endpoint **này** hoạt động **ra sao**? |
| Ví dụ | `endpoints.web.exposure.include: health,info`<br>`endpoints.web.base-path: /manage` (đổi `/actuator` thành `/manage`) | `endpoint.health.show-details: always`<br>`endpoint.health.show-components: always` |

Có thể hình dung app là một toà nhà:
- `endpoints` là **bảo vệ ở cổng**: quyết định phòng nào khách được vào. Mặc định qua web chỉ mở mỗi `health`, nên phải `include: health,info` thì mới vào được `/actuator/info`.
- `endpoint.health` là **cách bày trí bên trong phòng `health`**: khách vào thì thấy gì. Mặc định chỉ thấy `{"status":"UP"}`, còn `show-details: always` thì bày ra chi tiết từng thành phần `db`, `mongo`, `redis`.

Hai tầng này độc lập với nhau. Nếu chỉ đặt `show-details` mà `health` không được expose thì vẫn không vào được. Ngược lại, nếu đã expose mà không đặt `show-details` thì chỉ thấy `UP`.

```yaml
management:
  endpoints:            # ← chung cho mọi endpoint
    web:
      exposure:
        include: health,info
  endpoint:             # ← riêng từng endpoint
    health:             #    tên endpoint
      show-details: always
```

#### Bẫy hay gặp: đặt nhầm chỗ sẽ không báo lỗi

Nếu viết nhầm `management.endpoints.health.show-details`, tức là thêm chữ `s`, thì app **vẫn chạy bình thường**. Spring Boot bỏ qua thuộc tính nó không biết, không báo gì cả. Kết quả là `/actuator/health` vẫn chỉ trả `{"status":"UP"}`. Trong VSCode, nếu đã cài extension *Spring Boot Tools*, dòng sai sẽ bị gạch vàng kèm cảnh báo "Unknown property". Đó là cách phát hiện nhanh nhất.

---

### Đưa hằng số (ví dụ `RECENT_TTL` 30 ngày) sang `application.yml` được không? `@Value` có đọc được `Duration` không?

> Hỏi ở: [Bài 16 — Bước 3: `ProductStatsService`](../study/16-luot-xem-top-san-pham-redis.md#bước-3--productstatsservice-mọi-thao-tác-với-redis), dòng `private static final Duration RECENT_TTL = Duration.ofDays(30);`

#### Được. Và `@Value` đọc thẳng ra `Duration` luôn

Làm giống hệt cách bài 15 đọc `app.upload.dir` (xem [Bài 15 — Bước 1](../study/15-anh-va-mo-ta-san-pham.md#bước-1--cấu-hình-trong-applicationyml)), chỉ khác là kiểu tham số là `Duration` thay vì `String`.

**1. `application.yml`**: thêm vào khối `app:` đang có:

```yaml
app:
  # ... (upload, product giữ nguyên)
  stats:
    # "Bạn vừa xem": bao lâu không xem gì thì danh sách tự biến mất
    recent-ttl: 30d
```

**2. `ProductStatsService`**: bỏ hằng số, nhận giá trị qua constructor:

```java
// Bỏ dòng này:
// private static final Duration RECENT_TTL = Duration.ofDays(30);

private final StringRedisTemplate redisTemplate;
private final Duration recentTtl;   // đọc từ app.stats.recent-ttl

public ProductStatsService(StringRedisTemplate redisTemplate,
                           @Value("${app.stats.recent-ttl}") Duration recentTtl) {
    this.redisTemplate = redisTemplate;
    this.recentTtl = recentTtl;
}
```

Trong `recordView()` đổi `RECENT_TTL` thành `recentTtl`. Nhớ thêm `import org.springframework.beans.factory.annotation.Value;`.

Spring Boot tự đổi chuỗi `"30d"` thành `Duration.ofDays(30)`. Đây cũng chính là cơ chế đang đọc `timeout: 2s` của Redis ở Bước 1 bài 16.

> Bài 16 không dùng `@Value` cho chỗ này. Bài đưa các giá trị vào file `config/stats.properties`, và đọc qua class dùng chung `common/AppConfig` (`config.getDuration("app.stats.recent-ttl")`), xem [Bước 2](../study/16-luot-xem-top-san-pham-redis.md#bước-2--mỗi-module-một-file-properties-đọc-qua-package-common). `AppConfig` đọc từ `Environment` của Spring, tức là cùng cơ chế với `@Value`, nên chuỗi `30d` vẫn được đổi thành `Duration` y như trên.

#### Vì sao phải bỏ `static final`?

- `@Value` **không gán được vào biến `static`**. Spring tiêm giá trị vào **từng object** (bean), còn biến `static` thuộc về class, nên Spring không đụng tới.
- Vì không còn là hằng số, tên biến đổi từ kiểu `HOA_GẠCH_DƯỚI` (quy ước cho hằng số) sang `camelCase` (quy ước cho field thường): `RECENT_TTL` → `recentTtl`.
- Vẫn giữ `final`: giá trị gán một lần trong constructor rồi không đổi nữa, giống `redisTemplate`.

#### Cách viết thời gian trong `application.yml`

| Viết | Nghĩa |
|---|---|
| `500ms` | 500 mili giây |
| `2s` | 2 giây |
| `10m` | 10 phút |
| `12h` | 12 giờ |
| `30d` | 30 ngày |
| `P30D` | 30 ngày, viết theo chuẩn ISO-8601 (ít dùng, khó đọc hơn) |

> ⚠️ **Bẫy:** viết số trơn `30`, không có đơn vị, thì Spring hiểu là **30 mili giây**, không phải 30 ngày. Key sẽ hết hạn gần như ngay lập tức và "Bạn vừa xem" luôn trống. Luôn ghi kèm đơn vị.

#### Nên đưa cái gì ra cấu hình, cái gì để là hằng số?

| Đưa ra `application.yml` | Để là hằng số trong code |
|---|---|
| Con số **có thể muốn chỉnh** mà không sửa code: thời hạn (`recent-ttl`), số lượng (`RECENT_LIMIT` = 5 cũng đưa ra được, kiểu `int`), đường dẫn thư mục... | Thứ gắn chặt với **dữ liệu đã lưu**: tên key Redis `demo:product:views`, `demo:recent:`. Đổi tên key là dữ liệu cũ bị "mồ côi", code không tìm thấy nữa |
| Giá trị **khác nhau giữa các môi trường** (dev muốn TTL `5m` để thử cho nhanh, chạy thật thì `30d`) | Thứ thuộc về logic, đổi là phải sửa code đi kèm |

Muốn đổi theo môi trường mà không sửa file, dùng lại cú pháp biến môi trường quen thuộc: `recent-ttl: ${RECENT_TTL:30d}`.

#### Lưu ý khi đổi giá trị

Đổi `recent-ttl` rồi restart app thì **chỉ lần xem tiếp theo** mới dùng hạn mới, vì `expire` được gọi lại mỗi lần xem. Các key đã có trong Redis vẫn giữ hạn cũ cho tới khi user xem thêm sản phẩm. Kiểm tra bằng `TTL demo:recent:admin` trong `redis-cli`.

---

### Mỗi module một class cấu hình riêng (`StatsProperties`, `ProductProperties`) được không? `@ConfigurationProperties` là gì?

> Hỏi ở: [Bài 16 — Bước 3: `ProductStatsService`](../study/16-luot-xem-top-san-pham-redis.md#bước-3--productstatsservice-mọi-thao-tác-với-redis), câu hỏi nối tiếp mục [Đưa hằng số sang `application.yml`](#đưa-hằng-số-ví-dụ-recent_ttl-30-ngày-sang-applicationyml-được-không-value-có-đọc-được-duration-không). Ý tưởng ban đầu: `product/product_config.java`, `stats/stat_config.java`. Sau khi bàn tiếp, bài 16 **không** dùng cách này. Bài chọn mỗi module một file `.properties` và một hàm đọc dùng chung `common/AppConfig`, xem [Bước 2](../study/16-luot-xem-top-san-pham-redis.md#bước-2--mỗi-module-một-file-properties-đọc-qua-package-common) và mục [file `.ini` của PHP](#java-có-file-cấu-hình-kiểu-ini-của-php-key--value-không-có-cần-tự-viết-hàm-đọc-chung-không) bên dưới. Mục này giữ lại để biết thêm một cách làm phổ biến của Spring Boot.

#### Được, và đây là cách Spring Boot khuyên dùng khi một module có nhiều cấu hình

Dùng `@ConfigurationProperties`: một class Java gom **cả nhóm** cấu hình cùng tiền tố (`app.stats.*`) thành các field. Thay vì rải `@Value("${...}")` khắp nơi, service chỉ cần nhận một object.

#### Đặt tên: `StatsProperties`, không phải `stat_config.java`

- Tên class Java viết **PascalCase** và tên file phải trùng tên class. `stat_config` (snake_case) chạy được nhưng sai quy ước.
- Trong Spring, hậu tố **`Config`** thường dành cho class `@Configuration` (class *tạo bean*, như `SecurityConfig`, `WebConfig` trong `config/`). Class *chứa giá trị cấu hình* thì quy ước đặt hậu tố **`Properties`**. Nên đặt `StatsProperties`, `ProductProperties` để khỏi nhầm hai loại.
- Đặt class trong **chính package của module** (`stats/StatsProperties.java`) là hợp với kiểu tổ chức theo feature của project.

#### Ví dụ: `stats/StatsProperties.java`

```yaml
# application.yml
app:
  stats:
    recent-ttl: 30d
    recent-limit: 5
```

```java
package com.example.demo.stats;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Gom mọi cấu hình app.stats.* vào 1 object.
// recent-ttl trong yml → field recentTtl (Spring tự đổi gạch ngang sang camelCase)
@ConfigurationProperties(prefix = "app.stats")
public class StatsProperties {

    private Duration recentTtl = Duration.ofDays(30);   // giá trị mặc định nếu yml không khai báo
    private int recentLimit = 5;

    public Duration getRecentTtl() { return recentTtl; }
    public void setRecentTtl(Duration recentTtl) { this.recentTtl = recentTtl; }

    public int getRecentLimit() { return recentLimit; }
    public void setRecentLimit(int recentLimit) { this.recentLimit = recentLimit; }
}
```

Spring tạo object rồi gọi **setter** để gán giá trị đọc từ yml, nên bắt buộc phải có setter.

**Bật lên:** thêm **một lần** `@ConfigurationPropertiesScan` vào `DemoApplication`. Spring sẽ tự tìm mọi class `@ConfigurationProperties` trong project:

```java
@SpringBootApplication
@ConfigurationPropertiesScan
public class DemoApplication { ... }
```

**Dùng trong service:** nhận qua constructor như mọi bean khác:

```java
public ProductStatsService(StringRedisTemplate redisTemplate, StatsProperties props) {
    this.redisTemplate = redisTemplate;
    this.props = props;
}

// ...
redisTemplate.opsForList().trim(recentKey, 0, props.getRecentLimit() - 1);
redisTemplate.expire(recentKey, props.getRecentTtl());
```

Áp dụng tương tự cho các module khác: `product/ProductProperties` (`app.product.default-image`), `storage/StorageProperties` (`app.upload.dir`).

#### `@Value` hay `@ConfigurationProperties`?

| | `@Value("${app.stats.recent-ttl}")` | `@ConfigurationProperties(prefix = "app.stats")` |
|---|---|---|
| Hợp khi | Chỉ 1–2 giá trị lẻ | Một nhóm nhiều giá trị cùng tiền tố |
| Viết thêm | Không cần class mới | Thêm 1 class + getter/setter |
| Giá trị mặc định | `${app.x:30d}` (sau dấu `:`) | Gán ngay ở field |
| Gõ sai tên key | Lỗi lúc khởi động (`Could not resolve placeholder`) | **Không báo lỗi**, field giữ giá trị mặc định ⚠️ |
| Tên trong yml | Nên gõ đúng chuỗi như trong code | Linh hoạt: `recent-ttl` hay `recentTtl` trong yml đều được, biến môi trường `APP_STATS_RECENTTTL` cũng tự khớp |

Project hiện tại mỗi module chỉ có 1 cấu hình nên `@Value` là đủ. Khi `stats` có từ 2 giá trị trở lên (TTL + giới hạn) thì chuyển sang `StatsProperties` sẽ gọn hơn.

> Mẹo: thêm `annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'` vào `build.gradle` thì VSCode sẽ **hết gạch vàng "Unknown property"** ở các dòng `app.*`, và còn gợi ý tên key khi gõ. Sau khi thêm phải build lại (`./gradlew build`) thì VSCode mới nhận.

#### Còn muốn tách cả file `.yml` theo module?

Cũng được: tạo `src/main/resources/stats.yml`, rồi trong `application.yml` thêm `spring.config.import: classpath:stats.yml`. (Bài 16 tách theo một cách khác: file `.properties` trong `config/`, tự nạp bằng `ModuleConfigLoader`, không phải khai báo từng file.) Nhưng project nhỏ thì nên để chung một file. Tách ra nhiều file thì lúc tìm một cấu hình phải mở nhiều chỗ.

---

### Java có file cấu hình kiểu `.ini` của PHP (`key = "value"`) không? Có cần tự viết hàm đọc chung không?

> Hỏi ở: [Bài 16 — Bước 2: Mỗi module một file `.properties`](../study/16-luot-xem-top-san-pham-redis.md#bước-2--mỗi-module-một-file-properties-đọc-qua-package-common). Người hỏi từng dùng file `.ini` bên PHP và một hàm đọc cấu hình dùng chung. Câu hỏi này dẫn tới việc viết lại Bước 2 của bài 16.

#### Có: file `.properties`, và Spring Boot đọc sẵn nó

File `.properties` là "họ hàng" gần nhất của `.ini`: mỗi dòng một cặp `key=value`. Spring Boot đọc được cả `application.properties` lẫn `application.yml` (file project đang dùng). Hai kiểu này chỉ khác **cách viết**, còn nội dung thì như nhau:

```ini
; PHP: config/stats.ini
[stats]
recent_ttl = "30d"
recent_limit = 5
```

```properties
# Java: application.properties. Không có [section] → dùng tiền tố có dấu chấm
app.stats.recent-ttl=30d
app.stats.recent-limit=5
app.stats.top-limit=5
```

```yaml
# Java: application.yml (project đang dùng). Thụt lề thay cho việc lặp lại tiền tố
app:
  stats:
    recent-ttl: 30d
    recent-limit: 5
    top-limit: 5
```

Cả 2 cách viết Java ở trên đều tạo ra **cùng các key** `app.stats.recent-ttl`, `app.stats.recent-limit`... Vì vậy code Java đọc được từ file nào cũng như nhau, không phải sửa dòng Java nào.

#### "Hàm đọc cấu hình dùng chung": Spring đã làm sẵn, không cần tự viết

Bên PHP thường phải tự viết (hoặc framework cung cấp) một hàm như `parse_ini_file()` hay `config('stats.recent_ttl')`. Bên Spring Boot, phần đó đã có sẵn:

| PHP | Java / Spring Boot |
|---|---|
| `parse_ini_file('stats.ini', true)` | `java.util.Properties` + `load(...)`. Đây là Java thuần, **chỉ dùng khi không có Spring** |
| Hàm chung `config('stats.recent_ttl')` | `Environment`: `env.getProperty("app.stats.recent-limit", Integer.class, 5)`. Đọc được mọi nguồn: yml, properties, biến môi trường... |
| Mảng cấu hình của một module | Class `XxxProperties` + `@ConfigurationProperties`, xem mục [Mỗi module một class cấu hình riêng](#mỗi-module-một-class-cấu-hình-riêng-statsproperties-productproperties-được-không-configurationproperties-là-gì) |

Nếu tự viết hàm đọc chung (như `AppConfig` của bài 16, bọc quanh `Environment`), nên giữ được 3 điểm mạnh mà `XxxProperties` có sẵn:

- **Có kiểu dữ liệu.** `"30d"` phải được đổi thành `Duration`, còn `"5"` thành `int`. Giá trị sai định dạng (ví dụ `30days`) thì app phải **không khởi động** và báo lỗi ngay, chứ không đợi tới lúc có người bấm vào trang mới lỗi. `AppConfig` làm được điều này: nó dùng `env.getRequiredProperty(key, Duration.class)` và được gọi **trong constructor**.
- **Mỗi key chỉ đọc ở một chỗ.** Gọi `getProperty("app.stats.recent-ttl")` rải rác ở 5 chỗ thì có 5 chỗ có thể gõ sai. Nên đọc một lần trong constructor rồi lưu vào field `final`.
- **Đọc từ mọi nguồn**: `application.yml`, `application.properties`, biến môi trường `APP_STATS_RECENTLIMIT=10`... Code không cần biết giá trị đến từ đâu.

#### Muốn mỗi module một file riêng (giống `stats.ini`)?

Làm được: tạo `src/main/resources/config/stats.properties`, rồi khai báo trong `application.yml`:

```yaml
spring:
  config:
    import: classpath:config/stats.properties   # nhiều file thì ngăn cách bằng dấu phẩy
```

`StatsProperties` vẫn giữ nguyên, vì key vẫn là `app.stats.*`. Có điều với project nhỏ thì nên để chung một file: tìm cấu hình chỉ cần mở một chỗ.

#### ⚠️ Bẫy khi chuyển từ `.ini` sang `.properties` (đã chạy thử)

| Viết | `.ini` (PHP) hiểu là | `.properties` (Java) hiểu là |
|---|---|---|
| `key = "30d"` | `30d` (bỏ dấu nháy) | `"30d"`, **giữ nguyên cả dấu nháy**. Đọc thành `Duration` sẽ lỗi. Không viết nháy |
| `key = abc   ` (thừa dấu cách ở cuối) | `abc` | `abc   `: dấu cách ở **đầu** giá trị bị bỏ, dấu cách ở **cuối** thì giữ lại |
| `[stats]` | Tên nhóm | Không có khái niệm nhóm. Phải viết tiền tố đầy đủ `app.stats.` ở mọi dòng |
| `app.msg=Xin chào` | `Xin chào` | `Xin chÃ o`: **lỗi font tiếng Việt**. Spring Boot đọc `.properties` theo bảng mã ISO-8859-1, không phải UTF-8. Còn `.yml` thì đọc theo UTF-8 nên không bị |

Comment tiếng Việt (dòng bắt đầu bằng `#`) thì không sao, vì comment bị bỏ qua. Chỉ **giá trị** có tiếng Việt mới bị lỗi font.

#### Project này làm thế nào (bài 16)

[Bài 16 Bước 2](../study/16-luot-xem-top-san-pham-redis.md#bước-2--mỗi-module-một-file-properties-đọc-qua-package-common) làm đúng theo kiểu `.ini` + hàm đọc chung:

- **`application.yml`** chỉ giữ cấu hình **của Spring** (DB, Redis, log...).
- **Mỗi module một file** `src/main/resources/config/<module>.properties` (`stats`, `product`, `upload`), chỉ gồm các dòng `key=value`.
- **`common/ModuleConfigLoader`** (một `EnvironmentPostProcessor`) tự nạp **mọi** file `config/*.properties` lúc khởi động, và đọc theo **UTF-8**, nên tránh được lỗi font tiếng Việt ở bảng trên.
- **`common/AppConfig`** là hàm đọc dùng chung: `config.getInt("app.stats.top-limit")`, `config.getDuration(...)`. Thiếu key thì app báo lỗi lúc khởi động.
- Thêm module mới: tạo file `config/<module>.properties` + `import com.example.demo.common.AppConfig`. Không phải sửa gì khác.
- **Đừng để cả `application.properties` lẫn `application.yml` cùng lúc.** Spring đọc cả hai, và nếu trùng key thì `.properties` thắng. Khi đó sửa yml mà giá trị không đổi, rất khó tìm ra lý do. (Các file trong `config/` của bài 16 thì khác: chúng có tên riêng và được ưu tiên **thấp hơn** `application.yml`.)
