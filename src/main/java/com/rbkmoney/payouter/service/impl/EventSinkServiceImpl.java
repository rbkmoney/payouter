package com.rbkmoney.payouter.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.payout_processing.*;
import com.rbkmoney.geck.serializer.kit.json.JsonHandler;
import com.rbkmoney.geck.serializer.kit.tbase.TBaseProcessor;
import com.rbkmoney.payouter.dao.PayoutEventDao;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutEvent;
import com.rbkmoney.payouter.domain.tables.records.PayoutRecord;
import com.rbkmoney.payouter.exception.DaoException;
import com.rbkmoney.payouter.exception.StorageException;
import com.rbkmoney.payouter.service.EventSinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class EventSinkServiceImpl implements EventSinkService{

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final PayoutEventDao payoutEventDao;

    @Autowired
    public EventSinkServiceImpl(PayoutEventDao payoutEventDao) {
        this.payoutEventDao = payoutEventDao;
    }

    public Long getLastEventId() throws StorageException {
        try {
            return payoutEventDao.getLastEventId();
        } catch (DaoException ex) {
            throw new StorageException("Failed to get last event id", ex);
        }
    }

    public PayoutEvent getEvent(long eventId) throws StorageException {
        try {
            return payoutEventDao.getEvent(eventId);
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to get payout event, eventId=%d", eventId), ex);
        }
    }

    public List<PayoutEvent> getEvents(Optional<Long> after, int limit) throws StorageException {
        try {
            return payoutEventDao.getEvents(after, limit);
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to get payout event range, after=%s, limit=%d", after, limit), ex);
        }
    }

    public void savePayoutCreatedEvent(PayoutRecord payoutRecord, UserInfo userInfo) throws StorageException {
        PayoutEvent payoutEvent = new PayoutEvent();
        payoutEvent.setEventType(PayoutChange._Fields.PAYOUT_CREATED.getFieldName());
        payoutEvent.setPayoutId(Long.toString(payoutRecord.getId()));
        payoutEvent.setPayoutStatus(PayoutStatus._Fields.UNPAID.getFieldName());
        payoutEvent.setPayoutCreatedAt(payoutRecord.getCreatedAt());
        payoutEvent.setPayoutPartyId(payoutRecord.getPartyId());
        payoutEvent.setPayoutShopId(payoutRecord.getShopId());

        //account
        payoutEvent.setPayoutType(PayoutType._Fields.BANK_ACCOUNT.getFieldName());
        payoutEvent.setPayoutAccountId(payoutRecord.getBankAccount());
        payoutEvent.setPayoutAccountBankPostId(payoutRecord.getBankPostAccount());
        payoutEvent.setPayoutAccountBankName(payoutRecord.getBankName());
        payoutEvent.setPayoutAccountBankBik(payoutRecord.getBankBik());
        payoutEvent.setPayoutAccountPurpose(payoutRecord.getPurpose());
        payoutEvent.setPayoutAccountInn(payoutRecord.getInn());

        //account cash flow
        FinalCashFlowPosting finalCashFlowPosting = new FinalCashFlowPosting();
        finalCashFlowPosting.setSource(
                new FinalCashFlowAccount(
                        CashFlowAccount.merchant(MerchantCashFlowAccount.settlement),
                        payoutRecord.getShopAcc()
                )
        );
        finalCashFlowPosting.setDestination(
                new FinalCashFlowAccount(
                        CashFlowAccount.merchant(MerchantCashFlowAccount.settlement),
                        payoutRecord.getShopPayoutAcc()
                )
        );
        finalCashFlowPosting.setVolume(
                new Cash(
                        payoutRecord.getAmount(),
                        new CurrencyRef(payoutRecord.getCurrencyCode())
                )
        );
        try {
            payoutEvent.setPayoutCashFlow(
                    new ObjectMapper().writeValueAsString(Arrays.asList(
                            new TBaseProcessor().process(finalCashFlowPosting, new JsonHandler())
                    ))
            );
        } catch (IOException ex) {
            throw new StorageException("Failed to generate cash flow", ex);
        }

        payoutEvent.setPayoutAccountLegalAgreementId(payoutRecord.getAccountLegalAgreementId());
        payoutEvent.setPayoutAccountLegalAgreementSignedAt(payoutRecord.getAccountLegalAgreementSignedAt());

        payoutEvent.setUserId(userInfo.getId());
        payoutEvent.setUserType(userInfo.getType().getSetField().getFieldName());


        saveEvent(payoutEvent);
    }

    public void savePayoutPaidEvent(PayoutRecord payoutRecord) throws StorageException {
        PayoutEvent payoutEvent = new PayoutEvent();
        payoutEvent.setEventType(PayoutChange._Fields.PAYOUT_STATUS_CHANGED.getFieldName());
        payoutEvent.setPayoutId(Long.toString(payoutRecord.getId()));
        payoutEvent.setPayoutStatus(PayoutStatus._Fields.PAID.getFieldName());
        payoutEvent.setPayoutPaidDetailsType(PaidDetails._Fields.ACCOUNT_DETAILS.getFieldName());

        saveEvent(payoutEvent);
    }

    public void savePayoutCancelledEvent(PayoutRecord payoutRecord, String details, UserInfo userInfo) throws StorageException {
        PayoutEvent payoutEvent = new PayoutEvent();
        payoutEvent.setEventType(PayoutChange._Fields.PAYOUT_STATUS_CHANGED.getFieldName());
        payoutEvent.setPayoutId(Long.toString(payoutRecord.getId()));
        payoutEvent.setPayoutStatus(PayoutStatus._Fields.CANCELLED.getFieldName());
        payoutEvent.setUserId(userInfo.getId());
        payoutEvent.setUserType(userInfo.getType().getSetField().getFieldName());

        payoutEvent.setPayoutStatusCancelDetails(details);

        saveEvent(payoutEvent);
    }

    public void savePayoutConfirmedEvent(PayoutRecord payoutRecord, UserInfo userInfo) throws StorageException {
        PayoutEvent payoutEvent = new PayoutEvent();
        payoutEvent.setEventType(PayoutChange._Fields.PAYOUT_STATUS_CHANGED.getFieldName());
        payoutEvent.setPayoutId(Long.toString(payoutRecord.getId()));
        payoutEvent.setPayoutStatus(PayoutStatus._Fields.CONFIRMED.getFieldName());

        payoutEvent.setUserId(userInfo.getId());
        payoutEvent.setUserType(userInfo.getType().getSetField().getFieldName());

        saveEvent(payoutEvent);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveEvent(PayoutEvent payoutEvent) throws StorageException {
        log.debug("Trying to save payout event, payoutId='{}', eventType='{}'", payoutEvent.getPayoutId(), payoutEvent.getEventType());
        try {
            payoutEvent.setEventCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
            long eventId = payoutEventDao.saveEvent(payoutEvent);
            log.info("Payout event has been successfully saved, payoutId='{}', eventId='{}', eventType='{}'", payoutEvent.getPayoutId(), eventId, payoutEvent.getEventType());
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to save payout event, payoutId=%s", payoutEvent.getPayoutId()),  ex);
        }
    }
}
