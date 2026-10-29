# 08 — Làm giao diện web cho Category

Hiện `Category` **chỉ có REST API** (`/api/categories`). Trên nav đã có link "Danh mục" trỏ tới `/categories`, nhưng bấm vào sẽ **404** vì chưa có ai xử lý đường dẫn đó.

Mục tiêu bài này: làm cho Category đủ bộ **xem danh sách / thêm / sửa / xoá** trên trình duyệt — y hệt Product đã làm ở [05-giao-dien-thymeleaf.md](05-giao-dien-thymeleaf.md), và dùng luôn layout tổng từ [07-tach-giao-dien-fragment.md](07-tach-giao-dien-fragment.md).

> Bài này **không học cú pháp mới** — chủ yếu là *lặp lại khuôn cũ cho model thứ hai*. Làm xong bạn sẽ thấy rõ phần nào là "khuôn chung", phần nào phải chỉnh theo từng model.

## Ý tưởng

Đã có sẵn Entity → Repository → Service. Chỉ cần thêm **1 controller + 2 template**:

```
category/
├── Category.java              ✅ đã có
├── CategoryRepository.java    ✅ đã có
├── CategoryService.java       ✅ đã có (có cả updateCategory)
├── CategoryController.java    ✅ đã có  — REST, /api/categories, trả JSON
└── CategoryViewController.java  ⬅️ MỚI  — web,  /categories,     trả tên template

templates/
├── fragments/layout.html      ✅ đã có  — khung tổng (nav, css, footer)
├── products/…                 ✅ đã có
└── categories/
    ├── list.html              ⬅️ MỚI  — cái bảng
    └── form.html              ⬅️ MỚI  — cái form (dùng chung cho thêm & sửa)
```

Các đường dẫn sẽ có (giống hệt bên Product):

| Method | Đường dẫn | Làm gì | Trả về |
|--------|-----------|--------|--------|
| GET | `/categories` | Danh sách | `categories/list` |
| GET | `/categories/new` | Form thêm (rỗng) | `categories/form` |
| GET | `/categories/{id}/edit` | Form sửa (có sẵn dữ liệu) | `categories/form` |
| POST | `/categories/save` | Lưu (thêm hoặc sửa) | `redirect:/categories` |
| GET | `/categories/{id}/delete` | Xoá | `redirect:/categories` |

---

## ⚠️ Điểm khác Product cần nhớ trước khi code

Nhìn vào 2 entity:

```java
// Product.java
private long id;     // long NGUYÊN THUỶ → object mới có id = 0

// Category.java
private Long id;     // Long (class bọc) → object mới có id = null
```

Vì vậy chỗ phân biệt "thêm hay sửa":

| | Product | Category |
|---|---|---|
| Trong controller | `product.getId() == 0` | `category.getId() == null` |
| Trong template | `${product.id == 0}` | `${category.id == null}` |

> Nếu bê nguyên `== 0` sang Category: so sánh `Long` (đang là `null`) với số `0` → Java phải "mở hộp" giá trị null → **NullPointerException**. Đây là lỗi hay gặp nhất khi chép code giữa 2 model.

---

## Bước 1 — Tạo `CategoryViewController`

Tạo `src/main/java/com/example/demo/category/CategoryViewController.java`:

```java
package com.example.demo.category;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

// @Controller (KHÔNG phải @RestController): hàm trả về TÊN TEMPLATE, không phải JSON
@Controller
@RequestMapping("/categories")
public class CategoryViewController {

    private final CategoryService categoryService;

    // Dùng chung CategoryService với CategoryController (REST)
    public CategoryViewController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // GET /categories → trang danh sách
    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        return "categories/list";   // → templates/categories/list.html
    }

    // GET /categories/new → form rỗng
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("category", new Category());
        return "categories/form";
    }

    // GET /categories/5/edit → form có sẵn dữ liệu của category 5
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("category", categoryService.findById(id));
        return "categories/form";
    }

    // POST /categories/save → form gửi lên đây
    @PostMapping("/save")
    public String save(@ModelAttribute Category category) {
        // id là Long → category mới có id = null (KHÔNG phải 0 như Product)
        if (category.getId() == null) {
            categoryService.creatCategory(category);
        } else {
            categoryService.updateCategory(category.getId(), category);
        }
        return "redirect:/categories";
    }

    // GET /categories/5/delete → xoá rồi quay về danh sách
    @GetMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        categoryService.delete(id);
        return "redirect:/categories";
    }
}
```

