package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.payouter.dao.PayoutSummaryDao;
import com.rbkmoney.payouter.domain.enums.PayoutSummaryOperationType;
import com.rbkmoney.payouter.domain.tables.pojos.Adjustment;
import com.rbkmoney.payouter.domain.tables.pojos.Payment;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutSummary;
import com.rbkmoney.payouter.domain.tables.pojos.Refund;
import com.rbkmoney.payouter.exception.DaoException;
import com.rbkmoney.payouter.exception.StorageException;
import com.rbkmoney.payouter.service.PayoutSummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CashFlowDescriptionServiceImpl implements PayoutSummaryService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final PayoutSummaryDao payoutSummaryDao;

    @Autowired
    public CashFlowDescriptionServiceImpl(PayoutSummaryDao payoutSummaryDao) {
        this.payoutSummaryDao = payoutSummaryDao;
    }

    @Override
    public List<PayoutSummary> get(String payoutId) throws StorageException {
        try {
            return payoutSummaryDao.get(payoutId);
        } catch (DaoException e) {
            throw new StorageException(String.format("Failed to get cash flow description list for payoutId=%s", payoutId), e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void save(long payoutId, String currencyCode, List<Payment> payments, List<Refund> refunds, List<Adjustment> adjustments) throws StorageException {
        List<PayoutSummary> cashFlowDescriptions = new ArrayList<>();

        long paymentAmount = payments.stream().mapToLong(Payment::getAmount).sum();
        long paymentFee = payments.stream().mapToLong(Payment::getFee).sum();
        LocalDateTime paymentFromTime = payments.stream().map(Payment::getCapturedAt).min(LocalDateTime::compareTo).get();
        LocalDateTime paymentToTime = payments.stream().map(Payment::getCapturedAt).max(LocalDateTime::compareTo).get();
        PayoutSummary paymentSummary = new PayoutSummary();
        paymentSummary.setAmount(paymentAmount);
        paymentSummary.setFee(paymentFee);
        paymentSummary.setCurrencyCode(currencyCode);
        paymentSummary.setCashFlowType(PayoutSummaryOperationType.payment);
        paymentSummary.setCount(payments.size());
        paymentSummary.setPayoutId(String.valueOf(payoutId));
        paymentSummary.setFromTime(paymentFromTime);
        paymentSummary.setToTime(paymentToTime);
        cashFlowDescriptions.add(paymentSummary);

        if (!refunds.isEmpty()) {
            long refundAmount = refunds.stream().mapToLong(Refund::getAmount).sum();
            long refundFee = refunds.stream().mapToLong(Refund::getFee).sum();
            LocalDateTime refundFromTime = refunds.stream().map(Refund::getSucceededAt).min(LocalDateTime::compareTo).get();
            LocalDateTime refundToTime = refunds.stream().map(Refund::getSucceededAt).max(LocalDateTime::compareTo).get();
            PayoutSummary refundSummary = new PayoutSummary();
            refundSummary.setAmount(refundAmount);
            refundSummary.setFee(refundFee);
            refundSummary.setCurrencyCode(currencyCode);
            refundSummary.setCashFlowType(PayoutSummaryOperationType.refund);
            refundSummary.setCount(refunds.size());
            refundSummary.setPayoutId(String.valueOf(payoutId));
            refundSummary.setFromTime(refundFromTime);
            refundSummary.setToTime(refundToTime);
            cashFlowDescriptions.add(refundSummary);
        }

        if (!adjustments.isEmpty()) {
            long adjustmentFee = adjustments.stream().mapToLong(Adjustment::getPaymentFee).sum();
            long adjustmentNewFee = adjustments.stream().mapToLong(Adjustment::getNewFee).sum();
            LocalDateTime adjustmentFromTime = adjustments.stream().map(Adjustment::getCapturedAt).min(LocalDateTime::compareTo).get();
            LocalDateTime adjustmentToTime = adjustments.stream().map(Adjustment::getCapturedAt).max(LocalDateTime::compareTo).get();
            PayoutSummary adjustmentSummary = new PayoutSummary();
            adjustmentSummary.setAmount(adjustmentFee - adjustmentNewFee);
            adjustmentSummary.setFee(0L);
            adjustmentSummary.setCurrencyCode(currencyCode);
            adjustmentSummary.setCashFlowType(PayoutSummaryOperationType.adjustment);
            adjustmentSummary.setCount(adjustments.size());
            adjustmentSummary.setPayoutId(String.valueOf(payoutId));
            adjustmentSummary.setFromTime(adjustmentFromTime);
            adjustmentSummary.setToTime(adjustmentToTime);
            cashFlowDescriptions.add(adjustmentSummary);
        }

        save(cashFlowDescriptions);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void save(List<PayoutSummary> payoutSummaries) throws StorageException {
        log.info("Trying to save payout summaries, payoutSummaries='{}'", payoutSummaries);
        try {
            payoutSummaryDao.save(payoutSummaries);
            log.info("Payout summaries have been saved, payoutSummaries='{}'", payoutSummaries);
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to save payout summaries, payoutSummaries='%s'", payoutSummaries), ex);
        }
    }
}