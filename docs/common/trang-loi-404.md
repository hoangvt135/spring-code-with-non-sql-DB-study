# Trang lỗi 404 (NotFoundException)

> **Module:** `common` (`NotFoundException`) + `templates/error/` · **Bài học nguồn:** [bài 10](../study/10-trang-404.md), [bài 16.5](../study/16.5-nang-cap-spring-boot-4.md) · **Cập nhật:** 2026-11-10
> **Trạng thái code:** ✅ Hoàn thành — theo code thật ngày 2026-11-10

## Chức năng này làm gì

Cho **một** trang 404 dùng chung, có layout như mọi trang khác, thay cho trang Whitelabel. Hai trường hợp đều dẫn về đó:

- URL không khớp route nào (ví dụ `/orders`) → Spring tự trả 404.
- Route có, nhưng id không tồn tại (ví dụ `/categories/999/edit`) → Service ném **`NotFoundException`**. Class này gắn `@ResponseStatus(HttpStatus.NOT_FOUND)` nên Spring trả **404** thay vì 500.

Không có controller nào cho trang lỗi: `BasicErrorController` có sẵn của Spring Boot tự tìm `templates/error/404.html`.

## Đường dẫn & giao diện

| Trường hợp | Trình duyệt (`Accept: text/html`) | `curl` / REST API |
|---|---|---|
| Id không tồn tại, đã đăng nhập | `templates/error/404.html`, có nav | JSON `{"status":404,"error":"Not Found","message":"...","path":"..."}` |
| URL không có route, đã đăng nhập | `templates/error/404.html` | JSON 404 |
| **Chưa đăng nhập** | Bị chuyển về `/login` trước (`anyRequest().authenticated()`), không thấy 404 | 401 |

Trang 404 dùng các biến Spring đưa sẵn: `${path}` (URL vừa mở), `${message}` (message của exception, chỉ hiện khi có). Có link "Về trang sản phẩm" và "Danh mục".

## Luồng xử lý

```
GET /categories/999/edit
  └─ CategoryViewController.editForm(999)
       └─ categoryService.findById(999)
            └─ repository.findById(999) rỗng → orElseThrow → throw NotFoundException("không tìm thấy category id 999")
  exception bay ra khỏi controller, không ai bắt
  └─ @ResponseStatus(NOT_FOUND) → mã 404 → forward /error (permitAll)
       └─ BasicErrorController tìm template:
            error/404.html ✅ (có) → error/4xx.html → error.html → Whitelabel

GET /orders  → không có route → 404 → /error → error/404.html

Nhánh khác (KHÔNG phải 404):
  /categories/abc/edit  → "abc" không đổi được sang Long → 400 → không có error/4xx.html → Whitelabel
  RuntimeException / IllegalArgumentException khác (vd upload sai đuôi, giá âm) → 500 → Whitelabel
```

## File liên quan

| File | Vai trò |
|---|---|
| [`common/NotFoundException.java`](../../src/main/java/com/example/demo/common/NotFoundException.java) | `extends RuntimeException` + `@ResponseStatus(HttpStatus.NOT_FOUND)`, constructor nhận `message` |
| [`templates/error/404.html`](../../src/main/resources/templates/error/404.html) | Trang 404, `layout:decorate="~{fragments/layout}"` |
| `category/CategoryService.java` | `findById()` ném `NotFoundException("không tìm thấy category id " + id)` |
| `product/ProductService.java` | `findById()` ném `NotFoundException("Không tìm thấy sản phẩm id=" + id)` |
| [`application.yml`](../../src/main/resources/application.yml) | `server.error.include-message: always` |
| `config/SecurityConfig.java` | `/error` nằm trong danh sách `permitAll()` |

Những chỗ ra 404 nhờ gọi `findById()` của Service:

