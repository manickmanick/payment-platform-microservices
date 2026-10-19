package com.order.order_service.dto; import java.math.BigDecimal; import java.util.List;
public record DeliveryRequest(Long orderId,Long userId,String customerEmail,String customerPhone,boolean smsOptIn,String deliveryAddress,BigDecimal totalAmount,BigDecimal deliveryLatitude,BigDecimal deliveryLongitude,List<Item> items){public record Item(String itemName,int quantity){}}
