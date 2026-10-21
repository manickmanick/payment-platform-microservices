package com.fooddelivery.catalog.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "restaurant_reviews", uniqueConstraints = @UniqueConstraint(name = "uk_reviews_restaurant_user", columnNames = {"restaurant_id", "user_id"}))
public class RestaurantReview {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "restaurant_id", nullable = false) private Long restaurantId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false) private Byte  rating;
    @Column(length = 1000) private String comment;
    @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
    protected RestaurantReview() {}
    public RestaurantReview(Long restaurantId, Long userId, Byte rating, String comment) { this.restaurantId=restaurantId; this.userId=userId; this.rating=rating; this.comment=comment; }
    public Long getId(){return id;} public Long getRestaurantId(){return restaurantId;} public Long getUserId(){return userId;} public Byte getRating(){return rating;} public String getComment(){return comment;} public LocalDateTime getCreatedAt(){return createdAt;}
}
