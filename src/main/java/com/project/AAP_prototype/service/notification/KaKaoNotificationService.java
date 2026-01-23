package com.project.AAP_prototype.service.notification;

import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.Map;

@Async
@Slf4j
@Service
public class KaKaoNotificationService implements NotificationService {
    @Override
    public void send(String status, String message, Map<String, Object> details) {
        try {
            // TODO: 카카오 알림톡 API 연동 로직
            log.info("[카카오 알림] 메시지: {}", message);    
        } catch (Exception e) {
            // TODO: handle exception
        }
        
    }
}