# 07 — Tách giao diện: một Layout tổng, trang con chỉ chứa lõi

`list.html` và `form.html` đang **chép lại** phần `<head>`, `<style>`, khung trang... Sửa header một chỗ là phải sửa mọi file. Mục tiêu bài này: **viết khung trang MỘT LẦN**, mỗi trang con chỉ còn **phần ruột của nó** (list = cái bảng, form = cái form).

Ta dùng kiểu **Layout / Decorator** (giống "trang cha - trang con"): gần nhất với cách chia component ở các framework khác.

## Ý tưởng

```
        layout.html  (khung tổng, viết 1 lần)
   ┌───────────────────────────────────┐
   │ <head> + CSS chung                │
   │ <header> nav chung                │
   │ ┌───────── ô trống "content" ───┐ │   ◄── mỗi trang con điền phần lõi vào đây
   │ │                               │ │
   │ └───────────────────────────────┘ │
   │ <footer> chung                    │
   └───────────────────────────────────┘
            ▲                    ▲
            │ điền cái bảng      │ điền cái form
      list.html (chỉ <table>)   form.html (chỉ <form>)
```

- `layout.html` giữ toàn bộ `<html><head><body>`, header, footer + một "ô trống".
- `list.html` / `form.html` **không** còn khung trang — chỉ khai báo "hãy dùng layout" và cung cấp phần lõi.

> Kỹ thuật này cần thư viện **`thymeleaf-layout-dialect`** (Thymeleaf thuần chỉ nhúng được mảnh, chưa "bọc cả trang" gọn như vậy).

---

## Bước 1 — Thêm thư viện layout-dialect

Mở `build.gradle`, thêm vào khối `dependencies`:

```gradle
implementation 'nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect:3.4.0'
```

> Không cần khai báo thêm bean/cấu hình gì — Spring Boot tự nhận diện thư viện này và bật `layout:` lên. Vì sửa `build.gradle` nên phải build lại (`docker compose up --build`, hoặc dev mode sẽ tự tải).

---

## Bước 2 — Tách CSS ra file tĩnh (viết một lần)

CSS đang nằm trong `<style>` của từng trang. Gom hết vào **một** file.

Tạo `src/main/resources/static/css/app.css`:

```css
body { font-family: sans-serif; max-width: 700px; margin: 40px auto; }
table { border-collapse: collapse; width: 100%; }
th, td { border: 1px solid #ccc; padding: 8px 12px; text-align: left; }
th { background: #f4f4f4; }
a.btn, button { display: inline-block; padding: 6px 12px; background: #2563eb;
                color: white; text-decoration: none; border: none;
                border-radius: 4px; cursor: pointer; }
label { display: block; margin: 12px 0 4px; }
input { width: 100%; padding: 8px; box-sizing: border-box; }
nav a { margin-right: 12px; }
footer { margin-top: 40px; color: #888; font-size: 14px; }
```

> Mọi thứ trong `src/main/resources/static/` được phục vụ trực tiếp. File trên truy cập tại `/css/app.css`.

---

## Bước 3 — Tạo Layout tổng

Tạo `src/main/resources/templates/layout.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout">
<head>
    <meta charset="UTF-8">
    <!-- $CONTENT_TITLE = <title> mà trang con khai báo -->
    <title layout:title-pattern="$CONTENT_TITLE">Demo</title>
    <link rel="stylesheet" th:href="@{/css/app.css}">
</head>
<body>
    <header>
        <nav>
            <a th:href="@{/products}">Sản phẩm</a>
            <a th:href="@{/categories}">Danh mục</a>
        </nav>
        <hr>
    </header>

    <!-- 👇 Ô TRỐNG: phần lõi của mỗi trang con sẽ được chèn vào đây -->
    <main layout:fragment="content">
        <p>Nội dung trang sẽ được chèn vào đây.</p>
    </main>

    <footer>
        <hr>
        <p>© 2026 Demo Spring Boot — học vừa làm</p>
    </footer>
</body>
</html>
```

- `layout:fragment="content"` = đánh dấu "ô trống". Trang con điền vào đúng chỗ này.
- `layout:title-pattern="$CONTENT_TITLE"` = lấy `<title>` của trang con làm tiêu đề (có thể đổi thành `$CONTENT_TITLE - Demo Shop` nếu muốn thêm hậu tố).

---

## Bước 4 — `list.html` giờ chỉ còn cái bảng

Xoá sạch nội dung cũ của `templates/products/list.html`, thay bằng:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout}">
<head>
    <title>Danh sách sản phẩm</title>
</head>
<body>
    <!-- Chỉ phần bên trong layout:fragment="content" được dùng, phần bọc ngoài bị bỏ -->
    <section layout:fragment="content">
        <h1>Sản phẩm</h1>
        <p><a class="btn" th:href="@{/products/new}">+ Thêm sản phẩm</a></p>

        <table>
            <thead>
                <tr><th>ID</th><th>Tên</th><th>Giá</th><th>Hành động</th></tr>
            </thead>
            <tbody>
                <tr th:each="p : ${products}">
                    <td th:text="${p.id}">1</td>
                    <td th:text="${p.name}">Tên</td>
                    <td th:text="${p.price}">0</td>
                    <td>
                        <a th:href="@{/products/{id}/edit(id=${p.id})}">Sửa</a> |
                        <a th:href="@{/products/{id}/delete(id=${p.id})}"
                           onclick="return confirm('Xoá sản phẩm này?')">Xoá</a>
                    </td>
                </tr>
                <tr th:if="${products.isEmpty()}">
                    <td colspan="4">Chưa có sản phẩm nào.</td>
                </tr>
            </tbody>
        </table>
    </section>
