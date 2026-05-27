package com.example.springboot.product;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
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
}
