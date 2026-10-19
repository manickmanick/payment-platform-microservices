package com.ecommerce.user_service.repository;
import com.ecommerce.user_service.entity.UserActionToken;import java.util.Optional;import org.springframework.data.jpa.repository.JpaRepository;
public interface UserActionTokenRepository extends JpaRepository<UserActionToken,Long>{Optional<UserActionToken> findByTokenHashAndPurpose(String tokenHash,String purpose);void deleteByUser_IdAndPurpose(Long userId,String purpose);}
