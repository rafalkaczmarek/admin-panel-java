package com.example.springboot.common.web;

import com.example.springboot.auth.exception.AuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(AuthException.class)
	public ResponseEntity<ApiError> auth(AuthException ex) {
		if (ex.status() == HttpStatus.UNAUTHORIZED) {
			log.warn("Auth error code={} status={}", ex.code(), ex.status().value());
		} else {
			log.info("Auth error code={} status={}", ex.code(), ex.status().value());
		}
		return ResponseEntity.status(ex.status()).body(new ApiError(ex.code(), ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
		log.debug("Validation error: {}", ex.getClass().getSimpleName());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ApiError("BAD_REQUEST", "Invalid request body."));
	}

	@ExceptionHandler(NoHandlerFoundException.class)
	public ResponseEntity<ApiError> notFound(NoHandlerFoundException ex) {
		log.debug("Not found: {}", ex.getRequestURL());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("NOT_FOUND", "Resource not found."));
	}
}
