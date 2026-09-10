package com.payment.platform.saga_orchestrator.service;


import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ResponseLossSimulator {

    private final AtomicBoolean triggered =
            new AtomicBoolean(false);

    public boolean shouldSimulateResponseLoss() {

        return triggered.compareAndSet(false, true);
    }

    public void reset() {

        triggered.set(false);
    }
}
