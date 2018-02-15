package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.domain.FinalCashFlowPosting;

import java.util.List;

public interface ShumwayService {

    void hold(String payoutId, List<FinalCashFlowPosting> finalCashFlowPostings);

    void commit(String payoutId);

    void rollback(String payoutId);

    void revert(String payoutId);

}
