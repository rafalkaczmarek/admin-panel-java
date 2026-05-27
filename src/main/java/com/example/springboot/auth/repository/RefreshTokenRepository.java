package com.example.springboot.auth.repository;

import com.example.springboot.auth.domain.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
	Optional<RefreshToken> findByTokenHash(String tokenHash);

	void deleteByTokenHash(String tokenHash);
}
