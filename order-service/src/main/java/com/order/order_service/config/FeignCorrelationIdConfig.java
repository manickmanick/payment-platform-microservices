
package com.order.order_service.config;

import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignCorrelationIdConfig {

    private static final String CORRELATION_ID_HEADER =
            "X-Correlation-ID";

    private static final String CORRELATION_ID_MDC =
            "correlationId";

    @Bean
    public RequestInterceptor correlationIdRequestInterceptor() {

        return requestTemplate -> {

            String correlationId =
                    MDC.get(CORRELATION_ID_MDC);

            if (correlationId != null && !correlationId.isBlank()) {

                requestTemplate.header(
                        CORRELATION_ID_HEADER,
                        correlationId
                );
            }
        };
    }
}
