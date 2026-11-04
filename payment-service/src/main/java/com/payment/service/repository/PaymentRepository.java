
package com.payment.service.repository;

import com.payment.service.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Find payment using client-generated idempotency key
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    // Find payment using Stripe PaymentIntent ID
    Optional<Payment> findByStripePaymentIntentId(
            String stripePaymentIntentId
    );

    // Find payment using Stripe PaymentIntent ID with database row lock
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Payment p
            WHERE p.stripePaymentIntentId = :stripePaymentIntentId
            """)
    Optional<Payment> findByStripePaymentIntentIdForUpdate(
            @Param("stripePaymentIntentId") String stripePaymentIntentId
    );

    // Find all payments belonging to a user
    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Find all payment attempts for an order
    List<Payment> findByOrderId(Long orderId);

    // Find payment attempts for an order, newest first
    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
