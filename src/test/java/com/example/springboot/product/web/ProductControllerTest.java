package com.example.springboot.product.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.springboot.auth.domain.AppUser;
import com.example.springboot.auth.repository.UserRepository;
import com.example.springboot.auth.security.JwtService;
import com.example.springboot.product.domain.Product;
import com.example.springboot.product.repository.ProductRepository;
import java.math.BigDecimal;
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

	private String accessToken;

	@BeforeEach
	void setUp() {
		productRepository.deleteAll();
		userRepository.deleteAll();

		String email = "admin+" + UUID.randomUUID() + "@dashstack.com";
		AppUser user = userRepository.save(new AppUser(
				UUID.randomUUID().toString(),
				email,
				passwordEncoder.encode("admin123"),
				List.of("admin")));
		accessToken = jwtService.signAccessToken(user).token();
	}

	@Test
	void shouldReturn401WithoutAccessToken() throws Exception {
		mockMvc.perform(get("/api/products").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void shouldReturnListOfProducts() throws Exception {
		productRepository.save(new Product("Keyboard", new BigDecimal("199.99")));
		productRepository.save(new Product("Mouse", new BigDecimal("79.00")));

		MvcResult result = mockMvc.perform(get("/api/products")
				.header("Authorization", "Bearer " + accessToken)
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn();

		String body = result.getResponse().getContentAsString();
		assertThat(body).contains("Keyboard");
		assertThat(body).contains("Mouse");
	}

	@Test
	void shouldReturnProductById() throws Exception {
		var product = productRepository.save(new Product(
				"img.png",
				"Keyboard",
				"Peripherals",
				new BigDecimal("199.99"),
				10,
				new String[] {"black"}));

		mockMvc.perform(get("/api/products/" + product.getId())
				.header("Authorization", "Bearer " + accessToken)
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Keyboard"))
				.andExpect(jsonPath("$.category").value("Peripherals"));
	}

	@Test
	void shouldReturn404WhenProductNotFound() throws Exception {
		mockMvc.perform(get("/api/products/" + UUID.randomUUID())
				.header("Authorization", "Bearer " + accessToken)
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
	}

	@Test
	void shouldCreateProduct() throws Exception {
		mockMvc.perform(post("/api/products")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "image": "img.png",
						  "name": "Monitor",
						  "category": "Displays",
						  "price": 499.99,
						  "piece": 5,
						  "availableColors": ["black", "silver"]
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Monitor"))
				.andExpect(jsonPath("$.id").isNotEmpty());

		assertThat(productRepository.count()).isEqualTo(1);
	}

	@Test
	void shouldReturn400WhenCreateBodyInvalid() throws Exception {
		mockMvc.perform(post("/api/products")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "image": "",
						  "name": "",
						  "category": "",
						  "price": -1,
						  "piece": -1
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"));
	}

	@Test
	void shouldUpdateProduct() throws Exception {
		var product = productRepository.save(new Product("Keyboard", new BigDecimal("199.99")));

		mockMvc.perform(put("/api/products/" + product.getId())
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "image": "new.png",
						  "name": "Mechanical Keyboard",
						  "category": "Peripherals",
						  "price": 249.99,
						  "piece": 3,
						  "availableColors": ["white"]
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Mechanical Keyboard"))
				.andExpect(jsonPath("$.price").value(249.99));
	}

	@Test
	void shouldReturn404WhenUpdatingMissingProduct() throws Exception {
		mockMvc.perform(put("/api/products/" + UUID.randomUUID())
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "image": "img.png",
						  "name": "Keyboard",
						  "category": "Peripherals",
						  "price": 199.99,
						  "piece": 1,
						  "availableColors": []
						}
						"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
	}

	@Test
	void shouldDeleteProduct() throws Exception {
		var product = productRepository.save(new Product("Keyboard", new BigDecimal("199.99")));

		mockMvc.perform(delete("/api/products/" + product.getId())
				.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isNoContent());

		assertThat(productRepository.existsById(product.getId())).isFalse();
	}

	@Test
	void shouldReturn404WhenDeletingMissingProduct() throws Exception {
		mockMvc.perform(delete("/api/products/" + UUID.randomUUID())
				.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
	}
}
