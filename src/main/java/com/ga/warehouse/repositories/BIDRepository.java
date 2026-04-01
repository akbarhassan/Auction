package com.ga.warehouse.repositories;


import com.ga.warehouse.models.BID;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BIDRepository extends CrudRepository<BID, Long> {
    List<BID> findByAuctionIdOrderByAmountDesc(Long auctionId);

    Optional<BID> findFirstByAuctionIdOrderByAmountDesc(Long auctionId);

    List<BID> findByBidderId(Long userId);

    long countByAuctionId(Long auctionId);
}
