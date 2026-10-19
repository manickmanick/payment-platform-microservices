package com.ecommerce.user_service.repository;
import com.ecommerce.user_service.entity.FavoriteRestaurant;import java.util.List;import java.util.Optional;import org.springframework.data.jpa.repository.JpaRepository;
public interface FavoriteRestaurantRepository extends JpaRepository<FavoriteRestaurant,Long>{List<FavoriteRestaurant> findByUserIdOrderByCreatedAtDesc(Long userId);Optional<FavoriteRestaurant> findByUserIdAndRestaurantId(Long userId,Long restaurantId);}
