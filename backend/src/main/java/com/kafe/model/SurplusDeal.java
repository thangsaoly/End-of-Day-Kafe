package com.kafe.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "surplus_deals")
public class SurplusDeal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cafe_id", nullable = false)
    private Cafe cafe;

    @Column(nullable = false)
    private String title;

    @Column(name = "original_price", nullable = false)
    private Double originalPrice;

    @Column(name = "discount_price", nullable = false)
    private Double discountPrice;

    @Column(name = "quantity_available", nullable = false)
    private Integer quantityAvailable;

    // Day the pickup window applies to (Asia/Phnom_Penh). Nullable only so that
    // rows created before this column existed can still be loaded.
    @Column(name = "pickup_date")
    private LocalDate pickupDate;

    @Column(name = "pickup_start", nullable = false)
    private String pickupStart;

    @Column(name = "pickup_end", nullable = false)
    private String pickupEnd;

    @Column(name = "image_url")
    private String imageUrl;

    public SurplusDeal() {}

    public SurplusDeal(Cafe cafe, String title, Double originalPrice, Double discountPrice, Integer quantityAvailable, String pickupStart, String pickupEnd, String imageUrl) {
        this.cafe = cafe;
        this.title = title;
        this.originalPrice = originalPrice;
        this.discountPrice = discountPrice;
        this.quantityAvailable = quantityAvailable;
        this.pickupStart = pickupStart;
        this.pickupEnd = pickupEnd;
        this.imageUrl = imageUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cafe getCafe() {
        return cafe;
    }

    public void setCafe(Cafe cafe) {
        this.cafe = cafe;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Double getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(Double originalPrice) {
        this.originalPrice = originalPrice;
    }

    public Double getDiscountPrice() {
        return discountPrice;
    }

    public void setDiscountPrice(Double discountPrice) {
        this.discountPrice = discountPrice;
    }

    public Integer getQuantityAvailable() {
        return quantityAvailable;
    }

    public void setQuantityAvailable(Integer quantityAvailable) {
        this.quantityAvailable = quantityAvailable;
    }

    public String getPickupStart() {
        return pickupStart;
    }

    public void setPickupStart(String pickupStart) {
        this.pickupStart = pickupStart;
    }

    public String getPickupEnd() {
        return pickupEnd;
    }

    public void setPickupEnd(String pickupEnd) {
        this.pickupEnd = pickupEnd;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDate getPickupDate() {
        return pickupDate;
    }

    public void setPickupDate(LocalDate pickupDate) {
        this.pickupDate = pickupDate;
    }
}
