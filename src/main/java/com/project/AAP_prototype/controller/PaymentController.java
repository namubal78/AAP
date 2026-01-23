package com.project.AAP_prototype.controller;

import com.project.AAP_prototype.entity.Payment;
import com.project.AAP_prototype.repository.PaymentRepository;
import com.project.AAP_prototype.service.PaymentService; // Service 임포트
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map; // Map 임포트 추가

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class PaymentController {

    private final PaymentRepository paymentRepository; // Repository 주입
    private final PaymentService paymentService; // Service 주입
    
    // 1-1. 테스트) 결제 정보 저장 API (프론트에서 버튼 누르면 호출됨)
    /*
    @PostMapping("/save")
    public ResponseEntity<Payment> savePayment(@RequestBody Payment payment) {
        // 모의 결제이므로 상태를 바로 'PAID'로 설정해서 저장해 보겠습니다.
        payment.setStatus("PAID"); 
        Payment savedPayment = paymentRepository.save(payment);
        return ResponseEntity.ok(savedPayment);
    }
    */

    // 1-2. 디벨롭) 백엔드: 결제 사후 검증 로직 구현
    @PostMapping("/verify")
    // public ResponseEntity<?> verifyAndSave(@RequestBody Map<String, String> request) { // fe 에서 숫자를 보내올 경우 대비하여 object로 변경로 수정
    public ResponseEntity<?> verifyAndSave(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            // 0. 프론트에서 보낸 데이터 추출
            String impUid = (String) request.get("imp_uid");
            String merchantUid = (String) request.get("merchant_uid");
            Long amount = Long.valueOf(String.valueOf(request.get("amount")));
            String buyerName = (String) request.get("buyerName");

            // 1. [보안 핵심] 포트원 API를 통해 실제 결제 내역 조회 (SDK 또는 RestTemplate 사용)
            // logic: getPaymentInfoFromPortOne(impUid);
            // - 여기서 impUid를 가지고 포트원 서버에 "진짜 1000원 결제됐니?"라고 물어봐야 합니다.
            // boolean isValid = portOneService.verify(impUid, amount);
            
            // 2-1. [사후 검증] DB의 상품 가격과 실제 결제 금액 대조
            // long realAmount = paymentInfo.getAmount(); 
            // if (realAmount != expectedAmount) return Error;

            // 2-2. 검증 완료 후 저장
            /*
            Payment payment = new Payment();
            payment.setOrderId(merchantUid);
            payment.setAmount(amount);
            payment.setBuyerName(buyerName);
            payment.setStatus("PAID"); // 검증 후 상태 변경
            */

            // 2-3. 디벨롭) 2-1, 202 합쳐서 verifyAndSave로 Service를 통한 사후 검증 및 저장 (핵심 보안 로직)
            /*
            관심사 분리: 컨트롤러는 요청만 받고, 실제 **보안 검증(금액 대조)**은 Service에서 처리하도록 구조를 개선했습니다.
            */
            Payment validatedPayment = paymentService.verifyAndSave(impUid, merchantUid, amount, buyerName, httpRequest);
            return ResponseEntity.ok(validatedPayment);

        } catch (Exception e) { // 예외 처리: 결제 금액이 다르거나 API 통신에 실패할 경우
            // 검증 실패 시 400 에러와 메시지 반환 (보안상 필수)
            return ResponseEntity.badRequest().body("결제 보안 검증 실패: " +e.getMessage());
        }
    }

    // 전체 결제 내역 최신순 조회 API
    @GetMapping("/list")
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentRepository.findAllByOrderByCreatedAtDesc());
    }

    // 환불(결제 취소) 요청 처리 API
    @PostMapping("/refund")
    public ResponseEntity<String> refundPayment(@RequestBody Map<String, String> request) {
        try {
            // 프론트엔드 App.js에서 보낸 merchantUid와 reason을 추출합니다.
            String merchantUid = request.get("merchantUid");
            String reason = request.get("reason");

            // PaymentService에 이미 작성해둔 refund 로직을 호출합니다.
            paymentService.refund(merchantUid, reason);
            
            return ResponseEntity.ok("환불 처리가 완료되었습니다.");
        } catch (Exception e) {
            // 환불 실패 시 에러 메시지 반환
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("환불 실패: " + e.getMessage());
        }
    }
}