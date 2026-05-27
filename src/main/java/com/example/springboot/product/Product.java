package com.example.springboot.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "products")
public class Product {

	@Id
	@UuidGenerator
	@Column(nullable = false, name = "product_id")
	private String id;

	@Column(nullable = false)
	private String image;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String category;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal price;

	@Column(nullable = false)
	private int piece = 0;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(nullable = false, name = "available_colors")
	private String[] availableColors = new String[0];

	@CreationTimestamp
	@Column(nullable = false, name = "created_at")
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false, name = "updated_at")
	private Instant updatedAt;

	protected Product() {
	}

	public Product(String name, BigDecimal price) {
		this("",
				name,
				"",
				price,
				0,
				new String[0]);
	}

	public Product(String image, String name, String category, BigDecimal price, int piece, String[] availableColors) {
		this.image = image;
		this.name = name;
		this.category = category;
		this.price = price;
		this.piece = piece;
		this.availableColors = availableColors == null ? new String[0] : availableColors;
	}

	public String getId() {
		return id;
	}

	public String getImage() {
		return image;
	}

	public String getName() {
		return name;
	}

	public String getCategory() {
		return category;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public int getPiece() {
		return piece;
	}

	public String[] getAvailableColors() {
		return availableColors;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
