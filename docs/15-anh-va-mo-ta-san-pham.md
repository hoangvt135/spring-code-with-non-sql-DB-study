# 15 — Ảnh và mô tả cho sản phẩm (upload file)

Tới giờ sản phẩm chỉ có tên, giá, danh mục. Bài này thêm:

- **Mô tả** (`description`): một đoạn văn bản dài, nhập bằng `<textarea>`.
- **Ảnh sản phẩm**: chọn file trong form Thêm/Sửa, upload lên server.
- **Trang danh sách làm lại**: có cột ảnh thumbnail, mô tả rút gọn, giá định dạng `1.500.000 ₫`.
- **Ảnh mặc định** cho sản phẩm chưa có ảnh. Đường dẫn ảnh này khai báo trong `application.yml`, **tương đối trong source** nên deploy đi đâu cũng chạy.
- **Thư mục lưu ảnh upload cấu hình được** (biến môi trường `UPLOAD_DIR`).

Mục tiêu khi xong bài:

- Sửa một sản phẩm, chọn ảnh, bấm Lưu: trang danh sách hiện ảnh nhỏ của sản phẩm đó.
- File ảnh nằm trong thư mục `uploads/`, còn bảng `product` trong MySQL chỉ lưu **tên file**.
- Đổi ảnh: file cũ bị xoá. Xoá sản phẩm: file ảnh cũng bị xoá.
- Sản phẩm chưa có ảnh hiện ảnh mặc định "Chưa có ảnh".

> Kiến thức mới: form `multipart/form-data`, `MultipartFile`, tự đặt cấu hình riêng (`app.*`) và đọc bằng `@Value`, `WebMvcConfigurer` để phục vụ file nằm **ngoài** source, gọi bean Spring ngay trong Thymeleaf (`${@productService...}`), Docker volume cho dữ liệu upload.

---

## Tư vấn trước khi code: lưu ảnh ở đâu? File vật lý hay BLOB?

### Bạn đang hiểu đúng một nửa

**BLOB** (*Binary Large Object*) là cách gọi chung cho **"một cục byte lớn lưu thẳng trong database"**: ảnh, PDF, file zip... Nó **không phải tính năng riêng của MongoDB**:

| DB | Lưu file dạng BLOB bằng gì | Giới hạn |
|---|---|---|
| MySQL | Kiểu cột `BLOB`, `MEDIUMBLOB`, `LONGBLOB` | 64KB / 16MB / 4GB mỗi ô |
| MongoDB | Trường kiểu `BinData` trong document | Cả document tối đa **16MB** |
| MongoDB (file lớn) | **GridFS**: cắt file thành mảnh 255KB, lưu vào 2 collection `fs.files` + `fs.chunks` | Gần như không giới hạn |

Chỗ dễ hiểu sai: **"document" trong MongoDB nghĩa là *một bản ghi dạng JSON***, giống một dòng trong bảng MySQL, **không phải "tài liệu"** kiểu file Word/PDF. MongoDB được gọi là *document database* vì lý do đó, chứ không phải vì nó chuyên lưu tài liệu. Lưu ảnh vào MongoDB thì vẫn là BLOB, về bản chất không khác MySQL lưu BLOB.

### So sánh 4 cách hay gặp

| | ① **File vật lý** trên ổ đĩa, DB chỉ lưu tên file ⬅️ bài này | ② BLOB trong MySQL | ③ MongoDB GridFS | ④ Object storage (Amazon S3, MinIO...) |
|---|---|---|---|---|
| Hiển thị ảnh | Spring trả thẳng file, nhanh, trình duyệt cache được | Phải viết controller đọc byte từ DB rồi trả về. Mỗi lần xem ảnh là một truy vấn DB nặng | Như ② | Trình duyệt tải thẳng từ S3/CDN, app không phải làm gì |
| Kích thước DB, backup | DB nhẹ. Nhưng phải nhớ backup **thêm** thư mục ảnh | DB phình rất nhanh (1.000 ảnh × 2MB = 2GB), dump/restore chậm | DB Mongo phình | Tách hẳn khỏi DB, S3 tự lo độ bền |
| Nhất quán DB ↔ file | Có thể lệch: file "mồ côi", hoặc DB trỏ tới file đã mất | **Tốt nhất**: ảnh và sản phẩm nằm chung một transaction | **Không** chung transaction với MySQL (sản phẩm ở MySQL, ảnh ở Mongo) | Có thể lệch như ① |
| Chạy nhiều server app | Mỗi server một ổ đĩa riêng → cần ổ dùng chung (NFS, volume chung) | Tự nhiên dùng chung | Tự nhiên dùng chung | Tự nhiên dùng chung |
| Độ khó với project này | **Thấp** | Trung bình | Trung bình–cao: 1 sản phẩm nằm ở 2 DB | Phải thêm container MinIO + thư viện SDK |

### Khuyên dùng cho project này: ① file vật lý

- **Đơn giản nhất**, và gần với thực tế nhất: dự án thật hầu như luôn làm theo kiểu "file để một nơi, DB chỉ giữ **tên/đường dẫn**". Production thường dùng ④, mà ④ thực chất cũng là ① nhưng file được đặt trên một "ổ đĩa" của dịch vụ khác.
- Ảnh sản phẩm là thứ **được xem rất nhiều**. Để Spring trả thẳng file thì tốt hơn nhiều so với lần nào cũng kéo vài MB từ DB lên.
- ③ GridFS đặc biệt **không hợp** ở đây: sản phẩm nằm ở MySQL, ảnh lại ở MongoDB. Xoá sản phẩm và xoá ảnh không cùng một transaction, nên mất luôn ưu điểm "nhất quán" là lý do chính để chọn BLOB.