Giải thích nhanh (chi tiết xem bài 05):
- `Model model` = "cái túi" để đưa dữ liệu sang template. `addAttribute("categories", ...)` → trong HTML dùng `${categories}`.
- `@ModelAttribute Category category` = Spring tự lấy các ô trong form (`name`, `id`) gán vào object `Category`.
- `redirect:/categories` = bảo trình duyệt chuyển sang trang danh sách (tránh bấm F5 bị gửi form lần 2).
- Tên hàm Service là `creatCategory` (thiếu chữ **e**) — đúng như trong `CategoryService.java` hiện tại. Nếu bạn sửa lại tên cho đúng chính tả thì nhớ sửa ở cả `CategoryController`.

---

## Bước 2 — Trang danh sách `categories/list.html`

Tạo thư mục `src/main/resources/templates/categories/` (nếu chưa có), rồi tạo `list.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{fragments/layout}">
<head>
    <title>Danh sách danh mục</title>
</head>
<body>
    <section layout:fragment="content">
        <h1>Danh mục</h1>
        <p><a class="btn" th:href="@{/categories/new}">+ Thêm danh mục</a></p>

        <table>
            <thead>
                <tr><th>ID</th><th>Tên</th><th>Số sản phẩm</th><th>Hành động</th></tr>
            </thead>
            <tbody>
                <tr th:each="c : ${categories}">
                    <td th:text="${c.id}">1</td>
                    <td th:text="${c.name}">Tên</td>
                    <!-- c.products là List<Product> nhờ @OneToMany (bài 06) -->
                    <td th:text="${c.products.size()}">0</td>
                    <td>
                        <a th:href="@{/categories/{id}/edit(id=${c.id})}">Sửa</a> |
                        <a th:href="@{/categories/{id}/delete(id=${c.id})}"
                           onclick="return confirm('Xoá danh mục này?')">Xoá</a>
                    </td>
                </tr>
                <tr th:if="${categories.isEmpty()}">
                    <td colspan="4">Chưa có danh mục nào.</td>
                </tr>
            </tbody>
        </table>
    </section>
</body>
</html>
```

Chú ý:
- `layout:decorate="~{fragments/layout}"` — layout của repo này nằm ở `templates/fragments/layout.html`, nên phải ghi `fragments/layout` (bài 07 ghi `~{layout}` vì lúc đó layout nằm thẳng trong `templates/`). Quy tắc: **đường dẫn tính từ `templates/`, bỏ đuôi `.html`**.
- Cột **Số sản phẩm** là điểm mới so với Product: nhờ quan hệ 2 chiều ở bài 06, từ một category ta đi ngược sang được danh sách product của nó. `@JsonIgnore` chỉ chặn khi xuất **JSON**, còn Thymeleaf vẫn đọc bình thường.

---

## Bước 3 — Form thêm/sửa `categories/form.html`

Tạo `src/main/resources/templates/categories/form.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{fragments/layout}">
<head>
    <title>Danh mục</title>
</head>
<body>
    <section layout:fragment="content">
        <!-- id == null → đang thêm mới; có id → đang sửa -->
        <h1 th:text="${category.id == null} ? 'Thêm danh mục' : 'Sửa danh mục'">Danh mục</h1>

        <form th:action="@{/categories/save}" th:object="${category}" method="post">
            <!-- Ô ẩn giữ id: thêm mới thì rỗng, sửa thì có số -->
            <input type="hidden" th:field="*{id}">

            <label>Tên danh mục</label>
            <input type="text" th:field="*{name}" required>

            <button type="submit">Lưu</button>
            <a th:href="@{/categories}">Huỷ</a>
        </form>
    </section>
</body>
</html>
```

- `th:object="${category}"` + `th:field="*{name}"` = ô input gắn với `category.name` (tự điền giá trị khi sửa, tự đặt `name="name"` để gửi lên).
- Ô ẩn `id`: khi thêm mới nó **rỗng** → Spring gán `null` cho `Long id` → controller hiểu là "thêm". Khi sửa nó có số → controller gọi `updateCategory`.
- Form chỉ có `name` vì `Category` chỉ có 1 trường để nhập. `products` không đưa vào form — quan hệ được gán từ phía Product (phía chủ, có `@JoinColumn`).

---

## Bước 4 — Chạy & kiểm tra

Vì chỉ thêm file `.java` và `.html` (không sửa `build.gradle`):

```bash
# Chế độ thường: phải build lại image
docker compose up --build

# Chế độ dev: app tự restart khi có file .java mới (xem bài 04)
docker compose -f docker-compose.dev.yml logs -f app-dev
```

