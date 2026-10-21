package com.ecommerce.user_service.controller;

import com.ecommerce.user_service.entity.UserAddress;
import com.ecommerce.user_service.service.AddressService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me/addresses")
public class AddressController {

 private final AddressService service;

 public AddressController(AddressService service) {
  this.service = service;
 }

 @GetMapping
 public List<UserAddress> list(
         @AuthenticationPrincipal Jwt jwt
 ) {
  return service.list(userId(jwt));
 }

 @PostMapping
 @ResponseStatus(HttpStatus.CREATED)
 public UserAddress create(
         @AuthenticationPrincipal Jwt jwt,
         @Valid @RequestBody AddressRequest request
 ) {
  return service.create(
          userId(jwt),
          request.label(),
          request.line1(),
          request.line2(),
          request.city(),
          request.state(),
          request.postalCode(),
          request.country(),
          request.phoneNumber(),
          request.smsOptIn(),
          request.primaryAddress()
  );
 }

 @PutMapping("/{id}")
 public UserAddress update(
         @AuthenticationPrincipal Jwt jwt,
         @PathVariable long id,
         @Valid @RequestBody AddressRequest request
 ) {
  return service.update(
          userId(jwt),
          id,
          request.label(),
          request.line1(),
          request.line2(),
          request.city(),
          request.state(),
          request.postalCode(),
          request.country(),
          request.phoneNumber(),
          request.smsOptIn(),
          request.primaryAddress()
  );
 }

 @DeleteMapping("/{id}")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void delete(
         @AuthenticationPrincipal Jwt jwt,
         @PathVariable long id
 ) {
  service.delete(userId(jwt), id);
 }

 private long userId(Jwt jwt) {
  return Long.parseLong(
          jwt.getClaim("userId").toString()
  );
 }

 public record AddressRequest(
         @NotBlank @Size(max = 60)
         String label,

         @NotBlank @Size(max = 250)
         String line1,

         @Size(max = 250)
         String line2,

         @NotBlank @Size(max = 100)
         String city,

         @NotBlank @Size(max = 100)
         String state,

         @NotBlank @Size(max = 20)
         String postalCode,

         @NotBlank @Size(max = 100)
         String country,

         @Size(max = 30)
         String phoneNumber,

         boolean smsOptIn,

         boolean primaryAddress
 ) {
 }
}