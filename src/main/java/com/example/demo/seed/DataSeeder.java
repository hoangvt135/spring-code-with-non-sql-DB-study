package com.example.demo.seed;

import com.example.demo.category.Category;
import com.example.demo.category.CategoryRepository;
import com.example.demo.product.Product;
import com.example.demo.product.ProductRepository;
import com.example.demo.user.User;
import com.example.demo.user.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                     CategoryRepository categoryRepository,
                     ProductRepository productRepository,
                     PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedAdmin();
        seedSampleData();
    }

    private void seedAdmin() {
        if (userRepository.existsByUsername("admin")) {
            log.info("[Seed] đã có admin");
            return;
        }

        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole("ADMIN");
        userRepository.save(admin);

        log.info("[Seed] đã tạo admin");
    }

    private void seedSampleData() {
        Category phone = createCategory("Điện thoại");
        Category keyboard = createCategory("Bàn phím");
        Category mouse = createCategory("Chuột");

        createProduct("iPhone 15", 22990000, phone);
        createProduct("Samsung Galaxy S24", 19990000, phone);
        createProduct("Keychron K2", 1890000, keyboard);
        createProduct("Logitech MX Keys", 2490000, keyboard);
        createProduct("Logitech G102", 390000, mouse);

        log.info("[Seed] Đã tạo 3 danh mục + 5 sản phẩm mẫu");
    }

    private Category createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        return categoryRepository.save(category);  
    }

    private void createProduct(String name, double price, Category category) {
        createProduct(name, price, category, "");
    }

    private void createProduct(String name, double price, Category category, String description) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(price);
        product.setCategory(category); 
        product.setDescription(description); 
        productRepository.save(product);
    }
}
