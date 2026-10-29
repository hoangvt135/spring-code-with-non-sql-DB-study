package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Một controller đơn giản để kiểm tra app đã chạy chưa.
 *
 * - @RestController: đánh dấu class này nhận request HTTP và trả về JSON.
 * - @GetMapping("/"): map request GET tới đường dẫn tương ứng.
 *
 * Sau khi chạy app, mở http://localhost:8080/ để thấy kết quả.
 */
@RestController
public class HelloController {

    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of(
                "message", "Spring Boot đang chạy! 🎉",
                "hint", "Xem thư mục docs/ để bắt đầu thực hành CRUD"
        );
    }

    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of("greeting", "Xin chào từ Spring Boot");
    }
}
