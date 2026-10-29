# 02 — Thực hành: Xây API CRUD cho `Product`

Mục tiêu: tự tay tạo đủ API để **thêm / xem / sửa / xoá** sản phẩm.
Bạn sẽ tạo 4 tầng theo đúng thứ tự: **Entity → Repository → Service → Controller**.

> 💡 Cách học tốt nhất: **tự gõ lại** từng đoạn code (đừng copy toàn bộ), đọc phần giải thích, rồi chạy thử ngay.

Tất cả file nằm trong `src/main/java/com/example/demo/`.

---

## Bước 1 — Entity: mô tả bảng dữ liệu

Tạo file `product/Product.java`:

```java
package com.example.demo.product;

import jakarta.persistence.*;

@Entity                 // Đánh dấu: class này ↔ một bảng trong DB
@Table(name = "product")
public class Product {

    @Id                                                  // Khoá chính
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // DB tự tăng id
    private Long id;

    @Column(nullable = false)
    private String name;

    private Double price;

    // JPA bắt buộc có constructor rỗng
    public Product() {}

    // Getter & Setter — Spring dùng để đọc/ghi dữ liệu
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}
```

**Điều gì xảy ra?** Khi app khởi động, nhờ `ddl-auto: update` (trong `application.yml`),
Hibernate sẽ tự tạo bảng `product` với cột `id`, `name`, `price` trong MySQL. Bạn không cần viết SQL `CREATE TABLE`.

---

## Bước 2 — Repository: thao tác với DB

Tạo file `product/ProductRepository.java`:

```java
package com.example.demo.product;

import org.springframework.data.jpa.repository.JpaRepository;

// Chỉ cần kế thừa JpaRepository<Kiểu Entity, Kiểu khoá chính>
public interface ProductRepository extends JpaRepository<Product, Long> {
    // Không cần viết gì thêm! Bạn đã có sẵn:
    //   save(), findAll(), findById(), deleteById(), count()...
}
```

**Kỳ diệu ở đâu?** Spring Data JPA tự sinh code hiện thực cho interface này lúc chạy.
Bạn chỉ khai báo, không cần viết SQL cho các thao tác cơ bản.

---

## Bước 3 — Service: chứa logic nghiệp vụ

Tạo file `product/ProductService.java`:

```java
package com.example.demo.product;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repository;

    // Spring tự "tiêm" (inject) ProductRepository vào đây — gọi là Dependency Injection
    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<Product> findAll() {
        return repository.findAll();
    }

    public Product findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm id=" + id));
    }

    public Product create(Product product) {
        // Ví dụ logic nghiệp vụ: chặn giá âm
        if (product.getPrice() != null && product.getPrice() < 0) {
            throw new IllegalArgumentException("Giá không được âm");
        }
        return repository.save(product);
    }

    public Product update(Long id, Product data) {
        Product product = findById(id);      // đảm bảo tồn tại
        product.setName(data.getName());
        product.setPrice(data.getPrice());
        return repository.save(product);     // save = update khi id đã có
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
```

**Điểm quan trọng:** logic nghiệp vụ (như "giá không âm") nằm ở đây, KHÔNG nằm ở controller.

---

## Bước 4 — Controller: mở API ra ngoài

Tạo file `product/ProductController.java`:

```java
package com.example.demo.product;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/products")   // Mọi API dưới đây bắt đầu bằng /api/products
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    // READ - danh sách:  GET /api/products
    @GetMapping
    public List<Product> getAll() {
        return service.findAll();
    }

    // READ - một cái:    GET /api/products/1
    @GetMapping("/{id}")
    public Product getOne(@PathVariable Long id) {
        return service.findById(id);
    }

    // CREATE:            POST /api/products   (kèm JSON trong body)
    @PostMapping
    public Product create(@RequestBody Product product) {
        return service.create(product);
    }

    // UPDATE:            PUT /api/products/1
    @PutMapping("/{id}")
    public Product update(@PathVariable Long id, @RequestBody Product product) {
        return service.update(id, product);
    }

    // DELETE:            DELETE /api/products/1
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
```

Các annotation cần nhớ:
- `@PathVariable` → lấy giá trị từ đường dẫn (số `1` trong `/api/products/1`).
- `@RequestBody` → chuyển JSON trong body request thành object `Product`.

---

## Bước 5 — Chạy & thử

Khởi động lại app (nếu đang chạy Docker):

```bash
docker compose up --build
```

Rồi thử các lệnh sau (dùng `curl`, hoặc dùng Postman cho dễ):

```bash
# CREATE: tạo 2 sản phẩm
curl -X POST http://localhost:8093/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Bàn phím","price":500000}'

curl -X POST http://localhost:8093/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Chuột","price":250000}'

# READ: xem tất cả
curl http://localhost:8093/api/products

# READ: xem sản phẩm id=1
curl http://localhost:8093/api/products/1

# UPDATE: sửa sản phẩm id=1
curl -X PUT http://localhost:8093/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Bàn phím cơ","price":800000}'

# DELETE: xoá sản phẩm id=2
curl -X DELETE http://localhost:8093/api/products/2
```

Kiểm tra dữ liệu thật trong MySQL:

```bash
docker exec -it demo-mysql mysql -u demo_user -pdemo_pass demo_db \
  -e "SELECT * FROM product;"
```

🎉 Nếu thấy dữ liệu trong bảng → bạn đã hoàn thành một luồng CRUD hoàn chỉnh từ HTTP xuống tận DB!

---

## Bài tập tự luyện (nâng dần)

1. **Thêm trường `description`** vào `Product` (nhớ getter/setter). Chạy lại và xem cột mới xuất hiện trong DB.
2. **Validation**: thêm `@NotBlank` lên `name` trong entity và `@Valid` trước `@RequestBody` ở controller. Thử tạo sản phẩm thiếu tên → xem lỗi.
3. **Tìm kiếm theo tên**: thêm vào repository dòng
   `List<Product> findByNameContaining(String keyword);`
   rồi tạo API `GET /api/products/search?keyword=...`. (Spring tự sinh câu query từ tên method!)
4. **Xử lý lỗi đẹp hơn**: khi không tìm thấy id, trả về HTTP 404 thay vì 500. Gợi ý: tạo class với `@RestControllerAdvice`.

➡️ Cần lệnh nhanh? Xem [03-cheatsheet.md](03-cheatsheet.md).
