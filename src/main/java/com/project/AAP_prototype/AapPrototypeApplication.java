package com.project.AAP_prototype;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class AapPrototypeApplication {

	public static void main(String[] args) {
		SpringApplication.run(AapPrototypeApplication.class, args);
	}

	// RestTemplate Bean 등록하여 금융기관 검증용 토큰 생성
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

}
