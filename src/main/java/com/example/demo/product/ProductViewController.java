package com.example.demo.product;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.security.Principal;
import java.util.List;
import com.example.demo.category.CategoryService;
import com.example.demo.stats.ProductStatsService;

@Controller
@RequestMapping("/products")
public class ProductViewController {
    
    private final ProductService productService;
    private final CategoryService categoryService;
    private final ProductStatsService productStatsService;

    public ProductViewController(ProductService productService,
                                 CategoryService categoryService,
                                 ProductStatsService productStatsService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.productStatsService = productStatsService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", productService.findAll());
        model.addAttribute("views", productStatsService.allViews());       // Map: id → lượt xem
        model.addAttribute("topProducts", productService.topViewed());     // số lượng: app.stats.top-limit
        return "products/list";
    }

    // GET /products/12 → trang chi tiết. Mỗi lần mở là 1 lượt xem.
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Principal principal, Model model) {
        // 1) Tìm trong MySQL TRƯỚC: id không tồn tại → 404 ngay, không đếm bậy vào Redis
        Product product = productService.findById(id);

        // 2) Đếm lượt xem + ghi "vừa xem". principal.getName() = tên đăng nhập (bài 12)
        long views = productStatsService.recordView(id, principal.getName());

        // 3) "Bạn vừa xem": bỏ chính sản phẩm đang xem (nó vừa được đẩy lên đầu danh sách ở bước 2)
        List<Product> recent = productService.recentlyViewed(principal.getName());
        recent.removeIf(p -> p.getId() == product.getId());

        model.addAttribute("product", product);
        model.addAttribute("views", views);
        model.addAttribute("recent", recent);
        return "products/detail";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryService.findAll());
        return "products/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.findById(id));
        model.addAttribute("categories", categoryService.findAll());
        return "products/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Product product, 
                        @RequestParam(required = false) Long categoryId,
                        @RequestParam(required = false) MultipartFile imageFile ) {
        if (categoryId != null) {
            product.setCategory(categoryService.findById(categoryId));
        }
        productService.save(product, imageFile);
        return "redirect:/products";
    }

    @GetMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        productService.delete(id);

        return "redirect:/products";
    }
}
