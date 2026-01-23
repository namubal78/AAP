package com.project.AAP_prototype.service.notification;

import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.Map;

@Async
@Slf4j
@Service
public class EmailNotificationService implements NotificationService {
    @Override
    public void send(String status, String message, Map<String, Object> details) {
        try {
            // TODO: JavaMailSender 연동 로직
            log.info("[이메일 알림] 상태: {}, 수신자: {}", status, details.get("buyerName"));    
        } catch (Exception e) {
            // TODO: handle exception
        }
        
    }
}