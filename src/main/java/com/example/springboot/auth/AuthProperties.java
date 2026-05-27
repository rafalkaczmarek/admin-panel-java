package com.example.springboot.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(Jwt jwt, Refresh refresh) {
	public record Jwt(String accessSecret, Duration accessTtl) {
	}

	public record Refresh(long ttlDays, long ttlRememberDays) {
	}
}
