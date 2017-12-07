package com.rbkmoney.payouter.service;

import com.rbkmoney.payouter.domain.tables.pojos.CashFlowPosting;

import java.util.List;


public interface ShumwayService {

    void hold(long payoutId, List<CashFlowPosting> postings);

    void commit(long payoutId, List<CashFlowPosting> postings);

    void rollback(long payoutId, List<CashFlowPosting> postings);

    void revert(long payoutId, List<CashFlowPosting> postings);

}
