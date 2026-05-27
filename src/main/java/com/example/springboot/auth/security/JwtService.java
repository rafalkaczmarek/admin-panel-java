package com.example.springboot.auth.security;

import com.example.springboot.auth.domain.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtService {
	private final AuthProperties props;

	public JwtService(AuthProperties props) {
		this.props = props;
	}

	private SecretKey key() {
		String raw = props.jwt().accessSecret();
		if (raw == null || raw.isBlank()) {
			throw new IllegalStateException(
					"JWT access secret is missing. Set app.auth.jwt.access-secret (or JWT_ACCESS_SECRET).");
		}

		byte[] keyBytes;
		if (raw.startsWith("base64:")) {
			keyBytes = Decoders.BASE64.decode(raw.substring("base64:".length()));
		} else {
			keyBytes = raw.getBytes(StandardCharsets.UTF_8);
		}

		if (keyBytes.length < 32) {
			throw new IllegalStateException(
					"JWT access secret is too short for HS256 (need >= 32 bytes). " +
							"Provide a longer value or use base64:<...> (e.g. 32+ random bytes).");
		}

		return Keys.hmacShaKeyFor(keyBytes);
	}

	public SignedAccessToken signAccessToken(AppUser user) {
		Instant now = Instant.now();
		Instant exp = now.plus(props.jwt().accessTtl());

		String token = Jwts.builder()
				.subject(user.getId())
				.claim("email", user.getEmail())
				.claim("roles", user.getRoles())
				.issuedAt(Date.from(now))
				.expiration(Date.from(exp))
				.signWith(key(), Jwts.SIG.HS256)
				.compact();

		return new SignedAccessToken(token, exp);
	}

	public Claims verify(String token) {
		return Jwts.parser()
				.verifyWith(key())
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	public record SignedAccessToken(String token, Instant expiresAt) {
	}
}
