package com.rbkmoney.payouter.service;

import com.rbkmoney.payouter.domain.enums.PayoutAccountType;
import com.rbkmoney.payouter.domain.enums.PayoutStatus;
import com.rbkmoney.payouter.domain.enums.PayoutType;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.exception.InvalidStateException;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.exception.StorageException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PayoutService {

    List<Long> createPayouts(LocalDateTime fromTime, LocalDateTime toTime, PayoutType payoutType) throws InvalidStateException, NotFoundException, StorageException;

    long createPayout(String partyId, String shopId, LocalDateTime fromTime, LocalDateTime toTime, PayoutType payoutType) throws InvalidStateException, NotFoundException, StorageException;

    void pay(long payoutId) throws InvalidStateException, StorageException;

    void confirm(long payoutId) throws InvalidStateException, StorageException;

    void cancel(long payoutId, String details) throws InvalidStateException, StorageException;

    List<Payout> getUnpaidPayoutsByAccountType(PayoutAccountType accountType) throws StorageException;

    List<Payout> search(Optional<PayoutStatus> payoutStatus, Optional<LocalDateTime> fromTime, Optional<LocalDateTime> toTime, Optional<List<Long>> payoutIds, Optional<Long> fromId, Optional<Integer> size);

    void excludeFromPayout(long payoutId) throws StorageException;

}
