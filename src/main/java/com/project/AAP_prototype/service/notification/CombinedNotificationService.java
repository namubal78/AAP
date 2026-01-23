package com.project.AAP_prototype.service.notification;

import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

// 이 클래스는 등록된 모든 알림 서비스를 순회하며 한 번에 발송하는 마스터 서비스입니다.

@Service
@Primary // PaymentService에서 주입받을 때 이 통합 서비스가 우선권을 가짐
public class CombinedNotificationService implements NotificationService {
    private final List<NotificationService> notificationServices;

    // 스프링이 NotificationService 인터페이스를 구현한 모든 클래스를 자동으로 리스트에 담아 주입함
    public CombinedNotificationService(List<NotificationService> notificationServices) {
        this.notificationServices = notificationServices;
    }


    /*
        비동기를 적용하면 PaymentService의 동작 방식이 다음과 같이 최적화됩니다.
        결제 검증 및 DB 저장: 기존처럼 동기식으로 빠르게 처리.
        알림 호출: notificationService.send()를 호출하는 즉시, 알림이 다 완료될 때까지 기다리지 않고 다음 코드로 넘어감.
        응답 반환: 사용자에게 즉시 "결제 성공" 응답을 보냄.
        백그라운드: 별도의 쓰레드에서 슬랙, 이메일, SMS 등이 차례로 전송됨.

        💡 주의사항: 비동기 처리와 보안
        예외 처리:
            비동기 쓰레드에서 발생한 에러(예: 슬랙 서버 다운)는 메인 쓰레드인 PaymentService로 전달되지 않습니다. 
            따라서 각 NotificationService 구현체 내부에서 반드시 try-catch로 에러 로그를 남겨야 합니다.
        컨텍스트 유실: 
            비동기 쓰레드는 호출한 쓰레드와 다른 쓰레드이므로, HttpServletRequest 같은 객체를 직접 넘기면 안 됩니다. 
            필요한 데이터(IP 주소 등)는 미리 꺼내서 Map<String, Object> details에 담아 넘겨야 합니다.
    */
    @Async // 이 메서드 호출은 호출자(PaymentService)와 별개의 쓰레드에서 수행
    @Override
    public void send(String status, String message, Map<String, Object> details) {
        // 루프를 돌며 모든 매체(슬랙, SMS 등)로 알림 전송
        for (NotificationService service : notificationServices) {
            // 무한 루프 방지를 위해 자기 자신은 제외
            if (!(service instanceof CombinedNotificationService)) {
                service.send(status, message, details);
            }
        }
    }
}