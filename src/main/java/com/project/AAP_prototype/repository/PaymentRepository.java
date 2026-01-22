package com.project.AAP_prototype.repository;

import com.project.AAP_prototype.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // 기본 저장, 조회 기능이 자동으로 포함됩니다.
}