Khi nào BLOB (② / ③) mới đáng chọn? Khi file **nhỏ và ít** (chữ ký, avatar vài KB), **bắt buộc** phải đi chung transaction với bản ghi, hoặc hệ thống không có chỗ lưu file dùng chung mà cũng không muốn dựng S3.

> **Thiết kế để sau này đổi được.** Ta gom toàn bộ việc "lưu file ở đâu" vào **một class duy nhất**: `FileStorageService`. DB chỉ giữ **tên file**. Sau này muốn chuyển sang S3/MinIO hay GridFS thì chỉ phải viết lại class đó, còn `Product`, controller và template giữ nguyên (xem bài tập 6).

---

## Ý tưởng

### Luồng upload và hiển thị

```
Trình duyệt                          Spring Boot                                    Lưu ở đâu
───────────                          ───────────                                    ─────────
POST /products/save          ──▶  ProductViewController.save()
 (multipart: name, price,          └─ ProductService.save(product, imageFile)
  description, imageFile)              ├─ FileStorageService.save(imageFile)  ──▶  uploads/3f2a….jpg            (ổ đĩa)
                                       └─ productRepository.save(product)     ──▶  product.image_path="3f2a….jpg" (MySQL)

GET /products                ──▶  list.html in ra <img src="/uploads/3f2a….jpg">
GET /uploads/3f2a….jpg       ──▶  WebConfig: đọc thẳng file trong uploads/  (KHÔNG qua controller nào)

Sản phẩm chưa có ảnh         ──▶  <img src="/images/product-default.svg">
                                  file nằm trong src/main/resources/static/ → đóng gói luôn trong .jar
```

### Hai loại đường dẫn, hai cách xử lý khác nhau

| | Ảnh mặc định | Ảnh người dùng upload |
|---|---|---|
| Ai tạo ra | Lập trình viên, nằm cùng source code | Người dùng, lúc app **đang chạy** |
| Nằm ở | `src/main/resources/static/images/` → đóng gói trong `.jar` | Thư mục `uploads/`, **ngoài** source |
| Cấu hình | `app.product.default-image: images/product-default.svg` (tương đối so với `static/`) | `app.upload.dir: ${UPLOAD_DIR:uploads}` |
| Deploy sang máy khác | Đi theo file `.jar`, **không phải làm gì** | Thư mục tự được tạo. Muốn đổi chỗ thì đặt biến `UPLOAD_DIR`. Chạy Docker thì cần **volume** để không mất |

### ⚠️ Vì sao KHÔNG lưu ảnh upload vào `src/main/resources/static/`?

Thoạt nhìn đây là chỗ tiện nhất, vì `/css/app.css` cũng đang nằm ở đó. Nhưng làm vậy sẽ sai:

1. **Khi chạy bằng `.jar`** (`docker compose up --build`), thư mục `static/` nằm **bên trong file `.jar`**, là một file nén chỉ đọc. App không ghi thêm file vào đó được.
2. **Build lại là mất**: mỗi lần build, `static/` được copy lại từ source. Ảnh người dùng upload không nằm trong source nên sẽ biến mất.
3. **Chế độ dev**: `gradle classes --continuous` thấy `src/main/resources` có file mới sẽ copy lại, DevTools thấy vậy liền **restart app**. Mỗi lần upload một ảnh là app khởi động lại một lần.

Quy tắc cần nhớ: **`static/` dành cho file của lập trình viên. File của người dùng phải để ở một thư mục riêng, ngoài source.**

### Các file sẽ đụng tới

```
src/main/resources/application.yml                       ⬅️ SỬA — giới hạn upload + khối app:
src/main/resources/static/images/product-default.svg     ⬅️ MỚI — ảnh mặc định
src/main/resources/static/css/app.css                    ⬅️ SỬA — style ảnh nhỏ
src/main/resources/templates/products/form.html          ⬅️ SỬA — ô mô tả, ô chọn ảnh
src/main/resources/templates/products/list.html          ⬅️ SỬA — làm lại bảng
com/example/demo/
├── storage/
│   └── FileStorageService.java     ⬅️ MỚI — lưu / xoá file trên ổ đĩa
├── config/
│   ├── WebConfig.java              ⬅️ MỚI — map URL /uploads/** → thư mục uploads
│   └── SecurityConfig.java         ⬅️ SỬA — cho phép xem ảnh
└── product/
    ├── Product.java                ⬅️ SỬA — thêm description, imagePath
    ├── ProductService.java         ⬅️ SỬA — save kèm ảnh, xoá ảnh, tính URL ảnh
    └── ProductViewController.java  ⬅️ SỬA — nhận file từ form
docker-compose.yml, .gitignore, .dockerignore            ⬅️ SỬA — volume, bỏ qua uploads/
```

---

## Bước 1 — Cấu hình trong `application.yml`

Có 2 chỗ cần sửa.

**(a) Giới hạn kích thước upload.** Thêm `servlet:` vào **bên trong khối `spring:` đang có**, cùng cấp với `datasource:`, `jpa:`, `data:`:

```yaml
spring:
  # ... (application, datasource, jpa, data giữ nguyên)

  # Giới hạn upload. Mặc định Spring chỉ cho 1MB/file — ảnh chụp điện thoại thường lớn hơn
  servlet:
    multipart:
      max-file-size: 5MB       # 1 file tối đa
      max-request-size: 6MB    # cả form (file + các ô chữ) tối đa
```

> Đừng viết thêm một khối `spring:` thứ hai ở cuối file. YAML không cho trùng khoá, app sẽ không khởi động được và báo `DuplicateKeyException ... found duplicate key spring`.

