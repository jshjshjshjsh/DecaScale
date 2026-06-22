package com.jsh.decascale.order;

import com.jsh.decascale.order.domain.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    @Query("SELECT MAX(o.id) FROM Order o")
    Long findMaxId();

    @Query("SELECT o FROM Order o WHERE o.id <= :targetId ORDER BY o.id DESC")
    List<Order> findOrdersByIdMath(@Param("targetId") Long targetId, Pageable pageable);

    List<Order> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);
}
