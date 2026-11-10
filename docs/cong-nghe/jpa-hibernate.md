# Spring Data JPA & Hibernate

> **Dùng trong project từ:** [bài 02](../study/02-thuc-hanh-crud.md) · **Phiên bản:** Spring Data JPA 3.4.1, Hibernate ORM 6.6.4.Final, Jakarta Persistence 3.1 (theo Spring Boot 3.4.1 trong `build.gradle`) · **Tài liệu chính thức:** [Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/3.4/), [Hibernate ORM 6.6 User Guide](https://docs.hibernate.org/orm/6.6/userguide/html_single/Hibernate_User_Guide.html)
>
> 🔼 **Sau [bài 16.5](../study/16.5-nang-cap-spring-boot-4.md)** (người học chưa làm): Hibernate **7.4**, Spring Data **2026.0**. Bỏ dòng `hibernate.dialect` (cảnh báo `HHH90000025`, Bước 2). Entity và Repository không phải sửa.

## Nó là gì, giải quyết vấn đề gì

Bên PHP, có hai cách quen thuộc để làm việc với MySQL:

- **PDO**: tự viết SQL, tự lấy từng dòng kết quả rồi gán vào mảng hoặc object.
- **Eloquent** (Laravel): mỗi bảng có một class model, gọi `Product::find(1)`, `$product->save()`, khỏi viết SQL.

Cách thứ hai gọi là **ORM** (Object-Relational Mapping): ánh xạ **một class ↔ một bảng**, **một object ↔ một dòng**, **một trường ↔ một cột**. Trong project này, phần ORM gồm 3 lớp chồng lên nhau, rất hay bị gọi lẫn tên:

| Tên | Là gì | So với PHP | Trong project thấy ở đâu |
|---|---|---|---|
| **JPA** (Jakarta Persistence) | **Bản đặc tả**: một bộ interface và annotation (`@Entity`, `@Id`, `EntityManager`...). Tự nó không chạy được | Giống một "interface" chung, chưa có code thật | `import jakarta.persistence.*;` trong mọi entity |
| **Hibernate** | **Thư viện làm thật** bản đặc tả JPA: sinh SQL, gửi tới MySQL qua JDBC, đổ kết quả vào object | Giống phần lõi của Eloquent | Dòng `Hibernate: select ...` trong log; `ddl-auto` tạo bảng |
| **Spring Data JPA** | Lớp **tiện ích của Spring** đặt trên JPA: chỉ cần khai báo interface `extends JpaRepository`, Spring tự sinh class làm sẵn `save`, `findAll`... | Giống các hàm có sẵn `find()`, `all()`, `where()` của Eloquent | `ProductRepository`, `CategoryRepository`, `UserRepository` |

Tất cả có được nhờ một dòng trong `build.gradle`: `spring-boot-starter-data-jpa`. Driver `mysql-connector-j` (bản 9.1.0 theo Spring Boot 3.4.1) là phần JDBC nói chuyện thật với MySQL, giống extension `pdo_mysql` bên PHP.

Một khác biệt lớn so với Eloquent: Eloquent là **Active Record** (object tự lưu mình: `$product->save()`), còn JPA là **Data Mapper**: entity chỉ chứa dữ liệu, việc lưu do Repository lo (`productRepository.save(product)`). Vì vậy `Product` trong project không có hàm `save()` nào.

## Khái niệm cốt lõi

### Entity: class ↔ bảng

Một class có `@Entity` là một entity. Hibernate đọc các annotation để biết bảng tên gì, cột nào là khoá chính, cột nào bắt buộc.

```java
@Entity                          // class này ứng với một bảng
@Table(name = "category")        // tên bảng. Bỏ đi thì tên bảng lấy theo tên class
public class Category {
    @Id                                                   // khoá chính
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // MySQL tự tăng (AUTO_INCREMENT)
    private Long id;

    @Column(nullable = false)    // NOT NULL
    private String name;

    public Category() {}         // JPA bắt buộc có constructor không tham số
}
```

- **`@GeneratedValue(strategy = GenerationType.IDENTITY)`**: để MySQL tự cấp id bằng `AUTO_INCREMENT`. Hệ quả: lúc lưu object mới, Hibernate phải chạy `INSERT` **ngay** để lấy id về, không chờ được.
- **Constructor rỗng**: Hibernate tạo object bằng `new Category()` rồi mới gán từng trường. Thiếu constructor này là lỗi lúc chạy.
- **Mọi trường đều thành cột**, kể cả khi không có `@Column`. Khi nào cần `@Column`, xem [Hỏi đáp: `@Column`](#column-vì-sao-có-trường-khai-báo-có-trường-không).
- **Tên bảng, tên cột**: Spring Boot cài sẵn quy tắc đặt tên `CamelCaseToUnderscoresNamingStrategy`: đổi `imagePath` → `image_path`, và **viết thường hết**. Vì vậy `Product` ghi `@Table(name="Product")` nhưng bảng thật trong MySQL vẫn là `product`.

### Repository và các hàm có sẵn

```java
public interface ProductRepository extends JpaRepository<Product, Long> { }
//                                                   ↑ entity  ↑ kiểu của khoá chính
```

Lúc khởi động, Spring Data tạo một object làm thật interface này (class `SimpleJpaRepository`) và đăng ký nó thành bean, nên `ProductService` nhận được qua constructor. Danh sách hàm có sẵn ở [Tra cứu nhanh](#tra-cứu-nhanh).

Hai hàm hay dùng nhất có kiểu trả về đáng chú ý:

- `findById(id)` trả **`Optional<Product>`** (một cái "hộp" có thể rỗng), không trả `null`. Project mở hộp bằng `.orElseThrow(() -> new NotFoundException(...))`.
- `save(entity)` trả về **object đã lưu**. Nên dùng object được trả về, đừng dùng object truyền vào (xem [Persistence context](#persistence-context-và-save)).

### Query method: Spring đọc tên hàm để sinh SQL

Chỉ cần **khai báo** tên hàm đúng quy tắc, không viết thân hàm:

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);   // ... WHERE username = ?
    boolean existsByUsername(String username);        // có dòng nào khớp không?
}
```

| Phần đầu tên | Nghĩa | Kiểu trả về thường dùng |
|---|---|---|
| `findBy...` | Lấy dữ liệu | `Optional<T>` (0 hoặc 1 dòng), `List<T>` (nhiều dòng) |
| `existsBy...` | Có hay không | `boolean` |
| `countBy...` | Đếm | `long` |
| `deleteBy...` | Xoá theo điều kiện | `void` hoặc `long` (số dòng xoá) |

Phần sau `By` là tên **trường trong entity** (không phải tên cột), ghép thêm từ khoá: `findByNameContaining` (`LIKE %...%`), `findByPriceLessThan`, `findByNameAndPrice`, `findByCategoryId` (đi xuyên quan hệ `category.id`, xem [bài 06 Bước 5](../study/06-quan-he-model.md#bước-5--truy-vấn-theo-quan-hệ-lấy-product-của-1-category)). Gõ sai tên trường thì app **không khởi động được** (`No property '...' found for type ...`), nên lỗi lộ ra sớm.

Với `existsBy...`, SQL thật mà Spring Data JPA sinh ra có dạng `select u.id from users u where u.username=? limit ?` (chỉ lấy tối đa 1 dòng). Bài 12 ghi `SELECT COUNT(*) > 0` cho dễ hiểu, ý nghĩa thì như nhau.

So với Eloquent: `findByUsername("an")` ≈ `User::where('username', 'an')->first()`, `existsByUsername("an")` ≈ `User::where('username', 'an')->exists()`.

### Quan hệ: `@ManyToOne`, `@OneToMany(mappedBy)`, `@JoinColumn`

Project có một quan hệ: **Category 1 — N Product**.

```java
// Product.java — phía CHỦ (owning side): bảng product giữ cột khoá ngoại
@ManyToOne
@JoinColumn(name = "category_id")
private Category category;

// Category.java — phía PHỤ (inverse side): chỉ "nhìn theo", không tạo cột nào
@OneToMany(mappedBy = "category")    // "category" = tên TRƯỜNG bên Product
@JsonIgnore                          // chặn vòng lặp JSON vô tận
private List<Product> products = new ArrayList<>();
```

**Phía chủ** là phía Hibernate đọc để ghi cột `category_id`. Hệ quả rất thực tế:

- Gán `product.setCategory(phone)` rồi `save(product)` → cột `category_id` được ghi. ✅
- Chỉ thêm `phone.getProducts().add(product)` rồi `save(phone)` → **không** ghi gì vào `category_id`, vì `Category.products` là phía phụ. ❌

Đó là lý do mọi chỗ trong project (form ở bài 11, `DataSeeder` ở bài 13) đều gán danh mục **từ phía `Product`**. Giải thích bằng hình: [bài 06 mục 2](../study/06-quan-he-model.md#2-hai-phía-của-quan-hệ-chủ-và-phụ).

So với Eloquent: `@ManyToOne` ≈ `belongsTo`, `@OneToMany(mappedBy)` ≈ `hasMany`. Khác ở chỗ Eloquent chỉ cần cột `category_id` có sẵn, còn JPA cần cả hai trường được khai báo và khớp tên với nhau.

### Lazy và eager: khi nào dữ liệu liên quan được nạp

| Annotation | Mặc định | Nghĩa |
|---|---|---|
| `@ManyToOne` (`Product.category`) | **EAGER** | Nạp sản phẩm là nạp luôn danh mục của nó |
| `@OneToMany` (`Category.products`) | **LAZY** | Chỉ nạp danh sách sản phẩm khi code thật sự gọi `getProducts()` |

Bật `show-sql` sẽ thấy rõ:

- `productRepository.findById(1)` → **một** câu `select ... from product p1_0 left join category c1_0 on ...` (eager → JOIN luôn).
- `productRepository.findAll()` → một câu lấy mọi sản phẩm, rồi **thêm một câu cho mỗi danh mục khác nhau** mà các sản phẩm trỏ tới. Đây là dạng nhẹ của lỗi **N+1 query**: dữ liệu ít thì không sao, nhưng nên biết nó tồn tại.

LAZY nghĩa là `products` lúc đầu chỉ là một "vỏ" (Hibernate gọi là proxy/collection chưa khởi tạo). Gọi `getProducts()` khi kết nối tới DB đã đóng thì gặp `LazyInitializationException`. Trong project chưa gặp lỗi này nhờ **open-in-view** (mục dưới).

### `ddl-auto: update`: Hibernate tự tạo bảng

```yaml
# application.yml
spring.jpa.hibernate.ddl-auto: update
```

Lúc khởi động, Hibernate so entity với bảng trong MySQL rồi **chỉ thêm** những gì còn thiếu: bảng mới, cột mới, khoá ngoại mới. Nó **không** sửa kiểu/độ dài cột đã có, **không** xoá cột, **không** đổi tên cột. Bảng đầy đủ "sửa gì thì cần reset": [bài 15.5 — Khi nào cần reset?](../study/15.5-reset-du-lieu-va-chay-lai-seeder.md#khi-nào-cần-reset). 5 giá trị của `ddl-auto` và vì sao project giữ `update`: [bài 15.5](../study/15.5-reset-du-lieu-va-chay-lai-seeder.md#sao-không-đổi-luôn-sang-ddl-auto-create-cho-khỏi-phải-reset).

So với Laravel: không có file migration nào. Phần "migrate" do Hibernate đoán từ entity, phần "seed" do `DataSeeder` làm ([bài 13](../study/13-du-lieu-mau-seeder.md#migrate-hay-seed-hai-việc-khác-nhau)). Dự án thật thường dùng `validate` + Flyway/Liquibase, vì `update` không có lịch sử thay đổi và không làm được việc sửa/xoá cột.

### `show-sql`, `format_sql`: xem SQL Hibernate gửi đi

```yaml
spring.jpa.show-sql: true                          # in mọi câu SQL
spring.jpa.properties.hibernate.format_sql: true   # xuống dòng, thụt lề cho dễ đọc
```

Lưu ý:

- `show-sql` in thẳng ra console (dòng bắt đầu bằng `Hibernate:`), **không** đi qua hệ thống log, nên **không** có trong `logs/app.log` (xem [logging.md](logging.md)).
- Giá trị tham số hiện là dấu `?`. Muốn thấy giá trị thật thì bật log ở mức TRACE cho logger `org.hibernate.orm.jdbc.bind` (tên logger của Hibernate 6; tài liệu cũ cho Hibernate 5 ghi `org.hibernate.type.descriptor.sql` sẽ không còn tác dụng).
- Hibernate 6 đặt bí danh bảng kiểu `p1_0`, `c1_0`. Đó là tên tạm trong câu SQL, không phải tên bảng thật.

### Open-in-view: kết nối DB mở suốt request

Spring Boot mặc định bật `spring.jpa.open-in-view=true`. Nghĩa là **suốt một request web**, từ lúc vào controller tới lúc Thymeleaf vẽ xong HTML, vẫn giữ một `EntityManager` (phiên làm việc với DB) mở. Nhờ vậy template gọi tới dữ liệu lazy vẫn chạy.

Vì project không tự ghi thuộc tính này, mỗi lần khởi động log có cảnh báo:

```
spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning
```

Đây là cảnh báo, không phải lỗi. Nhược điểm: kết nối DB bị giữ lâu hơn cần thiết, và SQL có thể chạy "ngầm" lúc vẽ template. Với project học tập thì để nguyên được. Nếu tắt (`false`) thì mọi dữ liệu lazy phải được nạp xong trong service.

Ngoài request web thì **không** có open-in-view: ví dụ `DataSeeder` (chạy lúc khởi động) gọi `category.getProducts()` sẽ gặp `LazyInitializationException`.

### Persistence context và `save()`

**Persistence context** (do `EntityManager` quản lý) là "bộ nhớ tạm" của Hibernate trong một phiên làm việc:

- Object nạp từ DB được ghi nhớ (trạng thái *managed*). Gọi `findById(1)` lần thứ hai trong cùng phiên thì **không** chạy SQL lần nữa.
- Hibernate nhớ trạng thái ban đầu, lúc *flush* sẽ tự so và sinh `UPDATE` cho những trường đã đổi (*dirty checking*).

`save()` của Spring Data quyết định "thêm hay sửa" như sau:

```
save(entity)
 ├─ entity mới?  (id == null với Long, hoặc id == 0 với long nguyên thuỷ)
 │     └─ có → em.persist(entity)  → INSERT, trả về CHÍNH object đó (đã có id)
 └─ không → em.merge(entity)       → SELECT (nếu chưa có trong bộ nhớ tạm) rồi UPDATE,
                                     trả về object managed, có thể là MỘT OBJECT KHÁC
```

Vì nhánh `merge` có thể trả về object khác, quy tắc an toàn là: **luôn dùng giá trị `save()` trả về**. `DataSeeder.createCategory()` làm đúng như vậy: `return categoryRepository.save(category);` ([bài 13](../study/13-du-lieu-mau-seeder.md)).

⚠️ Từ **Hibernate 6.6**: `merge` một entity có id tự sinh, mà id đó **không có trong DB**, sẽ ném `OptimisticLockException` (bản cũ thì lặng lẽ `INSERT` dòng mới). Ví dụ: `POST /api/products` gửi kèm `"id": 999` không tồn tại → lỗi 500 (`ObjectOptimisticLockingFailureException`). Gửi kèm id **đã có** thì ngược lại: dòng đó bị ghi đè.

### Vì sao `Product.id` là `long` mà `Category.id` là `Long`

`long` là kiểu nguyên thuỷ, không chứa được `null`, mặc định là `0`. `Long` là kiểu object, mặc định là `null` (so sánh kỹ: [kiểu dữ liệu Java](../kien-thuc/java-kieu-du-lieu.md#kiểu-dữ-liệu-viết-thường-int-long-và-viết-hoa-integer-long-string-duration-khác-nhau-thế-nào)).

Bài 02 viết `Long id`, đến bài 06 Bước 2 thì `Product` đổi thành `long id` (không giải thích). Code hiện tại giữ `long`, và nhiều chỗ đã dựa vào nó:

- `ProductService.save()` phân biệt thêm/sửa bằng `data.getId() == 0`. Form "thêm mới" không gửi `id` → `id` giữ giá trị mặc định `0`.
- Spring Data cũng coi `0` là "entity mới" với khoá chính kiểu nguyên thuỷ, nên `save()` vẫn chọn đúng `persist`.
- Cột `id` là `BIGINT NOT NULL AUTO_INCREMENT` dù dùng `long` hay `Long`, vì khoá chính luôn `NOT NULL`.

Nếu đổi sang `Long` thì `data.getId() == 0` sẽ ném `NullPointerException` khi `id` là `null` (Java tự "mở hộp" `null` để so với `0`). Phải sửa kèm thành `data.getId() == null`. Ngược lại, `Category` dùng `Long` nên `CategoryViewController` kiểm tra `category.getId() == null`. Lưu ý: `JpaRepository<Product, Long>` vẫn ghi `Long` vì trong `<>` chỉ được ghi class, không ghi kiểu nguyên thuỷ.

### `@Transactional`: gom nhiều thao tác thành một

**Transaction** = "hoặc tất cả thành công, hoặc không có gì thay đổi" (khái niệm phía MySQL: [mysql.md](mysql.md#transaction)). Trong Spring, đặt `@Transactional` (của `org.springframework.transaction.annotation`, đừng nhầm với `jakarta.transaction.Transactional`) lên một method của bean, thường là method public của service: Spring mở transaction trước khi vào method, `COMMIT` khi method chạy xong, `ROLLBACK` nếu method ném `RuntimeException`.

Project **chưa dùng `@Transactional` ở đâu cả**. Code vẫn chạy vì bản thân các hàm của `SimpleJpaRepository` đã có `@Transactional` sẵn (hàm đọc là `readOnly`). Hệ quả: **mỗi lần gọi repository là một transaction riêng**. Ví dụ trong `ProductService.save()` khi thêm sản phẩm có ảnh:

```
create(data)                     → transaction 1: INSERT product      → COMMIT
product.setImagePath(newImage)
productRepository.save(product)  → transaction 2: UPDATE image_path   → COMMIT
```

Nếu bước 2 lỗi thì bước 1 **không** bị huỷ. Thêm `@Transactional` lên `save()` sẽ gom 2 câu thành một transaction. Dù vậy nó **chỉ** lo được MySQL: file trong `uploads/` và dữ liệu Redis không quay lui theo, nên `delete()` vẫn phải tự dọn hai chỗ đó.

Ba điều hay bị hiểu sai về `@Transactional`:

1. Chỉ có tác dụng khi method được gọi **từ bean khác** (qua proxy của Spring). Method trong cùng class gọi nhau (`this.update(...)`) thì annotation của method được gọi bị bỏ qua.
2. Mặc định chỉ rollback với `RuntimeException` (và `Error`). Exception kiểu checked (ví dụ `IOException`) thì vẫn `COMMIT`.
3. Trong một method `@Transactional`, sửa entity đã nạp (`product.setName(...)`) là đủ: lúc `COMMIT` Hibernate tự sinh `UPDATE` (dirty checking), không bắt buộc gọi `save()`.

## Project này dùng nó thế nào

| File | Dùng gì | Bài học |
|---|---|---|
| `product/Product.java` | `@Entity`, `@Table(name="Product")` (bảng thật `product`), `long id`, `@Column(nullable=false)` cho `name`, `@Column(length=2000)` cho `description`, `@ManyToOne` + `@JoinColumn(name="category_id")` | [02](../study/02-thuc-hanh-crud.md#bước-1--entity-mô-tả-bảng-dữ-liệu), [06](../study/06-quan-he-model.md#bước-2--thêm-quan-hệ-vào-product), [15](../study/15-anh-va-mo-ta-san-pham.md#bước-3--thêm-2-trường-vào-product) |
| `category/Category.java` | `Long id`, `@OneToMany(mappedBy="category")` + `@JsonIgnore` | [06](../study/06-quan-he-model.md) |
| `user/User.java` | `@Table(name="users")`, `@Column(nullable=false, unique=true)` cho `username` | [12](../study/12-dang-nhap-spring-security.md#bước-2--entity-user) |
| `*Repository.java` | `ProductRepository`, `CategoryRepository` không có hàm tự khai báo. `UserRepository` có 2 query method `findByUsername`, `existsByUsername` | [02](../study/02-thuc-hanh-crud.md#bước-2--repository-thao-tác-với-db), [12](../study/12-dang-nhap-spring-security.md#bước-3--userrepository) |
| `product/ProductService.java` | `findAll`, `findById(...).orElseThrow(...)`, `save`, `delete(entity)`, `findAllById(ids)` (bài 16: lấy nhiều sản phẩm bằng một câu `WHERE id IN (...)`, rồi tự xếp lại thứ tự) | [02](../study/02-thuc-hanh-crud.md#bước-3--service-chứa-logic-nghiệp-vụ), [11](../study/11-chon-danh-muc-cho-san-pham.md#bước-5--productserviceupdate-phải-copy-cả-category), [16](../study/16-luot-xem-top-san-pham-redis.md) |
| `seed/DataSeeder.java` | `existsByUsername`, `count()`, `save()` và dùng object được trả về để gán danh mục cho sản phẩm | [13](../study/13-du-lieu-mau-seeder.md) |
| `application.yml` | `ddl-auto: update`, `show-sql: true`, `format_sql: true`, `dialect: org.hibernate.dialect.MySQLDialect` | [02](../study/02-thuc-hanh-crud.md), [15.5](../study/15.5-reset-du-lieu-va-chay-lai-seeder.md) |

Đoạn đáng chú ý nhất là `update()` của `ProductService` (bài 11 Bước 5):

```java
public Product update(Long id, Product data) {
    Product product = findById(id);           // nạp bản đang có trong DB (managed)
    product.setName(data.getName());          // chép từng trường từ dữ liệu form sang
    product.setPrice(data.getPrice());
    product.setCategory(data.getCategory());  // quên dòng này → sửa xong mất danh mục
    product.setDescription(data.getDescription());
    return productRepository.save(product);   // id != 0 → merge → UPDATE
}
```

Mẫu "nạp bản cũ → chép trường → `save`" giữ lại các trường form **không gửi lên** (ở đây là `imagePath`). Nếu `save(data)` thẳng thì `image_path` sẽ bị ghi đè thành `NULL`.

Mô tả chức năng theo từng module: [Quản lý sản phẩm](../product/quan-ly-san-pham.md), [Quản lý danh mục](../category/quan-ly-danh-muc.md), [Đăng nhập, đăng ký](../user/dang-nhap-dang-ky.md), [Dữ liệu mẫu](../seed/du-lieu-mau.md).

Chỗ code khác bài học (liên quan tới JPA):

- Bài 02 khai báo `Long id` cho `Product`, code là `long id` (giải thích ở [trên](#vì-sao-productid-là-long-mà-categoryid-là-long)).
- Bài 06 Bước 5 thêm `ProductRepository.findByCategoryId(...)`, code **không có** hàm này.
- Bài 12 Bước 2 khai báo `role` là `@Column(nullable = false)`, code là `nullable = true` (thừa, vì `true` là mặc định).

## Tra cứu nhanh

| Annotation / Hàm | Của ai | Làm gì | Trả về / Ghi chú |
|---|---|---|---|
| `@Entity` | JPA | Đánh dấu class là entity (ứng với một bảng) | Bắt buộc có `@Id` và constructor rỗng |
| `@Table(name = "...")` | JPA | Đặt tên bảng | Không ghi → lấy theo tên class, đổi sang `snake_case` chữ thường |
| `@Id` | JPA | Khoá chính | |
| `@GeneratedValue(strategy = IDENTITY)` | JPA | DB tự cấp id (`AUTO_INCREMENT`) | `INSERT` chạy ngay lúc lưu để lấy id |
| `@Column(...)` | JPA | Chỉnh cột: `nullable`, `length`, `unique`, `name`... | Chi tiết: [Hỏi đáp](#column-vì-sao-có-trường-khai-báo-có-trường-không) |
| `@Transient` | JPA | Trường không lưu xuống DB | |
| `@ManyToOne` | JPA | Nhiều → một. Phía chủ, giữ khoá ngoại | Mặc định EAGER |
| `@JoinColumn(name = "category_id")` | JPA | Tên cột khoá ngoại | Đi cùng `@ManyToOne` |
| `@OneToMany(mappedBy = "category")` | JPA | Một → nhiều. Phía phụ | Mặc định LAZY. `mappedBy` = tên trường bên phía chủ |
| `@JsonIgnore` | Jackson | Bỏ trường khỏi JSON | Chặn vòng lặp ở quan hệ 2 chiều |
| `@Transactional` | Spring (`org.springframework.transaction.annotation`) | Gom các thao tác DB trong method thành một transaction | Project chưa dùng |
| `save(entity)` | Spring Data (`CrudRepository`) | Thêm (id `null`/`0`) hoặc sửa | Entity đã lưu. Dùng giá trị trả về |
| `saveAll(list)` | Spring Data | Lưu nhiều entity | `List<T>` |
| `findById(id)` | Spring Data | Tìm theo khoá chính | `Optional<T>` |
| `findAll()` | Spring Data | Lấy tất cả | `List<T>` (rỗng nếu không có) |
| `findAll(Sort.by("name"))` | Spring Data | Lấy tất cả, có sắp xếp | `List<T>` |
| `findAllById(ids)` | Spring Data | `WHERE id IN (...)` | `List<T>`, **không** đảm bảo đúng thứ tự `ids` |
| `existsById(id)` | Spring Data | Có dòng với id này không | `boolean` |
| `count()` | Spring Data | Đếm số dòng | `long` |
| `delete(entity)` | Spring Data | Xoá một entity | `void` |
| `deleteById(id)` | Spring Data | Xoá theo id | `void`. Từ Spring Data 3, id không tồn tại thì **lặng lẽ bỏ qua**, không báo lỗi |
| `getReferenceById(id)` | Spring Data (`JpaRepository`) | Lấy một "vỏ" chỉ có id, chưa chạy SQL | Tiện để gán quan hệ mà khỏi `SELECT` |
| `flush()`, `saveAndFlush(entity)` | Spring Data (`JpaRepository`) | Đẩy ngay các thay đổi đang chờ xuống DB | |
| `findByX`, `existsByX`, `countByX` | Spring Data (query method) | Spring đọc tên hàm để sinh SQL | Kiểu trả về do mình khai báo |
| `orElseThrow(() -> ...)` | Java (`java.util.Optional`) | Lấy giá trị ra khỏi `Optional`, rỗng thì ném lỗi | Giá trị bên trong |

## Bẫy thường gặp

| Triệu chứng | Nguyên nhân | Cách sửa |
|---|---|---|
| `Data too long for column 'description'` dù entity đã ghi `length = 2000` | Cột được tạo trước khi thêm `length`. `ddl-auto: update` không sửa cột cũ | `ALTER TABLE product MODIFY description VARCHAR(2000);` hoặc reset ([bài 15.5](../study/15.5-reset-du-lieu-va-chay-lai-seeder.md)) |
| `Field 'xxx' doesn't have a default value` khi thêm mới | Đã xoá/đổi tên trường trong entity, cột cũ `NOT NULL` vẫn còn trong bảng | Reset DB, hoặc `ALTER TABLE ... DROP COLUMN xxx;` |
| App không khởi động, log `No property 'xxx' found for type 'Product'` | Tên query method không khớp tên trường | Sửa tên hàm theo đúng tên trường (phân biệt hoa thường) |
| `Infinite recursion` / `StackOverflowError` khi trả JSON | Quan hệ 2 chiều, hai phía in JSON lẫn nhau | `@JsonIgnore` ở phía `@OneToMany` ([bài 06](../study/06-quan-he-model.md)) |
| Lưu xong mà `category_id` vẫn `NULL` | Chỉ thêm vào `category.getProducts()` (phía phụ), hoặc JSON thiếu `"category":{"id":1}` | Gán từ phía chủ: `product.setCategory(...)` |
| Sửa sản phẩm xong mất danh mục / mất ảnh | `update()` quên chép trường, hoặc `save()` thẳng object từ form | Nạp bản cũ rồi chép từng trường ([bài 11 Bước 5](../study/11-chon-danh-muc-cho-san-pham.md#bước-5--productserviceupdate-phải-copy-cả-category)) |
| `LazyInitializationException: could not initialize proxy - no Session` | Đọc trường LAZY ngoài phiên làm việc (seeder, thread khác, hoặc đã tắt open-in-view) | Nạp dữ liệu cần dùng ngay trong method `@Transactional`, hoặc viết query method riêng |
| `NullPointerException` ở `data.getId() == 0` | Đã đổi `Product.id` sang `Long` | Đổi điều kiện thành `data.getId() == null` |
| `POST /api/products` có `"id": 999` → 500, log `ObjectOptimisticLockingFailureException` | Hibernate 6.6: `merge` entity có id không tồn tại thì ném lỗi | Không gửi `id` khi thêm mới |
| Log có `HHH90000025: MySQLDialect does not need to be specified explicitly` | `application.yml` ghi `hibernate.dialect`, Hibernate 6 tự nhận diện được | Xoá dòng `dialect` (không bắt buộc, chỉ là cảnh báo) |
| `DataIntegrityViolationException` khi xoá danh mục | Còn sản phẩm trỏ tới danh mục đó (khoá ngoại) | Kiểm tra `getProducts().isEmpty()` trước khi xoá ([mysql.md](mysql.md#khoá-ngoại-category_id-và-chuyện-xoá-danh-mục)) |
| Xoá id không tồn tại mà không thấy lỗi 404 | `deleteById` của Spring Data 3 bỏ qua id không có | Gọi `findById(...).orElseThrow(...)` trước rồi `delete(entity)`, như `ProductService.delete()` |

## Đọc thêm

- [Spring Data JPA — Defining Query Methods](https://docs.spring.io/spring-data/jpa/reference/3.4/repositories/query-methods-details.html): toàn bộ từ khoá dùng được trong tên hàm (`Containing`, `Between`, `OrderBy`...).
- [Spring Data JPA — Persisting Entities](https://docs.spring.io/spring-data/jpa/reference/3.4/jpa/entity-persistence.html): cách `save()` xác định entity mới.
- [Spring Boot — Data access: JPA](https://docs.spring.io/spring-boot/3.4/reference/data/sql.html#data.sql.jpa-and-spring-data): `ddl-auto`, open-in-view, các cấu hình `spring.jpa.*`.
- [Hibernate 6.6 Migration Guide](https://docs.hibernate.org/orm/6.6/migration-guide/migration-guide.html): thay đổi hành vi `merge` nêu ở trên.
- Trong repo: [mysql.md](mysql.md) (phía DB), [spring-boot.md](spring-boot.md) (bean, dependency injection), [lombok.md](lombok.md) (bài 17: `@Getter`/`@Setter` cho entity, không dùng `@Data`).

## Hỏi đáp

Các câu hỏi phát sinh trong lúc học, chuyển từ sổ tay chung sang đây.

### `@Column`: vì sao có trường khai báo, có trường không?

> Hỏi ở: [Bài 15 — Bước 3: Thêm 2 trường vào `Product`](../study/15-anh-va-mo-ta-san-pham.md#bước-3--thêm-2-trường-vào-product). `@Column` xuất hiện lần đầu ở [Bài 02 — Bước 1: Entity](../study/02-thuc-hanh-crud.md#bước-1--entity-mô-tả-bảng-dữ-liệu).

#### Mọi trường của `@Entity` đều tự thành cột, không cần `@Column`

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

#### Những trường không dùng `@Column`

- **`@Id`**: khoá chính, đã có annotation riêng.
- **Quan hệ (`@ManyToOne`)**: dùng **`@JoinColumn(name = "category_id")`** thay cho `@Column`, vì cột này là khoá ngoại.
- **Kiểu nguyên thuỷ** (`long`, `int`, `double`...): không thể chứa `null`, nên Hibernate **tự thêm `NOT NULL`**. Ví dụ `long id` → `BIGINT NOT NULL`. Kiểu bọc (`Long`, `Double`) thì cho phép `NULL`.

#### ⚠️ 2 điều dễ hiểu lầm

1. **`nullable = false` chỉ là ràng buộc ở DB, không phải kiểm tra form.** Bỏ trống thì lỗi chỉ xuất hiện lúc ghi DB, dưới dạng lỗi SQL khó đọc. Thêm nữa, chuỗi rỗng `""` **vẫn lọt qua**, vì `""` khác `NULL`. Muốn báo lỗi đẹp ngay từ form thì dùng Bean Validation (`@NotBlank`, `@Size`...) kết hợp `@Valid`.
2. **`ddl-auto: update` không sửa cột đã có.** Nó chỉ **thêm** bảng, thêm cột mới. Nếu cột `description` đã được tạo là `VARCHAR(255)` rồi bạn mới thêm `length = 2000`, thì cột trong DB **vẫn là 255** và nhập mô tả dài sẽ bị lỗi `Data too long for column`. Tương tự khi đổi `nullable`, thêm `unique` cho cột cũ. Có 2 cách sửa: xoá sạch rồi tạo lại (`docker compose down -v`), hoặc tự sửa bằng SQL: `ALTER TABLE product MODIFY description VARCHAR(2000);`. Kiểm tra cột thật bằng lệnh `DESCRIBE product;` trong MySQL.