**(b) Cấu hình riêng của app.** Thêm vào **cuối file**, ở cấp ngoài cùng (cùng cấp với `spring:`, `server:`, `management:`):

```yaml
# Cấu hình riêng của app (không phải của Spring) → gom dưới tiền tố "app" cho dễ phân biệt
app:
  upload:
    # Thư mục lưu file người dùng upload.
    # Đường dẫn TƯƠNG ĐỐI → tính từ thư mục đang chạy app (Docker: /app → /app/uploads).
    # Muốn để chỗ khác: đặt biến môi trường UPLOAD_DIR, ví dụ /var/lib/demo/uploads
    dir: ${UPLOAD_DIR:uploads}
  product:
    # Ảnh khi sản phẩm chưa có ảnh. Tương đối so với src/main/resources/static/
    # → nằm trong source, đóng gói luôn vào .jar, deploy đi đâu cũng có
    default-image: images/product-default.svg
```

Cú pháp `${UPLOAD_DIR:uploads}` giống hệt `${DB_HOST:localhost}`: lấy giá trị của biến môi trường `UPLOAD_DIR`, nếu không có biến đó thì dùng `uploads`.

> VSCode có thể gạch vàng các dòng `app.*` kèm cảnh báo "Unknown property". Không sao cả, đó là cấu hình **do mình tự đặt** nên Spring không biết trước. Ở Bước 4 và Bước 7, code sẽ đọc chúng bằng `@Value`.

---

## Bước 2 — Ảnh mặc định

Tạo thư mục `src/main/resources/static/images/`, rồi tạo file `product-default.svg` trong đó:

```xml
<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200" viewBox="0 0 200 200">
  <rect width="200" height="200" fill="#e5e7eb"/>
  <rect x="50" y="55" width="100" height="75" rx="6" fill="none" stroke="#9ca3af" stroke-width="6"/>
  <circle cx="75" cy="80" r="9" fill="#9ca3af"/>
  <path d="M56 124 L90 95 L110 112 L125 100 L144 124 Z" fill="#9ca3af"/>
  <text x="100" y="165" font-family="sans-serif" font-size="16" fill="#6b7280" text-anchor="middle">Chưa có ảnh</text>
</svg>
```

SVG là ảnh vẽ bằng chữ (XML), nên chỉ cần dán nội dung trên vào file là xong, không cần công cụ vẽ ảnh. Muốn dùng ảnh `.png` của riêng bạn thì chép file vào đây rồi sửa `default-image` cho khớp tên.

Mọi thứ trong `static/` được phục vụ ở URL tương ứng: `static/images/product-default.svg` → `http://localhost:8093/images/product-default.svg`. Cơ chế này giống hệt `static/css/app.css` → `/css/app.css`.

---

## Bước 3 — Thêm 2 trường vào `Product`

Mở `product/Product.java`, thêm 2 trường ngay dưới `price`:

```java
    private Double price;

    // Mô tả dài → cho phép tới 2000 ký tự (mặc định String chỉ là VARCHAR(255))
    @Column(length = 2000)
    private String description;

    // CHỈ lưu TÊN FILE ảnh, ví dụ "3f2a9c1e-....jpg"
    // Không lưu nội dung ảnh (BLOB), cũng không lưu cả đường dẫn thư mục
    private String imagePath;
```

Rồi thêm getter/setter ở cuối class:

```java
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getImagePath() {
        return imagePath;
    }
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
```

Vì đang dùng `ddl-auto: update`, khi app khởi động lại Hibernate sẽ tự thêm 2 cột `description` và `image_path` vào bảng `product`. Các sản phẩm cũ có 2 cột này bằng `NULL`, tức là "chưa có ảnh".

> **Vì sao chỉ lưu tên file, không lưu `/app/uploads/3f2a….jpg`?** Nếu lưu đường dẫn đầy đủ, mai kia bạn đổi `UPLOAD_DIR` hoặc chuyển server thì mọi dòng trong DB đều sai. Chỉ lưu tên file thì việc ghép thành đường dẫn thật thuộc về `FileStorageService`, đổi một chỗ là xong.

---

## Bước 4 — `FileStorageService`: lưu và xoá file

Tạo package `storage/` cạnh `product/`, rồi tạo `storage/FileStorageService.java`:

```java
package com.example.demo.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

// Class DUY NHẤT biết file upload nằm ở đâu.
// Sau này muốn chuyển sang S3/MinIO/GridFS → chỉ viết lại class này.
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    // URL công khai của file upload: /uploads/<tên file>  (dùng ở WebConfig, SecurityConfig, ProductService)
    public static final String URL_PREFIX = "/uploads/";

    // Chỉ nhận ảnh. Cố ý KHÔNG có "svg": file SVG chứa được JavaScript → người dùng có thể cài mã độc
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    // Thư mục lưu file, dạng tuyệt đối, ví dụ /app/uploads
    private final Path root;

    // @Value đọc giá trị app.upload.dir trong application.yml
    public FileStorageService(@Value("${app.upload.dir}") String uploadDir) {
        // "uploads" (tương đối) → ghép với thư mục đang chạy app → /app/uploads
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);   // chưa có thì tạo, có rồi thì thôi
        } catch (IOException e) {
            throw new RuntimeException("Không tạo được thư mục upload: " + root, e);
        }
        log.info("[Upload] Thư mục lưu file: {}", root);
    }

    public Path getRoot() {
        return root;
    }

    // Lưu file, trả về TÊN FILE MỚI (để ghi vào DB). Người dùng không chọn file → trả null.
    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());   // "Ảnh Đẹp.JPG" → "JPG"
        if (ext == null || !ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
            throw new IllegalArgumentException("Chỉ nhận ảnh jpg, jpeg, png, gif, webp. File gửi lên: " + file.getOriginalFilename());
        }

        // KHÔNG dùng tên gốc của người dùng. Đặt tên ngẫu nhiên để:
        //  - 2 người cùng upload "anh.jpg" không ghi đè lên nhau
        //  - tên không có dấu tiếng Việt, khoảng trắng
        //  - không ai lợi dụng được tên kiểu "../../etc/passwd"
        String filename = UUID.randomUUID() + "." + ext.toLowerCase();
        try {
            file.transferTo(resolve(filename));
        } catch (IOException e) {
            throw new RuntimeException("Lưu file thất bại: " + filename, e);
        }
        return filename;
    }

    // Xoá file. Không có file / file đã mất → bỏ qua, không làm hỏng việc chính
    public void delete(String filename) {
        if (filename == null || filename.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(filename));
        } catch (IOException e) {
            log.warn("[Upload] Không xoá được file {}: {}", filename, e.getMessage());
        }
    }

    // Tên file → đường dẫn thật, và CHẶN mọi đường dẫn thoát ra ngoài thư mục upload
    private Path resolve(String filename) {
        Path target = root.resolve(filename).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Tên file không hợp lệ: " + filename);
        }
        return target;
    }
}
```

