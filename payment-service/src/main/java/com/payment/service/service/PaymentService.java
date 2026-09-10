package com.payment.service.service;

import com.payment.service.dto.PaymentRequest;
import com.payment.service.dto.PaymentResponse;
import com.payment.service.entity.Payment;
import com.payment.service.entity.PaymentStatus;
import com.payment.service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) {

        PaymentStatus status = request.simulateFailure()
                ? PaymentStatus.FAILED
                : PaymentStatus.SUCCESS;

        Payment payment = Payment.builder()
                .userId(request.userId())
                .orderId(request.orderId())
                .amount(request.amount())
                .currency(request.currency())
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        return new PaymentResponse(
                savedPayment.getId(),
                savedPayment.getStatus().name()
        );
    }
}