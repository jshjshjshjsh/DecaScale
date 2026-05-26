package com.jsh.decascale.order;

import com.jsh.decascale.order.domain.Order;
import com.jsh.decascale.order.domain.OrderRequestDto;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    // 무식한 Offset 기반 페이징 API
    @GetMapping("/api/v1/orders/naive")
    public Page<Order> getOrdersNaive(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return orderService.getOrdersNaive(page, size);
    }

    // 개선된 페이징 API
    @GetMapping("/api/v2/orders")
    public List<Order> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return orderService.getOrdersByIdMath(page, size);
    }

    @PostMapping("/api/orders")
    public String createOrder(@org.springframework.web.bind.annotation.RequestBody OrderRequestDto request) {
        orderService.createOrder(request.getUserId(), request.getProductId());
        return "SUCCESS";
    }
}