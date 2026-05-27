package com.example.springboot.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "users")
public class AppUser {
	@Id
	@Column(nullable = false, name = "user_id")
	private String id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false, name = "password_hash")
	private String passwordHash;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(nullable = false, name = "roles")
	private String[] roles = new String[0];

	@CreationTimestamp
	@Column(nullable = false, name = "created_at")
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false, name = "updated_at")
	private Instant updatedAt;

	protected AppUser() {
	}

	public AppUser(String id, String email, String passwordHash, List<String> roles) {
		this.id = id;
		this.email = email;
		this.passwordHash = passwordHash;
		this.roles = roles == null ? new String[0] : roles.toArray(String[]::new);
	}

	public String getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public List<String> getRoles() {
		return Arrays.asList(roles);
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
