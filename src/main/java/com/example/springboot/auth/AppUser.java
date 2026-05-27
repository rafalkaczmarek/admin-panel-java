package com.example.springboot.auth;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;

@Entity
@Table(name = "users")
public class AppUser {
	@Id
	@Column(nullable = false)
	private String id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false, name = "password_hash")
	private String passwordHash;

	@ElementCollection(fetch = FetchType.EAGER)
	private List<String> roles;

	protected AppUser() {
	}

	public AppUser(String id, String email, String passwordHash, List<String> roles) {
		this.id = id;
		this.email = email;
		this.passwordHash = passwordHash;
		this.roles = roles;
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
		return roles;
	}
}
