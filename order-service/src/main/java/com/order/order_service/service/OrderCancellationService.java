package com.order.order_service.service;

import com.order.order_service.client.CatalogClient;
import com.order.order_service.client.PaymentClient;
import com.order.order_service.entity.Order;
import com.order.order_service.entity.OrderStatus;
import com.order.order_service.repository.OrderItemRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderCancellationService {
    private final OrderService orders;private final OrderItemRepository items;private final PaymentClient payments;private final CatalogClient catalog;private final CouponService coupons;private final String internalKey;private final String paymentInternalKey;
    public OrderCancellationService(OrderService orders,OrderItemRepository items,PaymentClient payments,CatalogClient catalog,CouponService coupons,@Value("${catalog-service.internal-key:local-internal-key-change-me}")String key,@Value("${payment-service.internal-key:local-payment-key-change-me}")String paymentInternalKey){this.orders=orders;this.items=items;this.payments=payments;this.catalog=catalog;this.coupons=coupons;internalKey=key;this.paymentInternalKey=paymentInternalKey;}
    @Transactional
    public Map<String,Object> cancel(long orderId,long userId,String bearer){Order order=orders.getOrder(orderId);if(!order.getUserId().equals(userId))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Cannot cancel another user's order");if(order.getStatus()==OrderStatus.CANCELLED)return Map.of("orderId",orderId,"status","CANCELLED","refundStatus","UNCHANGED");if(!java.util.Set.of(OrderStatus.PENDING,OrderStatus.CONFIRMED,OrderStatus.ACCEPTED).contains(order.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"This order can no longer be cancelled");Map<String,Object> refund=payments.refundOrder(orderId,"Bearer "+bearer,paymentInternalKey);Object state=refund.get("status");if("PENDING".equals(state)||"PROCESSING".equals(state))throw new ResponseStatusException(HttpStatus.CONFLICT,"Refund is still processing; retry cancellation after payment status updates");orders.cancelOrder(orderId);if(order.isStockReserved()){for(var line:items.findByOrderIdOrderById(orderId))try{catalog.release(line.getMenuItemId(),line.getQuantity(),internalKey);}catch(Exception ignored){}order.setStockReserved(false);}coupons.release(orderId);Map<String,Object> response=new LinkedHashMap<>();response.put("orderId",orderId);response.put("status","CANCELLED");response.put("refundStatus",state);response.put("refundId",refund.getOrDefault("refundId",""));return response;}
}
