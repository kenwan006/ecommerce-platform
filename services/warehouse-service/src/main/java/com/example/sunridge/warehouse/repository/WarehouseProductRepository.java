package com.example.sunridge.warehouse.repository;

import com.example.sunridge.warehouse.entity.WarehouseProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseProductRepository extends JpaRepository<WarehouseProduct, Long> {}
