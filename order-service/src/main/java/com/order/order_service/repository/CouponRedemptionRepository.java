package com.order.order_service.repository;
import com.order.order_service.entity.CouponRedemption;import org.springframework.data.jpa.repository.JpaRepository;
public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption,Long>{boolean existsByCouponIdAndUserId(Long couponId,Long userId);java.util.Optional<CouponRedemption> findByOrderId(Long orderId);void deleteByOrderId(Long orderId);}
