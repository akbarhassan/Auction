package com.ga.warehouse.models;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ga.warehouse.enums.WareHouseItemsCondition;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "auction_items")
public class AuctionItem {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String sku;

    @Column
    private String description;

    @Column
    private String displayImage;

    @ElementCollection
    @CollectionTable(name = "auction_item_gallery", joinColumns = @JoinColumn(name = "auction_item_id"))
    @Column(name = "image_url")
    private List<String> galleryImages = new ArrayList<>();

    @Column
    @Enumerated(EnumType.STRING)
    private WareHouseItemsCondition status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"role", "passwordHistories", "bids", "userProfile"})
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
