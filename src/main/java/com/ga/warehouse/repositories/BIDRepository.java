package com.ga.warehouse.repositories;


import com.ga.warehouse.models.BID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BIDRepository extends CrudRepository<BID, Long> {
    List<BID> findByAuctionIdOrderByAmountDesc(Long auctionId);

    Optional<BID> findFirstByAuctionIdOrderByAmountDesc(Long auctionId);

    List<BID> findByBidderId(Long userId);

    long countByAuctionId(Long auctionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BID b WHERE b.auction.id = :auctionId ORDER BY b.amount DESC")
    Optional<BID> findTopByAuctionIdOrderByAmountDescWithLock(@Param("auctionId") Long auctionId);
}
