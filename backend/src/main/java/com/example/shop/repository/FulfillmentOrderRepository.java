package com.example.shop.repository;

import com.example.shop.entity.FulfillmentOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FulfillmentOrderRepository extends JpaRepository<FulfillmentOrder, Long> {
  Optional<FulfillmentOrder> findByOrderId(Long orderId);
  List<FulfillmentOrder> findByStatusOrderByIdAsc(String status);
}
