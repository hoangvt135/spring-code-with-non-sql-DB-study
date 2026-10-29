package com.example.demo.product;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    // Không cần viết gì thêm — Spring tự sinh save(), findAll(), findById(), deleteById()...
}
