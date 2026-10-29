# 05 — Giao diện web với Thymeleaf

Đến giờ bạn đã có REST API (trả JSON). Giờ ta làm **giao diện web** để xem/thêm/sửa/xoá sản phẩm bằng trình duyệt — toàn bộ viết bằng Java/Spring, **không cần JavaScript**.

## Thymeleaf là gì? Khác REST thế nào?

- **REST API** (`@RestController`) trả về **dữ liệu JSON** cho máy/JS đọc.
- **Thymeleaf** giúp server **render ra trang HTML** hoàn chỉnh rồi gửi cho trình duyệt hiển thị.

```
Trình duyệt  ──GET /products──►  @Controller  ──lấy data──►  ProductService
                                     │
                                     │ đưa data vào "model"
                                     ▼
                              Template Thymeleaf (list.html)
                                     │ trộn data + HTML
                                     ▼
                          Trang HTML hoàn chỉnh ──► trả về trình duyệt
```

Điểm mấu chốt: dùng `@Controller` (KHÔNG phải `@RestController`). Method trả về **tên file template** (chuỗi), không phải dữ liệu.

| | `@RestController` (đã học) | `@Controller` (bài này) |
|---|---|---|
| Trả về | Dữ liệu (JSON) | Tên template HTML |
| Ví dụ return | `return product;` | `return "products/list";` |
| Dùng cho | API cho máy/JS | Trang web cho người xem |

> Ta **giữ nguyên** REST API cũ ở `/api/products`, và thêm giao diện web ở `/products`. Hai thứ song song, không đụng nhau.

---

## Bước 1 — Thêm thư viện Thymeleaf

Mở `build.gradle`, thêm dòng sau vào khối `dependencies { ... }`:

```gradle
implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
```

Chỉ cần thêm dependency này là Spring Boot tự cấu hình Thymeleaf, và tự tìm template trong thư mục `src/main/resources/templates/`.

---

## Bước 2 — Controller trả về trang web

Tạo file `src/main/java/com/example/demo/product/ProductViewController.java`:

```java
package com.example.demo.product;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller                       // KHÁC @RestController: trả về tên template, không phải JSON
@RequestMapping("/products")
public class ProductViewController {

    private final ProductService service;

    public ProductViewController(ProductService service) {
        this.service = service;
    }

    // Danh sách:  GET /products
    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", service.findAll()); // đưa data vào "model" để template dùng
        return "products/list";                            // -> templates/products/list.html
    }

    // Form thêm mới:  GET /products/new
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("product", new Product());      // object rỗng để form bind vào
        return "products/form";
    }

    // Form sửa:  GET /products/edit/1
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("product", service.findById(id));
        return "products/form";
    }

    // Lưu (dùng chung cho cả thêm & sửa):  POST /products/save
    @PostMapping("/save")
    public String save(@ModelAttribute Product product) {
        if (product.getId() == 0) {          // id = 0 nghĩa là sản phẩm mới
            service.create(product);
        } else {
            service.update(product.getId(), product);
        }
        return "redirect:/products";         // lưu xong quay lại danh sách
    }

    // Xoá:  GET /products/delete/1
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "redirect:/products";
    }
}
```

**Ghi nhớ 3 khái niệm mới:**
- `Model model` — cái "túi" để đưa dữ liệu từ Java sang template.
- `@ModelAttribute Product product` — Spring tự gom dữ liệu từ các ô input của form thành object `Product`.
- `return "redirect:/products"` — sau khi lưu/xoá thì chuyển hướng, tránh gửi lại form khi refresh.

---

## Bước 3 — Tạo template HTML

Template đặt trong `src/main/resources/templates/`. Tạo thư mục con `products/`.

