package com.ecommerce.user_service.service;

import com.ecommerce.user_service.entity.UserAddress;
import com.ecommerce.user_service.repository.UserAddressRepository;

import jakarta.persistence.EntityNotFoundException;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class AddressService {

    private final UserAddressRepository repo;

    public AddressService(UserAddressRepository repo) {
        this.repo = repo;
    }

    public List<UserAddress> list(long userId) {
        return repo.findByUserIdOrderByIdDesc(userId);
    }

    @Transactional
    public UserAddress create(
            long userId,
            String label,
            String line1,
            String line2,
            String city,
            String state,
            String postalCode,
            String country,
            String phoneNumber,
            boolean smsOptIn,
            boolean primaryAddress
    ) {
        validatePhoneNumber(phoneNumber, smsOptIn);

        if (primaryAddress) {
            repo.clearPrimaryAddressForUser(userId);
        }

        UserAddress address = new UserAddress(
                userId,
                label,
                line1,
                line2,
                city,
                state,
                postalCode,
                country,
                phoneNumber,
                smsOptIn,
                primaryAddress
        );

        return repo.save(address);
    }

    @Transactional
    public UserAddress update(
            long userId,
            long addressId,
            String label,
            String line1,
            String line2,
            String city,
            String state,
            String postalCode,
            String country,
            String phoneNumber,
            boolean smsOptIn,
            boolean primaryAddress
    ) {
        validatePhoneNumber(phoneNumber, smsOptIn);

        UserAddress address = find(userId, addressId);

        if (primaryAddress) {
            repo.clearPrimaryAddressForUser(userId);
        }

        address.update(
                label,
                line1,
                line2,
                city,
                state,
                postalCode,
                country,
                phoneNumber,
                smsOptIn,
                primaryAddress
        );

        return repo.save(address);
    }

    @Transactional
    public void delete(long userId, long addressId) {
        repo.delete(find(userId, addressId));
    }

    private UserAddress find(long userId, long addressId) {
        return repo.findByIdAndUserId(addressId, userId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Address not found")
                );
    }

    private void validatePhoneNumber(
            String phoneNumber,
            boolean smsOptIn
    ) {
        if (smsOptIn
                && (phoneNumber == null || phoneNumber.isBlank())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phone number is required for SMS updates"
            );
        }
    }
}