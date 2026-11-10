# Java: kiểu dữ liệu và generic

Kiểu nguyên thuỷ và wrapper, generic `<>`.

Mỗi mục là một câu hỏi phát sinh trong lúc học, ghi rõ hỏi ở bài nào. Mục lục chung: [common_knowledge.md](../common_knowledge.md).

## Kiểu dữ liệu viết thường (`int`, `long`) và viết hoa (`Integer`, `Long`, `String`, `Duration`) khác nhau thế nào?

> Hỏi ở: [Bài 16 — Bước 3: `ProductStatsService`](../study/16-luot-xem-top-san-pham-redis.md#bước-3--productstatsservice-mọi-thao-tác-với-redis) (thấy `private final Duration recentTtl;` nhưng `private final int recentLimit;`)

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

## Dấu `()` và dấu `<>` khác nhau thế nào? (`List<Long>`, `new HashMap<>()`, generic)

> Hỏi ở: [Bài 16 — Bước 3: `ProductStatsService`](../study/16-luot-xem-top-san-pham-redis.md#bước-3--productstatsservice-mọi-thao-tác-với-redis) (hàm thường dùng `()`, nhưng có chỗ lại có `<>`, ví dụ `Set<TypedTuple<String>>`, `new HashMap<>()`)

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
