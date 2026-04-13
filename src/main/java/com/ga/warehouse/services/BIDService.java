package com.ga.warehouse.services;


import com.ga.warehouse.enums.AuctionStatus;
import com.ga.warehouse.exceptions.ResourceNotFoundException;
import com.ga.warehouse.exceptions.ValidationException;
import com.ga.warehouse.models.Auction;
import com.ga.warehouse.models.BID;
import com.ga.warehouse.models.User;
import com.ga.warehouse.models.UserProfile;
import com.ga.warehouse.repositories.AuctionRepository;
import com.ga.warehouse.repositories.BIDRepository;
import com.ga.warehouse.repositories.UserRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class BIDService {
    private final BIDRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Qualifier("bidExecutorService")
    private final ExecutorService bidExecutorService;

    @Qualifier("notificationExecutorService")
    private final ExecutorService notificationExecutorService;


    /**
     * Place a bid using explicit Java threading
     * Submits bid processing to a thread pool with Future result
     */
    public Future<BID> placeBidAsync(Long auctionId, Float bidAmount, Long bidderId) {
        // Submit task to thread pool
        return bidExecutorService.submit(() -> {
            try {
                log.info("🧵 Bid processing started in thread: {}", Thread.currentThread().getName());
                return processBid(auctionId, bidAmount, bidderId);
            } catch (Exception e) {
                log.error("❌ Bid processing failed in thread: {}", Thread.currentThread().getName(), e);
                throw e; // Re-throw so Future captures the exception
            }
        });
    }

    /**
     *
     * @param auction
     */
    private void validateAuctionForBidding(Auction auction) {
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new ValidationException("Auction is not active (status: " + auction.getStatus() + ")");
        }
        if (LocalDateTime.now().isAfter(auction.getEndsAt())) {
            throw new ValidationException("Auction has ended");
        }
    }

    /**
     *
     * @param auction
     * @param bidAmount
     */
    private void validateBidAmount(Auction auction, Float bidAmount) {
        Float minRequired = auction.getCurrentHighestBid() + auction.getMinimumIncrement();
        if (bidAmount < minRequired) {
            throw new ValidationException(
                    String.format("Bid must be at least %.2f (current: %.2f, increment: %.2f)",
                            minRequired, auction.getCurrentHighestBid(), auction.getMinimumIncrement())
            );
        }
    }

    /**
     *
     * @param auction
     * @param bidder
     */
    private void validateBidder(Auction auction, User bidder) {
        if (auction.getAuctionedBy().getId().equals(bidder.getId())) {
            throw new ValidationException("You cannot bid on your own auction");
        }
    }


    /**
     *
     * @param previousBidder
     * @param auction
     * @param newBidAmount
     * @param previousBidAmount
     */
    private void notifyPreviousBidder(User previousBidder, Auction auction, Float newBidAmount, Float previousBidAmount) {
        // Extract data BEFORE going async (avoid lazy loading issues)
        String bidderEmail = previousBidder.getEmail();
        Long bidderId = previousBidder.getId();
        String bidderName = getDisplayName(previousBidder);
        String itemName = auction.getAuctionItem().getName();
        Long auctionId = auction.getId();
        Float minIncrement = auction.getMinimumIncrement();
        Float startPrice = auction.getStartPrice();
        java.time.LocalDateTime endsAt = auction.getEndsAt();

        // Submit to notification thread pool
        CompletableFuture.runAsync(() -> {
            try {
                log.info("📧 Sending outbid notification in thread: {}",
                        Thread.currentThread().getName());

                sendOutbidNotification(bidderEmail, bidderName, itemName, auctionId,
                        previousBidAmount != null ? previousBidAmount : startPrice,
                        newBidAmount, minIncrement, endsAt);

                log.info("✅ Outbid notification sent to user {} for auction {}",
                        bidderId, auctionId);

            } catch (Exception e) {
                log.error("❌ Failed to send outbid notification to user {}",
                        bidderId, e);
            }
        }, notificationExecutorService);
    }


    /**
     *
     * @param auctionId
     * @param bidAmount
     * @param bidderId
     * @return
     */
    @Transactional
    public BID processBid(Long auctionId, Float bidAmount, Long bidderId) {
        Auction auction = auctionRepository.findByIdWithLock(auctionId).orElseThrow(
                () -> new ResourceNotFoundException("Auction not found with id: " + auctionId)
        );

        validateAuctionForBidding(auction);

        validateBidAmount(auction, bidAmount);

        User bidder = userRepository.findById(bidderId)
                .orElseThrow(() -> new ResourceNotFoundException("Bidder not found"));
        validateBidder(auction, bidder);

        // 5. Get previous highest bidder (for notification)
        Optional<BID> previousHighestBid = bidRepository.findFirstByAuctionIdOrderByAmountDesc(auctionId);
        User previousBidder = previousHighestBid.map(BID::getBidder).orElse(null);
        Float previousBidAmount = previousHighestBid.map(BID::getAmount).orElse(null);

        BID newBid = new BID();
        newBid.setAmount(bidAmount);
        newBid.setBidder(bidder);
        newBid.setAuction(auction);
        BID savedBid = bidRepository.save(newBid);

        auction.setCurrentHighestBid(bidAmount);
        auctionRepository.save(auction);

        log.info("✅ Bid placed successfully: {} by user {} on auction {}",
                bidAmount, bidderId, auctionId);

        if (previousBidder != null && !previousBidder.getId().equals(bidderId)) {
            notifyPreviousBidder(previousBidder, auction, bidAmount, previousBidAmount);
        }

        return savedBid;
    }


    @Transactional(readOnly = true)
    public List<BID> getBidsForAuction(Long auctionId) {
        return bidRepository.findByAuctionIdOrderByAmountDesc(auctionId);
    }

    @Transactional(readOnly = true)
    public Optional<BID> getHighestBid(Long auctionId) {
        return bidRepository.findFirstByAuctionIdOrderByAmountDesc(auctionId);
    }

    @Transactional(readOnly = true)
    public long getBidCount(Long auctionId) {
        return bidRepository.countByAuctionId(auctionId);
    }


    @PreDestroy
    public void shutdown() {
        bidExecutorService.shutdown();
        notificationExecutorService.shutdown();
        try {
            if (!bidExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
                bidExecutorService.shutdownNow();
            }
            if (!notificationExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
                notificationExecutorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            bidExecutorService.shutdownNow();
            notificationExecutorService.shutdownNow();
        }
    }


    /**
     * Send outbid notification email (called asynchronously)
     * Uses primitive parameters to avoid lazy loading issues in async context
     */
    private void sendOutbidNotification(String bidderEmail, String bidderName, String itemName,
                                        Long auctionId, Float previousBid, Float newBidAmount,
                                        Float minIncrement, LocalDateTime endsAt) {
        Float minNextBid = newBidAmount + minIncrement;
        String auctionLink = "http://localhost:8080/auctions/" + auctionId;

        Map<String, Object> model = Map.of(
                "bidderName", bidderName,
                "itemName", itemName,
                "previousBid", previousBid,
                "newBid", newBidAmount,
                "minNextBid", minNextBid,
                "auctionLink", auctionLink,
                "auctionEndsAt", endsAt,
                "unsubscribeLink", "http://localhost:8080/unsubscribe?email=" + bidderEmail
        );

        emailService.sendEmail(
                bidderEmail,
                "🔔 You've Been Outbid on " + itemName,
                "email/bid-outbid",
                model
        );

        log.info("📧 Outbid notification sent to {}", bidderEmail);
    }


    /**
     * Helper: Get display name from UserProfile, fallback to email
     */
    private String getDisplayName(User user) {
        try {
            UserProfile profile = user.getUserProfile();
            if (profile != null && profile.getFullName() != null && !profile.getFullName().isEmpty()) {
                return profile.getFullName();
            }
        } catch (org.hibernate.LazyInitializationException e) {
            log.debug("UserProfile not loaded for user {}, using email as fallback", user.getId());
        }
        return user.getEmail() != null ? user.getEmail() : "Bidder";
    }


}
