package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.domain.tables.pojos.CashFlowDescription;
import com.rbkmoney.payouter.exception.DaoException;

import java.util.List;

public interface CashFlowDescriptionDao {
    void save(List<CashFlowDescription> cashFlowDescription) throws DaoException;
    List<CashFlowDescription> get(long payoutId) throws DaoException;
}