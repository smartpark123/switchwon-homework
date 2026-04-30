package com.switchwon.forex.order.controller;

import com.switchwon.forex.common.response.ApiResponse;
import com.switchwon.forex.order.dto.OrderListResponse;
import com.switchwon.forex.order.dto.OrderRequest;
import com.switchwon.forex.order.dto.OrderResponse;
import com.switchwon.forex.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.createOrder(request)));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<OrderListResponse>> getOrderList() {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderList()));
    }
}
