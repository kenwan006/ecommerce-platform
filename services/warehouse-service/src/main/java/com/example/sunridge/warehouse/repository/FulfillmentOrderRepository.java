package com.example.sunridge.warehouse.repository;

import com.example.sunridge.warehouse.entity.FulfillmentOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FulfillmentOrderRepository extends JpaRepository<FulfillmentOrder, Long> {
  Optional<FulfillmentOrder> findByCommerceOrderId(Long commerceOrderId);

  List<FulfillmentOrder> findByStatusOrderByIdAsc(String status);
}
