package com.ga.warehouse.controllers;


import com.ga.warehouse.enums.WareHouseItemsCondition;
import com.ga.warehouse.models.AuctionItem;
import com.ga.warehouse.models.Category;
import com.ga.warehouse.models.User;
import com.ga.warehouse.response.SuccessResponse;
import com.ga.warehouse.security.MyUserDetails;
import com.ga.warehouse.services.AuctionItemService;
import com.ga.warehouse.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class AuctionItemControllerTest {

    @Mock
    private AuctionItemService auctionItemService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuctionItemController auctionItemController;

    // ============ TEST DATA ============
    private User creator;
    private MyUserDetails userDetails;
    private AuctionItem item1;
    private AuctionItem item2;
    private Category category;

    // ============ SETUP ============
    @BeforeEach
    void setUp() {
        creator = new User();
        creator.setId(1L);
        creator.setEmail("creator@test.com");

        userDetails = new MyUserDetails(creator);

        category = new Category();
        category.setId(1L);
        category.setName("Electronics");

        item1 = new AuctionItem();
        item1.setId(1L);
        item1.setName("Laptop");
        item1.setDescription("High-end gaming laptop");
        item1.setSku("SKU-001");
        item1.setStatus(WareHouseItemsCondition.ON_HOLD);

        item2 = new AuctionItem();
        item2.setId(2L);
        item2.setName("Phone");
        item2.setDescription("Smartphone");
        item2.setSku("SKU-002");
        item2.setStatus(WareHouseItemsCondition.AUCTION);
    }

    // ============ TEST: createAuctionItem ============
    @Test
    @DisplayName("createAuctionItem: valid item → returns CREATED with item data")
    void createAuctionItem_validItem_returnsCreated() {
        AuctionItem newItem = new AuctionItem();
        newItem.setName("Tablet");
        newItem.setDescription("New tablet");
        newItem.setCategory(category);

        when(userService.findUserByEmailAddress("creator@test.com")).thenReturn(creator);
        when(auctionItemService.createAuctionItem(newItem, 1L)).thenReturn(item1);

        ResponseEntity<SuccessResponse> result = auctionItemController.createAuctionItem(newItem, userDetails);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().message()).isEqualTo("Auction item created successfully");
        assertThat(result.getBody().data()).isEqualTo(item1);

        verify(userService, times(1)).findUserByEmailAddress("creator@test.com");
        verify(auctionItemService, times(1)).createAuctionItem(newItem, 1L);
    }

    // ============ TEST: getAllAuctionItems ============
    @Test
    @DisplayName("getAllAuctionItems: returns list of items")
    void getAllAuctionItems_returnsListOfItems() {
        List<AuctionItem> items = Arrays.asList(item1, item2);
        when(auctionItemService.getAllAuctionItems()).thenReturn(items);

        ResponseEntity<SuccessResponse> result = auctionItemController.getAllAuctionItems();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().message()).isEqualTo("All auction items retrieved");
        assertThat(result.getBody().data()).isEqualTo(items);

        verify(auctionItemService, times(1)).getAllAuctionItems();
    }

    // ============ TEST: getAuctionItemById ============
    @Test
    @DisplayName("getAuctionItemById: returns item by ID")
    void getAuctionItemById_returnsItemById() {
        when(auctionItemService.getAuctionItemById(1L)).thenReturn(item1);

        ResponseEntity<SuccessResponse> result = auctionItemController.getAuctionItemById(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().message()).isEqualTo("Auction item retrieved successfully");
        assertThat(result.getBody().data()).isEqualTo(item1);

        verify(auctionItemService, times(1)).getAuctionItemById(1L);
    }

    // ============ TEST: updateAuctionItem ============
    @Test
    @DisplayName("updateAuctionItem: updates and returns item")
    void updateAuctionItem_updatesAndReturnsItem() {
        AuctionItem updateRequest = new AuctionItem();
        updateRequest.setName("Updated Laptop");
        updateRequest.setStatus(WareHouseItemsCondition.SOLD);

        AuctionItem updatedItem = new AuctionItem();
        updatedItem.setId(1L);
        updatedItem.setName("Updated Laptop");
        updatedItem.setStatus(WareHouseItemsCondition.SOLD);

        when(auctionItemService.updateAuctionItem(1L, updateRequest)).thenReturn(updatedItem);

        ResponseEntity<SuccessResponse> result = auctionItemController.updateAuctionItem(1L, updateRequest);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().message()).isEqualTo("Auction item updated successfully");
        assertThat(result.getBody().data()).isEqualTo(updatedItem);

        verify(auctionItemService, times(1)).updateAuctionItem(1L, updateRequest);
    }

    // ============ TEST: deleteAuctionItem ============
    @Test
    @DisplayName("deleteAuctionItem: deletes item and returns success")
    void deleteAuctionItem_deletesItem() {
        doNothing().when(auctionItemService).deleteAuctionItem(1L);

        ResponseEntity<SuccessResponse> result = auctionItemController.deleteAuctionItem(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().message()).isEqualTo("Auction item deleted successfully");
        assertThat(result.getBody().data()).isNull();

        verify(auctionItemService, times(1)).deleteAuctionItem(1L);
    }

    // ============ TEST: uploadDisplayImage ============
    @Test
    @DisplayName("uploadDisplayImage: uploads image and returns updated item")
    void uploadDisplayImage_uploadsImage() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "image content".getBytes());

        AuctionItem updatedItem = new AuctionItem();
        updatedItem.setId(1L);
        updatedItem.setDisplayImage("uploaded-test.jpg");

        when(auctionItemService.uploadDisplayImage(1L, file)).thenReturn(updatedItem);

        ResponseEntity<SuccessResponse> result = auctionItemController.uploadDisplayImage(1L, file);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().message()).isEqualTo("Display image uploaded successfully");
        assertThat(result.getBody().data()).isEqualTo(updatedItem);

        verify(auctionItemService, times(1)).uploadDisplayImage(1L, file);
    }

    // ============ TEST: uploadGalleryImages ============
    @Test
    @DisplayName("uploadGalleryImages: uploads images and returns updated item")
    void uploadGalleryImages_uploadsImages() {
        MockMultipartFile file1 = new MockMultipartFile("files", "image1.jpg", "image/jpeg", "content1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "image2.jpg", "image/jpeg", "content2".getBytes());
        List<MultipartFile> files = Arrays.asList(file1, file2);

        AuctionItem updatedItem = new AuctionItem();
        updatedItem.setId(1L);
        updatedItem.getGalleryImages().add("gallery-1.jpg");
        updatedItem.getGalleryImages().add("gallery-2.jpg");

        when(auctionItemService.uploadGalleryImages(eq(1L), any(List.class))).thenReturn(updatedItem);

        ResponseEntity<SuccessResponse> result = auctionItemController.uploadGalleryImages(1L, files);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().message()).isEqualTo("Gallery images uploaded successfully");

        verify(auctionItemService, times(1)).uploadGalleryImages(eq(1L), any(List.class));
    }

    // ============ TEST: removeGalleryImage ============
    @Test
    @DisplayName("removeGalleryImage: removes image and returns success")
    void removeGalleryImage_removesImage() {
        AuctionItem updatedItem = new AuctionItem();
        updatedItem.setId(1L);
        when(auctionItemService.removeGalleryImage(1L, "image.jpg")).thenReturn(updatedItem);

        ResponseEntity<SuccessResponse> result = auctionItemController.removeGalleryImage(1L, "image.jpg");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().message()).isEqualTo("Gallery image removed successfully");
        assertThat(result.getBody().data()).isNull();

        verify(auctionItemService, times(1)).removeGalleryImage(1L, "image.jpg");
    }
}
