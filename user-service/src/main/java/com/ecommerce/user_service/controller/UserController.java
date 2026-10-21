package com.ecommerce.user_service.controller;
import com.ecommerce.user_service.service.UserManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api/users") public class UserController {
 private final UserManagementService users;
 public UserController(UserManagementService s) {
  users = s;
 }
 @GetMapping("/me") public Map < String, Object > me(@AuthenticationPrincipal Jwt jwt) {
  return users.get(userId(jwt));
 }
 @PutMapping("/me") public Map < String, Object > updateMe(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProfileRequest request) {
  return users.updateProfile(userId(jwt), request.name());
 }
 @PutMapping("/me/password") @ResponseStatus(HttpStatus.NO_CONTENT) public void changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PasswordChange request) {
  users.changePassword(userId(jwt), request.currentPassword(), request.newPassword());
 }
 @GetMapping public Map < String, Object > list(@AuthenticationPrincipal Jwt jwt) {
  admin(jwt);
  var rows = users.list();
  return Map.of("content", rows, "totalElements", rows.size());
 }
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public Map < String, Object > create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateUserRequest request) {
  admin(jwt);
  return users.create(request.name(), request.email(), request.password(), request.role());
 }
 @GetMapping("/{id}") public Map < String, Object > get(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
  admin(jwt);
  return users.get(id);
 }
 @PutMapping("/{id}") public Map < String, Object > update(@AuthenticationPrincipal Jwt jwt, @PathVariable long id, @Valid @RequestBody AdminUpdateRequest request) {
  admin(jwt);
  return users.update(id, request.name(), request.email(), request.role());
 }
 @PutMapping("/{id}/password") @ResponseStatus(HttpStatus.NO_CONTENT) public void resetPassword(@AuthenticationPrincipal Jwt jwt, @PathVariable long id, @Valid @RequestBody PasswordReset request) {
  admin(jwt);
  users.resetPassword(id, request.newPassword());
 }
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
  admin(jwt);
  if (userId(jwt) == id) throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot delete your own account");
  users.delete(id);
 }
 private long userId(Jwt jwt) {
  return Long.parseLong(jwt.getClaim("userId").toString());
 }
 private void admin(Jwt jwt) {
  if (!"ROLE_ADMIN".equals(jwt.getClaimAsString("role"))) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator role is required");
 }
 public record ProfileRequest(@NotBlank @Size(max = 100) String name) {}
 public record PasswordChange(@NotBlank String currentPassword, @NotBlank @Size(min = 8, max = 72) String newPassword) {}
 public record PasswordReset(@NotBlank @Size(min = 8, max = 72) String newPassword) {}
 public record CreateUserRequest(@NotBlank @Size(max = 100) String name, @NotBlank @Email @Size(max = 150) String email, @NotBlank @Size(min = 8, max = 72) String password, String role) {}
 public record AdminUpdateRequest(@NotBlank @Size(max = 100) String name, @NotBlank @Email @Size(max = 150) String email, @NotBlank String role) {}
}