package com.rbkmoney.payouter.exception;

public class AccounterException extends RuntimeException {

    public AccounterException() {
    }

    public AccounterException(String message) {
        super(message);
    }

    public AccounterException(String message, Throwable cause) {
        super(message, cause);
    }

    public AccounterException(Throwable cause) {
        super(cause);
    }

    public AccounterException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
