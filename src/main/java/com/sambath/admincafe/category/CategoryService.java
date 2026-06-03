package com.sambath.admincafe.category;

import com.sambath.admincafe.category.dto.CategoryRequest;
import com.sambath.admincafe.category.dto.CategoryResponse;
import com.sambath.admincafe.common.ConflictException;
import com.sambath.admincafe.common.NotFoundException;
import com.sambath.admincafe.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public CategoryResponse create(CategoryRequest request) {
        Category category = new Category();
        category.setId(slugify(request.name()));
        category.setName(request.name());
        category.setImage(request.image());
        category.setIcon("Coffee");
        return toResponse(categoryRepository.save(category));
    }

    public CategoryResponse update(String id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found: " + id));
        categoryRepository.findFirstByNameIgnoreCase(request.name())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new ConflictException("Category name already exists.");
                });
        category.setName(request.name());
        category.setImage(request.image());
        return toResponse(categoryRepository.save(category));
    }

    public void delete(String id) {
        if (!categoryRepository.existsById(id)) {
            throw new NotFoundException("Category not found: " + id);
        }
        categoryRepository.deleteById(id);
    }

    private CategoryResponse toResponse(Category c) {
        long count = productRepository.countByCategory(c.getId());
        return new CategoryResponse(c.getId(), c.getName(), count, c.getImage(), c.getIcon());
    }

    private static String slugify(String name) {
        return name.toLowerCase().trim().replaceAll("\\s+", "_");
    }
}
