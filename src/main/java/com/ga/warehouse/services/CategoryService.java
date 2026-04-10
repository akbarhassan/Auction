package com.ga.warehouse.services;


import com.ga.warehouse.exceptions.ResourceAlreadyExistsException;
import com.ga.warehouse.exceptions.ResourceNotFoundException;
import com.ga.warehouse.models.Category;
import com.ga.warehouse.repositories.CategoryRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final FileStorageService fileStorageService;


    /**
     *
     * @param category
     * @return
     */
    @Transactional
    public Category createCategory(Category category) {
        if (categoryRepository.existsByName(category.getName())) {
            throw new ResourceAlreadyExistsException("Category with name " + category.getName() + " already exists");
        }
        return categoryRepository.save(category);
    }


    /**
     *
     * @return
     */
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }


    /**
     *
     * @param id
     * @return
     */
    @Transactional
    public Category findCategoryById(Long id) {
        return categoryRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("No role exists with current id")
        );
    }


    /**
     *
     * @param id
     * @param category
     * @return
     */
    @Transactional
    public Category updateCategory(Long id, Category category) {
        Category currentCategory = categoryRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("No category exists with current id")
        );

        if (category.getName() != null) {
            if (!currentCategory.getName().equals(category.getName())) {
                if (categoryRepository.existsByName(category.getName())) {
                    throw new ResourceAlreadyExistsException("Category with name " + category.getName() + " already exists");
                }
                currentCategory.setName(category.getName());
            }
        }
        if (category.getDescription() != null) {
            currentCategory.setDescription(category.getDescription());
        }

        return categoryRepository.save(currentCategory);
    }

    @Transactional
    public Category uploadCategoryImage(Long categoryId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        Category currentCategory = categoryRepository.findById(categoryId).orElseThrow(
                () -> new ResourceNotFoundException("No category exists with current id")
        );

        String fileName = fileStorageService.saveFile(file, categoryId, "categories");

        currentCategory.setCategoryImage(fileName);

        return categoryRepository.save(currentCategory);
    }


    /**
     *
     * @param id
     */
    @Transactional
    public void deleteCategoryById(Long id) {
        Category category = categoryRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("No category exists with id: " + id)
        );

        if (category.getCategoryImage() != null) {
            fileStorageService.deleteFile(category.getCategoryImage(), "categories");
        }

        categoryRepository.deleteById(id);
    }


}
