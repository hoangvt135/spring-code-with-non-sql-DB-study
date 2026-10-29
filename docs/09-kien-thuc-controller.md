# 09 — Kiến thức cơ bản về Controller

Tài liệu tra cứu: giải thích các annotation `@...` và các kiểu `return` hay gặp trong file controller. Ví dụ lấy thẳng từ `CategoryViewController` và `CategoryController` trong project.

---

## 1. Controller là gì

Controller là "cửa ngõ" của app: nó nhận request HTTP từ trình duyệt hoặc Postman, gọi Service xử lý, rồi trả kết quả về. Trong project này, mọi request đi theo một chiều:

```
Trình duyệt → Controller → Service → Repository → MySQL
```

Controller **không** tự xử lý nghiệp vụ và **không** gọi thẳng Repository. Nó chỉ làm 3 việc: nhận dữ liệu từ request, gọi Service, chọn thứ trả về.

### Hai loại controller

Cùng một `CategoryService` được dùng bởi hai controller khác nhau:

| | REST API | Giao diện web (Thymeleaf) |
|---|---|---|
| Ví dụ | `CategoryController` | `CategoryViewController` |
| Annotation trên class | `@RestController` | `@Controller` |
| Đường dẫn | `/api/categories` | `/categories` |
| `return` trả về | Dữ liệu (object, List) → Spring tự đổi thành JSON | Tên template (`"categories/list"`) hoặc `"redirect:..."` |
| Ai dùng | Postman, app mobile, JavaScript | Người dùng mở trình duyệt |

> Mẹo nhớ: `@RestController` = `@Controller` + `@ResponseBody`. Chữ `@ResponseBody` nghĩa là "giá trị return chính là nội dung trả về", nên không đi tìm template nữa.

```java
@RestController                      // trả JSON
@RequestMapping("/api/categories")
public class CategoryController { ... }

@Controller                          // trả trang HTML
@RequestMapping("/categories")
public class CategoryViewController { ... }
```

### Constructor injection

Controller nhận Service qua constructor. Spring tự tạo `CategoryService` và truyền vào, nên không cần viết `new CategoryService()` hay `@Autowired`.

```java
private final CategoryService categoryService;

public CategoryViewController(CategoryService categoryService) {
    this.categoryService = categoryService;
}
```

---

## 2. Khai báo đường dẫn: `@RequestMapping` và `@XxxMapping`

URL đầy đủ của một method = phần `@RequestMapping` trên class **+** phần `@GetMapping`/`@PostMapping`... trên method.

```java
@Controller
@RequestMapping("/categories")          // tiền tố chung cho cả class
public class CategoryViewController {

    @GetMapping                         // GET  /categories
    public String list(Model model) { ... }

    @GetMapping("/new")                 // GET  /categories/new
    public String createForm(Model model) { ... }

    @GetMapping("/{id}/edit")           // GET  /categories/5/edit
    public String editForm(@PathVariable Long id, Model model) { ... }

    @PostMapping("/save")               // POST /categories/save
    public String save(@ModelAttribute Category category) { ... }
}
```

`@RequestMapping` đặt trên class để khỏi lặp lại `/categories` ở mọi method. `@GetMapping` không có ngoặc nghĩa là dùng đúng tiền tố đó.

### Mỗi annotation ứng với một HTTP method

| Annotation | HTTP method | Dùng để | Ví dụ trong project |
|---|---|---|---|
| `@GetMapping` | GET | Lấy / xem dữ liệu, mở trang | `GET /categories`, `GET /api/products/1` |
| `@PostMapping` | POST | Tạo mới, gửi form | `POST /categories/save`, `POST /api/products` |
| `@PutMapping` | PUT | Cập nhật | `PUT /api/products/1` |
| `@DeleteMapping` | DELETE | Xoá | `DELETE /api/products/1` |

Form HTML chỉ gửi được GET và POST. Vì vậy bên giao diện web, sửa cũng đi qua `POST /save`, còn xoá đi qua link `GET /{id}/delete`. `PUT` và `DELETE` chỉ xuất hiện ở REST API.

