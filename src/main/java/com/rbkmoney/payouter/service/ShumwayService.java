package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.accounter.PostingPlanLog;
import com.rbkmoney.damsel.domain.FinalCashFlowPosting;

import java.util.List;

public interface ShumwayService {

    PostingPlanLog hold(String payoutId, List<FinalCashFlowPosting> finalCashFlowPostings);

    void commit(String payoutId);

    void rollback(String payoutId);

    void revert(String payoutId);

    List<FinalCashFlowPosting> getPostings(String payoutId);

}
