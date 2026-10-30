package com.order.order_service.controller;

import com.order.order_service.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
public class CartController {
    private final CartService service;

    public CartController(CartService s) {
        service = s;
    }

    @GetMapping
    public Object get(@AuthenticationPrincipal Jwt jwt) {
        return service.get(uid(jwt));
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public Object add(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddItem x) {
        return service.add(uid(jwt), x.menuItemId(), x.quantity(), jwt.getTokenValue());
    }

    @PatchMapping("/items/{id}")
    public Object update(@AuthenticationPrincipal Jwt jwt, @PathVariable long id, @Valid @RequestBody Quantity x) {
        return service.update(uid(jwt), id, x.quantity());
    }

    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        service.remove(uid(jwt), id);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@AuthenticationPrincipal Jwt jwt) {
        service.clear(uid(jwt));
    }

    private long uid(Jwt jwt) {
        return Long.parseLong(jwt.getClaim("userId").toString());
    }

    public record AddItem(@NotNull Long menuItemId, @NotNull @Min(1) @Max(99) Integer quantity) {
    }

    public record Quantity(@NotNull @Min(1) @Max(99) Integer quantity) {
    }
}
