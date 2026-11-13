package com.order.order_service.service;

import com.order.order_service.client.CatalogClient;
import com.order.order_service.client.PaymentClient;
import com.order.order_service.dto.*;
import com.order.order_service.dto.menu.MenuItemBulkRequest;
import com.order.order_service.dto.menu.MenuItemResponse;
import com.order.order_service.entity.*;
import com.order.order_service.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import feign.FeignException;


@Service
public class CheckoutService {
    private static final BigDecimal DELIVERY_FEE = new BigDecimal("49.00");
    private static final BigDecimal FREE_DELIVERY_THRESHOLD = new BigDecimal("499.00");
    private static final BigDecimal TAX_RATE = new BigDecimal("0.05");
    private final CartItemRepository carts;
    private final OrderItemRepository items;
    private final OrderRepository orders;
    private final OrderService orderService;
    private final PaymentClient payments;
    private final CatalogClient catalog;
    private final CouponService coupons;
    private final String catalogInternalKey;

    public CheckoutService(CartItemRepository c, OrderItemRepository i, OrderRepository o, OrderService s, PaymentClient p, CatalogClient catalog, CouponService coupons, @Value("${catalog-service.internal-key:local-internal-key-change-me}") String key) {
        carts = c;
        items = i;
        orders = o;
        orderService = s;
        payments = p;
        this.catalog = catalog;
        this.coupons = coupons;
        catalogInternalKey = key;
    }

    @Transactional
    public Map<String, Object> checkout(long uid, String email, String address, String key, String token, String couponCode, BigDecimal deliveryLatitude, BigDecimal deliveryLongitude, String customerPhone, boolean smsOptIn) {
        if ((deliveryLatitude == null) != (deliveryLongitude == null))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both delivery coordinates must be supplied together");
        if (smsOptIn && (customerPhone == null || !customerPhone.matches("^\\+[1-9]\\d{7,14}$")))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid E.164 phone number is required for SMS updates");
        var cart = carts.findByUserIdOrderById(uid);
        if (cart.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Your cart is empty");

        try {

            // collect all menu item ids
            List<Long> menuItemIds = cart.stream()
                    .map(CartItem::getMenuItemId)
                    .distinct()
                    .toList();
//            for (CartItem c : cart) {
                // old approach below:

//                Map<String, Object> current = catalog.getMenuItem(c.getMenuItemId(), "Bearer " + token);
//                if (!Boolean.TRUE.equals(current.get("available")))
//                    throw new ResponseStatusException(HttpStatus.CONFLICT, "A cart item is no longer available");
//                c.refresh(((Number) current.get("restaurantId")).longValue(), Objects.toString(current.get("name")), new BigDecimal(current.get("price").toString()));

//            }


            // 2. Call Catalog Service only once
            List<MenuItemResponse> menuItems =
                    catalog.getMenuItems(
                            new MenuItemBulkRequest(menuItemIds),
                            "Bearer " + token
                    );

            // 3. Create a lookup map
            Map<Long, MenuItemResponse> menuItemMap = menuItems.stream()
                    .collect(Collectors.toMap(
                            MenuItemResponse::id,
                            item -> item
                    ));

            // 4. Refresh cart items
            for (CartItem c : cart) {

                MenuItemResponse current =
                        menuItemMap.get(c.getMenuItemId());

                if (current == null) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_GATEWAY,
                            "Catalog response is missing a menu item"
                    );
                }

//                c.refresh(
//                        current.restaurantId(),
//                        current.name(),
//                        current.price()
//                );

                c.refresh(((Number) current.restaurantId()).longValue(), Objects.toString(current.name()), new BigDecimal(current.price().toString()));
            }


        } catch (ResponseStatusException e) {
            throw e;
        }catch (FeignException e) {

            HttpStatus status = HttpStatus.resolve(e.status());

            if (status != null) {
                throw new ResponseStatusException(
                        status,
                        "Catalog Service: " + e.getMessage()
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not communicate with Catalog Service",
                    e
            );
        }
        catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not validate cart items", e);
        }

