
package com.order.order_service.repository;

import com.order.order_service.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, Long> {


    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(String status);
}
