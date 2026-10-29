# 11 — Chọn danh mục cho sản phẩm (vòng lặp `th:each` trong `<select>`)

Ở bài 06 ta đã nối `Product` với `Category` (1 danh mục có nhiều sản phẩm), nhưng tới giờ **chỉ gán được danh mục qua REST API**. Form sản phẩm trên trình duyệt chưa có ô chọn danh mục.

Mục tiêu bài này: form thêm/sửa sản phẩm có một ô **dropdown liệt kê mọi danh mục**. Khi sửa thì tự chọn sẵn danh mục hiện tại, bấm Lưu thì danh mục được lưu vào DB.

> Kiến thức mới: dùng `th:each` để lặp ra các `<option>`, và `th:selected` để chọn sẵn một option. Ngoài ra: một controller dùng **hai** Service, và `@RequestParam` (đã nhắc ở bài 09).

---

## Ý tưởng

Dropdown cần **danh sách category**, mà danh sách này nằm ở `CategoryService`. Vì vậy `ProductViewController` phải nhận thêm `CategoryService` và bỏ danh sách vào `Model`:

```
GET /products/new
   │
   ▼
ProductViewController.createForm()
   ├── model "product"    ← new Product()                    (như cũ)
   └── model "categories" ← categoryService.findAll()        ⬅️ MỚI
   │
   ▼
products/form.html
   <select name="categoryId">
       <option th:each="c : ${categories}" ...>   ← lặp ra 1 option cho mỗi category
```

Khi bấm Lưu, form gửi lên **id** của category được chọn (`categoryId=3`). Controller dùng id đó gọi `categoryService.findById(3)` để lấy object `Category`, rồi gán vào product.

Các file sẽ đụng tới:

```
product/
├── ProductViewController.java   ✏️ SỬA  — nhận CategoryService, đưa "categories" vào model, xử lý categoryId khi lưu
└── ProductService.java          ✏️ SỬA  — update() copy thêm category

templates/products/
├── form.html                    ✏️ SỬA  — thêm <select> lặp category
└── list.html                    ✏️ SỬA  — thêm cột Danh mục (để kiểm tra kết quả)
```

---

## ⚠️ Vì sao không dùng `th:field="*{category_id}"`?

Nhìn bảng trong DB thì thấy cột tên `category_id`, nên dễ viết:

```html
<select th:field="*{category_id}">   <!-- ❌ SAI -->
```

Nhưng `th:field="*{xxx}"` **không đọc cột trong DB**. Nó đọc **thuộc tính của object Java**, tức là gọi `product.getXxx()`. Trong `Product.java` chỉ có:

```java
@ManyToOne
@JoinColumn(name = "category_id")   // "category_id" là tên CỘT trong DB
private Category category;          // "category" là tên THUỘC TÍNH trong Java
```

Không có `getCategory_id()` nên trang lỗi **500**, log ghi kiểu `Invalid property 'category_id' of bean class [Product]`.

> Quy tắc: trong Java và Thymeleaf luôn dùng **tên thuộc tính** (`category`). Tên cột (`category_id`) chỉ để Hibernate nói chuyện với MySQL.

Ở bài này ta **không** dùng `th:field` cho ô danh mục, mà tự đặt `name="categoryId"` rồi tự xử lý ở controller. Cách này dài hơn một chút nhưng thấy rõ từng bước dữ liệu đi.

---

## Bước 1 — `ProductViewController` nhận thêm `CategoryService`

Mở `product/ProductViewController.java`, sửa phần đầu class:

```java
package com.example.demo.product;

import com.example.demo.category.CategoryService;   // ⬅️ thêm import
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/products")
public class ProductViewController {

    private final ProductService productService;
    private final CategoryService categoryService;   // ⬅️ thêm

    // Spring tự truyền CẢ HAI service vào constructor
    public ProductViewController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }
```

Controller của feature `product` dùng Service của feature `category` là bình thường. Quy tắc chỉ cấm controller gọi thẳng **Repository**, còn gọi **Service** thì được.

---

## Bước 2 — Đưa danh sách category vào model

Cả hai form (thêm và sửa) đều dùng chung `products/form.html`, nên **cả hai** method đều phải đưa `"categories"` vào model:

```java
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryService.findAll());   // ⬅️ thêm
        return "products/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.findById(id));
        model.addAttribute("categories", categoryService.findAll());   // ⬅️ thêm
        return "products/form";
    }
```

> Quên dòng này ở một trong hai method thì form đó lỗi 500, vì `${categories}` là `null` và `th:each` không lặp được.

---

## Bước 3 — Vòng lặp `th:each` trong `<select>`

Mở `templates/products/form.html`, thay khối `<select>` cũ bằng:

