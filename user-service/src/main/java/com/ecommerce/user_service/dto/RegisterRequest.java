package com.ecommerce.user_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Email
    @Size(max = 150)
    private String email;

    @NotBlank
    @Size(min = 8, max = 72)
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    @JsonProperty("isRestaurantOwner")
    private boolean isRestaurantOwner;

    @Size(max = 100, message = "Secret code is too long")
    private String secretCode;

    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordMatching(){
        return password != null && password.equals(confirmPassword);
    }

    @AssertTrue(message = "Secret code is required for restaurant owners")
    public boolean isSecretCodeValid(){
        return !isRestaurantOwner || (secretCode != null && !secretCode.isBlank());
    }

}