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
- [Java có giá trị mặc định cho tham số (`String description = ""`) không?](#java-có-giá-trị-mặc-định-cho-tham-số-string-description---không)

---

## Chữ `groovy` (`yaml`, `java`...) sau dấu ``` trong bài học là gì? Groovy là gì?

> Hỏi ở: [Bài 14 — Bước 3: Thêm thư viện vào `build.gradle`](14-moi-truong-mongodb-redis.md#bước-3--thêm-thư-viện-vào-buildgradle)

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

> Hỏi ở: [Bài 14 — Bước 4: Cấu hình kết nối trong `application.yml`](14-moi-truong-mongodb-redis.md#bước-4--cấu-hình-kết-nối-trong-applicationyml)

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

> Hỏi ở: [Bài 14 — Bước 5: `NoSqlConnectionCheck`](14-moi-truong-mongodb-redis.md#bước-5--thử-ghiđọc-lúc-khởi-động-nosqlconnectioncheck) (cú pháp này xuất hiện lần đầu ở [Bài 13 — Bước 1: `DataSeeder`](13-du-lieu-mau-seeder.md#bước-1--khung-class-dataseeder))

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

> Hỏi khi chuẩn bị [Bài 15 — Ảnh và mô tả cho sản phẩm](15-anh-va-mo-ta-san-pham.md#tư-vấn-trước-khi-code-lưu-ảnh-ở-đâu-file-vật-lý-hay-blob), liên quan tới MongoDB ở [Bài 14](14-moi-truong-mongodb-redis.md). Bảng so sánh đầy đủ nằm trong bài 15.

- **BLOB** (*Binary Large Object*) là cách gọi chung cho "một cục byte lớn lưu thẳng trong DB": ảnh, PDF, zip... Đây **không phải tính năng riêng của MongoDB**. MySQL có kiểu cột `BLOB` / `MEDIUMBLOB` (16MB) / `LONGBLOB` (4GB). MongoDB có kiểu `BinData`, nhưng cả document tối đa 16MB. File lớn hơn thì dùng **GridFS**: cắt file thành mảnh 255KB rồi lưu vào 2 collection `fs.files` + `fs.chunks`.
- **"Document" trong MongoDB = một bản ghi dạng JSON**, tương đương một dòng trong bảng MySQL. Nó **không** có nghĩa là "tài liệu" kiểu Word/PDF. MongoDB không có ưu thế đặc biệt nào trong việc lưu file so với MySQL.
- **Lưu ở đâu?** Hầu hết dự án làm theo kiểu: **file để một nơi** (ổ đĩa, hoặc S3/MinIO khi chạy production), **DB chỉ lưu tên file**. Lý do là hiển thị nhanh, DB nhẹ, backup nhanh. BLOB chỉ đáng chọn khi file nhỏ, ít, và bắt buộc phải nằm chung transaction với bản ghi.
- Với project này, lưu ảnh vào MongoDB GridFS còn kém hơn nữa: sản phẩm nằm ở MySQL, ảnh lại ở MongoDB, nên xoá sản phẩm và xoá ảnh không cùng một transaction. Như vậy mất luôn ưu điểm "nhất quán" là lý do chính để chọn BLOB.

---

## `@Column`: vì sao có trường khai báo, có trường không?

> Hỏi ở: [Bài 15 — Bước 3: Thêm 2 trường vào `Product`](15-anh-va-mo-ta-san-pham.md#bước-3--thêm-2-trường-vào-product). `@Column` xuất hiện lần đầu ở [Bài 02 — Bước 1: Entity](02-thuc-hanh-crud.md#bước-1--entity-mô-tả-bảng-dữ-liệu).

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

> Hỏi ở: [Bài 15 — Bước 4: `FileStorageService`: lưu và xoá file](15-anh-va-mo-ta-san-pham.md#bước-4--filestorageservice-lưu-và-xoá-file)

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

- Với `catch (Exception e)`: lỗi "tên file không hợp lệ" bị đổi thành "Lưu file thất bại", tức là thông báo sai bản chất. Controller muốn `catch (IllegalArgumentException e)` để hiện chữ đỏ trên form ([Bài 15 — Bài tập 2](15-anh-va-mo-ta-san-pham.md#bài-tập)) sẽ **không bắt được nữa**, vì lỗi đã bị gói thành `RuntimeException`.
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

> Hỏi ở: [Bài 15 — Bước 4: `FileStorageService`: lưu và xoá file](15-anh-va-mo-ta-san-pham.md#bước-4--filestorageservice-lưu-và-xoá-file). Cùng kiểu viết này cũng có ở [Bài 14](14-moi-truong-mongodb-redis.md).

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

## Java có giá trị mặc định cho tham số (`String description = ""`) không?

> Hỏi ở: [Bài 13 — Bước 3: Danh mục và sản phẩm mẫu](13-du-lieu-mau-seeder.md#bước-3--danh-mục-và-sản-phẩm-mẫu) (khi muốn `createProduct()` nhận thêm `description` từ [Bài 15](15-anh-va-mo-ta-san-pham.md))

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
