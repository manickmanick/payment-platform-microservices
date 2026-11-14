
package com.company.api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdGlobalFilter
        implements GlobalFilter, Ordered {

    private static final String CORRELATION_ID_HEADER =
            "X-Correlation-ID";

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        // 1. Generate a trusted correlation ID
        String correlationId = UUID.randomUUID().toString();

        // 2. Mutate the incoming request
        ServerHttpRequest mutatedRequest = exchange
                .getRequest()
                .mutate()
                .headers(headers ->
                        headers.set(
                                CORRELATION_ID_HEADER,
                                correlationId
                        )
                )
                .build();

        // 3. Create a new exchange with the mutated request
        ServerWebExchange mutatedExchange = exchange
                .mutate()
                .request(mutatedRequest)
                .build();

        // 4. Add the same ID to the outgoing response
        exchange.getResponse().beforeCommit(() -> {

            exchange.getResponse()
                    .getHeaders()
                    .set(
                            CORRELATION_ID_HEADER,
                            correlationId
                    );

            return Mono.empty();
        });

        // 5. Continue Gateway filter chain
        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
