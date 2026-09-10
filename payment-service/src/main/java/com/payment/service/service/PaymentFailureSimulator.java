package com.payment.service.service;


import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class PaymentFailureSimulator {

    private final AtomicInteger attempts = new AtomicInteger();

    public boolean shouldFail(){
        int currentAttempt = attempts.incrementAndGet();
        return currentAttempt == 1;
    }

    public void reset(){
        attempts.set(0);
    }
}
