package com.example.springboot.auth.repository;

import com.example.springboot.auth.domain.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<AppUser, String> {
	Optional<AppUser> findByEmail(String email);
}
