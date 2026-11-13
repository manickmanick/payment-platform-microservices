package com.order.order_service.dto.menu;

import java.util.List;

public record MenuItemBulkRequest(

        List<Long> menuItemIds
) {
}
