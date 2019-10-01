package com.rbkmoney.payouter.service;

import com.rbkmoney.damsel.accounter.PostingPlanLog;
import com.rbkmoney.damsel.domain.FinalCashFlowPosting;
import com.rbkmoney.damsel.shumpune.Balance;
import com.rbkmoney.damsel.shumpune.Clock;

import java.util.List;

public interface ShumwayService {

    Clock hold(String payoutId, List<FinalCashFlowPosting> finalCashFlowPostings);

    void commit(String payoutId);

    void rollback(String payoutId);

    void revert(String payoutId);

    Balance getBalance(Long accountId, Clock clock, String payoutId);

    List<FinalCashFlowPosting> getPostings(String payoutId);

}
