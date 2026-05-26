package com.jsh.decascale.product;

import com.jsh.decascale.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
//    -- 테이블이 없으면 자동 생성되겠지만, 데이터는 수동으로 넣어줘야 함
//    INSERT INTO products (id, name, stock, price) VALUES (777, '오픈런특가 네스프레소 머신', 10, 150000);
//    INSERT INTO products (id, name, stock, price) VALUES (888, '아르페지오 캡슐 1슬리브', 100, 8000);
}