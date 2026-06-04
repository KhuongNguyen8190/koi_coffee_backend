package com.koicoffee.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {
    private String staffName;
    private Integer totalPrice;
    private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {
        private Long id; // Tương ứng với ID của Product (Món ăn)
        private Integer quantity;
        private Integer price;
        private String note;
    }
}