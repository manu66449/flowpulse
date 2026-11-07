package com.flowpulse.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync // <--- Enables Spring's background task execution
@EnableScheduling // <--- Enables Spring's cron/scheduled task engine
@EnableRetry// <--- Enables Spring's retry interceptor engine
public class CoreServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoreServiceApplication.class, args);
	}

}
