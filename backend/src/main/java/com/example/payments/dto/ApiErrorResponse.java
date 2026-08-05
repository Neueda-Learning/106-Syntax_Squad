package com.example.payments.dto;

public class ApiErrorResponse {

    private String errorCode;
    private String message;

    public ApiErrorResponse(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }
}