Mấy điểm cần để ý:

- **`MultipartFile`** là cách Spring gói "một file được upload": tên gốc (`getOriginalFilename()`), kích thước (`getSize()`), có rỗng không (`isEmpty()`), và lưu ra ổ đĩa (`transferTo(...)`).
- **Tại sao kiểm tra cả `null` lẫn `isEmpty()`?** Khi form có ô chọn file mà người dùng không chọn gì, tuỳ trường hợp Spring sẽ đưa vào `null` hoặc một file rỗng. Kiểm tra cả hai thì chắc chắn đúng.
- **`resolve()` để làm gì?** `delete()` nhận tên file lấy từ DB. Nếu vì lý do nào đó trong DB có giá trị `../../app.jar`, `normalize()` sẽ tính ra đường dẫn thật và `startsWith(root)` sẽ chặn lại. Nhờ vậy không bao giờ xoá nhầm file nằm ngoài thư mục upload.

---

## Bước 5 — `WebConfig`: cho trình duyệt xem được file trong `uploads/`

Spring chỉ tự phục vụ file trong `static/` (thuộc classpath). Thư mục `uploads/` nằm **ngoài** source nên phải khai báo thêm. Tạo `config/WebConfig.java`:

```java
package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.demo.storage.FileStorageService;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final FileStorageService fileStorageService;

    public WebConfig(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    // URL /uploads/abc.jpg  →  file <thư mục upload>/abc.jpg trên ổ đĩa
    // Giống cách Spring tự làm cho static/: không cần viết controller nào
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(FileStorageService.URL_PREFIX + "**")
                // "file:" = đọc từ ổ đĩa (khác "classpath:" = đọc trong source/.jar)
                // BẮT BUỘC có "/" ở cuối, thiếu là 404 hết
                .addResourceLocations("file:" + fileStorageService.getRoot() + "/");
    }
}
```

`WebMvcConfigurer` là "cửa" để tuỳ chỉnh Spring MVC. Ở đây ta chỉ dùng một phương thức: thêm một cặp "URL ↔ thư mục".

---

## Bước 6 — `SecurityConfig`: cho phép xem ảnh

Ảnh sản phẩm là nội dung công khai, giống `/css/**`. Nếu không mở quyền, khi chưa đăng nhập thì request ảnh sẽ bị chuyển về trang `/login` và ảnh hiện thành ảnh vỡ. Sửa dòng `requestMatchers` trong `config/SecurityConfig.java`:

```java
                .requestMatchers("/login", "/register", "/css/**", "/images/**",
                                 FileStorageService.URL_PREFIX + "**",      // "/uploads/**"
                                 "/error", "/actuator/health").permitAll()
```

và thêm import `import com.example.demo.storage.FileStorageService;`.

> Dùng hằng `URL_PREFIX` thay vì gõ tay `"/uploads/**"` để chỉ có **một** chỗ quyết định tiền tố URL. Sau này đổi tiền tố ở `FileStorageService` thì `WebConfig` và `SecurityConfig` tự đổi theo.

---

## Bước 7 — `ProductService`: lưu kèm ảnh, xoá ảnh, tính URL ảnh

Thay **toàn bộ** `product/ProductService.java`:

