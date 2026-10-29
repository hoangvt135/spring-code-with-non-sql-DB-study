package com.example.demo.product;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")   // Mọi API dưới đây bắt đầu bằng /api/products
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    // READ - danh sách:  GET /api/products
    @GetMapping
    public List<Product> getAll() {
        return service.findAll();
    }

    // READ - một cái:    GET /api/products/1
    @GetMapping("/{id}")
    public Product getOne(@PathVariable Long id) {
        return service.findById(id);
    }

    // CREATE:            POST /api/products   (kèm JSON trong body)
    @PostMapping
    public Product create(@RequestBody Product product) {
        return service.create(product);
    }

    // UPDATE:            PUT /api/products/1
    @PutMapping("/{id}")
    public Product update(@PathVariable Long id, @RequestBody Product product) {
        return service.update(id, product);
    }

    // DELETE:            DELETE /api/products/1
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}