package com.jsh.decascale.order;

import com.jsh.decascale.product.ProductRepository;
import com.jsh.decascale.product.domain.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class DataBombRunner implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void run(String... args) throws Exception {
         decaInsert();
        // concurrencyInsert();
        // RedisInsert();
    }

    private void RedisInsert(){
        if (productRepository.count() == 0) {
            System.out.println("K6 동시성 폭격용 타겟 세팅 (DB + Redis)...");

            productRepository.save(new Product(777L, "오픈런특가 네스프레소 머신", 10, BigDecimal.valueOf(150000)));
            productRepository.save(new Product(888L, "아르페지오 캡슐 1슬리브", 100, BigDecimal.valueOf(8000)));

            redisTemplate.opsForValue().set("product:stock:777", "10");
            redisTemplate.opsForValue().set("product:stock:888", "100");

            System.out.println("Redis 메모리 방어막 장전 완료! 발사 준비 끝!");
        }
    }

    private void concurrencyInsert(){
        if (productRepository.count() == 0) {
            System.out.println("K6 동시성 폭격용 타겟(상품) 생성 시작...");

            // 1. 오픈런 타겟: 777번 (낙관적 락 테스트용, 재고 10개)
            productRepository.save(new Product(777L, "오픈런특가 네스프레소 머신", 10, BigDecimal.valueOf(150000)));

            // 2. 따닥 방어 타겟: 888번 (유니크 인덱스 테스트용, 재고 100개)
            productRepository.save(new Product(888L, "아르페지오 캡슐 1슬리브", 100, BigDecimal.valueOf(8000)));

            System.out.println("폭격 타겟 세팅 완료! K6 발사 준비 끝!");
        }
    }

    private void decaInsert(){
        // 이미 데이터가 있는지 확인해서 중복 삽입 방지
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Long.class);
        if (count != null && count > 0) {
            System.out.println("이미 데이터가 존재합니다. 폭격 코드를 건너뜁니다.");
            return;
        }

        int totalRows = 10_000_000;
        int batchSize = 10_000; // 1만 건씩 묶어서 Insert
        String sql = "INSERT INTO orders (user_id, product_id, order_status, total_amount, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";

        List<Object[]> batchArgs = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        String[] statuses = {"PAYMENT_WAIT", "SHIPPING", "COMPLETED", "CANCEL"};
        Random random = new Random();

        System.out.println("1,000만 건 데이터 입력 시작...");

        for (int i = 1; i <= totalRows; i++) {
            batchArgs.add(new Object[]{
                    (long) random.nextInt(100_000) + 1,
                    (long) random.nextInt(5_000) + 1,
                    statuses[random.nextInt(4)],
                    BigDecimal.valueOf(random.nextInt(100_000) + 1000),
                    LocalDateTime.now().minusDays(random.nextInt(365)),
                    LocalDateTime.now()
            });

            if (i % batchSize == 0) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
                batchArgs.clear();
                System.out.println("현재 삽입 완료: " + i + "개... (" + (i * 100 / totalRows) + "%)");
            }
        }

        long endTime = System.currentTimeMillis();
        System.out.println("폭격 완료! 총 소요 시간: " + (endTime - startTime) / 1000 + "초");
    }
}