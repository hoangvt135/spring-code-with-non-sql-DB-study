# Quản lý sản phẩm

> **Module:** `product` · **Bài học nguồn:** [bài 02](../study/02-thuc-hanh-crud.md), [bài 05](../study/05-giao-dien-thymeleaf.md), [bài 06](../study/06-quan-he-model.md), [bài 09](../study/09-kien-thuc-controller.md), [bài 10](../study/10-trang-404.md), [bài 11](../study/11-chon-danh-muc-cho-san-pham.md), [bài 15](../study/15-anh-va-mo-ta-san-pham.md), [bài 16](../study/16-luot-xem-top-san-pham-redis.md) · **Cập nhật:** 2026-11-10
> **Trạng thái code:** 🔄 Đang làm (bài 15 Bước 10–11 còn thiếu CSS và cấu hình Docker; bài 16 đã có code tới Bước 8, chưa chạy kiểm tra Bước 9) — theo code thật ngày 2026-11-10

## Chức năng này làm gì

Thêm, xem, sửa, xoá sản phẩm. Mỗi sản phẩm có tên, giá, mô tả, một danh mục (có thể bỏ trống) và một ảnh (có thể bỏ trống, khi đó hiện ảnh mặc định). Chức năng có hai lối vào dùng chung `ProductService`:

- **REST API** `/api/products`: trả JSON, đăng nhập bằng HTTP Basic (`curl -u admin:admin123`). Không upload ảnh được.
- **Trang web** `/products`: Thymeleaf, đăng nhập bằng form. Có chọn danh mục và upload ảnh.

Trang chi tiết `/products/{id}`, cột "Lượt xem" và khung "Xem nhiều nhất" thuộc phần lượt xem (bài 16), xem [luot-xem-va-top-san-pham.md](luot-xem-va-top-san-pham.md).

## Đường dẫn & giao diện

| Method | URL | Controller#method | Mô tả |
|---|---|---|---|
| GET | `/api/products` | `ProductController#getAll` | Danh sách sản phẩm (JSON, kèm `category`) |
| GET | `/api/products/{id}` | `ProductController#getOne` | Một sản phẩm. Không có → 404 |
| POST | `/api/products` | `ProductController#create` | Thêm. Body JSON, ví dụ `{"name":"K2","price":1890000,"category":{"id":1}}` |
| PUT | `/api/products/{id}` | `ProductController#update` | Sửa tên, giá, mô tả, danh mục. Không đổi ảnh |
| DELETE | `/api/products/{id}` | `ProductController#delete` | Xoá sản phẩm, xoá file ảnh, dọn Redis |
| GET | `/products` | `ProductViewController#list` | Trang `products/list`: bảng có ảnh thumbnail, mô tả rút gọn 80 ký tự, danh mục, giá |
| GET | `/products/new` | `ProductViewController#createForm` | Trang `products/form` rỗng (`id = 0` → tiêu đề "Thêm sản phẩm") |
| GET | `/products/{id}/edit` | `ProductViewController#editForm` | Trang `products/form` đã điền sẵn, có ảnh hiện tại |
| POST | `/products/save` | `ProductViewController#save` | Nhận form `multipart/form-data` (thêm và sửa đi chung), xong `redirect:/products` |
| GET | `/products/{id}/delete` | `ProductViewController#delete` | Xoá (link có `confirm()`), xong `redirect:/products` |
| GET | `/products/{id}` | `ProductViewController#detail` | Trang chi tiết + đếm lượt xem (bài 16, xem doc lượt xem) |

> ⚠️ Khác bài học: bài 05 dùng `/products/edit/{id}` và `/products/delete/{id}`. Code thật (và template trong các bài từ 07 trở đi) dùng `/products/{id}/edit` và `/products/{id}/delete`.
> ⚠️ Khác bài học: bài 06 Bước 5 thêm `GET /api/products/by-category/{categoryId}` và `ProductRepository.findByCategoryId(...)`. Code thật **không có** endpoint và hàm này.

