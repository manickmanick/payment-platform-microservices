package com.fooddelivery.mail.messaging;
import org.springframework.context.annotation.Bean;import org.springframework.context.annotation.Configuration;import org.springframework.kafka.listener.DefaultErrorHandler;import org.springframework.util.backoff.FixedBackOff;
@Configuration public class KafkaRetryConfig{@Bean DefaultErrorHandler mailErrorHandler(){return new DefaultErrorHandler(new FixedBackOff(1000L,3L));}}
