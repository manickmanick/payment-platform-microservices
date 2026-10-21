package com.ecommerce.user_service.repository;

import com.ecommerce.user_service.entity.UserAddress;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAddressRepository
        extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByUserIdOrderByIdDesc(Long userId);

    Optional<UserAddress> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("""
        UPDATE UserAddress a
        SET a.primaryAddress = false
        WHERE a.userId = :userId
          AND a.primaryAddress = true
        """)
    int clearPrimaryAddressForUser(@Param("userId") Long userId);

    void deleteByUserId(Long userId);
}