## Luồng xử lý

**Lưu từ form web** (`POST /products/save`):

```
form (name, price, description, id ẩn, categoryId, imageFile)
  → ProductViewController.save
      ├─ categoryId != null → categoryService.findById(categoryId)
      │                         └─ không có → NotFoundException → trang 404
      └─ productService.save(product, imageFile)
           1) fileStorageService.save(imageFile)        // lưu file TRƯỚC, chưa đụng DB
                ├─ không chọn file → null (giữ ảnh cũ)
                ├─ đuôi không phải jpg/jpeg/png/gif/webp → IllegalArgumentException → 500
                └─ OK → tên file mới "<uuid>.<đuôi>"
           2) id == 0 ? create(data) : update(id, data)
                ├─ create: giá < 0 → IllegalArgumentException → 500; ép imagePath = null
                └─ update: findById (không có → 404); copy name, price, category, description
           3) có ảnh mới → gán imagePath, save lại, xoá file ảnh cũ
  → redirect:/products
File > 5MB → Tomcat bỏ đọc form → mất CSRF token → 403 (xem bài 15, Lỗi thường gặp)
```

**Xoá** (`DELETE /api/products/{id}` hoặc `GET /products/{id}/delete`):

```
productService.delete(id)
  → findById (không có → NotFoundException → 404)
  → productRepository.delete(product)                  // MySQL
  → fileStorageService.delete(imagePath)                // file ảnh; lỗi chỉ ghi log warn
  → productStatsService.removeProduct(id)               // Redis; lỗi chỉ ghi log warn
```

**Hiện ảnh**: template gọi `${@productService.imageUrl(p)}` → `imagePath == null` thì trả `/images/product-default.jpg`, ngược lại trả `/uploads/<tên file>`. `WebConfig` map `/uploads/**` tới thư mục upload thật trên đĩa.

## File liên quan

| File | Vai trò |
|---|---|
| `product/Product.java` | Entity, bảng `product` |
| `product/ProductRepository.java` | `JpaRepository<Product, Long>`, không có hàm tự viết |
| `product/ProductService.java` | Nghiệp vụ: `findAll`, `findById`, `create`, `update`, `delete`, `save(data, imageFile)`, `imageUrl`. Thêm `topViewed`, `recentlyViewed` (bài 16) |
| `product/ProductController.java` | REST API `/api/products` |
| `product/ProductViewController.java` | Trang web `/products`, nhận `categoryId` và `imageFile` |
| `templates/products/list.html` | Bảng danh sách |
| `templates/products/form.html` | Form thêm/sửa (`enctype="multipart/form-data"`, `<select name="categoryId">`, `<textarea>` mô tả, ô chọn ảnh) |
| `templates/products/detail.html` | Trang chi tiết (bài 16) |
| `storage/FileStorageService.java` | Lưu/xoá file ảnh, kiểm tra đuôi file, chặn tên file thoát khỏi thư mục upload |
| `config/WebConfig.java` | Cho trình duyệt xem file trong thư mục upload qua `/uploads/**` |
| `config/SecurityConfig.java` | `permitAll()` cho `/images/**` và `/uploads/**` |
| `common/NotFoundException.java` | `@ResponseStatus(404)`, ném từ `findById` |
| `templates/error/404.html` | Trang 404 cho giao diện web |
| `static/images/product-default.jpg` | Ảnh mặc định |
| `static/css/app.css` | `.thumb`, `.right`, `textarea.form_controll` (bài 15); `.detail`, `.description`... (bài 16) |
| `resources/config/product.properties`, `upload.properties` | Cấu hình (xem dưới) |

## Dữ liệu

Entity `Product` (`@Table(name="Product")`, Hibernate tự sinh bảng nhờ `ddl-auto: update`):

