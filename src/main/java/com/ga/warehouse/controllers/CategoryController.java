package com.ga.warehouse.controllers;


import com.ga.warehouse.models.Category;
import com.ga.warehouse.response.SuccessResponse;
import com.ga.warehouse.services.CategoryService;
import com.ga.warehouse.utils.ResponseBuilder;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@AllArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;


    /**
     *
     * @param category
     * @return
     */
    @PostMapping
    @PreAuthorize("hasAuthority('category:create')")
    public ResponseEntity<SuccessResponse> createCategory(@RequestBody Category category) {
        Category newCategory = categoryService.createCategory(category);
        return ResponseBuilder.success(HttpStatus.CREATED, "Category created successfully", newCategory);
    }

    /**
     *
     * @return
     */
    @GetMapping
    @PreAuthorize("hasAuthority('category:read')")
    public ResponseEntity<SuccessResponse> getAllCategories() {
        List<Category> categories = categoryService.getAllCategories();
        return ResponseBuilder.success(HttpStatus.OK, "All categories retrieved", categories);
    }


    /**
     *
     * @param id
     * @return
     */
    @GetMapping("{id}")
    @PreAuthorize("hasAuthority('category:read')")
    public ResponseEntity<SuccessResponse> getCategoryById(@PathVariable Long id) {
        Category category = categoryService.findCategoryById(id);
        return ResponseBuilder.success(HttpStatus.OK, "Category retrieved successfully", category);
    }

    @PutMapping("{id}")
    @PreAuthorize("hasAuthority('category:update')")
    public ResponseEntity<SuccessResponse> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        Category updatedCategory = categoryService.updateCategory(id, category);
        return ResponseBuilder.success(HttpStatus.OK, "Category updated successfully", updatedCategory);
    }

    @PostMapping("/{id}/image")  // ✅ Different endpoint (was duplicate!)
    @PreAuthorize("hasAuthority('category:update')")
    public ResponseEntity<SuccessResponse> updateCategoryImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        Category updatedCategory = categoryService.uploadCategoryImage(id, file);
        return ResponseBuilder.success(HttpStatus.OK, "Category image updated successfully", updatedCategory);
    }


    @DeleteMapping("{id}")
    @PreAuthorize("hasAuthority('category:delete')")
    public ResponseEntity<SuccessResponse> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategoryById(id);
        return ResponseBuilder.success(HttpStatus.OK, "Category deleted successfully", null);
    }
}
