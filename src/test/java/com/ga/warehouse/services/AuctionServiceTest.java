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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class AuctionServiceTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private AuctionItemRepository auctionItemRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuctionService auctionService;

    // ============ TEST DATA ============
    private Auction auction1;
    private Auction auction2;
    private AuctionItem auctionItem1;
    private User user1;
    private LocalDateTime now;

    // ============ SETUP ============
    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        user1 = new User();
        user1.setId(1L);
        user1.setEmail("user@test.com");

        auctionItem1 = new AuctionItem();
        auctionItem1.setId(1L);
        auctionItem1.setName("Test Item");

        auction1 = new Auction();
        auction1.setId(1L);
        auction1.setStartPrice(100.0f);
        auction1.setStartsAt(now.plusDays(1));
        auction1.setEndsAt(now.plusDays(2));
        auction1.setStatus(AuctionStatus.PENDING);
        auction1.setAuctionItem(auctionItem1);
        auction1.setAuctionedBy(user1);
        auction1.setMinimumIncrement(5.0f);

        auction2 = new Auction();
        auction2.setId(2L);
        auction2.setStartPrice(200.0f);
        auction2.setStartsAt(now.plusHours(1));
        auction2.setEndsAt(now.plusDays(1));
        auction2.setStatus(AuctionStatus.ACTIVE);
        auction2.setCurrentHighestBid(200.0f);
    }

    // ============ TEST: getAllAuctions ============
    @Test
    @DisplayName("getAllAuctions: returns list of all auctions")
    void getAllAuctions_returnsAllAuctions() {
        List<Auction> expectedAuctions = Arrays.asList(auction1, auction2);

        when(auctionRepository.findAll()).thenReturn(expectedAuctions);

        List<Auction> result = auctionService.getAllAuctions();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(auction1, auction2);

        verify(auctionRepository, times(1)).findAll();
        verifyNoMoreInteractions(auctionRepository);
    }

    // ============ TEST: getAuctionById ============
    @Test
    @DisplayName("getAuctionById: returns auction based on ID")
    void getAuctionById_returnsAuction() {
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction1));

        Auction result = auctionService.getAuctionById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStartPrice()).isEqualTo(100.0f);

        verify(auctionRepository, times(1)).findById(1L);
        verifyNoMoreInteractions(auctionRepository);
    }

    @Test
    @DisplayName("getAuctionById: non-existing ID → throws ResourceNotFoundException")
    void getAuctionById_nonExistingId_throwsResourceNotFoundException() {
        when(auctionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auctionService.getAuctionById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Auction with id: 999 not found");

        verify(auctionRepository, times(1)).findById(999L);
        verifyNoMoreInteractions(auctionRepository);
    }

    // ============ TEST: createAuction ============
    @Test
    @DisplayName("createAuction: valid auction → saves and returns auction")
    void createAuction_validAuction_savesAndReturnsAuction() {
        Auction newAuction = new Auction();
        newAuction.setStartPrice(150.0f);
        newAuction.setStartsAt(now.plusDays(1));
        newAuction.setEndsAt(now.plusDays(2));
        newAuction.setAuctionItem(auctionItem1);
        newAuction.setAuctionedBy(user1);

        when(auctionItemRepository.findById(1L)).thenReturn(Optional.of(auctionItem1));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(auctionRepository.findByAuctionItemId(1L)).thenReturn(Optional.empty());
        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> {
            Auction saved = i.getArgument(0);
            saved.setId(3L);
            return saved;
        });

        Auction result = auctionService.createAuction(newAuction, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getStartPrice()).isEqualTo(150.0f);
        assertThat(result.getStatus()).isEqualTo(AuctionStatus.PENDING);
        assertThat(result.getCurrentHighestBid()).isEqualTo(150.0f);

        verify(auctionRepository, times(1)).findByAuctionItemId(1L);
        verify(auctionItemRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findById(1L);
        verify(auctionRepository, times(1)).save(any(Auction.class));
    }

    @Test
    @DisplayName("createAuction: start price <= 0 → throws ValidationException")
    void createAuction_invalidStartPrice_throwsValidationException() {
        Auction newAuction = new Auction();
        newAuction.setStartPrice(0.0f);
        newAuction.setStartsAt(now.plusDays(1));
        newAuction.setEndsAt(now.plusDays(2));
        newAuction.setAuctionItem(auctionItem1);

        assertThatThrownBy(() -> auctionService.createAuction(newAuction, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Start price must be greater than 0");
    }

    @Test
    @DisplayName("createAuction: end date before start date → throws ValidationException")
    void createAuction_invalidDates_throwsValidationException() {
        Auction newAuction = new Auction();
        newAuction.setStartPrice(100.0f);
        newAuction.setStartsAt(now.plusDays(2));
        newAuction.setEndsAt(now.plusDays(1));
        newAuction.setAuctionItem(auctionItem1);

        assertThatThrownBy(() -> auctionService.createAuction(newAuction, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("End date must be after start date");
    }

    @Test
    @DisplayName("createAuction: missing auction item → throws ValidationException")
    void createAuction_missingAuctionItem_throwsValidationException() {
        Auction newAuction = new Auction();
        newAuction.setStartPrice(100.0f);
        newAuction.setStartsAt(now.plusDays(1));
        newAuction.setEndsAt(now.plusDays(2));

        assertThatThrownBy(() -> auctionService.createAuction(newAuction, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Auction item is required");
    }

    @Test
    @DisplayName("createAuction: auction already exists for item → throws ResourceAlreadyExistsException")
    void createAuction_duplicateAuction_throwsResourceAlreadyExistsException() {
        Auction newAuction = new Auction();
        newAuction.setStartPrice(100.0f);
        newAuction.setStartsAt(now.plusDays(1));
        newAuction.setEndsAt(now.plusDays(2));
        newAuction.setAuctionItem(auctionItem1);
        newAuction.setAuctionedBy(user1);

        when(auctionRepository.findByAuctionItemId(1L)).thenReturn(Optional.of(auction1));

        assertThatThrownBy(() -> auctionService.createAuction(newAuction, 1L))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("Auction already exists for this item");

        verify(auctionRepository, times(1)).findByAuctionItemId(1L);
        verifyNoMoreInteractions(auctionRepository);
    }

    // ============ TEST: updateAuction ============
    @Test
    @DisplayName("updateAuction: cancel pending auction → updates status to CANCELLED")
    void updateAuction_cancelPendingAuction_updatesStatus() {
        Auction updateRequest = new Auction();
        updateRequest.setStatus(AuctionStatus.CANCELLED);

        when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction1));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));

        Auction result = auctionService.updateAuction(1L, updateRequest);

        assertThat(result.getStatus()).isEqualTo(AuctionStatus.CANCELLED);

        verify(auctionRepository, times(1)).findById(1L);
        verify(auctionRepository, times(1)).save(auction1);
    }

    @Test
    @DisplayName("updateAuction: update endsAt for pending auction → updates successfully")
    void updateAuction_updateEndsAtForPending_updatesSuccessfully() {
        LocalDateTime newEndDate = now.plusDays(3);
        Auction updateRequest = new Auction();
        updateRequest.setEndsAt(newEndDate);

        when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction1));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));

        Auction result = auctionService.updateAuction(1L, updateRequest);

        assertThat(result.getEndsAt()).isEqualTo(newEndDate);

        verify(auctionRepository, times(1)).findById(1L);
        verify(auctionRepository, times(1)).save(auction1);
    }

    @Test
    @DisplayName("updateAuction: change start price → throws IllegalStateException")
    void updateAuction_changeStartPrice_throwsIllegalStateException() {
        Auction updateRequest = new Auction();
        updateRequest.setStartPrice(500.0f);

        when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction1));

        assertThatThrownBy(() -> auctionService.updateAuction(1L, updateRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Start price cannot be changed after auction creation");

        verify(auctionRepository, times(1)).findById(1L);
        verify(auctionRepository, never()).save(any(Auction.class));
    }

    @Test
    @DisplayName("updateAuction: non-existing ID → throws ResourceNotFoundException")
    void updateAuction_nonExistingId_throwsResourceNotFoundException() {
        Auction updateRequest = new Auction();
        updateRequest.setStatus(AuctionStatus.CANCELLED);

        when(auctionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auctionService.updateAuction(999L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Auction not found");

        verify(auctionRepository, times(1)).findById(999L);
        verify(auctionRepository, never()).save(any(Auction.class));
    }

    // ============ TEST: deleteAuctionById ============
    @Test
    @DisplayName("deleteAuctionById: existing ID → deletes auction")
    void deleteAuctionById_existingId_deletesAuction() {
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction1));
        doNothing().when(auctionRepository).delete(auction1);

        auctionService.deleteAuctionById(1L);

        verify(auctionRepository, times(1)).findById(1L);
        verify(auctionRepository, times(1)).delete(auction1);
        verifyNoMoreInteractions(auctionRepository);
    }

    @Test
    @DisplayName("deleteAuctionById: missing ID → throws ResourceNotFoundException")
    void deleteAuctionById_missingId_throwsResourceNotFoundException() {
        when(auctionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auctionService.deleteAuctionById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Auction with id: 999 not found");

        verify(auctionRepository, times(1)).findById(999L);
        verify(auctionRepository, never()).delete(any(Auction.class));
        verifyNoMoreInteractions(auctionRepository);
    }

    // ============ TEST: updateAuctionStatuses ============
    @Test
    @DisplayName("updateAuctionStatuses: activates pending auctions and ends active auctions")
    void updateAuctionStatuses_updatesStatuses() {
        Auction pendingAuction = new Auction();
        pendingAuction.setId(3L);
        pendingAuction.setStatus(AuctionStatus.PENDING);
        pendingAuction.setStartsAt(now.minusMinutes(1));

        Auction activeAuction = new Auction();
        activeAuction.setId(4L);
        activeAuction.setStatus(AuctionStatus.ACTIVE);
        activeAuction.setEndsAt(now.minusMinutes(1));

        when(auctionRepository.findByStatusAndStartsAtBefore(eq(AuctionStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(pendingAuction));
        when(auctionRepository.findByStatusAndEndsAtBefore(eq(AuctionStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(activeAuction));
        when(auctionRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        auctionService.updateAuctionStatuses();

        assertThat(pendingAuction.getStatus()).isEqualTo(AuctionStatus.ACTIVE);
        assertThat(activeAuction.getStatus()).isEqualTo(AuctionStatus.ENDED);

        verify(auctionRepository, times(1)).findByStatusAndStartsAtBefore(eq(AuctionStatus.PENDING), any(LocalDateTime.class));
        verify(auctionRepository, times(1)).findByStatusAndEndsAtBefore(eq(AuctionStatus.ACTIVE), any(LocalDateTime.class));
        verify(auctionRepository, times(2)).saveAll(anyList());
    }
}
