package com.fooddelivery.catalog.repository;

import com.fooddelivery.catalog.entity.MenuItem;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByRestaurant_IdOrderByName(Long id);
    void deleteByRestaurant_Id(Long id);
    boolean existsByCategory_Id(Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MenuItem m where m.id=:id")
    java.util.Optional<MenuItem> findByIdForUpdate(@Param("id") Long id);

    @Query("select m from MenuItem m where (:restaurantId is null or m.restaurant.id=:restaurantId) and (:categoryId is null or m.category.id=:categoryId) and (:available is null or m.available=:available) and (:foodType is null or m.foodType=:foodType) and (:minPrice is null or m.price>=:minPrice) and (:maxPrice is null or m.price<=:maxPrice) and (:q is null or lower(m.name) like lower(concat('%',:q,'%')) or lower(coalesce(m.description,'')) like lower(concat('%',:q,'%'))) and m.restaurant.status='OPEN' and m.restaurant.approvalStatus='APPROVED' order by m.averageRating desc,m.name asc")
    Page<MenuItem> search(@Param("q") String q, @Param("restaurantId") Long restaurantId,
                          @Param("categoryId") Long categoryId, @Param("available") Boolean available,
                          @Param("foodType") String foodType, @Param("minPrice") BigDecimal minPrice,
                          @Param("maxPrice") BigDecimal maxPrice, Pageable pageable);
}
