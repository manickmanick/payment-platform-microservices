package com.payment.service.controller;


import com.stripe.StripeClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.stripe.model.Account;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class StripeTestController {

    private final StripeClient stripeClient;

    @Value("${stripe.account-id}")
    private String accountId;

    @GetMapping("/test/stripe")
    public ResponseEntity<Map<String,String>> testStripe()  throws Exception{

        Account account = stripeClient.v1().accounts().retrieve(accountId);

        return ResponseEntity.ok(
                Map.of(
                        "message", "Stripe connection successful",
                        "accountId", account.getId()
                )
        );

    }
}
