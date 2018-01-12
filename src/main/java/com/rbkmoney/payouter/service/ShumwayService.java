package com.rbkmoney.payouter.service;

public interface ShumwayService {

    void hold(long payoutId);

    void commit(long payoutId);

    void rollback(long payoutId);

    void revert(long payoutId);

}
