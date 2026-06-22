package com.jsh.decascale.order;

import com.jsh.decascale.exception.OutOfStockException;
import com.jsh.decascale.order.domain.Order;
import com.jsh.decascale.product.domain.Product;
import com.jsh.decascale.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> stockScript;

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

    @Transactional
    public void createOrderWithRetry(Long userId, Long productId, String requestId) {
        int retryCount = 0;

        while (true) {
            try {
                retryCount++;
                executeOrderLogic(userId, productId, requestId);

                log.info("[성공] 유저 {}가 {}번 재시도 끝에 주문 성공!", userId, retryCount);
                break;

            } catch (Exception e) {
                // 유니크 인덱스든 낙관적 락이든 상관없이 모든 에러가 터지면
                // 포기 안 하고 0.05초 쉬었다가 좀비처럼 계속 다시 들이받음!
                log.warn("[충돌 발생] 유저 {} -> {}번째 재시도 중... 에러: {}", userId, retryCount, e.getClass().getSimpleName());
                try { Thread.sleep(50); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
    }

    // 💡 실제 로직은 별도 메서드로 분리 (트래픽 경합을 위해 락 없이 조회+수정)
    public void executeOrderLogic(Long userId, Long productId, String requestId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품 없음"));

        product.decreaseStock(1);

        Order order = Order.builder()
                .userId(userId)
                .productId(productId)
                .requestId(requestId)
                .orderStatus("PAYMENT_WAIT")
                .totalAmount(product.getPrice())
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();

        orderRepository.save(order);
    }

    @Transactional
    public void createOrderWithRedis(Long userId, Long productId, String requestId) {
        // 💡 1단계: DB 가기 전에 Redis 메모리에서 먼저 입구컷 시도! (단 0.001초 컷)
        String stockKey = "product:stock:" + productId;
        Long result = redisTemplate.execute(stockScript, Collections.singletonList(stockKey));

        if (result == null || result == 0L) {
            log.warn("[Redis 입구컷] 유저 {} 튕김! (재고 소진)", userId);
            throw new OutOfStockException("재고가 모두 소진되었습니다.");
        }

        // 💡 2단계: Redis를 통과한 '선택받은 자(10명)'만 DB에 접근해서 주문서 작성
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품 없음"));

        // 낙관적 락으로 최후의 DB 정합성(Layer 4) 방어
        product.decreaseStock(1);

        Order order = Order.builder()
                .userId(userId)
                .productId(productId)
                .requestId(requestId)
                .orderStatus("PAYMENT_WAIT")
                .totalAmount(product.getPrice())
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();

        orderRepository.save(order);
        log.info("[주문 완료] 유저 {} 성공적으로 결제 안착!", userId);
    }

    public List<Order> getOrdersByUserId(Long userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByIdDesc(userId, pageable);
    }
}