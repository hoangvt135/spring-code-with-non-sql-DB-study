package com.example.demo.category;


import org.springframework.stereotype.Service;
import com.example.demo.common.NotFoundException; 
import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public Category findById(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new NotFoundException("không tìm thấy category id "+ id));
    }

    public Category creatCategory(Category category) {
        return categoryRepository.save(category);
    }

    public void delete(Long id) {
        categoryRepository.deleteById(id);
    }

    public Category updateCategory(Long id, Category data) {
        Category category = findById(id);
        category.setName(data.getName());
        return categoryRepository.save(category);
    }
}
