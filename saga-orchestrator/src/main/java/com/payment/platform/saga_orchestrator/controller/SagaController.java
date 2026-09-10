package com.payment.platform.saga_orchestrator.controller;


import com.payment.platform.saga_orchestrator.dto.CreateOrderRequest;
import com.payment.platform.saga_orchestrator.dto.SagaOrderRequest;
import com.payment.platform.saga_orchestrator.service.SagaOrchestratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sagas")
@RequiredArgsConstructor
public class SagaController {

    private final SagaOrchestratorService sagaOrchestratorService;

    @PostMapping("/orders")
    public ResponseEntity<Void> createOrder(
            @Valid @RequestBody SagaOrderRequest request) {

        sagaOrchestratorService.executeSaga(request);

        return ResponseEntity.ok().build();
    }
}
