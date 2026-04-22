package com.ga.warehouse.controllers;

import com.ga.warehouse.models.BID;
import com.ga.warehouse.response.SuccessResponse;
import com.ga.warehouse.services.BIDService;
import com.ga.warehouse.services.UserService;
import com.ga.warehouse.utils.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

@Slf4j
@RestController
@RequestMapping("/api/v1/auctions/{auctionId}/bids")
@RequiredArgsConstructor
public class BIDController {

    private final BIDService bidService;
    private final UserService userService;

    /**
     * Place a bid on an auction using thread pool for concurrent processing
     * POST /api/v1/auctions/{auctionId}/bids
     * Body: { "amount": 150.0 }
     */
    @PostMapping
    @PreAuthorize("hasAuthority('bid:create')")
    public ResponseEntity<SuccessResponse> placeBid(
            @PathVariable Long auctionId,
            @RequestBody Float bidAmount,
            @AuthenticationPrincipal UserDetails userDetails) throws Exception {

        // 1. Get authenticated user ID
        Long bidderId = userService.findUserByEmailAddress(userDetails.getUsername()).getId();

        // 2. Submit bid to thread pool for concurrent processing
        log.info("📥 Bid request: auction={}, amount={}, user={}", auctionId, bidAmount, bidderId);

        Future<BID> bidFuture = bidService.placeBidAsync(auctionId, bidAmount, bidderId);

        // 3. Wait for result from thread pool (demonstrates thread usage while maintaining response)
        BID savedBid = bidFuture.get();

        log.info("📤 Bid response: id={}, processed by thread pool", savedBid.getId());

        return ResponseBuilder.success(
                HttpStatus.CREATED,
                "Bid placed successfully",
                savedBid
        );
    }

    /**
     * Get all bids for an auction
     * GET /api/v1/auctions/{auctionId}/bids
     */
    @GetMapping
    @PreAuthorize("hasAuthority('bid:read')")
    public ResponseEntity<SuccessResponse> getBids(@PathVariable Long auctionId) {
        List<BID> bids = bidService.getBidsForAuction(auctionId);
        return ResponseBuilder.success(HttpStatus.OK, "Bids retrieved", bids);
    }

    /**
     * Get current highest bid
     * GET /api/v1/auctions/{auctionId}/bids/highest
     */
    @GetMapping("/highest")
    @PreAuthorize("hasAuthority('bid:read')")
    public ResponseEntity<SuccessResponse> getHighestBid(@PathVariable Long auctionId) {
        Optional<BID> highest = bidService.getHighestBid(auctionId);
        return highest
                .map(bid -> ResponseBuilder.success(HttpStatus.OK, "Highest bid retrieved", bid))
                .orElse(ResponseBuilder.success(HttpStatus.OK, "No bids yet", null));
    }

    /**
     * Get bid count
     * GET /api/v1/auctions/{auctionId}/bids/count
     */
    @GetMapping("/count")
    @PreAuthorize("hasAuthority('bid:read')")
    public ResponseEntity<SuccessResponse> getBidCount(@PathVariable Long auctionId) {
        long count = bidService.getBidCount(auctionId);
        return ResponseBuilder.success(HttpStatus.OK, "Bid count retrieved", count);
    }
}