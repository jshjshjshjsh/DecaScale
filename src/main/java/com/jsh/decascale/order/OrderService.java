package com.jsh.decascale.order;

import com.jsh.decascale.order.domain.Order;
import com.jsh.decascale.product.domain.Product;
import com.jsh.decascale.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public Page<Order> getOrdersNaive(int page, int size) {
        // created_at 기준으로 최신순 정렬해서 가져오기
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderRepository.findAll(pageRequest);
    }

    public List<Order> getOrdersByIdMath(int page, int size) {
        // 1. 기준점이 될 최신 ID 조회
        Long maxId = orderRepository.findMaxId();
        if (maxId == null) {
            return Collections.emptyList();
        }

        // 2. 대형 커뮤니티식 ID 수학 공식 적용
        // 공식: 최신ID - (원하는페이지 * 페이지당사이즈)
        // 예: maxId=10,000,000 / page=0(1p), size=10 -> targetId = 10,000,000 (1000만부터 10개)
        // 예: maxId=10,000,000 / page=6(7p), size=10 -> targetId = 9,999,940  (999만9940부터 10개)
        long targetId = maxId - ((long) page * size);

        // 만약 유저가 말도 안 되게 큰 페이지를 넣어서 targetId가 0 이하로 떨어지면 빈 리스트 반환
        if (targetId <= 0) {
            return Collections.emptyList();
        }

        // 3. 딱 size만큼만 가져오도록 PageRequest 설정 (OFFSET은 0으로 고정!)
        PageRequest pageable = PageRequest.of(0, size);

        return orderRepository.findOrdersByIdMath(targetId, pageable);
    }

    @Transactional
    public void createOrder(Long userId, Long productId, String requestId) {
        // 1. 상품 조회 (락 없음)
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품 없음"));

        // 2. 재고 차감 (여기서 100명이 동시에 10개짜리 재고를 통과함)
        product.decreaseStock(1);

        // 3. 주문 생성 (여기서 한 유저가 따닥 버튼 누른 게 그대로 다 들어감)
        Order order = Order.builder()
                .userId(userId)
                .productId(productId)
                .requestId(requestId)
                .orderStatus("PAYMENT_WAIT")
                .totalAmount(product.getPrice())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        orderRepository.save(order);
    }
}