```java
package com.example.demo.product;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.common.NotFoundException;
import com.example.demo.storage.FileStorageService;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;
    private final String defaultImageUrl;   // "/images/product-default.svg"

    public ProductService(ProductRepository productRepository,
                          FileStorageService fileStorageService,
                          @Value("${app.product.default-image}") String defaultImage) {
        this.productRepository = productRepository;
        this.fileStorageService = fileStorageService;
        this.defaultImageUrl = "/" + defaultImage;

        // Cấu hình sai tên ảnh mặc định → báo ngay lúc khởi động, khỏi phải đi tìm vì sao ảnh vỡ
        if (!new ClassPathResource("static/" + defaultImage).exists()) {
            log.warn("[Product] Không thấy ảnh mặc định src/main/resources/static/{} — kiểm tra app.product.default-image", defaultImage);
        }
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm id=" + id));
    }

    public Product create(Product product) {
        // Ví dụ logic nghiệp vụ: chặn giá âm
        if (product.getPrice() != null && product.getPrice() < 0) {
            throw new IllegalArgumentException("Giá không được âm");
        }
        // Ảnh chỉ được gán qua save(..., imageFile) bên dưới, không nhận tên file do client tự gửi lên
        product.setImagePath(null);
        return productRepository.save(product);
    }

    public Product update(Long id, Product data) {
        Product product = findById(id);      // đảm bảo tồn tại
        product.setName(data.getName());
        product.setPrice(data.getPrice());
        product.setDescription(data.getDescription());
        product.setCategory(data.getCategory());
        // CỐ Ý không copy imagePath: form không gửi trường này lên → data.getImagePath() luôn null
        // → copy vào là xoá mất ảnh đang có mỗi lần bấm Lưu
        return productRepository.save(product);     // save = update khi id đã có
    }

    // Dùng cho form web: lưu thông tin + (nếu có chọn) ảnh mới
    public Product save(Product data, MultipartFile imageFile) {
        // 1) Lưu file TRƯỚC: file sai định dạng → ném lỗi ngay, DB chưa bị đổi gì
        String newImage = fileStorageService.save(imageFile);   // null nếu không chọn file

        // 2) Thêm hay sửa: id == 0 là sản phẩm mới (id kiểu long nguyên thuỷ, mặc định 0)
        Product product = (data.getId() == 0) ? create(data) : update(data.getId(), data);

        // 3) Có ảnh mới → gắn vào sản phẩm, rồi xoá ảnh cũ cho đỡ rác ổ đĩa
        if (newImage != null) {
            String oldImage = product.getImagePath();
            product.setImagePath(newImage);
            productRepository.save(product);
            fileStorageService.delete(oldImage);
        }
        return product;
    }

    public void delete(Long id) {
        Product product = findById(id);                       // không có → 404 như các chỗ khác
        productRepository.delete(product);
        fileStorageService.delete(product.getImagePath());    // xoá luôn file ảnh
    }

    // URL ảnh để đặt vào <img src="...">. Chưa có ảnh → ảnh mặc định
    public String imageUrl(Product product) {
        if (product.getImagePath() == null) {
            return defaultImageUrl;
        }
        return FileStorageService.URL_PREFIX + product.getImagePath();   // "/uploads/3f2a….jpg"
    }
}
```

Những gì thay đổi so với trước:

| Phương thức | Thay đổi |
|---|---|
| Constructor | Nhận thêm `FileStorageService` và ảnh mặc định (`@Value`). Kiểm tra ảnh mặc định có tồn tại không |
| `create()` | Bỏ qua `imagePath` mà client gửi lên |
| `update()` | Copy thêm `description`. **Không** copy `imagePath` (đọc comment trong code) |
| `save(data, imageFile)` | **Mới**. Phần "thêm hay sửa" (`id == 0`) **chuyển từ controller vào đây**, vì giờ còn phải xử lý ảnh |
| `delete()` | Tìm sản phẩm trước để biết tên file, xoá xong thì xoá file. Id không tồn tại → 404 (trước đây im lặng) |
| `imageUrl()` | **Mới**. Template gọi hàm này để lấy `src` của ảnh |

`ClassPathResource("static/...")` tìm file trong classpath: lúc dev là `build/resources/main/static/...`, lúc chạy `.jar` là bên trong file `.jar`. Vì vậy cách kiểm tra này chạy đúng ở mọi môi trường.

---

## Bước 8 — `ProductViewController`: nhận file từ form

Sửa phương thức `save()` trong `product/ProductViewController.java`:

```java
    @PostMapping("/save")
    public String save(@ModelAttribute Product product,
                       @RequestParam(required = false) Long categoryId,
                       // name="imageFile" của <input type="file"> trong form.html
                       @RequestParam(required = false) MultipartFile imageFile) {
        if (categoryId != null) {
            product.setCategory(categoryService.findById(categoryId));
        }
        // Thêm hay sửa + xử lý ảnh: giao hết cho Service (Bước 7)
        productService.save(product, imageFile);
        return "redirect:/products";
    }
```

Thêm import `import org.springframework.web.multipart.MultipartFile;`.

Đoạn `if (product.getId() == 0) { create } else { update }` cũ đã **chuyển vào `ProductService.save()`**. Controller giờ chỉ việc nhận dữ liệu và chuyển tiếp cho Service, đúng vai trò của nó.

> Ô chọn file đặt tên `imageFile`, **khác** tên trường `imagePath` của `Product`, là cố ý. `@ModelAttribute Product` tự gán mọi ô có tên trùng với trường của `Product`. Nếu đặt tên trùng, Spring sẽ cố nhét một file vào trường kiểu `String` rồi báo lỗi chuyển kiểu.

---

## Bước 9 — `form.html`: ô mô tả và ô chọn ảnh

Thay `templates/products/form.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{fragments/layout}">
<head>
    <title>Sản phẩm</title>
</head>
<body>
    <section layout:fragment="content">
        <h1 th:text="${product.id == 0} ? 'Thêm sản phẩm' : 'Sửa sản phẩm'">Sản phẩm</h1>

        <!-- enctype="multipart/form-data": BẮT BUỘC khi form có <input type="file">.
             Thiếu nó, trình duyệt chỉ gửi TÊN file chứ không gửi nội dung → imageFile luôn rỗng -->
        <form th:action="@{/products/save}" th:object="${product}" method="post"
              enctype="multipart/form-data">
            <input type="hidden" th:field="*{id}">

            <label>Tên sản phẩm</label>
            <input class="form_controll" type="text" th:field="*{name}" required>

            <label>Giá</label>
            <input class="form_controll" type="number" step="1000" th:field="*{price}">

            <label>Danh mục</label>
            <select class="form_controll" name="categoryId">
                <option value="">-- Chưa chọn --</option>
                <option th:each="c : ${categories}"
                        th:value="${c.id}"
                        th:text="${c.name}"
                        th:selected="${product.category?.id == c.id}">Tên danh mục</option>
            </select>

            <label>Mô tả</label>
            <textarea class="form_controll" th:field="*{description}" rows="4" maxlength="2000"></textarea>

            <label>Ảnh sản phẩm</label>
            <!-- Ảnh hiện tại (sản phẩm mới / chưa có ảnh → ảnh mặc định) -->
            <img class="preview" th:src="@{${@productService.imageUrl(product)}}" alt="Ảnh hiện tại">
            <input class="form_controll" type="file" name="imageFile" accept=".jpg,.jpeg,.png,.gif,.webp">
            <small class="muted">Bỏ trống = giữ ảnh hiện tại. Nhận jpg, png, gif, webp, tối đa 5MB.</small>

            <p>
                <button type="submit">Lưu</button>
                <a th:href="@{/products}">Huỷ</a>
            </p>
        </form>
    </section>
</body>
</html>
```

