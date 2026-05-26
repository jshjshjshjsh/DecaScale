package com.jsh.decascale.order.domain;

import lombok.Getter;

@Getter
public class OrderRequestDto {
    private Long userId;
    private Long productId;
    private String requestId;
}