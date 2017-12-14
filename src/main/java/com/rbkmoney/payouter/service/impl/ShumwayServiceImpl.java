package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.damsel.accounter.*;
import com.rbkmoney.payouter.domain.tables.pojos.CashFlowPosting;
import com.rbkmoney.payouter.service.ShumwayService;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ShumwayServiceImpl implements ShumwayService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final AccounterSrv.Iface shumwayClient;

    @Autowired
    public ShumwayServiceImpl(AccounterSrv.Iface shumwayClient) {
        this.shumwayClient = shumwayClient;
    }

    @Override
    public void hold(long payoutId, List<CashFlowPosting> postings) {
        log.debug("Trying to hold payout postings, payoutId={}, postings={}", payoutId, postings);
        try {
            shumwayClient.hold(new PostingPlanChange(toPlanId(payoutId), toPostingBatch(1, postings)));
            log.info("Payout postings has been held, payoutId={}, postings={}", payoutId, postings);
        } catch (TException ex) {
            throw new RuntimeException(String.format("Failed to hold postings, payoutId='%d', postings='%s'", payoutId, postings), ex);
        }
    }

    public void hold(String planId, List<CashFlowPosting> postings) {
        log.debug("Trying to hold payout postings, planId={}, postings={}", planId, postings);
        try {
            shumwayClient.hold(new PostingPlanChange(planId, toPostingBatch(1, postings)));
            log.info("Payout postings has been held, planId={}, postings={}", planId, postings);
        } catch (TException ex) {
            throw new RuntimeException(String.format("Failed to hold postings, planId='%d', postings='%s'", planId, postings), ex);
        }
    }

    @Override
    public void commit(long payoutId, List<CashFlowPosting> postings) {
        log.debug("Trying to commit payout postings, payoutId={}, postings={}", payoutId, postings);
        try {
            shumwayClient.commitPlan(new PostingPlan(toPlanId(payoutId), toPostingBatches(postings)));
            log.info("Payout postings has been committed, payoutId={}, postings={}", payoutId, postings);
        } catch (TException ex) {
            throw new RuntimeException(String.format("Failed to commit postings, payoutId='%d', postings='%s'", payoutId, postings), ex);
        }
    }

    public void commit(String planId, List<CashFlowPosting> postings) {
        log.debug("Trying to commit payout postings, planId={}, postings={}", planId, postings);
        try {
            shumwayClient.commitPlan(new PostingPlan(planId, toPostingBatches(postings)));
            log.info("Payout postings has been committed, payoutId={}, postings={}", planId, postings);
        } catch (TException ex) {
            throw new RuntimeException(String.format("Failed to commit postings, payoutId='%d', postings='%s'", planId, postings), ex);
        }
    }

    @Override
    public void rollback(long payoutId, List<CashFlowPosting> postings) {
        log.debug("Trying to rollback payout postings, payoutId={}, postings={}", payoutId, postings);
        try {
            shumwayClient.rollbackPlan(new PostingPlan(toPlanId(payoutId), toPostingBatches(postings)));
            log.info("Payout postings has been rolled back, payoutId={}, postings={}", payoutId, postings);
        } catch (TException ex) {
            throw new RuntimeException(String.format("Failed to rollback postings, payoutId='%d', postings='%s'", payoutId, postings), ex);
        }
    }

    @Override
    public void revert(long payoutId, List<CashFlowPosting> postings) {
        log.debug("Trying to revert payout postings, payoutId={}, postings={}", payoutId, postings);
        List<CashFlowPosting> revertPostings = postings.stream()
                .map(posting -> {
                    CashFlowPosting revertPosting = new CashFlowPosting();
                    revertPosting.setPayoutId(posting.getPayoutId());
                    revertPosting.setPlanId(toRevertPlanId(payoutId));
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
            doRevert(payoutId, revertPostings);
            log.info("Payout postings has been reverted, payoutId={}, postings={}", payoutId, postings);
        } catch (Exception ex) {
            throw new RuntimeException(String.format("Failed to revert postings, payoutId='%d', postings='%s'", payoutId, postings), ex);
        }
    }

    private void doRevert(long payoutId, List<CashFlowPosting> revertPostings) throws Exception {
        try {
            hold(toRevertPlanId(payoutId), revertPostings);
            commit(toRevertPlanId(payoutId), revertPostings);
        } catch (Exception ex) {
            processRollbackRevertWhenError(payoutId, revertPostings, ex);
        }
    }

    private void processRollbackRevertWhenError(long payoutId, List<CashFlowPosting> postings, Exception parent) throws Exception {
        try {
            rollback(payoutId, postings);
        } catch (Throwable ex) {
            Exception rollbackEx = new RuntimeException(String.format("Failed to rollback postings from revert action, payoutId='%d', postings='%s'", payoutId, postings), ex);
            parent.addSuppressed(rollbackEx);
            log.warn("Inconsistent state of postings in shumway, payoutId='{}', postings='{}'", payoutId, postings, ex);
        }
        throw parent;
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

    private String toRevertPlanId(long payoutId) {
        return "revert_" + toPlanId(payoutId);
    }

}
