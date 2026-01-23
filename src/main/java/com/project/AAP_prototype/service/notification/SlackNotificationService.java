package com.project.AAP_prototype.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackNotificationService implements NotificationService {
    private final RestTemplate restTemplate; // AapPrototypeApplication에 등록된 빈 사용

    @Value("${slack.webhook.url}") // application.yaml의 설정값 로드
    private String slackWebhookUrl;

    @Override
    // 개별 전송도 비동기로 처리하여 효율성 극대화
    @Async("taskExecutor") // AsyncConfig의 @Bean(name = "taskExecutor")와 일치해야 함
    // 명시적인 ThreadPool을 사용하는 것이 금융 시스템에선 더 안전합니다.
    public void send(String status, String message, Map<String, Object> details) {
        // 비동기 쓰레드 시작 로그 (디버깅용)
        log.info("[{}] 쓰레드에서 슬랙 전송 시작", Thread.currentThread().getName());
        log.debug("[Async-Slack] 전송 프로세스 시작: {}", message);

        try {
            Map<String, Object> payload = new HashMap<>();
            String color = "SUCCESS".equals(status) ? "#36a64f" : "#ff0000";

            Map<String, Object> attachment = new HashMap<>();
            attachment.put("color", color);
            attachment.put("title", "SUCCESS".equals(status) ? "✅ 결제 정상 처리" : "🚨 보안/예외 알림");
            attachment.put("text", message);

            if (details != null && !details.isEmpty()) { // 데이터가 하나도 없을 때 스트림 로직이 도는 것을 방지하여 아주 미세한 성능까지 최적화
                List<Map<String, Object>> fields = details.entrySet().stream().map(entry -> {
                    Map<String, Object> field = new HashMap<>();
                    field.put("title", entry.getKey());
                    field.put("value", String.valueOf(entry.getValue()));
                    field.put("short", true);
                    return field;
                }).toList();
                attachment.put("fields", fields);
            }

            payload.put("attachments", List.of(attachment));

            restTemplate.postForEntity(slackWebhookUrl, new HttpEntity<>(payload), String.class);
            log.info("[Async-Slack] 알림 전송 완료");

        } catch (Exception e) {
            // 비동기 쓰레드이므로 여기서 예외를 잡지 않으면 에러가 증발합니다.
            /*
                log.error에 e 포함: e.getMessage()만 찍는 것보다 e 객체를 마지막에 넣어주면 
                어디서 에러가 났는지 Stack Trace 전체를 볼 수 있어 금융 사고 분석 시 매우 유리합니다.
            */
            log.error("[Async-Slack-Error] 슬랙 전송 중 심각한 오류 발생! 사유: {}", e.getMessage(), e);
        }
    }
}