Cùng một URL nhưng khác HTTP method là hai route khác nhau: `GET /api/products/1` (xem) và `DELETE /api/products/1` (xoá) gọi hai method khác nhau.

### `{id}` trong đường dẫn

`{id}` là một "chỗ trống" khớp với bất kỳ giá trị nào ở vị trí đó: `/categories/5/edit`, `/categories/12/edit`... Giá trị này được lấy ra bằng `@PathVariable` (phần 3).

Nếu URL không khớp route nào, Spring báo **404** (`No static resource ...`). Ví dụ `/categories/list` không có route nào, dù có template tên `categories/list`.

---

## 3. Lấy dữ liệu từ request

Mỗi annotation đặt trước tham số cho Spring biết lấy giá trị ở **đâu** trong request.

| Annotation | Lấy từ đâu | Ví dụ request | Dùng ở |
|---|---|---|---|
| `@PathVariable` | Một đoạn trong đường dẫn (`{id}`) | `/categories/5/edit` → `id = 5` | Cả hai loại controller |
| `@ModelAttribute` | Các ô input của form HTML | `name=Điện thoại&id=` → một object `Category` | View controller |
| `@RequestBody` | Phần body dạng JSON | `{"name": "Điện thoại"}` → một object `Category` | REST controller |
| `@RequestParam` | Tham số sau dấu `?` | `/products?keyword=ao` → `keyword = "ao"` | Chưa dùng trong project, hay dùng cho tìm kiếm |

### `@PathVariable`

Tên tham số phải trùng với tên trong ngoặc nhọn. Spring tự đổi chuỗi `"5"` sang `Long` (hoặc `long`).

```java
@GetMapping("/{id}/edit")
public String editForm(@PathVariable Long id, Model model) {
    model.addAttribute("category", categoryService.findById(id));
    return "categories/form";
}
```

### `@ModelAttribute`

Spring đọc từng ô input của form và gọi setter tương ứng: ô `name` → `setName(...)`, ô `id` → `setId(...)`. Vì vậy entity **bắt buộc có setter** và constructor rỗng.

```java
@PostMapping("/save")
public String save(@ModelAttribute Category category) {
    if (category.getId() == null) {          // ô id rỗng → thêm mới
        categoryService.creatCategory(category);
    } else {                                  // có id → sửa
        categoryService.updateCategory(category.getId(), category);
    }
    return "redirect:/categories";
}
```

Tên ô input do `th:field="*{name}"` trong `form.html` sinh ra, nên phải trùng tên thuộc tính trong entity.

### `@RequestBody`

Dùng khi client gửi JSON (Postman, JavaScript). Spring dùng thư viện Jackson để đổi JSON thành object. Thiếu `@RequestBody` thì object nhận được sẽ rỗng hết.

```java
@PostMapping                                  // POST /api/categories
public Category create(@RequestBody Category category) {
    return categoryService.creatCategory(category);
}
```

> Phân biệt nhanh: dữ liệu từ **form HTML** → `@ModelAttribute`; dữ liệu **JSON** → `@RequestBody`.

---

## 4. `Model`: chuyển dữ liệu sang template

`Model` giống một cái túi: controller bỏ dữ liệu vào kèm một cái tên, template lấy ra bằng đúng cái tên đó. Spring tự tạo `Model` khi bạn khai báo nó là tham số của method.

```java
@GetMapping
public String list(Model model) {
    //                 tên (key)      giá trị
    model.addAttribute("categories", categoryService.findAll());
    return "categories/list";
}
```

```html
<!-- templates/categories/list.html -->
<tr th:each="c : ${categories}">     <!-- "categories" phải trùng key ở trên -->
    <td th:text="${c.name}">Tên</td>
</tr>
```