```html
            <label>Danh mục</label>
            <select name="categoryId">
                <!-- Option rỗng: cho phép sản phẩm chưa thuộc danh mục nào -->
                <option value="">-- Chưa chọn --</option>

                <!-- Lặp: mỗi category trong ${categories} sinh ra 1 thẻ <option> -->
                <option th:each="c : ${categories}"
                        th:value="${c.id}"
                        th:text="${c.name}"
                        th:selected="${product.category?.id == c.id}">Tên danh mục</option>
            </select>
```

Đọc từng thuộc tính:

| Thuộc tính | Ý nghĩa | Kết quả với category `{id: 3, name: "Bàn phím"}` |
|------------|---------|----------------------------------------------------|
| `th:each="c : ${categories}"` | Lặp qua danh sách, mỗi vòng gọi phần tử là `c`. **Chính thẻ chứa `th:each`** được nhân lên | Một thẻ `<option>` cho mỗi category |
| `th:value="${c.id}"` | Giá trị gửi lên server khi chọn | `value="3"` |
| `th:text="${c.name}"` | Chữ người dùng nhìn thấy | `Bàn phím` |
| `th:selected="..."` | Điều kiện đúng thì thêm `selected` (chọn sẵn) | Chọn sẵn nếu product đang thuộc category 3 |

Có 3 danh mục thì HTML gửi về trình duyệt trông như sau (đang sửa một sản phẩm thuộc "Bàn phím"):

```html
<select name="categoryId">
    <option value="">-- Chưa chọn --</option>
    <option value="1">Điện thoại</option>
    <option value="3" selected="selected">Bàn phím</option>
    <option value="4">Chuột</option>
</select>
```

### Dấu `?.` trong `product.category?.id`

Sản phẩm mới, hoặc sản phẩm chưa có danh mục, có `product.category` là `null`. Viết `product.category.id` sẽ lỗi vì đọc `.id` của `null`. Dấu `?.` nghĩa là "**nếu bên trái là null thì trả null luôn, đừng đọc tiếp**". Khi đó `null == c.id` ra `false`, nên không option nào được chọn sẵn và trình duyệt hiện "-- Chưa chọn --".

### `th:each` là `foreach` của Thymeleaf

So với vòng lặp Java bạn đã biết:

```java
for (Category c : categories) {
    // in ra <option value="c.id">c.name</option>
}
```

Bạn đã dùng đúng cú pháp này ở `list.html` (`<tr th:each="p : ${products}">`) để lặp **các dòng của bảng**. Ở đây cũng vậy, chỉ khác là lặp **các option**.

---

## Bước 4 — Nhận `categoryId` khi lưu

Form gửi lên `name=...&price=...&id=...&categoryId=3`. `@ModelAttribute Product` tự điền `name`, `price`, `id`. Còn `categoryId` thì `Product` không có thuộc tính nào tên như vậy, nên Spring bỏ qua. Ta nhận nó riêng bằng `@RequestParam`:

```java
    @PostMapping("/save")
    public String save(@ModelAttribute Product product,
                       @RequestParam(required = false) Long categoryId) {   // ⬅️ thêm tham số
        // Chọn "-- Chưa chọn --" → categoryId = null → sản phẩm không thuộc danh mục nào
        if (categoryId != null) {
            product.setCategory(categoryService.findById(categoryId));
        }

        if (product.getId() == 0) {
            productService.create(product);
        } else {
            productService.update(product.getId(), product);
        }
        return "redirect:/products";
    }
```

- `@RequestParam Long categoryId`: lấy tham số tên `categoryId` trong form. Tên biến phải **trùng** với `name="categoryId"` của thẻ `<select>`.
- `required = false`: cho phép không chọn. Option rỗng gửi lên `categoryId=` (chuỗi rỗng), Spring đổi thành `null`. Thiếu `required = false` thì không chọn danh mục sẽ ra lỗi **400**.
- `categoryService.findById(...)`: nếu ai đó sửa HTML gửi lên id không tồn tại thì ra trang **404** (nhờ `NotFoundException` ở bài 10).

---

## Bước 5 — `ProductService.update()` phải copy cả `category`

Đây là lỗi cũ đã được ghi chú từ trước: `update()` chỉ copy `name` và `price`. Không sửa thì **thêm mới** lưu được danh mục, nhưng **sửa** sẽ không đổi được danh mục.

```java
    public Product update(Long id, Product data) {
        Product product = findById(id);
        product.setName(data.getName());
        product.setPrice(data.getPrice());
        product.setCategory(data.getCategory());   // ⬅️ thêm
        return productRepository.save(product);
    }
```

`create()` không cần sửa, vì nó lưu nguyên object `product` mà controller đã gán category sẵn.

> Lưu ý: REST API `PUT /api/products/{id}` cũng dùng `update()`. Giờ JSON gửi lên **phải kèm** `"category": {"id": 3}`, nếu không danh mục sẽ bị xoá về `null`. Trước đây không sửa được danh mục qua PUT, giờ thì sửa được.

---

## Bước 6 — Hiện cột Danh mục ở trang danh sách

Để nhìn thấy kết quả, thêm một cột vào `templates/products/list.html`:

