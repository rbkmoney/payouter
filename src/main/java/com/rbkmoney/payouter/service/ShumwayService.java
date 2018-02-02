package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.domain.FinalCashFlowPosting;

import java.util.List;

public interface ShumwayService {

    void hold(long payoutId, List<FinalCashFlowPosting> finalCashFlowPostings);

    void commit(long payoutId);

    void rollback(long payoutId);

    void revert(long payoutId);

}
