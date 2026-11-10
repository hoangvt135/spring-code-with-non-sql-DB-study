# Thymeleaf

> **Dùng trong project từ:** [bài 05](../study/05-giao-dien-thymeleaf.md) · **Phiên bản:** Thymeleaf 3.1.3 (Spring Boot 3.4.1 tự chọn qua `spring-boot-starter-thymeleaf`), thymeleaf-extras-springsecurity6 3.1.3 (Spring Boot chọn), thymeleaf-layout-dialect **3.4.0** (ghi cứng trong `build.gradle`, Spring Boot 3.4.1 đề xuất 3.3.0) · **Tài liệu chính thức:** [Using Thymeleaf 3.1](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html), [Thymeleaf + Spring](https://www.thymeleaf.org/doc/tutorials/3.1/thymeleafspring.html), [Layout Dialect](https://ultraq.github.io/thymeleaf-layout-dialect/), [Thymeleaf Extras Spring Security](https://github.com/thymeleaf/thymeleaf-extras-springsecurity)
>
> 🔼 **Sau [bài 16.5](../study/16.5-nang-cap-spring-boot-4.md)** (người học chưa làm): Thymeleaf **3.1.5** (vá CVE-2026-40477), Layout Dialect **4.0.1**. **Cấm gọi bean `@...` trong `th:src`/`th:href`**: 3 template sản phẩm được sửa sang đọc `imageUrls` / `imageUrl` do controller đưa vào model (Bước 3).

## Nó là gì, giải quyết vấn đề gì

Thymeleaf là **template engine chạy phía server**, đúng vai trò của Blade bên Laravel: controller đưa dữ liệu vào, Thymeleaf trộn dữ liệu với file HTML, rồi gửi **trang HTML hoàn chỉnh** về trình duyệt. Trình duyệt không biết có Thymeleaf. Nó chỉ nhận HTML thường.

```
GET /products ──► ProductViewController.list(model)
                    │ model.addAttribute("products", ...)
                    │ return "products/list";
                    ▼
        Thymeleaf đọc templates/products/list.html
        + ghép vào khung templates/fragments/layout.html
                    ▼
        HTML hoàn chỉnh ──► trình duyệt
```

Nó dùng cho các controller `@Controller` (ví dụ `ProductViewController`). Controller `@RestController` (ví dụ `ProductController`) trả JSON, không đi qua Thymeleaf. Xem thêm [spring-web-mvc.md](spring-web-mvc.md).

### So với Blade

Khác biệt lớn nhất: Blade dùng **cú pháp riêng** chen vào giữa HTML (`{{ }}`, `@foreach`), còn Thymeleaf viết logic thành **thuộc tính** `th:*` gắn trên chính thẻ HTML.

| Việc cần làm | Blade (PHP) | Thymeleaf |
|---|---|---|
| In biến (có escape) | `{{ $p->name }}` | `<td th:text="${p.name}">Tên</td>` |
| In HTML thô (không escape) | `{!! $html !!}` | `th:utext="${html}"` |
| Vòng lặp | `@foreach ($products as $p)` | `<tr th:each="p : ${products}">` |
| Điều kiện | `@if (...)` / `@unless (...)` | `th:if="${...}"` / `th:unless="${...}"` |
| Tạo URL | `url('/products')`, `route(...)` | `th:href="@{/products}"` |
| Layout cha | `@extends('layout')` + `@section('content')` | `layout:decorate="~{fragments/layout}"` + `layout:fragment="content"` |
| Chỗ trống trong layout | `@yield('content')` | `layout:fragment="content"` (trong layout) |
| Nhúng file con | `@include('partials.x')` | `th:replace="~{partials/x :: ten}"` |
| CSRF trong form | `@csrf` | **Tự thêm**, miễn là form dùng `th:action` |
| Chỉ hiện khi đã đăng nhập | `@auth ... @endauth` | `sec:authorize="isAuthenticated()"` |
| Chuỗi đa ngôn ngữ | `__('messages.key')` | `#{key}` |
| Gọi code PHP tuỳ ý | `@php ... @endphp` | Không có. Chỉ gọi được method qua biểu thức |

## Khái niệm cốt lõi

### Natural template: file HTML vẫn mở được bằng trình duyệt

```html
<td th:text="${p.name}">Tên</td>
```

- Mở thẳng file `.html` bằng trình duyệt (không qua server): trình duyệt bỏ qua thuộc tính lạ `th:text`, hiện chữ **"Tên"**.
- Chạy qua Thymeleaf: nội dung thẻ bị **thay hẳn** bằng giá trị `${p.name}`. Chữ "Tên" biến mất.

Chữ "Tên" gọi là **chữ mẫu** (prototype text). Nó giúp người làm giao diện xem được bố cục mà không cần chạy app. Vì vậy các template trong project luôn có chữ mẫu, ví dụ `<span th:text="${views}">0</span>`.

Khai báo `xmlns:th="http://www.thymeleaf.org"` trên thẻ `<html>`: theo tài liệu chính thức, nó **không ảnh hưởng gì** tới việc xử lý, chỉ để IDE không báo "thuộc tính lạ". Project vẫn giữ trên mọi file (cùng `xmlns:layout`, `xmlns:sec`) cho thống nhất.

### Năm loại biểu thức

| Biểu thức | Tên | Dùng để | Ví dụ trong project |
|---|---|---|---|
| `${...}` | Biến (variable) | Đọc dữ liệu controller đưa vào model, gọi method | `${p.name}`, `${categories.isEmpty()}` |
| `*{...}` | Chọn (selection) | Đọc thuộc tính của object đang gắn bằng `th:object` | `th:field="*{name}"` |
| `@{...}` | Link (URL) | Tạo đường dẫn, tự thêm context path | `@{/products/{id}/edit(id=${p.id})}` |
| `#{...}` | Message | Đọc chuỗi trong `messages.properties` (đa ngôn ngữ) | Chưa dùng |
| `~{...}` | Fragment | Trỏ tới một template hoặc một mảnh template | `layout:decorate="~{fragments/layout}"` |

**`${...}` trong app Spring là SpEL** (Spring Expression Language), không phải OGNL như Thymeleaf dùng khi chạy riêng. Những gì SpEL làm được:

```html
${p.name}                          <!-- gọi p.getName() -->
${c.products.size()}               <!-- gọi method -->
${views.getOrDefault(p.id, 0)}     <!-- views là Map<Long, Long> -->
${product.category?.name}          <!-- ?. : category null thì cả biểu thức là null, không lỗi -->
${product.id == 0}                 <!-- so sánh; còn có !=, <, >, and, or, not -->
${param.error}                     <!-- tham số trên URL: /login?error -->
```

Ngoài `${...}` còn có các phép **của Thymeleaf** dùng được trong mọi thuộc tính `th:*`:

```html
th:text="${category.id == null} ? 'Thêm danh mục' : 'Sửa danh mục'"     <!-- điều kiện ? a : b -->
th:text="${product.category?.name} ?: 'Chưa có'"                        <!-- ?: : null thì dùng giá trị sau -->
th:text="${views.getOrDefault(t.id, 0)} + ' lượt xem'"                  <!-- nối chuỗi bằng + -->
th:text="|Xin chào ${name}|"                                            <!-- |...| : chèn biến vào chuỗi, khỏi nối + -->
```

**`@{...}`** có quy tắc riêng: `{id}` trong đường dẫn là **chỗ trống**, phần `(id=${p.id})` đặt **ở cuối cùng** là giá trị điền vào. Tham số nào không có chỗ trống cùng tên thì thành query string:

```html
@{/products/{id}/edit(id=${p.id})}       → /products/7/edit
@{/products(page=2, sort='name')}        → /products?page=2&sort=name
```

**`#{...}`**: Spring Boot chỉ tạo `MessageSource` khi có file `src/main/resources/messages.properties`. Project chưa có file này, nên `#{...}` hiện ra dạng `??key_vi_VN??` (tên key bọc dấu `??`). Muốn dùng thì tạo file đó trước.

### `th:text` và `th:utext`: escape để chống XSS

```html
<p th:text="${product.description}">...</p>    <!-- <script> → &lt;script&gt; : an toàn -->
<p th:utext="${product.description}">...</p>   <!-- in thẳng HTML : người dùng chèn được <script> -->
```

`th:text` đổi `<`, `>`, `&`, `"` thành dạng an toàn, giống `{{ }}` của Blade. Mô tả sản phẩm do người dùng nhập nên project dùng `th:text`. Chỉ dùng `th:utext` cho HTML **do chính lập trình viên viết**.

Muốn in biến giữa một đoạn chữ mà không cần thẻ bọc: `<p>Xin chào [[${name}]]</p>` (có escape) hoặc `[(${html})]` (không escape).

### `th:each`: vòng lặp

```html
<tr th:each="p : ${products}">...</tr>
<tr th:each="p, stat : ${products}">
    <td th:text="${stat.count}">1</td>    <!-- số thứ tự, bắt đầu từ 1 -->
</tr>
```

- Thẻ chứa `th:each` được **lặp lại cả thẻ** (cả `<tr>`), không chỉ phần bên trong. Muốn lặp mà không sinh thẻ thừa thì dùng thẻ ảo `<th:block th:each="...">`. Thẻ này biến mất sau khi xử lý.
- Biến trạng thái (`stat`, hoặc tự có sẵn tên `pStat` nếu không khai báo) có: `index` (từ 0), `count` (từ 1), `size`, `current`, `even`/`odd`, `first`, `last`.
- Lặp được: `List`, `Set`, mảng, `Map` (mỗi phần tử là `Map.Entry`, đọc bằng `${e.key}`, `${e.value}`), `Stream`... Một object đơn lẻ thì được coi là danh sách 1 phần tử.

**Lặp trên `null` không lỗi.** Mã nguồn Thymeleaf 3.1 (`IteratedGatheringModelProcessable`) xử lý `null` như danh sách rỗng: lặp **0 lần**, thẻ không được sinh ra. Cái gây lỗi 500 là gọi **method** trên `null`, ví dụ `${categories.isEmpty()}` khi controller quên `addAttribute("categories", ...)`. Lúc đó SpEL báo `EL1011E: Method call: Attempted to call method isEmpty() on null context object`.

> [Bài 11 Bước 2](../study/11-chon-danh-muc-cho-san-pham.md#bước-2--đưa-danh-sách-category-vào-model) ghi "quên dòng này thì form lỗi 500 vì `th:each` không lặp được". Theo mã nguồn thì `<select>` chỉ **trống trơn, không báo lỗi gì**. Hậu quả vẫn là lỗi: không chọn được danh mục. Và vì không có lỗi để thấy ngay, nó còn khó phát hiện hơn.

### `th:if` / `th:unless`: hiện hay bỏ cả thẻ

Điều kiện sai thì **cả thẻ và mọi thứ bên trong** bị bỏ khỏi HTML gửi về (không phải ẩn bằng CSS). `th:unless` là ngược lại của `th:if`.

Thymeleaf coi giá trị là **đúng** khi: khác `null`, và không phải `false`, số `0`, hay chuỗi `"false"`, `"off"`, `"no"`. Vì vậy:

- `th:if="${param.error}"`: URL `/login?error` có tham số `error` (dù rỗng) → đúng → hiện thông báo lỗi.
- `th:if="${message}"` ở `error/404.html`: có message → hiện, `null` → bỏ.
- `th:unless="${#strings.isEmpty(p.description)}"`: chỉ hiện khi có mô tả.

### Thuộc tính `th:xxx` cho thuộc tính HTML

Hầu hết thuộc tính HTML đều có bản `th:` tương ứng: tính giá trị rồi ghi đè thuộc tính thường.

| Viết | Ra HTML | Ghi chú |
|---|---|---|
| `th:href="@{/products}"` | `href="/products"` | Link |
| `th:src="@{/css/app.css}"` | `src="..."` | Ảnh, script |
| `th:action="@{/products/save}"` | `action="..."` | Form. **Kèm CSRF** (mục dưới) |
| `th:value="${c.id}"`, `th:alt="${p.name}"` | `value="3"`, `alt="..."` | Bất kỳ thuộc tính nào |
| `th:selected="${...}"` | `selected="selected"` hoặc **không có gì** | Thuộc tính kiểu bật/tắt: đúng thì thêm, sai thì bỏ hẳn. Cùng loại: `th:checked`, `th:disabled`, `th:required` |
| `th:classappend="${p.price == null} ? 'muted'"` | thêm class vào `class` sẵn có | |

### Form: `th:object` + `th:field`

```html
<form th:action="@{/categories/save}" th:object="${category}" method="post">
    <input type="hidden" th:field="*{id}">
    <input type="text" th:field="*{name}" required>
</form>
```

`th:field="*{name}"` sinh ra cùng lúc 3 thuộc tính `id="name" name="name" value="<giá trị hiện tại>"`. Nhờ vậy form "sửa" tự điền sẵn dữ liệu cũ. Với `<textarea>`, nó điền vào nội dung thẻ. Với checkbox, nó tự thêm `checked`. Khi form gửi lên, Spring dùng `name` để gọi setter tương ứng của object `@ModelAttribute` (xem [bài 09](../study/09-kien-thuc-controller.md#modelattribute)).

Ba quy tắc:

- `th:object` phải là **tên một model attribute**: `${category}` thì đúng, `${product.category}` thì sai (theo tài liệu Thymeleaf + Spring).
- `*{...}` chỉ có nghĩa bên trong thẻ có `th:object`. Ngoài đó nó hoạt động như `${...}`.
- Thẻ có `th:field` thì đừng viết thêm `name`, `value` hay `th:selected`: `th:field` sẽ ghi đè. Vì vậy ô chọn danh mục trong `products/form.html` dùng `name="categoryId"` + `th:selected`, **không** dùng `th:field`. Lý do chi tiết ở [bài 11](../study/11-chon-danh-muc-cho-san-pham.md).

### CSRF tự thêm vào form POST

Khi bật Spring Security, mọi request POST phải kèm CSRF token (xem [spring-security.md](spring-security.md)). Thymeleaf phối hợp với Spring Security: gặp `<form th:action="..." method="post">` là tự chèn thêm:

```html
<input type="hidden" name="_csrf" value="B3dFAN8_76Kx..."/>
```

Cơ chế: Spring Security đăng ký một `RequestDataValueProcessor`, và Thymeleaf gọi nó mỗi khi xử lý `th:action`. Form `method="get"` thì không được thêm token. Form viết `action="..."` thường (không có `th:`) thì **không có token → 403**. Chi tiết ở [bài 12 Bước 8](../study/12-dang-nhap-spring-security.md#bước-8--csrf-vì-sao-form-cũ-vẫn-chạy-và-vì-sao-bắt-buộc-dùng-thaction).

### Utility object: các hộp công cụ `#xxx`

Bên trong `${...}`, các tên bắt đầu bằng `#` là object có sẵn của Thymeleaf, chứa hàm tiện ích:

| Object | Hàm hay dùng | Ví dụ |
|---|---|---|
| `#strings` | `isEmpty`, `abbreviate`, `toUpperCase`, `contains`, `substring` | `${#strings.abbreviate(p.description, 80)}` → cắt còn 80 ký tự, đuôi `...` |
| `#numbers` | `formatDecimal`, `formatInteger` | `${#numbers.formatDecimal(p.price, 1, 'POINT', 0, 'COMMA')}` → `1.500.000` |
| `#lists`, `#sets`, `#maps`, `#arrays` | `isEmpty`, `size`, `contains`, `sort` | `${#lists.isEmpty(products)}` (an toàn với `null`) |
| `#temporals` | `format`, `day`, `month`, `year` | `${#temporals.format(order.createdAt, 'dd/MM/yyyy HH:mm')}` cho `LocalDateTime` |
| `#dates` | `format` | Như trên nhưng cho `java.util.Date` (kiểu cũ) |
| `#bools`, `#objects` | `isTrue`, `nullSafe` | `${#objects.nullSafe(p.price, 0)}` |
| `#aggregates` | `sum`, `avg` | `${#aggregates.sum(prices)}` |
| `#ctx`, `#locale` | Ngữ cảnh, ngôn ngữ hiện tại | Ít dùng |

Đọc `formatDecimal(p.price, 1, 'POINT', 0, 'COMMA')` theo thứ tự tham số: giá trị, **ít nhất 1 chữ số** phần nguyên, **dấu chấm** ngăn hàng nghìn, **0 chữ số** thập phân, dấu phẩy ngăn phần thập phân. Kết quả kiểu Việt Nam: `1500000.0` → `1.500.000`.

Hai điểm khác tài liệu cũ trên mạng:

- `#temporals` đã **có sẵn trong Thymeleaf 3.1**. Bài viết cũ bảo thêm thư viện `thymeleaf-extras-java8time` là không cần nữa.
- Thymeleaf 3.1 **bỏ** `#request`, `#session`, `#response`, `#servletContext`. Thay bằng `${param.x}` (tham số URL), `${session.x}` (thuộc tính session), `${application.x}`.

### Gọi bean Spring trong template: `${@productService.imageUrl(p)}`

Dấu `@` trong `${...}` là cú pháp SpEL để **lấy bean Spring theo tên**. Bean của class `ProductService` mặc định tên `productService`. Template gọi thẳng method của bean, controller không phải chuẩn bị gì thêm ([bài 15 Bước 9](../study/15-anh-va-mo-ta-san-pham.md#cú-pháp-mới-productserviceimageurlproduct)).

Project dùng nó trong `th:src`:

```html
<img class="thumb" th:src="@{${@productService.imageUrl(p)}}" th:alt="${p.name}">
```

> ⚠️ **Chỉ chạy được với Thymeleaf ≤ 3.1.3, tức bản project đang dùng.** Vì lý do bảo mật, phần URL gốc của `th:src`/`th:href`, cùng `th:utext` và `th:on*`, chạy ở **chế độ hạn chế** (restricted mode): cấm `T(...)`, cấm `new ...`, cấm `${param.*}`. Từ Thymeleaf **3.1.4**, chế độ này **cấm thêm** việc gọi bean bằng `@tên`. Vi phạm thì trang lỗi 500 lúc chạy (xem [tài liệu Appendix D](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html) và [issue #1093](https://github.com/thymeleaf/thymeleaf/issues/1093)). Nhóm phát triển Thymeleaf đã nói sẽ không mở lại. Nếu sau này nâng Spring Boot (bản mới hơn sẽ kéo theo Thymeleaf mới hơn), 3 template `products/*.html` sẽ phải sửa: tính URL ảnh ở controller rồi đưa vào model. Gọi bean trong `th:text`, `th:if`... thì vẫn được.

### Fragment và layout dialect

Thymeleaf có sẵn cách **nhúng** một mảnh vào trang (giống `@include`):

```html
<!-- templates/fragments/parts.html -->
<footer th:fragment="footer">© 2026</footer>

<!-- trang khác -->
<div th:replace="~{fragments/parts :: footer}"></div>   <!-- thay cả <div> bằng <footer> -->
<div th:insert="~{fragments/parts :: footer}"></div>    <!-- giữ <div>, nhét <footer> vào trong -->
```

Cách này phải nhúng header, footer vào **từng trang**. Project dùng thư viện **thymeleaf-layout-dialect** để làm ngược lại, giống `@extends` của Blade: trang con **khai báo khung** mình dùng, rồi chỉ chứa phần lõi.

```html
<!-- trang con: templates/products/list.html -->
<html layout:decorate="~{fragments/layout}">
<head><title>Danh sách sản phẩm</title></head>
<body>
    <section layout:fragment="content"> ...bảng sản phẩm... </section>
</body>
</html>

<!-- khung: templates/fragments/layout.html -->
<title layout:title-pattern="$CONTENT_TITLE">Demo</title>
...
<main layout:fragment="content">Nội dung mặc định</main>
```

Cách ghép:

1. Thymeleaf lấy **layout** làm trang gốc.
2. Mỗi `layout:fragment="X"` trong layout được **thay** bằng thẻ `layout:fragment="X"` cùng tên của trang con. Ở đây `<main>` của layout được thay bằng `<section>` của trang con.
3. `<head>` của hai bên được gộp lại. `layout:title-pattern` quyết định tiêu đề: `$CONTENT_TITLE` là `<title>` của trang con, `$LAYOUT_TITLE` là của layout. Ví dụ `"$CONTENT_TITLE - Demo"`.
4. Mọi thứ **ngoài** `layout:fragment` trong `<body>` của trang con bị bỏ.

Tài liệu cũ (layout dialect 1.x, 2.x) viết `layout:decorator="layout"`. Từ bản 3.0 thuộc tính này đã bị bỏ, phải dùng `layout:decorate="~{...}"`.

### `sec:authorize`, `sec:authentication`: hiện theo người đăng nhập

Thư viện `thymeleaf-extras-springsecurity6` thêm các thuộc tính `sec:`:

```html
<span sec:authorize="isAuthenticated()">            <!-- như th:if: chỉ giữ khi đã đăng nhập -->
    Xin chào, <b sec:authentication="name">user</b>   <!-- như th:text: in username -->
</span>
<a sec:authorize="hasRole('ADMIN')" th:href="@{/admin}">Quản trị</a>
```

Bản dùng trong biểu thức: `${#authentication.name}`, `${#authorization.expression('hasRole(''ADMIN'')')}`.

`sec:authorize` **chỉ ẩn phần giao diện**. Nó không chặn ai gõ thẳng URL. Việc chặn là của `SecurityConfig`. Ẩn nút "Quản trị" mà quên chặn `/admin/**` trong `SecurityConfig` thì vẫn là lỗ hổng.

### Template nằm đâu, file tĩnh nằm đâu

| Thư mục | Chứa | Truy cập thế nào |
|---|---|---|
| `src/main/resources/templates/` | Template Thymeleaf | **Không** truy cập trực tiếp bằng URL. Controller `return "products/list"` → `templates/products/list.html` (Spring Boot tự thêm tiền tố `templates/` và hậu tố `.html`) |
| `templates/error/` | Trang lỗi theo mã: `404.html`, `500.html`... | Spring Boot tự chọn khi có lỗi ([bài 10](../study/10-trang-404.md)) |
| `src/main/resources/static/` | CSS, ảnh có sẵn của lập trình viên | URL bỏ chữ `static`: `static/css/app.css` → `/css/app.css` |
| `uploads/` (gốc project) | Ảnh người dùng upload | `/uploads/...`, do `WebConfig` map ([bài 15](../study/15-anh-va-mo-ta-san-pham.md)) |

Quy ước của project: CSS **chỉ** viết trong `static/css/app.css`, gắn một lần ở layout bằng `th:href="@{/css/app.css}"`. Không viết `<style>` trong template, không viết `style="..."` trên thẻ.

Cache: mặc định Spring Boot **lưu đệm** template đã đọc (`spring.thymeleaf.cache=true`). DevTools tự đổi thành `false` khi chạy dev, nên sửa `.html` là thấy ngay, miễn là file đã được copy sang `build/` ([bài 04](../study/04-hot-reload.md)).

## Project này dùng nó thế nào

Cấu hình: chỉ có 3 dòng trong `build.gradle`, **không có** dòng `spring.thymeleaf.*` nào trong `application.yml`. Mọi thứ dùng mặc định của Spring Boot.

```groovy
implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
implementation 'nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect:3.4.0'
implementation 'org.thymeleaf.extras:thymeleaf-extras-springsecurity6'
```

Các template và cú pháp mỗi file dùng:

| File | Controller | Cú pháp đáng chú ý | Bài |
|---|---|---|---|
| `fragments/layout.html` | (khung chung) | `layout:fragment`, `layout:title-pattern`, `sec:authorize`, `sec:authentication`, form đăng xuất POST | [07](../study/07-tach-giao-dien-fragment.md), [12](../study/12-dang-nhap-spring-security.md#bước-9--nav-xin-chào-an--nút-đăng-xuất) |
| `products/list.html` | `ProductViewController.list` | `th:each`, `th:unless`, `#strings.abbreviate`, `#numbers.formatDecimal`, `?.`, `views.getOrDefault`, `@productService` | [05](../study/05-giao-dien-thymeleaf.md), [11](../study/11-chon-danh-muc-cho-san-pham.md), [15](../study/15-anh-va-mo-ta-san-pham.md), [16](../study/16-luot-xem-top-san-pham-redis.md) |
| `products/detail.html` | `ProductViewController.detail` | `?:`, `th:text` trên `<title>`, `th:each` trên `<a>` | [16](../study/16-luot-xem-top-san-pham-redis.md#bước-6--detailhtml-trang-chi-tiết) |
| `products/form.html` | `.../new`, `.../{id}/edit` | `th:object`, `th:field`, `<select>` với `th:each` + `th:selected`, `enctype="multipart/form-data"` | [05](../study/05-giao-dien-thymeleaf.md), [11](../study/11-chon-danh-muc-cho-san-pham.md), [15](../study/15-anh-va-mo-ta-san-pham.md) |
| `categories/list.html`, `categories/form.html` | `CategoryViewController` | `${category.id == null}` (vì `Category.id` là `Long`), `c.products.size()` | [08](../study/08-giao-dien-category.md) |
| `auth/login.html`, `auth/register.html` | `AuthController` | `${param.error}`, form POST `/login` có CSRF | [12](../study/12-dang-nhap-spring-security.md#bước-7--template-đăng-nhập--đăng-ký) |
| `error/404.html` | Spring Boot tự gọi | `${path}`, `${message}` | [10](../study/10-trang-404.md) |

Một ví dụ đủ nhiều cú pháp, trích `products/list.html`:

```html
<tr th:each="p : ${products}">
    <td><img class="thumb" th:src="@{${@productService.imageUrl(p)}}" th:alt="${p.name}" loading="lazy"></td>
    <td>
        <b><a th:href="@{/products/{id}(id=${p.id})}" th:text="${p.name}">Tên</a></b>
        <span class="muted"
              th:unless="${#strings.isEmpty(p.description)}"
              th:text="${#strings.abbreviate(p.description, 80)}">Mô tả ngắn</span>
    </td>
    <td th:text="${p.category?.name}">Danh mục</td>
    <td class="right" th:text="${views.getOrDefault(p.id, 0)}">0</td>
</tr>
<tr th:if="${products.isEmpty()}"><td colspan="6">Chưa có sản phẩm nào.</td></tr>
```

Phân biệt "thêm" và "sửa" trong template theo kiểu `id`:

- `products/form.html`: `${product.id == 0}`, vì `Product.id` là `long` nguyên thuỷ.
- `categories/form.html`: `${category.id == null}`, vì `Category.id` là `Long`.

Lý do ở [bài 08](../study/08-giao-dien-category.md).

Doc chức năng có mô tả giao diện: [quản lý sản phẩm](../product/quan-ly-san-pham.md), [lượt xem & top](../product/luot-xem-va-top-san-pham.md), [quản lý danh mục](../category/quan-ly-danh-muc.md), [đăng nhập & đăng ký](../user/dang-nhap-dang-ky.md), [trang lỗi 404](../common/trang-loi-404.md).

## Tra cứu nhanh

| Cú pháp | Của ai | Làm gì | Ghi chú |
|---|---|---|---|
| `th:text="${x}"` | Thymeleaf | Thay nội dung thẻ, có escape | An toàn với dữ liệu người dùng |
| `th:utext="${x}"` | Thymeleaf | Thay nội dung thẻ, **không** escape | Nguy cơ XSS. Chạy ở chế độ hạn chế |
| `[[${x}]]` / `[(${x})]` | Thymeleaf | In biến giữa đoạn chữ (escape / không) | Không cần thẻ bọc |
| `th:each="p, stat : ${list}"` | Thymeleaf | Lặp lại cả thẻ | `null` → lặp 0 lần |
| `th:if` / `th:unless` | Thymeleaf | Giữ hoặc bỏ cả thẻ | `0`, `"false"`, `"off"`, `"no"`, `null` là sai |
| `th:href`, `th:src`, `th:action` | Thymeleaf | Gán thuộc tính URL | Đi với `@{...}`. `th:action` kèm CSRF |
| `th:selected`, `th:checked` | Thymeleaf | Thêm hoặc bỏ thuộc tính bật/tắt | |
| `th:object="${o}"` + `th:field="*{f}"` | Thymeleaf + Spring | Gắn form với object, sinh `id`/`name`/`value` | `th:object` chỉ nhận tên model attribute |
| `th:block` | Thymeleaf | Thẻ ảo, biến mất sau khi xử lý | Lặp/điều kiện không sinh thẻ thừa |
| `th:fragment`, `th:replace`, `th:insert` | Thymeleaf | Khai báo và nhúng mảnh | `th:include` đã cũ, không dùng |
| `@{/a/{id}(id=${x}, q=1)}` | Thymeleaf | Tạo URL `/a/5?q=1` | `(...)` luôn ở **cuối** |
| `a ? b : c`, `a ?: b`, `\|...${x}...\|` | Thymeleaf | Điều kiện, giá trị mặc định khi null, chèn biến vào chuỗi | |
| `${a?.b}` | SpEL (Spring) | `a` null thì trả null, không lỗi | |
| `${@tenBean.method()}` | SpEL (Spring) | Gọi bean Spring | Cấm trong `th:src`/`th:href` từ 3.1.4 |
| `${param.x}`, `${session.x}` | Thymeleaf | Tham số URL, thuộc tính session | Thay `#request`, `#session` (đã bỏ ở 3.1) |
| `#strings`, `#numbers`, `#lists`, `#temporals` | Thymeleaf | Hàm tiện ích | `#temporals` có sẵn từ 3.1 |
| `layout:decorate="~{fragments/layout}"` | Layout dialect | Trang con chọn khung | Đặt trên `<html>` |
| `layout:fragment="content"` | Layout dialect | Ô trống (layout) / phần lõi (trang con) | Tên hai bên phải khớp |
| `layout:title-pattern="$CONTENT_TITLE"` | Layout dialect | Ghép tiêu đề | Có thêm `$LAYOUT_TITLE` |
| `sec:authorize="isAuthenticated()"` | extras-springsecurity6 | Giữ thẻ khi biểu thức đúng | Chỉ ẩn giao diện, không bảo vệ URL |
| `sec:authentication="name"` | extras-springsecurity6 | In thuộc tính của người đăng nhập | |

## Bẫy thường gặp

| Triệu chứng | Nguyên nhân | Cách sửa |
|---|---|---|
| Lỗi 500, log có `Error resolving template [products/lits]` | Tên trả về từ controller sai, hoặc file không nằm trong `templates/` | Kiểm tra tên file và đường dẫn tương đối |
| Lỗi 500, `EL1007E: Property or field 'name' cannot be found on null` | Đọc thuộc tính của biến `null`, ví dụ `${product.category.name}` khi chưa có danh mục | Dùng `?.`: `${product.category?.name}` |
| Lỗi 500, `EL1011E: Method call: Attempted to call method isEmpty() on null context object` | Controller quên `model.addAttribute(...)` cho biến đó | Thêm `addAttribute`. Hoặc dùng `#lists.isEmpty(x)` (an toàn với null) |
| `<select>` danh mục trống trơn, không có lỗi gì | Quên `addAttribute("categories", ...)`. `th:each` trên `null` lặp 0 lần | Thêm `addAttribute` ở **cả** `newForm` và `editForm` |
| Lỗi 500 ở `@{...}` | `(id=...)` không nằm ở cuối: `@{/products/{id}(id=${p.id})/edit}` | `@{/products/{id}/edit(id=${p.id})}` |
| Bấm Lưu bị **403 Forbidden** | Form viết `action="..."` thường, không có CSRF token | Đổi thành `th:action="@{...}"` |
| Thấy nguyên chữ mẫu ("Tên", "0 ₫") trên trình duyệt | Trang không đi qua Thymeleaf: mở file trực tiếp, để file trong `static/`, hoặc controller là `@RestController` | Để file trong `templates/` và trả về từ `@Controller` |
| Trang con không có header/footer | Sai đường dẫn trong `layout:decorate`, ví dụ `~{layout}` thay vì `~{fragments/layout}` | Đường dẫn tính từ `templates/`, không có `.html` |
| Phần lõi không hiện, chỉ thấy "Nội dung trang sẽ được chèn vào đây" | Tên `layout:fragment` ở trang con khác tên trong layout | Cả hai cùng là `content` |
| Nội dung đặt ngoài `<section layout:fragment>` biến mất | Layout dialect bỏ mọi thứ ngoài fragment | Đưa vào trong fragment |
| Mô tả có `<b>` hiện nguyên chữ `<b>` | `th:text` escape HTML, đúng như thiết kế | Giữ nguyên. Chỉ dùng `th:utext` với HTML tin cậy được |
| Danh mục đã lưu nhưng form sửa không chọn sẵn | Viết `th:field` cùng `th:selected`, hoặc so sánh `Long` với `long` sai | Làm như `products/form.html`, xem [bài 11](../study/11-chon-danh-muc-cho-san-pham.md) |
| Nâng Spring Boot xong, trang sản phẩm lỗi `forbidden in this context` | Thymeleaf ≥ 3.1.4 cấm `@bean` trong `th:src` | Tính URL ảnh ở controller, đưa vào model |
| Sửa `.html` không thấy đổi (chế độ dev) | File chưa được copy sang `build/`, hoặc trình duyệt giữ bản cũ | Xem log `app-dev`, tải lại trang. [Bài 04](../study/04-hot-reload.md) |
| Nút "Quản trị" đã ẩn mà user thường vẫn vào được `/admin` | `sec:authorize` chỉ ẩn giao diện | Chặn thêm trong `SecurityConfig` |
| `#{key}` hiện `??key_vi_VN??` | Không có `messages.properties` hoặc thiếu key | Tạo file `src/main/resources/messages.properties` |

## Đọc thêm

- [Using Thymeleaf 3.1](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html): tài liệu gốc. Đáng đọc nhất là mục 4 (biểu thức), 6 (vòng lặp), 7 (điều kiện), 8 (fragment), Appendix B (các `#xxx`) và Appendix D (chế độ hạn chế).
- [Thymeleaf + Spring](https://www.thymeleaf.org/doc/tutorials/3.1/thymeleafspring.html): SpEL, `@bean`, form `th:object`/`th:field`, hiện lỗi validation (`#fields`).
- [Thymeleaf Layout Dialect](https://ultraq.github.io/thymeleaf-layout-dialect/): `layout:decorate`, `layout:title-pattern`, cách gộp `<head>`.
- [thymeleaf-extras-springsecurity](https://github.com/thymeleaf/thymeleaf-extras-springsecurity): đủ danh sách thuộc tính `sec:`.
- Trong repo: [spring-web-mvc.md](spring-web-mvc.md) (controller, `Model`), [spring-security.md](spring-security.md) (CSRF, đăng nhập), [spring-boot.md](spring-boot.md) (auto-configuration).
