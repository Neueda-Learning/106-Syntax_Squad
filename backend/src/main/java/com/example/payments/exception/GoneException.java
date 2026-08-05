package com.example.payments.exception;

import com.example.payments.model.ErrorCode;
import org.springframework.http.HttpStatus;

public class GoneException extends ApiException {

    public GoneException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.GONE);
    }
}
