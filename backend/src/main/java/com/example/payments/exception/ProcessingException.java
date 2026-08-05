package com.example.payments.exception;

import com.example.payments.model.ErrorCode;
import org.springframework.http.HttpStatus;

public class ProcessingException extends ApiException {

    public ProcessingException(String message) {
        super(ErrorCode.PROCESSING_ERROR, message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
