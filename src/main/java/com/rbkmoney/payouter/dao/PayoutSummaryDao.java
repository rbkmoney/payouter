package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.domain.tables.pojos.PayoutSummary;
import com.rbkmoney.payouter.exception.DaoException;

import java.util.List;

public interface PayoutSummaryDao {
    void save(List<PayoutSummary> payoutSummaries) throws DaoException;

    List<PayoutSummary> get(String payoutId) throws DaoException;
}