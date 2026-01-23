package com.project.AAP_prototype.service.notification;

import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.Map;

@Async
@Slf4j
@Service
public class SmsNotificationService implements NotificationService {
    @Override
    public void send(String status, String message, Map<String, Object> details) {
        try {
            // TODO: SMS 서비스 API 연동 로직
            log.info("[SMS 알림] 상태: {}, 내용: {}", status, message);    
        } catch (Exception e) {
            // TODO: handle exception
        }
    }
}