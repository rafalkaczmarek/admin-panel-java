package com.example.springboot.auth;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
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

		boolean ok = user != null && passwordEncoder.matches(password, user.getPasswordHash());
		if (!ok) {
			throw new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Incorrect email or password.");
		}

		return issueSession(user, rememberMe);
	}

	@Transactional
	public IssuedSession refresh(String rawRefreshToken) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			throw new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_SESSION", "Missing refresh token.");
		}

		String tokenHash = TokenHasher.sha256Hex(rawRefreshToken);
		RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash).orElse(null);

		if (stored == null || stored.getExpiresAt().isBefore(Instant.now())) {
			if (stored != null) {
				refreshTokenRepository.deleteById(stored.getId());
			}
			throw new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_SESSION", "Session expired.");
		}

		// rotate refresh token
		refreshTokenRepository.deleteById(stored.getId());
		return issueSession(stored.getUser(), false);
	}

	@Transactional
	public void logout(String rawRefreshToken) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			return;
		}
		String tokenHash = TokenHasher.sha256Hex(rawRefreshToken);
		try {
			refreshTokenRepository.deleteByTokenHash(tokenHash);
		} catch (RuntimeException ignored) {
			// best-effort cleanup (idempotent)
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
