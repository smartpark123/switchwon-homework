package com.switchwon.forex.order.repository;

import com.switchwon.forex.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
