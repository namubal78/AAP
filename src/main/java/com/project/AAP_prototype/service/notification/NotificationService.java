package com.project.AAP_prototype.service.notification;

import java.util.Map;

/**
 * 모든 알림 서비스의 공통 인터페이스
 * 
 * 왜 이렇게 하나요? (금융 보안 관점)
 * 이렇게 인터페이스를 먼저 만드는 이유는 "결제 로직과 알림 로직의 분리" 때문입니다.
 *
 * 교체 용이성: 나중에 SMS 업체를 바꾸거나 카카오톡 대신 라인을 쓰게 되더라도, PaymentService의 결제 코드는 수정할 필요가 없습니다.
 *
 * 일관성: 모든 알림 매체가 status, message, details라는 동일한 데이터 규격을 사용하게 강제함으로써 코드의 안정성을 높입니다.
 */
public interface NotificationService {
    /**
     * 알림 전송 공통 규격
     * @param status SUCCESS(성공) 또는 ERROR(실패/예외)
     * @param message 알림에 표시할 주요 메시지
     * @param details 주문번호, 금액, 사용자 등 상세 정보 데이터
     */
    void send(String status, String message, Map<String, Object> details);
}