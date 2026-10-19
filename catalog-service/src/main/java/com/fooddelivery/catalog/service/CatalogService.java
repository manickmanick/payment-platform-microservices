package com.fooddelivery.catalog.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.fooddelivery.catalog.client.OrderEligibilityClient;
import com.fooddelivery.catalog.entity.*;
import com.fooddelivery.catalog.repository.*;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
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

    public CatalogService(CategoryRepository c, RestaurantRepository r, MenuItemRepository i,
                          RestaurantReviewRepository reviews,OrderEligibilityClient orderEligibility,
                          @Value("${cloudinary.cloud-name:}") String cloud,
                          @Value("${cloudinary.api-key:}") String key,
                          @Value("${cloudinary.api-secret:}") String secret,
                          @Value("${catalog.internal-key:local-internal-key-change-me}")String catalogInternalKey) {
        categories=c; restaurants=r; items=i; this.reviews=reviews;this.orderEligibility=orderEligibility;this.catalogInternalKey=catalogInternalKey;
        cloudinary=cloud.isBlank()||key.isBlank()||secret.isBlank()?null:
                new Cloudinary(ObjectUtils.asMap("cloud_name",cloud,"api_key",key,"api_secret",secret,"secure",true));
    }

    public List<Map<String,Object>> categories(){return categories.findAll().stream().map(this::categoryDto).toList();}
    @Transactional public Map<String,Object> createCategory(String name,String image){return categoryDto(categories.save(new Category(name,image)));}
    @Transactional public Map<String,Object> updateCategory(long id,String name,String image){Category c=category(id);c.update(name,image);return categoryDto(c);}
    @Transactional public void deleteCategory(long id){if(items.existsByCategory_Id(id))throw new ResponseStatusException(HttpStatus.CONFLICT,"Category is used by menu items");categories.delete(category(id));}
    public List<Map<String,Object>> restaurants(){return restaurants.findAllByOrderByCreatedAtDesc().stream().filter(r->"APPROVED".equals(r.getApprovalStatus())).map(this::restaurantDto).toList();}
    public Map<String,Object> searchRestaurants(String q,String city,String status,int page,int size){
        validatePage(page,size);var all=restaurants.findAllByOrderByCreatedAtDesc().stream()
                .filter(r->"APPROVED".equals(r.getApprovalStatus()))
                .filter(r->status==null||status.isBlank()||r.getStatus().equalsIgnoreCase(status))
                .filter(r->city==null||city.isBlank()||r.getCity().equalsIgnoreCase(city))
                .filter(r->q==null||q.isBlank()||r.getName().toLowerCase(Locale.ROOT).contains(q.trim().toLowerCase(Locale.ROOT)))
                .toList();int start=Math.min(page*size,all.size()),end=Math.min(start+size,all.size());
        return page(all.subList(start,end).stream().map(this::restaurantDto).toList(),all.size(),page,size);
    }
    public List<Map<String,Object>> pendingRestaurants(){return restaurants.findAllByOrderByCreatedAtDesc().stream().filter(r->"PENDING".equals(r.getApprovalStatus())).map(this::restaurantDto).toList();}
    @Transactional public Map<String,Object> reviewRestaurant(long id,String decision){if(!Set.of("APPROVED","REJECTED").contains(decision))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Decision must be APPROVED or REJECTED");Restaurant r=restaurantEntity(id);r.setApprovalStatus(decision);return restaurantDto(r);}
    public Map<String,Object> restaurant(long id){Restaurant r=restaurantEntity(id);if(!"APPROVED".equals(r.getApprovalStatus()))throw new EntityNotFoundException("Restaurant not found");return restaurantDto(r);}
    @Transactional public Map<String,Object> createRestaurant(long owner,String n,String d,String a,String c,String p,String status){return restaurantDto(restaurants.save(new Restaurant(n,d,a,c,p,status,owner)));}
    @Transactional public Map<String,Object> updateRestaurant(long id,long actor,boolean admin,String n,String d,String a,String c,String p,String status){Restaurant r=restaurantEntity(id);authorize(r,actor,admin);r.update(n,d,a,c,p,status);return restaurantDto(r);}
    @Transactional public void deleteRestaurant(long id,long actor,boolean admin){Restaurant r=restaurantEntity(id);authorize(r,actor,admin);items.deleteByRestaurant_Id(id);restaurants.delete(r);}
    public List<Map<String,Object>> menu(long restaurantId){Restaurant r=restaurantEntity(restaurantId);if(!"APPROVED".equals(r.getApprovalStatus())||!"OPEN".equals(r.getStatus()))return List.of();return items.findByRestaurant_IdOrderByName(restaurantId).stream().map(this::menuDto).toList();}
    public Map<String,Object> searchMenu(String q,Long restaurantId,Long categoryId,Boolean available,String foodType,BigDecimal minPrice,BigDecimal maxPrice,int page,int size){
        validatePage(page,size);if(minPrice!=null&&maxPrice!=null&&minPrice.compareTo(maxPrice)>0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"minPrice must not exceed maxPrice");
        var result=items.search(clean(q),restaurantId,categoryId,available,clean(foodType),minPrice,maxPrice,PageRequest.of(page,size));
        return page(result.getContent().stream().map(this::menuDto).toList(),result.getTotalElements(),page,size);
    }
    @Transactional public Map<String,Object> createMenu(long actor,boolean admin,long restaurantId,long categoryId,String n,String d,BigDecimal p,String f,Boolean a){Restaurant r=restaurantEntity(restaurantId);authorize(r,actor,admin);Category c=category(categoryId);return menuDto(items.save(new MenuItem(r,c,n,d,p,f,a)));}
    public Map<String,Object> menuItem(long id){MenuItem i=item(id);if(!"APPROVED".equals(i.getRestaurant().getApprovalStatus())||!"OPEN".equals(i.getRestaurant().getStatus()))throw new EntityNotFoundException("Menu item not found");return menuDto(i);}
    @Transactional public Map<String,Object> updateMenu(long id,long actor,boolean admin,long restaurantId,long categoryId,String n,String d,BigDecimal p,String f,Boolean a){MenuItem i=item(id);authorize(i.getRestaurant(),actor,admin);Restaurant r=restaurantEntity(restaurantId);authorize(r,actor,admin);i.update(r,category(categoryId),n,d,p,f,a);return menuDto(i);}
    @Transactional public Map<String,Object> setStock(long id,long actor,boolean admin,int quantity){if(quantity<0||quantity>100000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Stock must be between 0 and 100000");MenuItem i=item(id);authorize(i.getRestaurant(),actor,admin);i.setStockQuantity(quantity);return menuDto(i);}
    @Transactional public void reserveStock(long id,int quantity){if(quantity<1)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Quantity must be positive");MenuItem i=items.findByIdForUpdate(id).orElseThrow(()->new EntityNotFoundException("Menu item not found"));if(!Boolean.TRUE.equals(i.getAvailable())||i.getStockQuantity()<quantity)throw new ResponseStatusException(HttpStatus.CONFLICT,"Insufficient stock for menu item "+id);i.setStockQuantity(i.getStockQuantity()-quantity);}
    @Transactional public void releaseStock(long id,int quantity){MenuItem i=items.findByIdForUpdate(id).orElseThrow(()->new EntityNotFoundException("Menu item not found"));i.setStockQuantity(Math.min(100000,i.getStockQuantity()+Math.max(0,quantity)));}
    @Transactional public void deleteMenu(long id,long actor,boolean admin){MenuItem i=item(id);authorize(i.getRestaurant(),actor,admin);items.delete(i);}
    public Map<String,Object> reviewList(long restaurantId){restaurantEntity(restaurantId);var rows=reviews.findByRestaurantIdOrderByCreatedAtDesc(restaurantId).stream().map(r->Map.<String,Object>of("id",r.getId(),"userId",r.getUserId(),"rating",r.getRating(),"comment",Objects.toString(r.getComment(),""),"createdAt",r.getCreatedAt())).toList();return Map.of("content",rows,"totalElements",rows.size(),"averageRating",Objects.toString(reviews.averageForRestaurant(restaurantId),"0"));}
    @Transactional public Map<String,Object> addReview(long restaurantId,long userId,int rating,String comment){Restaurant restaurant=restaurantEntity(restaurantId);if(rating<1||rating>5)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Rating must be between 1 and 5");if(reviews.findByRestaurantIdAndUserId(restaurantId,userId).isPresent())throw new ResponseStatusException(HttpStatus.CONFLICT,"You already reviewed this restaurant");try{if(!Boolean.TRUE.equals(orderEligibility.eligible(userId,restaurantId,catalogInternalKey).get("eligible")))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Reviews are available after a delivered order");}catch(ResponseStatusException e){throw e;}catch(Exception e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Could not validate a delivered order",e);}RestaurantReview saved=reviews.save(new RestaurantReview(restaurantId,userId,rating,comment));long count=reviews.countForRestaurant(restaurantId);BigDecimal average=BigDecimal.valueOf(reviews.averageForRestaurant(restaurantId)).setScale(2,java.math.RoundingMode.HALF_UP);restaurant.refreshRating(average,(int)count);return Map.of("id",saved.getId(),"restaurantId",restaurantId,"userId",userId,"rating",rating,"comment",Objects.toString(comment,""),"averageRating",average,"reviewCount",count);}
    @Transactional public String upload(long id,boolean isRestaurant,long actor,boolean admin,MultipartFile file){if(cloudinary==null)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Cloudinary is not configured");if(file.isEmpty()||file.getContentType()==null||!file.getContentType().startsWith("image/"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A valid image file is required");if(file.getSize()>8L*1024*1024)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Image exceeds 8 MB");try{String url=Objects.toString(cloudinary.uploader().upload(file.getBytes(),ObjectUtils.asMap("folder","food-delivery","resource_type","image")).get("secure_url"));if(isRestaurant){Restaurant r=restaurantEntity(id);authorize(r,actor,admin);r.setImageUrl(url);}else{MenuItem i=item(id);authorize(i.getRestaurant(),actor,admin);i.setImageUrl(url);}return url;}catch(ResponseStatusException|EntityNotFoundException e){throw e;}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Image upload failed",e);}}
    private void validatePage(int page,int size){if(page<0||size<1||size>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Page must be non-negative and size must be between 1 and 100");}
    private Map<String,Object> page(List<?> rows,long total,int page,int size){return Map.of("content",rows,"totalElements",total,"totalPages",(total+size-1)/size,"page",page,"size",size);}
    private String clean(String s){return s==null||s.isBlank()?null:s.trim();}
    private Category category(long id){return categories.findById(id).orElseThrow(()->new EntityNotFoundException("Category not found"));}
    private Restaurant restaurantEntity(long id){return restaurants.findById(id).orElseThrow(()->new EntityNotFoundException("Restaurant not found"));}
    private MenuItem item(long id){return items.findById(id).orElseThrow(()->new EntityNotFoundException("Menu item not found"));}
    private void authorize(Restaurant r,long actor,boolean admin){if(!admin&&!Objects.equals(r.getOwnerId(),actor))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You do not own this restaurant");}
    private Map<String,Object> categoryDto(Category c){Map<String,Object> m=new LinkedHashMap<>();m.put("id",c.getId());m.put("name",c.getName());m.put("imageUrl",c.getImageUrl());return m;}
    private Map<String,Object> restaurantDto(Restaurant r){Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getId());m.put("ownerId",r.getOwnerId());m.put("name",r.getName());m.put("description",r.getDescription());m.put("address",r.getAddress());m.put("city",r.getCity());m.put("phoneNumber",r.getPhoneNumber());m.put("status",r.getStatus());m.put("approvalStatus",r.getApprovalStatus());m.put("imageUrl",r.getImageUrl());m.put("averageRating",r.getAverageRating());m.put("reviewCount",r.getReviewCount());m.put("createdAt",r.getCreatedAt());return m;}
    private Map<String,Object> menuDto(MenuItem i){Map<String,Object> m=new LinkedHashMap<>();m.put("id",i.getId());m.put("restaurantId",i.getRestaurant().getId());m.put("categoryId",i.getCategory().getId());m.put("categoryName",i.getCategory().getName());m.put("name",i.getName());m.put("description",i.getDescription());m.put("price",i.getPrice());m.put("foodType",i.getFoodType());m.put("available",Boolean.TRUE.equals(i.getAvailable())&&i.getStockQuantity()>0);m.put("stockQuantity",i.getStockQuantity());m.put("averageRating",i.getAverageRating());m.put("reviewCount",i.getReviewCount());m.put("imageUrl",i.getImageUrl());return m;}
}
