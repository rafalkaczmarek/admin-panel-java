package com.example.springboot.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.springboot.auth.exception.AuthException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.NoHandlerFoundException;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void authShouldMapUnauthorized() {
		AuthException ex = new AuthException(
				HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Incorrect email or password.");

		ResponseEntity<ApiError> response = handler.auth(ex);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("INVALID_CREDENTIALS");
		assertThat(response.getBody().message()).isEqualTo("Incorrect email or password.");
	}

	@Test
	void authShouldMapNonUnauthorizedStatus() {
		AuthException ex = new AuthException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied.");

		ResponseEntity<ApiError> response = handler.auth(ex);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(response.getBody().code()).isEqualTo("FORBIDDEN");
	}

	@Test
	void validationShouldReturnBadRequest() {
		MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);

		ResponseEntity<ApiError> response = handler.validation(ex);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody().code()).isEqualTo("BAD_REQUEST");
		assertThat(response.getBody().message()).isEqualTo("Invalid request body.");
	}

	@Test
	void notFoundShouldReturn404() {
		NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/api/missing", null);

		ResponseEntity<ApiError> response = handler.notFound(ex);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
		assertThat(response.getBody().message()).isEqualTo("Resource not found.");
	}
}