</body>
</html>
```

Chú ý dòng quan trọng nhất:
- `layout:decorate="~{layout}"` (trên thẻ `<html>`) = "trang này dùng khung `templates/layout.html`".
- `<title>` ở đây được **gộp** vào layout (nhờ `title-pattern`).
- Chỉ vùng `layout:fragment="content"` được lấy — `<html><head><body>` bọc ngoài chỉ để file còn hợp lệ, khi render sẽ bị thay bằng khung của layout.

---

## Bước 5 — `form.html` cũng chỉ còn cái form

Thay `templates/products/form.html` bằng:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout}">
<head>
    <title>Sản phẩm</title>
</head>
<body>
    <section layout:fragment="content">
        <h1 th:text="${product.id == 0} ? 'Thêm sản phẩm' : 'Sửa sản phẩm'">Sản phẩm</h1>

        <form th:action="@{/products/save}" th:object="${product}" method="post">
            <input type="hidden" th:field="*{id}">
            <label>Tên sản phẩm</label>
            <input type="text" th:field="*{name}" required>
            <label>Giá</label>
            <input type="number" step="1000" th:field="*{price}">
            <button type="submit">Lưu</button>
            <a th:href="@{/products}">Huỷ</a>
        </form>
    </section>
</body>
</html>
```

Giờ **không file nào lặp lại** `<head>`, CSS, header, footer nữa. Muốn đổi menu/màu/chân trang → sửa `layout.html` hoặc `app.css` **một chỗ**, mọi trang đổi theo.

---

## Bước 6 — Chạy & xem

```bash
docker compose up --build        # vì đã sửa build.gradle
# hoặc dev hot-reload: docker compose -f docker-compose.dev.yml up
```

Mở http://localhost:8093/products — giao diện có đủ header + bảng + footer, dù `list.html` giờ chỉ còn cái bảng. Xem mã nguồn trang (View Source) sẽ thấy HTML đầy đủ được layout ghép lại.

Kiểm tra nhanh khung đã áp cho mọi trang:
```bash
curl -s http://localhost:8093/products    | grep -c "<footer>"   # phải = 1
curl -s http://localhost:8093/products/new | grep -c "<nav>"      # phải = 1
```

---

## Cú pháp cần nhớ

| Cú pháp | Đặt ở | Ý nghĩa |
|---------|-------|---------|
| `xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"` | thẻ `<html>` | Khai báo dùng layout-dialect (bắt buộc ở cả layout & trang con) |
| `layout:fragment="content"` | trong `layout.html` | Đánh dấu "ô trống" để trang con điền vào |
| `layout:decorate="~{layout}"` | thẻ `<html>` của trang con | "Dùng khung `templates/layout.html`" |
| `layout:fragment="content"` | trong trang con | Phần lõi để nhét vào ô trống cùng tên |
| `layout:title-pattern="$CONTENT_TITLE"` | `<title>` của layout | Ghép tiêu đề trang con vào |

---

## Nó khác component của React/Vue thế nào?

Đây là **server-side**: Thymeleaf ghép `layout.html` + `list.html` thành **một trang HTML hoàn chỉnh trên server** rồi mới gửi cho trình duyệt. Nó giúp **không lặp code** (DRY), nhưng "mảnh" ở đây không có trạng thái riêng và không tự cập nhật — muốn đổi phần nào thì tải lại trang.

Còn component React/Vue là **object sống trong trình duyệt** (có state, tự re-render một vùng mà không tải lại trang). Muốn kiểu đó thì dùng React/Vue gọi vào REST API `/api/products` đã có.

---

## Lỗi thường gặp

| Triệu chứng | Nguyên nhân | Cách sửa |
|-------------|-------------|----------|
| Trang con hiện ra **không có** header/footer | Thiếu `layout:decorate` hoặc sai tên layout | Thêm `layout:decorate="~{layout}"` trên `<html>`; `~{layout}` = `templates/layout.html` |
| `layout:...` bị bỏ qua, in nguyên văn | Thiếu khai báo `xmlns:layout=...` trên `<html>` | Thêm namespace layout ở cả 2 file |
| Ô trống không được điền | Tên `layout:fragment` ở layout và trang con **không khớp** | Cả hai phải cùng là `content` (hoặc cùng tên bạn chọn) |
| Sửa `.html`/`.css` không thấy đổi (dev) | Resource không được copy lại | Dùng `gradle classes --continuous` (xem [04-hot-reload.md](04-hot-reload.md)) |
| CSS mất | Sai đường dẫn file tĩnh | `app.css` phải trong `static/css/`, dùng `th:href="@{/css/app.css}"` |

## Bài tập

1. **Trang danh mục**: làm `templates/categories/list.html` cũng `layout:decorate` — thấy ngay nó tự có header/footer mà không cần chép.
2. **Nhiều ô trống**: thêm `layout:fragment="pageActions"` ở header của layout, cho mỗi trang bơm nút riêng vào đó.
3. **CSS/JS riêng cho một trang**: dùng `layout:fragment` trong `<head>` để một trang nạp thêm file css riêng ngoài `app.css`.
4. **Đổi title có hậu tố**: sửa `title-pattern` thành `$CONTENT_TITLE - Demo Shop` và xem tiêu đề tab đổi.

➡️ Quay lại tổng quan: [../README.md](../README.md)
