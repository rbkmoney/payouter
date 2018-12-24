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
public class PayoutSummaryServiceImpl implements PayoutSummaryService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final PayoutSummaryDao payoutSummaryDao;

    @Autowired
    public PayoutSummaryServiceImpl(PayoutSummaryDao payoutSummaryDao) {
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