### Cú pháp mới: `${@productService.imageUrl(product)}`

Dấu **`@`** trước tên trong biểu thức `${...}` nghĩa là **lấy bean Spring có tên đó**. Bean của class `ProductService` mặc định tên là `productService` (chữ cái đầu viết thường). Template gọi thẳng `imageUrl()` mà controller không phải chuẩn bị thêm gì.

Bên ngoài còn bọc `@{...}` (link URL như mọi khi), để Thymeleaf ghép đúng đường dẫn gốc của app:

```
@{ ${@productService.imageUrl(product)} }
 │   └── "/uploads/3f2a….jpg"  hoặc  "/images/product-default.svg"
 └── biến thành URL hoàn chỉnh cho thuộc tính src
```

> `accept=".jpg,..."` chỉ giúp hộp thoại chọn file **lọc sẵn** cho tiện. Người dùng vẫn đổi được sang "All files", nên server **bắt buộc** phải tự kiểm tra lại (đã làm ở Bước 4).

---

## Bước 10 — `list.html` và CSS: bảng có ảnh thumbnail

Thay phần `<section layout:fragment="content">` trong `templates/products/list.html`:

```html
    <section layout:fragment="content">
        <h1>Sản phẩm</h1>
        <p><a class="btn" th:href="@{/products/new}">+ Thêm sản phẩm</a></p>

        <table>
            <thead>
                <tr><th>Ảnh</th><th>Sản phẩm</th><th>Danh mục</th><th class="right">Giá</th><th>Hành động</th></tr>
            </thead>
            <tbody>
                <tr th:each="p : ${products}">
                    <td>
                        <!-- Chưa có ảnh → imageUrl() trả về ảnh mặc định. loading="lazy": cuộn tới mới tải -->
                        <img class="thumb" th:src="@{${@productService.imageUrl(p)}}" th:alt="${p.name}" loading="lazy">
                    </td>
                    <td>
                        <b th:text="${p.name}">Tên</b>
                        <!-- Mô tả dài → chỉ hiện 80 ký tự đầu, phần sau thành "..." -->
                        <span class="muted"
                              th:unless="${#strings.isEmpty(p.description)}"
                              th:text="${#strings.abbreviate(p.description, 80)}">Mô tả ngắn</span>
                    </td>
                    <td th:text="${p.category?.name}">Danh mục</td>
                    <!-- 1500000.0 → "1.500.000 ₫". Chưa có giá → "—" -->
                    <td class="right"
                        th:text="${p.price != null} ? (${#numbers.formatDecimal(p.price, 1, 'POINT', 0, 'COMMA')} + ' ₫') : '—'">0 ₫</td>
                    <td>
                        <a th:href="@{/products/{id}/edit(id=${p.id})}">Sửa</a> |
                        <a th:href="@{/products/{id}/delete(id=${p.id})}"
                           onclick="return confirm('Xoá sản phẩm này? Ảnh của nó cũng bị xoá.')">Xoá</a>
                    </td>
                </tr>
                <tr th:if="${products.isEmpty()}">
                    <td colspan="5">Chưa có sản phẩm nào.</td>
                </tr>
            </tbody>
        </table>
    </section>
```

So với bảng cũ:

- Bỏ cột ID (người dùng không cần), thêm cột **Ảnh**. Cột **Sản phẩm** gồm tên in đậm, bên dưới là mô tả rút gọn.
- `#strings.abbreviate(text, 80)`: cắt còn 80 ký tự kể cả `...`. `#strings.isEmpty(...)` trả `true` cho cả `null` lẫn chuỗi rỗng.
- `#numbers.formatDecimal(số, 1, 'POINT', 0, 'COMMA')`: phân cách hàng nghìn bằng **dấu chấm**, 0 chữ số thập phân, kiểu Việt Nam.
- Sửa luôn lỗi cũ: dòng "Chưa có sản phẩm nào" trước đây để `colspan="4"` trong khi bảng có 5 cột.

Thêm vào cuối `static/css/app.css`:

```css
/* ----- Bài 15: ảnh sản phẩm ----- */
td {
    vertical-align: middle;
}

.thumb {
    display: block;
    width: 64px;
    height: 64px;
    object-fit: cover;          /* cắt cho vừa khung vuông, ảnh không bị méo */
    border-radius: 4px;
    border: 1px solid #ddd;
}

.preview {
    display: block;
    width: 160px;
    height: 160px;
    object-fit: cover;
    border-radius: 4px;
    border: 1px solid #ddd;
    margin-bottom: 8px;
}

.muted {
    display: block;
    margin-top: 4px;
    color: #666;
    font-size: 13px;
}

.right {
    text-align: right;
    white-space: nowrap;        /* "1.500.000 ₫" không bị xuống dòng */
}

textarea.form_controll {
    resize: vertical;           /* chỉ cho kéo dãn theo chiều dọc */
}
```

