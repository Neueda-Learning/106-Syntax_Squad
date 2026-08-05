package com.example.payments.exception;

import com.example.payments.model.ErrorCode;
import com.example.payments.model.PaymentStatus;
import org.springframework.http.HttpStatus;

public class InvalidStatusTransitionException extends ApiException {

    public InvalidStatusTransitionException(PaymentStatus fromStatus, PaymentStatus toStatus) {
        super(
            ErrorCode.INVALID_STATUS_TRANSITION,
            "Invalid status transition from " + fromStatus + " to " + toStatus,
            HttpStatus.BAD_REQUEST
        );
    }
}
