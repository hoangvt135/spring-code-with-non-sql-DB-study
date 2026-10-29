# 03 — Cheatsheet: các lệnh hay dùng

## Docker Compose (chạy cả app + DB)

```bash
docker compose up --build      # build lại & chạy (dùng khi vừa sửa code)
docker compose up              # chạy (không build lại)
docker compose up -d           # chạy nền (detached)
docker compose logs -f app     # xem log app theo thời gian thực
docker compose logs -f mysql   # xem log MySQL
docker compose ps              # xem container nào đang chạy
docker compose restart app     # khởi động lại riêng app
docker compose down            # dừng & xoá container (giữ dữ liệu DB)
docker compose down -v         # dừng & xoá luôn dữ liệu DB (làm lại từ đầu)
```

> Sau khi **sửa code Java**, phải `docker compose up --build` để build lại image.

## Làm việc với MySQL

```bash
# Vào MySQL shell trong container
docker exec -it demo-mysql mysql -u demo_user -pdemo_pass demo_db

# Chạy nhanh một câu lệnh SQL
docker exec -it demo-mysql mysql -u demo_user -pdemo_pass demo_db -e "SHOW TABLES;"
docker exec -it demo-mysql mysql -u demo_user -pdemo_pass demo_db -e "SELECT * FROM product;"
```

Kết nối từ công cụ ngoài (DBeaver, Workbench, IntelliJ):
- Host: `localhost` — Port: `3313` — DB: `demo_db`
- User: `demo_user` — Pass: `demo_pass`

## Gradle (chạy app trực tiếp trên máy, KHÔNG qua Docker)

> Cần một MySQL đang chạy. Cách nhanh: chỉ bật DB bằng docker `docker compose up mysql -d`,
> rồi chạy app bằng Gradle (app sẽ nối tới `localhost:3307` — nhớ set biến môi trường DB_PORT=3307).

```bash
./gradlew bootRun       # chạy app
./gradlew build         # build ra file .jar trong build/libs/
./gradlew test          # chạy test
./gradlew clean         # xoá thư mục build
```

Trên Windows dùng `gradlew.bat` thay cho `./gradlew`.

## Kiểm tra app

```bash
curl http://localhost:8093/                 # trang chủ
curl http://localhost:8093/actuator/health  # sức khỏe (mong đợi: {"status":"UP"})
```

## Chế độ dev hot reload

```bash
docker compose -f docker-compose.dev.yml up -d           # bật dev (hot reload)
docker compose -f docker-compose.dev.yml logs -f app-dev # xem log & app tự restart
docker compose -f docker-compose.dev.yml down            # dừng
```

> Prod (`docker-compose.yml`) và dev (`docker-compose.dev.yml`) đừng chạy cùng lúc — chúng dùng chung cổng 8093/3313.

## Xử lý sự cố thường gặp

| Triệu chứng | Nguyên nhân & cách xử lý |
|-------------|--------------------------|
| `Port 8080 already in use` | Có app khác chiếm cổng. Đổi `ports` trong `docker-compose.yml` thành `"8081:8080"`. |
| `Port 3307 already in use` | Đổi `"3307:3306"` thành cổng khác, ví dụ `"3308:3306"`. |
| App báo lỗi kết nối DB lúc khởi động | Đợi vài giây — `depends_on healthcheck` sẽ chờ DB sẵn sàng. Nếu vẫn lỗi, xem `docker compose logs mysql`. |
| Sửa code mà không thấy đổi | Quên build lại: `docker compose up --build`. |
| Muốn làm sạch dữ liệu DB | `docker compose down -v` rồi `docker compose up --build`. |
