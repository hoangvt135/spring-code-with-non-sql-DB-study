package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import com.example.demo.storage.FileStorageService;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1) Trang nào ai cũng vào được, trang nào phải đăng nhập
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register", "/css/**", "/images/**", FileStorageService.URL_PREFIX + "**", "/error", "/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            // 2) Đăng nhập bằng form, dùng trang login của mình
            .formLogin(form -> form
                .loginPage("/login")              // GET /login → trang của mình (AuthController)
                .defaultSuccessUrl("/products")   // login xong mà không có trang nào đang chờ → về /products
                .permitAll()
            )
            // 3) Đăng xuất: POST /logout (Spring tự xử lý)
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            // 4) Cho REST API: gửi user/mật khẩu kèm mỗi request (curl -u)
            .httpBasic(Customizer.withDefaults())
            // 5) Tắt CSRF cho /api/** (giải thích ở Bước 8)
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
