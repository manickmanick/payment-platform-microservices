
package com.fooddelivery.catalog;

import com.fooddelivery.catalog.service.CatalogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class CatalogController {

 private final CatalogService service;
 private final String internalKey;

 public CatalogController(
         CatalogService service,
         @Value("${catalog.internal-key:local-internal-key-change-me}")
         String internalKey) {

  this.service = service;
  this.internalKey = internalKey;
 }

 // Internal stock operations

 @PostMapping("/internal/menu-items/{id}/reserve")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void reserve(
         @RequestHeader("X-Internal-Key") String key,
         @PathVariable long id,
         @RequestParam int quantity) {

  checkInternal(key);
  service.reserveStock(id, quantity);
 }

 @PostMapping("/internal/menu-items/{id}/release")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void release(
         @RequestHeader("X-Internal-Key") String key,
         @PathVariable long id,
         @RequestParam int quantity) {

  checkInternal(key);
  service.releaseStock(id, quantity);
 }

 // Restaurant administration

 @GetMapping("/restaurants/admin/applications")
 public Map<String, Object> applications(
         @AuthenticationPrincipal Jwt jwt) {

  admin(jwt);

  var rows = service.pendingRestaurants();

  return Map.of(
          "content", rows,
          "totalElements", rows.size()
  );
 }

 @PutMapping("/restaurants/admin/{id}/review")
 public Map<String, Object> reviewRestaurant(
         @AuthenticationPrincipal Jwt jwt,
         @PathVariable long id,
         @Valid @RequestBody ReviewRestaurantRequest request) {

  admin(jwt);

  return service.reviewRestaurant(id, request.decision());
 }

 // Restaurant search

 @GetMapping("/restaurants/search")
 public Map<String, Object> searchRestaurants(
         @RequestParam(required = false) String q,
         @RequestParam(required = false) String city,
         @RequestParam(required = false) String status,
         @RequestParam(defaultValue = "0") int page,
         @RequestParam(defaultValue = "20") int size) {

  return service.searchRestaurants(q, city, status, page, size);
 }

 // Menu search

 @GetMapping("/menu-items/search")
 public Map<String, Object> searchMenu(
         @RequestParam(required = false) String q,
         @RequestParam(required = false) Long restaurantId,
         @RequestParam(required = false) Long categoryId,
         @RequestParam(required = false) Boolean available,
         @RequestParam(required = false) String foodType,
         @RequestParam(required = false) BigDecimal minPrice,
         @RequestParam(required = false) BigDecimal maxPrice,
         @RequestParam(defaultValue = "0") int page,
         @RequestParam(defaultValue = "20") int size) {

  return service.searchMenu(
          q,
          restaurantId,
          categoryId,
          available,
          foodType,
          minPrice,
          maxPrice,
          page,
          size
  );
 }

 // Restaurant reviews

 @GetMapping("/restaurants/{id}/reviews")
 public Map<String, Object> reviews(@PathVariable long id) {
  return service.reviewList(id);
 }

 @PostMapping("/restaurants/{id}/reviews")
 @ResponseStatus(HttpStatus.CREATED)
 public Map<String, Object> review(
         @AuthenticationPrincipal Jwt jwt,
         @PathVariable long id,
         @Valid @RequestBody ReviewRequest request) {

  long userId = authenticatedUserId(jwt);

  return service.addReview(
          id,
          userId,
          request.rating(),
          request.comment()
  );
 }

 // Menu stock management

 @PatchMapping("/menu-items/{id}/stock")
 public Map<String, Object> stock(
         @AuthenticationPrincipal Jwt jwt,
         @PathVariable long id,
         @Valid @RequestBody StockRequest request) {

  long actor = manager(jwt);

  return service.setStock(
          id,
          actor,
          isAdmin(jwt),
          request.quantity()
  );
 }

 // Categories

 @GetMapping("/categories")
 public Map<String, Object> categories() {
  var rows = service.categories();

  return Map.of(
          "content", rows,
          "totalElements", rows.size()
  );
 }

 @PostMapping("/categories")
 @ResponseStatus(HttpStatus.CREATED)
 public Map<String, Object> createCategory(
         @AuthenticationPrincipal Jwt jwt,
         @Valid @RequestBody CategoryRequest request) {

  admin(jwt);

  return service.createCategory(
          request.name(),
          request.imageUrl()
  );
 }

 @PutMapping("/categories/{id}")
 public Map<String, Object> updateCategory(
         @AuthenticationPrincipal Jwt jwt,
         @PathVariable long id,
         @Valid @RequestBody CategoryRequest request) {

  admin(jwt);

  return service.updateCategory(
          id,
          request.name(),
          request.imageUrl()
  );
 }

 @DeleteMapping("/categories/{id}")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void deleteCategory(
         @AuthenticationPrincipal Jwt jwt,
         @PathVariable long id) {

  admin(jwt);
  service.deleteCategory(id);
 }

 // Restaurants

 @GetMapping("/restaurants")
 public Map<String, Object> restaurants() {
  var rows = service.restaurants();

  return Map.of(
          "content", rows,
          "totalElements", rows.size()
  );
 }

 @GetMapping("/restaurants/{id}")
 public Map<String, Object> restaurant(@PathVariable long id) {
  return service.restaurant(id);
 }

 @PostMapping("/restaurants")
 @ResponseStatus(HttpStatus.CREATED)
 public Map<String, Object> createRestaurant(
         @AuthenticationPrincipal Jwt jwt,
         @Valid @RequestBody RestaurantRequest request) {

  long actor = manager(jwt);

  return service.createRestaurant(
          actor,
          request.name(),
          request.description(),
          request.address(),
          request.city(),
          request.phoneNumber(),
          request.status()
  );
 }

 @PutMapping("/restaurants/{id}")
 public Map<String, Object> updateRestaurant(
         @AuthenticationPrincipal Jwt jwt,
         @PathVariable long id,
         @Valid @RequestBody RestaurantRequest request) {

  long actor = manager(jwt);

  return service.updateRestaurant(
          id,
          actor,
          isAdmin(jwt),
          request.name(),
          request.description(),
          request.address(),
          request.city(),
          request.phoneNumber(),
          request.status()
  );
 }

 @DeleteMapping("/restaurants/{id}")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void deleteRestaurant(
         @AuthenticationPrincipal Jwt jwt,
         @PathVariable long id) {

  long actor = manager(jwt);

  service.deleteRestaurant(id, actor, isAdmin(jwt));
 }

 // Menu items

 @GetMapping("/menu-items/restaurant/{restaurantId}")
 public Map<String, Object> menu(
         @PathVariable long restaurantId) {

  var rows = service.menu(restaurantId);

  return Map.of(
          "content", rows,
          "totalElements", rows.size()
  );
 }

 @PostMapping("/menu-items")
 @ResponseStatus(HttpStatus.CREATED)
 public Map<String, Object> createMenu(
         @AuthenticationPrincipal Jwt jwt,
         @Valid @RequestBody MenuRequest request) {

  long actor = manager(jwt);

  return service.createMenu(
          actor,
          isAdmin(jwt),
          request.restaurantId(),
          request.categoryId(),
          request.name(),
          request.description(),
          request.price(),
          request.foodType(),
          request.available()
  );
 }

 @GetMapping("/menu-items/{id}")
 public Map<String, Object> menuItem(@PathVariable long id) {
  return service.menuItem(id);
 }

 @PutMapping("/menu-items/{id}")
 public Map<String, Object> updateMenu(
         @AuthenticationPrincipal Jwt jwt,
         @PathVariable long id,
         @Valid @RequestBody MenuRequest request) {

  long actor = manager(jwt);

  return service.updateMenu(
          id,
          actor,
          isAdmin(jwt),
          request.restaurantId(),
          request.categoryId(),
          request.name(),
          request.description(),
          request.price(),
          request.foodType(),
          request.available()
  );
 }

 @DeleteMapping("/menu-items/{id}")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void deleteMenu(
         @AuthenticationPrincipal Jwt jwt,
         @PathVariable long id) {

  long actor = manager(jwt);
  service.deleteMenu(id, actor, isAdmin(jwt));
 }

 // Image upload

 @PostMapping({
         "/restaurants/{id}/image",
         "/menu-items/{id}/images"
 })
 public Map<String, String> upload(
         @AuthenticationPrincipal Jwt jwt,
         @PathVariable long id,
         @RequestParam("file") MultipartFile file,
         HttpServletRequest request) {

  long actor = manager(jwt);

  boolean isRestaurant =
          request.getRequestURI().contains("/restaurants/");

  String url = service.upload(
          id,
          isRestaurant,
          actor,
          isAdmin(jwt),
          file
  );

  return Map.of("url", url);
 }

 // Security helpers

 private void checkInternal(String key) {
  if (key == null
          || !MessageDigest.isEqual(
          key.getBytes(StandardCharsets.UTF_8),
          internalKey.getBytes(StandardCharsets.UTF_8))) {

   throw new ResponseStatusException(
           HttpStatus.UNAUTHORIZED,
           "Invalid service credential"
   );
  }
 }

 private long authenticatedUserId(Jwt jwt) {
  if (jwt == null || jwt.getClaim("userId") == null) {
   throw new ResponseStatusException(
           HttpStatus.UNAUTHORIZED,
           "Authenticated user ID is missing"
   );
  }

  try {
   return Long.parseLong(
           jwt.getClaim("userId").toString()
   );
  } catch (NumberFormatException exception) {
   throw new ResponseStatusException(
           HttpStatus.UNAUTHORIZED,
           "Invalid authenticated user ID"
   );
  }
 }

 private long manager(Jwt jwt) {
  if (!isAdmin(jwt)
          && (jwt == null
          || !"ROLE_RESTAURANT_OWNER".equals(
          jwt.getClaimAsString("role")))) {

   throw new ResponseStatusException(
           HttpStatus.FORBIDDEN,
           "Restaurant owner role is required"
   );
  }

  return authenticatedUserId(jwt);
 }

 private boolean isAdmin(Jwt jwt) {
  return jwt != null
          && "ROLE_ADMIN".equals(
          jwt.getClaimAsString("role")
  );
 }

 private void admin(Jwt jwt) {
  if (!isAdmin(jwt)) {
   throw new ResponseStatusException(
           HttpStatus.FORBIDDEN,
           "Administrator role is required"
   );
  }
 }

 // Request DTOs

 public record ReviewRestaurantRequest(
         @NotBlank
         @Pattern(regexp = "APPROVED|REJECTED")
         String decision) {
 }

 public record ReviewRequest(
         @Min(1)
         @Max(5)
         int rating,

         @Size(max = 1000)
         String comment) {
 }

 public record StockRequest(
         @Min(0)
         @Max(100000)
         int quantity) {
 }

 public record CategoryRequest(
         @NotBlank
         @Size(max = 100)
         String name,

         @Size(max = 1000)
         String imageUrl) {
 }

 public record RestaurantRequest(
         @NotBlank
         @Size(max = 160)
         String name,

         @Size(max = 1000)
         String description,

         @NotBlank
         @Size(max = 500)
         String address,

         @NotBlank
         @Size(max = 100)
         String city,

         @Size(max = 30)
         String phoneNumber,

         @NotBlank
         @Pattern(regexp = "OPEN|CLOSED")
         String status) {
 }

 public record MenuRequest(
         @NotNull
         Long restaurantId,

         @NotNull
         Long categoryId,

         @NotBlank
         @Size(max = 160)
         String name,

         @NotBlank
         @Size(max = 1000)
         String description,

         @NotNull
         @DecimalMin("0.01")
         BigDecimal price,

         @NotBlank
         @Pattern(regexp = "VEG|NON_VEG")
         String foodType,

         @NotNull
         Boolean available) {
 }
}
