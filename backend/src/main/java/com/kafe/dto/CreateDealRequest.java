package com.kafe.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public class CreateDealRequest {

    private static final String TIME_PATTERN = "^([01]\\d|2[0-3]):[0-5]\\d$";

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Original price is required")
    @Positive(message = "Original price must be greater than 0")
    private Double originalPrice;

    @NotNull(message = "Discount price is required")
    @Positive(message = "Discount price must be greater than 0")
    private Double discountPrice;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    // Optional; defaults to today in Phnom Penh time
    private LocalDate pickupDate;

    @NotBlank(message = "Pickup start time is required")
    @Pattern(regexp = TIME_PATTERN, message = "Pickup start must be in HH:mm format")
    private String pickupStart;

    @NotBlank(message = "Pickup end time is required")
    @Pattern(regexp = TIME_PATTERN, message = "Pickup end must be in HH:mm format")
    private String pickupEnd;

    private String imageUrl;

    public CreateDealRequest() {}

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

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public LocalDate getPickupDate() {
        return pickupDate;
    }

    public void setPickupDate(LocalDate pickupDate) {
        this.pickupDate = pickupDate;
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
}
