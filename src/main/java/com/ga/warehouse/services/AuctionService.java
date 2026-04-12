package com.ga.warehouse.services;


import com.ga.warehouse.enums.AuctionStatus;
import com.ga.warehouse.exceptions.ResourceAlreadyExistsException;
import com.ga.warehouse.exceptions.ResourceNotFoundException;
import com.ga.warehouse.exceptions.ValidationException;
import com.ga.warehouse.models.Auction;
import com.ga.warehouse.models.AuctionItem;
import com.ga.warehouse.models.User;
import com.ga.warehouse.repositories.AuctionItemRepository;
import com.ga.warehouse.repositories.AuctionRepository;
import com.ga.warehouse.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuctionService {
    private final AuctionRepository auctionRepository;
    private final AuctionItemRepository auctionItemRepository;
    private final UserRepository userRepository;


    /**
     *
     * @param auction
     * @return
     */
    @Transactional
    public Auction createAuction(Auction auction, Long authenticatedUserId) {
        if (auction.getStartPrice() == null || auction.getStartPrice() <= 0) {
            throw new ValidationException("Start price must be greater than 0");
        }

        if (auction.getStartsAt() == null) {
            throw new ValidationException("Start date is required");
        }
        if (auction.getEndsAt() == null) {
            throw new ValidationException("End date is required");
        }

        if (!auction.getEndsAt().isAfter(auction.getStartsAt())) {
            throw new ValidationException("End date must be after start date");
        }

        if (auction.getAuctionItem() == null || auction.getAuctionItem().getId() == null) {
            throw new ValidationException("Auction item is required");
        }

        Optional<Auction> existing = auctionRepository.findByAuctionItemId(auction.getAuctionItem().getId());

        if (existing.isPresent()) {
            throw new ResourceAlreadyExistsException("Auction already exists for this item");
        }

        if (auction.getAuctionedBy() == null || auction.getAuctionedBy().getId() == null) {
            throw new ValidationException("Auction creator user is required");
        }

        User creator = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        auction.setAuctionedBy(creator);

        AuctionItem item = auctionItemRepository.findById(auction.getAuctionItem().getId()).orElseThrow(() -> new ResourceNotFoundException("Auction item not found"));
        auction.setAuctionItem(item);


        LocalDateTime now = LocalDateTime.now();
        if (auction.getStartsAt().isBefore(now) || auction.getStartsAt().isEqual(now)) {
            auction.setStatus(AuctionStatus.ACTIVE);
        } else {
            auction.setStatus(AuctionStatus.PENDING);
        }

        auction.setCurrentHighestBid(auction.getStartPrice());

        if (auction.getMinimumIncrement() == null || auction.getMinimumIncrement() <= 0) {
            auction.setMinimumIncrement(5.0f);
        }

        return auctionRepository.save(auction);
    }


    /**
     *
     * @param id
     * @param auction
     * @return
     */
    @Transactional
    public Auction updateAuction(Long id, Auction auction) {
        Auction existing = auctionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Auction not found"));

        if (existing.getStatus() == AuctionStatus.ENDED || existing.getStatus() == AuctionStatus.CANCELLED) {
            throw new IllegalStateException("Cannot modify an auction that is " + existing.getStatus());
        }

        if (auction.getStatus() != null && auction.getStatus() != existing.getStatus()) {
            if (auction.getStatus() == AuctionStatus.CANCELLED) {
                existing.setStatus(AuctionStatus.CANCELLED);
            } else {
                throw new IllegalArgumentException("Status can only be changed to CANCELLED manually. ACTIVE/ENDED are system-controlled.");
            }
        }

        if (existing.getStatus() == AuctionStatus.CANCELLED) {
            return auctionRepository.save(existing);
        }

        boolean isPending = existing.getStatus() == AuctionStatus.PENDING;
        boolean isActive = existing.getStatus() == AuctionStatus.ACTIVE;


        if (auction.getStartsAt() != null) {
            if (!isPending) {
                throw new IllegalStateException("Cannot change start date of an active auction.");
            }
            existing.setStartsAt(auction.getStartsAt());
        }

        if (auction.getEndsAt() != null) {
            if (!auction.getEndsAt().isAfter(existing.getStartsAt())) {
                throw new ValidationException("End date must be after start date.");
            }
            existing.setEndsAt(auction.getEndsAt());
        }

        if (auction.getMinimumIncrement() != null) {
            if (!isPending) {
                throw new IllegalStateException("Cannot change minimum increment once auction is active.");
            }
            if (auction.getMinimumIncrement() <= 0) {
                throw new ValidationException("Minimum increment must be greater than 0.");
            }
            existing.setMinimumIncrement(auction.getMinimumIncrement());
        }

        if (auction.getStartPrice() != null && !auction.getStartPrice().equals(existing.getStartPrice())) {
            throw new IllegalStateException("Start price cannot be changed after auction creation.");
        }

        if (auction.getCurrentHighestBid() != null && !auction.getCurrentHighestBid().equals(existing.getCurrentHighestBid())) {
            throw new IllegalStateException("Current highest bid cannot be manually updated. Use the bidding endpoint.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (isPending && existing.getStartsAt().isBefore(now)) {
            existing.setStatus(AuctionStatus.ACTIVE);
        }

        if (existing.getEndsAt().isBefore(now)) {
            existing.setStatus(AuctionStatus.ENDED);
        }
        return auctionRepository.save(existing);
    }


    /**
     *
     * @return
     */
    @Transactional
    public List<Auction> getAllAuctions() {
        return auctionRepository.findAll();
    }


    /**
     *
     * @param id
     * @return
     */
    public Auction getAuctionById(Long id) {
        return auctionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Auction with id: " + id + " not found"));
    }


    /**
     *
     * @param id
     */
    @Transactional
    public void deleteAuctionById(Long id) {
        Auction auction = auctionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Auction with id: " + id + " not found"));
        auctionRepository.delete(auction);
    }


    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    @Transactional
    public void updateAuctionStatuses() {
        LocalDateTime now = LocalDateTime.now();

        // PENDING → ACTIVE
        List<Auction> toActivate = auctionRepository.findByStatusAndStartsAtBefore(AuctionStatus.PENDING, now);
        toActivate.forEach(a -> a.setStatus(AuctionStatus.ACTIVE));
        auctionRepository.saveAll(toActivate);

        // ACTIVE → ENDED
        List<Auction> toEnd = auctionRepository.findByStatusAndEndsAtBefore(AuctionStatus.ACTIVE, now);
        toEnd.forEach(a -> a.setStatus(AuctionStatus.ENDED));
        auctionRepository.saveAll(toEnd);
    }
}
