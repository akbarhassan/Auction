package com.ga.warehouse.services;


import com.ga.warehouse.enums.AuctionStatus;
import com.ga.warehouse.enums.WareHouseItemsCondition;
import com.ga.warehouse.exceptions.ResourceNotFoundException;
import com.ga.warehouse.exceptions.ValidationException;
import com.ga.warehouse.models.Auction;
import com.ga.warehouse.models.AuctionItem;
import com.ga.warehouse.models.BID;
import com.ga.warehouse.models.User;
import com.ga.warehouse.repositories.AuctionRepository;
import com.ga.warehouse.repositories.BIDRepository;
import com.ga.warehouse.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class BIDServiceTest {

    @Mock
    private BIDRepository bidRepository;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private BIDService bidService;

    // ============ TEST DATA ============
    private Auction activeAuction;
    private Auction endedAuction;
    private User bidder1;
    private User bidder2;
    private User auctionCreator;
    private BID existingBid;

    // ============ SETUP ============
    @BeforeEach
    void setUp() {
        // Create thread pools for testing
        ExecutorService bidExecutor = Executors.newFixedThreadPool(2);
        ExecutorService notificationExecutor = Executors.newFixedThreadPool(2);
        ReflectionTestUtils.setField(bidService, "bidExecutorService", bidExecutor);
        ReflectionTestUtils.setField(bidService, "notificationExecutorService", notificationExecutor);

        auctionCreator = new User();
        auctionCreator.setId(1L);
        auctionCreator.setEmail("creator@test.com");

        bidder1 = new User();
        bidder1.setId(2L);
        bidder1.setEmail("bidder1@test.com");

        bidder2 = new User();
        bidder2.setId(3L);
        bidder2.setEmail("bidder2@test.com");

        AuctionItem item = new AuctionItem();
        item.setId(1L);
        item.setName("Test Item");

        activeAuction = new Auction();
        activeAuction.setId(1L);
        activeAuction.setStatus(AuctionStatus.ACTIVE);
        activeAuction.setStartPrice(100.0f);
        activeAuction.setCurrentHighestBid(100.0f);
        activeAuction.setMinimumIncrement(10.0f);
        activeAuction.setStartsAt(LocalDateTime.now().minusDays(1));
        activeAuction.setEndsAt(LocalDateTime.now().plusDays(1));
        activeAuction.setAuctionItem(item);
        activeAuction.setAuctionedBy(auctionCreator);

        endedAuction = new Auction();
        endedAuction.setId(2L);
        endedAuction.setStatus(AuctionStatus.ENDED);
        endedAuction.setEndsAt(LocalDateTime.now().minusDays(1));
        endedAuction.setAuctionedBy(auctionCreator);

        existingBid = new BID();
        existingBid.setId(1L);
        existingBid.setAmount(100.0f);
        existingBid.setBidder(bidder1);
        existingBid.setAuction(activeAuction);
    }

    // ============ TEST: getBidsForAuction ============
    @Test
    @DisplayName("getBidsForAuction: returns list of bids ordered by amount desc")
    void getBidsForAuction_returnsBidsOrderedByAmount() {
        BID bid1 = new BID();
        bid1.setId(1L);
        bid1.setAmount(150.0f);

        BID bid2 = new BID();
        bid2.setId(2L);
        bid2.setAmount(200.0f);

        when(bidRepository.findByAuctionIdOrderByAmountDesc(1L)).thenReturn(Arrays.asList(bid2, bid1));

        List<BID> result = bidService.getBidsForAuction(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAmount()).isEqualTo(200.0f);
        assertThat(result.get(1).getAmount()).isEqualTo(150.0f);

        verify(bidRepository, times(1)).findByAuctionIdOrderByAmountDesc(1L);
    }

    // ============ TEST: getHighestBid ============
    @Test
    @DisplayName("getHighestBid: returns highest bid")
    void getHighestBid_returnsHighestBid() {
        when(bidRepository.findFirstByAuctionIdOrderByAmountDesc(1L)).thenReturn(Optional.of(existingBid));

        Optional<BID> result = bidService.getHighestBid(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getAmount()).isEqualTo(100.0f);

        verify(bidRepository, times(1)).findFirstByAuctionIdOrderByAmountDesc(1L);
    }

    // ============ TEST: getBidCount ============
    @Test
    @DisplayName("getBidCount: returns count of bids")
    void getBidCount_returnsCount() {
        when(bidRepository.countByAuctionId(1L)).thenReturn(5L);

        long result = bidService.getBidCount(1L);

        assertThat(result).isEqualTo(5L);

        verify(bidRepository, times(1)).countByAuctionId(1L);
    }

    // ============ TEST: processBid - Success Cases ============
    @Test
    @DisplayName("processBid: valid bid on active auction → saves bid and updates auction")
    void processBid_validBid_savesBidAndUpdatesAuction() {
        when(auctionRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeAuction));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bidder1));
        when(bidRepository.findFirstByAuctionIdOrderByAmountDesc(1L)).thenReturn(Optional.empty());
        when(bidRepository.save(any(BID.class))).thenAnswer(i -> {
            BID saved = i.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));

        BID result = bidService.processBid(1L, 120.0f, 2L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getAmount()).isEqualTo(120.0f);
        assertThat(result.getBidder()).isEqualTo(bidder1);
        assertThat(result.getAuction()).isEqualTo(activeAuction);
        assertThat(activeAuction.getCurrentHighestBid()).isEqualTo(120.0f);

        verify(auctionRepository, times(1)).findByIdWithLock(1L);
        verify(userRepository, times(1)).findById(2L);
        verify(bidRepository, times(1)).save(any(BID.class));
        verify(auctionRepository, times(1)).save(activeAuction);
    }

    @Test
    @DisplayName("processBid: outbid previous bidder → saves bid and notifies previous bidder")
    void processBid_outbidPreviousBidder_savesAndNotifies() {
        when(auctionRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeAuction));
        when(userRepository.findById(3L)).thenReturn(Optional.of(bidder2));
        when(bidRepository.findFirstByAuctionIdOrderByAmountDesc(1L)).thenReturn(Optional.of(existingBid));
        when(bidRepository.save(any(BID.class))).thenAnswer(i -> {
            BID saved = i.getArgument(0);
            saved.setId(3L);
            return saved;
        });
        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));
        doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString(), any());

        BID result = bidService.processBid(1L, 150.0f, 3L);

        assertThat(result.getAmount()).isEqualTo(150.0f);
        assertThat(activeAuction.getCurrentHighestBid()).isEqualTo(150.0f);

        verify(bidRepository, times(1)).save(any(BID.class));
        verify(auctionRepository, times(1)).save(activeAuction);
        verify(emailService, timeout(1000)).sendEmail(anyString(), anyString(), anyString(), any());
    }

    // ============ TEST: processBid - Validation Failures ============
    @Test
    @DisplayName("processBid: auction not found → throws ResourceNotFoundException")
    void processBid_auctionNotFound_throwsResourceNotFoundException() {
        when(auctionRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bidService.processBid(999L, 120.0f, 2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Auction not found with id: 999");

        verify(bidRepository, never()).save(any(BID.class));
    }

    @Test
    @DisplayName("processBid: auction not active → throws ValidationException")
    void processBid_auctionNotActive_throwsValidationException() {
        when(auctionRepository.findByIdWithLock(2L)).thenReturn(Optional.of(endedAuction));

        assertThatThrownBy(() -> bidService.processBid(2L, 120.0f, 2L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Auction is not active");

        verify(bidRepository, never()).save(any(BID.class));
    }

    @Test
    @DisplayName("processBid: auction ended → throws ValidationException")
    void processBid_auctionEnded_throwsValidationException() {
        activeAuction.setEndsAt(LocalDateTime.now().minusHours(1));
        when(auctionRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeAuction));

        assertThatThrownBy(() -> bidService.processBid(1L, 120.0f, 2L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Auction has ended");

        verify(bidRepository, never()).save(any(BID.class));
    }

    @Test
    @DisplayName("processBid: bid below minimum → throws ValidationException")
    void processBid_bidBelowMinimum_throwsValidationException() {
        when(auctionRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeAuction));

        // Minimum bid should be 100 + 10 = 110
        assertThatThrownBy(() -> bidService.processBid(1L, 105.0f, 2L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Bid must be at least 110.00");

        verify(bidRepository, never()).save(any(BID.class));
    }

    @Test
    @DisplayName("processBid: bidder not found → throws ResourceNotFoundException")
    void processBid_bidderNotFound_throwsResourceNotFoundException() {
        when(auctionRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeAuction));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bidService.processBid(1L, 120.0f, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Bidder not found");

        verify(bidRepository, never()).save(any(BID.class));
    }

    @Test
    @DisplayName("processBid: creator bidding on own auction → throws ValidationException")
    void processBid_creatorBiddingOnOwnAuction_throwsValidationException() {
        when(auctionRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeAuction));
        when(userRepository.findById(1L)).thenReturn(Optional.of(auctionCreator));

        assertThatThrownBy(() -> bidService.processBid(1L, 120.0f, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("You cannot bid on your own auction");

        verify(bidRepository, never()).save(any(BID.class));
    }

    // ============ TEST: Concurrency & Race Conditions ============
    @Test
    @DisplayName("processBid: concurrent bids → only highest bid wins (simulated)")
    void processBid_concurrentBids_onlyHighestWins() throws InterruptedException {
        // This test simulates what happens with database locking
        AtomicInteger saveCount = new AtomicInteger(0);

        when(auctionRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeAuction));
        when(userRepository.findById(anyLong())).thenAnswer(i -> {
            Long id = i.getArgument(0);
            if (id == 2L) return Optional.of(bidder1);
            if (id == 3L) return Optional.of(bidder2);
            return Optional.empty();
        });
        when(bidRepository.findFirstByAuctionIdOrderByAmountDesc(1L))
            .thenReturn(Optional.empty())  // First call
            .thenReturn(Optional.of(existingBid));  // Subsequent calls see the previous bid
        when(bidRepository.save(any(BID.class))).thenAnswer(i -> {
            BID saved = i.getArgument(0);
            saved.setId(saveCount.incrementAndGet() + 1L);
            return saved;
        });
        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));

        // Simulate two concurrent bids
        CountDownLatch latch = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<BID> bid1 = executor.submit(() -> {
            try {
                return bidService.processBid(1L, 120.0f, 2L);
            } finally {
                latch.countDown();
            }
        });

        Future<BID> bid2 = executor.submit(() -> {
            try {
                return bidService.processBid(1L, 130.0f, 3L);
            } finally {
                latch.countDown();
            }
        });

        latch.await(5, TimeUnit.SECONDS);

        // Both bids should be processed (database locking prevents conflicts)
        assertThat(saveCount.get()).isEqualTo(2);

        executor.shutdown();
    }

    @Test
    @DisplayName("processBid: sequential higher bids → each updates highest bid")
    void processBid_sequentialHigherBids_eachUpdatesHighest() {
        when(auctionRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeAuction));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bidder1));
        when(userRepository.findById(3L)).thenReturn(Optional.of(bidder2));
        when(bidRepository.findFirstByAuctionIdOrderByAmountDesc(1L))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(existingBid));
        when(bidRepository.save(any(BID.class))).thenAnswer(i -> {
            BID saved = i.getArgument(0);
            saved.setId(System.currentTimeMillis());
            return saved;
        });
        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));

        // First bid
        BID firstBid = bidService.processBid(1L, 120.0f, 2L);
        assertThat(firstBid.getAmount()).isEqualTo(120.0f);
        assertThat(activeAuction.getCurrentHighestBid()).isEqualTo(120.0f);

        // Second higher bid
        BID secondBid = bidService.processBid(1L, 150.0f, 3L);
        assertThat(secondBid.getAmount()).isEqualTo(150.0f);
        assertThat(activeAuction.getCurrentHighestBid()).isEqualTo(150.0f);

        verify(bidRepository, times(2)).save(any(BID.class));
        verify(auctionRepository, times(2)).save(activeAuction);
    }

    // ============ TEST: placeBidAsync ============
    @Test
    @DisplayName("placeBidAsync: submits bid to thread pool → returns Future")
    void placeBidAsync_submitsToThreadPool_returnsFuture() throws Exception {
        // Setup a real executor for async testing
        ExecutorService realExecutor = Executors.newSingleThreadExecutor();
        ReflectionTestUtils.setField(bidService, "bidExecutorService", realExecutor);

        when(auctionRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeAuction));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bidder1));
        when(bidRepository.findFirstByAuctionIdOrderByAmountDesc(1L)).thenReturn(Optional.empty());
        when(bidRepository.save(any(BID.class))).thenAnswer(i -> {
            BID saved = i.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));

        Future<BID> future = bidService.placeBidAsync(1L, 120.0f, 2L);

        assertThat(future).isNotNull();
        BID result = future.get(5, TimeUnit.SECONDS);
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualTo(120.0f);

        realExecutor.shutdown();
    }

    @Test
    @DisplayName("placeBidAsync: exception in thread → Future captures exception")
    void placeBidAsync_exceptionInThread_futureCapturesException() {
        ExecutorService realExecutor = Executors.newSingleThreadExecutor();
        ReflectionTestUtils.setField(bidService, "bidExecutorService", realExecutor);

        when(auctionRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

        Future<BID> future = bidService.placeBidAsync(999L, 120.0f, 2L);

        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
                .hasCauseInstanceOf(ResourceNotFoundException.class);

        realExecutor.shutdown();
    }

    // ============ TEST: Edge Cases ============
    @Test
    @DisplayName("processBid: bid exactly at minimum increment → accepted")
    void processBid_exactlyAtMinimumIncrement_accepted() {
        when(auctionRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeAuction));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bidder1));
        when(bidRepository.findFirstByAuctionIdOrderByAmountDesc(1L)).thenReturn(Optional.empty());
        when(bidRepository.save(any(BID.class))).thenAnswer(i -> {
            BID saved = i.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));

        // Minimum is 100 + 10 = 110, so 110 should be accepted
        BID result = bidService.processBid(1L, 110.0f, 2L);

        assertThat(result.getAmount()).isEqualTo(110.0f);
        verify(bidRepository, times(1)).save(any(BID.class));
    }

    @Test
    @DisplayName("processBid: first bid on auction → sets as highest")
    void processBid_firstBid_setsAsHighest() {
        when(auctionRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeAuction));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bidder1));
        when(bidRepository.findFirstByAuctionIdOrderByAmountDesc(1L)).thenReturn(Optional.empty());
        when(bidRepository.save(any(BID.class))).thenAnswer(i -> {
            BID saved = i.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));

        BID result = bidService.processBid(1L, 120.0f, 2L);

        assertThat(result.getAmount()).isEqualTo(120.0f);
        assertThat(activeAuction.getCurrentHighestBid()).isEqualTo(120.0f);
        // No notification sent since there's no previous bidder
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString(), any());
    }
}
