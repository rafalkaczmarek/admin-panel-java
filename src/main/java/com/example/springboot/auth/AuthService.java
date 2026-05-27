package com.example.springboot.auth;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
	private static final Logger log = LoggerFactory.getLogger(AuthService.class);
	private static final int REFRESH_BYTES = 48;
	private final SecureRandom secureRandom = new SecureRandom();

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthProperties props;

	public AuthService(
			UserRepository userRepository,
			RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder,
			JwtService jwtService,
			AuthProperties props) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.props = props;
	}

	@Transactional
	public IssuedSession login(String email, String password, boolean rememberMe) {
		String normalizedEmail = String.valueOf(email).trim().toLowerCase();
		AppUser user = userRepository.findByEmail(normalizedEmail).orElse(null);
		if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
			log.warn("Login failed for email={}", normalizedEmail);
			throw new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Incorrect email or password.");
		}

		log.info("Login succeeded for userId={} email={} rememberMe={}", user.getId(), user.getEmail(), rememberMe);
		return issueSession(user, rememberMe);
	}

	@Transactional
	public IssuedSession refresh(String rawRefreshToken) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			log.warn("Refresh failed: missing refresh token");
			throw new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_SESSION", "Missing refresh token.");
		}

		String tokenHash = TokenHasher.sha256Hex(rawRefreshToken);
		RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash).orElse(null);

		if (stored == null || stored.getExpiresAt().isBefore(Instant.now())) {
			if (stored != null) {
				refreshTokenRepository.deleteById(stored.getId());
			}
			log.warn("Refresh failed: session expired or not found");
			throw new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_SESSION", "Session expired.");
		}

		// rotate refresh token
		refreshTokenRepository.deleteById(stored.getId());
		log.info("Refresh succeeded for userId={} email={}", stored.getUser().getId(), stored.getUser().getEmail());
		return issueSession(stored.getUser(), false);
	}

	@Transactional
	public void logout(String rawRefreshToken) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			log.debug("Logout: no refresh token provided (idempotent)");
			return;
		}
		String tokenHash = TokenHasher.sha256Hex(rawRefreshToken);
		try {
			refreshTokenRepository.deleteByTokenHash(tokenHash);
			log.info("Logout succeeded (refresh token deleted)");
		} catch (RuntimeException ignored) {
			// best-effort cleanup (idempotent)
			log.debug("Logout cleanup failed (ignored)", ignored);
		}
	}

	private IssuedSession issueSession(AppUser user, boolean rememberMe) {
		JwtService.SignedAccessToken signed = jwtService.signAccessToken(user);

		String refreshToken = generateRefreshToken();
		Instant refreshExp = refreshExpiry(rememberMe);
		refreshTokenRepository.save(new RefreshToken(
				UUID.randomUUID().toString(),
				TokenHasher.sha256Hex(refreshToken),
				user,
				refreshExp));

		return new IssuedSession(
				refreshToken,
				new AuthResponse(
						signed.token(),
						signed.expiresAt().toString(),
						new PublicUser(user.getEmail(), user.getRoles())));
	}

	private String generateRefreshToken() {
		byte[] bytes = new byte[REFRESH_BYTES];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private Instant refreshExpiry(boolean rememberMe) {
		long days = rememberMe ? props.refresh().ttlRememberDays() : props.refresh().ttlDays();
		return Instant.now().plus(days, ChronoUnit.DAYS);
	}

	public record PublicUser(String email, java.util.List<String> roles) {
	}

	public record AuthResponse(String accessToken, String expiresAt, PublicUser user) {
	}

	public record IssuedSession(String refreshToken, AuthResponse response) {
	}
}
