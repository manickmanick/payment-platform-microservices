package com.fooddelivery.catalog.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "restaurants")
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 160)
    private String name;
    @Column(length = 1000)
    private String description;
    @Column(nullable = false, length = 500)
    private String address;
    @Column(nullable = false, length = 100)
    private String city;
    @Column(name = "phone_number", length = 30)
    private String phoneNumber;
    @Column(nullable = false, length = 20)
    private String status = "OPEN";
    @Column(name = "image_url", length = 1000)
    private String imageUrl;
    @Column(name = "owner_id")
    private Long ownerId;
    @Column(name = "approval_status", nullable = false, length = 20)
    private String approvalStatus = "APPROVED";
    @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO;
    @Column(name = "review_count", nullable = false)
    private Integer reviewCount = 0;
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Restaurant() {
    }

    public Restaurant(String n, String d, String a, String c, String p, String s, Long owner) {
        name = n;
        description = d;
        address = a;
        city = c;
        phoneNumber = p;
        status = s;
        ownerId = owner;
        approvalStatus = "APPROVED";
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getStatus() {
        return status;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public java.math.BigDecimal getAverageRating() {
        return averageRating;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setImageUrl(String u) {
        imageUrl = u;
    }

    public void setApprovalStatus(String s) {
        approvalStatus = s;
    }

    public void refreshRating(BigDecimal rating, int count) {
        averageRating = rating;
        reviewCount = count;
    }

    public void update(String n, String d, String a, String c, String p, String s) {
        name = n;
        description = d;
        address = a;
        city = c;
        phoneNumber = p;
        status = s;
    }
}
