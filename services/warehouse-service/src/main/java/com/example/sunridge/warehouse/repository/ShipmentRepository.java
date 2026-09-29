package com.example.sunridge.warehouse.repository;

import com.example.sunridge.warehouse.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {}
