package com.payment.platform.saga_orchestrator.service;


import com.payment.platform.saga_orchestrator.client.PaymentClient;
import com.payment.platform.saga_orchestrator.dto.PaymentRequest;
import com.payment.platform.saga_orchestrator.dto.PaymentResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceClient {

    private final PaymentClient paymentClient;
    private final ResponseLossSimulator responseLossSimulator;

//    @Retry(name = "paymentServiceRetry")


    @CircuitBreaker(
            name = "paymentServiceCircuitBreaker",
            fallbackMethod = "paymentServiceFallback"
    )
    public PaymentResponse createPayment(PaymentRequest request) {

        log.info(
                "Calling Payment Service. orderId={}, idempotencyKey={}",
                request.orderId(),
                request.idempotencyKey()
        );

        PaymentResponse response =
                paymentClient.createPayment(request);

        log.info(
                "Payment Service responded. paymentId={}, status={}",
                response.paymentId(),
                response.status()
        );

//        if (responseLossSimulator.shouldSimulateResponseLoss()) {
//
//            log.warn("Simulating lost payment response");
//
//            throw new RuntimeException(
//                    "Simulated lost payment response"
//            );
//        }

        return response;
    }

    private PaymentResponse paymentServiceFallback(
            PaymentRequest request,
            Throwable throwable) {

        log.error(
                "Payment Service unavailable. Circuit Breaker fallback executed. " +
                        "orderId={}, reason={}",
                request.orderId(),
                throwable.getMessage()
        );

        return new PaymentResponse(
                null,
                "FAILED"
        );
    }
}
