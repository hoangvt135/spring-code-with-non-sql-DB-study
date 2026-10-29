# 06 — Quan hệ giữa các Model (1:N)

Đến giờ mỗi entity đứng riêng. Thực tế dữ liệu **liên kết với nhau**. Bài này ta tạo `Category` (danh mục) và liên kết **1 danh mục có nhiều sản phẩm** (1:N) với `Product`.

## 1. Quan hệ 1:N là gì?

> **Một** `Category` có **nhiều** `Product`. Ngược lại, **một** `Product` thuộc về **một** `Category`.

```
   Category "Bàn phím"  (1)
        │
        ├──► Product "Keychron K2"   (N)
        ├──► Product "Logitech MX"
        └──► Product "AKKO 3068"
```

Trong DB, quan hệ này thể hiện bằng một **khoá ngoại (foreign key)**: bảng `product` có thêm cột `category_id` trỏ tới `id` của bảng `category`.

```
Bảng category                 Bảng product
┌────┬──────────┐             ┌────┬───────────┬───────┬─────────────┐
│ id │ name     │             │ id │ name      │ price │ category_id │
├────┼──────────┤             ├────┼───────────┼───────┼─────────────┤
│ 1  │ Bàn phím │◄────────────┤ 10 │ Keychron  │ 2000  │ 1           │
│ 2  │ Chuột    │      │      │ 11 │ Logitech  │ 1500  │ 1           │
└────┴──────────┘      └──────┤ 12 │ MX Master │ 2500  │ 2           │
                              └────┴───────────┴───────┴─────────────┘
```

## 2. Hai phía của quan hệ: "chủ" và "phụ"

Đây là khái niệm **quan trọng nhất** phải hiểu:

| Phía | Entity | Annotation | Ý nghĩa |
|------|--------|-----------|---------|
| **Chủ (owning side)** | `Product` | `@ManyToOne` | Phía **giữ khoá ngoại** `category_id`. Nhiều product → một category |
| **Phụ (inverse side)** | `Category` | `@OneToMany(mappedBy="category")` | Phía chỉ "soi chiếu" lại. Một category → danh sách product |

- **`@ManyToOne`** đặt ở `Product` vì cột `category_id` nằm ở **bảng product**. Đây là phía thật sự quyết định quan hệ.
- **`@OneToMany(mappedBy="category")`** đặt ở `Category`. Chữ `mappedBy="category"` nghĩa là: "quan hệ này đã được khai báo ở field tên `category` bên `Product` rồi, tôi chỉ nhìn theo, KHÔNG tạo thêm cột/bảng nào".

> 🔑 Quy tắc vàng cho 1:N: **khoá ngoại luôn nằm ở phía "nhiều" (N)** → nên `@ManyToOne` là phía chủ.

---

## Bước 1 — Tạo entity `Category`

Tạo package mới `category`, file `src/main/java/com/example/demo/category/Category.java`:

```java
package com.example.demo.category;

import com.example.demo.product.Product;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // Phía "phụ": một category có nhiều product.
    // mappedBy = "category" -> trỏ tới field 'category' bên Product (bước 2).
    // @JsonIgnore: KHÔNG in danh sách products khi trả JSON category (tránh lặp vô tận - xem bước 3).
    @OneToMany(mappedBy = "category")
    @JsonIgnore
    private List<Product> products = new ArrayList<>();

    public Category() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<Product> getProducts() { return products; }
    public void setProducts(List<Product> products) { this.products = products; }
}
```

---

## Bước 2 — Thêm quan hệ vào `Product`

Mở `product/Product.java`, thêm field `category` (phía chủ, giữ khoá ngoại):

```java
// ...thêm import ở đầu file:
import com.example.demo.category.Category;

// ...thêm field vào trong class Product:
    @ManyToOne                              // nhiều product -> một category
    @JoinColumn(name = "category_id")       // tên cột khoá ngoại trong bảng product
    private Category category;

// ...thêm getter/setter:
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
```

Sau khi thêm, `Product.java` đầy đủ trông như sau (phần thêm được đánh dấu 👈):

```java
package com.example.demo.product;

import com.example.demo.category.Category;   // 👈 thêm
import jakarta.persistence.*;

@Entity
@Table(name = "Product")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String name;

    private Double price;

    @ManyToOne                               // 👈 thêm
    @JoinColumn(name = "category_id")        // 👈 thêm
    private Category category;                // 👈 thêm

    public Product() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Category getCategory() { return category; }        // 👈 thêm
    public void setCategory(Category category) { this.category = category; }  // 👈 thêm
}
```

Khi chạy, Hibernate tự thêm cột `category_id` vào bảng `product` (nhờ `ddl-auto: update`).

---

## Bước 3 — ⚠️ Bẫy lớn nhất: vòng lặp JSON vô tận

Khi trả JSON, nếu **cả hai phía đều in ra nhau** thì sẽ lặp vô tận rồi crash:

```
Product → category → products[] → product → category → products[] → ... 💥
(StackOverflowError / Infinite recursion)
```

**Cách xử lý** (bài này dùng cách đơn giản nhất): đặt `@JsonIgnore` lên field `products` bên `Category` (đã làm ở bước 1). Kết quả:

- GET product → **có** hiện `category` của nó ✅ (trực quan: sản phẩm thuộc danh mục nào)
- GET category → **không** kèm danh sách `products` (tránh lặp). Muốn xem product của 1 category thì dùng API riêng ở bước 5.

