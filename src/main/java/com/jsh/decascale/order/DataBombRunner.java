package com.jsh.decascale.order;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
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

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        // 이미 데이터가 있는지 확인해서 중복 삽입 방지
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Long.class);
        if (count != null && count > 0) {
            System.out.println("✅ 이미 데이터가 존재합니다. 폭격 코드를 건너뜁니다.");
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