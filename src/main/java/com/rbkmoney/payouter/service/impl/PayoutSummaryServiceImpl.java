package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.payouter.dao.PayoutSummaryDao;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutSummary;
import com.rbkmoney.payouter.exception.DaoException;
import com.rbkmoney.payouter.exception.StorageException;
import com.rbkmoney.payouter.service.PayoutSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutSummaryServiceImpl implements PayoutSummaryService {

    private final PayoutSummaryDao payoutSummaryDao;

    @Override
    public List<PayoutSummary> get(String payoutId) throws StorageException {
        try {
            return payoutSummaryDao.get(payoutId);
        } catch (DaoException e) {
            throw new StorageException(
                    String.format("Failed to get cash flow description list for payoutId=%s", payoutId), e);
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
            throw new StorageException(
                    String.format("Failed to save payout summaries, payoutSummaries='%s'", payoutSummaries), ex);
        }
    }
}