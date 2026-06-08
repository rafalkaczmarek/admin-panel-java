package com.example.springboot.product.service;

import com.example.springboot.product.domain.Product;
import com.example.springboot.product.dto.ProductDto;
import com.example.springboot.product.exception.ProductException;
import com.example.springboot.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
	private static final Logger log = LoggerFactory.getLogger(ProductService.class);
	private final ProductRepository productRepository;

	public ProductService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	@Transactional(readOnly = true)
	public List<ProductDto> listProducts() {
		log.debug("Listing products (sorted by id asc)");
		var products = productRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
				.stream()
				.map(ProductDto::from)
				.toList();
		log.debug("Listed {} product(s)", products.size());
		return products;
	}

	@Transactional(readOnly = true)
	public ProductDto getProduct(String id) {
		log.debug("Fetching product id={}", id);
		return productRepository.findById(id)
				.map(ProductDto::from)
				.orElseThrow(() -> productNotFound(id));
	}

	@Transactional
	public ProductDto createProduct(
			String image,
			String name,
			String category,
			BigDecimal price,
			int piece,
			String[] availableColors) {
		log.debug("Creating product name={}", name);
		var product = new Product(image, name, category, price, piece, availableColors);
		var saved = productRepository.save(product);
		log.debug("Created product id={}", saved.getId());
		return ProductDto.from(saved);
	}

	@Transactional
	public ProductDto updateProduct(
			String id,
			String image,
			String name,
			String category,
			BigDecimal price,
			int piece,
			String[] availableColors) {
		log.debug("Updating product id={}", id);
		var product = productRepository.findById(id).orElseThrow(() -> productNotFound(id));
		product.update(image, name, category, price, piece, availableColors);
		log.debug("Updated product id={}", id);
		return ProductDto.from(product);
	}

	@Transactional
	public void deleteProduct(String id) {
		log.debug("Deleting product id={}", id);
		if (!productRepository.existsById(id)) {
			throw productNotFound(id);
		}
		productRepository.deleteById(id);
		log.debug("Deleted product id={}", id);
	}

	private static ProductException productNotFound(String id) {
		return new ProductException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Product not found: " + id);
	}
}
