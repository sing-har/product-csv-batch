package com.product.batch.exception;

import com.product.batch.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

        @ExceptionHandler(ResponseStatusException.class)
        public ResponseEntity<ApiResponse<Object>> handleResponseStatusException(
                        ResponseStatusException exception) {
                log.warn("Handled response status exception: {}", exception.getReason());

                HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());

                return ResponseEntity
                                .status(status)
                                .body(ApiResponse.failure(exception.getReason(), null));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
                        MethodArgumentNotValidException exception) {
                Map<String, String> errors = new LinkedHashMap<>();

                for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
                        errors.put(fieldError.getField(), fieldError.getDefaultMessage());
                }

                log.warn("Validation failed: {}", errors);

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.failure("Validation failed", errors));
        }

        @ExceptionHandler(MissingServletRequestPartException.class)
        public ResponseEntity<ApiResponse<Object>> handleMissingServletRequestPartException(
                        MissingServletRequestPartException exception) {
                log.warn("Missing request part: {}", exception.getRequestPartName());

                String message = "Required file part '" + exception.getRequestPartName() + "' is missing";

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.failure(message, null));
        }

        @ExceptionHandler(MaxUploadSizeExceededException.class)
        public ResponseEntity<ApiResponse<Object>> handleMaxUploadSizeExceededException(
                        MaxUploadSizeExceededException exception) {
                log.warn("Uploaded file size exceeded limit");

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.failure("Uploaded file size exceeds allowed limit", null));
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ApiResponse<Object>> handleBadCredentialsException(
                        BadCredentialsException exception) {
                log.warn("Invalid login attempt");

                return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(ApiResponse.failure("Invalid username or password", null));
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(
                        AccessDeniedException exception) {
                log.warn("Access denied: {}", exception.getMessage());

                return ResponseEntity
                                .status(HttpStatus.FORBIDDEN)
                                .body(ApiResponse.failure("You do not have permission to access this resource", null));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Object>> handleGenericException(
                        Exception exception) {
                log.error("Unexpected error occurred", exception);

                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.failure("Something went wrong. Please try again later.", null));
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(
                        HttpMessageNotReadableException exception) {
                log.warn("Invalid request body: {}", exception.getMessage());

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.failure(
                                                "Invalid request body. Please check date format yyyy-MM-dd and enum values.",
                                                null));
        }
}