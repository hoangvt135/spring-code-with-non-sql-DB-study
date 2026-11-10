# Quản lý danh mục

> **Module:** `com.example.demo.category` · **Bài học nguồn:** [bài 06](../study/06-quan-he-model.md), [bài 08](../study/08-giao-dien-category.md), [bài 10](../study/10-trang-404.md), [bài 11](../study/11-chon-danh-muc-cho-san-pham.md), [bài 17](../study/17-lombok-slf4j-getter-setter.md) · **Cập nhật:** 2026-11-10
> **Trạng thái code:** ✅ Hoàn thành (CRUD web + REST) — theo code thật ngày 2026-11-10. Bài tập "xoá an toàn" của bài 08 chưa làm.

## Chức năng này làm gì

- Thêm / xem / sửa / xoá **danh mục** (`Category`) trên trình duyệt (`/categories`) và qua REST API (`/api/categories`).
- Một danh mục có nhiều sản phẩm (quan hệ **1–N** với `Product`). Trang danh sách hiện thêm cột **Số sản phẩm** của từng danh mục.
- Cung cấp danh sách danh mục cho ô chọn (dropdown) trong form sản phẩm, xem [Quản lý sản phẩm](../product/quan-ly-san-pham.md).
- Mọi đường dẫn đều phải đăng nhập (xem [Đăng nhập / đăng ký](../user/dang-nhap-dang-ky.md)).

## Đường dẫn & giao diện

| Method | URL | Controller#method | Mô tả |
|---|---|---|---|
| GET | `/categories` | `CategoryViewController#list` | Trang danh sách → `categories/list` |
| GET | `/categories/new` | `CategoryViewController#createForm` | Form thêm (rỗng) → `categories/form` |
| GET | `/categories/{id}/edit` | `CategoryViewController#editForm` | Form sửa; id không tồn tại → 404 |
| POST | `/categories/save` | `CategoryViewController#save` | `id == null` → thêm, có id → sửa; xong `redirect:/categories` |
| GET | `/categories/{id}/delete` | `CategoryViewController#delete` | Xoá rồi `redirect:/categories` (link có `confirm()` ở trình duyệt) |
| GET | `/api/categories` | `CategoryController#getAll` | JSON danh sách (không kèm `products`) |
| GET | `/api/categories/{id}` | `CategoryController#getOne` | JSON 1 danh mục; không có → 404 JSON |
| POST | `/api/categories` | `CategoryController#create` | Body `{"name":"..."}` → trả danh mục vừa tạo |
| DELETE | `/api/categories/{id}` | `CategoryController#delete` | Xoá, trả 200 không có body |

REST **không có** `PUT` (sửa danh mục chỉ làm được qua web). Gọi REST bằng HTTP Basic: `curl -u admin:admin123 http://localhost:8093/api/categories`.

## Luồng xử lý

```
Lưu (POST /categories/save, form: id, name)
  CategoryViewController.save(@ModelAttribute Category)
   ├── id == null ──► CategoryService.creatCategory() ──► repository.save()  (INSERT)
   └── id có số  ──► CategoryService.updateCategory(id, data)
                        ├── findById(id) ── không thấy ──► NotFoundException ──► 404 (error/404.html)
                        └── setName(...) ──► repository.save()  (UPDATE)
  ──► redirect:/categories

Xoá (GET /categories/{id}/delete hoặc DELETE /api/categories/{id})
  CategoryService.delete(id) ──► repository.deleteById(id)
   ├── danh mục không còn sản phẩm ──► xoá xong
   ├── id không tồn tại ──► không làm gì, không báo lỗi (Spring Data 3 bỏ qua)
   └── còn product trỏ tới (product.category_id) ──► MySQL chặn khoá ngoại
          ──► DataIntegrityViolationException ──► 500 (log: "foreign key constraint fails")

Danh sách (GET /categories)
  findAll() ──► categories/list.html ──► mỗi dòng gọi c.products.size()
            (tải lười: mỗi danh mục thêm 1 câu SELECT product)
```

## File liên quan

| File | Vai trò |
|---|---|
| `src/main/java/com/example/demo/category/Category.java` | Entity, bảng `category`, phía phụ của quan hệ (`@OneToMany(mappedBy)` + `@JsonIgnore`) |
| `src/main/java/com/example/demo/category/CategoryRepository.java` | `JpaRepository<Category, Long>`, không có hàm tự khai báo |
| `src/main/java/com/example/demo/category/CategoryService.java` | `findAll`, `findById` (ném `NotFoundException`), `creatCategory`, `updateCategory`, `delete` |
| `src/main/java/com/example/demo/category/CategoryController.java` | REST `/api/categories` |
| `src/main/java/com/example/demo/category/CategoryViewController.java` | Web `/categories` |
| `src/main/resources/templates/categories/list.html` | Bảng danh mục + cột Số sản phẩm |
| `src/main/resources/templates/categories/form.html` | Form dùng chung thêm/sửa, ô ẩn `id` |
| `src/main/java/com/example/demo/product/Product.java` | Phía chủ của quan hệ (`@ManyToOne` + `@JoinColumn(name = "category_id")`) |
| `src/main/java/com/example/demo/product/ProductViewController.java` | Dùng `CategoryService` cho dropdown và nhận `categoryId` (bài 11) |
| `src/main/java/com/example/demo/common/NotFoundException.java` | Exception gắn sẵn mã 404 (bài 10) |

## Dữ liệu

Bảng `category` (Hibernate tự sinh, `ddl-auto: update`):

