package com.ecommerce.user_service.controller;

import com.ecommerce.user_service.dto.AuthResponse;
import com.ecommerce.user_service.dto.LoginRequest;
import com.ecommerce.user_service.dto.RegisterRequest;
import com.ecommerce.user_service.service.AuthService;
import com.ecommerce.user_service.service.AuthTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthTokenService authTokens;

    public AuthController(AuthService authService,AuthTokenService authTokens) {
        this.authService = authService;
        this.authTokens=authTokens;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthResponse response = authService.register(request);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/email/verify") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(@Valid @RequestBody TokenRequest request){authTokens.verifyEmail(request.token());}
    @PostMapping("/email/resend")
    public Map<String,String> resendVerification(@Valid @RequestBody EmailRequest request){authTokens.resendVerification(request.email());return Map.of("message","If verification is needed, an email has been sent.");}
    @PostMapping("/password/forgot")
    public Map<String,String> forgotPassword(@Valid @RequestBody EmailRequest request){authTokens.requestPasswordReset(request.email());return Map.of("message","If the account exists, password reset instructions have been sent.");}
    @PostMapping("/password/reset") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request){authTokens.resetPassword(request.token(),request.password());}
    public record TokenRequest(@NotBlank String token){}
    public record EmailRequest(@NotBlank @Email @Size(max=254)String email){}
    public record ResetPasswordRequest(@NotBlank String token,@NotBlank @Size(min=8,max=72)String password){}
}
