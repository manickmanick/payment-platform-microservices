package com.fooddelivery.catalog.controller;


import com.fooddelivery.catalog.dto.menu.MenuItemBulkRequest;
import com.fooddelivery.catalog.dto.menu.MenuItemResponse;
import com.fooddelivery.catalog.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalog/menu-items")
public class CatalogController {


    private final CatalogService menuItemService;

    public CatalogController(CatalogService menuItemService) {
        this.menuItemService = menuItemService;
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<MenuItemResponse>> getMenuItems( @Valid @RequestBody MenuItemBulkRequest request){

        List<MenuItemResponse> response = menuItemService.getMenuItems(request);

        return ResponseEntity.ok(response);
    }
}
