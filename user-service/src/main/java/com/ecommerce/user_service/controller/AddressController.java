package com.ecommerce.user_service.controller;
import com.ecommerce.user_service.entity.UserAddress; import com.ecommerce.user_service.service.AddressService; import jakarta.validation.Valid; import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.Size; import java.util.List;
import org.springframework.http.HttpStatus; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.security.oauth2.jwt.Jwt; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/users/me/addresses") public class AddressController {
 private final AddressService service; public AddressController(AddressService s){service=s;}
 @GetMapping public List<UserAddress> list(@AuthenticationPrincipal Jwt jwt){return service.list(userId(jwt));}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public UserAddress create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody AddressRequest a){return service.create(userId(jwt),a.label(),a.line1(),a.line2(),a.city(),a.state(),a.postalCode(),a.country(),a.phoneNumber(),a.smsOptIn());}
 @PutMapping("/{id}") public UserAddress update(@AuthenticationPrincipal Jwt jwt,@PathVariable long id,@Valid @RequestBody AddressRequest a){return service.update(userId(jwt),id,a.label(),a.line1(),a.line2(),a.city(),a.state(),a.postalCode(),a.country(),a.phoneNumber(),a.smsOptIn());}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@AuthenticationPrincipal Jwt jwt,@PathVariable long id){service.delete(userId(jwt),id);}
 private long userId(Jwt jwt){return Long.parseLong(jwt.getClaim("userId").toString());}
 public record AddressRequest(@NotBlank @Size(max=60) String label,@NotBlank @Size(max=250) String line1,@Size(max=250) String line2,@NotBlank @Size(max=100) String city,@NotBlank @Size(max=100) String state,@NotBlank @Size(max=20) String postalCode,@NotBlank @Size(max=100) String country,@Size(max=30)String phoneNumber,boolean smsOptIn){}
}
