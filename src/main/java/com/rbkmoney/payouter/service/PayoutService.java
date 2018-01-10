package com.rbkmoney.payouter.service;

import com.rbkmoney.payouter.domain.enums.PayoutStatus;
import com.rbkmoney.payouter.domain.enums.PayoutType;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.exception.InvalidStateException;
import com.rbkmoney.payouter.exception.StorageException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PayoutService {

    List<Long> createPayouts(LocalDateTime fromTime, LocalDateTime toTime, PayoutType payoutType) throws InvalidStateException, StorageException;

    long createPayout(String partyId, String shopId, LocalDateTime fromTime, LocalDateTime toTime, PayoutType payoutType) throws InvalidStateException, StorageException;

    void pay(long payoutId) throws InvalidStateException, StorageException;

    void confirm(long payoutId) throws InvalidStateException, StorageException;

    void cancel(long payoutId) throws InvalidStateException, StorageException;

    List<Payout> search(Optional<PayoutStatus> payoutStatus, Optional<LocalDateTime> fromTime, Optional<LocalDateTime> toTimer, Optional<List<Long>> payoutIds, long fromId, int size);

}
