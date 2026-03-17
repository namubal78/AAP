package com.project.AAP_prototype.service;

import com.project.AAP_prototype.repository.PaymentRepository;
import com.project.AAP_prototype.service.notification.NotificationService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 이 임포트가 필요합니다
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import com.project.AAP_prototype.entity.Payment;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j; // 로깅 인터페이스 Slf4j 사용 선언
/*
    성능: 로그를 찍을 때 log.info("값: " + value)처럼 문자열을 더하지 않고, log.info("값: {}", value) 방식을 써서 불필요한 메모리 낭비를 방지

    * 대용량 처리할 때는 스택이 어떻게 바뀌나요?
    데이터가 초당 수만 건씩 들어오는 금융 대용량 시스템에서는 단순히 파일이나 콘솔에 로그를 찍는 것만으로는 부족합니다. 이때는 **"로그 수집 및 분석 스택"**으로 넘어갑니다.

    현재 스택 (단일 서버)
    방식: 로컬 서버 파일(app.log)에 저장
    문제점: 서버가 여러 대면 로그가 분산됨, 검색이 매우 힘듦.

    대용량/운영용 스택 (ELK 또는 EFK 스택)
    Logstash/Fluentd: 서버의 로그 파일을 실시간으로 긁어옵니다.
    Elasticsearch (DB): 긁어온 엄청난 양의 로그를 초고속으로 검색할 수 있게 저장합니다.
    Kibana (Dashboard): 저장된 로그를 그래프나 표로 시각화합니다.

    나중에 대용량이 되면 이 로그를 Elasticsearch 같은 곳으로 쏴주기만 하면 됩니다.
*/
@Slf4j // log.info() 사용 가능하게
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final NotificationService notificationService; // 주입 추가
    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate; // AapPrototypeApplication에서 등록한 Bean 주입

    // .env에 정의된 PORTONE_API_KEY 값을 주입받습니다.
    @Value("${PORTONE_API_KEY}")
    private String apiKey;

    // .env에 정의된 PORTONE_API_SECRET 값을 주입받습니다.
    @Value("${PORTONE_API_SECRET}")
    private String apiSecret;
    
    @Transactional // 선언적 트랜잭션 적용 
    /* 선언적 트랜잭션 
    비즈니스 로직과 트랜잭션 관리 코드를 분리하여, 
    개발자는 핵심 로직에만 집중하고 
    트랜잭션 관리는 프레임워크에 위임하는 기법
    */
    public Payment verifyAndSave(String impUid, String merchantUid, Long amount, String buyerName, HttpServletRequest httpRequest) throws Exception { // throws Exception으로 예외를 던지면, 컨트롤러 메소드는 try-catch로 감싸야 unhandled exception 방지 가능
        
        // 결제 요청 ip 관련 로그
        log.info("[Access Log] IP: {}, Request Path: {}", httpRequest.getRemoteAddr(), httpRequest.getRequestURI());

        // 요청 진입 로그
        log.info("[Payment Request] MerchantUid: {}, ImpUid: {}, Amount: {}", merchantUid, impUid, amount);

        try {
            // TODO: [개발 임시] PortOne V1 REST API가 kakaopay.TC0ONETIME(공용 테스트 MID) 결제를
            //       per-merchant 네임스페이스로 귀속하지 않아 getPaymentDataFromPortOne() 호출 시
            //       항상 404("존재하지 않는 결제정보")를 반환하는 이슈가 있음.
            //       실제 KakaoPay 개발자 테스트 CID 발급 후 아래 주석을 해제하고 DEV_MODE를 제거할 것.
            //
            // [보안 단계 1] String token = getPortOneToken();
            // [보안 단계 2] Map<String, Object> paymentData = getPaymentDataFromPortOne(impUid, token);
            // [보안 단계 3] Long realPaidAmount = Long.valueOf(String.valueOf(paymentData.get("amount")));
            //              if (!amount.equals(realPaidAmount)) throw new Exception("결제 금액 불일치: 보안 위협 감지");

            log.warn("[DEV_MODE] PortOne 사후 검증 우회 중 - 운영 환경에서는 반드시 제거할 것");

            // [보안 단계 4] 검증 완료 시에만 DB 저장
            Payment payment = new Payment();
            payment.setOrderId(merchantUid);
            payment.setAmount(amount);
            payment.setBuyerName(buyerName);
            payment.setStatus("PAID");
            payment.setReceiptUrl(null); // DEV_MODE: PortOne 응답 없으므로 null

            Payment savedPayment = paymentRepository.save(payment);

            // 통합 알림 발송 (슬랙, 메일, SMS 등 한 번에)
            notificationService.send("SUCCESS", "결제 성공 안내", Map.of(
                "주문번호", merchantUid,
                "금액", amount,
                "구매자", buyerName
            ));
            return savedPayment;

        } catch (Exception e) {
            log.error("[Payment Error] 주문번호 {} 처리 중 오류 발생: {}", merchantUid, e.getMessage());
            // 🚨 실패 알림 발송
            notificationService.send("ERROR", "결제 실패 또는 보안 위협 감지: " + e.getMessage(), Map.of(
                "주문번호", merchantUid,
                "접속IP", httpRequest.getRemoteAddr()
            ));
            throw e;
        }
    }

    // 포트원 서버로부터 인증 토큰을 가져오는 로직
    private String getPortOneToken() {
        String url = "https://api.iamport.kr/users/getToken";
        
        // 하드코딩된 API_KEY, API_SECRET 대신 주입받은 필드 변수(apiKey, apiSecret)를 사용합니다.
        Map<String, String> body = Map.of(
            "imp_key", apiKey, 
            "imp_secret", apiSecret
        );
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, body, Map.class);
            Map<String, Object> res = (Map<String, Object>) response.getBody().get("response");
            String token = (String) res.get("access_token");
            log.info("[Token] 발급된 토큰 앞 20자: {}", token != null ? token.substring(0, Math.min(20, token.length())) : "null");
            return token;
        } catch (Exception e) {
            log.error("[Token Error] 포트원 토큰 발급 실패: {}", e.getMessage());
            throw new RuntimeException("인증 토큰을 가져올 수 없습니다.");
        }
    }

    // 결제 고유 번호로 실제 결제 정보를 가져오는 로직
    private Map<String, Object> getPaymentDataFromPortOne(String impUid, String token) {
        String url = "https://api.iamport.kr/payments/" + impUid;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        
        // 포트원의 실제 응답 데이터인 'response' 안의 내용을 반환
        return (Map<String, Object>) response.getBody().get("response");
    }

    @Transactional
    public void refund(String merchantUid, String reason) throws Exception {
        // 1. 포트원 인증 토큰 가져오기 (기존 메서드 재사용)
        String token = getPortOneToken();

        // 2. 포트원 환불 API 호출
        String url = "https://api.iamport.kr/payments/cancel";
        Map<String, String> body = Map.of(
            "merchant_uid", merchantUid,
            "reason", reason
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            // 3. DB 상태 업데이트
            Payment payment = paymentRepository.findByOrderId(merchantUid)
                    .orElseThrow(() -> new RuntimeException("결제 건을 찾을 수 없습니다."));
            payment.setStatus("CANCELLED"); // 상태를 환불됨으로 변경
            paymentRepository.save(payment);

            // 4. 비동기 알림 전송 (슬랙/카카오 등)
            notificationService.send("SUCCESS", "✅ 결제 취소 완료", Map.of(
                "주문번호", merchantUid,
                "취소사유", reason
            ));

        } catch (Exception e) {
            notificationService.send("ERROR", "🚨 환불 처리 중 오류 발생", Map.of("주문번호", merchantUid));
            throw e;
        }
    }
}