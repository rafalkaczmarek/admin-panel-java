package com.example.springboot.product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductDto(
		String id,
		String image,
		String name,
		String category,
		BigDecimal price,
		int piece,
		String[] availableColors,
		Instant createdAt,
		Instant updatedAt) {
	static ProductDto from(Product product) {
		return new ProductDto(
				product.getId(),
				product.getImage(),
				product.getName(),
				product.getCategory(),
				product.getPrice(),
				product.getPiece(),
				product.getAvailableColors(),
				product.getCreatedAt(),
				product.getUpdatedAt());
	}
}
