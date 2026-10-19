package com.fooddelivery.delivery.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
public class SmsNotificationService {
    private static final Logger log = LoggerFactory.getLogger(SmsNotificationService.class);
    private final RestClient client;
    private final String accountSid;
    private final String authToken;
    private final String fromNumber;

    public SmsNotificationService(RestClient.Builder builder,
            @Value("${twilio.account-sid:}") String accountSid,
            @Value("${twilio.auth-token:}") String authToken,
            @Value("${twilio.from-number:}") String fromNumber) {
        this.client = builder.build(); this.accountSid = accountSid; this.authToken = authToken; this.fromNumber = fromNumber;
    }

    public void send(String phone, String message) {
        if (accountSid.isBlank() || authToken.isBlank() || fromNumber.isBlank()) return;
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("To", phone); form.add("From", fromNumber); form.add("Body", message);
            String credentials = Base64.getEncoder().encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
            client.post().uri("https://api.twilio.com/2010-04-01/Accounts/{sid}/Messages.json", accountSid)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).header("Authorization", "Basic " + credentials)
                    .body(form).retrieve().toBodilessEntity();
        } catch (Exception e) { log.warn("Could not send delivery SMS (provider failure)", e); }
    }
}
