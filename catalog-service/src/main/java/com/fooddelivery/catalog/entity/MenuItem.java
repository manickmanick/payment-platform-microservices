package com.fooddelivery.catalog.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "menu_items")
public class MenuItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    @Column(nullable = false, length = 160)
    private String name;
    @Column(length = 1000)
    private String description;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    @Column(name = "food_type", nullable = false, length = 20)
    private String foodType;
    @Column(nullable = false)
    private Boolean available;
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 100;
    @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO;
    @Column(name = "review_count", nullable = false)
    private Integer reviewCount = 0;
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected MenuItem() {
    }

    public MenuItem(Restaurant r, Category c, String n, String d, BigDecimal p, String f, Boolean a) {
        restaurant = r;
        category = c;
        name = n;
        description = d;
        price = p;
        foodType = f;
        available = a;
    }

    public Long getId() {
        return id;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public Category getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getFoodType() {
        return foodType;
    }

    public Boolean getAvailable() {
        return available;
    }


    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public BigDecimal getAverageRating() {
        return averageRating;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setStockQuantity(Integer q) {
        stockQuantity = q;
    }

    public void refreshRating(BigDecimal rating, int count) {
        averageRating = rating;
        reviewCount = count;
    }

    public void update(Restaurant r, Category c, String n, String d, BigDecimal p, String f, Boolean a) {
        restaurant = r;
        category = c;
        name = n;
        description = d;
        price = p;
        foodType = f;
        available = a;
    }
}
