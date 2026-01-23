package com.project.AAP_prototype.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync // 중요: 메인 클래스에 이게 없다면 여기서라도 꼭 선언해야 합니다.(메인에서 해놓긴 함)
public class AsyncConfig {

    @Bean(name = "taskExecutor") // 이 이름이 서비스의 @Async("taskExecutor")와 매칭됩니다.
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(5);         // 평상시 유지할 쓰레드 수
        executor.setMaxPoolSize(10);        // 부하가 걸릴 때 최대 쓰레드 수
        executor.setQueueCapacity(500);     // 대기 큐 용량
        executor.setThreadNamePrefix("AAP-Async-"); // 로그에서 확인할 이름
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        return executor;
    }
}