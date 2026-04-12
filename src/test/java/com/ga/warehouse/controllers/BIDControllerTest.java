package com.ga.warehouse.controllers;


import com.ga.warehouse.models.BID;
import com.ga.warehouse.models.User;
import com.ga.warehouse.response.SuccessResponse;
import com.ga.warehouse.security.MyUserDetails;
import com.ga.warehouse.services.BIDService;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class BIDControllerTest {

    @Mock
    private BIDService bidService;

    @Mock
    private UserService userService;

    @InjectMocks
    private BIDController bidController;

    // ============ TEST DATA ============
    private User bidder;
    private MyUserDetails userDetails;
    private BID bid1;
    private BID bid2;

    // ============ SETUP ============
    @BeforeEach
    void setUp() {
        bidder = new User();
        bidder.setId(1L);
        bidder.setEmail("bidder@test.com");

        userDetails = new MyUserDetails(bidder);

        bid1 = new BID();
        bid1.setId(1L);
        bid1.setAmount(100.0f);

        bid2 = new BID();
        bid2.setId(2L);
        bid2.setAmount(150.0f);
    }

    // ============ TEST: placeBid ============
    @Test
    @DisplayName("placeBid: valid bid → returns CREATED with bid data")
    void placeBid_validBid_returnsCreated() {
        when(userService.findUserByEmailAddress("bidder@test.com")).thenReturn(bidder);
        when(bidService.processBid(1L, 120.0f, 1L)).thenReturn(bid1);

        ResponseEntity<SuccessResponse> result = bidController.placeBid(1L, 120.0f, userDetails);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().message()).isEqualTo("Bid placed successfully");
        assertThat(result.getBody().data()).isEqualTo(bid1);

        verify(userService, times(1)).findUserByEmailAddress("bidder@test.com");
        verify(bidService, times(1)).processBid(1L, 120.0f, 1L);
    }

    // ============ TEST: getBids ============
    @Test
    @DisplayName("getBids: returns list of bids")
    void getBids_returnsListOfBids() {
        List<BID> bids = Arrays.asList(bid1, bid2);
        when(bidService.getBidsForAuction(1L)).thenReturn(bids);

        ResponseEntity<SuccessResponse> result = bidController.getBids(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().message()).isEqualTo("Bids retrieved");
        assertThat(result.getBody().data()).isEqualTo(bids);

        verify(bidService, times(1)).getBidsForAuction(1L);
    }

    // ============ TEST: getHighestBid ============
    @Test
    @DisplayName("getHighestBid: bid exists → returns OK with bid")
    void getHighestBid_bidExists_returnsOkWithBid() {
        when(bidService.getHighestBid(1L)).thenReturn(Optional.of(bid2));

        ResponseEntity<SuccessResponse> result = bidController.getHighestBid(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().message()).isEqualTo("Highest bid retrieved");
        assertThat(result.getBody().data()).isEqualTo(bid2);

        verify(bidService, times(1)).getHighestBid(1L);
    }

    @Test
    @DisplayName("getHighestBid: no bids → returns OK with null")
    void getHighestBid_noBids_returnsOkWithNull() {
        when(bidService.getHighestBid(1L)).thenReturn(Optional.empty());

        ResponseEntity<SuccessResponse> result = bidController.getHighestBid(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().message()).isEqualTo("No bids yet");
        assertThat(result.getBody().data()).isNull();

        verify(bidService, times(1)).getHighestBid(1L);
    }

    // ============ TEST: getBidCount ============
    @Test
    @DisplayName("getBidCount: returns count")
    void getBidCount_returnsCount() {
        when(bidService.getBidCount(1L)).thenReturn(5L);

        ResponseEntity<SuccessResponse> result = bidController.getBidCount(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().message()).isEqualTo("Bid count retrieved");
        assertThat(result.getBody().data()).isEqualTo(5L);

        verify(bidService, times(1)).getBidCount(1L);
    }
}
