package com.example.sunridge.payment.repository;

import com.example.sunridge.payment.entity.OutboxDeadLetter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxDeadLetterRepository extends JpaRepository<OutboxDeadLetter, Long> {}
