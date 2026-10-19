package com.fooddelivery.catalog.repository;

import com.fooddelivery.catalog.entity.RestaurantReview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RestaurantReviewRepository extends JpaRepository<RestaurantReview, Long> {
    List<RestaurantReview> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);
    Optional<RestaurantReview> findByRestaurantIdAndUserId(Long restaurantId, Long userId);
    @Query("select count(r) from RestaurantReview r where r.restaurantId=:restaurantId") long countForRestaurant(Long restaurantId);
    @Query("select coalesce(avg(r.rating),0) from RestaurantReview r where r.restaurantId=:restaurantId") Double averageForRestaurant(Long restaurantId);
}
