package com.ga.warehouse.services;


import com.ga.warehouse.enums.WareHouseItemsCondition;
import com.ga.warehouse.exceptions.ResourceNotFoundException;
import com.ga.warehouse.models.AuctionItem;
import com.ga.warehouse.models.Category;
import com.ga.warehouse.models.User;
import com.ga.warehouse.repositories.AuctionItemRepository;
import com.ga.warehouse.repositories.CategoryRepository;
import com.ga.warehouse.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class AuctionItemServiceTest {

    @Mock
    private AuctionItemRepository auctionItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private AuctionItemService auctionItemService;

    // ============ TEST DATA ============
    private AuctionItem item1;
    private AuctionItem item2;
    private User user1;
    private Category category1;

    // ============ SETUP ============
    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setId(1L);
        user1.setEmail("user@test.com");

        category1 = new Category();
        category1.setId(1L);
        category1.setName("Electronics");

        item1 = new AuctionItem();
        item1.setId(1L);
        item1.setName("Laptop");
        item1.setDescription("High-end gaming laptop");
        item1.setSku("SKU-001");
        item1.setStatus(WareHouseItemsCondition.ON_HOLD);
        item1.setCreatedBy(user1);
        item1.setCategory(category1);

        item2 = new AuctionItem();
        item2.setId(2L);
        item2.setName("Phone");
        item2.setDescription("Smartphone");
        item2.setSku("SKU-002");
        item2.setStatus(WareHouseItemsCondition.AUCTION);
        item2.setCreatedBy(user1);
    }

    // ============ TEST: getAllAuctionItems ============
    @Test
    @DisplayName("getAllAuctionItems: returns list of all auction items")
    void getAllAuctionItems_returnsAllAuctionItems() {
        List<AuctionItem> expectedItems = Arrays.asList(item1, item2);

        when(auctionItemRepository.findAll()).thenReturn(expectedItems);

        List<AuctionItem> result = auctionItemService.getAllAuctionItems();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(item1, item2);

        verify(auctionItemRepository, times(1)).findAll();
        verifyNoMoreInteractions(auctionItemRepository);
    }

    // ============ TEST: getAuctionItemById ============
    @Test
    @DisplayName("getAuctionItemById: returns auction item based on ID")
    void getAuctionItemById_returnsAuctionItem() {
        when(auctionItemRepository.findById(1L)).thenReturn(Optional.of(item1));

        AuctionItem result = auctionItemService.getAuctionItemById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Laptop");

        verify(auctionItemRepository, times(1)).findById(1L);
        verifyNoMoreInteractions(auctionItemRepository);
    }

    @Test
    @DisplayName("getAuctionItemById: non-existing ID → throws ResourceNotFoundException")
    void getAuctionItemById_nonExistingId_throwsResourceNotFoundException() {
        when(auctionItemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auctionItemService.getAuctionItemById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Auction Item not found");

        verify(auctionItemRepository, times(1)).findById(999L);
        verifyNoMoreInteractions(auctionItemRepository);
    }

    // ============ TEST: createAuctionItem ============
    @Test
    @DisplayName("createAuctionItem: valid item with category → saves and returns item")
    void createAuctionItem_validItemWithCategory_savesAndReturnsItem() {
        AuctionItem newItem = new AuctionItem();
        newItem.setName("Tablet");
        newItem.setDescription("New tablet");
        newItem.setCategory(category1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category1));
        when(auctionItemRepository.save(any(AuctionItem.class))).thenAnswer(i -> {
            AuctionItem saved = i.getArgument(0);
            saved.setId(3L);
            saved.setSku("GENERATED-SKU");
            return saved;
        });

        AuctionItem result = auctionItemService.createAuctionItem(newItem, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getName()).isEqualTo("Tablet");
        assertThat(result.getSku()).isNotNull();
        assertThat(result.getCreatedBy()).isEqualTo(user1);
        assertThat(result.getCategory()).isEqualTo(category1);

        verify(userRepository, times(1)).findById(1L);
        verify(categoryRepository, times(1)).findById(1L);
        verify(auctionItemRepository, times(1)).save(any(AuctionItem.class));
    }

    @Test
    @DisplayName("createAuctionItem: valid item without category → saves without category")
    void createAuctionItem_validItemWithoutCategory_savesWithoutCategory() {
        AuctionItem newItem = new AuctionItem();
        newItem.setName("Headphones");
        newItem.setDescription("Wireless headphones");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(auctionItemRepository.save(any(AuctionItem.class))).thenAnswer(i -> {
            AuctionItem saved = i.getArgument(0);
            saved.setId(3L);
            saved.setSku("GENERATED-SKU");
            return saved;
        });

        AuctionItem result = auctionItemService.createAuctionItem(newItem, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Headphones");
        assertThat(result.getCategory()).isNull();

        verify(userRepository, times(1)).findById(1L);
        verify(auctionItemRepository, times(1)).save(any(AuctionItem.class));
        verifyNoInteractions(categoryRepository);
    }

    @Test
    @DisplayName("createAuctionItem: non-existing user → throws ResourceNotFoundException")
    void createAuctionItem_nonExistingUser_throwsResourceNotFoundException() {
        AuctionItem newItem = new AuctionItem();
        newItem.setName("Tablet");

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auctionItemService.createAuctionItem(newItem, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Authenticated user not found");

        verify(userRepository, times(1)).findById(999L);
        verify(auctionItemRepository, never()).save(any(AuctionItem.class));
    }

    // ============ TEST: updateAuctionItem ============
    @Test
    @DisplayName("updateAuctionItem: valid update → updates and returns item")
    void updateAuctionItem_validUpdate_updatesAndReturnsItem() {
        AuctionItem updateRequest = new AuctionItem();
        updateRequest.setName("Updated Laptop");
        updateRequest.setDescription("Updated description");
        updateRequest.setStatus(WareHouseItemsCondition.SOLD);

        when(auctionItemRepository.findById(1L)).thenReturn(Optional.of(item1));
        when(auctionItemRepository.save(any(AuctionItem.class))).thenAnswer(i -> i.getArgument(0));

        AuctionItem result = auctionItemService.updateAuctionItem(1L, updateRequest);

        assertThat(result.getName()).isEqualTo("Updated Laptop");
        assertThat(result.getDescription()).isEqualTo("Updated description");
        assertThat(result.getStatus()).isEqualTo(WareHouseItemsCondition.SOLD);

        verify(auctionItemRepository, times(1)).findById(1L);
        verify(auctionItemRepository, times(1)).save(item1);
    }

    @Test
    @DisplayName("updateAuctionItem: update with new category → updates category")
    void updateAuctionItem_updateWithNewCategory_updatesCategory() {
        Category newCategory = new Category();
        newCategory.setId(2L);
        newCategory.setName("Gadgets");

        AuctionItem updateRequest = new AuctionItem();
        updateRequest.setCategory(newCategory);

        when(auctionItemRepository.findById(1L)).thenReturn(Optional.of(item1));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(newCategory));
        when(auctionItemRepository.save(any(AuctionItem.class))).thenAnswer(i -> i.getArgument(0));

        AuctionItem result = auctionItemService.updateAuctionItem(1L, updateRequest);

        assertThat(result.getCategory()).isEqualTo(newCategory);

        verify(auctionItemRepository, times(1)).findById(1L);
        verify(categoryRepository, times(1)).findById(2L);
        verify(auctionItemRepository, times(1)).save(item1);
    }

    // ============ TEST: uploadDisplayImage ============
    @Test
    @DisplayName("uploadDisplayImage: valid file → uploads and updates item")
    void uploadDisplayImage_validFile_uploadsAndUpdatesItem() {
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test content".getBytes());

        when(auctionItemRepository.findById(1L)).thenReturn(Optional.of(item1));
        when(fileStorageService.saveFile(any(MultipartFile.class), anyLong(), anyString())).thenReturn("uploaded-test.jpg");
        when(auctionItemRepository.save(any(AuctionItem.class))).thenAnswer(i -> i.getArgument(0));

        AuctionItem result = auctionItemService.uploadDisplayImage(1L, file);

        assertThat(result.getDisplayImage()).isEqualTo("uploaded-test.jpg");

        verify(auctionItemRepository, times(1)).findById(1L);
        verify(fileStorageService, times(1)).saveFile(file, 1L, "auction-items/display");
        verify(auctionItemRepository, times(1)).save(item1);
    }

    @Test
    @DisplayName("uploadDisplayImage: empty file → throws IllegalArgumentException")
    void uploadDisplayImage_emptyFile_throwsIllegalArgumentException() {
        MockMultipartFile emptyFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> auctionItemService.uploadDisplayImage(1L, emptyFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File cannot be empty");
    }

    // ============ TEST: uploadGalleryImages ============
    @Test
    @DisplayName("uploadGalleryImages: valid files → uploads and updates item")
    void uploadGalleryImages_validFiles_uploadsAndUpdatesItem() {
        MockMultipartFile file1 = new MockMultipartFile("image1", "test1.jpg", "image/jpeg", "content1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("image2", "test2.jpg", "image/jpeg", "content2".getBytes());
        List<MultipartFile> files = Arrays.asList(file1, file2);

        when(auctionItemRepository.findById(1L)).thenReturn(Optional.of(item1));
        when(fileStorageService.saveFile(any(MultipartFile.class), anyLong(), anyString()))
                .thenReturn("gallery-1.jpg", "gallery-2.jpg");
        when(auctionItemRepository.save(any(AuctionItem.class))).thenAnswer(i -> i.getArgument(0));

        AuctionItem result = auctionItemService.uploadGalleryImages(1L, files);

        assertThat(result.getGalleryImages()).contains("gallery-1.jpg", "gallery-2.jpg");

        verify(auctionItemRepository, times(1)).findById(1L);
        verify(fileStorageService, times(2)).saveFile(any(MultipartFile.class), eq(1L), eq("auction-items/gallery"));
        verify(auctionItemRepository, times(1)).save(item1);
    }

    @Test
    @DisplayName("uploadGalleryImages: empty list → throws IllegalArgumentException")
    void uploadGalleryImages_emptyList_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> auctionItemService.uploadGalleryImages(1L, Arrays.asList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one file is required");
    }

    // ============ TEST: deleteAuctionItem ============
    @Test
    @DisplayName("deleteAuctionItem: existing ID → deletes item")
    void deleteAuctionItem_existingId_deletesItem() {
        when(auctionItemRepository.existsById(1L)).thenReturn(true);
        doNothing().when(auctionItemRepository).deleteById(1L);

        auctionItemService.deleteAuctionItem(1L);

        verify(auctionItemRepository, times(1)).existsById(1L);
        verify(auctionItemRepository, times(1)).deleteById(1L);
        verifyNoMoreInteractions(auctionItemRepository);
    }

    @Test
    @DisplayName("deleteAuctionItem: missing ID → throws ResourceNotFoundException")
    void deleteAuctionItem_missingId_throwsResourceNotFoundException() {
        when(auctionItemRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> auctionItemService.deleteAuctionItem(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Auction Item not found");

        verify(auctionItemRepository, times(1)).existsById(999L);
        verify(auctionItemRepository, never()).deleteById(anyLong());
        verifyNoMoreInteractions(auctionItemRepository);
    }

    // ============ TEST: removeGalleryImage ============
    @Test
    @DisplayName("removeGalleryImage: existing image → removes and deletes file")
    void removeGalleryImage_existingImage_removesAndDeletesFile() {
        item1.getGalleryImages().add("gallery-1.jpg");
        item1.getGalleryImages().add("gallery-2.jpg");

        when(auctionItemRepository.findById(1L)).thenReturn(Optional.of(item1));
        doNothing().when(fileStorageService).deleteFile(anyString(), anyString());
        when(auctionItemRepository.save(any(AuctionItem.class))).thenAnswer(i -> i.getArgument(0));

        AuctionItem result = auctionItemService.removeGalleryImage(1L, "gallery-1.jpg");

        assertThat(result.getGalleryImages()).doesNotContain("gallery-1.jpg");
        assertThat(result.getGalleryImages()).contains("gallery-2.jpg");

        verify(auctionItemRepository, times(1)).findById(1L);
        verify(fileStorageService, times(1)).deleteFile("gallery-1.jpg", "auction-items/gallery");
        verify(auctionItemRepository, times(1)).save(item1);
    }

    @Test
    @DisplayName("removeGalleryImage: non-existing image → throws ResourceNotFoundException")
    void removeGalleryImage_nonExistingImage_throwsResourceNotFoundException() {
        when(auctionItemRepository.findById(1L)).thenReturn(Optional.of(item1));

        assertThatThrownBy(() -> auctionItemService.removeGalleryImage(1L, "non-existing.jpg"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Image not found in gallery: non-existing.jpg");

        verify(auctionItemRepository, times(1)).findById(1L);
        verify(fileStorageService, never()).deleteFile(anyString(), anyString());
    }
}
