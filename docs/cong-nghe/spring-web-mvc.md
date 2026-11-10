# Spring Web MVC: controller, REST, form

> **Dùng trong project từ:** [bài 02](../study/02-thuc-hanh-crud.md) · **Phiên bản:** Spring Framework 6.2.1 (Spring MVC), Tomcat nhúng 10.1.34, Jackson 2.18.2, tất cả đến từ `spring-boot-starter-web` của Spring Boot 3.4.1 · **Tài liệu chính thức:** [Spring Web MVC](https://docs.spring.io/spring-framework/reference/6.2/web/webmvc.html)
>
> 🔼 **Sau [bài 16.5](../study/16.5-nang-cap-spring-boot-4.md)** (người học chưa làm): Spring Framework **7.0**, Tomcat **11.0**, Jackson **3.1**. Đổi: starter `spring-boot-starter-web` → `spring-boot-starter-webmvc`; JSON xếp trường theo chữ cái; `@JsonIgnore` giữ package cũ (Bước 5, 8).

## Nó là gì, giải quyết vấn đề gì

Spring Web MVC (gọi tắt Spring MVC) là phần **web** của Spring Framework. Nó nhận request HTTP, tìm đúng hàm Java để xử lý, chuẩn bị tham số cho hàm đó, rồi biến giá trị `return` thành response: trang HTML, JSON, hoặc lệnh chuyển hướng.

Nếu bạn quen Laravel, gần như mọi thứ đều có bản tương ứng:

| Việc | Laravel | Spring MVC (project này) |
|---|---|---|
| Khai báo route | File `routes/web.php`: `Route::get('/products', ...)` | **Không có file route.** Annotation đặt ngay trên hàm: `@GetMapping` |
| Lấy tham số trên URL | `function show($id)` với route `/products/{id}` | `@PathVariable Long id` |
| Lấy dữ liệu form / query | `$request->input('categoryId')` | `@RequestParam Long categoryId`, hoặc gom cả form vào object bằng `@ModelAttribute` |
| Lấy JSON trong body | `$request->json()->all()` | `@RequestBody Product product` |
| File upload | `$request->file('imageFile')`, `$_FILES` | `MultipartFile imageFile` |
| Trả trang HTML | `return view('products.list', ['products' => $p]);` | `model.addAttribute("products", p); return "products/list";` |
| Trả JSON | `return response()->json($p);` | `return p;` trong `@RestController` |
| Chuyển hướng | `return redirect('/products');` | `return "redirect:/products";` |
| Báo "không tìm thấy" | `abort(404)` | `throw new NotFoundException(...)` (có `@ResponseStatus(NOT_FOUND)`) |

Chữ **MVC** là *Model – View – Controller*: **Controller** nhận request và gọi Service, **Model** là dữ liệu đưa sang giao diện, **View** là template Thymeleaf vẽ ra HTML (xem [thymeleaf.md](thymeleaf.md)). Với REST API thì không có View: dữ liệu được đổi thẳng thành JSON.

## Khái niệm cốt lõi

### `DispatcherServlet` và luồng một request

Mọi request đi vào app đều qua **một** cửa duy nhất là `DispatcherServlet`, một servlet do Spring Boot tự đăng ký vào Tomcat nhúng. Mẫu thiết kế này gọi là *front controller*, giống `public/index.php` + Router của Laravel. Đi theo request `GET /products/12`:

```
Trình duyệt / curl
   │  GET /products/12
   ▼
Tomcat nhúng (cổng 8080 trong container, 8093 ở máy bạn)
   ▼
Chuỗi Filter: Spring Security kiểm tra đăng nhập, CSRF...  → xem spring-security.md
   ▼
DispatcherServlet
   ├─ ① HandlerMapping: URL + HTTP method này khớp hàm nào?
   │       → ProductViewController.detail(...)
   │       (không khớp hàm nào → thử file tĩnh: static/, /uploads/** → vẫn không có → 404)
   ├─ ② HandlerAdapter: chuẩn bị từng tham số rồi gọi hàm
   │       @PathVariable Long id = 12, Principal principal, Model model
   ├─ ③ Xử lý giá trị return
   │       @RestController: object → HttpMessageConverter (Jackson) → JSON
   │       @Controller, "products/detail" → ViewResolver → Thymeleaf → HTML
   │       @Controller, "redirect:/products" → 302 + header Location
   └─ ④ Hàm ném exception? → HandlerExceptionResolver
           có @ResponseStatus(NOT_FOUND) → 404; lỗi tham số → 400; còn lại → 500
           → Tomcat chuyển sang /error → BasicErrorController → error/404.html hoặc JSON
```

Bạn chỉ viết phần ở giữa (hàm controller). Mọi bước còn lại là các bean mà Spring Boot đã cấu hình sẵn khi thấy `spring-boot-starter-web` và `spring-boot-starter-thymeleaf` (auto-configuration, xem [spring-boot.md](spring-boot.md)).

### Hai loại controller: `@Controller` và `@RestController`

| | `@Controller` (giao diện web) | `@RestController` (REST API) |
|---|---|---|
| Trong project | `ProductViewController`, `CategoryViewController`, `AuthController` | `ProductController`, `CategoryController`, `HelloController` |
| Tiền tố đường dẫn | `/products`, `/categories` | `/api/products`, `/api/categories` |
| Chuỗi `return "products/list"` | **Tên template** → `templates/products/list.html` | Trả nguyên văn chữ `products/list` |
| `return product;` (object) | Không dùng kiểu này | Jackson đổi thành JSON |
| Dữ liệu vào | Form HTML → `@ModelAttribute`, `@RequestParam` | JSON → `@RequestBody` |
| Đăng nhập | Form `/login` (session, cookie) | HTTP Basic: `curl -u admin:admin123` |

`@RestController` thực chất là `@Controller` + `@ResponseBody`. Chữ `@ResponseBody` nghĩa là "giá trị return **chính là** nội dung response", nên bước ③ ở trên đi nhánh Jackson thay vì đi tìm template.

Cả hai loại dùng **chung một Service**: `ProductController` và `ProductViewController` cùng gọi `ProductService.findById(...)`. Logic nghiệp vụ (chặn giá âm, ném lỗi khi không tìm thấy) chỉ viết **một lần** trong Service, còn controller chỉ lo "nhận vào, gọi Service, trả ra". Bảng so sánh gốc: [bài 09 — Hai loại controller](../study/09-kien-thuc-controller.md#hai-loại-controller).

### Khai báo đường dẫn: `@RequestMapping` và `@XxxMapping`

URL đầy đủ = `@RequestMapping` trên class **+** `@GetMapping`/`@PostMapping`... trên hàm:

```java
@Controller
@RequestMapping("/products")
public class ProductViewController {
    @GetMapping                  // GET  /products
    @GetMapping("/{id}")         // GET  /products/12
    @GetMapping("/new")          // GET  /products/new
    @GetMapping("/{id}/edit")    // GET  /products/12/edit
    @PostMapping("/save")        // POST /products/save
    @GetMapping("/{id}/delete")  // GET  /products/12/delete
}
```

(Đoạn trên chỉ để thấy các đường dẫn; mỗi annotation thật ra nằm trên một hàm riêng.)

Mấy quy tắc ít được nói tới:

- **`/new` và `/{id}` cùng tồn tại được.** Khi một URL khớp nhiều mẫu, Spring chọn mẫu **cụ thể hơn**: `/products/new` khớp cả `/new` lẫn `/{id}`, nhưng `/new` không có biến nên thắng. Nhờ vậy `/products/new` không bị hiểu là "sản phẩm có id = new".
- **Hai hàm trùng y hệt** (cùng URL, cùng method) thì app **không khởi động**, log báo `Ambiguous mapping`.
- **Dấu `/` ở cuối không còn được bỏ qua.** Từ Spring Framework 6 (Spring Boot 3), `/products/` **khác** `/products` và ra 404. Bài viết cũ cho Spring Boot 2 có thể nói ngược lại.
- Form HTML chỉ gửi được `GET` và `POST`, nên bên giao diện web, sửa đi qua `POST /save` và xoá đi qua `GET /{id}/delete`. `PUT`, `DELETE` chỉ có ở REST API. Chi tiết: [bài 09 mục 2](../study/09-kien-thuc-controller.md#2-khai-báo-đường-dẫn-requestmapping-và-xxxmapping).

### Lấy dữ liệu từ request

Mỗi tham số của hàm controller được một bộ phận riêng (*argument resolver*) điền giá trị. Annotation trước tham số cho Spring biết lấy ở đâu:

| Viết | Lấy từ | Ví dụ trong project |
|---|---|---|
| `@PathVariable Long id` | Một đoạn của đường dẫn (`{id}`) | `/products/12` → `id = 12` |
| `@RequestParam Long categoryId` | Tham số sau `?` **hoặc** ô input của form | `categoryId=3` trong form sản phẩm ([bài 11](../study/11-chon-danh-muc-cho-san-pham.md#bước-4--nhận-categoryid-khi-lưu)) |
| `@ModelAttribute Product product` | **Nhiều** ô input của form, gom vào một object | Form thêm/sửa sản phẩm, danh mục, đăng ký |
| `@RequestBody Product product` | Body JSON | `POST /api/products` |
| `MultipartFile imageFile` | Một file trong form `multipart/form-data` | Ảnh sản phẩm ([bài 15](../study/15-anh-va-mo-ta-san-pham.md#bước-8--productviewcontroller-nhận-file-từ-form)) |
| `Model model` | Spring tạo sẵn, để bỏ dữ liệu sang template | Mọi hàm hiển thị trang |
| `Principal principal` | Người đang đăng nhập (do Spring Security điền) | `principal.getName()` trong `detail()` ([bài 16](../study/16-luot-xem-top-san-pham-redis.md)) |

Chi tiết từng loại:

- **`@PathVariable` và `@RequestParam` tự đổi kiểu**: chuỗi `"12"` thành `Long`. Đổi không được (`/products/abc`) thì trả **400 Bad Request**.
- **`@RequestParam` mặc định là bắt buộc.** Thiếu tham số thì 400 `Required parameter 'categoryId' is not present`. `required = false` cho phép thiếu, khi đó giá trị là `null` (nên kiểu phải là `Long`, không phải `long`). Ô `<select>` chọn "-- Chưa chọn --" gửi lên chuỗi rỗng, Spring cũng đổi thành `null`. Muốn có giá trị mặc định: `@RequestParam(defaultValue = "1") int page`.
- **`@ModelAttribute`** tạo object bằng constructor rỗng, rồi với mỗi ô input, gọi **setter trùng tên**: ô `name` → `setName(...)`. Ô nào không có setter tương ứng (như `categoryId`) thì bị bỏ qua, nên phải nhận riêng bằng `@RequestParam`. Object này còn được **tự bỏ vào `Model`** với tên `"product"`, nên khi `return` lại form thì giá trị vừa gõ vẫn còn (`AuthController.register` dựa vào điều này).
- **`@RequestBody`** dùng Jackson đọc JSON. Request phải có header `Content-Type: application/json`, nếu không sẽ ra **415 Unsupported Media Type**. Thiếu chữ `@RequestBody` thì không lỗi, nhưng object nhận được rỗng (Spring coi nó là `@ModelAttribute` và đi tìm ô form, mà không có ô nào).
- **Tên tham số Java phải trùng tên trên request** (`id` ↔ `{id}`, `categoryId` ↔ `name="categoryId"`). Spring đọc được tên biến lúc chạy là nhờ cờ biên dịch `-parameters`, do plugin Spring Boot của Gradle tự bật (xem [gradle.md](gradle.md)). Muốn đặt tên khác thì ghi rõ: `@RequestParam("cat") Long categoryId`.

Phân biệt nhanh: dữ liệu từ **form HTML** → `@ModelAttribute` / `@RequestParam`; dữ liệu **JSON** → `@RequestBody`.

### `Model` và tên template

`Model` là "cái túi" chuyển dữ liệu sang template: `model.addAttribute("products", ...)` thì template đọc bằng `${products}`. Chuỗi `return` (không có `redirect:`) được `ThymeleafViewResolver` ghép thành đường dẫn file:

```
"products/list"  →  classpath:/templates/ + products/list + .html
                 =  src/main/resources/templates/products/list.html
```

Tiền tố `templates/` và đuôi `.html` là mặc định của Spring Boot, không cần cấu hình. Cách viết template và layout: [thymeleaf.md](thymeleaf.md), [bài 05](../study/05-giao-dien-thymeleaf.md).

### `redirect:` và mẫu Post/Redirect/Get

`return "redirect:/products"` không render gì cả: Spring trả **302** kèm header `Location: /products`, rồi trình duyệt tự gửi `GET /products`. Project dùng nó sau **mọi** thao tác thay đổi dữ liệu (lưu, xoá, đăng ký), để bấm F5 không gửi lại form và trang danh sách luôn được nạp đủ dữ liệu bởi hàm `list()`. Có thể kèm query string: `AuthController` trả `"redirect:/login?registered"` để trang login hiện thông báo. Sơ đồ 3 request: [bài 09 mục 6](../study/09-kien-thuc-controller.md#6-đi-hết-một-vòng-thêm-một-category).

### Mã HTTP: `@ResponseStatus` và `ResponseEntity`

Trong project, hàm REST trả thẳng object nên luôn là **200**; hàm `void` (như `delete`) trả 200 không có body. Mã khác được tạo bằng **exception**:

```java
@ResponseStatus(HttpStatus.NOT_FOUND)               // ném ra khỏi controller → 404 thay vì 500
public class NotFoundException extends RuntimeException { ... }
```

Ngoài kia hay dùng **`ResponseEntity`** để tự chọn mã và header ngay trong hàm. Project **chưa dùng** (giữ đúng cách bài 02 dạy), nhưng nên biết khi đọc code khác:

```java
// Ví dụ, KHÔNG có trong project
@PostMapping
public ResponseEntity<Product> create(@RequestBody Product product) {
    Product saved = service.create(product);
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);   // 201 Created
}
// Các cách viết hay gặp: ResponseEntity.ok(x) → 200, ResponseEntity.noContent().build() → 204,
//                        ResponseEntity.notFound().build() → 404
```

`@ResponseStatus` cũng đặt được lên **hàm**: `@ResponseStatus(HttpStatus.CREATED)` trên `create()` thì mọi lần thành công đều trả 201.

### Upload file: `MultipartFile`

Ba thứ phải khớp nhau thì file mới tới được controller:

1. Form có `method="post"` **và** `enctype="multipart/form-data"`. Thiếu `enctype` thì trình duyệt chỉ gửi **tên** file.
2. `name="imageFile"` của `<input type="file">` trùng tên tham số `MultipartFile imageFile`. Tên này cố ý **khác** trường `imagePath` của `Product`, để `@ModelAttribute` không cố nhét file vào một trường `String`.
3. Kích thước nằm trong giới hạn `spring.servlet.multipart.*`. Mặc định của Spring Boot là 1MB/file và 10MB/request. Project đặt `max-file-size: 5MB`, `max-request-size: 6MB` ([bài 15 Bước 1](../study/15-anh-va-mo-ta-san-pham.md#bước-1--cấu-hình-trong-applicationyml)).

Controller **không** tự lưu file: nó chuyển `MultipartFile` cho `ProductService.save(...)`, và chỉ `FileStorageService` biết file nằm ở đâu ([storage/luu-file-upload.md](../storage/luu-file-upload.md)). Chiều ngược lại, trình duyệt xem ảnh qua URL `/uploads/xxx.jpg` nhờ `WebConfig` khai báo thêm một *resource handler*, giống cách Spring tự phục vụ `static/` ([bài 15 Bước 5](../study/15-anh-va-mo-ta-san-pham.md#bước-5--webconfig-cho-trình-duyệt-xem-được-file-trong-uploads)). Các hàm của `MultipartFile`: xem bảng tra cứu trong bài 15 Bước 4.

### Jackson: entity ↔ JSON

`@RestController` đổi object thành JSON bằng **Jackson**, thư viện có sẵn trong `spring-boot-starter-web`. Quy tắc:

- **Java → JSON** đi qua **getter**: `getName()` → `"name"`, `getCategory()` → object `"category"` lồng bên trong. Field không có getter thì không xuất hiện. Field `null` vẫn được in ra là `null`.
- **JSON → Java** (`@RequestBody`) gọi constructor rỗng rồi các **setter**. Key thừa trong JSON bị bỏ qua (Spring Boot tắt sẵn chế độ báo lỗi khi gặp key lạ).

`GET /api/products/3` với dữ liệu seeder trả về:

```json
{"id":3,"name":"Keychron K2","price":1890000.0,"description":"","imagePath":null,"category":{"id":2,"name":"Bàn phím"}}
```

`category` không kèm danh sách `products` là nhờ `@JsonIgnore`:

```java
// category/Category.java
@OneToMany(mappedBy = "category")
@JsonIgnore                         // BẮT BUỘC: nếu không, Product → category → products → product → ... vô tận
private List<Product> products = new ArrayList<>();
```

Thiếu `@JsonIgnore`, Jackson đi vòng tròn tới khi tràn bộ nhớ và trả lỗi 500. Tuỳ phiên bản, log ghi `Infinite recursion (StackOverflowError)` hoặc `Document nesting depth (1001) exceeds the maximum allowed`. Lý do và các cách khác (`@JsonManagedReference`, DTO): [bài 06 Bước 3](../study/06-quan-he-model.md). Khi thêm quan hệ 2 chiều mới, làm y hệt.

Project **trả thẳng entity ra JSON** (quy ước trong `CLAUDE.md`, cho đơn giản). Ngoài kia người ta thường dùng **DTO** (class riêng chỉ chứa những gì muốn trả ra). Cái giá của cách đơn giản: **mọi getter đều lộ ra**. Hiện chưa có API nào trả `User`; nếu sau này thêm thì chuỗi BCrypt trong `password` sẽ nằm trong JSON, nên phải `@JsonIgnore` trường đó.

### Trang lỗi: `/error` và `templates/error/404.html`

Khi request kết thúc bằng lỗi (không có route, hoặc exception mang `@ResponseStatus(404)`), Tomcat chuyển nội bộ sang `/error`. Đường dẫn này do `BasicErrorController` của Spring Boot xử lý. Nó tìm template theo thứ tự `error/404.html` → `error/4xx.html` → `error.html` → trang **Whitelabel**, và đưa sẵn các biến `${status}`, `${error}`, `${message}`, `${path}`, `${timestamp}`. Project chỉ có `error/404.html` ([bài 10](../study/10-trang-404.md#spring-boot-tìm-trang-lỗi-thế-nào), [common/trang-loi-404.md](../common/trang-loi-404.md)).

- **HTML hay JSON** tuỳ header `Accept`: trình duyệt nhận trang `404.html`, còn `curl`/Postman nhận JSON `{"status":404,"error":"Not Found","message":"...","path":"..."}`.
- `${message}` mặc định bị giấu. Project bật bằng `server.error.include-message: always`.
- `/error` có trong danh sách `permitAll` của `SecurityConfig`. Thiếu nó thì chính trang lỗi cũng bị bắt đăng nhập.
- Lỗi 500 (không có `error/500.html`, `error/5xx.html`) vẫn hiện Whitelabel.
- Cách "bắt mọi exception ở một chỗ" bằng `@ControllerAdvice` + `@ExceptionHandler` mạnh hơn, nhưng project chưa dùng: xem [bài 10](../study/10-trang-404.md#vì-sao-không-dùng-controlleradvice).

## Project này dùng nó thế nào

Toàn bộ route của app:

| Controller | Loại | Route | Bài |
|---|---|---|---|
| `controller/HelloController` | REST | `GET /`, `GET /hello` | [01](../study/01-tong-quan.md) |
| `product/ProductController` | REST | `GET`, `POST /api/products` · `GET`, `PUT`, `DELETE /api/products/{id}` | [02](../study/02-thuc-hanh-crud.md#bước-4--controller-mở-api-ra-ngoài) |
| `category/CategoryController` | REST | `GET`, `POST /api/categories` · `GET`, `DELETE /api/categories/{id}` (chưa có `PUT`) | [06](../study/06-quan-he-model.md) |
| `product/ProductViewController` | View | `GET /products`, `/products/{id}`, `/products/new`, `/products/{id}/edit`, `/products/{id}/delete` · `POST /products/save` | [05](../study/05-giao-dien-thymeleaf.md#bước-2--controller-trả-về-trang-web), [11](../study/11-chon-danh-muc-cho-san-pham.md), [15](../study/15-anh-va-mo-ta-san-pham.md), [16](../study/16-luot-xem-top-san-pham-redis.md) |
| `category/CategoryViewController` | View | `GET /categories`, `/categories/new`, `/categories/{id}/edit`, `/categories/{id}/delete` · `POST /categories/save` | [08](../study/08-giao-dien-category.md), [09](../study/09-kien-thuc-controller.md) |
| `user/AuthController` | View | `GET /login` · `GET`, `POST /register` (còn `POST /login`, `POST /logout` do Spring Security tự xử lý) | [12](../study/12-dang-nhap-spring-security.md) |
| `config/WebConfig` | Resource handler | `GET /uploads/**` → thư mục `uploads/` | [15](../study/15-anh-va-mo-ta-san-pham.md) |
| (Spring Boot) `BasicErrorController` | Lỗi | `/error` → `templates/error/404.html` | [10](../study/10-trang-404.md) |

Hàm `save()` của `ProductViewController` gom gần đủ các kiểu nhận dữ liệu trong một chỗ:

```java
@PostMapping("/save")
public String save(@ModelAttribute Product product,                       // name, price, description, id
                   @RequestParam(required = false) Long categoryId,       // <select name="categoryId">
                   @RequestParam(required = false) MultipartFile imageFile) {   // <input type="file" name="imageFile">
    if (categoryId != null) {
        product.setCategory(categoryService.findById(categoryId));        // id lạ → NotFoundException → 404
    }
    productService.save(product, imageFile);                               // thêm hay sửa: Service quyết định (id == 0?)
    return "redirect:/products";                                           // Post/Redirect/Get
}
```

Doc theo chức năng: [product/quan-ly-san-pham.md](../product/quan-ly-san-pham.md), [category/quan-ly-danh-muc.md](../category/quan-ly-danh-muc.md), [user/dang-nhap-dang-ky.md](../user/dang-nhap-dang-ky.md), [storage/luu-file-upload.md](../storage/luu-file-upload.md), [common/trang-loi-404.md](../common/trang-loi-404.md).

## Tra cứu nhanh

| Annotation / Hàm | Của ai | Làm gì | Trả về / Ghi chú |
|---|---|---|---|
| `@Controller` | Spring MVC | Bean controller trả **tên template** | |
| `@RestController` | Spring MVC | `@Controller` + `@ResponseBody`: trả **dữ liệu** (JSON) | |
| `@RequestMapping("/x")` | Spring MVC | Tiền tố URL chung (trên class) hoặc route mọi method (trên hàm) | |
| `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping` | Spring MVC | Route cho từng HTTP method | Không có tham số = đúng tiền tố của class |
| `@PathVariable` | Spring MVC | Lấy `{biến}` trong URL | Đổi kiểu sai → 400 |
| `@RequestParam` | Spring MVC | Lấy tham số query / ô form | Mặc định bắt buộc; `required = false`, `defaultValue = "..."` |
| `@ModelAttribute` | Spring MVC | Gom các ô form vào object qua setter | Object tự được bỏ vào `Model` |
| `@RequestBody` | Spring MVC | Đọc body JSON thành object (Jackson) | Cần `Content-Type: application/json` |
| `model.addAttribute(ten, giaTri)` | `Model` (Spring MVC) | Đưa dữ liệu sang template | Trả `Model` (thường bỏ qua) |
| `"redirect:/url"` | Spring MVC (quy ước chuỗi) | Trả 302, trình duyệt tự gọi URL mới | Phần sau `redirect:` là **URL**, không phải tên file |
| `@ResponseStatus(HttpStatus.X)` | Spring MVC | Gắn mã HTTP cho exception hoặc cho hàm | Project: `NotFoundException` → 404 |
| `ResponseEntity.ok(x)`, `.status(...).body(x)` | Spring Web | Tự chọn mã, header, body | Project chưa dùng |
| `MultipartFile` | Spring Web | File upload: `isEmpty()`, `getOriginalFilename()`, `transferTo(path)` | Xem bảng ở [bài 15](../study/15-anh-va-mo-ta-san-pham.md) Bước 4 |
| `Principal.getName()` | Java (`java.security`), Spring Security điền | Tên đăng nhập của người đang gửi request | `String` |
| `WebMvcConfigurer.addResourceHandlers(registry)` | Spring MVC | Thêm cặp URL ↔ thư mục file tĩnh | Spring tự gọi 1 lần lúc khởi động |
| `@JsonIgnore` | Jackson | Bỏ một trường khỏi JSON (cả chiều vào lẫn ra) | Bắt buộc ở `Category.products` |
| `server.error.include-message` | Spring Boot | Cho `${message}` hiện trên trang/JSON lỗi | Project: `always` |
| `spring.servlet.multipart.max-file-size` | Spring Boot | Giới hạn kích thước 1 file upload | Mặc định 1MB, project 5MB |

## Bẫy thường gặp

| Triệu chứng | Nguyên nhân | Cách sửa |
|---|---|---|
| 404 (`No static resource ...`) khi mở `/products/` | Từ Spring 6, dấu `/` ở cuối không còn khớp route không có `/` | Bỏ dấu `/` cuối URL |
| Chưa đăng nhập, mở `/orders` thì bị đưa về `/login` chứ không thấy 404 | Spring Security chặn **trước** `DispatcherServlet`: mọi URL (trừ danh sách `permitAll`) đều phải đăng nhập, kể cả URL không tồn tại | Bình thường. Đăng nhập rồi mở lại thì thấy trang 404 |
| 400 `Required parameter 'categoryId' is not present` | `@RequestParam` bắt buộc nhưng request không gửi | Thêm `required = false` (kiểu wrapper `Long`) hoặc `defaultValue` |
| 400 khi mở `/products/abc` | `abc` không đổi được sang `Long` | Đúng thiết kế |
| 405 `Method Not Allowed` | Form gửi `POST` tới route chỉ có `@GetMapping` (hoặc ngược lại) | So `method="post"` với annotation |
| 415 `Unsupported Media Type` khi gọi API | Gửi body mà thiếu header `Content-Type: application/json` | `curl -H 'Content-Type: application/json' -d '{...}'` |
| Gọi `POST /api/products` thành công nhưng mọi trường đều `null` | Quên `@RequestBody` | Thêm `@RequestBody` trước tham số |
| `Name for argument of type [java.lang.Long] not specified, and parameter name information not available via reflection` | Code được biên dịch **không** có cờ `-parameters` (biên dịch ngoài Gradle) | Build bằng Gradle, hoặc ghi tên rõ: `@PathVariable("id")` |
| App không khởi động, log `Ambiguous mapping` | Hai hàm cùng URL + cùng HTTP method | Đổi đường dẫn một trong hai |
| Lỗi 500, log `Infinite recursion` / `Document nesting depth ... exceeds` | Quan hệ 2 chiều không có `@JsonIgnore` ở một phía | `@JsonIgnore` ở phía `@OneToMany` |
| Gửi form `POST` bị **403** | Form không có CSRF token: viết `action="..."` thay vì `th:action="@{...}"` | Dùng `th:action`, xem [spring-security.md](spring-security.md) và [bài 12 Bước 8](../study/12-dang-nhap-spring-security.md) |
| Chọn ảnh rồi Lưu mà ảnh không đổi, `uploads/` trống | Form thiếu `enctype="multipart/form-data"` | Thêm vào thẻ `<form>` |
| Ảnh > 5MB thì ra **403** hoặc mất kết nối, không có thông báo "file quá lớn" | Vượt `max-file-size`, Tomcat bỏ đọc form, Spring Security không thấy CSRF token | Chọn ảnh nhỏ hơn, hoặc tăng giới hạn ([bài 15](../study/15-anh-va-mo-ta-san-pham.md)) |
| `@RestController` trả về chữ `products/list` thay vì trang HTML | Chuỗi `return` trong REST controller là dữ liệu, không phải tên template | Trang web phải nằm trong `@Controller` |
| 500 `Error resolving template [products/lisst]` | Sai tên template hoặc thiếu file | So chuỗi `return` với đường dẫn trong `templates/` |
| Gửi giá âm qua API thì ra **500**, không phải 400 | `ProductService` ném `IllegalArgumentException`, mà exception này không có `@ResponseStatus` nên bị coi là lỗi server | Đúng như code hiện tại. Muốn 400 thì làm một exception riêng kiểu `NotFoundException`, gắn `@ResponseStatus(HttpStatus.BAD_REQUEST)` |
| Giá lớn hiện thành `2.299E7` trong JSON | `price` là `Double`, Java in số lớn theo dạng khoa học. JSON vẫn hợp lệ, JavaScript đọc ra đúng `22990000` | Không cần sửa. Muốn số nguyên thì phải đổi kiểu trường (kéo theo sửa entity, seeder) |

## Đọc thêm

- Tài liệu chính thức: [Spring Web MVC (Spring Framework 6.2)](https://docs.spring.io/spring-framework/reference/6.2/web/webmvc.html), mục [DispatcherServlet](https://docs.spring.io/spring-framework/reference/6.2/web/webmvc/mvc-servlet.html) và [Annotated Controllers](https://docs.spring.io/spring-framework/reference/6.2/web/webmvc/mvc-controller.html).
- Phần Spring Boot cấu hình sẵn cho MVC (trang lỗi, static, multipart, Jackson): [Servlet Web Applications](https://docs.spring.io/spring-boot/3.4/reference/web/servlet.html).
- Jackson: [Jackson annotations](https://github.com/FasterXML/jackson-annotations/wiki/Jackson-Annotations).
- Bài học: [02 CRUD API](../study/02-thuc-hanh-crud.md) · [05 Thymeleaf](../study/05-giao-dien-thymeleaf.md) · [09 Kiến thức controller](../study/09-kien-thuc-controller.md) · [10 Trang 404](../study/10-trang-404.md) · [11 Chọn danh mục](../study/11-chon-danh-muc-cho-san-pham.md) · [15 Ảnh và upload](../study/15-anh-va-mo-ta-san-pham.md).
- Tài liệu liên quan: [spring-boot.md](spring-boot.md) · [thymeleaf.md](thymeleaf.md) · [spring-security.md](spring-security.md) · [jpa-hibernate.md](jpa-hibernate.md).
