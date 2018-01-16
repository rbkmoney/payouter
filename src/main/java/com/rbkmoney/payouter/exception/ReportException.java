package com.rbkmoney.payouter.exception;

import org.apache.thrift.TException;

public class ReportException extends RuntimeException{
    public ReportException(Throwable cause) {
        super(cause);
    }

    public ReportException(String message, Throwable cause) {
        super(message, cause);
    }
}
