package com.order.order_service.service;

import com.order.order_service.entity.Coupon;
import com.order.order_service.entity.CouponRedemption;
import com.order.order_service.repository.CouponRedemptionRepository;
import com.order.order_service.repository.CouponRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CouponService {
    private final CouponRepository coupons;
    private final CouponRedemptionRepository redemptions;
    public CouponService(CouponRepository coupons,CouponRedemptionRepository redemptions){this.coupons=coupons;this.redemptions=redemptions;}

    @Transactional(readOnly=true)
    public BigDecimal quote(String code,BigDecimal subtotal,long userId){return discount(validate(code,subtotal,userId,false),subtotal);}

    @Transactional
    public BigDecimal redeem(String code,BigDecimal subtotal,long userId,long orderId){Coupon coupon=validate(code,subtotal,userId,true);if(redemptions.existsByCouponIdAndUserId(coupon.getId(),userId))throw new ResponseStatusException(HttpStatus.CONFLICT,"Coupon has already been redeemed by this account");coupon.incrementUsage();redemptions.save(new CouponRedemption(coupon.getId(),userId,orderId));return discount(coupon,subtotal);}

    private Coupon validate(String raw,BigDecimal subtotal,long userId,boolean lock){String code=raw==null?"":raw.trim().toUpperCase(Locale.ROOT);Coupon c=(lock?coupons.findByCodeForUpdate(code):coupons.findByCode(code)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Coupon not found"));LocalDateTime now=LocalDateTime.now();if(!Boolean.TRUE.equals(c.getActive())||now.isBefore(c.getStartsAt())||now.isAfter(c.getExpiresAt()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Coupon is not currently active");if(subtotal.compareTo(c.getMinOrderAmount())<0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Order does not meet coupon minimum");if(c.getUsageLimit()!=null&&c.getUsageCount()>=c.getUsageLimit())throw new ResponseStatusException(HttpStatus.CONFLICT,"Coupon usage limit reached");if(redemptions.existsByCouponIdAndUserId(c.getId(),userId))throw new ResponseStatusException(HttpStatus.CONFLICT,"Coupon has already been redeemed by this account");return c;}

    private BigDecimal discount(Coupon c,BigDecimal subtotal){BigDecimal result="PERCENT".equals(c.getDiscountType())?subtotal.multiply(c.getDiscountValue()).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP):c.getDiscountValue();if(c.getMaxDiscount()!=null)result=result.min(c.getMaxDiscount());return result.min(subtotal).setScale(2,RoundingMode.HALF_UP);}
    @Transactional public void release(Long orderId){redemptions.findByOrderId(orderId).ifPresent(r->{coupons.findById(r.getCouponId()).ifPresent(Coupon::decrementUsage);redemptions.delete(r);});}
}
