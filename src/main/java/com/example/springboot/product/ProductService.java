package com.example.springboot.product;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
	private final ProductRepository productRepository;

	public ProductService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	@Transactional(readOnly = true)
	public List<ProductDto> listProducts() {
		return productRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
				.stream()
				.map(ProductDto::from)
				.toList();
	}
}