Tên trong `addAttribute("...")` và tên trong `${...}` phải **giống hệt nhau**, kể cả hoa thường và dấu. Viết `"categỏy"` ở controller mà template dùng `${category}` thì template nhận `null` và báo lỗi.

Quy ước đặt tên trong project:

- Danh sách → số nhiều: `"categories"`, `"products"`.
- Một object (form thêm/sửa) → số ít: `"category"`, `"product"`.

Form thêm mới cũng cần bỏ vào một object rỗng (`new Category()`). Lý do: `th:object="${category}"` trong `form.html` cần một object để điền vào các ô, kể cả khi các ô đều trống.

`Model` chỉ dùng trong `@Controller` (view). Bên `@RestController`, dữ liệu được trả thẳng bằng `return`.

---

## 5. Các kiểu `return`

Trong `@Controller`, chuỗi bạn `return` có hai nghĩa hoàn toàn khác nhau tùy có chữ `redirect:` ở đầu hay không. Trong `@RestController`, giá trị return chính là dữ liệu JSON.

| Bạn viết | Spring hiểu là | Trình duyệt thấy | Thanh địa chỉ |
|---|---|---|---|
| `return "categories/list";` | Tên **template** → mở file `templates/categories/list.html`, điền dữ liệu từ `Model` | Trang HTML | Giữ nguyên URL vừa gọi |
| `return "redirect:/categories";` | Một **URL** → bảo trình duyệt gửi request mới `GET /categories` | Trang của URL mới | Đổi thành `/categories` |
| `return categoryService.findAll();` (REST) | **Dữ liệu** → đổi thành JSON | `[{"id":1,"name":"..."}]` | Giữ nguyên |

### Trả về tên template (không có `redirect:`)

Chuỗi này **không phải URL**. Spring ghép nó thành đường dẫn file:

```
"categories/list"  →  src/main/resources/templates/ + categories/list + .html
```

Dùng khi cần **hiển thị** một trang: danh sách, form thêm, form sửa. Nhiều method có thể trả về cùng một template: `createForm()` và `editForm()` đều trả `"categories/form"`, chỉ khác dữ liệu trong `Model`.

### Trả về `redirect:`

Phần sau `redirect:` **là URL**, không phải tên file. Spring không render trang nào cả. Nó trả về mã `302` kèm URL mới, rồi trình duyệt tự gọi URL đó. Request mới này lại đi vào method `list()` như bình thường.

Dùng sau khi **thay đổi dữ liệu**: lưu (`save`) và xoá (`delete`). Phần 6 giải thích vì sao.

```java
@GetMapping("/{id}/delete")
public String delete(@PathVariable Long id) {
    categoryService.delete(id);
    return "redirect:/categories";   // URL, không phải "categories/list"
}
```

### Hai lỗi hay nhầm

- `return "redirect:categories/list"` → trình duyệt đi tới URL `/categories/list`, không có route nào → **404**.
- `return "categories/list"` sau khi lưu → trang hiện ra nhưng **trống**, vì method `save()` không bỏ `"categories"` vào `Model`. Bấm F5 còn gửi lại form một lần nữa.

### Trong `@RestController`

Kiểu trả về của method quyết định nội dung JSON:

| Kiểu trả về | JSON nhận được |
|---|---|
| `Category` | Một object `{...}` |
| `List<Category>` | Một mảng `[{...}, {...}]` |
| `Map<String, String>` (như `HelloController`) | `{"message": "..."}` |
| `void` (như `delete`) | Không có body, chỉ mã `200` |

Bên này một chuỗi `return "categories/list"` sẽ được trả nguyên văn thành chữ, không tìm template.

---

## 6. Đi hết một vòng: thêm một category

Thêm một category qua giao diện web tốn **3 request**, và chỉ request giữa (POST) dùng `redirect:`.

