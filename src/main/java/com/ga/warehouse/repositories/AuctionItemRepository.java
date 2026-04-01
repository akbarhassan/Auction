package com.ga.warehouse.repositories;


import com.ga.warehouse.enums.WareHouseItemsCondition;
import com.ga.warehouse.models.AuctionItem;
import com.ga.warehouse.models.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionItemRepository extends JpaRepository<AuctionItem, Long> {
    Optional<AuctionItem> findBySku(String sku);

    List<AuctionItem> findByCategory(Category category);

    List<AuctionItem> findByStatus(WareHouseItemsCondition status);

    List<AuctionItem> findByNameContainingIgnoreCase(String name);

}
