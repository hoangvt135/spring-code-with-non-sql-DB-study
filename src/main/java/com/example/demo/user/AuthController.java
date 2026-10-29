package com.example.demo.user;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }
    
    // GET /login → chỉ hiện trang. POST /login do Spring Security xử lý, KHÔNG viết ở đây
    @GetMapping("/login")
    public String loginForm() {
        return "auth/login";
    }

    // GET /register → form rỗng
    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("user", new User());
        return "auth/register";
    }

    // POST /register → tạo tài khoản
    @PostMapping("/register")
    public String register(@ModelAttribute User user, Model model) {
        try {
            userService.register(user);
        } catch (IllegalArgumentException e) {
            // Lỗi → hiện lại form kèm thông báo, giữ nguyên tên đã gõ
            model.addAttribute("error", e.getMessage());
            user.setPassword("");   // không gửi mật khẩu ngược về trình duyệt
            return "auth/register";
        }
        return "redirect:/login?registered";
    }
}
