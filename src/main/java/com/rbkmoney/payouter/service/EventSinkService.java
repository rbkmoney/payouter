package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.domain.FinalCashFlowPosting;
import com.rbkmoney.damsel.payout_processing.UserInfo;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutEvent;
import com.rbkmoney.payouter.exception.StorageException;

import java.util.List;
import java.util.Optional;

public interface EventSinkService {

    Long getLastEventId() throws StorageException;

    PayoutEvent getEvent(long eventId) throws StorageException;

    List<PayoutEvent> getEvents(Optional<Long> after, int limit) throws StorageException;

    void saveEvent(PayoutEvent payoutEvent) throws StorageException;

    void savePayoutCreatedEvent(String payoutId, String purpose, Payout payout, List<FinalCashFlowPosting> cashFlowPostings, UserInfo userInfo) throws StorageException;

    void savePayoutPaidEvent(String payoutId) throws StorageException;

    void savePayoutCancelledEvent(String payoutId, String details, UserInfo userInfo) throws StorageException;

    void savePayoutConfirmedEvent(String payoutId, UserInfo userInfo) throws StorageException;

}
