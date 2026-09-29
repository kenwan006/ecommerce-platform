package com.example.sunridge.warehouse.repository;

import com.example.sunridge.warehouse.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {}
