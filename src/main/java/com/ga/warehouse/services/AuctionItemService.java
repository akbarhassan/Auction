package com.ga.warehouse.services;


import com.ga.warehouse.exceptions.ResourceNotFoundException;
import com.ga.warehouse.models.AuctionItem;
import com.ga.warehouse.models.Category;
import com.ga.warehouse.models.User;
import com.ga.warehouse.repositories.AuctionItemRepository;
import com.ga.warehouse.repositories.CategoryRepository;
import com.ga.warehouse.repositories.UserRepository;
import com.ga.warehouse.utils.SkuGenerator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuctionItemService {
    private final AuctionItemRepository auctionItemRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final FileStorageService fileStorageService;


    @Transactional
    public AuctionItem createAuctionItem(AuctionItem auctionItem) {
        auctionItem.setSku(SkuGenerator.generate());

        validateAndLinkCreator(auctionItem);

        if (auctionItem.getCategory() != null && auctionItem.getCategory().getId() != null) {
            Category category = categoryRepository.findById(auctionItem.getCategory().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            auctionItem.setCategory(category);
        }

        return auctionItemRepository.save(auctionItem);
    }


    @Transactional
    public List<AuctionItem> getAllAuctionItems() {
        return auctionItemRepository.findAll();
    }


    @Transactional
    public AuctionItem getAuctionItemById(Long id) {
        return auctionItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auction Item not found"));
    }


    @Transactional
    public AuctionItem updateAuctionItem(Long id, AuctionItem updatedItem) {
        AuctionItem existing = getAuctionItemById(id);

        if (updatedItem.getName() != null) existing.setName(updatedItem.getName());
        if (updatedItem.getDescription() != null) existing.setDescription(updatedItem.getDescription());
        if (updatedItem.getStatus() != null) existing.setStatus(updatedItem.getStatus());

        if (updatedItem.getCategory() != null && updatedItem.getCategory().getId() != null) {
            Category category = categoryRepository.findById(updatedItem.getCategory().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            existing.setCategory(category);
        }

        return auctionItemRepository.save(existing);
    }


    @Transactional
    public AuctionItem uploadDisplayImage(Long itemId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        AuctionItem item = getAuctionItemById(itemId);

        // Reuse your existing fileStorageService
        String fileName = fileStorageService.saveFile(file, itemId, "auction-items/display");

        item.setDisplayImage(fileName);
        return auctionItemRepository.save(item);
    }


    @Transactional
    public AuctionItem uploadGalleryImages(Long itemId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one file is required");
        }

        AuctionItem item = getAuctionItemById(itemId);

        // Save each file and collect filenames
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String fileName = fileStorageService.saveFile(file, itemId, "auction-items/gallery");
                item.getGalleryImages().add(fileName);
            }
        }

        return auctionItemRepository.save(item);
    }


    @Transactional
    public void deleteAuctionItem(Long id) {
        if (!auctionItemRepository.existsById(id)) {
            throw new ResourceNotFoundException("Auction Item not found");
        }

        auctionItemRepository.deleteById(id);
    }


    @Transactional
    public AuctionItem removeGalleryImage(Long itemId, String fileName) {
        AuctionItem item = getAuctionItemById(itemId);

        if (item.getGalleryImages().remove(fileName)) {
            fileStorageService.deleteFile(fileName, "auction-items/gallery");
            return auctionItemRepository.save(item);
        }

        throw new ResourceNotFoundException("Image not found in gallery: " + fileName);
    }


    private void validateAndLinkCreator(AuctionItem auctionItem) {
        if (auctionItem.getCreatedBy() == null || auctionItem.getCreatedBy().getId() == null) {
            throw new IllegalArgumentException("Creator user ID is required");
        }
        User creator = userRepository.findById(auctionItem.getCreatedBy().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Creator not found"));
        auctionItem.setCreatedBy(creator);
    }
}
