package com.example.springboot.auth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
	private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
	private final JwtService jwtService;

	public JwtAuthFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String path = request.getRequestURI();
		if (path.startsWith("/api/auth/")) {
			filterChain.doFilter(request, response);
			return;
		}

		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		String token = extractBearer(header);
		if (token == null) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			Claims claims = jwtService.verify(token);
			String userId = claims.getSubject();
			String email = claims.get("email", String.class);
			Object rolesObj = claims.get("roles");
			List<String> roles = rolesObj instanceof List<?> raw
					? raw.stream().filter(String.class::isInstance).map(String.class::cast).toList()
					: List.of();
			Collection<? extends GrantedAuthority> authorities = roles
					.stream()
					.map(r -> new SimpleGrantedAuthority("ROLE_" + r))
					.toList();

			var auth = new UsernamePasswordAuthenticationToken(
					new AuthPrincipal(userId, email, roles),
					null,
					authorities);
			SecurityContextHolder.getContext().setAuthentication(auth);
			log.debug("JWT accepted path={} userId={} email={}", path, userId, email);
			filterChain.doFilter(request, response);
		} catch (RuntimeException ex) {
			log.warn("JWT rejected path={} reason={}", path, ex.getClass().getSimpleName());
			throw new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_SESSION", "Invalid access token.");
		}
	}

	private static String extractBearer(String header) {
		if (header == null || header.isBlank()) {
			return null;
		}
		String[] parts = header.split(" ");
		if (parts.length != 2) {
			return null;
		}
		if (!"Bearer".equals(parts[0]) || parts[1].isBlank()) {
			return null;
		}
		return parts[1];
	}

	public record AuthPrincipal(String id, String email, List<String> roles) {
	}
}
