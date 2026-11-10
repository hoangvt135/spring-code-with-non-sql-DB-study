# 01 — Tổng quan kiến trúc

Trước khi code, hãy hiểu một request đi qua đâu. Đây là kiến trúc **phân tầng** (layered) chuẩn của Spring Boot.

## Luồng một request

```
   Client (trình duyệt / Postman)
        │  HTTP request  ví dụ: GET /api/products
        ▼
┌─────────────────┐
│   Controller    │  Nhận request, trả JSON. Không chứa logic nghiệp vụ.
│  @RestController │
└────────┬────────┘
         │ gọi
         ▼
┌─────────────────┐
│    Service      │  Chứa logic nghiệp vụ (kiểm tra, tính toán, quy tắc).
│    @Service     │
└────────┬────────┘
         │ gọi
         ▼
┌─────────────────┐
│   Repository    │  Nói chuyện với DB (lưu, tìm, xoá). Spring tự sinh code.
│  JpaRepository  │
└────────┬────────┘
         │ SQL
         ▼
┌─────────────────┐
│   Entity + DB   │  Entity = 1 class Java ↔ 1 bảng trong MySQL.
│    @Entity      │
└─────────────────┘
```

## Vai trò từng tầng

| Tầng | Annotation | Nhiệm vụ | Ví dụ |
|------|-----------|----------|-------|
| **Entity** | `@Entity` | Ánh xạ 1 class ↔ 1 bảng DB | `Product` ↔ bảng `product` |
| **Repository** | `extends JpaRepository` | Thao tác DB (CRUD sẵn có) | `save()`, `findAll()`, `deleteById()` |
| **Service** | `@Service` | Xử lý logic nghiệp vụ | "không cho giá âm" |
| **Controller** | `@RestController` | Nhận HTTP, trả JSON | `GET /api/products` |
| **DTO** *(nâng cao)* | — | Định dạng dữ liệu vào/ra | tách khỏi entity |

## Vì sao chia tầng?

- **Dễ đọc**: mỗi file làm một việc.
- **Dễ test**: test service mà không cần chạy web.
- **Dễ thay đổi**: đổi DB chỉ ảnh hưởng tầng repository.

## CRUD là gì?

CRUD = 4 thao tác cơ bản với dữ liệu, tương ứng 4 method HTTP:

| Thao tác | HTTP | Đường dẫn ví dụ | Ý nghĩa |
|----------|------|-----------------|---------|
| **C**reate | `POST` | `/api/products` | Tạo mới |
| **R**ead | `GET` | `/api/products` hoặc `/api/products/1` | Xem danh sách / xem 1 |
| **U**pdate | `PUT` | `/api/products/1` | Sửa |
| **D**elete | `DELETE` | `/api/products/1` | Xoá |

➡️ Tiếp theo: [02-thuc-hanh-crud.md](02-thuc-hanh-crud.md) — tự tay xây đủ 4 thao tác này cho `Product`.
