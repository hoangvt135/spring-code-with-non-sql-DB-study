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

@Service
public class FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    public static final String URL_PREFIX = "/uploads/";

    private static final Set<String> ALLOWED_EXTENSION =  Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final Path root;

    public FileStorageService(@Value("${app.upload.dir}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new RuntimeException("không tạo được thu mục upload: " + root, e);
        }
        log.info("[Upload] thư mục lưu file: {}", root);
    }

    public Path getRoot() {
        return root;
    }

    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (ext == null || !ALLOWED_EXTENSION.contains(ext.toLowerCase())) {
            throw new IllegalArgumentException("ảnh không được hỗ trợ, chỉ nhận JPG, JPEG, PNG, GIF, WEBP");
        }

        String filename = UUID.randomUUID() + "." + ext.toLowerCase();
        try {
            file.transferTo(resolve(filename));
        } catch (IOException e) {
            throw new RuntimeException("lưu file thất bại: " + filename, e);
        }
        return filename;
    }

    public void delete(String filename) {
        if (filename == null || filename.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(filename));
        } catch (IOException e) {
            log.warn("[Upload] không xóa được file {}: {}", filename, e.getMessage());
        }
    }

    private Path resolve(String filename) {
        Path target = root.resolve(filename).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("tên file không hợp lệ: " + filename);
        }
        return target;
    }
}
