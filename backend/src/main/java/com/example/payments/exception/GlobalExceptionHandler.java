package com.example.payments.exception;

import com.example.payments.dto.ApiErrorResponse;
import com.example.payments.model.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException exception) {
        ApiErrorResponse response = new ApiErrorResponse(exception.getErrorCode().name(), exception.getMessage());
        return ResponseEntity.status(exception.getHttpStatus()).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException exception) {
        ApiErrorResponse response = new ApiErrorResponse(
            ErrorCode.VALIDATION_FAILED.name(),
            "Malformed request or invalid enum value"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception exception) {
        ApiErrorResponse response = new ApiErrorResponse(
            ErrorCode.PROCESSING_ERROR.name(),
            "Unexpected processing error"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
