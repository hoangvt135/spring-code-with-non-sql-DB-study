# Demo — Spring Boot + MySQL + Docker

Project mẫu để **vừa học vừa thực hành** Spring Boot. App và database đều chạy bằng Docker.

## Công nghệ

- Java 21 + Spring Boot 3.4
- Gradle (đã có sẵn `./gradlew`, không cần cài Gradle)
- MySQL 8.4
- Docker + Docker Compose

## Cấu trúc thư mục

```
java_code/
├── build.gradle              # Khai báo thư viện & cấu hình build
├── settings.gradle
├── Dockerfile                # Cách đóng gói app thành image (build 2 giai đoạn)
├── docker-compose.yml        # Chạy app + MySQL cùng lúc
├── .env.example              # Mẫu biến môi trường (copy thành .env)
├── docs/                     # 👉 Hướng dẫn học CRUD từng bước
└── src/main/
    ├── java/com/example/demo/
    │   ├── DemoApplication.java          # Điểm khởi động
    │   └── controller/HelloController.java
    └── resources/application.yml         # Cấu hình app & kết nối DB
```

## Chạy nhanh (toàn bộ bằng Docker)

```bash
# (tuỳ chọn) tạo file .env từ mẫu
cp .env.example .env

# Build và chạy app + MySQL
docker compose up --build
```

Sau khi khởi động xong, mở thử (app map ra cổng **8093**):

- http://localhost:8093/ → thông báo app đang chạy
- http://localhost:8093/hello → lời chào
- http://localhost:8093/actuator/health → trạng thái sức khỏe (status: UP)

Dừng lại: `Ctrl + C`, hoặc xoá hẳn (kèm dữ liệu DB):

```bash
docker compose down        # dừng
docker compose down -v     # dừng + xoá dữ liệu MySQL
```

## Kết nối vào MySQL để xem dữ liệu

```bash
docker exec -it demo-mysql mysql -u demo_user -pdemo_pass demo_db
```

Hoặc dùng DBeaver/MySQL Workbench: host `localhost`, port `3313`, user `demo_user`, pass `demo_pass`, database `demo_db`.

## Chế độ dev (hot reload) — sửa code không cần build lại image

```bash
docker compose -f docker-compose.dev.yml up -d          # bật
docker compose -f docker-compose.dev.yml logs -f app-dev # xem app tự restart khi lưu file
docker compose -f docker-compose.dev.yml down           # dừng
```

Chi tiết trong [docs/04-hot-reload.md](docs/04-hot-reload.md).

## Bắt đầu học

Mở thư mục [docs/](docs/) và làm theo thứ tự:

1. [docs/01-tong-quan.md](docs/01-tong-quan.md) — Kiến trúc & luồng một request đi qua các tầng
2. [docs/02-thuc-hanh-crud.md](docs/02-thuc-hanh-crud.md) — Tự tay xây API CRUD cho `Product`
3. [docs/03-cheatsheet.md](docs/03-cheatsheet.md) — Các lệnh Docker/Gradle hay dùng
4. [docs/04-hot-reload.md](docs/04-hot-reload.md) — Vì sao Java cần build & cách bật hot reload
5. [docs/05-giao-dien-thymeleaf.md](docs/05-giao-dien-thymeleaf.md) — Làm giao diện web với Thymeleaf (thuần Java)
6. [docs/06-quan-he-model.md](docs/06-quan-he-model.md) — Quan hệ 1:N giữa các model (Category ↔ Product)
7. [docs/07-tach-giao-dien-fragment.md](docs/07-tach-giao-dien-fragment.md) — Tách giao diện: Layout tổng + trang con chỉ chứa lõi (layout-dialect)
8. [docs/08-giao-dien-category.md](docs/08-giao-dien-category.md) — Làm giao diện web (list/thêm/sửa/xoá) cho Category