| Trường | Cột | Kiểu Java | Ràng buộc |
|---|---|---|---|
| `id` | `id` | `long` (nguyên thuỷ) | `@Id`, `IDENTITY` (MySQL tự tăng). Object mới có `id = 0` |
| `name` | `name` | `String` | `nullable = false` |
| `price` | `price` | `Double` | Có thể `null`. Service chặn giá âm khi **thêm** |
| `description` | `description` | `String` | `length = 2000` (mặc định chỉ 255) |
| `imagePath` | `image_path` | `String` | Chỉ lưu **tên file** (`<uuid>.jpg`), `null` = chưa có ảnh |
| `category` | `category_id` | `Category` | `@ManyToOne` + `@JoinColumn`, có thể `null` |

> ⚠️ Khác bài học: bài 02 khai báo `private Long id`. Code thật là `long` (bài 06 Bước 2 đổi mà không giải thích), nên mọi chỗ kiểm tra "sản phẩm mới" đều dùng `id == 0`.

- **Quan hệ:** `Category 1 — N Product`. `Product` là phía chủ (giữ khoá ngoại). `Category.products` có `@JsonIgnore` để JSON không lặp vô tận ([bài 06](../study/06-quan-he-model.md) Bước 3). Chi tiết: [quan-ly-danh-muc.md](../category/quan-ly-danh-muc.md).
- **File ngoài DB:** nội dung ảnh nằm trong thư mục `uploads/` (đã có trong `.gitignore`), xem [luu-file-upload.md](../storage/luu-file-upload.md).
- **Dữ liệu ngoài MySQL:** lượt xem nằm trong Redis (key `demo:product:views`), xem [luot-xem-va-top-san-pham.md](luot-xem-va-top-san-pham.md).
- **Dữ liệu mẫu:** `DataSeeder` tạo 5 sản phẩm, chưa truyền mô tả, xem [du-lieu-mau.md](../seed/du-lieu-mau.md).

## Cấu hình

Đọc qua `AppConfig` trong constructor (cách nạp file: [cau-hinh-module.md](../common/cau-hinh-module.md)).

| File | Key | Giá trị hiện tại | Ý nghĩa |
|---|---|---|---|
| `config/product.properties` | `app.product.default-image` | `images/product-default.jpg` | Ảnh mặc định, tương đối so với `static/`. Không có file → log `warn` lúc khởi động |
| `config/upload.properties` | `app.upload.dir` | `${UPLOAD_DIR:uploads}` | Thư mục lưu ảnh (do `FileStorageService` đọc) |
| `application.yml` | `spring.servlet.multipart.max-file-size` / `max-request-size` | `5MB` / `6MB` | Giới hạn upload (cấu hình của Spring nên vẫn ở `application.yml`) |

> ⚠️ Khác bài học: bài 15 đặt ảnh mặc định là `images/product-default.svg`. Code thật dùng `.jpg`.
> ⚠️ Khác bài học: bài 15 đọc cấu hình bằng `@Value`. Code đã chuyển sang `AppConfig` (bài 16 Bước 4), nhưng `ProductService.java` vẫn còn dòng `import ...annotation.Value;` thừa, và field `productRepository` chưa có `final` như trong bài.

## Quy tắc & lưu ý khi sửa

