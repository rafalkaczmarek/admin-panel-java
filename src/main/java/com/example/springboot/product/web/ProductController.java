package com.example.springboot.product.web;

import com.example.springboot.product.dto.ProductDto;
import com.example.springboot.product.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {
	private static final Logger log = LoggerFactory.getLogger(ProductController.class);
	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@GetMapping
	public List<ProductDto> listProducts() {
		log.info("GET /api/products");
		var products = productService.listProducts();
		log.info("GET /api/products -> {} item(s)", products.size());
		return products;
	}

	@GetMapping("/{id}")
	public ProductDto getProduct(@PathVariable String id) {
		log.info("GET /api/products/{}", id);
		return productService.getProduct(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ProductDto createProduct(@Valid @RequestBody ProductRequestBody body) {
		log.info("POST /api/products name={}", body.name());
		return productService.createProduct(
				body.image(),
				body.name(),
				body.category(),
				body.price(),
				body.piece(),
				body.availableColors());
	}

	@PutMapping("/{id}")
	public ProductDto updateProduct(@PathVariable String id, @Valid @RequestBody ProductRequestBody body) {
		log.info("PUT /api/products/{} name={}", id, body.name());
		return productService.updateProduct(
				id,
				body.image(),
				body.name(),
				body.category(),
				body.price(),
				body.piece(),
				body.availableColors());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteProduct(@PathVariable String id) {
		log.info("DELETE /api/products/{}", id);
		productService.deleteProduct(id);
	}

	public record ProductRequestBody(
			@NotBlank String image,
			@NotBlank String name,
			@NotBlank String category,
			@NotNull @DecimalMin("0.00") BigDecimal price,
			@Min(0) int piece,
			String[] availableColors) {
	}
}