> Các cách khác (nâng cao, tự tìm hiểu sau): `@JsonManagedReference`/`@JsonBackReference`, hoặc dùng **DTO** để tách entity khỏi dữ liệu trả về.

---

## Bước 4 — Repository, Service, Controller cho `Category`

### 4a. `category/CategoryRepository.java`
```java
package com.example.demo.category;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
```

### 4b. `category/CategoryService.java`
```java
package com.example.demo.category;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    public List<Category> findAll() { return repository.findAll(); }

    public Category findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục id=" + id));
    }

    public Category create(Category category) { return repository.save(category); }

    public void delete(Long id) { repository.deleteById(id); }
}
```

### 4c. `category/CategoryController.java`
```java
package com.example.demo.category;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<Category> getAll() { return service.findAll(); }

    @GetMapping("/{id}")
    public Category getOne(@PathVariable Long id) { return service.findById(id); }

    @PostMapping
    public Category create(@RequestBody Category category) { return service.create(category); }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) { service.delete(id); }
}
```

---

## Bước 5 — Truy vấn theo quan hệ: lấy product của 1 category

Đây là lúc thấy sức mạnh của quan hệ. Thêm vào `product/ProductRepository.java`:

```java
import java.util.List;

// ...trong interface ProductRepository:
    // Spring tự sinh câu SQL: WHERE category_id = ?
    List<Product> findByCategoryId(Long categoryId);
```

Rồi thêm API vào `product/ProductController.java`:

```java
    // GET /api/products/by-category/1  -> mọi product thuộc category id=1
    @GetMapping("/by-category/{categoryId}")
    public List<Product> getByCategory(@PathVariable Long categoryId) {
        return service.findByCategory(categoryId);   // thêm method này ở service (dưới)
    }
```

Và trong `product/ProductService.java`:

```java
    public List<Product> findByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }
```

> ⚠️ Dùng **đúng tên field repository bạn đã đặt** trong `ProductService`. Nếu bạn đặt là `repository` thì viết `repository.findByCategoryId(...)`; nếu là `productRepository` thì viết `productRepository.findByCategoryId(...)`.

---

## Bước 6 — Chạy & thử

```bash
docker compose up --build          # hoặc dev: docker compose -f docker-compose.dev.yml up
```

```bash
# 1) Tạo một danh mục
curl -X POST http://localhost:8093/api/categories \
  -H "Content-Type: application/json" \
  -d '{"name":"Bàn phím"}'
# -> trả về danh mục có "id": 1

# 2) Tạo product THUỘC danh mục id=1  (chú ý field "category")
curl -X POST http://localhost:8093/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Keychron K2","price":2000000,"category":{"id":1}}'

# 3) Xem product -> thấy kèm category của nó
curl http://localhost:8093/api/products/1

# 4) Lấy mọi product thuộc category id=1
curl http://localhost:8093/api/products/by-category/1
```

Kiểm tra cột khoá ngoại trong DB:
```bash
docker exec -it demo-mysql mysql -u demo_user -pdemo_pass demo_db \
  -e "SELECT p.id, p.name, p.category_id, c.name AS category FROM product p LEFT JOIN category c ON p.category_id = c.id;"
```

Bạn sẽ thấy cột `category_id` được điền → quan hệ đã hoạt động! 🎉

---

## Cú pháp & khái niệm cần nhớ

| Annotation | Đặt ở | Ý nghĩa |
|-----------|-------|---------|
| `@ManyToOne` | Phía "nhiều" (`Product`) | Nhiều bản ghi trỏ về một. Phía **giữ khoá ngoại** |
| `@JoinColumn(name="category_id")` | Cạnh `@ManyToOne` | Đặt tên cột khoá ngoại |
| `@OneToMany(mappedBy="category")` | Phía "một" (`Category`) | Danh sách bản ghi con. `mappedBy` = tên field bên phía chủ |
| `@JsonIgnore` | Field dễ gây lặp | Bỏ field đó khỏi JSON để tránh đệ quy vô tận |

---

## Lỗi thường gặp

| Triệu chứng | Nguyên nhân | Cách sửa |
|-------------|-------------|----------|
| `Infinite recursion` / `StackOverflowError` khi gọi API | Hai phía in JSON lẫn nhau | Thêm `@JsonIgnore` (hoặc `@JsonBackReference`) một phía |
| `mappedBy` báo lỗi / tạo bảng phụ lạ | Tên trong `mappedBy` không trùng field bên `Product` | `mappedBy="category"` phải khớp đúng tên field `private Category category` |
| Tạo product kèm category nhưng `category_id` = null | JSON gửi sai, thiếu `"category":{"id":...}` | Gửi đúng dạng `{"category":{"id":1}}` và category id đó phải tồn tại |
| Không import được `Category` | Khác package | Thêm `import com.example.demo.category.Category;` |

## Bài tập tự luyện

1. **Hiện tên danh mục trên giao diện Thymeleaf**: thêm cột "Danh mục" vào `products/list.html` bằng `th:text="${p.category?.name}"` (dấu `?` tránh lỗi khi category null).
2. **Chọn danh mục khi thêm product**: trong `form.html` thêm thẻ `<select>` liệt kê category (gợi ý: controller đưa danh sách category vào model).
3. **Quan hệ N:N**: thử tạo `Tag` (nhãn) với quan hệ nhiều-nhiều tới `Product` bằng `@ManyToMany`.
4. **Xoá an toàn**: thử xoá một category đang có product → xem lỗi ràng buộc khoá ngoại, rồi tìm hiểu `orphanRemoval` / `cascade`.

➡️ Quay lại tổng quan: [../README.md](../README.md)