- **`id` là `long` nguyên thuỷ.** `ProductService.save()` và `form.html` (`${product.id == 0}`) dựa vào điều này. Đổi sang `Long` thì phải sửa thành `id == null`, nếu không sẽ NPE.
- **`update()` cố ý không copy `imagePath`**: form không gửi trường này, copy vào là xoá mất ảnh mỗi lần bấm Lưu. `create()` ép `imagePath = null` để client không tự gửi tên file lên. Ảnh chỉ được gán trong `save(data, imageFile)`.
- **`update()` copy cả `category` và `description`**: `PUT /api/products/{id}` không kèm `"category"` hay `"description"` thì hai trường này bị đặt về `null`.
- **Chặn giá âm chỉ có ở `create()`**, `update()` không kiểm tra. Lỗi nghiệp vụ là `IllegalArgumentException` → HTTP 500 (chưa có trang lỗi riêng, chỉ có `error/404.html`).
- **Ô file tên `imageFile`, khác `imagePath`**, là cố ý: trùng tên thì `@ModelAttribute` cố nhét file vào trường `String` và báo lỗi.
- **`categoryId` nhận riêng bằng `@RequestParam(required = false)`**, không dùng `th:field`. Lý do ở [bài 11](../study/11-chon-danh-muc-cho-san-pham.md).
- **Không có transaction chung** giữa MySQL, thư mục `uploads/` và Redis. Xoá sản phẩm thì phải tự xoá file ảnh và dọn Redis như trong `delete()`. Thêm DB phụ mới (ví dụ đánh giá ở MongoDB) thì nhớ dọn thêm ở đây.
- Controller không gọi thẳng Repository. Thêm trường vào `Product` thì nhớ sửa `update()`, `form.html` và `DataSeeder`.
- Link Xoá là `GET` nên không được CSRF bảo vệ (bài tập 4 của [bài 12](../study/12-dang-nhap-spring-security.md)).
- **Còn dở (bài 15):** `app.css` chưa có class `.preview` và `.muted` (Bước 10) mà `form.html`, `list.html`, `detail.html` đang dùng. `.dockerignore` chưa có `uploads/`, `docker-compose.yml` chưa có volume `uploads_data` (Bước 11), nên chạy chế độ thường thì build lại container là mất ảnh.

## Liên quan

- [Quản lý danh mục](../category/quan-ly-danh-muc.md): nguồn danh sách cho `<select>` và `categoryService.findById`.
- [Lưu file upload](../storage/luu-file-upload.md): `FileStorageService`, `WebConfig`.
- [Trang lỗi 404](../common/trang-loi-404.md): `NotFoundException`.
- [Cấu hình module](../common/cau-hinh-module.md): `config/*.properties`, `AppConfig`.
- [Lượt xem và top sản phẩm](luot-xem-va-top-san-pham.md): trang chi tiết, Redis.
- [Dữ liệu mẫu](../seed/du-lieu-mau.md): `DataSeeder`.
- Kiến thức controller (`@PathVariable`, `@ModelAttribute`, `redirect:`): [bài 09](../study/09-kien-thuc-controller.md).

## Lịch sử thay đổi

| Bài | Thay đổi |
|---|---|
| [02](../study/02-thuc-hanh-crud.md) | Entity `Product` (id, tên, giá), Repository, Service chặn giá âm, REST API CRUD `/api/products` |
| [05](../study/05-giao-dien-thymeleaf.md) | Trang web `/products`: `ProductViewController`, `list.html`, `form.html`, lưu chung qua `POST /save` |
| [06](../study/06-quan-he-model.md) | Thêm `category` (`@ManyToOne`), `id` đổi sang `long` |
| [07](../study/07-tach-giao-dien-fragment.md) | Template dùng layout chung. `list.html` trong bài đã dùng link `/{id}/edit`, `/{id}/delete`, nhưng không có bước nào sửa mapping tương ứng trong controller |
| [10](../study/10-trang-404.md) | `findById` ném `NotFoundException` → 404 thay cho 500 |
| [11](../study/11-chon-danh-muc-cho-san-pham.md) | `<select>` chọn danh mục, nhận `categoryId`, `update()` copy `category`, cột Danh mục ở danh sách |
| [12](../study/12-dang-nhap-spring-security.md) | Mọi đường dẫn bắt đăng nhập; REST dùng HTTP Basic |
| [15](../study/15-anh-va-mo-ta-san-pham.md) | Thêm `description`, `imagePath`; `save(data, imageFile)` chuyển logic thêm/sửa từ controller vào service; `delete()` xoá cả file ảnh; `imageUrl()`; cột ảnh thumbnail |
| [16](../study/16-luot-xem-top-san-pham-redis.md) | Cấu hình sang `config/product.properties` + `AppConfig`; `delete()` dọn Redis; thêm trang chi tiết, `topViewed()`, `recentlyViewed()` |
