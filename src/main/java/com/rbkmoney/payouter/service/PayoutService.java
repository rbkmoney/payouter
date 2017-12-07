package com.rbkmoney.payouter.service;

import com.rbkmoney.payouter.domain.enums.PayoutType;
import com.rbkmoney.payouter.exception.InvalidStateException;
import com.rbkmoney.payouter.exception.StorageException;

import java.time.LocalDateTime;

public interface PayoutService {

    long createPayout(String partyId, String shopId, LocalDateTime fromTime, LocalDateTime toTime, PayoutType payoutType) throws InvalidStateException, StorageException;

    void pay(long payoutId) throws InvalidStateException, StorageException;

    void confirm(long payoutId) throws InvalidStateException, StorageException;

    void cancel(long payoutId) throws InvalidStateException, StorageException;

}
