package com.rbkmoney.payouter.exception;

public class ScheduleProcessingException extends RuntimeException {

    public ScheduleProcessingException() {
    }

    public ScheduleProcessingException(String message) {
        super(message);
    }

    public ScheduleProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ScheduleProcessingException(Throwable cause) {
        super(cause);
    }

    public ScheduleProcessingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
