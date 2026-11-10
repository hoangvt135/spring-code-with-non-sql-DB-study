# Gradle

> **Dùng trong project từ:** [bài 03](../study/03-cheatsheet.md) (lệnh), [bài 04](../study/04-hot-reload.md) (hot reload) · **Phiên bản:** Gradle 8.11 (`gradle/wrapper/gradle-wrapper.properties` và image Docker `gradle:8.11-jdk21`), plugin Spring Boot 3.4.1, plugin `io.spring.dependency-management` 1.1.7, Java toolchain 21 · **Tài liệu chính thức:** [Gradle 8.11 User Manual](https://docs.gradle.org/8.11/userguide/userguide.html)
>
> 🔼 **Sau [bài 16.5](../study/16.5-nang-cap-spring-boot-4.md)** (người học chưa làm): Gradle **9.8.2** ở cả 3 chỗ: wrapper, image `gradle:9.8.2-jdk21` của chế độ dev và của `Dockerfile` (Spring Boot 4 cần Gradle ≥ 8.14). Bỏ số phiên bản ghi tay của layout-dialect; thêm `-Xlint:deprecation` (Bước 1, 4, 5).

## Nó là gì, giải quyết vấn đề gì

Gradle là **công cụ build** của project. Nó làm hai nhóm việc:

1. **Quản lý thư viện**, giống Composer bên PHP: đọc danh sách thư viện trong `build.gradle`, tải về từ Maven Central (kho thư viện Java, giống Packagist), kéo luôn các thư viện mà thư viện đó cần.
2. **Biên dịch, đóng gói, chạy, test**: việc mà PHP không cần. File `.php` chạy thẳng được, còn file `.java` phải được dịch thành `.class` rồi đóng thành `.jar` ([bài 04](../study/04-hot-reload.md#vì-sao-java-cần-build-còn-php-thì-không)). Gradle gọi `javac`, copy tài nguyên, đóng gói, chạy app, chạy test.

Đối chiếu với Composer:

| Composer (PHP) | Gradle (project này) | Ghi chú |
|---|---|---|
| `composer.json` | `build.gradle` (+ `settings.gradle`) | `build.gradle` viết bằng **Groovy**, là code chứ không phải JSON |
| `composer.lock` | Không có file lock | Phiên bản được cố định bởi BOM của Spring Boot (mục bên dưới). Gradle có tính năng *dependency locking* nhưng project không bật |
| Packagist | Maven Central (`repositories { mavenCentral() }`) | |
| `vendor/` trong project | `~/.gradle/caches/` (thư mục **chung** của máy, ngoài project) | Mọi project trên máy dùng chung một kho đã tải |
| `composer install` / `require` | Không có lệnh riêng: sửa `build.gradle`, lần build sau Gradle tự tải | |
| `vendor/autoload.php` | **Classpath**: danh sách `.jar` + thư mục `.class` mà JVM được phép nạp | Gradle tự lập classpath cho từng việc (biên dịch, chạy, test) |
| `scripts` trong `composer.json` | **Task**: `build`, `bootRun`, `test`... | Plugin thêm task cho mình |
| `composer.phar` cài theo project | **Wrapper** `gradlew` | Tự tải đúng phiên bản Gradle mà project cần |

## Khái niệm cốt lõi

### Các file của Gradle trong project

```
java_code/
├── settings.gradle          tên project ('demo') + nơi tải PLUGIN
├── build.gradle             plugin, phiên bản Java, danh sách thư viện, cấu hình task
├── gradle.properties        tuỳ chỉnh Gradle: thử lại khi mạng chập chờn, timeout
├── gradlew, gradlew.bat     wrapper: script chạy Gradle (Linux/Mac, Windows)
├── gradle/wrapper/          gradle-wrapper.jar + gradle-wrapper.properties (bản 8.11)
├── .gradle/                 cache riêng của project (do Gradle tạo, đã .gitignore)
└── build/                   mọi thứ Gradle sinh ra: .class, .jar... (đã .gitignore)
```

Cú pháp Groovy của `build.gradle` (vì sao khối code trong bài học ghi là `groovy`): xem [kien-thuc/doc-bai-hoc-va-tra-cuu.md](../kien-thuc/doc-bai-hoc-va-tra-cuu.md#vì-sao-khối-code-của-buildgradle-lại-ghi-là-groovy).

### Plugin: thứ thêm task và quy ước

```groovy
plugins {
    id 'java'                                                  // biên dịch Java, task compileJava, jar, test...
    id 'org.springframework.boot' version '3.4.1'             // bootRun, bootJar, cấu hình developmentOnly...
    id 'io.spring.dependency-management' version '1.1.7'      // nhập BOM của Spring Boot → tự chọn phiên bản thư viện
}
```

Bản thân Gradle gần như "trống". Mọi khả năng đều đến từ plugin. Riêng plugin Spring Boot, khi thấy plugin `java`, sẽ tự làm thêm (theo [tài liệu plugin](https://docs.spring.io/spring-boot/3.4/gradle-plugin/reacting.html)):

- Tạo task `bootJar` (đóng `.jar` chạy được, chứa cả thư viện bên trong) và `bootRun` (chạy app).
- Đổi tên file của task `jar` thường thành `...-plain.jar`, để file chính là của `bootJar`.
- Tạo configuration `developmentOnly` (dành cho DevTools).
- Bật cờ `-parameters` cho `javac`: giữ lại tên tham số trong `.class`. Nhờ vậy `@PathVariable Long id` biết tên `id` mà không phải ghi `@PathVariable("id")` (xem [spring-web-mvc.md](spring-web-mvc.md)).
- Đặt bảng mã UTF-8 cho file `.java`: comment tiếng Việt không bị lỗi khi biên dịch.

Plugin được tải từ đâu thì do `settings.gradle` quyết định. Project đặt `mavenCentral()` **trước** `gradlePluginPortal()` vì máy chủ của Plugin Portal từng bị chặn ở mạng này (lỗi `Remote host terminated the handshake`).

### Java toolchain

```groovy
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
```

Gradle biên dịch và chạy app bằng **JDK 21**, dù bản thân Gradle đang chạy bằng Java nào. Trong container `gradle:8.11-jdk21` thì luôn có JDK 21. Còn chạy `./gradlew` trên máy host thì máy phải **có sẵn** JDK 21: project không cấu hình chỗ tải JDK tự động, nên máy chỉ có Java 25 sẽ báo lỗi không tìm thấy bản 21 phù hợp.

### Khai báo thư viện: tọa độ và configuration

Mỗi thư viện được gọi bằng **tọa độ** `group:artifact:version`:

```groovy
implementation 'nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect:3.4.0'
//              └──── group ─────────┘ └──── artifact ────────┘ └ version
```

Từ đứng trước tọa độ là **configuration**: nó quyết định thư viện có mặt ở **giai đoạn nào**. Đây là chỗ Composer không có (Composer chỉ có `require` và `require-dev`):

| Configuration | Lúc biên dịch | Lúc chạy app | Đóng vào `.jar` | Trong project |
|---|---|---|---|---|
| `implementation` | ✔ | ✔ | ✔ | Các `spring-boot-starter-*`, layout dialect, `thymeleaf-extras-springsecurity6` |
| `runtimeOnly` | ✘ | ✔ | ✔ | `mysql-connector-j`: code chỉ dùng JDBC chung, driver MySQL chỉ cần lúc chạy |
| `compileOnly` | ✔ | ✘ | ✘ | Lombok (bài 17): chỉ cần cho `javac` hiểu `@Getter` |
| `annotationProcessor` | Chạy **bên trong** `javac` | ✘ | ✘ | Lombok (bài 17): sinh getter/setter, biến `log` |
| `developmentOnly` | ✘ | ✔ khi `bootRun` | ✘ | `spring-boot-devtools` |
| `testImplementation` | ✔ (code test) | ✔ (khi chạy test) | ✘ | `spring-boot-starter-test` |
| `testRuntimeOnly` | ✘ | ✔ (khi chạy test) | ✘ | `junit-platform-launcher`: Gradle cần nó để khởi động JUnit 5 |

Hai điều rút ra:

- **`runtimeOnly` là cố ý.** Nếu bạn `import com.mysql...` trong code thì sẽ lỗi biên dịch: đó là dấu hiệu đang viết code dính chặt vào MySQL, thứ mà JPA giúp tránh.
- **Lombok cần cả `compileOnly` lẫn `annotationProcessor`.** Thiếu cái đầu thì `import lombok.Getter` lỗi; thiếu cái sau thì `javac` hiểu chữ `@Getter` nhưng không ai sinh code ([bài 17 Bước 1](../study/17-lombok-slf4j-getter-setter.md#bước-1--buildgradle-thêm-lombok)).

Thư viện còn kéo theo **thư viện phụ thuộc** (transitive). `spring-boot-starter-web` kéo Spring MVC, Tomcat nhúng, Jackson; `spring-boot-starter-data-redis` kéo Lettuce. Đó là lý do dòng `implementation` ít mà `~/.gradle/caches` nặng cả trăm MB. Xem cây phụ thuộc bằng `gradle dependencies --configuration runtimeClasspath`.

### Phiên bản do Spring Boot quản lý (BOM)

Gần như mọi dòng trong `dependencies` đều **không ghi số phiên bản**. Plugin `io.spring.dependency-management` nhập **BOM** (*Bill of Materials*: bảng phiên bản đã được kiểm tra là chạy được với nhau) của Spring Boot 3.4.1, rồi tự điền phiên bản. Một số phiên bản mà BOM này chọn:

| Thư viện | Phiên bản (Spring Boot 3.4.1) |
|---|---|
| Spring Framework | 6.2.1 |
| Spring Security | 6.4.2 |
| Hibernate ORM | 6.6.4.Final |
| Jackson | 2.18.2 |
| Tomcat nhúng | 10.1.34 |
| Thymeleaf | 3.1.3.RELEASE |
| MySQL Connector/J | 9.1.0 |
| Lettuce (Redis) / MongoDB Java Driver | 6.4.1.RELEASE / 5.2.1 |
| Logback / SLF4J | 1.5.12 / 2.0.16 |
| Lombok | 1.18.36 |
| JUnit Jupiter | 5.11.4 |

(Đọc từ file `spring-boot-dependencies-3.4.1.pom` trong cache Gradle.)

Muốn nâng **cả bộ** thì chỉ cần đổi `version '3.4.1'` của plugin Spring Boot. Thư viện **ngoài** BOM thì phải ghi phiên bản. Nếu ghi phiên bản cho một thư viện **có trong** BOM thì phiên bản ghi tay **thắng**. Ví dụ ở project: BOM chọn `thymeleaf-layout-dialect` 3.3.0, nhưng `build.gradle` ghi `3.4.0` nên Gradle dùng 3.4.0. Ghi tay như vậy thì mình tự chịu trách nhiệm về độ tương thích.

### Wrapper `gradlew`

`gradlew` là một script nhỏ: lần đầu chạy, nó đọc `gradle/wrapper/gradle-wrapper.properties`, tải đúng **Gradle 8.11** về `~/.gradle/wrapper/dists/`, rồi chạy bản đó. Nhờ vậy ai clone project về cũng build bằng cùng một phiên bản Gradle, không cần cài Gradle trước (chỉ cần có Java). Trên Windows thì dùng `gradlew.bat`.

Trong Docker, project **không** dùng wrapper: image `gradle:8.11-jdk21` đã cài sẵn lệnh `gradle` (bản 8.11.x), nên `Dockerfile` và `docker-compose.dev.yml` gọi thẳng `gradle`. Hai cách cho cùng kết quả vì cùng phiên bản.

### Task và các lệnh hay dùng

Mọi việc Gradle làm đều là **task**. Task phụ thuộc nhau, gọi một task thì các task nó cần chạy trước:

```
compileJava ───────┐
                   ├──► classes ──┬──► jar      (demo-0.0.1-SNAPSHOT-plain.jar) ──┐
processResources ──┘              ├──► bootJar  (demo-0.0.1-SNAPSHOT.jar) ────────┴──► assemble ──┐
                                  └──► bootRun  (chạy app)                                        ├──► build
                         compileTestJava ──► test ──► check ──────────────────────────────────────┘
```

- `compileJava`: `.java` → `.class` vào `build/classes/java/main/`.
- `processResources`: copy `src/main/resources/` (template, `application.yml`, `config/*.properties`, ảnh tĩnh) vào `build/resources/main/`.
- `classes`: gom 2 việc trên. Đây là lý do chế độ dev chạy `classes --continuous` chứ không phải `compileJava --continuous`: sửa template cũng phải được copy lại ([bài 04](../study/04-hot-reload.md#cơ-chế-hoạt-động-bên-trong)).
- Gradle **bỏ qua** task mà đầu vào không đổi (log ghi `UP-TO-DATE`), nên build lần hai nhanh hơn nhiều.

Các tuỳ chọn dòng lệnh hay gặp trong project:

| Tuỳ chọn | Nghĩa |
|---|---|
| `-x test` | Bỏ qua task `test` |
| `--continuous` (viết tắt `-t`) | Chạy xong không thoát, canh file đầu vào; đổi là chạy lại |
| `--console=plain` | Log dạng chữ thường, không có thanh tiến trình động: hợp với `docker logs` |
| `-q` | Chỉ in lỗi, bớt ồn |
| `--no-daemon` | Không giữ tiến trình Gradle chạy nền sau khi xong (Docker build chỉ chạy một lần) |
| `--offline` | Không lên mạng, chỉ dùng cache |
| `--refresh-dependencies` | Bỏ qua cache, kiểm tra lại thư viện trên mạng |
| `--stacktrace` / `--info` | In chi tiết hơn khi build lỗi |

### Thư mục `build/` và cache

| Đường dẫn | Chứa gì |
|---|---|
| `build/classes/java/main/` | File `.class` (đây là chỗ `javap` đọc ở [bài 17](../study/17-lombok-slf4j-getter-setter.md)) |
| `build/resources/main/` | Bản copy của `src/main/resources/` |
| `build/libs/` | File `.jar` sau `bootJar` / `build`: `demo-0.0.1-SNAPSHOT.jar` (tên = `rootProject.name` + `version`) |
| `build/generated/sources/annotationProcessor/` | Chỗ dành cho code mà annotation processor sinh ra **dưới dạng file `.java`**. Lombok không ghi file nào: nó chèn code ngay trong lúc `javac` biên dịch, nên thư mục này trống |
| `build/reports/` | Báo cáo: kết quả test, các cảnh báo của Gradle |
| `build/resolvedMainClassName` | Plugin Spring Boot ghi tên class có `main()` (`com.example.demo.DemoApplication`) |

`build/` xoá đi lúc nào cũng được (`gradle clean`), lần build sau sẽ tạo lại. Còn **thư viện đã tải** nằm ở chỗ khác, tuỳ ai chạy Gradle:

| Ai chạy Gradle | Cache thư viện nằm ở |
|---|---|
| VSCode / `./gradlew` trên máy host | `~/.gradle/caches/` của bạn |
| Container `app-dev` | Volume Docker `gradle_cache` (tên đầy đủ `java_code_gradle_cache`), gắn vào `/home/gradle/.gradle` qua `GRADLE_USER_HOME` |
| `docker compose up --build` (`Dockerfile`) | Bên trong lớp image đang build; không có cache dùng lại giữa các lần build |

Riêng thư mục `bin/` là của **VSCode** (extension Java tự biên dịch một bản riêng để gạch đỏ, gợi ý code), không phải của Gradle.

## Project này dùng nó thế nào

**Chế độ dev** (`docker-compose.dev.yml`, [bài 04](../study/04-hot-reload.md)): container `app-dev` dùng image `gradle:8.11-jdk21`, mount cả project vào `/app`, rồi chạy **2 lệnh Gradle song song**:

```yaml
command: >
  sh -c "gradle classes --continuous -x test --console=plain -q &
         exec gradle bootRun --console=plain"
```

- Lệnh 1 chạy nền (`&`): canh file, biên dịch lại và copy resource vào `build/`.
- Lệnh 2 (`bootRun`): chạy app với classpath trỏ vào `build/classes` + `build/resources` + thư viện, **có** DevTools vì đây là `developmentOnly`. DevTools thấy `build/` đổi thì restart app ([spring-boot.md](spring-boot.md)).
- `user: "1000:1000"`: file trong `build/` thuộc về user của bạn, không phải `root`.
- `/app/.gradle` là `tmpfs` (nằm trong RAM của container): tránh tranh khoá với Gradle mà VSCode chạy trên host (lỗi `Timeout waiting to lock ... buildOutputCleanup`).
- Sửa `build.gradle` thì `bootRun` **không** nhận thư viện mới: classpath của nó đã chốt từ lúc khởi động. Phải `docker compose -f docker-compose.dev.yml restart app-dev` ([bài 14 Bước 3](../study/14-moi-truong-mongodb-redis.md#bước-3--thêm-thư-viện-vào-buildgradle), [bài 17 Bước 1](../study/17-lombok-slf4j-getter-setter.md#bước-1--buildgradle-thêm-lombok)).

**Chế độ thường** (`Dockerfile`): build 2 giai đoạn.

```dockerfile
FROM gradle:8.11-jdk21 AS build
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY src ./src
RUN gradle bootJar --no-daemon -x test          # → build/libs/demo-0.0.1-SNAPSHOT.jar

FROM eclipse-temurin:21-jre                     # chỉ có JRE, không có Gradle
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Chỉ chạy `bootJar` (không phải `build`) nên `build/libs/` chỉ có **một** file `.jar`, và `COPY ... *.jar app.jar` chạy được. DevTools không có trong file này.

Hai điểm cần biết về `Dockerfile` hiện tại:

- Comment trong file nói copy `build.gradle` trước "để tận dụng cache của Docker". Nhưng giữa `COPY build.gradle` và `COPY src` không có bước `RUN` nào tải thư viện, nên mỗi lần sửa code, lệnh `RUN gradle bootJar` chạy lại từ đầu và **tải lại toàn bộ thư viện**. Muốn tận dụng thật thì cần thêm một bước tải thư viện trước `COPY src`. Project chưa làm.
- `gradle.properties` **không** được copy vào image, nên các tuỳ chỉnh thử lại/timeout khi mạng chập chờn chỉ có tác dụng ở chế độ dev (nơi cả project được mount).

**`build.gradle` qua các bài:**

| Bài | Thêm vào `build.gradle` |
|---|---|
| Ban đầu | `starter-web`, `starter-data-jpa`, `starter-validation`, `mysql-connector-j`, `starter-actuator`, `devtools`, `starter-test` |
| [05](../study/05-giao-dien-thymeleaf.md#bước-1--thêm-thư-viện-thymeleaf) | `spring-boot-starter-thymeleaf` |
| [07](../study/07-tach-giao-dien-fragment.md#bước-1--thêm-thư-viện-layout-dialect) | `thymeleaf-layout-dialect:3.4.0` |
| [12](../study/12-dang-nhap-spring-security.md#bước-1--thêm-thư-viện-và-thử-khoá-cửa) | `spring-boot-starter-security`, `thymeleaf-extras-springsecurity6` |
| [14](../study/14-moi-truong-mongodb-redis.md#bước-3--thêm-thư-viện-vào-buildgradle) | `spring-boot-starter-data-mongodb`, `spring-boot-starter-data-redis` |
| [17](../study/17-lombok-slf4j-getter-setter.md#bước-1--buildgradle-thêm-lombok) | `compileOnly` + `annotationProcessor` `org.projectlombok:lombok` (tại thời điểm viết doc này, `build.gradle` **chưa** có 2 dòng này) |

**Chạy Gradle trên host** (ít dùng, cần JDK 21): chỉ bật DB bằng Docker, rồi truyền cổng qua biến môi trường, ví dụ `DB_PORT=3313 MONGO_PORT=27018 REDIS_PORT=6380 ./gradlew bootRun` ([bài 14](../study/14-moi-truong-mongodb-redis.md#chạy-app-ngay-trên-máy-không-docker-thì-sao)). Đừng chạy cùng lúc với container `app-dev`: hai bên dùng chung thư mục `build/` và cổng.

## Tra cứu nhanh

| Lệnh / Khai báo | Của ai | Làm gì | Ghi chú |
|---|---|---|---|
| `./gradlew <task>` | Gradle wrapper | Chạy task bằng đúng Gradle 8.11 | Trong container dùng `gradle <task>` |
| `build` | Plugin `java` | Biên dịch, test, đóng gói | Ra 2 file: `.jar` và `-plain.jar` |
| `bootJar` | Plugin Spring Boot | Đóng `.jar` chạy được bằng `java -jar` | Không chứa DevTools |
| `bootRun` | Plugin Spring Boot | Chạy app từ `build/classes` | Có DevTools |
| `classes` | Plugin `java` | `compileJava` + `processResources` | Dùng cho hot reload |
| `test` | Plugin `java` | Chạy JUnit 5 | Project chưa có `src/test/` nên không chạy gì |
| `test --tests '*ProductServiceTest*'` | Plugin `java` | Chỉ chạy test khớp mẫu | |
| `clean` | Gradle | Xoá `build/` | |
| `dependencies --configuration runtimeClasspath` | Gradle | In cây thư viện lúc chạy | |
| `dependencyInsight --dependency jackson-databind` | Gradle | Vì sao có thư viện này, bản nào được chọn | |
| `tasks` | Gradle | Liệt kê mọi task | |
| `implementation` / `runtimeOnly` / `compileOnly` | Plugin `java` | Thư viện cho biên dịch + chạy / chỉ chạy / chỉ biên dịch | Xem bảng configuration |
| `annotationProcessor` | Plugin `java` | Chương trình chạy trong `javac` để sinh code | Lombok |
| `developmentOnly` | Plugin Spring Boot | Chỉ có khi `bootRun`, không vào `.jar` | DevTools |
| `testImplementation` / `testRuntimeOnly` | Plugin `java` | Thư viện chỉ cho code test | |
| `mavenCentral()` | Gradle | Kho tải thư viện | Trong `settings.gradle` còn dùng để tải plugin |

## Bẫy thường gặp

| Triệu chứng | Nguyên nhân | Cách sửa |
|---|---|---|
| Thêm dòng vào `build.gradle` mà app báo `cannot find symbol`, `package ... does not exist`, hoặc `NoSuchBeanDefinitionException` | Container `app-dev` chưa đọc lại `build.gradle` | `docker compose -f docker-compose.dev.yml restart app-dev` |
| Build trong Docker chạy được, nhưng VSCode vẫn gạch đỏ `import` mới | VSCode chưa đồng bộ `build.gradle` | `Ctrl+Shift+P` → **Java: Reload Projects**; nặng hơn thì **Java: Clean Java Language Server Workspace** |
| `Timeout waiting to lock ... buildOutputCleanup` | Gradle trong container và Gradle của VSCode cùng khoá `.gradle/` | Project đã xử lý bằng `tmpfs` cho `/app/.gradle`. Nếu tự sửa compose, giữ nguyên khối đó |
| `Remote host terminated the handshake` khi tải plugin | Máy chủ Gradle Plugin Portal bị chặn | Giữ `mavenCentral()` đứng đầu trong `pluginManagement` của `settings.gradle` |
| `./gradlew: Permission denied` | File mất quyền chạy (thường do copy qua Windows) | `chmod +x gradlew` |
| Chạy `./gradlew` trên host báo không tìm thấy Java 21 | Toolchain đòi JDK 21, máy chỉ có bản khác | Cài JDK 21, hoặc chạy bằng Docker như bình thường |
| File trong `build/` thuộc `root`, VSCode/host không xoá được | Container chạy Gradle bằng `root` | Giữ `user: "1000:1000"` trong compose; sửa quyền bằng `sudo chown -R $USER build` |
| Đổi `Dockerfile` sang `gradle build` thì `COPY ... *.jar app.jar` lỗi | `build` sinh thêm `...-plain.jar`, có 2 file khớp `*.jar` | Giữ `bootJar`, hoặc chỉ rõ tên file |
| Lombok: `cannot find symbol: method getName()` | Thiếu dòng `annotationProcessor` | Thêm đủ 2 dòng ([bài 17](../study/17-lombok-slf4j-getter-setter.md#bước-1--buildgradle-thêm-lombok)) |
| `./gradlew test` báo thành công mà không chạy test nào | Chưa có thư mục `src/test/java` | Bình thường. Muốn có test thì tạo thư mục đó trước |
| Ghi phiên bản tay cho thư viện của Spring, app lỗi lạ lúc chạy (`NoSuchMethodError`, `ClassNotFoundException`) | Phiên bản ghi tay thắng BOM, nhưng lệch với các thư viện Spring khác | Bỏ số phiên bản, để BOM chọn |
| `docker compose up --build` lần nào cũng tải lại thư viện, rất lâu | `Dockerfile` không có bước tải thư viện riêng trước `COPY src` | Biết vậy; dùng chế độ dev khi đang code |
| `down -v` xong thì lần `up` sau rất lâu | `-v` xoá cả volume `gradle_cache`, phải tải lại hết thư viện | Muốn chỉ làm sạch dữ liệu thì dùng script reset của [bài 15.5](../study/15.5-reset-du-lieu-va-chay-lai-seeder.md) |

## Đọc thêm

- Tài liệu chính thức: [Gradle 8.11 User Manual](https://docs.gradle.org/8.11/userguide/userguide.html), các mục [Gradle Wrapper](https://docs.gradle.org/8.11/userguide/gradle_wrapper.html), [Java Plugin](https://docs.gradle.org/8.11/userguide/java_plugin.html), [Continuous Build](https://docs.gradle.org/8.11/userguide/continuous_builds.html), [Toolchains](https://docs.gradle.org/8.11/userguide/toolchains.html).
- Plugin Spring Boot cho Gradle: [Spring Boot Gradle Plugin 3.4](https://docs.spring.io/spring-boot/3.4/gradle-plugin/index.html), [Phản ứng với plugin khác](https://docs.spring.io/spring-boot/3.4/gradle-plugin/reacting.html).
- Plugin quản lý phiên bản: [Dependency Management Plugin](https://docs.spring.io/dependency-management-plugin/docs/current/reference/html/).
- Bài học: [03 Cheatsheet](../study/03-cheatsheet.md#gradle-chạy-app-trực-tiếp-trên-máy-không-qua-docker) · [04 Hot reload](../study/04-hot-reload.md) · [14 Bước 3](../study/14-moi-truong-mongodb-redis.md#bước-3--thêm-thư-viện-vào-buildgradle) · [17 Bước 1](../study/17-lombok-slf4j-getter-setter.md#bước-1--buildgradle-thêm-lombok).
- Tài liệu liên quan: [spring-boot.md](spring-boot.md) (DevTools, starter) · [docker.md](docker.md) (2 chế độ chạy) · [lombok.md](lombok.md).
