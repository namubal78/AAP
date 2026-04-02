package com.project.AAP_prototype.repository;

import com.project.AAP_prototype.entity.Payment;

import java.util.Optional;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
// 1. 프론트엔드 목록 표시용: 모든 결제 내역을 최신순으로 가져오기
    List<Payment> findAllByOrderByCreatedAtDesc();

    // 2. 환불 처리용: 주문번호(merchant_uid)로 특정 결제 건 찾기
    Optional<Payment> findByOrderId(String orderId);

    // 3. 멱등성 체크용: 주문번호 존재 여부 확인
    boolean existsByOrderId(String orderId);
}