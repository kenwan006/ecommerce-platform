package com.example.sunridge.warehouse.repository;

import com.example.sunridge.warehouse.entity.WarehouseOutboxDeadLetter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseOutboxDeadLetterRepository
    extends JpaRepository<WarehouseOutboxDeadLetter, Long> {}
