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
    private ProductRepository productRepository;
    private final FileStorageService fileStorageService;
    private final String defaultImageUrl;

    public ProductService(ProductRepository productRepository, 
                            FileStorageService fileStorageService, 
                            @Value("${app.product.default-image}") String defaultImage) {
        this.productRepository = productRepository;
        this.fileStorageService = fileStorageService;
        this.defaultImageUrl = "/" + defaultImage;

        if (!new ClassPathResource("static/" + defaultImage).exists()) {
            log.warn("[Product] ảnh mặc định không tồn tại src/main/resources/static/{}", defaultImage);
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
        product.setImagePath(null);
        return productRepository.save(product);
    }

    public Product update(Long id, Product data) {
        Product product = findById(id);      // đảm bảo tồn tại
        product.setName(data.getName());
        product.setPrice(data.getPrice());
        product.setCategory(data.getCategory());
        product.setDescription(data.getDescription());
        return productRepository.save(product);     // save = update khi id đã có
    }

    public void delete(Long id) {
        Product product = findById(id);
        productRepository.delete(product);
        fileStorageService.delete(product.getImagePath());
    }

    public Product save(Product data, MultipartFile imageFile) {
        String newImage = fileStorageService.save(imageFile);

        Product product = (data.getId() == 0) ? create(data) : update(data.getId(), data);

        if (newImage != null) {
            String oldImage = product.getImagePath();
            product.setImagePath(newImage);
            productRepository.save(product);
            fileStorageService.delete(oldImage);
        }
        return product;
    }

    public String imageUrl(Product product) {
        if (product.getImagePath() == null) {
            return defaultImageUrl;
        }
        return FileStorageService.URL_PREFIX + product.getImagePath();
    }
}
