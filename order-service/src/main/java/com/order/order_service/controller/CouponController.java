package com.order.order_service.controller;

import com.order.order_service.entity.Coupon;
import com.order.order_service.repository.CouponRepository;
import com.order.order_service.service.CouponService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/coupons")
public class CouponController {
    private final CouponService service; private final CouponRepository coupons;
    public CouponController(CouponService service,CouponRepository coupons){this.service=service;this.coupons=coupons;}
    @GetMapping("/{code}/quote") public Map<String,Object> quote(@PathVariable String code,@RequestParam BigDecimal subtotal,@AuthenticationPrincipal Jwt jwt){long uid=Long.parseLong(jwt.getClaim("userId").toString());return Map.of("code",code,"discount",service.quote(code,subtotal,uid));}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody CreateCoupon c){admin(jwt);String code=c.code().trim().toUpperCase(Locale.ROOT);if(c.startsAt().isAfter(c.expiresAt()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Coupon start must be before expiry");if(coupons.findByCode(code).isPresent())throw new ResponseStatusException(HttpStatus.CONFLICT,"Coupon code already exists");if(!c.discountType().equals("FIXED")&&!c.discountType().equals("PERCENT"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"discountType must be FIXED or PERCENT");if("PERCENT".equals(c.discountType())&&c.discountValue().compareTo(new BigDecimal("100"))>0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Percentage discount cannot exceed 100");Coupon saved=coupons.save(new Coupon(code,c.discountType(),c.discountValue(),c.maxDiscount(),c.minOrderAmount(),c.usageLimit(),c.startsAt(),c.expiresAt()));return Map.of("id",saved.getId(),"code",saved.getCode(),"discountType",saved.getDiscountType(),"discountValue",saved.getDiscountValue(),"usageLimit",saved.getUsageLimit()==null?0:saved.getUsageLimit());}
    private void admin(Jwt jwt){if(!"ROLE_ADMIN".equals(jwt.getClaimAsString("role")))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Administrator role is required");}
    public record CreateCoupon(@NotBlank @Size(max=40)String code,@NotBlank String discountType,@NotNull @DecimalMin("0.01")BigDecimal discountValue,@DecimalMin("0.01")BigDecimal maxDiscount,@NotNull @DecimalMin("0.00")BigDecimal minOrderAmount,@Min(1)Integer usageLimit,@NotNull LocalDateTime startsAt,@NotNull LocalDateTime expiresAt){}
}
