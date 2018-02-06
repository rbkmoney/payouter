package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.damsel.accounter.*;
import com.rbkmoney.damsel.base.InvalidRequest;
import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.payouter.dao.CashFlowPostingDao;
import com.rbkmoney.payouter.domain.enums.AccountType;
import com.rbkmoney.payouter.domain.tables.pojos.CashFlowPosting;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.exception.AccounterException;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.service.ShumwayService;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShumwayServiceImpl implements ShumwayService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final AccounterSrv.Iface shumwayClient;

    private final CashFlowPostingDao cashFlowPostingDao;

    private final RetryTemplate retryTemplate;

    @Autowired
    public ShumwayServiceImpl(AccounterSrv.Iface shumwayClient, CashFlowPostingDao cashFlowPostingDao, RetryTemplate retryTemplate) {
        this.shumwayClient = shumwayClient;
        this.cashFlowPostingDao = cashFlowPostingDao;
        this.retryTemplate = retryTemplate;
    }

    @Override
    public void hold(long payoutId, List<FinalCashFlowPosting> finalCashFlowPostings) {
        hold(payoutId, toPlanId(payoutId), 1L, toCashFlowPostings(finalCashFlowPostings));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void hold(long payoutId, String planId, long batchId, List<CashFlowPosting> cashFlowPostings) {
        log.debug("Trying to hold payout postings, payoutId='{}', cashFlowPostings='{}'", payoutId, cashFlowPostings);
        List<CashFlowPosting> newCashFlowPostings = cashFlowPostings.stream().map(cashFlowPosting -> {
            cashFlowPosting.setPayoutId(payoutId);
            cashFlowPosting.setPlanId(planId);
            cashFlowPosting.setBatchId(batchId);
            return cashFlowPosting;
        }).collect(Collectors.toList());

        try {
            cashFlowPostingDao.save(cashFlowPostings);
            hold(planId, toPostingBatch(batchId, newCashFlowPostings));
            log.info("Payout has been held, payoutId='{}', cashFlowPostings='{}'", payoutId, newCashFlowPostings);
        } catch (Exception ex) {
            throw new AccounterException(String.format("Failed to hold payout, payoutId='%d'", payoutId), ex);
        }
    }

    public void hold(String postingPlanId, PostingBatch postingBatch) throws TException {
        try {
            log.debug("Start hold operation, postingPlanId='{}', postingBatch='{}'", postingPlanId, postingBatch);
            retryTemplate.execute(
                    context -> shumwayClient.hold(new PostingPlanChange(postingPlanId, postingBatch))
            );
        } finally {
            log.debug("End hold operation, postingPlanId='{}', postingBatch='{}'", postingPlanId, postingBatch);
        }
    }

    @Override
    public void commit(long payoutId) {
        log.debug("Trying to commit payout postings, payoutId='{}'", payoutId);
        try {
            List<CashFlowPosting> cashFlowPostings = cashFlowPostingDao.getByPayoutId(payoutId);
            if (cashFlowPostings.isEmpty()) {
                throw new NotFoundException(String.format("Cash flow posting for commit not found, payoutId='%d'", payoutId));
            }

            commit(toPlanId(payoutId), toPostingBatches(cashFlowPostings));
            log.info("Payout has been committed, payoutId='{}', cashFlowPostings='{}'", payoutId, cashFlowPostings);
        } catch (Exception ex) {
            throw new AccounterException(String.format("Failed to commit payout, payoutId='%d'", payoutId), ex);
        }
    }

    public void commit(String postingPlanId, List<PostingBatch> postingBatches) throws TException {
        try {
            log.debug("Start commit operation, postingPlanId='{}', postingBatches='{}'", postingPlanId, postingBatches);
            retryTemplate.execute(
                    context -> shumwayClient.commitPlan(new PostingPlan(postingPlanId, postingBatches))
            );
        } finally {
            log.debug("End commit operation, postingPlanId='{}', postingBatches='{}'", postingPlanId, postingBatches);
        }
    }

    @Override
    public void rollback(long payoutId) {
        log.debug("Trying to rollback payout postings, payoutId='{}'", payoutId);
        try {
            List<CashFlowPosting> cashFlowPostings = cashFlowPostingDao.getByPayoutId(payoutId);
            if (cashFlowPostings.isEmpty()) {
                throw new NotFoundException(String.format("Cash flow posting for rollback not found, payoutId='%d'", payoutId));
            }

            rollback(toPlanId(payoutId), toPostingBatches(cashFlowPostings));
            log.info("Payout has been rolled back, payoutId={}", payoutId);
        } catch (Exception ex) {
            throw new AccounterException(String.format("Failed to rollback payout, payoutId='%d'", payoutId), ex);
        }
    }

    public void rollback(String postingPlanId, List<PostingBatch> postingBatches) throws TException {
        try {
            log.debug("Start rollback operation, postingPlanId='{}', postingBatches='{}'", postingPlanId, postingBatches);
            retryTemplate.execute(
                    context -> shumwayClient.rollbackPlan(new PostingPlan(postingPlanId, postingBatches))
            );
        } finally {
            log.debug("End rollback operation, postingPlanId='{}', postingBatches='{}'", postingPlanId, postingBatches);
        }
    }

    @Override
    public void revert(long payoutId) {
        log.debug("Trying to revert payout, payoutId={}", payoutId);
        try {
            List<CashFlowPosting> cashFlowPostings = cashFlowPostingDao.getByPayoutId(payoutId);
            if (cashFlowPostings.isEmpty()) {
                throw new NotFoundException(String.format("Cash flow posting for revert not found, payoutId='%d'", payoutId));
            }

            doRevert(payoutId, cashFlowPostings);
            log.info("Payout has been reverted, payoutId={}", payoutId);
        } catch (Exception ex) {
            throw new AccounterException(String.format("Failed to revert payout, payoutId='%d'", payoutId), ex);
        }
    }

    private void doRevert(long payoutId, List<CashFlowPosting> cashFlowPostings) throws Exception {
        String revertPlanId = toRevertPlanId(payoutId);
        List<CashFlowPosting> revertCashFlowPostings = cashFlowPostings.stream()
                .map(cashFlowPosting -> toRevertCashFlowPosting(cashFlowPosting))
                .collect(Collectors.toList());

        try {
            hold(revertPlanId, toPostingBatch(1L, revertCashFlowPostings));
            commit(revertPlanId, toPostingBatches(revertCashFlowPostings));
        } catch (Exception ex) {
            processRollbackRevertWhenError(revertPlanId, toPostingBatches(revertCashFlowPostings), ex);
        }
    }

    private void processRollbackRevertWhenError(String revertPlanId, List<PostingBatch> postingBatches, Exception parent) throws Exception {
        try {
            rollback(revertPlanId, postingBatches);
        } catch (Exception ex) {
            if (!(ex instanceof InvalidRequest)) {
                log.error("Inconsistent state of postings in shumway, revertPlanId='{}', postingBatches='{}'", revertPlanId, postingBatches, ex);
            }
            Exception rollbackEx = new RuntimeException(String.format("Failed to rollback postings from revert action, revertPlanId='%s', postingBatches='%s'", revertPlanId, postingBatches), ex);
            rollbackEx.addSuppressed(parent);
            throw rollbackEx;
        }
        throw parent;
    }

    private CashFlowPosting toRevertCashFlowPosting(CashFlowPosting cashFlowPosting) {
        CashFlowPosting revertCashFlowPosting = new CashFlowPosting();
        revertCashFlowPosting.setBatchId(1L);
        revertCashFlowPosting.setFromAccountId(cashFlowPosting.getToAccountId());
        revertCashFlowPosting.setFromAccountType(cashFlowPosting.getToAccountType());
        revertCashFlowPosting.setToAccountId(cashFlowPosting.getFromAccountId());
        revertCashFlowPosting.setToAccountType(cashFlowPosting.getFromAccountType());
        revertCashFlowPosting.setAmount(cashFlowPosting.getAmount());
        revertCashFlowPosting.setCurrencyCode(cashFlowPosting.getCurrencyCode());
        revertCashFlowPosting.setDescription("Revert payout: " + cashFlowPosting.getPayoutId());
        return revertCashFlowPosting;
    }

    private List<PostingBatch> toPostingBatches(List<CashFlowPosting> postings) {
        return postings.stream()
                .collect(Collectors.groupingBy(CashFlowPosting::getBatchId, Collectors.toList()))
                .entrySet().stream()
                .map(entry -> toPostingBatch(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private PostingBatch toPostingBatch(long batchId, List<CashFlowPosting> postings) {
        return new PostingBatch(
                batchId,
                postings.stream()
                        .map(cashFlowPosting -> toPosting(cashFlowPosting))
                        .collect(Collectors.toList())
        );
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

    private List<CashFlowPosting> toCashFlowPostings(List<FinalCashFlowPosting> finalCashFlowPostings) {
        return finalCashFlowPostings.stream()
                .map(finalCashFlowPosting -> {
                    CashFlowPosting cashFlowPosting = new CashFlowPosting();
                    FinalCashFlowAccount source = finalCashFlowPosting.getSource();
                    cashFlowPosting.setFromAccountId(source.getAccountId());
                    cashFlowPosting.setFromAccountType(toAccountType(source.getAccountType()));
                    FinalCashFlowAccount destination = finalCashFlowPosting.getDestination();
                    cashFlowPosting.setToAccountId(destination.getAccountId());
                    cashFlowPosting.setToAccountType(toAccountType(destination.getAccountType()));
                    cashFlowPosting.setAmount(finalCashFlowPosting.getVolume().getAmount());
                    cashFlowPosting.setCurrencyCode(finalCashFlowPosting.getVolume().getCurrency().getSymbolicCode());
                    cashFlowPosting.setDescription(finalCashFlowPosting.getDetails());
                    return cashFlowPosting;
                }).collect(Collectors.toList());
    }

    private AccountType toAccountType(CashFlowAccount cashFlowAccount) {
        CashFlowAccount._Fields cashFlowAccountType = cashFlowAccount.getSetField();
        switch (cashFlowAccountType) {
            case SYSTEM:
                switch (cashFlowAccount.getSystem()) {
                    case settlement:
                        return AccountType.SYSTEM_SETTLEMENT;
                    default:
                        throw new IllegalArgumentException();
                }
            case EXTERNAL:
                switch (cashFlowAccount.getExternal()) {
                    case income:
                        return AccountType.EXTERNAL_INCOME;
                    case outcome:
                        return AccountType.EXTERNAL_OUTCOME;
                    default:
                        throw new IllegalArgumentException();
                }
            case MERCHANT:
                switch (cashFlowAccount.getMerchant()) {
                    case settlement:
                        return AccountType.MERCHANT_SETTLEMENT;
                    case guarantee:
                        return AccountType.MERCHANT_GUARANTEE;
                    default:
                        throw new IllegalArgumentException();
                }
            case PROVIDER:
                switch (cashFlowAccount.getProvider()) {
                    case settlement:
                        return AccountType.PROVIDER_SETTLEMENT;
                    default:
                        throw new IllegalArgumentException();
                }
            default:
                throw new IllegalArgumentException();
        }
    }

    private CashFlowAccount toCashFlowAccount(AccountType accountType) {
        switch (accountType) {
            case PROVIDER_SETTLEMENT:
                return CashFlowAccount.provider(ProviderCashFlowAccount.settlement);
            case MERCHANT_SETTLEMENT:
                return CashFlowAccount.merchant(MerchantCashFlowAccount.settlement);
            case MERCHANT_GUARANTEE:
                return CashFlowAccount.merchant(MerchantCashFlowAccount.guarantee);
            case SYSTEM_SETTLEMENT:
                return CashFlowAccount.system(SystemCashFlowAccount.settlement);
            case EXTERNAL_INCOME:
                return CashFlowAccount.external(ExternalCashFlowAccount.income);
            case EXTERNAL_OUTCOME:
                return CashFlowAccount.external(ExternalCashFlowAccount.outcome);
            default:
                throw new IllegalArgumentException();
        }
    }

    private String toPlanId(long payoutId) {
        return "payout_" + payoutId;
    }

    private String toRevertPlanId(long payoutId) {
        return "revert_" + toPlanId(payoutId);
    }

}
