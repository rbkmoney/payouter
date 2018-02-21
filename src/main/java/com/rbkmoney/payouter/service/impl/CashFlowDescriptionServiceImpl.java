package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.payouter.dao.CashFlowDescriptionDao;
import com.rbkmoney.payouter.domain.tables.pojos.Adjustment;
import com.rbkmoney.payouter.domain.tables.pojos.CashFlowDescription;
import com.rbkmoney.payouter.domain.tables.pojos.Payment;
import com.rbkmoney.payouter.domain.tables.pojos.Refund;
import com.rbkmoney.payouter.exception.DaoException;
import com.rbkmoney.payouter.exception.StorageException;
import com.rbkmoney.payouter.service.CashFlowDescriptionService;
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
public class CashFlowDescriptionServiceImpl implements CashFlowDescriptionService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final CashFlowDescriptionDao cashFlowDescriptionDao;

    @Autowired
    public CashFlowDescriptionServiceImpl(CashFlowDescriptionDao cashFlowDescriptionDao) {
        this.cashFlowDescriptionDao = cashFlowDescriptionDao;
    }

    @Override
    public List<CashFlowDescription> get(String payoutId) throws StorageException {
        try {
            return cashFlowDescriptionDao.get(payoutId);
        } catch (DaoException e) {
            throw new StorageException(String.format("Failed to get cash flow description list for payoutId=%s", payoutId), e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void save(long payoutId, String currencyCode, List<Payment> payments, List<Refund> refunds, List<Adjustment> adjustments) throws StorageException {
        List<CashFlowDescription> cashFlowDescriptions = new ArrayList<>();

        long paymentAmount = payments.stream().mapToLong(Payment::getAmount).sum();
        long paymentFee = payments.stream().mapToLong(Payment::getFee).sum();
        LocalDateTime paymentFromTime = payments.stream().map(Payment::getCreatedAt).min(LocalDateTime::compareTo).get();
        LocalDateTime paymentToTime = payments.stream().map(Payment::getCreatedAt).max(LocalDateTime::compareTo).get();
        CashFlowDescription paymentCashFlow = new CashFlowDescription();
        paymentCashFlow.setAmount(paymentAmount);
        paymentCashFlow.setFee(paymentFee);
        paymentCashFlow.setCurrencyCode(currencyCode);
        paymentCashFlow.setCashFlowType(com.rbkmoney.payouter.domain.enums.CashFlowType.payment);
        paymentCashFlow.setCount(payments.size());
        paymentCashFlow.setPayoutId(String.valueOf(payoutId));
        paymentCashFlow.setFromTime(paymentFromTime);
        paymentCashFlow.setToTime(paymentToTime);
        cashFlowDescriptions.add(paymentCashFlow);

        if (!refunds.isEmpty()) {
            long refundAmount = refunds.stream().mapToLong(Refund::getAmount).sum();
            long refundFee = refunds.stream().mapToLong(Refund::getFee).sum();
            LocalDateTime refundFromTime = refunds.stream().map(Refund::getCreatedAt).min(LocalDateTime::compareTo).get();
            LocalDateTime refundToTime = refunds.stream().map(Refund::getCreatedAt).max(LocalDateTime::compareTo).get();
            CashFlowDescription refundCashFlow = new CashFlowDescription();
            refundCashFlow.setAmount(refundAmount);
            refundCashFlow.setFee(refundFee);
            refundCashFlow.setCurrencyCode(currencyCode);
            refundCashFlow.setCashFlowType(com.rbkmoney.payouter.domain.enums.CashFlowType.refund);
            refundCashFlow.setCount(refunds.size());
            refundCashFlow.setPayoutId(String.valueOf(payoutId));
            refundCashFlow.setFromTime(refundFromTime);
            refundCashFlow.setToTime(refundToTime);
            cashFlowDescriptions.add(refundCashFlow);
        }

        if (!adjustments.isEmpty()) {
            long adjustmentFee = adjustments.stream().mapToLong(Adjustment::getPaymentFee).sum();
            long adjustmentNewFee = adjustments.stream().mapToLong(Adjustment::getNewFee).sum();
            LocalDateTime adjustmentFromTime = adjustments.stream().map(Adjustment::getCreatedAt).min(LocalDateTime::compareTo).get();
            LocalDateTime adjustmentToTime = adjustments.stream().map(Adjustment::getCreatedAt).max(LocalDateTime::compareTo).get();
            CashFlowDescription adjustmentCashFlow = new CashFlowDescription();
            adjustmentCashFlow.setAmount(adjustmentFee - adjustmentNewFee);
            adjustmentCashFlow.setCurrencyCode(currencyCode);
            adjustmentCashFlow.setCashFlowType(com.rbkmoney.payouter.domain.enums.CashFlowType.adjustment);
            adjustmentCashFlow.setCount(adjustments.size());
            adjustmentCashFlow.setPayoutId(String.valueOf(payoutId));
            adjustmentCashFlow.setFromTime(adjustmentFromTime);
            adjustmentCashFlow.setToTime(adjustmentToTime);
            cashFlowDescriptions.add(adjustmentCashFlow);
        }

        save(cashFlowDescriptions);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void save(List<CashFlowDescription> cashFlowDescriptions) throws StorageException {
        log.info("Trying to save cash flow descriptions, cashFlowDescriptions='{}'", cashFlowDescriptions);
        try {
            cashFlowDescriptionDao.save(cashFlowDescriptions);
            log.info("Cash flow descriptions have been saved, cashFlowDescriptions='{}'", cashFlowDescriptions);
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to save cash flow descriptions, cashFlowDescriptions='%s'", cashFlowDescriptions), ex);
        }
    }
}