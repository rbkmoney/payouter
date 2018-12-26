package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.damsel.domain.FinalCashFlowPosting;
import com.rbkmoney.damsel.payout_processing.PayoutChange;
import com.rbkmoney.payouter.dao.PayoutEventDao;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutEvent;
import com.rbkmoney.payouter.exception.DaoException;
import com.rbkmoney.payouter.exception.StorageException;
import com.rbkmoney.payouter.service.EventSinkService;
import com.rbkmoney.payouter.util.DamselUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

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
    public void savePayoutCreatedEvent(Payout payout, List<FinalCashFlowPosting> cashFlowPostings) throws StorageException {
        PayoutEvent payoutEvent = DamselUtil.toPayoutEvent(payout, cashFlowPostings);

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
    public void savePayoutCancelledEvent(String payoutId, String details) throws StorageException {
        PayoutEvent payoutEvent = new PayoutEvent();
        payoutEvent.setPayoutId(payoutId);
        payoutEvent.setEventType(PayoutChange._Fields.PAYOUT_STATUS_CHANGED.getFieldName());
        payoutEvent.setPayoutStatus(com.rbkmoney.damsel.payout_processing.PayoutStatus._Fields.CANCELLED.getFieldName());
        payoutEvent.setPayoutStatusCancelDetails(details);

        saveEvent(payoutEvent);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void savePayoutConfirmedEvent(String payoutId) throws StorageException {
        PayoutEvent payoutEvent = new PayoutEvent();
        payoutEvent.setPayoutId(payoutId);
        payoutEvent.setEventType(PayoutChange._Fields.PAYOUT_STATUS_CHANGED.getFieldName());
        payoutEvent.setPayoutStatus(com.rbkmoney.damsel.payout_processing.PayoutStatus._Fields.CONFIRMED.getFieldName());

        saveEvent(payoutEvent);
    }
}
