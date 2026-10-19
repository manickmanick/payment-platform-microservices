package com.fooddelivery.delivery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
@Configuration public class DeliverySecurityConfig {
 @Bean JwtDecoder deliveryJwtDecoder(@Value("${JWT_SECRET:change-this-development-secret-to-at-least-32-bytes}") String secret){return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256")).macAlgorithm(MacAlgorithm.HS256).build();}
 @Bean SecurityFilterChain deliverySecurity(HttpSecurity http)throws Exception{return http.csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests(a->a.requestMatchers("/internal/**","/actuator/health","/actuator/info","/swagger-ui/**","/swagger-ui.html","/v3/api-docs/**").permitAll().anyRequest().authenticated()).oauth2ResourceServer(o->o.jwt(jwt->{})).build();}
}
