package com.ecommerce.user_service.repository;
import com.ecommerce.user_service.entity.UserAddress; import java.util.List; import java.util.Optional; import org.springframework.data.jpa.repository.JpaRepository;
public interface UserAddressRepository extends JpaRepository<UserAddress,Long>{List<UserAddress> findByUserIdOrderByIdDesc(Long userId);Optional<UserAddress> findByIdAndUserId(Long id,Long userId);void deleteByUserId(Long userId);}
