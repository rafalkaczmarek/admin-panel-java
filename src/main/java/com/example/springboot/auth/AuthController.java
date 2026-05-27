package com.example.springboot.auth;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private static final String REFRESH_COOKIE = "refreshToken";

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	public AuthService.AuthResponse login(@Valid @RequestBody LoginBody body, HttpServletResponse response) {
		AuthService.IssuedSession session = authService.login(body.email(), body.password(),
				Boolean.TRUE.equals(body.rememberMe()));
		response.addHeader("Set-Cookie",
				refreshCookie(session.response().expiresAt(), session.refreshToken()).toString());
		return session.response();
	}

	@PostMapping("/refresh")
	public AuthService.AuthResponse refresh(
			@CookieValue(name = REFRESH_COOKIE, required = false) String rawRefreshToken,
			HttpServletResponse response) {
		AuthService.IssuedSession session = authService.refresh(rawRefreshToken);
		response.addHeader("Set-Cookie",
				refreshCookie(session.response().expiresAt(), session.refreshToken()).toString());
		return session.response();
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(
			@CookieValue(name = REFRESH_COOKIE, required = false) String rawRefreshToken,
			HttpServletResponse response) {
		authService.logout(rawRefreshToken);
		response.addHeader("Set-Cookie", clearRefreshCookie().toString());
	}

	private static ResponseCookie refreshCookie(String expiresAtIso, String token) {
		Instant expiresAt = Instant.parse(expiresAtIso);
		long seconds = Math.max(0, Duration.between(Instant.now(), expiresAt).getSeconds());
		return ResponseCookie.from(REFRESH_COOKIE, token)
				.httpOnly(true)
				.sameSite("Lax")
				.secure(false)
				.path("/api/auth")
				.maxAge(Duration.ofSeconds(seconds))
				.build();
	}

	private static ResponseCookie clearRefreshCookie() {
		return ResponseCookie.from(REFRESH_COOKIE, "")
				.httpOnly(true)
				.sameSite("Lax")
				.secure(false)
				.path("/api/auth")
				.maxAge(0)
				.build();
	}

	public record LoginBody(
			@NotBlank String email,
			@NotBlank String password,
			Boolean rememberMe) {
	}
}