| Trường | Kiểu | Ràng buộc |
|---|---|---|
| `id` | `Long` (class bọc) | Khoá chính, tự tăng (`IDENTITY`). Object mới có `id = null` |
| `name` | `String` | `NOT NULL`; form có `required` |
| `products` | `List<Product>` | Không phải cột. `@OneToMany(mappedBy = "category")`, `@JsonIgnore`, mặc định `new ArrayList<>()` |

Quan hệ **Category 1 — N Product**: khoá ngoại `category_id` nằm ở bảng `product` (phía "nhiều" là phía chủ). Không khai báo `cascade` hay `orphanRemoval`. `Product.category` được phép `null` (sản phẩm chưa có danh mục).
Chi tiết: [bài 06, phần 2](../study/06-quan-he-model.md#2-hai-phía-của-quan-hệ-chủ-và-phụ).

JSON: `GET /api/products` có kèm `category` của sản phẩm. `GET /api/categories` **không** kèm `products`, vì `@JsonIgnore` chặn vòng lặp vô tận `Product → category → products → ...` ([bài 06, Bước 3](../study/06-quan-he-model.md)). Thymeleaf vẫn đọc được `c.products`, vì `@JsonIgnore` chỉ tác dụng khi xuất JSON.

Dữ liệu mẫu: `DataSeeder` tạo 3 danh mục (Điện thoại, Bàn phím, Chuột) nếu bảng `category` đang trống, xem [Dữ liệu mẫu](../seed/du-lieu-mau.md).

## Cấu hình

Không có.

## Quy tắc & lưu ý khi sửa

- **Thêm hay sửa:** `Category.id` là `Long`, nên kiểm tra `getId() == null` (template: `${category.id == null}`). Đừng chép `== 0` từ Product, sẽ bị `NullPointerException` ([bài 08](../study/08-giao-dien-category.md#bước-1--tạo-categoryviewcontroller)).
- **Tên hàm `creatCategory`** (thiếu chữ "e") được dùng ở cả `CategoryController` và `CategoryViewController`. Đổi tên thì sửa cả hai.
- **Xoá danh mục còn sản phẩm → 500.** Code chưa kiểm tra trước. Muốn báo lỗi dễ hiểu thì làm bài tập 2 của [bài 08](../study/08-giao-dien-category.md#bài-tập): kiểm tra `getProducts().isEmpty()` trong `CategoryService.delete()`.
- **Xoá bằng GET** (`/categories/{id}/delete`) nên CSRF không bảo vệ link này. Xem bài tập 4 của [bài 12](../study/12-dang-nhap-spring-security.md#bài-tập).
- **Quan hệ 2 chiều mới** thì làm y hệt: phía `@OneToMany` bắt buộc có `@JsonIgnore`.
- `POST /api/categories` gửi kèm `id` đã có thì `save()` sẽ **ghi đè** danh mục đó (không có kiểm tra).
- Cột Số sản phẩm sinh N+1 câu SQL (1 câu lấy danh mục + 1 câu cho mỗi danh mục). Chấp nhận được với project học tập.
- Thêm trường vào `Category` thì nhớ cập nhật `DataSeeder` và `categories/form.html`.

## Liên quan

- [Quản lý sản phẩm](../product/quan-ly-san-pham.md): phía chủ của quan hệ, dropdown chọn danh mục (bài 11).
- [Trang lỗi 404](../common/trang-loi-404.md): `NotFoundException` dùng trong `findById`.
- [Đăng nhập / đăng ký](../user/dang-nhap-dang-ky.md): mọi URL ở đây đều cần đăng nhập.
- [Dữ liệu mẫu](../seed/du-lieu-mau.md): danh mục mẫu.

## Lịch sử thay đổi

| Bài | Thay đổi |
|---|---|
| [06](../study/06-quan-he-model.md) | Tạo `Category` + Repository/Service/REST Controller; thêm `@ManyToOne` vào `Product`; `@JsonIgnore` ở `products` |
| [08](../study/08-giao-dien-category.md) | Thêm `updateCategory`, `CategoryViewController`, `categories/list.html`, `categories/form.html` |
| [10](../study/10-trang-404.md#bước-3--service-ném-notfoundexception) | `findById` ném `NotFoundException` → 404 thay vì 500 |
| [11](../study/11-chon-danh-muc-cho-san-pham.md#bước-4--nhận-categoryid-khi-lưu) | `ProductViewController` dùng `CategoryService` cho dropdown và gán danh mục khi lưu sản phẩm |
| [13](../study/13-du-lieu-mau-seeder.md) | Seeder tạo 3 danh mục mẫu |
| [17](../study/17-lombok-slf4j-getter-setter.md) | ⬜ Chưa làm. `Category` dùng `@Getter @Setter` (bỏ 6 getter/setter viết tay). `@JsonIgnore` vẫn đặt trên trường `products`. Không dùng `@Data` vì quan hệ 2 chiều với `Product` gây `StackOverflowError` |

Các điểm code khác bài học:
- ⚠️ Khác bài học: bài 06 Bước 5 thêm `ProductRepository.findByCategoryId` và API `GET /api/products/by-category/{categoryId}`, nhưng code **không có** hai thứ này.
- ⚠️ Khác bài học: message 404 trong code là `"không tìm thấy category id " + id`, bài 10 ghi `"Không tìm thấy danh mục id=" + id`.
- ⚠️ Khác bài học: bài 06 đặt tên hàm `create` và field `repository`, code dùng `creatCategory` và `categoryRepository` (bài 08 đã theo tên trong code).
