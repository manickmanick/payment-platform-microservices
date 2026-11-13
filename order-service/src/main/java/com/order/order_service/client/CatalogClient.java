package com.order.order_service.client;

import java.util.List;
import java.util.Map;

import com.order.order_service.dto.menu.MenuItemBulkRequest;
import com.order.order_service.dto.menu.MenuItemResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "catalog-service", url = "${catalog-service.url:http://localhost:8086}")
public interface CatalogClient {
    @GetMapping("/menu-items/{id}")
    Map<String, Object> getMenuItem(@PathVariable("id") Long id, @RequestHeader("Authorization") String authorization);

    @GetMapping("/restaurants/{id}")
    Map<String, Object> getRestaurant(@PathVariable("id") Long id);

    @PostMapping("/internal/menu-items/{id}/reserve")
    void reserve(@PathVariable("id") Long id, @RequestParam("quantity") int quantity, @RequestHeader("X-Internal-Key") String key);

    @PostMapping("/internal/menu-items/{id}/release")
    void release(@PathVariable("id") Long id, @RequestParam("quantity") int quantity, @RequestHeader("X-Internal-Key") String key);


    @PostMapping("/api/catalog/menu-items/bulk")
    List<MenuItemResponse> getMenuItems(@RequestBody MenuItemBulkRequest request, @RequestHeader("Authorization") String token);
}
