package com.jsh.decascale.product.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor
public class Product {
    @Id
    private Long id;

    private String name;
    private int stock;
    private BigDecimal price;

    @Version
    private Long version;

    public Product(Long id, String name, int stock, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.stock = stock;
        this.price = price;
    }

    // 락이 없는 아주 순진한 재고 차감 메서드
    public void decreaseStock(int quantity) {
        if (this.stock - quantity < 0) {
            throw new RuntimeException("재고가 부족합니다.");
        }
        this.stock -= quantity;
    }
}