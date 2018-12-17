package com.rbkmoney.payouter.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import com.rbkmoney.damsel.domain.FinalCashFlowPosting;
import com.rbkmoney.damsel.payout_processing.PayoutChange;
import com.rbkmoney.damsel.payout_processing.UserInfo;
import com.rbkmoney.geck.serializer.kit.json.JsonHandler;
import com.rbkmoney.geck.serializer.kit.tbase.TBaseProcessor;
import com.rbkmoney.payouter.dao.PayoutEventDao;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutEvent;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EventSinkServiceImpl implements EventSinkService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final PayoutEventDao payoutEventDao;

    @Autowired
    public EventSinkServiceImpl(PayoutEventDao payoutEventDao) {
        this.payoutEventDao = payoutEventDao;
    }

    @Override
    public Long getLastEventId() throws StorageException {
        try {
            return payoutEventDao.getLastEventId();
        } catch (DaoException ex) {
            throw new StorageException("Failed to get last event id", ex);
        }
    }

    @Override
    public PayoutEvent getEvent(long eventId) throws StorageException {
        try {
            return payoutEventDao.getEvent(eventId);
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to get payout event, eventId=%d", eventId), ex);
        }
    }

    @Override
    public List<PayoutEvent> getEvents(Optional<Long> after, int limit) throws StorageException {
        try {
            return payoutEventDao.getEvents(after, limit);
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to get payout event range, after=%s, limit=%d", after, limit), ex);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void saveEvent(PayoutEvent payoutEvent) throws StorageException {
        log.debug("Trying to save payout event, payoutId='{}', eventType='{}'", payoutEvent.getPayoutId(), payoutEvent.getEventType());
        try {
            payoutEvent.setEventCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
            long eventId = payoutEventDao.saveEvent(payoutEvent);
            log.info("Payout event has been successfully saved, payoutId='{}', eventId='{}', eventType='{}'", payoutEvent.getPayoutId(), eventId, payoutEvent.getEventType());
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to save payout event, payoutId=%s", payoutEvent.getPayoutId()), ex);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void savePayoutCreatedEvent(String payoutId, String purpose, Payout payout, List<FinalCashFlowPosting> cashFlowPostings, UserInfo userInfo) throws StorageException {
        PayoutEvent payoutEvent = new PayoutEvent();
        payoutEvent.setPayoutId(payoutId);
        payoutEvent.setEventType(PayoutChange._Fields.PAYOUT_CREATED.getFieldName());
        payoutEvent.setPayoutStatus(com.rbkmoney.damsel.payout_processing.PayoutStatus._Fields.UNPAID.getFieldName());
        payoutEvent.setPayoutCreatedAt(payout.getCreatedAt());
        payoutEvent.setPayoutPartyId(payout.getPartyId());
        payoutEvent.setPayoutShopId(payout.getShopId());
        payoutEvent.setContractId(payout.getContractId());
        payoutEvent.setPayoutType(payout.getType().getLiteral());

        payoutEvent.setPayoutAccountType(payout.getAccountType().getLiteral());
        payoutEvent.setPayoutAccountId(payout.getBankAccount());
        payoutEvent.setPayoutAccountLegalName(payout.getAccountLegalName());
        payoutEvent.setPayoutAccountTradingName(payout.getAccountTradingName());
        payoutEvent.setPayoutAccountRegisteredAddress(payout.getAccountRegisteredAddress());
        payoutEvent.setPayoutAccountActualAddress(payout.getAccountActualAddress());
        payoutEvent.setPayoutAccountRegisteredNumber(payout.getAccountRegisteredNumber());
        payoutEvent.setPayoutAccountBankPostId(payout.getBankPostAccount());
        payoutEvent.setPayoutAccountBankName(payout.getBankName());
        payoutEvent.setPayoutAccountBankNumber(payout.getBankNumber());
        payoutEvent.setPayoutAccountBankAddress(payout.getBankAddress());
        payoutEvent.setPayoutAccountBankBic(payout.getBankBic());
        payoutEvent.setPayoutAccountBankIban(payout.getBankIban());
        payoutEvent.setPayoutAccountBankLocalCode(payout.getBankLocalCode());
        payoutEvent.setPayoutAccountBankAbaRtn(payout.getBankAbaRtn());
        payoutEvent.setPayoutAccountBankCountryCode(payout.getBankCountryCode());

        //OH SHI—
        payoutEvent.setPayoutInternationalCorrespondentAccountBankAccount(payout.getIntCorrBankAccount());
        payoutEvent.setPayoutInternationalCorrespondentAccountBankName(payout.getIntCorrBankName());
        payoutEvent.setPayoutInternationalCorrespondentAccountBankNumber(payout.getIntCorrBankNumber());
        payoutEvent.setPayoutInternationalCorrespondentAccountBankAddress(payout.getIntCorrBankAddress());
        payoutEvent.setPayoutInternationalCorrespondentAccountBankBic(payout.getIntCorrBankBic());
        payoutEvent.setPayoutInternationalCorrespondentAccountBankIban(payout.getIntCorrBankIban());
        payoutEvent.setPayoutInternationalCorrespondentAccountBankAbaRtn(payout.getIntCorrBankAbaRtn());
        payoutEvent.setPayoutInternationalCorrespondentAccountBankCountryCode(payout.getIntCorrBankCountryCode());

        payoutEvent.setPayoutAccountInn(payout.getInn());
        payoutEvent.setPayoutAccountPurpose(purpose);

        try {
            payoutEvent.setPayoutCashFlow(
                    new ObjectMapper().writeValueAsString(cashFlowPostings.stream().map(
                            cashFlowPosting -> {
                                try {
                                    return new TBaseProcessor().process(cashFlowPosting, new JsonHandler());
                                } catch (IOException ex) {
                                    throw new RuntimeJsonMappingException(ex.getMessage());
                                }
                            }).collect(Collectors.toList())
                    )
            );
        } catch (IOException ex) {
            throw new StorageException("Failed to generate cash flow", ex);
        }

        payoutEvent.setPayoutAccountLegalAgreementId(payout.getAccountLegalAgreementId());
        payoutEvent.setPayoutAccountLegalAgreementSignedAt(payout.getAccountLegalAgreementSignedAt());

        payoutEvent.setUserId(userInfo.getId());
        payoutEvent.setUserType(userInfo.getType().getSetField().getFieldName());

        saveEvent(payoutEvent);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void savePayoutPaidEvent(String payoutId) throws StorageException {
        PayoutEvent payoutEvent = new PayoutEvent();
        payoutEvent.setPayoutId(payoutId);
        payoutEvent.setEventType(PayoutChange._Fields.PAYOUT_STATUS_CHANGED.getFieldName());
        payoutEvent.setPayoutStatus(com.rbkmoney.damsel.payout_processing.PayoutStatus._Fields.PAID.getFieldName());

        saveEvent(payoutEvent);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void savePayoutCancelledEvent(String payoutId, String details, UserInfo userInfo) throws StorageException {
        PayoutEvent payoutEvent = new PayoutEvent();
        payoutEvent.setPayoutId(payoutId);
        payoutEvent.setEventType(PayoutChange._Fields.PAYOUT_STATUS_CHANGED.getFieldName());
        payoutEvent.setPayoutStatus(com.rbkmoney.damsel.payout_processing.PayoutStatus._Fields.CANCELLED.getFieldName());
        payoutEvent.setPayoutStatusCancelDetails(details);
        payoutEvent.setUserId(userInfo.getId());
        payoutEvent.setUserType(userInfo.getType().getSetField().getFieldName());

        saveEvent(payoutEvent);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void savePayoutConfirmedEvent(String payoutId, UserInfo userInfo) throws StorageException {
        PayoutEvent payoutEvent = new PayoutEvent();
        payoutEvent.setPayoutId(payoutId);
        payoutEvent.setEventType(PayoutChange._Fields.PAYOUT_STATUS_CHANGED.getFieldName());
        payoutEvent.setPayoutStatus(com.rbkmoney.damsel.payout_processing.PayoutStatus._Fields.CONFIRMED.getFieldName());
        payoutEvent.setUserId(userInfo.getId());
        payoutEvent.setUserType(userInfo.getType().getSetField().getFieldName());

        saveEvent(payoutEvent);
    }
}
