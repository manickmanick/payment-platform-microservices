
package com.fooddelivery.catalog.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.fooddelivery.catalog.client.OrderEligibilityClient;
import com.fooddelivery.catalog.entity.*;
import com.fooddelivery.catalog.repository.*;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final CategoryRepository categories;
    private final RestaurantRepository restaurants;
    private final MenuItemRepository items;
    private final RestaurantReviewRepository reviews;
    private final OrderEligibilityClient orderEligibility;
    private final String catalogInternalKey;
    private final Cloudinary cloudinary;

    public CatalogService(
            CategoryRepository categories,
            RestaurantRepository restaurants,
            MenuItemRepository items,
            RestaurantReviewRepository reviews,
            OrderEligibilityClient orderEligibility,
            @Value("${cloudinary.cloud-name:}") String cloud,
            @Value("${cloudinary.api-key:}") String key,
            @Value("${cloudinary.api-secret:}") String secret,
            @Value("${catalog.internal-key:local-internal-key-change-me}")
            String catalogInternalKey) {

        this.categories = categories;
        this.restaurants = restaurants;
        this.items = items;
        this.reviews = reviews;
        this.orderEligibility = orderEligibility;
        this.catalogInternalKey = catalogInternalKey;

        this.cloudinary = cloud.isBlank() || key.isBlank() || secret.isBlank()
                ? null
                : new Cloudinary(
                ObjectUtils.asMap(
                        "cloud_name", cloud,
                        "api_key", key,
                        "api_secret", secret,
                        "secure", true
                )
        );
    }

    public List<Map<String, Object>> categories() {
        return categories.findAll()
                .stream()
                .map(this::categoryDto)
                .toList();
    }

    @Transactional
    public Map<String, Object> createCategory(String name, String image) {
        return categoryDto(categories.save(new Category(name, image)));
    }

    @Transactional
    public Map<String, Object> updateCategory(
            long id, String name, String image) {

        Category category = category(id);
        category.update(name, image);
        return categoryDto(category);
    }

    @Transactional
    public void deleteCategory(long id) {
        if (items.existsByCategory_Id(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Category is used by menu items"
            );
        }

        categories.delete(category(id));
    }

    public List<Map<String, Object>> restaurants() {
        return restaurants.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(r -> "APPROVED".equals(r.getApprovalStatus()))
                .map(this::restaurantDto)
                .toList();
    }

    public Map<String, Object> searchRestaurants(
            String q,
            String city,
            String status,
            int page,
            int size) {

        validatePage(page, size);

        var all = restaurants.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(r -> "APPROVED".equals(r.getApprovalStatus()))
                .filter(r -> status == null || status.isBlank()
                        || r.getStatus().equalsIgnoreCase(status))
                .filter(r -> city == null || city.isBlank()
                        || r.getCity().equalsIgnoreCase(city))
                .filter(r -> q == null || q.isBlank()
                        || r.getName().toLowerCase(Locale.ROOT)
                        .contains(q.trim().toLowerCase(Locale.ROOT)))
                .toList();

        int start = Math.min(page * size, all.size());
        int end = Math.min(start + size, all.size());

        return page(
                all.subList(start, end)
                        .stream()
                        .map(this::restaurantDto)
                        .toList(),
                all.size(),
                page,
                size
        );
    }

    public List<Map<String, Object>> pendingRestaurants() {
        return restaurants.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(r -> "PENDING".equals(r.getApprovalStatus()))
                .map(this::restaurantDto)
                .toList();
    }

    @Transactional
    public Map<String, Object> reviewRestaurant(long id, String decision) {
        if (!Set.of("APPROVED", "REJECTED").contains(decision)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Decision must be APPROVED or REJECTED"
            );
        }

        Restaurant restaurant = restaurantEntity(id);
        restaurant.setApprovalStatus(decision);
        return restaurantDto(restaurant);
    }

    public Map<String, Object> restaurant(long id) {
        Restaurant restaurant = restaurantEntity(id);

        if (!"APPROVED".equals(restaurant.getApprovalStatus())) {
            throw new EntityNotFoundException("Restaurant not found");
        }

        return restaurantDto(restaurant);
    }

    @Transactional
    public Map<String, Object> createRestaurant(
            long owner,
            String name,
            String description,
            String address,
            String city,
            String phone,
            String status) {

        return restaurantDto(
                restaurants.save(
                        new Restaurant(
                                name, description, address, city,
                                phone, status, owner
                        )
                )
        );
    }

    @Transactional
    public Map<String, Object> updateRestaurant(
            long id,
            long actor,
            boolean admin,
            String name,
            String description,
            String address,
            String city,
            String phone,
            String status) {

        Restaurant restaurant = restaurantEntity(id);
        authorize(restaurant, actor, admin);
        restaurant.update(
                name, description, address, city, phone, status
        );

        return restaurantDto(restaurant);
    }

    @Transactional
    public void deleteRestaurant(long id, long actor, boolean admin) {
        Restaurant restaurant = restaurantEntity(id);
        authorize(restaurant, actor, admin);
        items.deleteByRestaurant_Id(id);
        restaurants.delete(restaurant);
    }

    public List<Map<String, Object>> menu(long restaurantId) {
        Restaurant restaurant = restaurantEntity(restaurantId);

        if (!"APPROVED".equals(restaurant.getApprovalStatus())
                || !"OPEN".equals(restaurant.getStatus())) {
            return List.of();
        }

        return items.findByRestaurant_IdOrderByName(restaurantId)
                .stream()
                .map(this::menuDto)
                .toList();
    }

    public Map<String, Object> searchMenu(
            String q,
            Long restaurantId,
            Long categoryId,
            Boolean available,
            String foodType,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size) {

        validatePage(page, size);

        if (minPrice != null && maxPrice != null
                && minPrice.compareTo(maxPrice) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "minPrice must not exceed maxPrice"
            );
        }

        var result = items.search(
                clean(q),
                restaurantId,
                categoryId,
                available,
                clean(foodType),
                minPrice,
                maxPrice,
                PageRequest.of(page, size)
        );

        return page(
                result.getContent().stream()
                        .map(this::menuDto)
                        .toList(),
                result.getTotalElements(),
                page,
                size
        );
    }

    @Transactional
    public Map<String, Object> createMenu(
            long actor,
            boolean admin,
            long restaurantId,
            long categoryId,
            String name,
            String description,
            BigDecimal price,
            String foodType,
            Boolean available) {

        Restaurant restaurant = restaurantEntity(restaurantId);
        authorize(restaurant, actor, admin);

        Category category = category(categoryId);

        return menuDto(
                items.save(
                        new MenuItem(
                                restaurant, category, name,
                                description, price, foodType, available
                        )
                )
        );
    }

    public Map<String, Object> menuItem(long id) {
        MenuItem item = item(id);

        if (!"APPROVED".equals(item.getRestaurant().getApprovalStatus())
                || !"OPEN".equals(item.getRestaurant().getStatus())) {
            throw new EntityNotFoundException("Menu item not found");
        }

        return menuDto(item);
    }

    @Transactional
    public Map<String, Object> updateMenu(
            long id,
            long actor,
            boolean admin,
            long restaurantId,
            long categoryId,
            String name,
            String description,
            BigDecimal price,
            String foodType,
            Boolean available) {

        MenuItem menuItem = item(id);
        authorize(menuItem.getRestaurant(), actor, admin);

        Restaurant restaurant = restaurantEntity(restaurantId);
        authorize(restaurant, actor, admin);

        menuItem.update(
                restaurant,
                category(categoryId),
                name,
                description,
                price,
                foodType,
                available
        );

        return menuDto(menuItem);
    }

    @Transactional
    public Map<String, Object> setStock(
            long id, long actor, boolean admin, int quantity) {

        if (quantity < 0 || quantity > 100000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Stock must be between 0 and 100000"
            );
        }

        MenuItem menuItem = item(id);
        authorize(menuItem.getRestaurant(), actor, admin);
        menuItem.setStockQuantity(quantity);

        return menuDto(menuItem);
    }

    @Transactional
    public void reserveStock(long id, int quantity) {
        if (quantity < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Quantity must be positive"
            );
        }

        MenuItem menuItem = items.findByIdForUpdate(id)
                .orElseThrow(
                        () -> new EntityNotFoundException(
                                "Menu item not found"
                        )
                );

        if (!Boolean.TRUE.equals(menuItem.getAvailable())
                || menuItem.getStockQuantity() < quantity) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Insufficient stock for menu item " + id
            );
        }

        menuItem.setStockQuantity(
                menuItem.getStockQuantity() - quantity
        );
    }

    @Transactional
    public void releaseStock(long id, int quantity) {
        MenuItem menuItem = items.findByIdForUpdate(id)
                .orElseThrow(
                        () -> new EntityNotFoundException(
                                "Menu item not found"
                        )
                );

        menuItem.setStockQuantity(
                Math.min(
                        100000,
                        menuItem.getStockQuantity()
                                + Math.max(0, quantity)
                )
        );
    }

    @Transactional
    public void deleteMenu(long id, long actor, boolean admin) {
        MenuItem menuItem = item(id);
        authorize(menuItem.getRestaurant(), actor, admin);
        items.delete(menuItem);
    }

    public Map<String, Object> reviewList(long restaurantId) {
        restaurantEntity(restaurantId);

        var rows = reviews
                .findByRestaurantIdOrderByCreatedAtDesc(restaurantId)
                .stream()
                .map(review -> Map.<String, Object>of(
                        "id", review.getId(),
                        "userId", review.getUserId(),
                        "rating", review.getRating(),
                        "comment", Objects.toString(
                                review.getComment(), ""
                        ),
                        "createdAt", review.getCreatedAt()
                ))
                .toList();

        return Map.of(
                "content", rows,
                "totalElements", rows.size(),
                "averageRating", Objects.toString(
                        reviews.averageForRestaurant(restaurantId), "0"
                )
        );
    }

    @Transactional
    public Map<String, Object> addReview(
            long restaurantId,
            long userId,
            int rating,
            String comment) {

        Restaurant restaurant = restaurantEntity(restaurantId);

        if (rating < 1 || rating > 5) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rating must be between 1 and 5"
            );
        }

        if (reviews.findByRestaurantIdAndUserId(
                restaurantId, userId
        ).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You already reviewed this restaurant"
            );
        }

        try {
            if (!Boolean.TRUE.equals(
                    orderEligibility.eligible(
                            userId, restaurantId, catalogInternalKey
                    ).get("eligible"))) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Reviews are available after a delivered order"
                );
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Could not validate a delivered order",
                    exception
            );
        }

        // Fix: convert the validated int rating to Byte.
        RestaurantReview saved = reviews.save(
                new RestaurantReview(
                        restaurantId,
                        userId,
                        Byte.valueOf((byte) rating),
                        comment
                )
        );

        long count = reviews.countForRestaurant(restaurantId);

        BigDecimal average = BigDecimal.valueOf(
                reviews.averageForRestaurant(restaurantId)
        ).setScale(2, RoundingMode.HALF_UP);

        restaurant.refreshRating(average, (int) count);

        return Map.of(
                "id", saved.getId(),
                "restaurantId", restaurantId,
                "userId", userId,
                "rating", rating,
                "comment", Objects.toString(comment, ""),
                "averageRating", average,
                "reviewCount", count
        );
    }

    @Transactional
    public String upload(
            long id,
            boolean isRestaurant,
            long actor,
            boolean admin,
            MultipartFile file) {

        if (cloudinary == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Cloudinary is not configured"
            );
        }

        if (file.isEmpty()
                || file.getContentType() == null
                || !file.getContentType().startsWith("image/")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A valid image file is required"
            );
        }

        if (file.getSize() > 8L * 1024 * 1024) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Image exceeds 8 MB"
            );
        }

        try {
            String url = Objects.toString(
                    cloudinary.uploader()
                            .upload(
                                    file.getBytes(),
                                    ObjectUtils.asMap(
                                            "folder", "food-delivery",
                                            "resource_type", "image"
                                    )
                            )
                            .get("secure_url")
            );

            if (isRestaurant) {
                Restaurant restaurant = restaurantEntity(id);
                authorize(restaurant, actor, admin);
                restaurant.setImageUrl(url);
            } else {
                MenuItem menuItem = item(id);
                authorize(menuItem.getRestaurant(), actor, admin);
            }

            return url;
        } catch (ResponseStatusException
                 | EntityNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Image upload failed",
                    exception
            );
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 100"
            );
        }
    }

    private Map<String, Object> page(
            List<?> rows, long total, int page, int size) {

        return Map.of(
                "content", rows,
                "totalElements", total,
                "totalPages", (total + size - 1) / size,
                "page", page,
                "size", size
        );
    }

    private String clean(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

    private Category category(long id) {
        return categories.findById(id)
                .orElseThrow(
                        () -> new EntityNotFoundException(
                                "Category not found"
                        )
                );
    }

    private Restaurant restaurantEntity(long id) {
        return restaurants.findById(id)
                .orElseThrow(
                        () -> new EntityNotFoundException(
                                "Restaurant not found"
                        )
                );
    }

    private MenuItem item(long id) {
        return items.findById(id)
                .orElseThrow(
                        () -> new EntityNotFoundException(
                                "Menu item not found"
                        )
                );
    }

    private void authorize(
            Restaurant restaurant, long actor, boolean admin) {

        if (!admin && !Objects.equals(
                restaurant.getOwnerId(), actor
        )) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not own this restaurant"
            );
        }
    }

    private Map<String, Object> categoryDto(Category category) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", category.getId());
        result.put("name", category.getName());
        result.put("imageUrl", category.getImageUrl());
        return result;
    }

    private Map<String, Object> restaurantDto(Restaurant restaurant) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", restaurant.getId());
        result.put("ownerId", restaurant.getOwnerId());
        result.put("name", restaurant.getName());
        result.put("description", restaurant.getDescription());
        result.put("address", restaurant.getAddress());
        result.put("city", restaurant.getCity());
        result.put("phoneNumber", restaurant.getPhoneNumber());
        result.put("status", restaurant.getStatus());
        result.put("approvalStatus", restaurant.getApprovalStatus());
        result.put("imageUrl", restaurant.getImageUrl());
        result.put("averageRating", restaurant.getAverageRating());
        result.put("reviewCount", restaurant.getReviewCount());
        result.put("createdAt", restaurant.getCreatedAt());
        return result;
    }

    private Map<String, Object> menuDto(MenuItem item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId());
        result.put("restaurantId", item.getRestaurant().getId());
        result.put("categoryId", item.getCategory().getId());
        result.put("categoryName", item.getCategory().getName());
        result.put("name", item.getName());
        result.put("description", item.getDescription());
        result.put("price", item.getPrice());
        result.put("foodType", item.getFoodType());
        result.put(
                "available",
                Boolean.TRUE.equals(item.getAvailable())
                        && item.getStockQuantity() > 0
        );
        result.put("stockQuantity", item.getStockQuantity());
        result.put("averageRating", item.getAverageRating());
        result.put("reviewCount", item.getReviewCount());
        return result;
    }
}
