# Java: ngoại lệ (exception)

Bắt lỗi rộng hay hẹp, checked exception.

Mỗi mục là một câu hỏi phát sinh trong lúc học, ghi rõ hỏi ở bài nào. Mục lục chung: [common_knowledge.md](../common_knowledge.md).

## `catch (IOException e)` và `catch (Exception e)` khác nhau thế nào?

> Hỏi ở: [Bài 15 — Bước 4: `FileStorageService`: lưu và xoá file](../study/15-anh-va-mo-ta-san-pham.md#bước-4--filestorageservice-lưu-và-xoá-file)

### Khác nhau ở chỗ "bắt rộng hay hẹp"

`catch (X e)` bắt lỗi kiểu `X` **và mọi lớp con của `X`**. Cây kế thừa của lỗi trong Java:

```
Throwable
├── Error                          ← lỗi JVM (hết RAM...), không nên bắt
└── Exception
    ├── IOException                ← lỗi đọc/ghi file, mạng   (checked)
    │   ├── FileNotFoundException
    │   └── NoSuchFileException ...
    └── RuntimeException           ← lỗi do code sai          (unchecked)
        ├── IllegalArgumentException
        ├── NullPointerException
        └── ...
```

| | `catch (IOException e)` | `catch (Exception e)` |
|---|---|---|
| Bắt được | Chỉ lỗi vào/ra (I/O): ổ đầy, không có quyền ghi, file bị khoá... | **Gần như mọi thứ**: I/O, `NullPointerException`, `IllegalArgumentException`, lỗi do chính code mình viết sai |
| Ý nghĩa khi đọc code | "Ở đây tôi biết có thể hỏng ổ đĩa, và tôi xử lý đúng việc đó" | "Có gì hỏng thì cứ gom hết vào đây" |
| Lỗi không lường trước | Không bị bắt → bay tiếp lên trên, dễ phát hiện | Bị **nuốt mất** hoặc bị gói thành thông báo sai |

### Checked exception: vì sao bắt buộc phải có `try/catch`?

`IOException` là **checked exception**. Hàm nào có thể ném nó thì phải khai báo `throws IOException` (ví dụ `Files.createDirectories`, `MultipartFile.transferTo`, `Files.deleteIfExists`). Khi gọi những hàm đó, **trình biên dịch bắt buộc** bạn phải xử lý: hoặc `try/catch`, hoặc tự khai báo `throws` tiếp ra ngoài. Đó là lý do trong `FileStorageService` có `try/catch` dù code trông rất đơn giản.

`RuntimeException` và các lớp con (như `IllegalArgumentException`) là **unchecked**: không bắt buộc bắt, trình biên dịch không nhắc gì.

Một khác biệt nhỏ khi biên dịch: nếu trong `try` **không có** dòng nào ném `IOException` mà vẫn viết `catch (IOException e)`, Java báo lỗi `exception IOException is never thrown in body of corresponding try statement`. Còn `catch (Exception e)` thì lúc nào viết cũng được, vì nó bao cả lỗi unchecked.

### Ví dụ cụ thể trong `FileStorageService`: bắt rộng làm hỏng gì?

Trong `save()`, hàm `resolve(filename)` nằm **bên trong** khối `try` và có thể ném `IllegalArgumentException` (khi tên file cố thoát ra ngoài thư mục upload):

```java
try {
    file.transferTo(resolve(filename));   // resolve() có thể ném IllegalArgumentException
} catch (Exception e) {                    // ← bắt luôn cả IllegalArgumentException
    throw new RuntimeException("Lưu file thất bại: " + filename, e);
}
```

- Với `catch (Exception e)`: lỗi "tên file không hợp lệ" bị đổi thành "Lưu file thất bại", tức là thông báo sai bản chất. Controller muốn `catch (IllegalArgumentException e)` để hiện chữ đỏ trên form ([Bài 15 — Bài tập 2](../study/15-anh-va-mo-ta-san-pham.md#bài-tập)) sẽ **không bắt được nữa**, vì lỗi đã bị gói thành `RuntimeException`.
- Với `catch (IOException e)`: chỉ lỗi ghi đĩa mới bị gói lại, còn `IllegalArgumentException` vẫn giữ nguyên và bay lên controller.

Trong `delete()` còn tệ hơn, nếu viết:

```java
} catch (Exception e) {
    // không làm gì
}
```

thì **mọi lỗi đều biến mất không dấu vết**, kể cả lỗi chặn đường dẫn nguy hiểm `../../app.jar`. Cách của bài học là `catch (IOException e)` kèm `log.warn(...)`: chỉ bỏ qua lỗi xoá file (việc phụ, không nên làm hỏng việc chính), mà vẫn có log để biết.

### Quy tắc dễ nhớ

1. **Bắt hẹp nhất có thể**: bắt đúng loại lỗi mà bạn biết cách xử lý ở chỗ đó.
2. **Không bao giờ để `catch` rỗng.** Ít nhất phải ghi log.
3. `catch (Exception e)` chỉ hợp lý ở **tầng ngoài cùng**, nơi gom mọi lỗi lại để trả một trang lỗi chung, ghi log rồi trả về "500". Không nên dùng nó ở giữa các tầng logic.
4. Nếu cần bắt nhiều loại, viết riêng từng `catch`, hoặc gộp bằng dấu `|`: `catch (IOException | IllegalStateException e)`.
