package com.ga.warehouse.controllers;

import com.ga.warehouse.models.AuctionItem;
import com.ga.warehouse.response.SuccessResponse;
import com.ga.warehouse.services.AuctionItemService;
import com.ga.warehouse.services.UserService;
import com.ga.warehouse.utils.ResponseBuilder;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auction-items")
@AllArgsConstructor
public class AuctionItemController {
    private final AuctionItemService auctionItemService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasAuthority('auctionitem:create')")
    public ResponseEntity<SuccessResponse> createAuctionItem(@RequestBody AuctionItem auctionItem, @AuthenticationPrincipal UserDetails userDetails) {
        Long creatorId = userService.findUserByEmailAddress(userDetails.getUsername()).getId();

        AuctionItem newItem = auctionItemService.createAuctionItem(auctionItem, creatorId);
        return ResponseBuilder.success(HttpStatus.CREATED, "Auction item created successfully", newItem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('auctionitem:read')")
    public ResponseEntity<SuccessResponse> getAllAuctionItems() {
        List<AuctionItem> items = auctionItemService.getAllAuctionItems();
        return ResponseBuilder.success(HttpStatus.OK, "All auction items retrieved", items);
    }

    @GetMapping("{id}")
    @PreAuthorize("hasAuthority('auctionitem:read')")
    public ResponseEntity<SuccessResponse> getAuctionItemById(@PathVariable Long id) {
        AuctionItem item = auctionItemService.getAuctionItemById(id);
        return ResponseBuilder.success(HttpStatus.OK, "Auction item retrieved successfully", item);
    }

    @PutMapping("{id}")
    @PreAuthorize("hasAuthority('auctionitem:update')")
    public ResponseEntity<SuccessResponse> updateAuctionItem(@PathVariable Long id, @RequestBody AuctionItem auctionItem) {
        AuctionItem updated = auctionItemService.updateAuctionItem(id, auctionItem);
        return ResponseBuilder.success(HttpStatus.OK, "Auction item updated successfully", updated);
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasAuthority('auctionitem:delete')")
    public ResponseEntity<SuccessResponse> deleteAuctionItem(@PathVariable Long id) {
        auctionItemService.deleteAuctionItem(id);
        return ResponseBuilder.success(HttpStatus.OK, "Auction item deleted successfully", null);
    }

    // === FILE UPLOADS ===
    @PostMapping("{id}/display-image")
    @PreAuthorize("hasAuthority('auctionitem:update')")
    public ResponseEntity<SuccessResponse> uploadDisplayImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        AuctionItem updated = auctionItemService.uploadDisplayImage(id, file);
        return ResponseBuilder.success(HttpStatus.OK, "Display image uploaded successfully", updated);
    }

    @PostMapping("{id}/gallery-images")
    @PreAuthorize("hasAuthority('auctionitem:update')")
    public ResponseEntity<SuccessResponse> uploadGalleryImages(@PathVariable Long id, @RequestParam("files") List<MultipartFile> files) {
        AuctionItem updated = auctionItemService.uploadGalleryImages(id, files);
        return ResponseBuilder.success(HttpStatus.OK, "Gallery images uploaded successfully", updated);
    }

    @DeleteMapping("{id}/gallery-images/{fileName}")
    @PreAuthorize("hasAuthority('auctionitem:update')")
    public ResponseEntity<SuccessResponse> removeGalleryImage(@PathVariable Long id, @PathVariable String fileName) {
        auctionItemService.removeGalleryImage(id, fileName);
        return ResponseBuilder.success(HttpStatus.OK, "Gallery image removed successfully", null);
    }
}