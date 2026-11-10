# Lombok

> **Dùng trong project từ:** [bài 17](../study/17-lombok-slf4j-getter-setter.md) · **Phiên bản:** Lombok 1.18.36 (Spring Boot 3.4.1 tự chọn; sau [bài 16.5](../study/16.5-nang-cap-spring-boot-4.md) là 1.18.48 do Spring Boot 4.1.2 chọn, nên `build.gradle` không ghi số phiên bản) · **Tài liệu chính thức:** [projectlombok.org/features](https://projectlombok.org/features/), [Cài với Gradle](https://projectlombok.org/setup/gradle)

> **Trạng thái hiện tại (kiểm tra ngày 2026-11-10):** người học **chưa làm** bài 17. `build.gradle` **chưa có** Lombok. 5 class vẫn khai báo logger bằng `LoggerFactory.getLogger(...)`, và 3 entity `Product`, `Category`, `User` vẫn viết getter/setter bằng tay. Mọi đoạn "project dùng thế nào" dưới đây mô tả trạng thái **sau** khi làm xong bài 17.

## Nó là gì, giải quyết vấn đề gì

Java bắt viết rất nhiều code lặp lại, không có logic gì. Ví dụ `Product` có 6 trường thì có 12 method `getXxx()`/`setXxx()`, mỗi class có log thì có một dòng khai báo `Logger`. Lombok là thư viện **sinh code đó lúc biên dịch**: bạn chỉ đặt annotation, ví dụ `@Getter`, và file `.class` ra đời có đủ getter, y như khi bạn tự viết tay.

So với PHP:

| | PHP | Lombok (Java) |
|---|---|---|
| Không muốn viết getter/setter | Để `public` property, hoặc dùng magic `__get`/`__set` | `@Getter` / `@Setter` |
| Cơ chế | Chạy lúc **runtime** (PHP tự gọi `__get` khi đọc property) | Chạy lúc **biên dịch**. Lúc app chạy, Lombok **không có mặt** |
| Công cụ khác đọc được không? | Magic method khó cho IDE và thư viện phân tích | `.class` có method thật nên Spring, Hibernate, Jackson, Thymeleaf đọc bình thường |

Java 16+ có `record` để viết class dữ liệu ngắn gọn mà không cần thư viện, nhưng `record` không đổi giá trị được và không có constructor rỗng, nên **không dùng làm entity JPA** được. Vì vậy Lombok vẫn phổ biến trong các project Spring.

## Khái niệm cốt lõi

### Annotation processor: code được sinh lúc biên dịch

```
Product.java (chỉ có trường + @Getter @Setter)
      │
      ▼  javac biên dịch; Lombok chen vào giữa, thêm getName(), setName(...)...
      │
Product.class (có ĐỦ getter/setter, y như bạn tự viết)
```

`javac` (trình biên dịch Java) cho phép cắm thêm **annotation processor**: chương trình chạy trong lúc biên dịch để xử lý annotation. Annotation processor bình thường chỉ được **tạo file mới**. Lombok thì khác: nó dùng API nội bộ của `javac` để **sửa thẳng** class đang biên dịch. Hai hệ quả:

- File `.java` **không đổi**. Bạn không thấy method nào trong code, nhưng gọi `product.getName()` vẫn biên dịch được.
- Lombok phụ thuộc vào cấu trúc bên trong của `javac`, nên mỗi khi Java ra bản mới thì phải có bản Lombok mới tương ứng. Dùng Lombok quá cũ với JDK quá mới sẽ lỗi biên dịch. Để Spring Boot chọn phiên bản (1.18.36, chạy tốt với Java 21) là cách an toàn nhất.

### Hai dòng trong Gradle: `compileOnly` + `annotationProcessor`

```groovy
compileOnly 'org.projectlombok:lombok'
annotationProcessor 'org.projectlombok:lombok'
```

| Dòng | Làm gì | Thiếu thì sao |
|---|---|---|
| `compileOnly` | Cho code **thấy** các annotation (`import lombok.Getter;` biên dịch được). Không đóng gói vào app khi chạy | `package lombok does not exist` |
| `annotationProcessor` | Bảo `javac` **chạy** Lombok trong lúc biên dịch | `javac` hiểu chữ `@Getter` nhưng không ai sinh code → hàng loạt `cannot find symbol: method getName()` |

Không ghi số phiên bản vì plugin `io.spring.dependency-management` tự lấy bản Spring Boot đã chọn. Test (`src/test/`) cần thêm `testCompileOnly` và `testAnnotationProcessor`. Xem thêm [gradle.md](gradle.md) và [bài 17 Bước 1](../study/17-lombok-slf4j-getter-setter.md#bước-1--buildgradle-thêm-lombok).

### `@Slf4j`: sinh dòng khai báo logger

```java
@Slf4j
@Service
public class ProductStatsService { ... log.warn("[Stats] ..."); }

// Lombok sinh ra, đúng như bản viết tay:
private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProductStatsService.class);
```

- Tên biến luôn là `log`, tên logger luôn đúng class đang chứa nó. Không còn lỗi copy-paste quên sửa tên class.
- Lombok có thêm các anh em cho thư viện log khác: `@Log4j2`, `@CommonsLog`, `@Log` (`java.util.logging`). Project dùng SLF4J nên chỉ cần `@Slf4j`.
- **`ModuleConfigLoader` không dùng được `@Slf4j`**: nó cần logger "hoãn" của `DeferredLogFactory`, thứ không annotation Lombok nào sinh ra. Giải thích ở [logging.md](logging.md#log-trước-khi-hệ-thống-log-sẵn-sàng-deferredlogfactory).

### `@Getter` / `@Setter`: đặt trên class hay trên trường

```java
@Getter @Setter              // trên class: mọi trường (trừ static) đều có getter + setter
@Entity
public class Product {
    private long id;
    private String name;
    ...
}

public class FileStorageService {
    @Getter                  // trên trường: chỉ trường này có getter
    private final Path root;
}
```

Quy tắc đặt tên Lombok dùng (theo chuẩn JavaBeans, cũng là chuẩn mà Spring, Jackson, Thymeleaf đọc):

| Trường | Getter | Setter |
|---|---|---|
| `String name` | `getName()` | `setName(String)` |
| `long id` | `getId()` trả `long` | `setId(long)` |
| `boolean active` (nguyên thuỷ) | **`isActive()`** | `setActive(boolean)` |
| `boolean isActive` (tên đã có chữ `is`) | `isActive()` (Lombok không thêm `is` lần nữa) | `setActive(boolean)` |
| `Boolean active` (kiểu bọc) | `getActive()` (chữ `get`, không phải `is`) | `setActive(Boolean)` |
| `final` | Có | **Không** sinh |
| `static` | Không sinh (đặt trên class) | Không sinh |

Thêm vài điều cần biết:

- Class đã có method trùng tên (ví dụ bạn còn sót `getName()` viết tay) thì Lombok **bỏ qua** method đó, dùng bản của bạn và không báo gì.
- Đổi mức truy cập: `@Setter(AccessLevel.PROTECTED)`. Tắt cho một trường khi đã đặt trên class: `@Getter(AccessLevel.NONE)`.
- Annotation của thư viện khác đặt **trên trường** vẫn có tác dụng như cũ. Ví dụ `@JsonIgnore` trên `Category.products`: Jackson tự áp nó cho getter tương ứng.

### Các annotation khác: biết để đọc code trên mạng

Ví dụ trên mạng rất hay đặt `@Data` hay `@Builder` lên entity. Project này **cấm** hoặc **không dùng** những annotation đó cho entity, vì những lý do sau:

| Annotation | Sinh ra | Trong project | Vì sao |
|---|---|---|---|
| `@Data` | `@Getter` + `@Setter` + `@ToString` + `@EqualsAndHashCode` + `@RequiredArgsConstructor` | **Cấm** cho entity | Kéo theo `@ToString` và `@EqualsAndHashCode`, xem hai dòng dưới |
| `@ToString` | `toString()` in **mọi trường** | **Cấm** cho entity | Quan hệ 2 chiều `Category ↔ Product`: `product.toString()` → `category.toString()` → `products.toString()` → `product.toString()`... đến khi **`StackOverflowError`**. Đụng vào danh sách lazy → `LazyInitializationException` hoặc bắn thêm SQL. Trên `User` thì in cả **`password`** ra log |
| `@EqualsAndHashCode` | `equals()` / `hashCode()` từ **mọi trường** | **Cấm** cho entity | Cũng vòng lặp vô tận như trên. Ngoài ra `hashCode()` **đổi giá trị** sau khi lưu (vì `id` từ 0/null thành số thật), nên entity đã bỏ vào `HashSet` trước khi lưu sẽ "mất tích" khỏi set |
| `@Builder` | Kiểu tạo object `User.builder().username("an").build()` | Không dùng cho entity | Cần constructor đủ mọi tham số, nên đụng với constructor rỗng mà JPA bắt buộc. Giá trị khởi tạo của trường **bị bỏ qua**: `role = "USER"` thành `null`, trừ khi thêm `@Builder.Default` |
| `@AllArgsConstructor` | Constructor nhận mọi trường, theo **thứ tự khai báo** | Không dùng cho entity | Đổi thứ tự 2 trường cùng kiểu `String` là mọi chỗ gọi constructor **lặng lẽ** truyền sai, không có lỗi biên dịch |
| `@NoArgsConstructor` | Constructor rỗng | Không cần | Entity đã viết tay `public Product() {}` cho rõ ý |
| `@RequiredArgsConstructor` | Constructor cho mọi trường **`final`** (và `@NonNull`) | **Không dùng** cho Service | Quy tắc bài 17: constructor injection viết tay để thấy rõ class cần gì. Bẫy thật: trong `ProductService`, trường `productRepository` **không có `final`**, nên `@RequiredArgsConstructor` sẽ bỏ qua nó âm thầm, và `productRepository` là `null` lúc chạy |
| `@Value` (Lombok) | Class bất biến: mọi trường `private final`, chỉ có getter | Không dùng | **Trùng tên** với `@Value` của Spring (đọc cấu hình). Import nhầm là lỗi rất khó hiểu |
| `@SneakyThrows` | Ném checked exception mà không cần khai báo `throws` | Không dùng | Giấu lỗi khỏi trình biên dịch. Project xử lý lỗi theo kiểu ném `RuntimeException` rõ ràng |

Nếu sau này thật sự cần `toString()` cho entity, cách an toàn là tự viết tay chỉ với `id` và `name`. Hoặc dùng `@ToString` kèm `@ToString.Exclude` trên trường quan hệ và trường nhạy cảm, nhưng như vậy phải nhớ loại trừ mỗi khi thêm trường mới.

### IDE: vì sao VSCode có thể gạch đỏ

Trình soạn thảo cũng phải "biết" Lombok, nếu không nó sẽ báo `getName()` không tồn tại (vì trong file `.java` đúng là không có).

- **VSCode**: extension *Language Support for Java by Red Hat* đã kèm sẵn hỗ trợ Lombok, không phải cài thêm. Build trong Docker chạy được mà VSCode vẫn gạch đỏ: `Ctrl+Shift+P` → **Java: Clean Java Language Server Workspace** ([bài 17 Bước 4](../study/17-lombok-slf4j-getter-setter.md#bước-4--vscode-không-gạch-đỏ)).
- **IntelliJ IDEA**: plugin Lombok có sẵn. Lần đầu mở project, IntelliJ hỏi bật *annotation processing*, chọn bật.

Gạch đỏ trong IDE và lỗi biên dịch thật là **hai chuyện khác nhau**. Thứ quyết định là log của container `demo-app-dev`.

### Xem code Lombok đã sinh: `javap` và delombok

**`javap`** (có sẵn trong JDK) liệt kê trường và method trong file `.class`. Lệnh đã chạy thử khi viết bài 17:

```bash
docker exec demo-app-dev javap -p build/classes/java/main/com/example/demo/product/Product.class
#   public long getId();
#   public java.lang.String getName();
#   public void setId(long);
#   ...
```

**Delombok** là công cụ đi kèm Lombok, chuyển code có annotation thành code Java thường (đã "bung" hết getter/setter), để đọc hoặc để bỏ hẳn Lombok khỏi project. Cú pháp theo tài liệu chính thức là `java -jar lombok.jar delombok src -d src-delomboked`. Trong project, file `lombok.jar` nằm trong cache Gradle của container (volume `gradle_cache`), nên phải tìm đường dẫn của nó trước. Lệnh này **chưa chạy thử** trong project.

### `lombok.config`: cấu hình chung

Lombok đọc file `lombok.config` ở thư mục gốc project (nếu có), ví dụ `lombok.log.fieldName = logger` để đổi tên biến `log`. Project **không có** file này và bài 17 không cần. Đổi cấu hình mặc định làm code khác với mọi ví dụ trên mạng, nên chỉ thêm khi có lý do rõ ràng.

## Project này dùng nó thế nào

Sau bài 17 (theo [bài 17 — Ý tưởng](../study/17-lombok-slf4j-getter-setter.md#ý-tưởng)):

| File | Dùng | Thay cho |
|---|---|---|
| `build.gradle` | `compileOnly` + `annotationProcessor 'org.projectlombok:lombok'` | |
| `nosql/NoSqlConnectionCheck`, `seed/DataSeeder`, `product/ProductService`, `storage/FileStorageService`, `stats/ProductStatsService` | `@Slf4j` | Dòng `private static final Logger log = LoggerFactory.getLogger(...)` + 2 dòng `import org.slf4j...` |
| `product/Product` | `@Getter @Setter` trên class | 12 method |
| `category/Category` | `@Getter @Setter` trên class (`@JsonIgnore` vẫn trên trường `products`) | 6 method |
| `user/User` | `@Getter @Setter` trên class | 8 method |
| `common/ModuleConfigLoader` | **Không đổi** | Vẫn dùng `DeferredLogFactory` |

Ví dụ `Product` sau bài 17:

```java
@Getter     // getId(), getName(), getPrice(), getDescription(), getImagePath(), getCategory()
@Setter     // setId(long), setName(String), ...
@Entity
@Table(name="Product")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;          // vẫn long nguyên thuỷ → getId() trả long → save() vẫn so được == 0
    ...
    public Product() {}       // giữ lại: JPA bắt buộc có constructor rỗng
}
```

Quy tắc của project (bài 17 và [CLAUDE.md](../../CLAUDE.md)):

- Chỉ dùng **3 annotation**: `@Slf4j`, `@Getter`, `@Setter`.
- Entity **không** dùng `@Data`, `@ToString`, `@EqualsAndHashCode`, `@Builder`, `@AllArgsConstructor`.
- Service giữ constructor viết tay, **không** dùng `@RequiredArgsConstructor`.
- Không có gì thay đổi về hành vi: JSON của `/api/products` giữ nguyên, form vẫn dùng setter để điền dữ liệu, log in ra giữ nguyên tên class.
- Mỗi lần thêm trường vào entity thì chỉ cần khai báo trường (getter/setter tự có), nhưng **vẫn phải** cập nhật `DataSeeder`.

Liên quan: [logging.md](logging.md) (logger mà `@Slf4j` sinh ra), [jpa-hibernate.md](jpa-hibernate.md) (entity, quan hệ 2 chiều, lazy), [gradle.md](gradle.md) (`compileOnly`, `annotationProcessor`), [quản lý sản phẩm](../product/quan-ly-san-pham.md), [đăng nhập & đăng ký](../user/dang-nhap-dang-ky.md) (`User`).

## Tra cứu nhanh

| Annotation | Của ai | Làm gì | Ghi chú |
|---|---|---|---|
| `@Slf4j` | `lombok.extern.slf4j.Slf4j` | Sinh `private static final org.slf4j.Logger log` | Dùng trong project |
| `@Getter` | `lombok.Getter` | Sinh getter. Trên class: mọi trường không `static`. Trên trường: trường đó | Dùng trong project. `boolean` → `isXxx()` |
| `@Setter` | `lombok.Setter` | Sinh setter | Dùng trong project. Không sinh cho trường `final` |
| `@Getter(AccessLevel.NONE)` | `lombok.AccessLevel` | Tắt getter cho một trường | |
| `@ToString` / `@ToString.Exclude` | `lombok.ToString` | Sinh `toString()` / loại một trường ra | **Cấm** cho entity |
| `@EqualsAndHashCode` | `lombok.EqualsAndHashCode` | Sinh `equals()` / `hashCode()` | **Cấm** cho entity |
| `@Data` | `lombok.Data` | Gộp 5 annotation | **Cấm** cho entity |
| `@Builder` / `@Builder.Default` | `lombok.Builder` | Kiểu tạo object `X.builder()...build()` / giữ giá trị khởi tạo | Không dùng cho entity |
| `@NoArgsConstructor` | `lombok.NoArgsConstructor` | Constructor rỗng | Không cần, đã viết tay |
| `@AllArgsConstructor` | `lombok.AllArgsConstructor` | Constructor mọi trường | Không dùng cho entity |
| `@RequiredArgsConstructor` | `lombok.RequiredArgsConstructor` | Constructor cho trường `final` | Không dùng. Trường thiếu `final` bị bỏ qua |
| `@Value` | `lombok.Value` | Class bất biến | Không dùng. Đừng nhầm với `@Value` của Spring |
| `compileOnly` / `annotationProcessor` | Gradle | Khai báo Lombok | Cần cả hai |
| `javap -p X.class` | JDK | Xem method trong `.class` | Kiểm tra Lombok đã sinh gì |

## Bẫy thường gặp

| Triệu chứng | Nguyên nhân | Cách sửa |
|---|---|---|
| Hàng chục lỗi `cannot find symbol`, `symbol: variable log`, `method getName()` | Thiếu `annotationProcessor`, hoặc sửa `build.gradle` mà chưa `restart app-dev` | Đủ 2 dòng, rồi `docker compose -f docker-compose.dev.yml restart app-dev` |
| `package lombok does not exist` | Thiếu `compileOnly` | Thêm dòng `compileOnly` |
| `warning: Field 'log' already exists.` | Có `@Slf4j` nhưng còn dòng `Logger log = ...` cũ | Xoá dòng cũ và 2 dòng `import org.slf4j...` |
| Build trong Docker chạy, VSCode vẫn gạch đỏ | IDE chưa đọc lại `build.gradle` | **Java: Clean Java Language Server Workspace** |
| Một getter trả kết quả "lạ" | Còn sót getter viết tay có logic riêng, Lombok nhường cho nó | Xoá method viết tay còn sót |
| `StackOverflowError` khi log entity, bỏ vào `HashSet`, hoặc lúc debug | `@Data` / `@ToString` / `@EqualsAndHashCode` trên entity có quan hệ 2 chiều | Chỉ dùng `@Getter @Setter` |
| `LazyInitializationException` khi log một `Category` | `toString()` do Lombok sinh đụng vào `products` (lazy) | Như trên |
| Mật khẩu (đã mã hoá) xuất hiện trong log | `@ToString` trên `User` + `log.debug("{}", user)` | Bỏ `@ToString`, không log cả entity |
| Entity đã thêm vào `HashSet` thì không tìm lại được sau khi `save()` | `@EqualsAndHashCode` tính theo `id`, mà `id` đổi sau khi lưu | Không dùng `@EqualsAndHashCode` cho entity |
| User tạo bằng `builder()` có `role = null` | `@Builder` bỏ qua giá trị khởi tạo `= "USER"` | Không dùng `@Builder` cho entity (hoặc `@Builder.Default`) |
| `NullPointerException` ở repository trong Service sau khi đổi sang `@RequiredArgsConstructor` | Trường không có `final` nên không được đưa vào constructor | Giữ constructor viết tay (quy tắc project) |
| Lỗi lạ quanh `@Value` | Import `lombok.Value` thay vì `org.springframework.beans.factory.annotation.Value` (hoặc ngược lại) | Kiểm tra dòng `import`. Project không dùng cả hai |
| Lưu sản phẩm mới lại thành sửa, hoặc NPE ở `save()` | Lỡ đổi `long id` thành `Long id` trong lúc sửa `Product` | Giữ `long` (xem CLAUDE.md) |
| Lỗi biên dịch khó hiểu sau khi nâng JDK | Phiên bản Lombok quá cũ so với JDK | Để Spring Boot chọn phiên bản. Nâng Spring Boot thì Lombok nâng theo |
| Dùng Lombok trong `src/test/` báo `package lombok does not exist` | Test có cấu hình riêng | Thêm `testCompileOnly` + `testAnnotationProcessor` |

## Đọc thêm

- [Lombok features](https://projectlombok.org/features/): mỗi annotation một trang, có code "With Lombok" và "Vanilla Java" (code tương đương khi viết tay) đặt cạnh nhau. Đây là cách nhanh nhất để hiểu một annotation sinh ra gì.
- [Lombok — Gradle setup](https://projectlombok.org/setup/gradle).
- [Lombok — delombok](https://projectlombok.org/features/delombok).
- [Bài 17](../study/17-lombok-slf4j-getter-setter.md): hướng dẫn từng bước, phần kiểm tra và bài tập (bài tập 3 và 4 cho bạn tự thấy bẫy `@ToString` và `@Data`).
