package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.domain.tables.pojos.CashFlowPosting;
import com.rbkmoney.payouter.exception.DaoException;

import java.util.List;

public interface CashFlowPostingDao extends GenericDao {

    void save(List<CashFlowPosting> cashFlowPostings) throws DaoException;

    List<CashFlowPosting> getByPayoutId(long payoutId) throws DaoException;

}
