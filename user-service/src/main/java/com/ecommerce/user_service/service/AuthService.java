package com.ecommerce.user_service.service;

import com.ecommerce.user_service.dto.AuthResponse;
import com.ecommerce.user_service.dto.LoginRequest;
import com.ecommerce.user_service.dto.RegisterRequest;
import com.ecommerce.user_service.entity.User;
import com.ecommerce.user_service.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    @Value("${app.admin-emails:}")
    private String adminEmails;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuthTokenService authTokens;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            AuthTokenService authTokens
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.authTokens = authTokens;
    }

//    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered"
            );
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.isRestaurantOwner() ? "ROLE_RESTAURANT_OWNER" : "ROLE_USER");
        user.setEmailVerified(true);

        User savedUser = userRepository.save(user);
//        authTokens.sendVerification(savedUser);
        return createResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                email,
                                request.getPassword()
                        )
                );

        User user = (User) authentication.getPrincipal();

        return createResponse(user);
    }

    @Transactional
    public AuthResponse loginWithGoogle(String email, String name) {
        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail).orElseGet(() -> {
            User created = new User();
            created.setEmail(normalizedEmail);
            created.setName(name == null || name.isBlank() ? normalizedEmail : name.trim());
            created.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
            created.setRole(roleFor(normalizedEmail));
            created.setEmailVerified(true);
            return userRepository.save(created);
        });
        user.setEmailVerified(true);
        return createResponse(user);
    }

    private AuthResponse createResponse(User user) {
        String token = user.isEmailVerified() ? jwtService.generateToken(user) : null;

        return new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                "Bearer",
                token,
                jwtService.getExpirationSeconds(),
                user.getRole(),
                user.isEmailVerified()
        );
    }

    @Value("${app.driver-emails:}") private String driverEmails;
    @Value("${app.restaurant-owner-emails:}") private String ownerEmails;
    private String roleFor(String email) {
        if(matches(adminEmails,email)) return "ROLE_ADMIN";
        if(matches(driverEmails,email)) return "ROLE_DRIVER";
        if(matches(ownerEmails,email)) return "ROLE_RESTAURANT_OWNER";
        return "ROLE_USER";
    }
    private boolean matches(String list,String email) {
        return java.util.Arrays.stream(list.split(",")).map(String::trim).map(String::toLowerCase).anyMatch(email::equals);
    }
}
