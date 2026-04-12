package com.ga.warehouse.controllers;


import com.ga.warehouse.models.Auction;
import com.ga.warehouse.response.SuccessResponse;
import com.ga.warehouse.services.AuctionService;
import com.ga.warehouse.utils.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;


    /**
     *
     * @param auction
     * @return
     */
    @PostMapping
    @PreAuthorize("hasAuthority('auction:create')")
    public ResponseEntity<SuccessResponse> createAuction(@RequestBody Auction auction) {
        Auction createdAuction = auctionService.createAuction(auction);
        return ResponseBuilder.success(HttpStatus.CREATED, "Auction created successfully", createdAuction);
    }


    /**
     *
     * @return
     */
    @GetMapping
    @PreAuthorize("hasAuthority('auction:read')")
    public ResponseEntity<SuccessResponse> getAllAuctions() {
        List<Auction> auctions = auctionService.getAllAuctions();
        return ResponseBuilder.success(HttpStatus.OK, "All auctions retrieved successfully", auctions);
    }

    /**
     *
     * @param id
     * @return
     */
    @GetMapping("{id}")
    @PreAuthorize("hasAuthority('auction:read')")
    public ResponseEntity<SuccessResponse> getAuction(@PathVariable Long id) {
        Auction auction = auctionService.getAuctionById(id);
        return ResponseBuilder.success(HttpStatus.OK, "Auction retrieved successfully", auction);
    }


    /**
     *
     * @param id
     * @param auction
     * @return
     */
    @PutMapping("{id}")
    @PreAuthorize("hasAuthority('auction:update')")
    public ResponseEntity<SuccessResponse> updateAuction(@PathVariable Long id, @RequestBody Auction auction) {
        Auction updatedAuction = auctionService.updateAuction(id, auction);
        return ResponseBuilder.success(HttpStatus.OK, "Auction updated successfully", updatedAuction);
    }

    /**
     *
     * @param id
     * @return
     */
    @DeleteMapping("{id}")
    @PreAuthorize("hasAuthority('auction:delete')")
    public ResponseEntity<SuccessResponse> deleteAuction(@PathVariable Long id) {
        auctionService.deleteAuctionById(id);
        return ResponseBuilder.success(HttpStatus.OK, "Auction deleted successfully", null);
    }


}
