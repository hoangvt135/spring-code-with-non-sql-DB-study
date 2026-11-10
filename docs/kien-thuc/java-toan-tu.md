# Java: toán tử

Toán tử logic `&&`, `||` và cơ chế đoản mạch.

Mỗi mục là một câu hỏi phát sinh trong lúc học, ghi rõ hỏi ở bài nào. Mục lục chung: [common_knowledge.md](../common_knowledge.md).

## `return a() && b() && c();` hoạt động thế nào? (toán tử `&&` và cơ chế "đoản mạch")

> Hỏi ở: câu hỏi ngoài giáo trình (đoạn code mẫu trên mạng). Liên hệ với [Bài 02 — Bước 3: Service](../study/02-thuc-hanh-crud.md#bước-3--service-chứa-logic-nghiệp-vụ), nơi có `product.getPrice() != null && product.getPrice() < 0`.

Đoạn code mẫu:

```java
return service1.function1(data)
    && service2.function2(data)
    && service3.function3(data);
```

(Trên mạng hay viết `$var`, đó là kiểu PHP. Trong Java tên biến không có `$`.)

### 1. Đây không phải cú pháp `return` đặc biệt nào cả

`return` chỉ trả về **giá trị của một biểu thức**. Biểu thức ở đây là `a && b && c`, và ngắt dòng chỉ để dễ đọc. Viết dài ra sẽ là:

```java
boolean ketQua = service1.function1(data) && service2.function2(data) && service3.function3(data);
return ketQua;
```

Do đó: cả 3 hàm **phải trả về `boolean`**, và hàm chứa dòng `return` này cũng phải có kiểu trả về `boolean`.

### 2. `&&` tính từ trái sang phải và **dừng ngay khi gặp `false`** (short-circuit, "đoản mạch")

`A && B` chỉ `true` khi cả hai đều `true`. Nên nếu `A` đã `false` thì kết quả chắc chắn là `false`, Java **không gọi `B` nữa**.

| `function1` | `function2` | `function3` | Kết quả | Những hàm **thực sự được gọi** |
|---|---|---|---|---|
| `false` | (bỏ qua) | (bỏ qua) | `false` | chỉ 1 |
| `true` | `false` | (bỏ qua) | `false` | 1, 2 |
| `true` | `true` | `false` | `false` | 1, 2, 3 |
| `true` | `true` | `true` | `true` | 1, 2, 3 |

Tương đương với:

```java
if (!service1.function1(data)) return false;
if (!service2.function2(data)) return false;
return service3.function3(data);
```

Kiểu viết này hợp với **chuỗi kiểm tra**: "qua bước 1 mới kiểm tra bước 2...". Ví dụ `return isLoggedIn(user) && hasPermission(user) && isNotLocked(user);`

### 3. Ví dụ ngay trong project: chặn NullPointerException

```java
if (product.getPrice() != null && product.getPrice() < 0) { ... }
```

Nếu giá là `null`, vế trái `false` nên vế phải `product.getPrice() < 0` **không bao giờ chạy**. Nếu không có đoản mạch, so sánh `null < 0` sẽ ném `NullPointerException`. Vì vậy **thứ tự các vế quan trọng**: vế "bảo vệ" phải đứng trước.

### 4. `||` cũng đoản mạch, nhưng ngược lại

`A || B` **dừng ngay khi gặp `true`**, vì chỉ cần một vế đúng là đủ. Ví dụ `return isAdmin(user) || isOwner(user, product);`: là admin thì không cần kiểm tra chủ sở hữu.

Còn `&` và `|` (một ký tự) khi dùng với `boolean` thì **không đoản mạch**: luôn tính hết tất cả các vế. Hiếm khi cần dùng.

### 5. Những bẫy cần nhớ

- **Hàm có tác dụng phụ có thể bị bỏ qua.** Nếu `function2` có ghi DB, gửi mail hay xoá file, mà `function1` trả `false`, thì `function2` không chạy. Chỉ nên xâu chuỗi những hàm **chỉ kiểm tra** (trả lời đúng/sai, không thay đổi gì).
- **Không biết bước nào hỏng.** Kết quả chỉ là `false`, không có lý do. Nếu cần báo lỗi cụ thể cho người dùng, viết từng `if` riêng và ném exception kèm thông báo, như `ProductService` đang làm (`throw new IllegalArgumentException("Giá không được âm")`).
- **Thứ tự cũng ảnh hưởng tốc độ.** Nên đặt kiểm tra rẻ (so sánh biến) trước, kiểm tra đắt (gọi DB, gọi API) sau, để hay được dừng sớm.
