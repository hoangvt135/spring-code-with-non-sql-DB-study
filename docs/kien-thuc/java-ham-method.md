# Java: hàm (method) và tham số

Varargs, nạp chồng (overload), ghi đè (`@Override`).

Mỗi mục là một câu hỏi phát sinh trong lúc học, ghi rõ hỏi ở bài nào. Mục lục chung: [common_knowledge.md](../common_knowledge.md).

## `String... args` (3 dấu chấm) trong `run()` là gì?

> Hỏi ở: [Bài 14 — Bước 5: `NoSqlConnectionCheck`](../study/14-moi-truong-mongodb-redis.md#bước-5--thử-ghiđọc-lúc-khởi-động-nosqlconnectioncheck) (cú pháp này xuất hiện lần đầu ở [Bài 13 — Bước 1: `DataSeeder`](../study/13-du-lieu-mau-seeder.md#bước-1--khung-class-dataseeder))

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

## Java có giá trị mặc định cho tham số (`String description = ""`) không? Nạp chồng (overload) là gì?

> Hỏi ở: [Bài 13 — Bước 3: Danh mục và sản phẩm mẫu](../study/13-du-lieu-mau-seeder.md#bước-3--danh-mục-và-sản-phẩm-mẫu) (khi muốn `createProduct()` nhận thêm `description` từ [Bài 15](../study/15-anh-va-mo-ta-san-pham.md)).
> Phần "Tìm hiểu kỹ hơn về nạp chồng" được hỏi thêm ở [Bài 16 — Bước 2](../study/16-luot-xem-top-san-pham-redis.md#bước-2--mỗi-module-một-file-properties-đọc-qua-package-common), khi so sánh với [`@Override`](#override-là-gì-bỏ-đi-có-sao-không-khác-gì-nạp-chồng-overloading).

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

## `@Override` là gì? Bỏ đi có sao không? Khác gì nạp chồng (overloading)?

> Hỏi ở: [Bài 16 — Bước 2, mục ③ `ModuleConfigLoader`](../study/16-luot-xem-top-san-pham-redis.md#bước-2--mỗi-module-một-file-properties-đọc-qua-package-common) (gõ nhầm `postProcessEnviroment`, thiếu chữ `n`, và bị báo 2 lỗi)

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