```html
            <thead>
                <tr><th>ID</th><th>Tên</th><th>Giá</th><th>Danh mục</th><th>Hành động</th></tr>
            </thead>
            <tbody>
                <tr th:each="p : ${products}">
                    <td th:text="${p.id}">1</td>
                    <td th:text="${p.name}">Tên</td>
                    <td th:text="${p.price}">0</td>
                    <!-- ?. : sản phẩm chưa có danh mục thì để trống, không lỗi -->
                    <td th:text="${p.category?.name}">Danh mục</td>
                    <td> ... (giữ nguyên Sửa | Xoá) ... </td>
                </tr>
                <tr th:if="${products.isEmpty()}">
                    <td colspan="5">Chưa có sản phẩm nào.</td>   <!-- 4 → 5 vì thêm 1 cột -->
                </tr>
```

---

## Bước 7 — Chạy & kiểm tra

```bash
# Chế độ dev: app tự restart (xem bài 04)
docker compose -f docker-compose.dev.yml logs -f app-dev
```

Chuẩn bị: vào http://localhost:8093/categories tạo sẵn 2–3 danh mục.

1. **+ Thêm sản phẩm** → dropdown liệt kê đủ danh mục, đang chọn "-- Chưa chọn --".
2. Nhập tên, chọn "Bàn phím" → Lưu → trang danh sách hiện cột Danh mục = "Bàn phím".
3. **Sửa** sản phẩm đó → dropdown **chọn sẵn** "Bàn phím". Đổi sang "Chuột" → Lưu → cột đổi thành "Chuột".
4. **Sửa** lại, chọn "-- Chưa chọn --" → Lưu → cột Danh mục trống.
5. Vào trang Danh mục → cột "Số sản phẩm" tăng/giảm đúng theo các thao tác trên.

Kiểm tra trong DB:

```bash
docker exec -it demo-mysql mysql -u demo_user -pdemo_pass demo_db \
  -e "SELECT id, name, category_id FROM product;"
```

Cột `category_id` chứa **id** của danh mục (hoặc `NULL`). Đây chính là "tên cột" mà `@JoinColumn` khai báo.

---

## Lỗi thường gặp

| Triệu chứng | Nguyên nhân | Cách sửa |
|-------------|-------------|----------|
| Mở form → **500**, log ghi `Invalid property 'category_id'` | Dùng tên cột DB trong `th:field` | Bỏ `th:field`, dùng `name="categoryId"` như Bước 3 |
| Form thêm chạy, form sửa **500** (hoặc ngược lại) | Chỉ một trong hai method có `addAttribute("categories", ...)` | Thêm vào cả `createForm()` lẫn `editForm()` |
| Dropdown chỉ có "-- Chưa chọn --" | Chưa có danh mục nào, hoặc sai tên `${categories}` | Tạo danh mục ở `/categories`; so tên với `addAttribute` |
| Mở form sửa sản phẩm chưa có danh mục → **500** | Viết `product.category.id` (thiếu `?`) | Dùng `product.category?.id` |
| Lưu mà không chọn danh mục → **400** | Thiếu `required = false` | `@RequestParam(required = false) Long categoryId` |
| Thêm mới lưu được danh mục, **sửa thì không đổi** | `update()` chưa copy `category` | Làm Bước 5 |
| Danh mục luôn bị mất khi lưu | `name` của `<select>` khác tên tham số `@RequestParam` | Cả hai phải là `categoryId` |
| App không khởi động, log ghi `cannot find symbol CategoryService` | Thiếu import | `import com.example.demo.category.CategoryService;` |

## Bài tập

1. **Bắt buộc chọn danh mục**: thêm `required` vào thẻ `<select>` để trình duyệt không cho gửi khi đang ở "-- Chưa chọn --". (Gợi ý: option rỗng phải có `value=""` thì `required` mới chặn được.)
2. **Lọc theo danh mục**: trên trang danh sách sản phẩm, thêm một dropdown danh mục + nút "Lọc", gửi `GET /products?categoryId=3`. Trong `list()` nhận `@RequestParam(required = false) Long categoryId`. Có id thì chỉ hiện `categoryService.findById(id).getProducts()`, không có thì hiện tất cả.
3. **Link sang danh mục**: ở cột Danh mục của `list.html`, biến tên danh mục thành link tới `/categories/{id}/edit`. Nhớ xử lý trường hợp không có danh mục (gợi ý: `th:if="${p.category != null}"`).
4. **Thử cách `th:field`**: đổi thành `<select th:field="*{category}">` (tên thuộc tính, không phải tên cột), bỏ `@RequestParam` trong `save()`, rồi thử xem lưu và chọn sẵn có chạy không. Spring Data có một bộ chuyển đổi tên `DomainClassConverter` đổi id `"3"` thành object `Category`, hãy tìm hiểu nó. So sánh với cách `categoryId` trong bài: cách nào bạn thấy dễ hiểu hơn?

➡️ Quay lại tổng quan: [../README.md](../README.md)
