# Sổ tay kiến thức chung

Gom lại những câu hỏi "cái này là gì / tại sao lại thế" phát sinh trong lúc học các bài trong `docs/`.
Mỗi mục ghi rõ nó được hỏi ở bài nào, nên có thể đọc riêng từng mục.

## Mục lục

- [Chữ `groovy` (`yaml`, `java`...) sau dấu ``` trong bài học là gì? Groovy là gì?](#chữ-groovy-yaml-java-sau-dấu--trong-bài-học-là-gì-groovy-là-gì)
- [`management.endpoints` và `management.endpoint` (có/không có chữ `s`) khác nhau thế nào?](#managementendpoints-và-managementendpoint-cókhông-có-chữ-s-khác-nhau-thế-nào)
- [`String... args` (3 dấu chấm) trong `run()` là gì?](#string-args-3-dấu-chấm-trong-run-là-gì)
- [BLOB là gì? "Document" của MongoDB có phải là "tài liệu"? Lưu ảnh vào DB hay ra file?](#blob-là-gì-document-của-mongodb-có-phải-là-tài-liệu-lưu-ảnh-vào-db-hay-ra-file)
- [`@Column`: vì sao có trường khai báo, có trường không?](#column-vì-sao-có-trường-khai-báo-có-trường-không)
- [`catch (IOException e)` và `catch (Exception e)` khác nhau thế nào?](#catch-ioexception-e-và-catch-exception-e-khác-nhau-thế-nào)
- [Dấu `{}` trong `log.info(...)` / `log.warn(...)` là gì?](#dấu--trong-loginfo--logwarn-là-gì)
- [Java có giá trị mặc định cho tham số (`String description = ""`) không? Nạp chồng (overload) là gì?](#java-có-giá-trị-mặc-định-cho-tham-số-string-description---không-nạp-chồng-overload-là-gì)
- [`return a() && b() && c();` hoạt động thế nào? (toán tử `&&` và cơ chế "đoản mạch")](#return-a--b--c-hoạt-động-thế-nào-toán-tử--và-cơ-chế-đoản-mạch)
- [Đưa hằng số (ví dụ `RECENT_TTL` 30 ngày) sang `application.yml` được không? `@Value` có đọc được `Duration` không?](#đưa-hằng-số-ví-dụ-recent_ttl-30-ngày-sang-applicationyml-được-không-value-có-đọc-được-duration-không)
- [Mỗi module một class cấu hình riêng (`StatsProperties`, `ProductProperties`) được không? `@ConfigurationProperties` là gì?](#mỗi-module-một-class-cấu-hình-riêng-statsproperties-productproperties-được-không-configurationproperties-là-gì)
- [Java có file cấu hình kiểu `.ini` của PHP (`key = "value"`) không? Có cần tự viết hàm đọc chung không?](#java-có-file-cấu-hình-kiểu-ini-của-php-key--value-không-có-cần-tự-viết-hàm-đọc-chung-không)
- [Mỗi class đều phải tự khai báo logger. Có tạo một logger "common" rồi import vào được không?](#mỗi-class-đều-phải-tự-khai-báo-logger-có-tạo-một-logger-common-rồi-import-vào-được-không)
- [`@Override` là gì? Bỏ đi có sao không? Khác gì nạp chồng (overloading)?](#override-là-gì-bỏ-đi-có-sao-không-khác-gì-nạp-chồng-overloading)
- [Kiểu dữ liệu viết thường (`int`, `long`) và viết hoa (`Integer`, `Long`, `String`, `Duration`) khác nhau thế nào?](#kiểu-dữ-liệu-viết-thường-int-long-và-viết-hoa-integer-long-string-duration-khác-nhau-thế-nào)
- [Hàm có sẵn của thư viện (ví dụ `opsForZSet`) ở đâu ra? Làm sao biết nó làm gì?](#hàm-có-sẵn-của-thư-viện-ví-dụ-opsforzset-ở-đâu-ra-làm-sao-biết-nó-làm-gì)
- [Dấu `()` và dấu `<>` khác nhau thế nào? (`List<Long>`, `new HashMap<>()`, generic)](#dấu--và-dấu--khác-nhau-thế-nào-listlong-new-hashmap-generic)

---

## Chữ `groovy` (`yaml`, `java`...) sau dấu ``` trong bài học là gì? Groovy là gì?

> Hỏi ở: [Bài 14 — Bước 3: Thêm thư viện vào `build.gradle`](study/14-moi-truong-mongodb-redis.md#bước-3--thêm-thư-viện-vào-buildgradle)

### Đó là nhãn ngôn ngữ của khối code Markdown, **không phải code cần copy**

Trong file `.md`, một khối code được bọc bởi 3 dấu backtick. Chữ viết ngay sau 3 backtick mở đầu cho trình xem Markdown biết khối đó viết bằng ngôn ngữ gì, để **tô màu cú pháp** cho đúng:

````markdown
```groovy        ← nhãn ngôn ngữ, KHÔNG copy
    implementation 'org.springframework.boot:spring-boot-starter-data-mongodb'
```              ← dấu đóng khối, cũng KHÔNG copy
````

Chỉ copy **phần nằm giữa** hai dòng backtick.

Các nhãn khác trong giáo trình cũng vậy: ` ```yaml `, ` ```java `, ` ```bash `, ` ```json `, ` ```env `. Tất cả chỉ là nhãn tô màu. Mở file bằng chế độ Preview của VSCode (`Ctrl+Shift+V`) thì sẽ không thấy các chữ này nữa, chỉ thấy khối code đã được tô màu.

### Vì sao khối code của `build.gradle` lại ghi là `groovy`?

Vì `build.gradle` được viết bằng **ngôn ngữ Groovy**.

- **Groovy** là một ngôn ngữ lập trình chạy trên JVM, giống Java. Cú pháp của nó gọn hơn Java: không bắt buộc dấu `;`, khi gọi hàm có thể bỏ dấu ngoặc `()`, chuỗi dùng nháy đơn `'...'` cũng được.
- Gradle dùng Groovy để viết file cấu hình build. Dòng

  ```groovy
  implementation 'org.springframework.boot:spring-boot-starter-data-mongodb'
  ```

  thật ra là một **lời gọi hàm** `implementation(...)` đã bỏ dấu ngoặc. Khối `dependencies { ... }` cũng là một hàm, nhận vào một khối code (closure).
- Gradle còn một kiểu viết khác là **Kotlin DSL**, file tên `build.gradle.kts`. Với kiểu này, dòng trên phải viết là `implementation("org.springframework.boot:...")`: bắt buộc có ngoặc và nháy kép.

| | Groovy DSL (project này) | Kotlin DSL |
|---|---|---|
| Tên file | `build.gradle` | `build.gradle.kts` |
| Thêm thư viện | `implementation 'group:artifact'` | `implementation("group:artifact")` |

Để làm project này, bạn không cần học Groovy. Chỉ cần nhớ khuôn `implementation 'group:artifact'` và thêm vào bên trong khối `dependencies { }` là đủ.

---

## `management.endpoints` và `management.endpoint` (có/không có chữ `s`) khác nhau thế nào?

> Hỏi ở: [Bài 14 — Bước 4: Cấu hình kết nối trong `application.yml`](study/14-moi-truong-mongodb-redis.md#bước-4--cấu-hình-kết-nối-trong-applicationyml)

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

### Bẫy hay gặp: đặt nhầm chỗ sẽ không báo lỗi

Nếu viết nhầm `management.endpoints.health.show-details`, tức là thêm chữ `s`, thì app **vẫn chạy bình thường**. Spring Boot bỏ qua thuộc tính nó không biết, không báo gì cả. Kết quả là `/actuator/health` vẫn chỉ trả `{"status":"UP"}`. Trong VSCode, nếu đã cài extension *Spring Boot Tools*, dòng sai sẽ bị gạch vàng kèm cảnh báo "Unknown property". Đó là cách phát hiện nhanh nhất.

---

## `String... args` (3 dấu chấm) trong `run()` là gì?

> Hỏi ở: [Bài 14 — Bước 5: `NoSqlConnectionCheck`](study/14-moi-truong-mongodb-redis.md#bước-5--thử-ghiđọc-lúc-khởi-động-nosqlconnectioncheck) (cú pháp này xuất hiện lần đầu ở [Bài 13 — Bước 1: `DataSeeder`](study/13-du-lieu-mau-seeder.md#bước-1--khung-class-dataseeder))

### Đây là **varargs**: tham số nhận "bao nhiêu giá trị cũng được"

`String... args` nghĩa là hàm nhận vào **0, 1 hoặc nhiều** giá trị `String`, cách nhau bằng dấu phẩy. Bên trong hàm, `args` thực chất là một **mảng `String[]`**.

```java
static void chao(String... ten) {
    System.out.println("Số người: " + ten.length);   // dùng như mảng
    for (String t : ten) {
        System.out.println("Xin chào " + t);
    }
}

chao();                       // ten = []                → Số người: 0
chao("An");                   // ten = ["An"]            → Số người: 1
chao("An", "Bình", "Chi");    // ten = ["An","Bình","Chi"] → Số người: 3
chao(new String[]{"An"});     // truyền thẳng mảng cũng được
```

So sánh với kiểu mảng thông thường:

| | `String[] args` | `String... args` |
|---|---|---|
| Bên trong hàm | Là mảng | **Cũng là mảng**, dùng y hệt |
| Lúc gọi hàm | Phải tự tạo mảng: `chao(new String[]{"An", "Bình"})` | Liệt kê thẳng: `chao("An", "Bình")`, hoặc không truyền gì: `chao()` |

Tóm lại: `...` chỉ giúp **người gọi hàm** viết gọn hơn. Với người viết bên trong hàm thì nó không khác gì `String[]`.

### Quy tắc

- Mỗi hàm chỉ có **tối đa 1** tham số varargs, và nó phải đứng **cuối cùng**: `void f(int a, String... b)` ✅, còn `void f(String... b, int a)` ❌.
- Khi không truyền gì, `args` là **mảng rỗng** (`length == 0`), **không phải `null`**.
- Hàm quen thuộc cũng dùng varargs: `String.format("%s có %d sản phẩm", ten, soLuong)`, `List.of(1, 2, 3)`.

### Vì sao `run()` của `CommandLineRunner` lại nhận `String... args`?

Interface `CommandLineRunner` của Spring Boot khai báo sẵn hàm `void run(String... args)`. Vì class của mình `implements` interface này nên phải viết **đúng chữ ký** đó. `args` chính là các **tham số dòng lệnh** truyền lúc khởi động app (giống `main(String[] args)`), ví dụ:

```bash
java -jar demo.jar --foo=bar xinchao   # args = ["--foo=bar", "xinchao"]
```

Trong project này app chạy bằng Docker/Gradle, không truyền thêm tham số nào, và `DataSeeder` hay `NoSqlConnectionCheck` cũng **không dùng tới** `args`. Nó có mặt ở đó chỉ vì interface bắt buộc phải có.

---

## BLOB là gì? "Document" của MongoDB có phải là "tài liệu"? Lưu ảnh vào DB hay ra file?

> Hỏi khi chuẩn bị [Bài 15 — Ảnh và mô tả cho sản phẩm](study/15-anh-va-mo-ta-san-pham.md#tư-vấn-trước-khi-code-lưu-ảnh-ở-đâu-file-vật-lý-hay-blob), liên quan tới MongoDB ở [Bài 14](study/14-moi-truong-mongodb-redis.md). Bảng so sánh đầy đủ nằm trong bài 15.

- **BLOB** (*Binary Large Object*) là cách gọi chung cho "một cục byte lớn lưu thẳng trong DB": ảnh, PDF, zip... Đây **không phải tính năng riêng của MongoDB**. MySQL có kiểu cột `BLOB` / `MEDIUMBLOB` (16MB) / `LONGBLOB` (4GB). MongoDB có kiểu `BinData`, nhưng cả document tối đa 16MB. File lớn hơn thì dùng **GridFS**: cắt file thành mảnh 255KB rồi lưu vào 2 collection `fs.files` + `fs.chunks`.
- **"Document" trong MongoDB = một bản ghi dạng JSON**, tương đương một dòng trong bảng MySQL. Nó **không** có nghĩa là "tài liệu" kiểu Word/PDF. MongoDB không có ưu thế đặc biệt nào trong việc lưu file so với MySQL.
- **Lưu ở đâu?** Hầu hết dự án làm theo kiểu: **file để một nơi** (ổ đĩa, hoặc S3/MinIO khi chạy production), **DB chỉ lưu tên file**. Lý do là hiển thị nhanh, DB nhẹ, backup nhanh. BLOB chỉ đáng chọn khi file nhỏ, ít, và bắt buộc phải nằm chung transaction với bản ghi.
- Với project này, lưu ảnh vào MongoDB GridFS còn kém hơn nữa: sản phẩm nằm ở MySQL, ảnh lại ở MongoDB, nên xoá sản phẩm và xoá ảnh không cùng một transaction. Như vậy mất luôn ưu điểm "nhất quán" là lý do chính để chọn BLOB.

---

## `@Column`: vì sao có trường khai báo, có trường không?

> Hỏi ở: [Bài 15 — Bước 3: Thêm 2 trường vào `Product`](study/15-anh-va-mo-ta-san-pham.md#bước-3--thêm-2-trường-vào-product). `@Column` xuất hiện lần đầu ở [Bài 02 — Bước 1: Entity](study/02-thuc-hanh-crud.md#bước-1--entity-mô-tả-bảng-dữ-liệu).

### Mọi trường của `@Entity` đều tự thành cột, không cần `@Column`

JPA làm theo nguyên tắc **"có sẵn mặc định, chỉ khai báo khi muốn khác mặc định"**. Mọi trường trong class `@Entity` đều **tự động** thành một cột, dùng các giá trị mặc định. Chỉ khi muốn **khác** mặc định thì mới cần viết `@Column(...)`. (Ngoại lệ: trường `static`, trường có `transient` hoặc `@Transient` thì không thành cột.)

| Thuộc tính `@Column` | Mặc định (khi không ghi) | Ví dụ trong project |
|---|---|---|
| `name` | Tên trường đổi sang `snake_case`: `imagePath` → `image_path` | Chưa dùng. Chỉ cần khi tên cột trong DB khác hẳn tên trường |
| `nullable` | `true`: được để trống (`NULL`) | `Product.name`: `nullable = false` → cột `NOT NULL` |
| `length` | `255`: `String` → `VARCHAR(255)` | `Product.description`: `length = 2000` → `VARCHAR(2000)` |
| `unique` | `false` | `User.username`: `unique = true` → không cho 2 user trùng tên |
| `precision`, `scale` | Tuỳ DB | Dùng với `BigDecimal`, ví dụ tiền: `precision = 12, scale = 2` |
| `columnDefinition` | Hibernate tự chọn kiểu | Ghi thẳng kiểu SQL, ví dụ `"TEXT"`. Ít dùng, vì dễ khoá chặt vào một loại DB |

Đối chiếu với `Product`:

```java
@Column(nullable = false)      // khác mặc định: bắt buộc có tên
private String name;           //   → name VARCHAR(255) NOT NULL

private Double price;          // mặc định hết → price DOUBLE (cho phép NULL)

@Column(length = 2000)         // khác mặc định: cần dài hơn 255
private String description;    //   → description VARCHAR(2000)

private String imagePath;      // mặc định hết → image_path VARCHAR(255)  (tên file UUID ~40 ký tự là đủ)
```

Trong `User`, dòng `@Column(nullable = true)` ở trường `role` là **thừa**, vì `true` vốn đã là mặc định. Xoá đi thì kết quả vẫn y hệt.

### Những trường không dùng `@Column`

- **`@Id`**: khoá chính, đã có annotation riêng.
- **Quan hệ (`@ManyToOne`)**: dùng **`@JoinColumn(name = "category_id")`** thay cho `@Column`, vì cột này là khoá ngoại.
- **Kiểu nguyên thuỷ** (`long`, `int`, `double`...): không thể chứa `null`, nên Hibernate **tự thêm `NOT NULL`**. Ví dụ `long id` → `BIGINT NOT NULL`. Kiểu bọc (`Long`, `Double`) thì cho phép `NULL`.

### ⚠️ 2 điều dễ hiểu lầm

1. **`nullable = false` chỉ là ràng buộc ở DB, không phải kiểm tra form.** Bỏ trống thì lỗi chỉ xuất hiện lúc ghi DB, dưới dạng lỗi SQL khó đọc. Thêm nữa, chuỗi rỗng `""` **vẫn lọt qua**, vì `""` khác `NULL`. Muốn báo lỗi đẹp ngay từ form thì dùng Bean Validation (`@NotBlank`, `@Size`...) kết hợp `@Valid`.
2. **`ddl-auto: update` không sửa cột đã có.** Nó chỉ **thêm** bảng, thêm cột mới. Nếu cột `description` đã được tạo là `VARCHAR(255)` rồi bạn mới thêm `length = 2000`, thì cột trong DB **vẫn là 255** và nhập mô tả dài sẽ bị lỗi `Data too long for column`. Tương tự khi đổi `nullable`, thêm `unique` cho cột cũ. Có 2 cách sửa: xoá sạch rồi tạo lại (`docker compose down -v`), hoặc tự sửa bằng SQL: `ALTER TABLE product MODIFY description VARCHAR(2000);`. Kiểm tra cột thật bằng lệnh `DESCRIBE product;` trong MySQL.

---

## `catch (IOException e)` và `catch (Exception e)` khác nhau thế nào?

> Hỏi ở: [Bài 15 — Bước 4: `FileStorageService`: lưu và xoá file](study/15-anh-va-mo-ta-san-pham.md#bước-4--filestorageservice-lưu-và-xoá-file)

### Khác nhau ở chỗ "bắt rộng hay hẹp"

`catch (X e)` bắt lỗi kiểu `X` **và mọi lớp con của `X`**. Cây kế thừa của lỗi trong Java:

```
Throwable
├── Error                          ← lỗi JVM (hết RAM...), không nên bắt
└── Exception
    ├── IOException                ← lỗi đọc/ghi file, mạng   (checked)
    │   ├── FileNotFoundException
    │   └── NoSuchFileException ...
    └── RuntimeException           ← lỗi do code sai          (unchecked)
        ├── IllegalArgumentException
        ├── NullPointerException
        └── ...
```

| | `catch (IOException e)` | `catch (Exception e)` |
|---|---|---|
| Bắt được | Chỉ lỗi vào/ra (I/O): ổ đầy, không có quyền ghi, file bị khoá... | **Gần như mọi thứ**: I/O, `NullPointerException`, `IllegalArgumentException`, lỗi do chính code mình viết sai |
| Ý nghĩa khi đọc code | "Ở đây tôi biết có thể hỏng ổ đĩa, và tôi xử lý đúng việc đó" | "Có gì hỏng thì cứ gom hết vào đây" |
| Lỗi không lường trước | Không bị bắt → bay tiếp lên trên, dễ phát hiện | Bị **nuốt mất** hoặc bị gói thành thông báo sai |

### Checked exception: vì sao bắt buộc phải có `try/catch`?

`IOException` là **checked exception**. Hàm nào có thể ném nó thì phải khai báo `throws IOException` (ví dụ `Files.createDirectories`, `MultipartFile.transferTo`, `Files.deleteIfExists`). Khi gọi những hàm đó, **trình biên dịch bắt buộc** bạn phải xử lý: hoặc `try/catch`, hoặc tự khai báo `throws` tiếp ra ngoài. Đó là lý do trong `FileStorageService` có `try/catch` dù code trông rất đơn giản.

`RuntimeException` và các lớp con (như `IllegalArgumentException`) là **unchecked**: không bắt buộc bắt, trình biên dịch không nhắc gì.

Một khác biệt nhỏ khi biên dịch: nếu trong `try` **không có** dòng nào ném `IOException` mà vẫn viết `catch (IOException e)`, Java báo lỗi `exception IOException is never thrown in body of corresponding try statement`. Còn `catch (Exception e)` thì lúc nào viết cũng được, vì nó bao cả lỗi unchecked.

### Ví dụ cụ thể trong `FileStorageService`: bắt rộng làm hỏng gì?

Trong `save()`, hàm `resolve(filename)` nằm **bên trong** khối `try` và có thể ném `IllegalArgumentException` (khi tên file cố thoát ra ngoài thư mục upload):

```java
try {
    file.transferTo(resolve(filename));   // resolve() có thể ném IllegalArgumentException
} catch (Exception e) {                    // ← bắt luôn cả IllegalArgumentException
    throw new RuntimeException("Lưu file thất bại: " + filename, e);
}
```

- Với `catch (Exception e)`: lỗi "tên file không hợp lệ" bị đổi thành "Lưu file thất bại", tức là thông báo sai bản chất. Controller muốn `catch (IllegalArgumentException e)` để hiện chữ đỏ trên form ([Bài 15 — Bài tập 2](study/15-anh-va-mo-ta-san-pham.md#bài-tập)) sẽ **không bắt được nữa**, vì lỗi đã bị gói thành `RuntimeException`.
- Với `catch (IOException e)`: chỉ lỗi ghi đĩa mới bị gói lại, còn `IllegalArgumentException` vẫn giữ nguyên và bay lên controller.

Trong `delete()` còn tệ hơn, nếu viết:

```java
} catch (Exception e) {
    // không làm gì
}
```

thì **mọi lỗi đều biến mất không dấu vết**, kể cả lỗi chặn đường dẫn nguy hiểm `../../app.jar`. Cách của bài học là `catch (IOException e)` kèm `log.warn(...)`: chỉ bỏ qua lỗi xoá file (việc phụ, không nên làm hỏng việc chính), mà vẫn có log để biết.

### Quy tắc dễ nhớ

1. **Bắt hẹp nhất có thể**: bắt đúng loại lỗi mà bạn biết cách xử lý ở chỗ đó.
2. **Không bao giờ để `catch` rỗng.** Ít nhất phải ghi log.
3. `catch (Exception e)` chỉ hợp lý ở **tầng ngoài cùng**, nơi gom mọi lỗi lại để trả một trang lỗi chung, ghi log rồi trả về "500". Không nên dùng nó ở giữa các tầng logic.
4. Nếu cần bắt nhiều loại, viết riêng từng `catch`, hoặc gộp bằng dấu `|`: `catch (IOException | IllegalStateException e)`.

---

## Dấu `{}` trong `log.info(...)` / `log.warn(...)` là gì?

> Hỏi ở: [Bài 15 — Bước 4: `FileStorageService`: lưu và xoá file](study/15-anh-va-mo-ta-san-pham.md#bước-4--filestorageservice-lưu-và-xoá-file). Cùng kiểu viết này cũng có ở [Bài 14](study/14-moi-truong-mongodb-redis.md).

### `{}` là chỗ trống, được điền lần lượt bằng các tham số phía sau

`log` là logger của **SLF4J** (`org.slf4j.Logger`). Tham số đầu tiên là một **mẫu câu**. Mỗi `{}` trong mẫu câu được thay bằng các tham số phụ **theo đúng thứ tự**: `{}` thứ nhất nhận tham số thứ nhất, `{}` thứ hai nhận tham số thứ hai...

```java
log.warn("[Upload] Không xoá được file {}: {}", filename, e.getMessage());
//                                     ↑   ↑      ↑          ↑
//                                     └───┼──────┘          │
//                                         └─────────────────┘
// → [Upload] Không xoá được file 3f2a...jpg: Permission denied
```

Giá trị được đổi sang chuỗi bằng `toString()`. Nếu giá trị là `null` thì in ra chữ `null`, không bị lỗi.

### Khi số `{}` và số tham số không khớp nhau

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

### `e.getMessage()` và `e`: khác nhau thế nào?

```java
log.warn("Không xoá được file {}: {}", filename, e.getMessage());  // 1 dòng, chỉ có câu báo lỗi
log.warn("Không xoá được file {}", filename, e);                   // câu báo lỗi + toàn bộ stack trace
```

- Dùng `e.getMessage()` khi lỗi đã được lường trước và chỉ cần biết ngắn gọn (như xoá file thất bại).
- Truyền `e` vào cuối khi cần biết lỗi xảy ra **ở dòng code nào**, để gỡ lỗi.

### Vì sao không nối chuỗi bằng `+` cho nhanh?

```java
log.debug("Sản phẩm: " + product);   // luôn nối chuỗi, kể cả khi mức DEBUG đang tắt
log.debug("Sản phẩm: {}", product);  // chỉ ghép chuỗi khi mức DEBUG đang bật
```

Logger có các mức `trace < debug < info < warn < error`. Mức nào bị tắt trong cấu hình thì dòng log ở mức đó không được in ra. Khi dùng `{}`, việc ghép chuỗi chỉ thực sự diễn ra lúc dòng log **sắp được in**. Còn với `+`, chuỗi luôn được ghép trước, kể cả khi sau đó dòng log bị bỏ đi, tức là làm tốn công vô ích. Thêm nữa, mẫu câu có `{}` cũng dễ đọc hơn.

Lưu ý: bản thân các tham số **vẫn được tính trước** khi gọi `log`. Ví dụ `mongoTemplate.getDb().getName()` vẫn chạy dù log có bị tắt hay không. Chỉ riêng bước ghép chuỗi là được hoãn lại.

### Đừng nhầm với `String.format`

| | SLF4J `log.info(...)` | `String.format(...)` / `printf` |
|---|---|---|
| Chỗ trống | `{}` (không quan tâm kiểu dữ liệu) | `%s`, `%d`, `%.2f`... (phải đúng kiểu) |
| Ví dụ | `log.info("Giá: {}", price)` | `String.format("Giá: %.2f", price)` |

Viết `%s` trong `log.info` thì SLF4J không hiểu, nên sẽ in nguyên chữ `%s`.

---

## Java có giá trị mặc định cho tham số (`String description = ""`) không? Nạp chồng (overload) là gì?

> Hỏi ở: [Bài 13 — Bước 3: Danh mục và sản phẩm mẫu](study/13-du-lieu-mau-seeder.md#bước-3--danh-mục-và-sản-phẩm-mẫu) (khi muốn `createProduct()` nhận thêm `description` từ [Bài 15](study/15-anh-va-mo-ta-san-pham.md)).
> Phần "Tìm hiểu kỹ hơn về nạp chồng" được hỏi thêm ở [Bài 16 — Bước 2](study/16-luot-xem-top-san-pham-redis.md#bước-2--mỗi-module-một-file-properties-đọc-qua-package-common), khi so sánh với [`@Override`](#override-là-gì-bỏ-đi-có-sao-không-khác-gì-nạp-chồng-overloading).

### Không. Java **không có** default parameter

Kiểu viết dưới đây có trong PHP, Python, JavaScript, Kotlin... nhưng **Java báo lỗi biên dịch**:

```java
// ❌ Sai cú pháp trong Java
private void createProduct(String name, double price, Category category, String description = "") { ... }
```

### Cách làm trong Java: **nạp chồng hàm (overloading)**

Viết **2 hàm cùng tên, khác số tham số**. Hàm ngắn gọi sang hàm đầy đủ và tự điền giá trị mặc định:

```java
// Hàm ngắn: không truyền description → mặc định là chuỗi rỗng
private void createProduct(String name, double price, Category category) {
    createProduct(name, price, category, "");
}

// Hàm đầy đủ: làm việc thật
private void createProduct(String name, double price, Category category, String description) {
    Product product = new Product();
    product.setName(name);
    product.setPrice(price);
    product.setCategory(category);
    product.setDescription(description);
    productRepository.save(product);
}
```

Lúc gọi, Java tự chọn hàm theo **số lượng và kiểu** tham số truyền vào:

```java
createProduct("Logitech G102", 390000, mouse);                        // → hàm 3 tham số, description = ""
createProduct("iPhone 15", 22990000, phone, "Chip A16, camera 48MP");  // → hàm 4 tham số
```

Quy tắc: các hàm overload phải **khác nhau ở danh sách tham số** (số lượng hoặc kiểu). Chỉ khác kiểu trả về thì không được.

### Tìm hiểu kỹ hơn về nạp chồng (overload)

#### 1. "Chữ ký" của method: Java phân biệt các hàm cùng tên bằng gì?

**Chữ ký (signature)** của method gồm **tên + danh sách kiểu tham số** (theo đúng thứ tự). Trong một class, không được có 2 method trùng chữ ký. Nạp chồng chính là viết nhiều method **cùng tên nhưng khác chữ ký**.

Những thứ **không** thuộc chữ ký nên không dùng để phân biệt được: tên tham số, kiểu trả về, `public`/`private`, `static`, `throws`.

```java
void find(long id)                 { ... }
void find(String name)             { ... }   // ✅ khác kiểu
void find(String name, int page)   { ... }   // ✅ khác số lượng
void find(int page, String name)   { ... }   // ✅ khác thứ tự kiểu (hợp lệ nhưng dễ gọi nhầm, nên tránh)

void find(long productId)          { ... }   // ❌ chỉ khác tên tham số → trùng với find(long id)
Product find(long id)              { ... }   // ❌ chỉ khác kiểu trả về
```

Vì sao chỉ khác kiểu trả về lại không được? Vì Java cho phép gọi method mà bỏ qua giá trị trả về. Khi viết `find(5);` thì Java không có cách nào biết bạn muốn gọi bản trả về `void` hay bản trả về `Product`.

#### 2. Java chọn hàm nào **lúc biên dịch**, dựa vào kiểu **khai báo** của đối số

Đây là khác biệt lớn nhất với ghi đè:

- **Nạp chồng**: trình biên dịch chọn hàm ngay lúc biên dịch, dựa vào **kiểu của biến được truyền vào** như nó được khai báo trong code.
- **Ghi đè**: chương trình chọn hàm **lúc chạy**, dựa vào object thật sự là của class nào.

```java
void print(Object o) { System.out.println("Object"); }
void print(String s) { System.out.println("String"); }

String a = "hello";
Object b = "hello";      // object thật là String, nhưng biến khai báo kiểu Object

print(a);   // → "String"
print(b);   // → "Object"   ← trình biên dịch chỉ nhìn thấy kiểu Object
```

#### 3. Không có hàm khớp chính xác thì Java chọn thế nào?

Java thử lần lượt 3 vòng. Vòng nào tìm được hàm phù hợp thì dừng ở đó:

| Vòng | Java thử gì | Ví dụ |
|---|---|---|
| 1 | Khớp chính xác, hoặc **nới rộng kiểu số** (`int` → `long` → `float` → `double`) | `createProduct("Logitech G102", 390000, mouse)`: `390000` là `int`, tự nới thành `double price` |
| 2 | **Tự đóng hộp / mở hộp** (`int` ↔ `Integer`, `long` ↔ `Long`...) | `foo(5)` gọi được `foo(Integer x)` |
| 3 | **Varargs** (`String... args`) | `foo("a", "b")` gọi được `foo(String... xs)` |

Vì vòng 1 được ưu tiên trước vòng 2, nếu có cả `foo(long x)` và `foo(Integer x)` thì `foo(5)` sẽ chọn **`foo(long)`**. Kết quả này thường làm người mới bất ngờ.

Nếu trong cùng một vòng có **2 hàm phù hợp ngang nhau** thì trình biên dịch báo lỗi *"reference to ... is ambiguous"* (gọi mơ hồ). Ví dụ hay gặp nhất là truyền `null`:

```java
void show(String s)  { ... }
void show(Integer i) { ... }

show(null);            // ❌ ambiguous: null hợp với cả String lẫn Integer
show((String) null);   // ✅ ép kiểu để chỉ rõ
```

#### 4. Nạp chồng hay gặp trong thư viện

Bạn đã dùng nạp chồng rất nhiều mà có thể chưa để ý:

| Gọi | Các bản nạp chồng |
|---|---|
| `System.out.println(...)` | `println()`, `println(int)`, `println(double)`, `println(String)`, `println(Object)`... |
| `log.info(...)` (SLF4J) | `info(String)`, `info(String, Object)`, `info(String, Object, Object)`, `info(String, Object...)` |
| `Optional.orElseThrow(...)` (trong `UserService`) | `orElseThrow()` (ném lỗi mặc định), `orElseThrow(() -> new ...)` (ném lỗi do mình chọn) |
| `String.valueOf(...)` | `valueOf(int)`, `valueOf(double)`, `valueOf(boolean)`, `valueOf(Object)`... |

**Constructor** cũng nạp chồng được. Constructor ngắn gọi sang constructor đầy đủ bằng `this(...)`, cùng ý tưởng với `createProduct` ở trên:

```java
public Product() {}                                     // JPA bắt buộc phải có constructor rỗng
public Product(String name, double price) {
    this(name, price, "");                              // gọi sang constructor 3 tham số
}
public Product(String name, double price, String description) {
    this.name = name;
    this.price = price;
    this.description = description;
}
```

(Các entity trong project hiện chỉ có constructor rỗng. Đoạn trên chỉ để minh hoạ.)

#### 5. Bẫy: định ghi đè nhưng lại thành nạp chồng

Muốn ghi đè `equals` của `Object` mà viết sai kiểu tham số:

```java
public boolean equals(Product other) { ... }   // ❌ đây là NẠP CHỒNG, không phải ghi đè
```

`Object.equals` nhận `Object`, không phải `Product`. Vì vậy method trên là một hàm mới. Các thư viện như `List.contains`, `HashSet` vẫn gọi `equals(Object)` cũ, và code của bạn không bao giờ được chạy. Thêm `@Override` vào thì trình biên dịch báo lỗi ngay. Đây là thêm một lý do để [luôn ghi `@Override`](#override-là-gì-bỏ-đi-có-sao-không-khác-gì-nạp-chồng-overloading).

#### 6. Khi nào nên nạp chồng

- ✅ Các bản nạp chồng **làm cùng một việc**, chỉ khác cách truyền dữ liệu vào. Bản ngắn gọi sang bản đầy đủ, như `createProduct`.
- ✅ Dùng thay cho giá trị mặc định của tham số (Java không có tính năng này).
- ❌ Hai việc khác nhau thì đặt **tên khác nhau**, ví dụ `findById(long)` và `findByName(String)` thay vì cùng tên `find`. Spring Data JPA cũng đặt tên theo kiểu này.
- ❌ Tránh các bản nạp chồng chỉ khác thứ tự tham số, hoặc chỉ khác `int`/`Integer`/`long`. Người đọc code rất khó đoán hàm nào sẽ được gọi.

### Nhân tiện: `''` và `""` khác nhau trong Java

| | `'a'` (nháy đơn) | `"abc"` (nháy kép) |
|---|---|---|
| Kiểu | `char`, đúng **1 ký tự** | `String`, chuỗi bao nhiêu ký tự cũng được |
| Rỗng | `''` ❌ lỗi, `char` không thể rỗng | `""` ✅ chuỗi rỗng |

Nên giá trị mặc định cho `String` phải là `""`, không phải `''` (khác với PHP/JS, nơi hai loại nháy dùng thay nhau được).

### Các cách khác (để biết, project này chưa dùng)

- **Truyền `null` rồi xử lý bên trong**: `product.setDescription(description != null ? description : "")`. Gọn nhưng người gọi phải nhớ viết `null`.
- **Giá trị mặc định ngay trong entity**: `private String description = "";` trong `Product`. Khi đó không gọi `setDescription()` thì trường vẫn là `""`.
- Với tham số **request** (`@RequestParam`) thì Spring có sẵn: `@RequestParam(defaultValue = "1") int page`. Với **cấu hình** (`@Value`) thì dùng dấu `:`: `@Value("${app.seed-sample-data:true}")`. Hai cái này là tính năng của Spring, không phải của ngôn ngữ Java.

---

## `return a() && b() && c();` hoạt động thế nào? (toán tử `&&` và cơ chế "đoản mạch")

> Hỏi ở: câu hỏi ngoài giáo trình (đoạn code mẫu trên mạng). Liên hệ với [Bài 02 — Bước 3: Service](study/02-thuc-hanh-crud.md#bước-3--service-chứa-logic-nghiệp-vụ), nơi có `product.getPrice() != null && product.getPrice() < 0`.

Đoạn code mẫu:

```java
return service1.function1(data)
    && service2.function2(data)
    && service3.function3(data);
```

(Trên mạng hay viết `$var`, đó là kiểu PHP. Trong Java tên biến không có `$`.)

### 1. Đây không phải cú pháp `return` đặc biệt nào cả

`return` chỉ trả về **giá trị của một biểu thức**. Biểu thức ở đây là `a && b && c`, và ngắt dòng chỉ để dễ đọc. Viết dài ra sẽ là:

```java
boolean ketQua = service1.function1(data) && service2.function2(data) && service3.function3(data);
return ketQua;
```

Do đó: cả 3 hàm **phải trả về `boolean`**, và hàm chứa dòng `return` này cũng phải có kiểu trả về `boolean`.

### 2. `&&` tính từ trái sang phải và **dừng ngay khi gặp `false`** (short-circuit, "đoản mạch")

`A && B` chỉ `true` khi cả hai đều `true`. Nên nếu `A` đã `false` thì kết quả chắc chắn là `false`, Java **không gọi `B` nữa**.

| `function1` | `function2` | `function3` | Kết quả | Những hàm **thực sự được gọi** |
|---|---|---|---|---|
| `false` | (bỏ qua) | (bỏ qua) | `false` | chỉ 1 |
| `true` | `false` | (bỏ qua) | `false` | 1, 2 |
| `true` | `true` | `false` | `false` | 1, 2, 3 |
| `true` | `true` | `true` | `true` | 1, 2, 3 |

Tương đương với:

```java
if (!service1.function1(data)) return false;
if (!service2.function2(data)) return false;
return service3.function3(data);
```

Kiểu viết này hợp với **chuỗi kiểm tra**: "qua bước 1 mới kiểm tra bước 2...". Ví dụ `return isLoggedIn(user) && hasPermission(user) && isNotLocked(user);`

### 3. Ví dụ ngay trong project: chặn NullPointerException

```java
if (product.getPrice() != null && product.getPrice() < 0) { ... }
```

Nếu giá là `null`, vế trái `false` nên vế phải `product.getPrice() < 0` **không bao giờ chạy**. Nếu không có đoản mạch, so sánh `null < 0` sẽ ném `NullPointerException`. Vì vậy **thứ tự các vế quan trọng**: vế "bảo vệ" phải đứng trước.

### 4. `||` cũng đoản mạch, nhưng ngược lại

`A || B` **dừng ngay khi gặp `true`**, vì chỉ cần một vế đúng là đủ. Ví dụ `return isAdmin(user) || isOwner(user, product);`: là admin thì không cần kiểm tra chủ sở hữu.

Còn `&` và `|` (một ký tự) khi dùng với `boolean` thì **không đoản mạch**: luôn tính hết tất cả các vế. Hiếm khi cần dùng.

### 5. Những bẫy cần nhớ

- **Hàm có tác dụng phụ có thể bị bỏ qua.** Nếu `function2` có ghi DB, gửi mail hay xoá file, mà `function1` trả `false`, thì `function2` không chạy. Chỉ nên xâu chuỗi những hàm **chỉ kiểm tra** (trả lời đúng/sai, không thay đổi gì).
- **Không biết bước nào hỏng.** Kết quả chỉ là `false`, không có lý do. Nếu cần báo lỗi cụ thể cho người dùng, viết từng `if` riêng và ném exception kèm thông báo, như `ProductService` đang làm (`throw new IllegalArgumentException("Giá không được âm")`).
- **Thứ tự cũng ảnh hưởng tốc độ.** Nên đặt kiểm tra rẻ (so sánh biến) trước, kiểm tra đắt (gọi DB, gọi API) sau, để hay được dừng sớm.

---

## Đưa hằng số (ví dụ `RECENT_TTL` 30 ngày) sang `application.yml` được không? `@Value` có đọc được `Duration` không?

> Hỏi ở: [Bài 16 — Bước 3: `ProductStatsService`](study/16-luot-xem-top-san-pham-redis.md#bước-3--productstatsservice-mọi-thao-tác-với-redis), dòng `private static final Duration RECENT_TTL = Duration.ofDays(30);`

### Được. Và `@Value` đọc thẳng ra `Duration` luôn

Làm giống hệt cách bài 15 đọc `app.upload.dir` (xem [Bài 15 — Bước 1](study/15-anh-va-mo-ta-san-pham.md#bước-1--cấu-hình-trong-applicationyml)), chỉ khác là kiểu tham số là `Duration` thay vì `String`.

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

> Bài 16 không dùng `@Value` cho chỗ này. Bài đưa các giá trị vào file `config/stats.properties`, và đọc qua class dùng chung `common/AppConfig` (`config.getDuration("app.stats.recent-ttl")`), xem [Bước 2](study/16-luot-xem-top-san-pham-redis.md#bước-2--mỗi-module-một-file-properties-đọc-qua-package-common). `AppConfig` đọc từ `Environment` của Spring, tức là cùng cơ chế với `@Value`, nên chuỗi `30d` vẫn được đổi thành `Duration` y như trên.

### Vì sao phải bỏ `static final`?

- `@Value` **không gán được vào biến `static`**. Spring tiêm giá trị vào **từng object** (bean), còn biến `static` thuộc về class, nên Spring không đụng tới.
- Vì không còn là hằng số, tên biến đổi từ kiểu `HOA_GẠCH_DƯỚI` (quy ước cho hằng số) sang `camelCase` (quy ước cho field thường): `RECENT_TTL` → `recentTtl`.
- Vẫn giữ `final`: giá trị gán một lần trong constructor rồi không đổi nữa, giống `redisTemplate`.

### Cách viết thời gian trong `application.yml`

| Viết | Nghĩa |
|---|---|
| `500ms` | 500 mili giây |
| `2s` | 2 giây |
| `10m` | 10 phút |
| `12h` | 12 giờ |
| `30d` | 30 ngày |
| `P30D` | 30 ngày, viết theo chuẩn ISO-8601 (ít dùng, khó đọc hơn) |

> ⚠️ **Bẫy:** viết số trơn `30`, không có đơn vị, thì Spring hiểu là **30 mili giây**, không phải 30 ngày. Key sẽ hết hạn gần như ngay lập tức và "Bạn vừa xem" luôn trống. Luôn ghi kèm đơn vị.

### Nên đưa cái gì ra cấu hình, cái gì để là hằng số?

| Đưa ra `application.yml` | Để là hằng số trong code |
|---|---|
| Con số **có thể muốn chỉnh** mà không sửa code: thời hạn (`recent-ttl`), số lượng (`RECENT_LIMIT` = 5 cũng đưa ra được, kiểu `int`), đường dẫn thư mục... | Thứ gắn chặt với **dữ liệu đã lưu**: tên key Redis `demo:product:views`, `demo:recent:`. Đổi tên key là dữ liệu cũ bị "mồ côi", code không tìm thấy nữa |
| Giá trị **khác nhau giữa các môi trường** (dev muốn TTL `5m` để thử cho nhanh, chạy thật thì `30d`) | Thứ thuộc về logic, đổi là phải sửa code đi kèm |

Muốn đổi theo môi trường mà không sửa file, dùng lại cú pháp biến môi trường quen thuộc: `recent-ttl: ${RECENT_TTL:30d}`.

### Lưu ý khi đổi giá trị

Đổi `recent-ttl` rồi restart app thì **chỉ lần xem tiếp theo** mới dùng hạn mới, vì `expire` được gọi lại mỗi lần xem. Các key đã có trong Redis vẫn giữ hạn cũ cho tới khi user xem thêm sản phẩm. Kiểm tra bằng `TTL demo:recent:admin` trong `redis-cli`.

---

## Mỗi module một class cấu hình riêng (`StatsProperties`, `ProductProperties`) được không? `@ConfigurationProperties` là gì?

> Hỏi ở: [Bài 16 — Bước 3: `ProductStatsService`](study/16-luot-xem-top-san-pham-redis.md#bước-3--productstatsservice-mọi-thao-tác-với-redis), câu hỏi nối tiếp mục [Đưa hằng số sang `application.yml`](#đưa-hằng-số-ví-dụ-recent_ttl-30-ngày-sang-applicationyml-được-không-value-có-đọc-được-duration-không). Ý tưởng ban đầu: `product/product_config.java`, `stats/stat_config.java`. Sau khi bàn tiếp, bài 16 **không** dùng cách này. Bài chọn mỗi module một file `.properties` và một hàm đọc dùng chung `common/AppConfig`, xem [Bước 2](study/16-luot-xem-top-san-pham-redis.md#bước-2--mỗi-module-một-file-properties-đọc-qua-package-common) và mục [file `.ini` của PHP](#java-có-file-cấu-hình-kiểu-ini-của-php-key--value-không-có-cần-tự-viết-hàm-đọc-chung-không) bên dưới. Mục này giữ lại để biết thêm một cách làm phổ biến của Spring Boot.

### Được, và đây là cách Spring Boot khuyên dùng khi một module có nhiều cấu hình

Dùng `@ConfigurationProperties`: một class Java gom **cả nhóm** cấu hình cùng tiền tố (`app.stats.*`) thành các field. Thay vì rải `@Value("${...}")` khắp nơi, service chỉ cần nhận một object.

### Đặt tên: `StatsProperties`, không phải `stat_config.java`

- Tên class Java viết **PascalCase** và tên file phải trùng tên class. `stat_config` (snake_case) chạy được nhưng sai quy ước.
- Trong Spring, hậu tố **`Config`** thường dành cho class `@Configuration` (class *tạo bean*, như `SecurityConfig`, `WebConfig` trong `config/`). Class *chứa giá trị cấu hình* thì quy ước đặt hậu tố **`Properties`**. Nên đặt `StatsProperties`, `ProductProperties` để khỏi nhầm hai loại.
- Đặt class trong **chính package của module** (`stats/StatsProperties.java`) là hợp với kiểu tổ chức theo feature của project.

### Ví dụ: `stats/StatsProperties.java`

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

### `@Value` hay `@ConfigurationProperties`?

| | `@Value("${app.stats.recent-ttl}")` | `@ConfigurationProperties(prefix = "app.stats")` |
|---|---|---|
| Hợp khi | Chỉ 1–2 giá trị lẻ | Một nhóm nhiều giá trị cùng tiền tố |
| Viết thêm | Không cần class mới | Thêm 1 class + getter/setter |
| Giá trị mặc định | `${app.x:30d}` (sau dấu `:`) | Gán ngay ở field |
| Gõ sai tên key | Lỗi lúc khởi động (`Could not resolve placeholder`) | **Không báo lỗi**, field giữ giá trị mặc định ⚠️ |
| Tên trong yml | Nên gõ đúng chuỗi như trong code | Linh hoạt: `recent-ttl` hay `recentTtl` trong yml đều được, biến môi trường `APP_STATS_RECENTTTL` cũng tự khớp |

Project hiện tại mỗi module chỉ có 1 cấu hình nên `@Value` là đủ. Khi `stats` có từ 2 giá trị trở lên (TTL + giới hạn) thì chuyển sang `StatsProperties` sẽ gọn hơn.

> Mẹo: thêm `annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'` vào `build.gradle` thì VSCode sẽ **hết gạch vàng "Unknown property"** ở các dòng `app.*`, và còn gợi ý tên key khi gõ. Sau khi thêm phải build lại (`./gradlew build`) thì VSCode mới nhận.

### Còn muốn tách cả file `.yml` theo module?

Cũng được: tạo `src/main/resources/stats.yml`, rồi trong `application.yml` thêm `spring.config.import: classpath:stats.yml`. (Bài 16 tách theo một cách khác: file `.properties` trong `config/`, tự nạp bằng `ModuleConfigLoader`, không phải khai báo từng file.) Nhưng project nhỏ thì nên để chung một file. Tách ra nhiều file thì lúc tìm một cấu hình phải mở nhiều chỗ.

---

## Java có file cấu hình kiểu `.ini` của PHP (`key = "value"`) không? Có cần tự viết hàm đọc chung không?

> Hỏi ở: [Bài 16 — Bước 2: Mỗi module một file `.properties`](study/16-luot-xem-top-san-pham-redis.md#bước-2--mỗi-module-một-file-properties-đọc-qua-package-common). Người hỏi từng dùng file `.ini` bên PHP và một hàm đọc cấu hình dùng chung. Câu hỏi này dẫn tới việc viết lại Bước 2 của bài 16.

### Có: file `.properties`, và Spring Boot đọc sẵn nó

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

### "Hàm đọc cấu hình dùng chung": Spring đã làm sẵn, không cần tự viết

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

### Muốn mỗi module một file riêng (giống `stats.ini`)?

Làm được: tạo `src/main/resources/config/stats.properties`, rồi khai báo trong `application.yml`:

```yaml
spring:
  config:
    import: classpath:config/stats.properties   # nhiều file thì ngăn cách bằng dấu phẩy
```

`StatsProperties` vẫn giữ nguyên, vì key vẫn là `app.stats.*`. Có điều với project nhỏ thì nên để chung một file: tìm cấu hình chỉ cần mở một chỗ.

### ⚠️ Bẫy khi chuyển từ `.ini` sang `.properties` (đã chạy thử)

| Viết | `.ini` (PHP) hiểu là | `.properties` (Java) hiểu là |
|---|---|---|
| `key = "30d"` | `30d` (bỏ dấu nháy) | `"30d"`, **giữ nguyên cả dấu nháy**. Đọc thành `Duration` sẽ lỗi. Không viết nháy |
| `key = abc   ` (thừa dấu cách ở cuối) | `abc` | `abc   `: dấu cách ở **đầu** giá trị bị bỏ, dấu cách ở **cuối** thì giữ lại |
| `[stats]` | Tên nhóm | Không có khái niệm nhóm. Phải viết tiền tố đầy đủ `app.stats.` ở mọi dòng |
| `app.msg=Xin chào` | `Xin chào` | `Xin chÃ o`: **lỗi font tiếng Việt**. Spring Boot đọc `.properties` theo bảng mã ISO-8859-1, không phải UTF-8. Còn `.yml` thì đọc theo UTF-8 nên không bị |

Comment tiếng Việt (dòng bắt đầu bằng `#`) thì không sao, vì comment bị bỏ qua. Chỉ **giá trị** có tiếng Việt mới bị lỗi font.

### Project này làm thế nào (bài 16)

[Bài 16 Bước 2](study/16-luot-xem-top-san-pham-redis.md#bước-2--mỗi-module-một-file-properties-đọc-qua-package-common) làm đúng theo kiểu `.ini` + hàm đọc chung:

- **`application.yml`** chỉ giữ cấu hình **của Spring** (DB, Redis, log...).
- **Mỗi module một file** `src/main/resources/config/<module>.properties` (`stats`, `product`, `upload`), chỉ gồm các dòng `key=value`.
- **`common/ModuleConfigLoader`** (một `EnvironmentPostProcessor`) tự nạp **mọi** file `config/*.properties` lúc khởi động, và đọc theo **UTF-8**, nên tránh được lỗi font tiếng Việt ở bảng trên.
- **`common/AppConfig`** là hàm đọc dùng chung: `config.getInt("app.stats.top-limit")`, `config.getDuration(...)`. Thiếu key thì app báo lỗi lúc khởi động.
- Thêm module mới: tạo file `config/<module>.properties` + `import com.example.demo.common.AppConfig`. Không phải sửa gì khác.
- **Đừng để cả `application.properties` lẫn `application.yml` cùng lúc.** Spring đọc cả hai, và nếu trùng key thì `.properties` thắng. Khi đó sửa yml mà giá trị không đổi, rất khó tìm ra lý do. (Các file trong `config/` của bài 16 thì khác: chúng có tên riêng và được ưu tiên **thấp hơn** `application.yml`.)

---

## Mỗi class đều phải tự khai báo logger. Có tạo một logger "common" rồi import vào được không?

> Hỏi ở: [Bài 16](study/16-luot-xem-top-san-pham-redis.md), khi thấy file nào cũng có dòng `private static final Logger log = LoggerFactory.getLogger(NoSqlConnectionCheck.class);`. Phần thực hành ở [**bài 17**](study/17-lombok-slf4j-getter-setter.md).

### Vì sao mỗi class lại có logger riêng?

Tham số `NoSqlConnectionCheck.class` là **tên của logger**, và nó được in ra trên từng dòng log:

```
INFO ... c.e.demo.nosql.NoSqlConnectionCheck : [NoSQL] MongoDB OK, database = ...
INFO ... c.e.demo.product.ProductService     : ...
```

Nhờ tên này mà bạn biết dòng log đến từ class nào. Cấu hình `logging.level.com.example.demo: DEBUG` trong `application.yml` cũng dựa vào tên đó để bật/tắt log theo package.

### Tạo một logger chung trong `common/` được không?

Viết được, nhưng **không nên**:

```java
// ❌ common/Log.java
public class Log {
    public static final Logger log = LoggerFactory.getLogger("app");
}
```

Mọi dòng log khi đó đều mang tên `app`, nên không còn biết dòng nào của class nào, và cũng không bật/tắt theo package được nữa. Java không có cách nào "import" một biến mà biến đó **tự biết** nó đang được dùng trong class nào. Vì vậy mỗi class phải có một dòng khai báo riêng, hoặc để công cụ sinh hộ dòng đó.

### Các cách làm gọn

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

Để dùng được, phải thêm Lombok vào `build.gradle` (2 dòng `compileOnly` và `annotationProcessor`). Chi tiết ở [bài 17](study/17-lombok-slf4j-getter-setter.md#bước-1--buildgradle-thêm-lombok).

> Hiện project không có lỗi copy-paste nào: kiểm tra ngày 2026-11-04 thì cả 5 file khai báo logger đều ghi đúng tên class của chính nó.

---

## `@Override` là gì? Bỏ đi có sao không? Khác gì nạp chồng (overloading)?

> Hỏi ở: [Bài 16 — Bước 2, mục ③ `ModuleConfigLoader`](study/16-luot-xem-top-san-pham-redis.md#bước-2--mỗi-module-một-file-properties-đọc-qua-package-common) (gõ nhầm `postProcessEnviroment`, thiếu chữ `n`, và bị báo 2 lỗi)

### Ghi đè (override) là gì?

Khi một class `implements` một interface hoặc `extends` một class cha, nó có thể **viết lại** một method mà cha đã khai báo. Lúc chạy, Spring (hay bất kỳ ai) gọi method đó qua kiểu cha thì phiên bản **của class con** sẽ được chạy. Đây gọi là **ghi đè**.

```java
public class ModuleConfigLoader implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) { ... }
}
```

Spring không biết class `ModuleConfigLoader`. Nó chỉ biết "đây là một `EnvironmentPostProcessor`" và gọi `postProcessEnvironment(...)`. Nhờ ghi đè, code của mình được chạy.

### `@Override` chỉ là lời hứa để trình biên dịch kiểm tra

`@Override` **không làm thay đổi cách chạy**. Nó chỉ nói với trình biên dịch: *"method này ghi đè một method của cha, hãy kiểm tra giúp tôi"*. Nếu cha **không có** method nào trùng khớp thì trình biên dịch báo lỗi ngay:

```
The method postProcessEnviroment(...) must override or implement a supertype method
```

"Trùng khớp" nghĩa là:

- **Cùng tên**, phân biệt hoa thường (`postProcessEnvironment` khác `postProcessEnviroment`).
- **Cùng danh sách kiểu tham số**, cùng thứ tự. Tên tham số thì đặt tuỳ ý.
- Kiểu trả về giống hệt cha, hoặc là kiểu con của nó.
- Phạm vi truy cập không được hẹp hơn cha. Method của interface luôn là `public`, nên khi triển khai cũng phải ghi `public`.

### Bỏ `@Override` đi thì sao?

Code **vẫn chạy được** nếu tên và tham số đều đúng. Nhưng gõ sai thì `@Override` là thứ duy nhất phát hiện ra lỗi. Có hai trường hợp:

| Trường hợp | Có `@Override` | Không có `@Override` |
|---|---|---|
| Method cha là **abstract** (bắt buộc phải viết), ví dụ `postProcessEnvironment` | Báo lỗi ngay ở dòng gõ sai | Vẫn báo lỗi *"must implement the inherited abstract method"*, nhưng ở dòng khai báo class, khó nhận ra vì sao |
| Method cha **đã có sẵn thân hàm** (`default` trong interface, hoặc method thường của class cha), ví dụ `addResourceHandlers` của `WebMvcConfigurer` | Báo lỗi ngay | **Không báo gì cả.** Method gõ sai trở thành một method mới không ai gọi. App vẫn chạy, nhưng `/uploads/**` không hiện ảnh, rất khó tìm ra nguyên nhân |

Vì vậy: **mỗi khi ghi đè thì luôn ghi `@Override`.** Đó là lý do lỗi gõ thiếu chữ `n` bị báo **2 lỗi cùng lúc**: một lỗi do `@Override` (không có method cha nào tên như vậy), một lỗi do `implements` (method bắt buộc vẫn chưa được viết).

### Các chỗ dùng `@Override` trong project

| File | Ghi đè method nào | Của ai |
|---|---|---|
| `user/UserService` | `loadUserByUsername` | `UserDetailsService` (Spring Security gọi khi đăng nhập) |
| `seed/DataSeeder`, `nosql/NoSqlConnectionCheck` | `run(String... args)` | `CommandLineRunner` (Spring gọi sau khi app khởi động) |
| `config/WebConfig` | `addResourceHandlers` | `WebMvcConfigurer` (method `default`, xem bảng trên) |
| `common/ModuleConfigLoader` | `postProcessEnvironment` | `EnvironmentPostProcessor` (Spring gọi trước khi tạo bean) |

Ngoài ra còn hay gặp ghi đè `toString()`, `equals()`, `hashCode()` của class `Object` (mọi class Java đều ngầm `extends Object`).

### Ghi đè (override) khác nạp chồng (overload)

Hai khái niệm này tên gần giống nhau nhưng khác hẳn nhau. Nạp chồng được giải thích kỹ ở mục [Nạp chồng (overload) là gì?](#java-có-giá-trị-mặc-định-cho-tham-số-string-description---không-nạp-chồng-overload-là-gì).

| | Ghi đè (override) | Nạp chồng (overload) |
|---|---|---|
| Ở đâu | Class con viết lại method **của cha** | Nhiều method **trong cùng một class** |
| Tên | Giống hệt | Giống hệt |
| Tham số | **Phải giống** cha | **Phải khác** nhau (số lượng hoặc kiểu) |
| Dùng `@Override`? | Có | Không. Ghi vào sẽ bị báo lỗi |
| Ví dụ | `run(String... args)` trong `DataSeeder` | 2 hàm `createProduct(...)`, 3 và 4 tham số |

---

## Kiểu dữ liệu viết thường (`int`, `long`) và viết hoa (`Integer`, `Long`, `String`, `Duration`) khác nhau thế nào?

> Hỏi ở: [Bài 16 — Bước 3: `ProductStatsService`](study/16-luot-xem-top-san-pham-redis.md#bước-3--productstatsservice-mọi-thao-tác-với-redis) (thấy `private final Duration recentTtl;` nhưng `private final int recentLimit;`)

### Viết hoa hay viết thường **có ý nghĩa**, không phải tuỳ thích

Java chia kiểu dữ liệu thành 2 nhóm, và chữ cái đầu cho biết ngay nó thuộc nhóm nào:

| | Kiểu nguyên thuỷ (primitive) | Kiểu tham chiếu (class / object) |
|---|---|---|
| Cách viết | **viết thường** | **Viết Hoa** chữ đầu |
| Có những kiểu nào | Đúng 8 kiểu cố định: `byte`, `short`, `int`, `long`, `float`, `double`, `boolean`, `char` | Mọi thứ còn lại: `String`, `Duration`, `Integer`, `Long`, `Double`, `List`, `Product`, `Category`... |
| Biến chứa gì | Chính **giá trị** (số `5`, `true`) | Một **tham chiếu** (địa chỉ) trỏ tới object nằm ở chỗ khác trong bộ nhớ |
| Được là `null` không? | **Không.** Field chưa gán thì tự là `0` / `false` | **Được.** Field chưa gán thì là `null` |
| Có method không? | Không (`5.length()` là sai) | Có (`name.length()`, `recentTtl.toDays()`) |
| Ai định nghĩa | Có sẵn trong ngôn ngữ, là từ khoá | Là một class, do Java, thư viện hoặc chính mình viết |

Viết hoa là vì **quy ước đặt tên** của Java: tên class luôn viết hoa chữ đầu (`ProductStatsService`, `String`), còn tên biến và từ khoá viết thường. Nên chỉ cần nhìn chữ đầu là biết "đây là object" hay "đây là giá trị thô". Java **phân biệt hoa thường**: `long` và `Long` là hai kiểu khác nhau, không phải gõ sao cũng được.

> `String` viết hoa vì nó là class, không phải kiểu nguyên thuỷ, dù dùng hằng ngày như kiểu cơ bản. Java **không có** kiểu `string` viết thường.

### Ví dụ trong `ProductStatsService`

```java
private static final String VIEWS_KEY = "demo:product:views"; // String: class
private final StringRedisTemplate redisTemplate;              // class của Spring
private final Duration recentTtl;                             // class java.time.Duration
private final int recentLimit;                                 // nguyên thuỷ: chỉ là một con số
private final int topLimit;
```

- `Duration` phải viết hoa vì nó là class, không có phiên bản viết thường nào. Nó cần là object vì có method `toDays()`, `toSeconds()`...
- `recentLimit` chỉ là một con số, luôn phải có giá trị, nên dùng `int` cho gọn.

### Wrapper class: phiên bản "object" của kiểu nguyên thuỷ

Mỗi kiểu nguyên thuỷ có một class đi kèm, gọi là **wrapper**:

| Nguyên thuỷ | Wrapper |
|---|---|
| `int` | `Integer` (chú ý: không phải `Int`) |
| `long` | `Long` |
| `double` | `Double` |
| `boolean` | `Boolean` |
| `char` | `Character` |
| `byte`, `short`, `float` | `Byte`, `Short`, `Float` |

Java tự chuyển qua lại giữa hai dạng (gọi là **autoboxing / unboxing**), nên thường viết lẫn lộn vẫn chạy:

```java
Long a = 5L;     // tự "đóng hộp" long → Long
long b = a;      // tự "mở hộp" Long → long
```

### Khi nào **bắt buộc** dùng wrapper (viết hoa)?

**1. Khi cần giá trị `null` để nói "chưa có".** Trong project:

```java
// product/Product.java
private long id;       // nguyên thuỷ: sản phẩm mới có id = 0
private Double price;  // wrapper: null = người dùng chưa nhập giá
```

- `ProductService.save(...)` nhận biết thêm mới bằng `data.getId() == 0`, chính vì `id` là `long` nên mặc định là `0`.
- `Category.id` và `User.id` lại khai báo `Long`, nên sản phẩm chưa lưu có id là `null`. Bài 02 cũng dạy `Long id`. Cả hai cách đều chạy được với JPA, nhưng `Long` phổ biến hơn vì `null` nói rõ "chưa có id" hơn số `0`.
- Nếu `price` là `double` thì form để trống sẽ thành `0.0`, không phân biệt được "giá 0 đồng" với "quên nhập".

**2. Trong generic `<...>`.** Chỗ nằm trong dấu `<>` chỉ nhận class, không nhận kiểu nguyên thuỷ:

```java
JpaRepository<Product, Long>   // đúng
JpaRepository<Product, long>   // lỗi biên dịch
List<Integer> ids;             // đúng
List<int> ids;                 // lỗi biên dịch
```

Vì vậy `ProductRepository` ghi `Long` dù `Product.id` là `long`. Java tự chuyển đổi giữa hai bên.

### 2 cái bẫy khi dùng wrapper

**Bẫy 1: mở hộp một giá trị `null` sẽ gây `NullPointerException`.**

```java
Double price = null;
double p = price;      // NullPointerException: không có số nào để lấy ra
if (price > 0) { }     // cũng NPE, vì phải mở hộp để so sánh
```

Đây là lý do CLAUDE.md nhắc: nếu đổi `Product.id` từ `long` sang `Long` thì `data.getId() == 0` sẽ NPE với sản phẩm mới (id là `null`), phải sửa thành `data.getId() == null`.

**Bẫy 2: so sánh 2 wrapper bằng `==` là so sánh địa chỉ, không phải giá trị.**

```java
Long a = 1000L, b = 1000L;
a == b          // false! hai object khác nhau
a.equals(b)     // true, so sánh giá trị
```

Với số nhỏ (-128 → 127) thì `==` "tình cờ" ra `true` do Java dùng lại object có sẵn trong bộ nhớ đệm, nên lỗi này rất khó phát hiện. Quy tắc: **so sánh object thì dùng `.equals()`**, kể cả `String`. Còn nếu một bên là kiểu nguyên thuỷ (`data.getId() == 0`) thì `==` so sánh giá trị, không sao.

### Tóm lại: chọn kiểu nào?

| Tình huống | Dùng |
|---|---|
| Số luôn có giá trị, chỉ để tính toán (giới hạn, bộ đếm, vòng lặp) | `int`, `long`, `double`, `boolean` |
| Giá trị có thể "chưa có" (field entity, dữ liệu từ form/DB) | `Integer`, `Long`, `Double`, `Boolean` |
| Trong `<...>` (List, Map, Repository...) | Bắt buộc wrapper |
| Chuỗi, thời gian, object nghiệp vụ | Class tương ứng: `String`, `Duration`, `Product`... (không có bản viết thường) |

---

## Hàm có sẵn của thư viện (ví dụ `opsForZSet`) ở đâu ra? Làm sao biết nó làm gì?

> Hỏi ở: [Bài 16 — Bước 3: `ProductStatsService`](study/16-luot-xem-top-san-pham-redis.md#bước-3--productstatsservice-mọi-thao-tác-với-redis) (hiểu các hàm tự viết trong service, nhưng không hiểu `opsForZSet().incrementScore(...)`)

### Hàm đó từ đâu tới?

Trong code có 2 loại hàm:

| | Hàm tự viết | Hàm có sẵn (của thư viện) |
|---|---|---|
| Ví dụ | `recordView`, `topProductIds`, `findAllInOrder` | `opsForZSet`, `incrementScore`, `findAllById`, `String.valueOf` |
| Code nằm ở | File `.java` trong `src/` | File `.jar` mà Gradle tải về, nhờ các dòng `implementation '...'` trong `build.gradle` |
| Ai viết | Mình | Nhóm Spring, nhóm Java... |

Ví dụ `opsForZSet()` là method của class `StringRedisTemplate`, thuộc thư viện **Spring Data Redis**. Thư viện này có trong project nhờ dòng `spring-boot-starter-data-redis` thêm vào `build.gradle` ở bài 14. Biến `redisTemplate` có kiểu `StringRedisTemplate`, nên gọi được mọi method của class đó.

### Cách nhận biết một hàm là "có sẵn"

- Gọi trên một biến mà **kiểu của nó** không phải class mình viết: `redisTemplate` (kiểu `StringRedisTemplate`), `env` (`Environment`), `model` (`Model`).
- Class đó được `import` từ package lạ: `org.springframework...`, `java.util...`. Class mình viết thì có package `com.example.demo...`.
- Riêng `ProductRepository`: mình chỉ viết interface trống, nhưng nó `extends JpaRepository`, nên **thừa kế** sẵn hàng chục hàm như `findAll`, `findById`, `findAllById`, `save`, `delete`.

### Cách tự tra một hàm lạ trong VS Code

1. **Rê chuột** lên tên hàm: hiện ra khai báo đầy đủ (tham số, kiểu trả về) và đoạn mô tả (Javadoc) của tác giả thư viện, bằng tiếng Anh.
2. **Ctrl + click** (hoặc F12): mở thẳng mã nguồn của class đó, xem được mọi method khác của nó.
3. Gõ dấu `.` sau một biến, ví dụ `redisTemplate.`, rồi chờ: VS Code liệt kê mọi method gọi được trên biến đó.
4. Tra trên mạng theo dạng `Tên class + tên hàm`, ví dụ `ZSetOperations incrementScore`. Kết quả đầu thường là trang Javadoc chính thức (`docs.spring.io`, `docs.oracle.com`).

Khi đọc khai báo, chỉ cần để ý 3 thứ: **của class nào**, **nhận tham số gì**, **trả về kiểu gì**. Ví dụ:

```java
Double incrementScore(K key, V value, double delta)
//  ↑ trả về         ↑ 3 tham số. K, V là kiểu của key và value: với StringRedisTemplate thì cả hai là String
```

### Dòng gọi nối nhau: `a.b().c()`

```java
redisTemplate.opsForZSet().incrementScore(VIEWS_KEY, id, 1);
```

Mỗi dấu `.` là gọi hàm **trên kết quả của lần gọi trước**. `opsForZSet()` chưa gửi gì tới Redis, nó chỉ trả về một object `ZSetOperations` ("bộ lệnh dành cho Sorted Set"). Sau đó `.incrementScore(...)` được gọi trên object đó, và lúc này mới gửi lệnh `ZINCRBY`. Viết tách 2 dòng thì chạy y hệt:

```java
ZSetOperations<String, String> zset = redisTemplate.opsForZSet();
Double views = zset.incrementScore(VIEWS_KEY, id, 1);
```

Spring chia lệnh Redis theo kiểu dữ liệu: `opsForValue()` cho String, `opsForList()` cho List, `opsForZSet()` cho Sorted Set, `opsForHash()` cho Hash... Bảng đầy đủ các hàm dùng trong bài 16 (tham số, kết quả trả về, lệnh Redis tương ứng) nằm ở phần **📖 Tra cứu** cuối mỗi bước của bài 16, ví dụ [Bước 3](study/16-luot-xem-top-san-pham-redis.md#-tra-cứu-các-hàm-có-sẵn-dùng-trong-bước-này-1).

---

## Dấu `()` và dấu `<>` khác nhau thế nào? (`List<Long>`, `new HashMap<>()`, generic)

> Hỏi ở: [Bài 16 — Bước 3: `ProductStatsService`](study/16-luot-xem-top-san-pham-redis.md#bước-3--productstatsservice-mọi-thao-tác-với-redis) (hàm thường dùng `()`, nhưng có chỗ lại có `<>`, ví dụ `Set<TypedTuple<String>>`, `new HashMap<>()`)

### Một câu: `()` chứa **giá trị**, `<>` chứa **kiểu**

| | `( )` | `< >` |
|---|---|---|
| Bên trong là | **Giá trị**: số, chuỗi, biến, object | **Kiểu dữ liệu** (tên class): `Long`, `String`, `Product` |
| Có tác dụng lúc | Chương trình **chạy** | Trình biên dịch **kiểm tra code** (trước khi chạy) |
| Ví dụ | `incrementScore(VIEWS_KEY, id, 1)` | `List<Long>`, `Map<Long, Long>` |
| Đọc là | "gọi hàm với các giá trị này" | "... **chứa / làm việc với** kiểu này" |

### `<>` là gì: generic, tức "kiểu có chỗ trống"

Một số class được viết để **dùng được cho nhiều kiểu**. Ví dụ `List` có thể chứa số, chuỗi hay sản phẩm. Người viết `List` để lại một "chỗ trống" cho kiểu phần tử, và khi dùng thì mình điền kiểu vào trong `<>`:

```java
List<Long> ids;              // danh sách chứa các Long
List<Product> products;      // danh sách chứa các Product
Map<Long, Long> views;       // Map: key là Long, value là Long   (id → lượt xem)
Map<Long, Product> byId;     // Map: key là Long, value là Product
```

Điền kiểu vào để làm gì? Để **trình biên dịch bắt lỗi giúp mình**:

```java
List<Long> ids = new ArrayList<>();
ids.add(5L);          // đúng
ids.add("abc");       // LỖI ngay khi gõ: List<Long> không nhận String
Long first = ids.get(0);   // không cần ép kiểu: Java biết chắc phần tử là Long
```

PHP không có cái này: mảng PHP chứa gì cũng được, lỡ bỏ nhầm chuỗi vào mảng số thì chỉ khi chạy mới lộ lỗi. Gần nhất bên PHP là chú thích `/** @var array<int, Product> */` cho IDE đọc, nhưng PHP không bắt buộc.

### Các chỗ gặp `<>` trong project

| Code | Đọc là |
|---|---|
| `List<Long> ids` | danh sách các `Long` |
| `Set<String> members` | tập hợp các chuỗi |
| `Set<TypedTuple<String>> tuples` | tập hợp các **cặp** (member + score), trong đó member là `String`. `<>` **lồng nhau**: đọc từ ngoài vào trong |
| `Optional<User>` | "cái hộp" có thể chứa một `User` hoặc rỗng (bài 12) |
| `ZSetOperations<String, String>` | bộ lệnh Sorted Set, key là `String`, member là `String` |
| `interface ProductRepository extends JpaRepository<Product, Long>` | repository cho entity `Product`, khoá chính kiểu `Long`. Nhờ đó `findById(...)` biết trả về `Optional<Product>`, `findAll()` biết trả về `List<Product>` |

### `new HashMap<>()`: dấu `<>` rỗng (gọi là *diamond*, "hình thoi")

```java
Map<Long, Product> byId = new HashMap<>();
//  ↑ đã ghi đủ kiểu ở bên trái      ↑ bên phải để trống: Java tự điền <Long, Product>
```

Viết đầy đủ là `new HashMap<Long, Product>()`. Viết `<>` rỗng là cách viết tắt. Còn `()` ở cuối là **gọi constructor** (không truyền giá trị nào).

Chú ý: **không bỏ hẳn `<>`** (viết `new HashMap()`). Code vẫn chạy được nhưng mất kiểm tra kiểu, và VS Code sẽ cảnh báo *raw type*.

### Một dòng có cả `<>` lẫn `()`

```java
Set<TypedTuple<String>> tuples = redisTemplate.opsForZSet().rangeWithScores(VIEWS_KEY, 0, -1);
//  ↑ <>: kiểu của biến                              ↑ (): gọi hàm   ↑ (): gọi hàm, truyền 3 giá trị
```

Vế trái nói biến `tuples` có **kiểu** gì. Vế phải **chạy** các hàm để lấy giá trị.

### Bẫy: `Integer.class` nằm trong `()`, không phải `<>`

```java
env.getRequiredProperty(key, Integer.class)   // AppConfig, bài 16
```

Ở đây kiểu được truyền như một **giá trị** (`Integer.class` là một object mô tả class `Integer`), nên nó nằm trong `()`. Lý do: hàm này cần biết kiểu **lúc chạy** để đổi chuỗi `"5"` thành số. Còn thông tin trong `<>` chỉ dùng lúc biên dịch, khi chạy thì Java đã xoá đi.

### Trong `<>` chỉ được ghi class, không được ghi kiểu nguyên thuỷ

```java
List<Long> ids;   // đúng
List<long> ids;   // lỗi biên dịch
```

Xem mục [Kiểu dữ liệu viết thường và viết hoa](#kiểu-dữ-liệu-viết-thường-int-long-và-viết-hoa-integer-long-string-duration-khác-nhau-thế-nào).

### Những chỗ khác cũng dùng `()` (không phải gọi hàm)

| Code | Ý nghĩa |
|---|---|
| `public long recordView(long productId, String username)` | **Khai báo** hàm: trong `()` là danh sách tham số kèm kiểu |
| `new Product()` | Gọi constructor để tạo object |
| `() -> new RuntimeException("...")` | Lambda không có tham số (bài 02, `orElseThrow`) |
| `if (p != null)`, `for (Long id : ids)` | Điều kiện của `if`, `for`, `while` |
| `(int) 5.7` | Ép kiểu: đổi `5.7` thành `int` (ra `5`) |
| `(a + b) * 2` | Nhóm phép tính như toán học |

Còn `<` và `>` đứng **một mình** giữa hai số (`views > 0`, `i < 5`) thì chỉ là phép so sánh, không liên quan gì tới generic.
