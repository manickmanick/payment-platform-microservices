package com.payment.service.repository;

import com.payment.service.entity.OrderEventReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderEventReceiptRepository extends JpaRepository<OrderEventReceipt, String> {
}
