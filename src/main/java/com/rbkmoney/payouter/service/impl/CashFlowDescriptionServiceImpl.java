package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.payouter.dao.CashFlowDescriptionDao;
import com.rbkmoney.payouter.dao.PayoutEventDao;
import com.rbkmoney.payouter.domain.tables.pojos.CashFlowDescription;
import com.rbkmoney.payouter.exception.DaoException;
import com.rbkmoney.payouter.exception.StorageException;
import com.rbkmoney.payouter.service.CashFlowDescriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
    public List<CashFlowDescription> get(long payoutId) throws StorageException {
        try {
            return cashFlowDescriptionDao.get(payoutId);
        } catch (DaoException e) {
            throw new StorageException(String.format("Failed to get cash flow description list for payoutId=%d", payoutId), e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void save(List<CashFlowDescription> cashFlowDescription) throws StorageException {
        try {
            cashFlowDescriptionDao.save(cashFlowDescription);
        } catch (DaoException e) {
            throw new StorageException(String.format("Failed to save cash flow description list for payoutId=%d", cashFlowDescription.get(0).getPayoutId()), e);
        }
    }
}