Mở http://localhost:8093/categories rồi thử lần lượt:

1. Bấm **"Danh mục"** trên nav → ra trang danh sách (hết 404).
2. **+ Thêm danh mục** → nhập "Điện thoại" → Lưu → quay về danh sách, thấy dòng mới.
3. **Sửa** → form hiện sẵn "Điện thoại", tiêu đề là "Sửa danh mục" → đổi tên → Lưu.
4. **Xoá** một danh mục **chưa có sản phẩm** → biến mất khỏi bảng.

Kiểm tra nhanh bằng lệnh:
```bash
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8093/categories       # phải = 200
curl -s http://localhost:8093/categories/new | grep -c "<nav>"                  # phải = 1 (có layout)
curl -s http://localhost:8093/api/categories                                    # REST vẫn chạy bình thường
```

Dữ liệu thêm qua giao diện và qua API là **một** — vì cả hai controller dùng chung `CategoryService` và cùng một bảng `category`.

---

## So sánh: phần nào chép được, phần nào phải sửa

| Thứ | Product → Category | Ghi chú |
|-----|--------------------|---------|
| Khung controller (5 hàm, `@Controller`, `Model`, `redirect:`) | Chép được | Chỉ đổi tên |
| Tên hàm service | **Phải sửa** | `create/update` → `creatCategory/updateCategory` |
| Kiểm tra thêm hay sửa | **Phải sửa** | `== 0` → `== null` (do `long` vs `Long`) |
| `layout:decorate`, `layout:fragment` | Chép được | Cùng một layout |
| Các cột trong bảng, ô trong form | **Phải sửa** | Theo trường của entity |

Rút ra: khi thêm model thứ 3 (ví dụ `Tag`), bạn chỉ cần xem lại **3 dòng "Phải sửa"** ở trên.

---

## Lỗi thường gặp

| Triệu chứng | Nguyên nhân | Cách sửa |
|-------------|-------------|----------|
| Bấm "Danh mục" vẫn **404** | Chưa build lại, hoặc sai `@RequestMapping` | `docker compose up --build`; kiểm tra `@RequestMapping("/categories")` (có chữ **s**) |
| **500**, log ghi `NullPointerException` ở `save` | Chép `category.getId() == 0` từ Product | Đổi thành `category.getId() == null` |
| **500**, log ghi `Error resolving template [layout]` | Ghi `~{layout}` nhưng layout nằm trong `fragments/` | Dùng `layout:decorate="~{fragments/layout}"` |
| **500**, log ghi `Error resolving template [categories/list]` | Sai tên thư mục/file template | File phải ở `templates/categories/list.html` (khớp chuỗi controller trả về) |
| Trang hiện ra **không có** nav/CSS | Thiếu `layout:decorate` hoặc thiếu `xmlns:layout` | Xem lại 3 dòng đầu thẻ `<html>` |
| **Xoá** danh mục → **500**, log ghi `foreign key constraint fails` | Danh mục còn product trỏ tới (`product.category_id`) | MySQL không cho xoá "cha" khi còn "con". Xoá/đổi danh mục của các product đó trước — xem bài tập 2 |

> Gặp trang *Whitelabel Error Page* (500) thì lý do thật luôn nằm trong log:
> ```bash
> docker logs --tail 100 demo-app          # chế độ thường
> docker logs --tail 100 demo-app-dev      # chế độ dev
> ```

## Bài tập

1. **Chọn danh mục khi thêm product**: trong `products/form.html` thêm `<select th:field="*{category}">` liệt kê các category (gợi ý: `ProductViewController` cần nhận thêm `CategoryService` qua constructor và đưa `categoryService.findAll()` vào model). Nhớ: `ProductService.update()` hiện **không copy `category`**, phải bổ sung thì sửa mới giữ được danh mục.
2. **Xoá an toàn**: trong `CategoryService.delete()`, kiểm tra `category.getProducts().isEmpty()`; nếu còn product thì `throw new RuntimeException("Danh mục còn sản phẩm, không xoá được")` thay vì để MySQL báo lỗi khoá ngoại khó hiểu.
3. **Cột Danh mục ở trang sản phẩm**: thêm cột vào `products/list.html` hiển thị `${p.category?.name}` (dấu `?.` để không lỗi khi product chưa có danh mục).
4. **Xem sản phẩm theo danh mục**: thêm trang `GET /categories/{id}` liệt kê `category.products` của một danh mục.

➡️ Quay lại tổng quan: [../README.md](../README.md)
