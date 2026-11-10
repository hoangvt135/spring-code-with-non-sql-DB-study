# Docker & Docker Compose

> **Dùng trong project từ:** đầu tiên ([README](../../README.md), [bài 01](../study/01-tong-quan.md)); lệnh ở [bài 03](../study/03-cheatsheet.md), chế độ dev ở [bài 04](../study/04-hot-reload.md), thêm MongoDB/Redis ở [bài 14](../study/14-moi-truong-mongodb-redis.md), thư mục upload ở [bài 15 Bước 11](../study/15-anh-va-mo-ta-san-pham.md#bước-11--docker-và-git-không-làm-mất-không-commit-nhầm-ảnh) · **Phiên bản:** Docker Compose v2 (lệnh `docker compose`, file không có khoá `version:`); image `gradle:8.11-jdk21`, `eclipse-temurin:21-jre`, `mysql:8.4`, `mongo:8.0`, `redis:7.4` · **Tài liệu chính thức:** [docs.docker.com](https://docs.docker.com/) · [Compose file reference](https://docs.docker.com/reference/compose-file/) · [Dockerfile reference](https://docs.docker.com/reference/dockerfile/)
>
> 🔼 **Sau [bài 16.5](../study/16.5-nang-cap-spring-boot-4.md)** (người học chưa làm): Image build/dev đổi sang **`gradle:9.8.2-jdk21`** (cả `docker-compose.dev.yml` và `Dockerfile`), phải `up -d` để tạo lại container. Image chạy `eclipse-temurin:21-jre` giữ nguyên (Bước 4).

## Nó là gì, giải quyết vấn đề gì

Không có Docker, muốn chạy project này bạn phải tự cài lên máy: JDK 21, Gradle, MySQL 8.4, MongoDB 8.0, Redis 7.4, đúng phiên bản, cấu hình đúng user và mật khẩu. Máy khác (đồng nghiệp, server) lại phải cài lại từ đầu, và thường lệch phiên bản ở đâu đó.

**Docker đóng gói một chương trình cùng mọi thứ nó cần để chạy (hệ điều hành thu gọn, thư viện, cấu hình) thành một khối chạy giống hệt nhau trên mọi máy.** **Docker Compose** mô tả *nhiều* khối như vậy chạy cùng nhau (app + 3 DB) trong một file YAML, rồi bật/tắt tất cả bằng một lệnh.

Nhờ vậy máy host của bạn **không cần cài Java hay Gradle**. Mọi thứ chạy trong container.

So với cách làm bên PHP:

| Bên PHP (XAMPP, Laragon, cài tay) | Project này |
|---|---|
| Cài Apache + PHP + MySQL lên máy | Mỗi thứ là một container, không cài gì lên máy ngoài Docker |
| Mỗi dự án dùng chung một MySQL | Mỗi dự án có MySQL riêng, phiên bản riêng, cổng riêng |
| `php.ini`, `my.cnf` sửa tay trên máy | Cấu hình nằm trong file compose, commit cùng code |
| Laravel Sail | Gần giống: Sail cũng là Docker Compose |

## Khái niệm cốt lõi

### Image và container

| | **Image** | **Container** |
|---|---|---|
| Là gì | Bản mẫu chỉ đọc: hệ điều hành thu gọn + chương trình + thư viện | Một lần **chạy** của image, có trạng thái riêng |
| Giống trong Java | Class | Object (`new`) |
| Ví dụ | `mysql:8.4`, `redis:7.4`, `java_code-app` (tự build) | `demo-mysql`, `demo-redis`, `demo-app` |
| Lấy từ đâu | Tải về từ Docker Hub, hoặc tự build từ `Dockerfile` | `docker compose up` tạo từ image |
| Xoá container thì | Image vẫn còn | Mọi thứ ghi **bên trong** container mất theo, trừ những gì nằm trong volume |

Tên image có dạng `tên:tag`. Tag `8.4` của `mysql:8.4` là **tag trôi**: nó luôn trỏ tới bản vá mới nhất của dòng 8.4 (8.4.x). Lần đầu `up` thì Docker tải về, các lần sau dùng bản đã có trên máy cho tới khi bạn `docker compose pull`.

Điểm quan trọng nhất: **container là thứ dùng xong vứt.** `down`, `up --build`, đổi `environment`... đều có thể xoá container cũ và tạo container mới. Dữ liệu nào cần giữ phải nằm ngoài container (volume hoặc bind mount, xem [Volume](#volume-và-bind-mount-dữ-liệu-sống-ngoài-container)).

### Dockerfile: build 2 giai đoạn

`Dockerfile` là "công thức" tạo image cho app. Project dùng **multi-stage build** (build nhiều giai đoạn):

```dockerfile
# ---------- Giai đoạn 1: Build ----------
FROM gradle:8.11-jdk21 AS build          # image có sẵn JDK 21 + Gradle
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY src ./src
RUN gradle bootJar --no-daemon -x test   # → build/libs/demo-0.0.1-SNAPSHOT.jar

# ---------- Giai đoạn 2: Run ----------
FROM eclipse-temurin:21-jre              # chỉ có JRE, không có Gradle, không có source
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar   # chỉ lấy file .jar từ giai đoạn 1
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

| Lệnh | Làm gì |
|---|---|
| `FROM image AS tên` | Bắt đầu một giai đoạn, dựa trên image có sẵn |
| `WORKDIR /app` | Thư mục làm việc (tự tạo nếu chưa có), giống `cd` |
| `COPY nguồn đích` | Chép file từ máy (thư mục build context) vào image |
| `COPY --from=build` | Chép từ **giai đoạn khác**, không phải từ máy |
| `RUN lệnh` | Chạy lệnh **lúc build image**, kết quả được lưu vào image |
| `EXPOSE 8080` | Chỉ để ghi chú "app nghe ở cổng 8080". **Không** tự mở cổng ra máy, việc đó là của `ports` trong compose |
| `ENTRYPOINT [...]` | Lệnh chạy **khi container khởi động** |

**Vì sao 2 giai đoạn?** Để build cần JDK + Gradle + source + thư viện tải về (nặng). Để chạy chỉ cần JRE + một file `.jar`. Image cuối chỉ chứa giai đoạn 2, nên nhẹ hơn nhiều và không chứa source code.

**`.dockerignore`** liệt kê những thứ không gửi vào quá trình build: `.git`, `build`, `.gradle`, `docs`, `*.md`, `.env`... Nhờ vậy build nhanh hơn và không lỡ đóng gói mật khẩu trong `.env` vào image. Bài 15 Bước 11 thêm `uploads/` vào đây (người học chưa thêm, xem [kế hoạch](../ke-hoach.md)).

**Cache theo lớp (layer):** mỗi lệnh `COPY`/`RUN` tạo một lớp. Lớp nào có đầu vào không đổi thì Docker dùng lại kết quả cũ. Trong Dockerfile hiện tại, `COPY src` nằm **trước** lệnh `RUN` duy nhất, nên chỉ cần sửa một file `.java` là lệnh `RUN gradle bootJar` chạy lại từ đầu, kể cả tải lại thư viện. Comment "Copy file cấu hình build trước để tận dụng cache" vì thế chưa có tác dụng thật: muốn có, phải thêm một lệnh `RUN` tải thư viện nằm giữa hai lệnh `COPY`. Project chưa làm việc này, và cũng không cần lắm vì hằng ngày bạn dùng chế độ dev.

### Docker Compose: service, file compose

File compose mô tả các **service**. Mỗi service sinh ra một container:

```yaml
services:
  mysql:                      # ← tên service: dùng trong lệnh compose VÀ làm hostname trong mạng
    image: mysql:8.4          # dùng image có sẵn
    container_name: demo-mysql   # tên container cố định (không có thì compose tự đặt java_code-mysql-1)
    environment: { ... }      # biến môi trường truyền vào container
    ports: ["3313:3306"]
    volumes: [mysql_data:/var/lib/mysql]
    healthcheck: { ... }
  app:
    build: .                  # tự build image từ Dockerfile ở thư mục hiện tại
    depends_on: { mysql: { condition: service_healthy } }
volumes:
  mysql_data:                 # khai báo volume có tên
```

Phân biệt **tên service** và **tên container**, vì lệnh nào dùng tên nào là khác nhau:

| | Tên service | Tên container |
|---|---|---|
| Ví dụ | `mysql`, `app-dev`, `redis` | `demo-mysql`, `demo-app-dev`, `demo-redis` |
| Dùng trong | `docker compose logs -f app-dev`, `docker compose exec redis ...`, hostname trong mạng | `docker logs demo-app-dev`, `docker exec -it demo-redis ...` |

File compose của project **không có dòng `version:`** ở đầu. Đó là cách viết hiện nay (Compose Specification). Các hướng dẫn cũ có `version: "3.8"`, Compose v2 coi dòng đó là thừa.

### Biến môi trường và file `.env`

```yaml
    environment:
      MYSQL_DATABASE: ${DB_NAME:-demo_db}
```

`${DB_NAME:-demo_db}` nghĩa là: lấy biến `DB_NAME` từ shell hoặc từ file **`.env`** cạnh file compose. Không có (hoặc rỗng) thì dùng `demo_db`. Compose tự đọc `.env`, không cần khai báo gì. Mẫu nằm ở `.env.example`.

Biến trong `.env` **chỉ dùng để điền vào file compose**. Nó không tự chui vào container. Muốn app nhìn thấy thì phải liệt kê dưới `environment:` của service đó, như `DB_HOST`, `MONGO_HOST`, `REDIS_HOST` của `app-dev`. Từ đó, Spring đọc chúng trong `application.yml` (`${DB_HOST:localhost}`).

Chú ý hai cú pháp trông giống nhau:

| Nơi | Cú pháp giá trị mặc định | Ví dụ |
|---|---|---|
| File compose | `${TÊN:-mặc định}` (có dấu `-`) | `${DB_NAME:-demo_db}` |
| `application.yml` của Spring | `${TÊN:mặc định}` (không có `-`) | `${DB_HOST:localhost}` |

Viết `${DB_HOST:-localhost}` trong `application.yml` thì Spring hiểu giá trị mặc định là `-localhost`.

### Network: tên service chính là hostname

Compose tự tạo một mạng riêng cho project (`java_code_default`). Trong mạng này, mỗi container gọi nhau bằng **tên service**, Docker tự phân giải ra địa chỉ IP:

```
┌──────────── mạng java_code_default ────────────────┐
│  app-dev ──▶ mysql:3306   mongo:27017   redis:6379  │
└────────────────────────────────────────────────────┘
        ▲ 8093      ▲ 3313      ▲ 27018     ▲ 6380      ← cổng mở ra máy bạn (ports)
```

Vì vậy `DB_HOST: mysql`, `MONGO_HOST: mongo`, `REDIS_HOST: redis`. Bên trong container, **`localhost` là chính container đó**, không phải máy bạn, cũng không phải container khác. Cấu hình rơi về `localhost` là nguyên nhân của rất nhiều lỗi "Connection refused" (bài 14, bảng lỗi).

### Ports: cửa mở ra máy bạn

`ports: "3313:3306"` đọc là **"cổng máy bạn : cổng trong container"**.

| Service | Container | Trong mạng Docker (app dùng) | Từ máy bạn |
|---|---|---|---|
| App | `demo-app` / `demo-app-dev` | `8080` | `http://localhost:8093` |
| MySQL | `demo-mysql` | `mysql:3306` | `localhost:3313` (DBeaver, Workbench) |
| MongoDB | `demo-mongo` | `mongo:27017` | `localhost:27018` (Compass) |
| Redis | `demo-redis` | `redis:6379` | `localhost:6380` (RedisInsight) |

- Cổng bên trái được đổi đi (3313 thay vì 3306...) để không đụng MySQL/Redis cài sẵn trên máy hoặc của dự án khác.
- App chạy **trong** mạng Docker nên dùng cổng **bên phải** (`DB_PORT: 3306`). Chỉ khi chạy app ngay trên máy (`./gradlew bootRun`) mới dùng cổng bên trái: `DB_PORT=3313 MONGO_PORT=27018 REDIS_PORT=6380 ./gradlew bootRun` ([bài 14](../study/14-moi-truong-mongodb-redis.md#vì-sao-app-lại-dùng-cổng-27017-6379-chứ-không-phải-27018-6380)).
- Ghi `"3313:3306"` thì Docker mở cổng trên **mọi địa chỉ mạng** của máy, nên máy khác cùng mạng LAN có thể kết nối được nếu firewall cho phép. Muốn chỉ máy mình dùng được thì ghi `"127.0.0.1:6380:6379"`. Điều này đáng lưu ý với Redis vì nó không có mật khẩu.

### Volume và bind mount: dữ liệu sống ngoài container

Container bị xoá là dữ liệu bên trong mất theo. Có 2 cách để dữ liệu nằm ngoài container:

| | **Named volume** | **Bind mount** |
|---|---|---|
| Cú pháp | `mysql_data:/var/lib/mysql` (bên trái là **tên**) | `./:/app` (bên trái là **đường dẫn** trên máy) |
| Nằm ở đâu | Docker tự quản lý, bạn không cần biết | Thư mục cụ thể trên máy bạn |
| Thấy file trong VSCode | Không | Có |
| `down -v` có xoá không | **Có** | **Không** (Docker không xoá thư mục của bạn) |
| Project dùng cho | Dữ liệu DB, cache Gradle | Code (chế độ dev) |

Các volume của project:

| Volume | Gắn vào | Chứa | Khai báo ở |
|---|---|---|---|
| `mysql_data` | `/var/lib/mysql` của `mysql` | Toàn bộ dữ liệu MySQL | Cả 2 file compose |
| `mongo_data` | `/data/db` của `mongo` | Dữ liệu MongoDB (và user root đã tạo) | Chỉ `docker-compose.dev.yml` |
| `redis_data` | `/data` của `redis` | File `dump.rdb` (xem [Redis — độ bền](redis.md#độ-bền-dữ-liệu-rdb-snapshot-và-volume-redis_data)) | Chỉ `docker-compose.dev.yml` |
| `gradle_cache` | `/home/gradle/.gradle` của `app-dev` | Thư viện Gradle đã tải | Chỉ `docker-compose.dev.yml` |
| *(bind mount)* `./` | `/app` của `app-dev` | Toàn bộ project | Chỉ `docker-compose.dev.yml` |
| *(tmpfs)* | `/app/.gradle` của `app-dev` | Thư mục `.gradle/` riêng của container, nằm trong RAM | Chỉ `docker-compose.dev.yml` |

Tên thật của volume có tiền tố là tên project: `java_code_mysql_data`, `java_code_redis_data`... (`docker volume ls` để xem).

Các DB image chỉ chạy script khởi tạo (tạo database, tạo user từ `MYSQL_USER`, `MONGO_INITDB_ROOT_*`) **khi volume còn trống**. Đổi mật khẩu trong `.env` sau khi volume đã có dữ liệu thì không có tác dụng, phải xoá volume.

Chế độ thường (`docker-compose.yml`) chưa có volume cho `uploads/`, nên ảnh upload nằm trong container và mất khi container bị tạo lại. Cách thêm `uploads_data`: [bài 15 Bước 11](../study/15-anh-va-mo-ta-san-pham.md#chế-độ-thường-docker-composeyml-phải-thêm-volume) (người học chưa làm). Chế độ dev thì `uploads/` nằm luôn trong project trên máy nhờ bind mount.

### Healthcheck và `depends_on`

Container "đã chạy" không có nghĩa là chương trình bên trong "đã sẵn sàng". MySQL cần vài chục giây để khởi tạo lần đầu. Nếu app khởi động trước đó, Hibernate kết nối thất bại và app chết.

**Healthcheck** là lệnh Docker chạy định kỳ bên trong container để hỏi "còn khoẻ không":

```yaml
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${DB_ROOT_PASSWORD:-root_pass}"]
      interval: 5s      # 5 giây hỏi một lần
      timeout: 5s       # quá 5 giây không trả lời coi như lần đó hỏng
      retries: 10       # hỏng 10 lần liên tiếp → unhealthy
```

| Service | Lệnh kiểm tra |
|---|---|
| `mysql` | `mysqladmin ping` |
| `mongo` | `mongosh --quiet --eval "db.adminCommand('ping')"` |
| `redis` | `redis-cli ping` |

Trạng thái hiện trong `docker ps`: `(health: starting)` → `(healthy)` hoặc `(unhealthy)`.

**`depends_on` với `condition: service_healthy`** bắt app chờ tới khi DB `healthy` mới khởi động. `app-dev` chờ cả 3 DB, `app` (chế độ thường) chờ MySQL.

`depends_on` chỉ có tác dụng **lúc khởi động**. Nếu Redis chết khi app đang chạy, Docker không làm gì app cả. Vì vậy code vẫn phải tự xử lý khi DB phụ chết (xem [Redis](redis.md#redis-chết-thì-app-phải-sống-timeout--dataaccessexception)).

### `restart: unless-stopped`

`mysql`, `mongo`, `redis` và `app` (chế độ thường) có `restart: unless-stopped`: container chết thì Docker tự bật lại, khởi động lại máy hoặc Docker Desktop cũng tự bật lại, **trừ khi** bạn đã chủ động `stop` nó. `app-dev` không có dòng này, nên sau khi khởi động lại máy, các DB tự lên còn app dev thì không.

### Hai chế độ chạy

| | **Thường** (`docker-compose.yml`) | **Dev** (`docker-compose.dev.yml`) |
|---|---|---|
| Service | `mysql`, `app` | `mysql`, `mongo`, `redis`, `app-dev` |
| App chạy bằng | Image tự build từ `Dockerfile`, chạy file `.jar` | Image `gradle:8.11-jdk21`, chạy `gradle bootRun` trên code được mount vào |
| Sửa code | Phải `docker compose up --build` | Tự biên dịch và restart trong vài giây (DevTools) |
| Mount code | Không | `./:/app` |
| Dùng khi | Thử giống môi trường thật | Hằng ngày khi đang code |

Cơ chế hot reload (2 tiến trình `gradle classes --continuous` và `gradle bootRun` chạy song song): [bài 04](../study/04-hot-reload.md#cơ-chế-hoạt-động-bên-trong).

**`user: "1000:1000"`** (chỉ có ở `app-dev`): mặc định tiến trình trong container chạy bằng `root`. Vì code được mount từ máy bạn, mọi file app-dev tạo ra (`build/`, `logs/`, `uploads/`) sẽ thuộc về `root` trên máy, và bạn không sửa, xoá được nếu không dùng `sudo`. `1000:1000` là user id và group id của user đầu tiên trên Linux/WSL (gõ `id -u` để kiểm tra). Chạy bằng id này thì file tạo ra thuộc về chính bạn. Image `gradle` cũng có sẵn user `gradle` với id 1000, nên thư mục `/home/gradle/.gradle` dùng được bình thường.

**`tmpfs` cho `/app/.gradle`**: VSCode trên máy cũng chạy Gradle và khoá thư mục `.gradle/` của project. Gradle trong container không xin nhả khoá được, chờ mãi rồi lỗi `Timeout waiting to lock`. Cho container một `.gradle/` riêng nằm trong RAM là tránh được (comment trong `docker-compose.dev.yml` giải thích chi tiết).

### Cùng project `java_code`: dùng chung volume, không chạy 2 chế độ cùng lúc

Compose đặt **tên project** theo tên thư mục chứa file compose. Cả hai file nằm trong `java_code/`, nên đều thuộc **project `java_code`**:

- Dùng chung network `java_code_default` và volume `java_code_mysql_data`. Chuyển qua lại giữa hai chế độ, dữ liệu MySQL vẫn là một.
- Cả hai đặt `container_name: demo-mysql` và cùng mở cổng `8093`, `3313`. Chạy chế độ này khi chế độ kia chưa tắt sẽ đụng nhau: lỗi `port is already allocated` hoặc `container name ... is already in use`. **Phải `down` chế độ này trước khi `up` chế độ kia.**
- `docker compose down -v` (file thường) **cũng xoá** `java_code_mysql_data`, tức là xoá luôn dữ liệu MySQL của chế độ dev ([bài 15.5](../study/15.5-reset-du-lieu-va-chay-lai-seeder.md#so-sánh-down--v-và-script-reset)).

Chạy file thường khi các container dev còn đó, compose có thể cảnh báo `Found orphan containers` (container "mồ côi" thuộc cùng project nhưng không có trong file đang dùng). Đó là dấu hiệu chế độ kia chưa được `down`.

### `down` và `down -v`

| Lệnh | Container | Network | Volume (dữ liệu DB, cache Gradle) | Thư mục trên máy (`uploads/`, code) |
|---|---|---|---|---|
| `stop` | Dừng, **không xoá** | Giữ | Giữ | Giữ |
| `down` | **Xoá** | Xoá | **Giữ** | Giữ |
| `down -v` | Xoá | Xoá | **Xoá các volume khai báo trong file** | Giữ |

`down -v` là cách "làm sạch tất cả", nhưng với chế độ dev nó xoá cả `gradle_cache` (lần sau phải tải lại thư viện vài phút) và để lại ảnh mồ côi trong `uploads/`. Khi chỉ muốn làm mới dữ liệu, dùng `./script/reset-db.sh` ([bài 15.5](../study/15.5-reset-du-lieu-va-chay-lai-seeder.md), [doc module](../seed/du-lieu-mau.md)). Chỉ dùng `down -v` khi đổi user/mật khẩu DB trong `.env`.

## Project này dùng nó thế nào

| File | Vai trò | Bài |
|---|---|---|
| [`Dockerfile`](../../Dockerfile) | Build 2 giai đoạn, chỉ dùng cho chế độ thường | 01, 03 |
| [`.dockerignore`](../../.dockerignore) | Không gửi `.git`, `build`, `docs`, `.env`... vào build | 01, 15 |
| [`docker-compose.yml`](../../docker-compose.yml) | Chế độ thường: `mysql` + `app`. **Chưa có** `mongo`, `redis`, volume `uploads_data` | 01, (14 Bước 2, 15 Bước 11 chưa làm) |
| [`docker-compose.dev.yml`](../../docker-compose.dev.yml) | Chế độ dev: `mysql`, `mongo`, `redis`, `app-dev` | 04, 14 |
| `.env` / [`.env.example`](../../.env.example) | Tên DB, user, mật khẩu cho compose. `.env` không commit | 01, 14 |
| [`script/reset-db.sh`](../../script/reset-db.sh) | Reset dữ liệu cả 3 DB bằng `docker exec`, bật lại app bằng `up -d` | 15.5 |

Đoạn chính của `app-dev`, cho thấy gần như mọi khái niệm ở trên:

```yaml
  app-dev:
    image: gradle:8.11-jdk21              # dùng thẳng image có Gradle, không build Dockerfile
    container_name: demo-app-dev
    user: "1000:1000"                     # file tạo ra thuộc về bạn, không phải root
    working_dir: /app
    depends_on:                           # chờ cả 3 DB healthy
      mysql: { condition: service_healthy }
      mongo: { condition: service_healthy }
      redis: { condition: service_healthy }
    environment:
      DB_HOST: mysql                      # tên service = hostname
      MONGO_HOST: mongo
      MONGO_DB: ${MONGO_DB:-demo_db}      # ⚠️ bài 14 ghi demo_mongo, xem MongoDB
      REDIS_HOST: redis
    ports:
      - "8093:8080"
    volumes:
      - ./:/app                           # bind mount: code trên máy
      - gradle_cache:/home/gradle/.gradle # named volume: thư viện đã tải
    command: >
      sh -c "gradle classes --continuous -x test --console=plain -q &
             exec gradle bootRun --console=plain"
```

`MONGO_DB` mặc định là `demo_db` trong compose, khác `demo_mongo` của bài 14: chi tiết ở [MongoDB](mongodb.md#tên-database-thật-là-demo_db-không-phải-demo_mongo).

Lệnh hằng ngày nằm ở [bài 03](../study/03-cheatsheet.md) và [bài 04](../study/04-hot-reload.md). Lưu ý bài 03 còn ghi cổng MySQL `3307` ở vài chỗ, cổng thật là `3313`.

## Tra cứu nhanh

Viết tắt: `DC` = `docker compose -f docker-compose.dev.yml` (chế độ dev). Chế độ thường thì bỏ `-f ...`.

| Lệnh | Của ai | Làm gì | Ghi chú |
|---|---|---|---|
| `DC up -d` | Compose | Tạo (nếu cần) và chạy mọi service, chạy nền | Container nào có cấu hình đổi thì được **tạo lại**. Không có `-d` thì log hiện ngay terminal, `Ctrl+C` là dừng |
| `docker compose up --build` | Compose | Build lại image app rồi chạy (chế độ thường) | Bắt buộc sau mỗi lần sửa code ở chế độ thường |
| `DC up -d --wait mysql mongo redis` | Compose | Chỉ bật vài service, chờ tới khi healthy | Script reset dùng |
| `DC ps` | Compose | Các container của project, trạng thái, health, cổng | |
| `DC logs -f app-dev` | Compose | Xem log theo thời gian thực | Dùng **tên service**. `--tail 100` để chỉ xem 100 dòng cuối |
| `DC restart app-dev` | Compose | Khởi động lại container | **Không** đọc lại thay đổi trong file compose hay `.env`. Muốn áp dụng thì `up -d` |
| `DC stop app-dev` / `DC start app-dev` | Compose | Dừng / bật lại, không xoá container | |
| `DC exec redis redis-cli` | Compose | Chạy lệnh trong container theo **tên service** | |
| `DC down` | Compose | Dừng và xoá container, network | Giữ volume |
| `DC down -v` | Compose | Như trên, **xoá cả volume** | Mất dữ liệu DB, cache Gradle |
| `DC config` | Compose | In file compose sau khi đã điền biến từ `.env` | Kiểm tra `${...}` ra giá trị gì |
| `DC pull` | Compose | Tải bản mới nhất của các image theo tag | |
| `DC rm -sf mongo` | Compose | Dừng và xoá riêng một container | Dùng trước khi xoá volume của nó |
| `docker ps` | Docker | Mọi container đang chạy trên máy | `--format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"` cho gọn |
| `docker exec -it demo-mysql mysql -u demo_user -pdemo_pass demo_db` | Docker | Vào MySQL shell | Dùng **tên container**. `-it` để gõ tương tác |
| `docker exec -it demo-mongo mongosh -u demo_user -p demo_pass --authenticationDatabase admin demo_db` | Docker | Vào MongoDB shell | Xem [MongoDB](mongodb.md) |
| `docker exec -it demo-redis redis-cli` | Docker | Vào Redis shell | Xem [Redis](redis.md) |
| `docker logs demo-app-dev` | Docker | Log theo tên container | `--since <thời điểm>` như script reset |
| `docker volume ls` / `docker volume rm java_code_mongo_data` | Docker | Liệt kê / xoá một volume | Container đang dùng volume thì không xoá được |
| `docker inspect --format '{{.State.Health.Status}}' demo-mysql` | Docker | Xem trạng thái health | `starting`, `healthy`, `unhealthy` |
| `docker image prune` | Docker | Xoá image "treo" (không còn tên) do build nhiều lần | Hỏi lại trước khi xoá |

## Bẫy thường gặp

| Triệu chứng | Nguyên nhân | Cách sửa |
|---|---|---|
| `Bind for 0.0.0.0:8093 failed: port is already allocated` | Chế độ kia đang chạy, hoặc chương trình khác chiếm cổng | `down` chế độ kia. Nếu là chương trình khác: đổi cổng **bên trái** trong `ports` |
| `container name "/demo-mysql" is already in use` | Chế độ kia vẫn còn container | `down` file compose kia trước |
| Sửa code ở chế độ thường mà không thấy đổi | Chưa build lại image | `docker compose up --build` |
| Sửa `environment` hoặc `.env` rồi `restart` mà không có tác dụng | `restart` không đọc lại cấu hình | `up -d` (compose tự tạo lại container) |
| Sửa `build.gradle` (thêm thư viện) mà chế độ dev báo `NoSuchBeanDefinitionException` / thiếu class | DevTools không tự tải thư viện mới | `DC restart app-dev` hoặc `DC up -d` |
| App báo `Connection refused` tới `localhost:3306` / `27017` / `6379` | Thiếu biến `*_HOST` trong `environment`, app rơi về `localhost` (chính container app) | Thêm `DB_HOST: mysql`, `MONGO_HOST: mongo`, `REDIS_HOST: redis` |
| App trong Docker kết nối tới cổng `3313` / `6380` và thất bại | Dùng nhầm cổng **bên trái** | Trong mạng Docker dùng cổng gốc `3306`, `27017`, `6379` |
| `dependency failed to start: container demo-mysql is unhealthy` | MySQL đang khởi tạo lần đầu (vừa `down -v`), lâu hơn `interval × retries` | Chờ khoảng 30 giây rồi `up -d` lại. Xem `DC logs mysql` |
| `Access denied` / `Authentication failed` sau khi đổi mật khẩu trong `.env` | Volume đã có dữ liệu, script khởi tạo không chạy lại | Xoá volume của DB đó (`down -v`, hoặc `docker volume rm`) |
| `down -v` file thường mà dữ liệu dev cũng mất | Hai file cùng project `java_code`, dùng chung `java_code_mysql_data` | Nhớ là chỉ có **một** dữ liệu MySQL |
| Sau `down -v`, lần `up` dev đầu tiên rất lâu | `gradle_cache` bị xoá, tải lại toàn bộ thư viện | Dùng `script/reset-db.sh` thay cho `down -v` khi chỉ cần làm mới dữ liệu |
| File trong `build/`, `logs/` thuộc `root`, không xoá được | Container chạy bằng root, hoặc id `1000` khác id của bạn | Kiểm tra `id -u`, sửa `user:` cho khớp. Dọn bằng `sudo chown -R $USER build logs` |
| Lỗi `Timeout waiting to lock ... buildOutputCleanup` | Gradle trong container tranh khoá `.gradle/` với Gradle của VSCode | Giữ nguyên khối `tmpfs` cho `/app/.gradle` |
| Ảnh upload mất sau `docker compose up --build` (chế độ thường) | `uploads/` nằm trong container, chưa có volume | Thêm `uploads_data` ([bài 15 Bước 11](../study/15-anh-va-mo-ta-san-pham.md#chế-độ-thường-docker-composeyml-phải-thêm-volume)) |
| Chế độ thường: log `[NoSQL]` báo lỗi, `/actuator/health` trả 503 | `docker-compose.yml` chưa có `mongo`, `redis` | Dùng chế độ dev, hoặc làm bài 14 Bước 2 |
| Sửa file mà hot reload không chạy (hay gặp trên WSL2) | Sự kiện thay đổi file không truyền vào container | [bài 04 — nếu hot reload không tự kích hoạt](../study/04-hot-reload.md#nếu-hot-reload-không-tự-kích-hoạt) |
| Giá trị trong `application.yml` thành `-localhost` | Viết cú pháp compose `${X:-y}` vào file của Spring | Spring dùng `${X:y}` |

## Đọc thêm

- Bài học: [01 — Tổng quan](../study/01-tong-quan.md), [03 — Cheatsheet](../study/03-cheatsheet.md), [04 — Hot reload](../study/04-hot-reload.md), [14 — Cài MongoDB + Redis](../study/14-moi-truong-mongodb-redis.md), [15 — Bước 11: Docker và git](../study/15-anh-va-mo-ta-san-pham.md#bước-11--docker-và-git-không-làm-mất-không-commit-nhầm-ảnh), [15.5 — Reset dữ liệu](../study/15.5-reset-du-lieu-va-chay-lai-seeder.md).
- Doc module: [Kiểm tra kết nối Mongo/Redis](../nosql/kiem-tra-ket-noi-mongo-redis.md) (bảng cổng và host), [Dữ liệu mẫu & reset](../seed/du-lieu-mau.md) (script reset).
- Công nghệ liên quan: [Gradle](gradle.md) (`bootJar`, `bootRun`, `classes --continuous`), [Spring Boot](spring-boot.md) (DevTools, biến môi trường trong `application.yml`, Actuator health), [MySQL](mysql.md), [MongoDB](mongodb.md), [Redis](redis.md).
- Tài liệu chính thức:
  - [Docker overview](https://docs.docker.com/get-started/docker-overview/): image, container, kiến trúc.
  - [Multi-stage builds](https://docs.docker.com/build/building/multi-stage/), [Build cache](https://docs.docker.com/build/cache/): giải thích cache theo lớp.
  - [Compose file reference](https://docs.docker.com/reference/compose-file/): `services`, `volumes`, `healthcheck`, `depends_on`...
  - [Variable interpolation](https://docs.docker.com/compose/how-tos/environment-variables/variable-interpolation/): cú pháp `${VAR:-default}` và file `.env`.
  - [Networking in Compose](https://docs.docker.com/compose/how-tos/networking/): vì sao tên service là hostname.
  - [Volumes](https://docs.docker.com/engine/storage/volumes/), [Bind mounts](https://docs.docker.com/engine/storage/bind-mounts/).
  - [Compose CLI reference](https://docs.docker.com/reference/cli/docker/compose/): mọi lệnh `docker compose ...`.
