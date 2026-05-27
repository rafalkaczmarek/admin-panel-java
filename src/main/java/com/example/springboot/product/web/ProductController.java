package com.example.springboot.product.web;

import com.example.springboot.product.dto.ProductDto;
import com.example.springboot.product.service.ProductService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
