package com.order.order_service.repository;

import com.order.order_service.entity.Order;
import com.order.order_service.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order,Long> {

    List<Order> findByUserId(Long userId);

    List<Order> findByStatus(OrderStatus status);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    @Query("select count(o) from Order o where o.createdAt>=:from and o.createdAt<:to") long countCreatedBetween(@Param("from")LocalDateTime from,@Param("to")LocalDateTime to);
    @Query("select coalesce(sum(o.amount),0) from Order o where o.createdAt>=:from and o.createdAt<:to and o.status<>com.order.order_service.entity.OrderStatus.CANCELLED") java.math.BigDecimal sumRevenueBetween(@Param("from")LocalDateTime from,@Param("to")LocalDateTime to);
    @Query("select o.status,count(o) from Order o where o.createdAt>=:from and o.createdAt<:to group by o.status") java.util.List<Object[]> statusCounts(@Param("from")LocalDateTime from,@Param("to")LocalDateTime to);
}
