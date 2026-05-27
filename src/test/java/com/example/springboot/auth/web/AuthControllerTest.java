package com.example.springboot.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.springboot.auth.domain.AppUser;
import com.example.springboot.auth.repository.RefreshTokenRepository;
import com.example.springboot.auth.repository.UserRepository;
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
	void logoutIsIdempotent() throws Exception {
		mockMvc.perform(post("/api/auth/logout"))
				.andExpect(status().isNoContent());
	}
}
