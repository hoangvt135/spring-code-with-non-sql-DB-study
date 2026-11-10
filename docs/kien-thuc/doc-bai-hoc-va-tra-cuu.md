# Đọc bài học và tự tra cứu

Cách đọc khối code trong bài học, và cách tự tra một hàm có sẵn của thư viện.

Mỗi mục là một câu hỏi phát sinh trong lúc học, ghi rõ hỏi ở bài nào. Mục lục chung: [common_knowledge.md](../common_knowledge.md).

## Chữ `groovy` (`yaml`, `java`...) sau dấu ``` trong bài học là gì? Groovy là gì?

> Hỏi ở: [Bài 14 — Bước 3: Thêm thư viện vào `build.gradle`](../study/14-moi-truong-mongodb-redis.md#bước-3--thêm-thư-viện-vào-buildgradle)

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

## Hàm có sẵn của thư viện (ví dụ `opsForZSet`) ở đâu ra? Làm sao biết nó làm gì?

> Hỏi ở: [Bài 16 — Bước 3: `ProductStatsService`](../study/16-luot-xem-top-san-pham-redis.md#bước-3--productstatsservice-mọi-thao-tác-với-redis) (hiểu các hàm tự viết trong service, nhưng không hiểu `opsForZSet().incrementScore(...)`)

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

Spring chia lệnh Redis theo kiểu dữ liệu: `opsForValue()` cho String, `opsForList()` cho List, `opsForZSet()` cho Sorted Set, `opsForHash()` cho Hash... Bảng đầy đủ các hàm dùng trong bài 16 (tham số, kết quả trả về, lệnh Redis tương ứng) nằm ở phần **📖 Tra cứu** cuối mỗi bước của bài 16, ví dụ [Bước 3](../study/16-luot-xem-top-san-pham-redis.md#-tra-cứu-các-hàm-có-sẵn-dùng-trong-bước-này-1).
