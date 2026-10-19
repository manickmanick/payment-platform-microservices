package com.order.order_service.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name="coupons")
public class Coupon {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,unique=true,length=40) private String code;
    @Column(name="discount_type",nullable=false,length=16) private String discountType;
    @Column(name="discount_value",nullable=false,precision=12,scale=2) private BigDecimal discountValue;
    @Column(name="max_discount",precision=12,scale=2) private BigDecimal maxDiscount;
    @Column(name="min_order_amount",nullable=false,precision=12,scale=2) private BigDecimal minOrderAmount;
    @Column(name="usage_limit") private Integer usageLimit;
    @Column(name="usage_count",nullable=false) private Integer usageCount=0;
    @Column(name="starts_at",nullable=false) private LocalDateTime startsAt;
    @Column(name="expires_at",nullable=false) private LocalDateTime expiresAt;
    @Column(nullable=false) private Boolean active=true;
    protected Coupon(){}
    public Coupon(String code,String type,BigDecimal value,BigDecimal cap,BigDecimal minimum,Integer limit,LocalDateTime starts,LocalDateTime expires){this.code=code;discountType=type;discountValue=value;maxDiscount=cap;minOrderAmount=minimum;usageLimit=limit;startsAt=starts;expiresAt=expires;}
    public Long getId(){return id;}public String getCode(){return code;}public String getDiscountType(){return discountType;}public BigDecimal getDiscountValue(){return discountValue;}public BigDecimal getMaxDiscount(){return maxDiscount;}public BigDecimal getMinOrderAmount(){return minOrderAmount;}public Integer getUsageLimit(){return usageLimit;}public Integer getUsageCount(){return usageCount;}public LocalDateTime getStartsAt(){return startsAt;}public LocalDateTime getExpiresAt(){return expiresAt;}public Boolean getActive(){return active;}public void incrementUsage(){usageCount++;}public void decrementUsage(){usageCount=Math.max(0,usageCount-1);}
}
