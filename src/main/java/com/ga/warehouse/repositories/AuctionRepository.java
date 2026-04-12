package com.ga.warehouse.repositories;


import com.ga.warehouse.enums.AuctionStatus;
import com.ga.warehouse.models.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {
    Optional<Auction> findByAuctionItemId(Long itemId);

    List<Auction> findByStatusAndStartsAtBeforeAndEndsAtAfter(
            AuctionStatus status,
            LocalDateTime now,
            LocalDateTime nowLater
    );

    List<Auction> findByStatus(AuctionStatus status);

    List<Auction> findByStatusAndEndsAtBefore(AuctionStatus status, LocalDateTime now);

    List<Auction> findByStatusAndStartsAtBefore(AuctionStatus status, LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auction a WHERE a.id = :id")
    Optional<Auction> findByIdWithLock(@Param("id") Long id);

}
