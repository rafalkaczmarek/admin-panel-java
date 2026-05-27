package com.example.springboot.product;

import java.math.BigDecimal;

public record ProductDto(Long id, String name, BigDecimal price) {
	static ProductDto from(Product product) {
		return new ProductDto(product.getId(), product.getName(), product.getPrice());
	}
}