        if (cart.stream().map(CartItem::getRestaurantId).distinct().count() > 1)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "All cart items must come from one restaurant");
        BigDecimal subtotal = cart.stream().map(CartItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        String idem = key == null || key.isBlank() ? UUID.randomUUID().toString() : key.trim();
        if (idem.length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key cannot exceed 100 characters");
        Order order = orders.findByIdempotencyKey(idem).orElse(null);
        if (order != null) {
            if (!order.getUserId().equals(uid))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key belongs to another account");
            if (order.getStatus() == OrderStatus.CANCELLED)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This checkout was cancelled; start a new checkout with a new idempotency key");
            return payment(order, idem, uid, token);
        }
        BigDecimal discount = couponCode == null || couponCode.isBlank() ? BigDecimal.ZERO : coupons.quote(couponCode, subtotal, uid);
        BigDecimal deliveryFee = subtotal.compareTo(FREE_DELIVERY_THRESHOLD) >= 0 ? BigDecimal.ZERO : DELIVERY_FEE;
        BigDecimal taxable = subtotal.subtract(discount).max(BigDecimal.ZERO);
        BigDecimal tax = taxable.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = taxable.add(tax).add(deliveryFee).setScale(2, RoundingMode.HALF_UP);
        List<CartItem> reserved = new ArrayList<>();
        try {
            for (CartItem c : cart) {
                catalog.reserve(c.getMenuItemId(), c.getQuantity(), catalogInternalKey);
                reserved.add(c);
            }
            order = orderService.createOrder(new CreateOrderRequest(uid, total));
            order.setIdempotencyKey(idem);
            order.setDeliveryAddress(address.trim());
            order.setCustomerEmail(email);
            order.setCustomerPhone(customerPhone);
            order.setSmsOptIn(smsOptIn);
            order.setDeliveryLatitude(deliveryLatitude);
            order.setDeliveryLongitude(deliveryLongitude);
            order.setSubtotal(subtotal);
            order.setDiscountAmount(discount);
            order.setDeliveryFee(deliveryFee);
            order.setTaxAmount(tax);
            order.setCouponCode(couponCode == null || couponCode.isBlank() ? null : couponCode.trim().toUpperCase(Locale.ROOT));
            order.setStockReserved(true);
            order = orders.save(order);
            for (CartItem c : cart)
                items.save(new OrderItem(order.getId(), c.getMenuItemId(), c.getRestaurantId(), c.getItemName(), c.getUnitPrice(), c.getQuantity()));
            if (order.getCouponCode() != null) coupons.redeem(order.getCouponCode(), subtotal, uid, order.getId());
            Map<String, Object> result = payment(order, idem, uid, token);
            carts.deleteByUserId(uid);
            result.put("subtotal", subtotal);
            result.put("discount", discount);
            result.put("deliveryFee", deliveryFee);
            result.put("tax", tax);
            result.put("total", total);
            return result;
        } catch (ResponseStatusException e) {
            release(reserved);
            throw e;
        } catch (Exception e) {
            release(reserved);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Checkout failed", e);
        }
    }

    private Map<String, Object> payment(
            Order order,
            String idem,
            long uid,
            String token) {

        try {
            PaymentResponse pay = payments.createPayment(
                    new PaymentRequest(
                            uid,
                            order.getId(),
                            order.getAmount(),
                            "INR",
                            false,
                            "order-" + idem
                    ),
                    "Bearer " + token
            );

            if (pay == null || pay.clientSecret() == null) {
                throw new IllegalStateException(
                        "Payment service did not return a client secret"
                );
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("orderId", order.getId());
            response.put("paymentId", pay.paymentId());
            response.put("status", pay.status());
            response.put("clientSecret", pay.clientSecret());
            response.put("subtotal", order.getSubtotal());
            response.put("discount", order.getDiscountAmount());
            response.put("deliveryFee", order.getDeliveryFee());
            response.put("tax", order.getTaxAmount());
            response.put("total", order.getAmount());

            return response;

        } catch (Exception e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Payment initialization status is unknown. Please retry.",
                    e
            );
        }
    }

    private void release(List<CartItem> rows) {
        for (CartItem c : rows)
            try {
                catalog.release(c.getMenuItemId(), c.getQuantity(), catalogInternalKey);
            } catch (Exception ignored) {
            }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> resumePayment(
            long orderId,
            long userId,
            String token
    ) {

        // 1. Find the existing order
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found"
                ));

        // 2. Verify order ownership
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not allowed to access this order"
            );
        }

        // 3. Only pending orders can resume payment
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment cannot be resumed for this order. Current order status: "
                            + order.getStatus()
            );
        }

        // 4. Retrieve the existing PaymentIntent from Payment Service
        try {
            Map<String, Object> paymentResponse =
                    payments.resumePayment(
                            orderId,
                            "Bearer " + token
                    );

            // 5. Payment was already successful
            if ("ALREADY_PAID".equals(paymentResponse.get("status"))) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "This order has already been paid"
                );
            }

            // 6. Return existing payment session to frontend
            Map<String, Object> response = new LinkedHashMap<>();

            response.put("orderId", order.getId());
            response.put("paymentId", paymentResponse.get("paymentId"));
            response.put("amount", order.getAmount());
            response.put("currency", paymentResponse.get("currency"));
            response.put("status", paymentResponse.get("status"));
            response.put("clientSecret", paymentResponse.get("clientSecret"));

            return response;

        } catch (ResponseStatusException exception) {
            throw exception;
        }
    }
}
