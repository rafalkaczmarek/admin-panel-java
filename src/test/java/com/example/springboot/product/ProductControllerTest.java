package com.example.springboot.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.springboot.auth.AppUser;
import com.example.springboot.auth.JwtService;
import com.example.springboot.auth.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	@Test
	void shouldReturnListOfProducts() throws Exception {
		String email = "admin+" + UUID.randomUUID() + "@dashstack.com";
		AppUser user = userRepository.save(new AppUser(
				UUID.randomUUID().toString(),
				email,
				passwordEncoder.encode("admin123"),
				List.of("admin")));
		String token = jwtService.signAccessToken(user).token();

		productRepository.save(new Product("Keyboard", new BigDecimal("199.99")));
		productRepository.save(new Product("Mouse", new BigDecimal("79.00")));

		MvcResult result = mockMvc.perform(get("/api/products")
						.header("Authorization", "Bearer " + token)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn();

		String body = result.getResponse().getContentAsString();
		assertThat(body).contains("Keyboard");
		assertThat(body).contains("Mouse");
	}
}
