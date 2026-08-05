package com.example.payments.exception;

import com.example.payments.model.ErrorCode;
import org.springframework.http.HttpStatus;

public class ForbiddenException extends ApiException {

    public ForbiddenException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.FORBIDDEN);
    }
}
