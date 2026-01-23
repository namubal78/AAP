package com.project.AAP_prototype;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import io.github.cdimascio.dotenv.Dotenv;

import org.springframework.scheduling.annotation.EnableAsync;

/*
금융권 수준의 대용량 트랜잭션을 처리할 때 비동기(@Async) 처리는 필수적입니다. 
알림(슬랙, 이메일 등)을 백그라운드에서 처리하면, 
알림 서버가 느려지더라도 사용자의 결제 완료 응답 속도에는 영향을 주지 않기 때문
*/
@EnableAsync // 비동기 처리 기능을 활성화
@SpringBootApplication
public class AapPrototypeApplication {

	public static void main(String[] args) {
		// .env 로드 후 시스템 변수에 등록
		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
		dotenv.entries().forEach(e -> System.setProperty(e.getKey(), e.getValue()));
		
		SpringApplication.run(AapPrototypeApplication.class, args);
	}

	// RestTemplate Bean 등록하여 1) 금융기관 검증용 토큰 생성, 2) 슬랙 webhook 호출
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

}
