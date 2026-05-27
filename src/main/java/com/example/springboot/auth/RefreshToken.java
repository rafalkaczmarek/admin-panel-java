package com.example.springboot.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
	@Id
	@Column(nullable = false, name = "refresh_token_id")
	private String id;

	@Column(nullable = false, unique = true, name = "token_hash")
	private String tokenHash;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private AppUser user;

	@Column(nullable = false, name = "expires_at")
	private Instant expiresAt;

	@CreationTimestamp
	@Column(nullable = false, name = "created_at")
	private Instant createdAt;

	protected RefreshToken() {
	}

	public RefreshToken(String id, String tokenHash, AppUser user, Instant expiresAt) {
		this.id = id;
		this.tokenHash = tokenHash;
		this.user = user;
		this.expiresAt = expiresAt;
	}

	public String getId() {
		return id;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public AppUser getUser() {
		return user;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
