package com.rbkmoney.payouter.exception;

public class InvalidChangesetException extends RuntimeException {

    public InvalidChangesetException() {
    }

    public InvalidChangesetException(String message) {
        super(message);
    }

    public InvalidChangesetException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidChangesetException(Throwable cause) {
        super(cause);
    }

}