### 3a. Trang danh sách — `templates/products/list.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Danh sách sản phẩm</title>
    <style>
        body { font-family: sans-serif; max-width: 700px; margin: 40px auto; }
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #ccc; padding: 8px 12px; text-align: left; }
        th { background: #f4f4f4; }
        a.btn { display: inline-block; padding: 6px 12px; background: #2563eb;
                color: white; text-decoration: none; border-radius: 4px; }
    </style>
</head>
<body>
    <h1>Sản phẩm</h1>
    <p><a class="btn" th:href="@{/products/new}">+ Thêm sản phẩm</a></p>

    <table>
        <thead>
            <tr><th>ID</th><th>Tên</th><th>Giá</th><th>Hành động</th></tr>
        </thead>
        <tbody>
            <!-- th:each lặp qua từng phần tử trong danh sách "products" -->
            <tr th:each="p : ${products}">
                <td th:text="${p.id}">1</td>
                <td th:text="${p.name}">Tên</td>
                <td th:text="${p.price}">0</td>
                <td>
                    <a th:href="@{/products/edit/{id}(id=${p.id})}">Sửa</a> |
                    <a th:href="@{/products/delete/{id}(id=${p.id})}"
                       onclick="return confirm('Xoá sản phẩm này?')">Xoá</a>
                </td>
            </tr>
            <!-- Hiện dòng này khi danh sách rỗng -->
            <tr th:if="${products.isEmpty()}">
                <td colspan="4">Chưa có sản phẩm nào.</td>
            </tr>
        </tbody>
    </table>
</body>
</html>
```

### 3b. Form thêm/sửa — `templates/products/form.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Sản phẩm</title>
    <style>
        body { font-family: sans-serif; max-width: 500px; margin: 40px auto; }
        label { display: block; margin: 12px 0 4px; }
        input { width: 100%; padding: 8px; box-sizing: border-box; }
        button { margin-top: 16px; padding: 8px 16px; background: #2563eb;
                 color: white; border: none; border-radius: 4px; cursor: pointer; }
    </style>
</head>
<body>
    <h1 th:text="${product.id == 0} ? 'Thêm sản phẩm' : 'Sửa sản phẩm'">Sản phẩm</h1>

    <!-- th:object gắn form với object "product". th:field tự điền value & name -->
    <form th:action="@{/products/save}" th:object="${product}" method="post">
        <!-- Ô ẩn giữ id để biết đang sửa (id>0) hay thêm mới (id=0) -->
        <input type="hidden" th:field="*{id}">

        <label>Tên sản phẩm</label>
        <input type="text" th:field="*{name}" required>

        <label>Giá</label>
        <input type="number" step="1000" th:field="*{price}">

        <button type="submit">Lưu</button>
        <a th:href="@{/products}">Huỷ</a>
    </form>
</body>
</html>
```

---

## Bước 4 — Chạy & xem

Vì đã sửa `build.gradle` (thêm dependency), phải build lại:

```bash
docker compose up --build
```

Rồi mở trình duyệt: **http://localhost:8093/products**

Bạn sẽ thấy trang danh sách. Bấm **"+ Thêm sản phẩm"** → điền form → **Lưu** → quay lại thấy sản phẩm mới. Thử **Sửa**, **Xoá**.

> 💡 Đang làm giao diện, nên dùng **chế độ hot reload** để sửa HTML thấy ngay:
> `docker compose -f docker-compose.dev.yml up` (sửa file `.html` chỉ cần refresh trình duyệt, không cần build lại).

---

## Cú pháp Thymeleaf cần nhớ

| Cú pháp | Ý nghĩa | Ví dụ |
|---------|---------|-------|
| `th:text="${...}"` | Đổ dữ liệu vào nội dung thẻ | `<td th:text="${p.name}">` |
| `th:each="p : ${list}"` | Lặp qua danh sách | `<tr th:each="p : ${products}">` |
| `th:if="${...}"` | Chỉ hiện thẻ khi điều kiện đúng | `<tr th:if="${products.isEmpty()}">` |
| `th:href="@{/path}"` | Tạo đường dẫn | `th:href="@{/products/new}"` |
| `@{/path/{id}(id=${p.id})}` | Đường dẫn có tham số | `@{/products/edit/{id}(id=${p.id})}` |
| `th:object="${obj}"` | Gắn form với 1 object | `<form th:object="${product}">` |
| `th:field="*{name}"` | Nối ô input với field của object | `<input th:field="*{name}">` |

- `${...}` : lấy biến từ model.
- `*{...}` : lấy field của object đang gắn bằng `th:object` (viết gọn thay cho `${product.name}`).
- `@{...}` : tạo URL (tự thêm context path nếu có).

---

## Lỗi thường gặp

| Triệu chứng | Nguyên nhân | Cách sửa |
|-------------|-------------|----------|
| Lỗi 500 khi mở trang list | Sai vị trí tham số trong URL Thymeleaf: `@{/products/{id}(id=${p.id})/edit}` | Tham số `(...)` phải ở **cuối cùng**: `@{/products/{id}/edit(id=${p.id})}` |
| Bấm Sửa/Xoá bị 404 | Đường dẫn trong template không khớp route ở controller (vd template `/edit/{id}` nhưng controller `/{id}/edit`) | Cho 2 bên giống hệt nhau |
| Sửa file `.html` mà không thấy đổi (dev mode) | Lệnh chạy chỉ biên dịch Java, không copy template | Dùng `gradle classes --continuous` (xem [04-hot-reload.md](04-hot-reload.md)) |
| `th:href` bị in nguyên văn ra HTML | Thẻ `<html>` không có `xmlns:th`, hoặc có 2 thẻ `<html>` | Chỉ để **một** thẻ: `<html lang="en" xmlns:th="http://www.thymeleaf.org">` |

> ⚠️ Lưu ý về URL: `{id}` trong đường dẫn là "chỗ trống", còn `(id=${p.id})` ở cuối mới là "giá trị điền vào chỗ trống đó". Vì tên trùng `{id}` nên Thymeleaf ghép vào path → ra `/products/1/edit`.

## Bài tập tự luyện

1. **Thêm cột mô tả**: nếu đã làm bài tập `description` ở [02-thuc-hanh-crud.md](02-thuc-hanh-crud.md), thêm cột đó vào bảng và form.
2. **Định dạng giá tiền**: dùng `th:text="${#numbers.formatDecimal(p.price, 0, 'COMMA', 0, 'POINT')}"` để hiện `500,000` thay vì `500000.0`.
3. **Trang chủ dẫn tới danh sách**: sửa `HelloController` hoặc thêm link ở `/` trỏ tới `/products`.
4. **Thông báo sau khi lưu**: hiện dòng "Đã lưu thành công" trên trang danh sách (gợi ý: dùng `RedirectAttributes.addFlashAttribute`).

➡️ Quay lại tổng quan các file: [../README.md](../README.md)