> **"Thumbnail" ở đây mới chỉ là *hiển thị nhỏ*.** Trình duyệt vẫn tải **nguyên file** (có thể vài MB) rồi CSS thu nhỏ lại. Dự án thật sẽ tạo thêm một file ảnh nhỏ thật sự lúc upload (thư viện Thumbnailator...). Bài này để phần đó lại cho sau.

---

## Bước 11 — Docker và git: không làm mất, không commit nhầm ảnh

### `.gitignore` và `.dockerignore`

Thêm vào cuối **cả hai file**:

```
# Ảnh người dùng upload (Bài 15) — là DỮ LIỆU, không phải source code
uploads/
```

- `.gitignore`: ảnh test của bạn không bị commit lẫn vào code.
- `.dockerignore`: lúc `docker compose up --build`, Docker không gửi cả thư mục ảnh vào quá trình build.

### Chế độ dev (`docker-compose.dev.yml`): **không cần sửa gì**

Container `app-dev` mount cả project vào `/app` và chạy ở `working_dir: /app`. Vì vậy `uploads` tương đối thành `/app/uploads`, cũng chính là thư mục **`uploads/` trong project trên máy bạn**. Upload xong là thấy file ngay trong VSCode.

### Chế độ thường (`docker-compose.yml`): **phải thêm volume**

Ở chế độ này, `/app/uploads` nằm **bên trong container**. Lần sau `docker compose up --build`, container cũ bị xoá và tạo lại, **mọi ảnh mất sạch**. Cách xử lý giống hệt cách MySQL giữ dữ liệu bằng `mysql_data`. Thêm vào service `app`:

```yaml
  app:
    # ... (build, container_name, depends_on, environment, ports giữ nguyên)
    volumes:
      # Ảnh upload nằm ngoài container → build lại / tạo lại container vẫn còn
      - uploads_data:/app/uploads
```

và khai báo ở khối `volumes:` cuối file (giữ nguyên các volume đang có):

```yaml
volumes:
  mysql_data:
  uploads_data:
```

> `docker compose down -v` giờ xoá **cả ảnh** lẫn dữ liệu MySQL. Sau đó các sản phẩm cũ (nếu được tạo lại) sẽ dùng ảnh mặc định.

### Chạy ở môi trường khác thì `uploads` nằm đâu?

| Cách chạy | Thư mục đang chạy app | `uploads` thành | Ghi chú |
|---|---|---|---|
| `docker-compose.dev.yml` | `/app` = project trên máy bạn | `<project>/uploads` | Thấy file ngay trên máy |
| `docker-compose.yml` | `/app` trong container | `/app/uploads` → volume `uploads_data` | Cần volume ở trên |
| `./gradlew bootRun` trên máy | thư mục project | `<project>/uploads` | Giống chế độ dev |
| Server thật, `java -jar demo.jar` | thư mục đang đứng khi gõ lệnh | **dễ lệch** | Nên đặt đường dẫn tuyệt đối: `UPLOAD_DIR=/var/lib/demo/uploads java -jar demo.jar` |

Ảnh mặc định thì **không phụ thuộc** bảng này, vì nó nằm trong `.jar`.

---

## Bước 12 — Chạy & kiểm tra

```bash
docker compose -f docker-compose.dev.yml up -d
docker compose -f docker-compose.dev.yml logs -f app-dev
```

Trong log phải thấy:

```
[Upload] Thư mục lưu file: /app/uploads
```

và **không** có dòng `[Product] Không thấy ảnh mặc định...`.

**1. Ảnh mặc định được phục vụ:**

```bash
curl -I http://localhost:8093/images/product-default.svg
# HTTP/1.1 200
# Content-Type: image/svg+xml
```

**2. Trang danh sách:** đăng nhập, vào `http://localhost:8093/products`. Mọi sản phẩm cũ đều hiện ảnh "Chưa có ảnh".

**3. Upload ảnh:** bấm **Sửa** một sản phẩm, nhập mô tả, chọn một ảnh `.jpg`, rồi bấm **Lưu**. Ở trang danh sách, sản phẩm đó hiện ảnh nhỏ và mô tả rút gọn. Kiểm tra trên máy:

```bash
ls -la uploads/
# 3f2a9c1e-7b4d-4c8a-9e21-0a6c5d1f8b3e.jpg
```

**4. Xem trong DB:** bảng chỉ lưu tên file.

```bash
docker exec -it demo-mysql mysql -u demo_user -pdemo_pass demo_db \
  -e "SELECT id, name, image_path, LEFT(description, 30) AS mo_ta FROM product;"
```

**5. Đổi ảnh:** sửa lại sản phẩm đó, chọn ảnh khác. Chạy `ls uploads/`: file cũ đã biến mất, chỉ còn file mới.

**6. Sửa mà không chọn ảnh:** chỉ đổi giá rồi Lưu. Ảnh **vẫn giữ nguyên**. Đây là lý do `update()` không copy `imagePath`.

**7. Xoá sản phẩm:** file ảnh của nó cũng biến khỏi `uploads/`.

**8. File sai định dạng:** trong hộp thoại chọn file, đổi bộ lọc sang "All files" rồi chọn một file `.pdf`. Bấm Lưu sẽ ra trang lỗi `Chỉ nhận ảnh jpg, jpeg, png, gif, webp...`, và sản phẩm **không bị thay đổi gì** (vì file được kiểm tra trước khi ghi DB).

**9. REST API vẫn chạy:** `GET /api/products` giờ trả thêm `description` và `imagePath`:

```bash
curl -s -u admin:admin123 http://localhost:8093/api/products | head -c 400
```

