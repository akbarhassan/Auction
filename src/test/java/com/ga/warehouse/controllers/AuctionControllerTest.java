package com.ga.warehouse.controllers;


import com.ga.warehouse.enums.AuctionStatus;
import com.ga.warehouse.models.Auction;
import com.ga.warehouse.models.AuctionItem;
import com.ga.warehouse.models.User;
import com.ga.warehouse.response.SuccessResponse;
import com.ga.warehouse.security.MyUserDetails;
import com.ga.warehouse.services.AuctionService;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class AuctionControllerTest {

    @Mock
    private AuctionService auctionService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuctionController auctionController;

    // ============ TEST DATA ============
    private User creator;
    private MyUserDetails userDetails;
    private Auction auction1;
    private Auction auction2;
    private AuctionItem item;

    // ============ SETUP ============
    @BeforeEach
    void setUp() {
        creator = new User();
        creator.setId(1L);
        creator.setEmail("creator@test.com");

        userDetails = new MyUserDetails(creator);

        item = new AuctionItem();
        item.setId(1L);
        item.setName("Test Item");

        auction1 = new Auction();
        auction1.setId(1L);
        auction1.setStartPrice(100.0f);
        auction1.setCurrentHighestBid(100.0f);
        auction1.setStatus(AuctionStatus.ACTIVE);
        auction1.setStartsAt(LocalDateTime.now().minusDays(1));
        auction1.setEndsAt(LocalDateTime.now().plusDays(1));
        auction1.setAuctionItem(item);

        auction2 = new Auction();
        auction2.setId(2L);
        auction2.setStartPrice(200.0f);
        auction2.setStatus(AuctionStatus.PENDING);
        auction2.setStartsAt(LocalDateTime.now().plusDays(1));
        auction2.setEndsAt(LocalDateTime.now().plusDays(2));
    }

    // ============ TEST: createAuction ============
    @Test
    @DisplayName("createAuction: valid auction → returns CREATED with auction data")
    void createAuction_validAuction_returnsCreated() {
        Auction newAuction = new Auction();
        newAuction.setStartPrice(150.0f);
        newAuction.setStartsAt(LocalDateTime.now().plusHours(1));
        newAuction.setEndsAt(LocalDateTime.now().plusDays(1));
        newAuction.setAuctionItem(item);

        when(userService.findUserByEmailAddress("creator@test.com")).thenReturn(creator);
        when(auctionService.createAuction(newAuction, 1L)).thenReturn(auction1);

        ResponseEntity<SuccessResponse> result = auctionController.createAuction(newAuction, userDetails);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().message()).isEqualTo("Auction created successfully");
        assertThat(result.getBody().data()).isEqualTo(auction1);

        verify(userService, times(1)).findUserByEmailAddress("creator@test.com");
        verify(auctionService, times(1)).createAuction(newAuction, 1L);
    }

    // ============ TEST: getAllAuctions ============
    @Test
    @DisplayName("getAllAuctions: returns list of auctions")
    void getAllAuctions_returnsListOfAuctions() {
        List<Auction> auctions = Arrays.asList(auction1, auction2);
        when(auctionService.getAllAuctions()).thenReturn(auctions);

        ResponseEntity<SuccessResponse> result = auctionController.getAllAuctions();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().message()).isEqualTo("All auctions retrieved successfully");
        assertThat(result.getBody().data()).isEqualTo(auctions);

        verify(auctionService, times(1)).getAllAuctions();
    }

    // ============ TEST: getAuction ============
    @Test
    @DisplayName("getAuction: returns auction by ID")
    void getAuction_returnsAuctionById() {
        when(auctionService.getAuctionById(1L)).thenReturn(auction1);

        ResponseEntity<SuccessResponse> result = auctionController.getAuction(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().message()).isEqualTo("Auction retrieved successfully");
        assertThat(result.getBody().data()).isEqualTo(auction1);

        verify(auctionService, times(1)).getAuctionById(1L);
    }

    // ============ TEST: updateAuction ============
    @Test
    @DisplayName("updateAuction: updates and returns auction")
    void updateAuction_updatesAndReturnsAuction() {
        Auction updateRequest = new Auction();
        updateRequest.setStatus(AuctionStatus.CANCELLED);

        Auction updatedAuction = new Auction();
        updatedAuction.setId(1L);
        updatedAuction.setStatus(AuctionStatus.CANCELLED);

        when(auctionService.updateAuction(1L, updateRequest)).thenReturn(updatedAuction);

        ResponseEntity<SuccessResponse> result = auctionController.updateAuction(1L, updateRequest);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().message()).isEqualTo("Auction updated successfully");
        assertThat(result.getBody().data()).isEqualTo(updatedAuction);

        verify(auctionService, times(1)).updateAuction(1L, updateRequest);
    }

    // ============ TEST: deleteAuction ============
    @Test
    @DisplayName("deleteAuction: deletes auction and returns success")
    void deleteAuction_deletesAuction() {
        doNothing().when(auctionService).deleteAuctionById(1L);

        ResponseEntity<SuccessResponse> result = auctionController.deleteAuction(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().message()).isEqualTo("Auction deleted successfully");
        assertThat(result.getBody().data()).isNull();

        verify(auctionService, times(1)).deleteAuctionById(1L);
    }
}
