package com.example.sunridge.warehouse.repository;

import com.example.sunridge.warehouse.entity.Warehouse;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
  Optional<Warehouse> findByCode(String code);
}
