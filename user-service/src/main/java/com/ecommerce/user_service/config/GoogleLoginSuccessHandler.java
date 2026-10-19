package com.ecommerce.user_service.config;

import com.ecommerce.user_service.dto.AuthResponse;
import com.ecommerce.user_service.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GoogleLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final AuthService authService;
    private final String frontendUrl;
    public GoogleLoginSuccessHandler(AuthService authService, @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.authService=authService; this.frontendUrl=frontendUrl;
    }
    @Override public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User principal=(OAuth2User)authentication.getPrincipal();
        String email=principal.getAttribute("email");
        if(email==null || email.isBlank()) { response.sendError(400,"Google account did not provide an email"); return; }
        Object verified=principal.getAttribute("email_verified");
        if(Boolean.FALSE.equals(verified)) { response.sendError(403,"Google email is not verified"); return; }
        AuthResponse token=authService.loginWithGoogle(email,principal.getAttribute("name"));
        response.sendRedirect(frontendUrl+"/oauth/callback#access_token="+java.net.URLEncoder.encode(token.accessToken(),java.nio.charset.StandardCharsets.UTF_8));
    }
}
