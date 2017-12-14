package com.rbkmoney.payouter.exception;

import org.apache.thrift.TException;

public class ReportException extends RuntimeException{
    public ReportException(TException e) {
        super(e);
    }
}
