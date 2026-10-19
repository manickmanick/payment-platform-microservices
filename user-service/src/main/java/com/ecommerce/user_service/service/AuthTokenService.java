package com.ecommerce.user_service.service;

import com.ecommerce.user_service.entity.User;
import com.ecommerce.user_service.entity.UserActionToken;
import com.ecommerce.user_service.repository.UserActionTokenRepository;
import com.ecommerce.user_service.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Service
public class AuthTokenService {
    private static final SecureRandom RANDOM=new SecureRandom();
    private final UserActionTokenRepository tokens;private final UserRepository users;private final org.springframework.security.crypto.password.PasswordEncoder encoder;private final KafkaTemplate<String,String> kafka;private final ObjectMapper mapper;private final String frontendUrl;
    public AuthTokenService(UserActionTokenRepository tokens,UserRepository users,org.springframework.security.crypto.password.PasswordEncoder encoder,KafkaTemplate<String,String> kafka,ObjectMapper mapper,@Value("${app.frontend-url:http://localhost:5173}")String frontendUrl){this.tokens=tokens;this.users=users;this.encoder=encoder;this.kafka=kafka;this.mapper=mapper;this.frontendUrl=frontendUrl;}
    @Transactional public void sendVerification(User user){issue(user,"VERIFY_EMAIL",24,"Verify your food delivery account", "/verify-email?token=");}
    @Transactional public void resendVerification(String email){users.findByEmail(normalize(email)).filter(u->!u.isEmailVerified()).ifPresent(this::sendVerification);}
    @Transactional public void verifyEmail(String raw){UserActionToken token=consume(raw,"VERIFY_EMAIL");token.getUser().setEmailVerified(true);users.save(token.getUser());}
    @Transactional public void requestPasswordReset(String email){users.findByEmail(normalize(email)).filter(User::isEmailVerified).ifPresent(u->issue(u,"RESET_PASSWORD",1,"Reset your password", "/reset-password?token="));}
    @Transactional public void resetPassword(String raw,String password){UserActionToken token=consume(raw,"RESET_PASSWORD");User user=token.getUser();user.setPassword(encoder.encode(password));users.save(user);}
    private void issue(User user,String purpose,int hours,String subject,String path){tokens.deleteByUser_IdAndPurpose(user.getId(),purpose);byte[] bytes=new byte[32];RANDOM.nextBytes(bytes);String raw=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);tokens.save(new UserActionToken(user,hash(raw),purpose,LocalDateTime.now().plusHours(hours)));String url=frontendUrl+path+java.net.URLEncoder.encode(raw,StandardCharsets.UTF_8);String body="Use this secure link within "+hours+" hour(s):\n\n"+url+"\n\nIf you did not request this, ignore this email.";try{String event=mapper.writeValueAsString(Map.of("to",user.getEmail(),"subject",subject,"body",body));kafka.send("mail-requested",user.getEmail(),event).get(8,TimeUnit.SECONDS);}catch(Exception e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Email could not be queued",e);}}
    private UserActionToken consume(String raw,String purpose){if(raw==null||raw.isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Token is required");UserActionToken t=tokens.findByTokenHashAndPurpose(hash(raw),purpose).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Token is invalid or expired"));if(t.getConsumedAt()!=null||t.getExpiresAt().isBefore(LocalDateTime.now()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Token is invalid or expired");t.consume();return t;}
    private String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private String normalize(String email){return email==null?"":email.trim().toLowerCase(java.util.Locale.ROOT);}
}