(dùng tài khoản bạn đã tạo ở bài 12/13)

---

## Lỗi thường gặp

| Triệu chứng | Nguyên nhân | Cách sửa |
|-------------|-------------|----------|
| Chọn ảnh, bấm Lưu, nhưng ảnh không đổi, `uploads/` trống | `<form>` thiếu `enctype="multipart/form-data"` | Kiểm tra Bước 9 |
| Sửa sản phẩm (không chọn ảnh) thì ảnh cũ **mất** | Trong `update()` có dòng `product.setImagePath(data.getImagePath())` | Bỏ dòng đó, xem comment ở Bước 7 |
| Ảnh vỡ, mở `/uploads/xxx.jpg` ra **404** dù file có trong `uploads/` | Thiếu `WebConfig`, hoặc `addResourceLocations` thiếu `/` ở cuối | Kiểm tra Bước 5 |
| Ảnh mặc định vỡ, log có `[Product] Không thấy ảnh mặc định` | File không nằm ở `static/images/`, hoặc `default-image` ghi thừa `static/` / thừa `/` ở đầu | Giá trị đúng: `images/product-default.svg` |
| App không khởi động: `Could not resolve placeholder 'app.upload.dir'` | Thiếu khối `app:` trong `application.yml`, hoặc thụt lề sai (`app:` bị lọt vào trong `management:`) | `app:` phải sát lề trái, xem Bước 1 |
| App không khởi động: `DuplicateKeyException ... duplicate key spring` | Viết thêm một khối `spring:` thứ hai | Gộp `servlet:` vào khối `spring:` đang có |
| Chọn ảnh > 5MB thì ra trang **403 Forbidden** hoặc trình duyệt báo mất kết nối, không thấy báo "file quá lớn" | Request vượt giới hạn, Tomcat không đọc nội dung form nữa → Spring Security không tìm thấy CSRF token → 403 | Chọn ảnh nhỏ hơn, hoặc tăng `max-file-size` ở Bước 1 |
| Upload `.jpg` vẫn báo "Chỉ nhận ảnh..." | File thật ra không có đuôi, hoặc đuôi lạ như `.jfif`, `.heic` | Đổi đuôi / chuyển định dạng ảnh, hoặc thêm đuôi vào `ALLOWED_EXTENSIONS` |
| Chạy `docker compose up --build` xong thì **mất hết ảnh** | `docker-compose.yml` thiếu volume `uploads_data` | Kiểm tra Bước 11 |
| Mỗi lần upload là app restart | Bạn đã đổi `app.upload.dir` trỏ vào `src/main/resources/...` | Trả lại `uploads`. Xem mục "Vì sao KHÔNG lưu vào static/" |
| Template báo lỗi `EL1057E: No bean resolver registered` hoặc `Bean 'productService' not found` | Gõ sai tên bean, ví dụ `@ProductService` (viết hoa) | Tên bean viết thường chữ đầu: `@productService` |

---

## Bài tập

1. **Nút "Xoá ảnh"**: thêm vào form một checkbox `<input type="checkbox" name="removeImage">`. Khi được tick, đưa sản phẩm về ảnh mặc định và xoá file trên ổ đĩa. Phải sửa những chỗ nào: controller, `ProductService.save()`, hay cả hai?
2. **Báo lỗi dễ hiểu hơn**: chọn file `.pdf` hiện đang ra trang lỗi trắng. Hãy cho nó quay lại form, giữ nguyên dữ liệu đã nhập, và hiện dòng chữ đỏ "Chỉ nhận ảnh...". Gợi ý: `try/catch IllegalArgumentException` trong controller, `model.addAttribute("error", ...)`, rồi `return "products/form"` thay vì redirect. Vì sao lúc này **không được** redirect?
3. **Đổi chỗ lưu**: trong `docker-compose.dev.yml`, thêm `UPLOAD_DIR: /app/storage/products` vào `environment` của `app-dev`, rồi `up -d` lại. Phải sửa **dòng Java nào**? (Gợi ý: không dòng nào.) Ảnh cũ trong `uploads/` còn hiện không, vì sao? Nhớ cập nhật `.gitignore`.
4. **Thử sai ảnh mặc định**: đổi `default-image` thành `images/khong-co.png`. Log báo gì? Trang danh sách hiện gì? Đổi lại như cũ.
5. **File mồ côi**: thêm sản phẩm mới với giá `-5` và có chọn ảnh. `create()` ném lỗi "Giá không được âm", nhưng chạy `ls uploads/` thì sao? Vì sao lại có file thừa? Sửa thế nào? (Gợi ý: thứ tự các bước trong `save()`, hoặc dùng `try/catch` để xoá file mới khi ghi DB lỗi.)
6. **Thử GridFS (nâng cao, cần xong bài 14)**: viết class `GridFsStorageService` có đúng 3 phương thức `save`, `delete`, và một controller `GET /uploads/{filename}` đọc file từ MongoDB bằng `GridFsTemplate`. Để chuyển qua lại giữa 2 cách lưu, bạn phải sửa những file nào? So sánh với bảng ở phần "Tư vấn" đầu bài: bạn cảm nhận được nhược điểm nào của BLOB?
7. **Tìm hiểu**: nếu chạy **2 container app** phía sau một load balancer, người dùng upload ảnh vào container A rồi lần tải trang sau lại rơi vào container B. Chuyện gì xảy ra? Có những cách nào để giải quyết (volume dùng chung, MinIO/S3...)?

➡️ Bài sau: [15.5 — Reset dữ liệu và chạy lại seeder khi đổi cấu trúc bảng](15.5-reset-du-lieu-va-chay-lai-seeder.md)

➡️ Quay lại tổng quan: [../README.md](../README.md)
