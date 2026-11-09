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

### 📖 Tra cứu: các hàm có sẵn dùng trong bước này

Các hàm dưới đây **không phải do mình viết**. `findAll`, `findById`, `save`, `deleteById` có sẵn trong thư viện **Spring Data JPA**: `ProductRepository` được thừa hưởng chúng nhờ dòng `extends JpaRepository<Product, Long>` ở Bước 2. `Optional` và các class lỗi (`RuntimeException`...) thì có sẵn trong Java. Cách tự nhận biết và tự tra một hàm lạ: xem [sổ tay kiến thức chung](common_knowledge.md#hàm-có-sẵn-của-thư-viện-ví-dụ-opsforzset-ở-đâu-ra-làm-sao-biết-nó-làm-gì).

| Hàm | Của ai | Làm gì | Trả về |
|---|---|---|---|
| `repository.findAll()` | Spring Data JPA, thừa kế từ `JpaRepository` | Lấy mọi dòng trong bảng: `SELECT ... FROM product` | `List<Product>`. Bảng rỗng thì list rỗng, **không** phải `null` |
| `repository.findById(id)` | Spring Data JPA, thừa kế từ `JpaRepository` | Tìm 1 dòng theo khoá chính: `SELECT ... WHERE id = ?` | `Optional<Product>`: một "hộp" có thể chứa sản phẩm, hoặc rỗng nếu không tìm thấy. Hàm này không trả `null` |
| `.orElseThrow(() -> new RuntimeException("..."))` | Java `Optional` | Mở "hộp" vừa nhận từ `findById`. Có sản phẩm thì lấy ra. Hộp rỗng thì **ném** lỗi do lambda tạo ra. `() -> new RuntimeException(...)` là một lambda không có tham số, chỉ **chạy khi hộp rỗng** | `Product`, chắc chắn khác `null` |
| `new RuntimeException("...")` | Java | Tạo một object lỗi kèm câu thông báo. Bản thân `new` chỉ **tạo**, chưa ném. Phải có `throw` (hoặc `orElseThrow` ném giùm) thì lỗi mới bay ra. Loại lỗi này không bắt buộc khai báo `throws` ở đầu hàm | object `RuntimeException` |
| `new IllegalArgumentException("...")` | Java | Như trên. Đây là một loại `RuntimeException` riêng, mang nghĩa "dữ liệu đầu vào không hợp lệ". Dùng kèm `throw` | object `IllegalArgumentException` |
| `repository.save(product)` | Spring Data JPA, thừa kế từ `JpaRepository` | Lưu entity. `id` chưa có giá trị (`null`) thì coi là bản ghi mới → `INSERT`, DB tự sinh id. `id` đã có giá trị thì coi là bản ghi cũ → cập nhật (`UPDATE`) | `Product` đã lưu, có `id` do DB sinh. Nên dùng object trả về này, đừng dùng lại object truyền vào |
| `repository.deleteById(id)` | Spring Data JPA, thừa kế từ `JpaRepository` | Tìm bản ghi theo id, có thì xoá (`DELETE`). Không có id đó thì **lặng lẽ bỏ qua**, không báo lỗi | không trả gì (`void`) |

Đọc dòng gọi nối nhau `repository.findById(id).orElseThrow(...)` từ trái sang phải:

1. `repository.findById(id)` → trả về `Optional<Product>` (cái "hộp").
2. `.orElseThrow(...)` gọi trên cái hộp đó → trả về `Product`, hoặc ném lỗi nếu hộp rỗng.

> So với PHP: hàm tìm theo id trong PHP thường trả `null` khi không thấy, và rất dễ quên kiểm tra. Java bọc kết quả trong `Optional` để **buộc** bạn quyết định luôn: không thấy thì làm gì (ở đây là ném lỗi).

- Vì sao `findAll()` trả đúng `List<Product>`? Vì ở Bước 2 bạn khai báo `JpaRepository<Product, Long>`: `Product` là kiểu entity, `Long` là kiểu khoá chính. Spring Data điền 2 kiểu này vào mọi hàm thừa kế, nên `findById` nhận `Long`, `save` nhận và trả `Product`.
- Nếu sau này `id` đổi sang kiểu `long` (viết thường), thì `save` hiểu "chưa có id" là `id == 0` thay vì `null`.
- `count()` (có trong comment ở Bước 2, bài này chưa dùng): đếm số dòng trong bảng, trả về `long`.

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
