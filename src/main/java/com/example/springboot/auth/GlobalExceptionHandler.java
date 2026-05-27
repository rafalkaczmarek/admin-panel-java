package com.example.springboot.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(AuthException.class)
	public ResponseEntity<ApiError> auth(AuthException ex) {
		return ResponseEntity.status(ex.status()).body(new ApiError(ex.code(), ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ApiError("BAD_REQUEST", "Invalid request body."));
	}

	@ExceptionHandler(NoHandlerFoundException.class)
	public ResponseEntity<ApiError> notFound(NoHandlerFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("NOT_FOUND", "Resource not found."));
	}
}
