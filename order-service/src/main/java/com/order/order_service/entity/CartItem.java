package com.order.order_service.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items", uniqueConstraints = @UniqueConstraint(name = "uk_cart_user_item", columnNames = {"user_id", "menu_item_id"}))
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "menu_item_id", nullable = false)
    private Long menuItemId;
    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;
    @Column(name = "item_name", nullable = false, length = 160)
    private String itemName;
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;
    @Column(nullable = false)
    private Integer quantity;
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected CartItem() {
    }

    public CartItem(long u, long m, long r, String n, BigDecimal p, int q) {
        userId = u;
        menuItemId = m;
        restaurantId = r;
        itemName = n;
        unitPrice = p;
        quantity = q;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getMenuItemId() {
        return menuItemId;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public String getItemName() {
        return itemName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(int n) {
        quantity = n;
    }

    public void refresh(long r, String n, BigDecimal p) {
        restaurantId = r;
        itemName = n;
        unitPrice = p;
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