```
   Trình duyệt                              CategoryViewController
       │                                              │
       │ ① GET /categories/new                        │
       ├─────────────────────────────────────────────►│ createForm()
       │                                              │ return "categories/form"
       │◄ ─ ─ ─ ─ ─ ─ HTML: form trống ─ ─ ─ ─ ─ ─ ─ ─┤
       │                                              │
       │   (người dùng điền tên, bấm Lưu)             │
       │                                              │
       │ ② POST /categories/save                      │
       ├─────────────────────────────────────────────►│ save()
       │                                              │ lưu vào DB qua Service
       │                                              │ return "redirect:/categories"
       │◄═══════ 302 · Location: /categories ═════════┤
       │                                              │
       │   (trình duyệt TỰ gọi URL mới, không cần bấm)│
       │                                              │
       │ ③ GET /categories                            │
       ├─────────────────────────────────────────────►│ list()
       │                                              │ return "categories/list"
       │◄ ─ ─ ─ HTML: danh sách có category mới ─ ─ ─ ┤
```

Request ① và ③ là GET nên trả về tên template. Request ② là POST, làm thay đổi DB, nên trả về `redirect:`. Mẫu này có tên là **Post / Redirect / Get** (PRG).

### Vì sao không `return "categories/list"` luôn trong `save()`?

1. **Trang sẽ trống.** Template `list.html` cần `${categories}`, mà chỉ method `list()` mới bỏ nó vào `Model`. Redirect để `list()` chạy và chuẩn bị dữ liệu, khỏi viết lại code đó trong `save()`.
2. **F5 sẽ lưu trùng.** Không redirect thì thanh địa chỉ vẫn là `/categories/save`, và request cuối là POST. Bấm F5, trình duyệt gửi lại form, nên DB có thêm một dòng giống hệt. Có redirect thì request cuối là `GET /categories`, F5 chỉ tải lại danh sách.
3. **URL đúng.** Thanh địa chỉ hiện `/categories`, người dùng có thể copy hoặc bookmark.

Xoá cũng theo cùng lý do: `delete()` thay đổi DB nên trả về `redirect:/categories`.

---

## 7. Tóm tắt và lỗi hay gặp

### `CategoryViewController` trong một bảng

| Method | Route | Lấy dữ liệu bằng | Return |
|---|---|---|---|
| `list()` | `GET /categories` | — | `"categories/list"` (template) |
| `createForm()` | `GET /categories/new` | — | `"categories/form"` (template) |
| `editForm()` | `GET /categories/{id}/edit` | `@PathVariable` | `"categories/form"` (template) |
| `save()` | `POST /categories/save` | `@ModelAttribute` | `"redirect:/categories"` |
| `delete()` | `GET /categories/{id}/delete` | `@PathVariable` | `"redirect:/categories"` |

> Quy tắc gọn: method **hiển thị** trả về tên template; method **thay đổi dữ liệu** trả về `redirect:`.

### Thông báo lỗi → nguyên nhân

| Bạn thấy | Nguyên nhân thường gặp | Cách sửa |
|---|---|---|
| `404` `No static resource categories/list` | Gõ tên template vào thanh địa chỉ, hoặc viết `redirect:categories/list` | Mở `/categories`; redirect tới URL của route |
| `500` `Error resolving template [categories/list]` | Thiếu file `templates/categories/list.html`, hoặc sai chính tả tên | Tạo file, so lại tên với chuỗi return |
| `500` khi mở form, lỗi nhắc tới `null` / `category` | Key trong `addAttribute` khác tên trong `${...}` | Sửa cho hai tên giống hệt nhau |
| `400` `Bad Request` khi mở `/categories/abc/edit` | `{id}` không đổi được sang `Long` | Truyền đúng số vào URL |
| `405` `Method Not Allowed` | Form gửi `POST` tới route chỉ có `@GetMapping` (hoặc ngược lại) | So `method="post"` trong form với annotation |
| Sau khi lưu, trang danh sách trống | `save()` return tên template thay vì `redirect:` | Đổi thành `return "redirect:/categories"` |

Khi đang chạy chế độ dev, xem lỗi chi tiết bằng:

```bash
docker compose -f docker-compose.dev.yml logs -f app-dev
```
