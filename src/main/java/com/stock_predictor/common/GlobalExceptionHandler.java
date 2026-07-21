package com.stock_predictor.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(ex.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(ex.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleGeneric(Exception ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiError("An unexpected error occurred"));
	}
}
