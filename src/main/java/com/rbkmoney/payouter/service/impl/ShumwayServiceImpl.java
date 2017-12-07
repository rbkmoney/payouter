package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.damsel.accounter.*;
import com.rbkmoney.payouter.domain.tables.pojos.CashFlowPosting;
import com.rbkmoney.payouter.service.ShumwayService;
import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ShumwayServiceImpl implements ShumwayService {

    private final AccounterSrv.Iface shumwayClient;

    @Autowired
    public ShumwayServiceImpl(AccounterSrv.Iface shumwayClient) {
        this.shumwayClient = shumwayClient;
    }

    @Override
    public void hold(long payoutId, List<CashFlowPosting> postings) {
        try {
            shumwayClient.hold(new PostingPlanChange(toPlanId(payoutId), toPostingBatch(1, postings)));
        } catch (TException ex) {
            throw new RuntimeException("Failed to hold postings, payoutId=" + payoutId, ex);
        }
    }

    @Override
    public void commit(long payoutId, List<CashFlowPosting> postings) {
        try {
            shumwayClient.commitPlan(new PostingPlan(toPlanId(payoutId), toPostingBatches(postings)));
        } catch (TException ex) {
            throw new RuntimeException("Failed to commit postings, payoutId=" + payoutId, ex);
        }
    }

    @Override
    public void rollback(long payoutId, List<CashFlowPosting> postings) {
        try {
            shumwayClient.rollbackPlan(new PostingPlan(toPlanId(payoutId), toPostingBatches(postings)));
        } catch (TException ex) {
            throw new RuntimeException("Failed to rollback postings, payoutId=" + payoutId, ex);
        }
    }

    @Override
    public void revert(long payoutId, List<CashFlowPosting> postings) {
        List<CashFlowPosting> revertPostings = postings.stream()
                .map(posting -> {
                    CashFlowPosting revertPosting = new CashFlowPosting();
                    revertPosting.setPayoutId(posting.getPayoutId());
                    revertPosting.setPlanId("revert_" + posting.getPlanId());
                    revertPosting.setFromAccountId(posting.getToAccountId());
                    revertPosting.setFromAccountType(posting.getToAccountType());
                    revertPosting.setToAccountId(posting.getFromAccountId());
                    revertPosting.setToAccountType(posting.getFromAccountType());
                    revertPosting.setCurrencyCode(posting.getCurrencyCode());
                    revertPosting.setAmount(posting.getAmount());
                    revertPosting.setDescription(
                            Optional.ofNullable(posting.getDescription())
                                    .map(rPosting -> "Revert " + rPosting)
                                    .orElse(null)
                    );
                    return revertPosting;
                }).collect(Collectors.toList());
        try {
            hold(payoutId, revertPostings);
            commit(payoutId, revertPostings);
        } catch (RuntimeException ex) {
            throw new RuntimeException("Failed to revert postings, payoutId=" + payoutId, ex);
        }
    }

    private PostingBatch toPostingBatch(long batchId, List<CashFlowPosting> postings) {
        return new PostingBatch(
                batchId,
                postings.stream()
                        .map(cashFlowPosting -> toPosting(cashFlowPosting))
                        .collect(Collectors.toList())
        );
    }

    private List<PostingBatch> toPostingBatches(List<CashFlowPosting> postings) {
        return postings.stream()
                .collect(Collectors.groupingBy(CashFlowPosting::getBatchId, Collectors.toList()))
                .entrySet().stream()
                .map(entry -> toPostingBatch(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private Posting toPosting(CashFlowPosting cashFlowPosting) {
        Posting posting = new Posting();
        posting.setFromId(cashFlowPosting.getFromAccountId());
        posting.setToId(cashFlowPosting.getToAccountId());
        posting.setAmount(cashFlowPosting.getAmount());
        posting.setCurrencySymCode(cashFlowPosting.getCurrencyCode());
        posting.setDescription(cashFlowPosting.getDescription());

        return posting;
    }

    private String toPlanId(long payoutId) {
        return "payout_" + payoutId;
    }

}
