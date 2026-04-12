package com.ga.warehouse.services;


import com.ga.warehouse.exceptions.ResourceAlreadyExistsException;
import com.ga.warehouse.exceptions.ResourceNotFoundException;
import com.ga.warehouse.models.Category;
import com.ga.warehouse.repositories.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    // ============ TEST DATA ============
    private Category category1;
    private Category category2;

    // ============ SETUP ============
    // @BeforeEach runs before EACH test (ensures test isolation)
    @BeforeEach
    void setUp() {
        // Create test data
        category1 = new Category();
        category1.setId(1L);
        category1.setName("Electronics");
        category1.setDescription("Electronic items");

        category2 = new Category();
        category2.setId(2L);
        category2.setName("Furniture");
        category2.setDescription("Furniture items");
    }

    // ============ TEST: getAllCategories ============
    @Test
    @DisplayName("getAllCategories: returns list of all categories")
    void getAllCategories_returnsAllCategories() {
        List<Category> expectedCategories = Arrays.asList(category1, category2);

        when(categoryRepository.findAll()).thenReturn(expectedCategories);

        // call service
        List<Category> result = categoryService.getAllCategories();
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(category1, category2);

        // Verify the repository method was called exactly once
        verify(categoryRepository, times(1)).findAll();

        // Verify NO other repository methods were called (optional, for strictness)
        verifyNoMoreInteractions(categoryRepository);
    }

    // ============ TEST: getCategory ============
    @Test
    @DisplayName("findCategoryById: returns a category based on ID")
    void findCategoryById_returnsCategory() {
        Category expectedCategory = category1;

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category1));

        Category result = categoryService.findCategoryById(1L);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedCategory);
        verify(categoryRepository, times(1)).findById(1L);
        verifyNoMoreInteractions(categoryRepository);
    }


    @Test
    @DisplayName("findCategoryById: non-existing ID → throws ResourceNotFoundException")
    void findCategoryById_nonExistingId_throwsResourceNotFoundException() {

        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findCategoryById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No Category exists with current id");

        verify(categoryRepository, times(1)).findById(999L);

        verifyNoMoreInteractions(categoryRepository);
    }

    @Test
    @DisplayName("createCategory: unique name -> saves and returns category")
    void createCategory_uniqueName_savesAndReturnsCategory() {
        Category newCategory = new Category();
        newCategory.setName("Books");
        newCategory.setDescription("Books items");

        // allow save to mock
        when(categoryRepository.existsByName("Books")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        Category result = categoryService.createCategory(newCategory);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Books");
        assertThat(result.getDescription()).isEqualTo("Books items");


        // Verify interactions
        verify(categoryRepository, times(1)).existsByName("Books");
        verify(categoryRepository, times(1)).save(newCategory);
        verifyNoMoreInteractions(categoryRepository);
    }

    @Test
    @DisplayName("createCategory: duplicate name → throws ResourceAlreadyExistsException")
    void createCategory_duplicateName_throwsResourceAlreadyExistsException() {
        // ARRANGE
        Category duplicateCategory = new Category();
        duplicateCategory.setName("Electronics");

        when(categoryRepository.existsByName("Electronics")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(duplicateCategory))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("Category with name Electronics already exists");

        // Verify save was NEVER called (validation failed first)
        verify(categoryRepository, times(1)).existsByName("Electronics");
        verify(categoryRepository, never()).save(any(Category.class));
        verifyNoMoreInteractions(categoryRepository);
    }

    @Test
    @DisplayName("updateCategory: valid name + description → updates and returns category")
    void updateCategory_validNameAndDescription_updatesAndReturnsCategory() {

        Category updateRequest = new Category();

        updateRequest.setName("ElectronicsNew");
        updateRequest.setDescription("Electronics updated");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category1));
        when(categoryRepository.existsByName("ElectronicsNew")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        Category result = categoryService.updateCategory(1L, updateRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("ElectronicsNew");
        assertThat(result.getDescription()).isEqualTo("Electronics updated");

        verify(categoryRepository, times(1)).findById(1L);
        verify(categoryRepository, times(1)).existsByName("ElectronicsNew");
        verify(categoryRepository, times(1)).save(category1);
        verifyNoMoreInteractions(categoryRepository);
    }

    @Test
    @DisplayName("updateCategory: duplicate name → throws ResourceAlreadyExistsException")
    void updateCategory_duplicateName_throwsResourceAlreadyExistsException() {
        Category duplicateCategory = new Category();

        duplicateCategory.setName("Furniture");

        // load category
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category1));
        when(categoryRepository.existsByName("Furniture")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.updateCategory(1L, duplicateCategory))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("Category with name Furniture already exists");


        verify(categoryRepository, times(1)).findById(1L);
        verify(categoryRepository, times(1)).existsByName("Furniture");
        verify(categoryRepository, never()).save(any(Category.class));
        verifyNoMoreInteractions(categoryRepository);

    }

    @Test
    @DisplayName("deleteCategoryById: existing ID → deletes category")
    void deleteCategoryById_existingId_deletesCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category1));
        categoryService.deleteCategoryById(1L);


        verify(categoryRepository, times(1)).findById(1L);
        verify(categoryRepository, times(1)).deleteById(1L);
        verifyNoMoreInteractions(categoryRepository);
    }

    @Test
    @DisplayName("deleteCategoryById: missing ID → throws ResourceNotFoundException")
    void deleteCategoryById_missingId_throwsResourceNotFoundException() {
        
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategoryById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No category exists with id: 999");

        verify(categoryRepository, times(1)).findById(999L);
        verify(categoryRepository, never()).deleteById(anyLong());
        verifyNoMoreInteractions(categoryRepository);
    }


}


