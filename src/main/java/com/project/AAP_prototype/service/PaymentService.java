package com.project.AAP_prototype.service;

import com.project.AAP_prototype.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 이 임포트가 필요합니다
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import com.project.AAP_prototype.entity.Payment;
import lombok.RequiredArgsConstructor;
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

    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate; // AapPrototypeApplication에서 등록한 Bean 주입

    // 포트원 관리자 콘솔에서 확인 가능한 키 (보안을 위해 나중에 환경변수로 빼는 것이 좋습니다)
    private final String API_KEY = "7767620761143508";
    private final String API_SECRET = "bFR54lStXwfYuPF4ivstqvvN0u5neGtRfWAtQHOabhMkfpWrhIo66EvU0fgGtDHDdzVQqFQLkwoQCPe8";

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
            // [보안 단계 1] 포트원 서버와 통신하기 위한 Access Token 발급
            // 별도로 RestTemplate Bean 등록도 필요함(AapPrototypeApplication.java 혹은 별도 Config 클래스)
            String token = getPortOneToken();

            // [보안 단계 2] 발급받은 토큰으로 포트원 서버에서 실제 결제 내역 조회
            Long realPaidAmount = getAmountFromPortOne(impUid, token);

            // [보안 단계 3] 금액 대조 (프론트에서 보낸 금액 vs 실제 결제된 금액)
            if (!amount.equals(realPaidAmount)) {
                throw new Exception("결제 금액 불일치: 보안 위협 감지");
            }

            // [보안 단계 4] 검증 완료 시에만 DB 저장
            Payment payment = new Payment();
            payment.setOrderId(merchantUid);
            payment.setAmount(amount);
            payment.setBuyerName(buyerName);
            payment.setStatus("PAID"); 
            
            return paymentRepository.save(payment);
        } catch (Exception e) {
            log.error("[Payment Error] 주문번호 {} 처리 중 오류 발생: {}", merchantUid, e.getMessage());
            throw e;
        }
    }

    // 포트원 서버로부터 인증 토큰을 가져오는 로직
    private String getPortOneToken() {
        String url = "https://api.iamport.kr/users/getToken";
        Map<String, String> body = Map.of("imp_key", API_KEY, "imp_secret", API_SECRET);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(url, body, Map.class);
        Map<String, Object> res = (Map<String, Object>) response.getBody().get("response");
        return (String) res.get("access_token");
    }

    // 결제 고유 번호로 실제 결제 정보를 가져오는 로직
    private Long getAmountFromPortOne(String impUid, String token) {
        String url = "https://api.iamport.kr/payments/" + impUid;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        Map<String, Object> res = (Map<String, Object>) response.getBody().get("response");
        
        return Long.valueOf(String.valueOf(res.get("amount")));
    }
}