| Service | Hàm gọi `findById()` | URL ví dụ |
|---|---|---|
| `CategoryService` | `findById`, `updateCategory` | `/categories/999/edit`, `/api/categories/999`, `/products/save` với `categoryId` không tồn tại |
| `ProductService` | `findById`, `update`, `delete` (→ `save`) | `/products/999`, `/products/999/edit`, `/products/999/delete`, `/api/products/999` |

> ⚠️ Khác bài học: message trong `CategoryService` là `"không tìm thấy category id 999"` (bài học Bước 3 ghi `"Không tìm thấy danh mục id=999"`). Message của `ProductService` thì giống bài học.

## Dữ liệu

Không có.

## Cấu hình

```yaml
server:
  error:
    include-message: always   # cho ${message} hiện ở trang lỗi và trong JSON lỗi
```

Ở chế độ dev, DevTools tự bật cái này; ghi rõ trong `application.yml` để chế độ thường (`docker compose up --build`) cũng hiện message ([bài 10 Bước 4](../study/10-trang-404.md#bước-4--cho-phép-hiện-message-lỗi)).

## Quy tắc & lưu ý khi sửa

- "Không tìm thấy" thì ném **`NotFoundException`**, không ném `RuntimeException` trơn (sẽ ra 500). Ném ở **Service** (`findById()`), các hàm khác gọi lại `findById()` là tự có 404.
- Message viết tiếng Việt, **không** chứa thông tin nhạy cảm, vì nó hiện thẳng ra trang và JSON.
- Template phải đúng chỗ, đúng tên: `templates/error/404.html` (thư mục `error`, không phải `errors`). Muốn thêm trang cho nhóm lỗi khác thì đặt `error/4xx.html`, `error/500.html` cạnh đó (trang 500 **không** hiện `${message}`).
- Trang lỗi cũng dùng layout chung: giữ `xmlns:layout` và `layout:decorate`.
- Không dùng `@ControllerAdvice` / global exception handler trừ khi được yêu cầu ([lý do](../study/10-trang-404.md#vì-sao-không-dùng-controlleradvice)).
- Package model mới: `findById()` cũng phải ném `NotFoundException` (import từ `com.example.demo.common`).

> ⚠️ Khác bài học (bài tập chưa làm): `CategoryService.delete()` vẫn gọi thẳng `deleteById(id)`, nên `/categories/999/delete` **không** ra 404 mà lặng lẽ quay về danh sách ([bài tập 3](../study/10-trang-404.md#bài-tập)). Chưa có `error/4xx.html` và `error/500.html` (bài tập 1, 2).

## Liên quan

- [Quản lý sản phẩm](../product/quan-ly-san-pham.md), [Quản lý danh mục](../category/quan-ly-danh-muc.md): nơi ném `NotFoundException`.
- [Lượt xem và top sản phẩm](../product/luot-xem-va-top-san-pham.md): trang chi tiết gọi `findById()` **trước** khi đếm lượt xem, nên id sai ra 404 mà không ghi bậy vào Redis.
- [Đăng nhập & đăng ký](../user/dang-nhap-dang-ky.md): `SecurityConfig`, vì sao chưa đăng nhập thì không thấy 404.

## Lịch sử thay đổi

| Bài | Thay đổi |
|---|---|
| [10](../study/10-trang-404.md) | Tạo `templates/error/404.html`, `common/NotFoundException`; `findById()` của 2 Service ném `NotFoundException`; bật `include-message: always` |
| [12](../study/12-dang-nhap-spring-security.md) | `/error` được `permitAll()` |
| [16](../study/16-luot-xem-top-san-pham-redis.md#bước-5--productviewcontroller-trang-chi-tiết) | Thêm `/products/{id}` (trang chi tiết), cũng ra 404 qua `findById()` |
| [16.5](../study/16.5-nang-cap-spring-boot-4.md) | ⬜ Chưa làm. Spring Boot 4: `server.error.include-message` → `spring.web.error.include-message` (tên cũ bị bỏ qua → trang lỗi không hiện câu thông báo) |
