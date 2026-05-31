package com.example.springboot.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.springboot.auth.domain.AppUser;
import com.example.springboot.auth.repository.RefreshTokenRepository;
import com.example.springboot.auth.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setup() {
		refreshTokenRepository.deleteAll();
		userRepository.deleteAll();
		userRepository.save(new AppUser(
				UUID.randomUUID().toString(),
				"admin@dashstack.com",
				passwordEncoder.encode("admin123"),
				List.of("admin")));
	}

	@Test
	void loginShouldReturn200AndSetRefreshCookie() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"admin@dashstack.com","password":"admin123"}
						"""))
				.andExpect(status().isOk())
				.andReturn();

		String body = result.getResponse().getContentAsString();
		assertThat(body).contains("accessToken");
		assertThat(body).contains("expiresAt");
		assertThat(body).contains("admin@dashstack.com");

		String setCookie = result.getResponse().getHeader("Set-Cookie");
		assertThat(setCookie).contains("refreshToken=");
		assertThat(refreshTokenRepository.count()).isEqualTo(1);
	}

	@Test
	void loginShouldReturn401ForInvalidPassword() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"admin@dashstack.com","password":"wrong"}
						"""))
				.andExpect(status().isUnauthorized())
				.andReturn();

		assertThat(result.getResponse().getContentAsString()).contains("INVALID_CREDENTIALS");
	}

	@Test
	void refreshShouldReturn401WhenCookieMissing() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/refresh"))
				.andExpect(status().isUnauthorized())
				.andReturn();

		assertThat(result.getResponse().getContentAsString()).contains("INVALID_SESSION");
	}

	@Test
	void refreshShouldReturn401ForUnknownCookie() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/refresh")
				.cookie(new Cookie("refreshToken", "not-a-valid-session")))
				.andExpect(status().isUnauthorized())
				.andReturn();

		assertThat(result.getResponse().getContentAsString()).contains("INVALID_SESSION");
	}

	@Test
	void refreshShouldRotateTokenAndReturn200() throws Exception {
		Cookie refreshCookie = refreshCookieFrom(loginAsAdmin());

		MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
				.andExpect(status().isOk())
				.andReturn();

		String body = refreshResult.getResponse().getContentAsString();
		assertThat(body).contains("accessToken");

		String setCookie = refreshResult.getResponse().getHeader("Set-Cookie");
		assertThat(setCookie).contains("refreshToken=");
		assertThat(refreshTokenRepository.count()).isEqualTo(1);

		mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void logoutShouldInvalidateRefreshSession() throws Exception {
		Cookie refreshCookie = refreshCookieFrom(loginAsAdmin());

		mockMvc.perform(post("/api/auth/logout").cookie(refreshCookie))
				.andExpect(status().isNoContent());

		assertThat(refreshTokenRepository.count()).isZero();

		MvcResult result = mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
				.andExpect(status().isUnauthorized())
				.andReturn();

		assertThat(result.getResponse().getContentAsString()).contains("INVALID_SESSION");
	}

	@Test
	void logoutIsIdempotent() throws Exception {
		mockMvc.perform(post("/api/auth/logout"))
				.andExpect(status().isNoContent());
	}

	@Test
	void loginShouldReturn400WhenBodyInvalid() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"","password":""}
						"""))
				.andExpect(status().isBadRequest())
				.andReturn();

		assertThat(result.getResponse().getContentAsString()).contains("BAD_REQUEST");
	}

	@Test
	void loginShouldAcceptRememberMe() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"admin@dashstack.com","password":"admin123","rememberMe":true}
						"""))
				.andExpect(status().isOk())
				.andReturn();

		assertThat(refreshCookieFrom(result)).isNotNull();
		assertThat(refreshTokenRepository.count()).isEqualTo(1);
	}

	@Test
	void loginShouldReturn401ForUnknownEmail() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"unknown@dashstack.com","password":"admin123"}
						"""))
				.andExpect(status().isUnauthorized())
				.andReturn();

		assertThat(result.getResponse().getContentAsString()).contains("INVALID_CREDENTIALS");
	}

	private MvcResult loginAsAdmin() throws Exception {
		return mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"admin@dashstack.com","password":"admin123"}
						"""))
				.andExpect(status().isOk())
				.andReturn();
	}

	private static Cookie refreshCookieFrom(MvcResult result) {
		Cookie[] cookies = result.getResponse().getCookies();
		if (cookies == null) {
			return null;
		}
		return Arrays.stream(cookies)
				.filter(c -> "refreshToken".equals(c.getName()))
				.findFirst()
				.orElse(null);
	}
}
