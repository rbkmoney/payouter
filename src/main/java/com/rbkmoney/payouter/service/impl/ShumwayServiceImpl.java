package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.damsel.base.InvalidRequest;
import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.shumpune.*;
import com.rbkmoney.payouter.dao.CashFlowPostingDao;
import com.rbkmoney.payouter.domain.enums.AccountType;
import com.rbkmoney.payouter.domain.tables.pojos.CashFlowPosting;
import com.rbkmoney.payouter.exception.AccounterException;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.service.ShumwayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShumwayServiceImpl implements ShumwayService {

    private final AccounterSrv.Iface shumwayClient;

    private final CashFlowPostingDao cashFlowPostingDao;

    private final RetryTemplate retryTemplate;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Clock hold(String payoutId, List<FinalCashFlowPosting> finalCashFlowPostings) {
        return hold(payoutId, toPlanId(payoutId), 1L, toCashFlowPostings(payoutId, finalCashFlowPostings));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Clock hold(String payoutId, String planId, long batchId, List<CashFlowPosting> cashFlowPostings) {
        log.debug("Trying to hold payout postings, payoutId='{}', cashFlowPostings='{}'", payoutId, cashFlowPostings);
        List<CashFlowPosting> newCashFlowPostings = cashFlowPostings.stream().map(cashFlowPosting -> {
            cashFlowPosting.setPayoutId(payoutId);
            cashFlowPosting.setPlanId(planId);
            cashFlowPosting.setBatchId(batchId);
            return cashFlowPosting;
        }).collect(Collectors.toList());

        try {
            cashFlowPostingDao.save(cashFlowPostings);
            Clock clock = hold(planId, toPostingBatch(batchId, newCashFlowPostings));
            log.info("Payout has been held, payoutId='{}', cashFlowPostings='{}', clock='{}'",
                    payoutId, newCashFlowPostings, clock);
            return clock;
        } catch (Exception ex) {
            throw new AccounterException(String.format("Failed to hold payout, payoutId='%s'", payoutId), ex);
        }
    }

    public Clock hold(String postingPlanId, PostingBatch postingBatch) throws TException {
        try {
            log.debug("Start hold operation, postingPlanId='{}', postingBatch='{}'", postingPlanId, postingBatch);
            return retryTemplate.execute(
                    context -> shumwayClient.hold(new PostingPlanChange(postingPlanId, postingBatch))
            );
        } finally {
            log.debug("End hold operation, postingPlanId='{}', postingBatch='{}'", postingPlanId, postingBatch);
        }
    }

    @Override
    public void commit(String payoutId) {
        log.debug("Trying to commit payout postings, payoutId='{}'", payoutId);
        try {
            List<CashFlowPosting> cashFlowPostings = cashFlowPostingDao.getByPayoutId(payoutId);
            if (cashFlowPostings.isEmpty()) {
                throw new NotFoundException(
                        String.format("Cash flow posting for commit not found, payoutId='%s'", payoutId));
            }

            commit(toPlanId(payoutId), toPostingBatches(cashFlowPostings));
            log.info("Payout has been committed, payoutId='{}', cashFlowPostings='{}'", payoutId, cashFlowPostings);
        } catch (Exception ex) {
            throw new AccounterException(String.format("Failed to commit payout, payoutId='%s'", payoutId), ex);
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
    public void rollback(String payoutId) {
        log.debug("Trying to rollback payout postings, payoutId='{}'", payoutId);
        try {
            List<CashFlowPosting> cashFlowPostings = cashFlowPostingDao.getByPayoutId(payoutId);
            if (cashFlowPostings.isEmpty()) {
                throw new NotFoundException(
                        String.format("Cash flow posting for rollback not found, payoutId='%s'", payoutId));
            }

            rollback(toPlanId(payoutId), toPostingBatches(cashFlowPostings));
            log.info("Payout has been rolled back, payoutId={}", payoutId);
        } catch (Exception ex) {
            throw new AccounterException(String.format("Failed to rollback payout, payoutId='%s'", payoutId), ex);
        }
    }

    public void rollback(String postingPlanId, List<PostingBatch> postingBatches) throws TException {
        try {
            log.debug("Start rollback operation, postingPlanId='{}', postingBatches='{}'",
                    postingPlanId, postingBatches);
            retryTemplate.execute(
                    context -> shumwayClient.rollbackPlan(new PostingPlan(postingPlanId, postingBatches))
            );
        } finally {
            log.debug("End rollback operation, postingPlanId='{}', postingBatches='{}'", postingPlanId, postingBatches);
        }
    }

    @Override
    public void revert(String payoutId) {
        log.debug("Trying to revert payout, payoutId={}", payoutId);
        try {
            List<CashFlowPosting> cashFlowPostings = cashFlowPostingDao.getByPayoutId(payoutId);
            if (cashFlowPostings.isEmpty()) {
                throw new NotFoundException(
                        String.format("Cash flow posting for revert not found, payoutId='%s'", payoutId));
            }

            doRevert(payoutId, cashFlowPostings);
            log.info("Payout has been reverted, payoutId={}", payoutId);
        } catch (Exception ex) {
            throw new AccounterException(String.format("Failed to revert payout, payoutId='%s'", payoutId), ex);
        }
    }

    @Override
    public Balance getBalance(Long accountId, Clock clock, String payoutId) {
        String clockLog = clock.isSetLatest() ? "Latest" : Arrays.toString(clock.getVector().getState());
        try {
            log.debug("Start getBalance operation, payoutId='{}', accountId='{}', clock='{}'",
                    payoutId, accountId, clockLog);
            return retryTemplate.execute(
                    context -> shumwayClient.getBalanceByID(accountId, clock)
            );
        } catch (Exception e) {
            throw new AccounterException(String.format("Failed to getBalance, " +
                    "payoutId='%s', accountId='%s', clock='%s'", payoutId, accountId, clockLog), e);
        } finally {
            log.debug("End getBalance operation, payoutId='{}', accountId='{}', clock='{}'",
                    payoutId, accountId, clockLog);
        }
    }

    @Override
    public List<FinalCashFlowPosting> getPostings(String payoutId) {
        List<CashFlowPosting> cashFlowPostings = cashFlowPostingDao.getByPayoutId(payoutId);
        if (cashFlowPostings.isEmpty()) {
            throw new NotFoundException(String.format("Cash flow posting not found, payoutId='%s'", payoutId));
        }

        return toFinalCashFlowPostings(cashFlowPostings);
    }

    private void doRevert(String payoutId, List<CashFlowPosting> cashFlowPostings) throws Exception {
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

    private void processRollbackRevertWhenError(String planId, List<PostingBatch> postingBatches, Exception parent)
            throws Exception {
        try {
            rollback(planId, postingBatches);
        } catch (Exception ex) {
            if (!(ex instanceof InvalidRequest)) {
                log.error("Inconsistent state of postings in shumway, revertPlanId='{}', postingBatches='{}'",
                        planId, postingBatches, ex);
            }
            var rollbackEx = new RuntimeException(String.format("Failed to rollback postings from revert action, " +
                    "revertPlanId='%s', postingBatches='%s'", planId, postingBatches), ex);
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

    private List<FinalCashFlowPosting> toFinalCashFlowPostings(List<CashFlowPosting> cashFlowPostings) {
        return cashFlowPostings.stream()
                .map(this::toFinalCashFlowPosting)
                .collect(Collectors.toList());
    }

    private FinalCashFlowPosting toFinalCashFlowPosting(CashFlowPosting cashFlowPosting) {
        FinalCashFlowPosting finalCashFlowPosting = new FinalCashFlowPosting();
        finalCashFlowPosting.setSource(
                new FinalCashFlowAccount(toCashFlowAccount(cashFlowPosting.getFromAccountType()),
                        cashFlowPosting.getFromAccountId())
        );
        finalCashFlowPosting.setDestination(
                new FinalCashFlowAccount(toCashFlowAccount(cashFlowPosting.getToAccountType()),
                        cashFlowPosting.getToAccountId())
        );
        finalCashFlowPosting.setVolume(
                new Cash(
                        cashFlowPosting.getAmount(),
                        new CurrencyRef(cashFlowPosting.getCurrencyCode())
                )
        );
        finalCashFlowPosting.setDetails(cashFlowPosting.getDescription());
        return finalCashFlowPosting;
    }

    private List<CashFlowPosting> toCashFlowPostings(String payoutId, List<FinalCashFlowPosting> cashFlowPostings) {
        return cashFlowPostings.stream()
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
                    cashFlowPosting.setDescription(buildCashFlowDescription(payoutId, finalCashFlowPosting));
                    return cashFlowPosting;
                }).collect(Collectors.toList());
    }

    private String buildCashFlowDescription(String payoutId, FinalCashFlowPosting finalCashFlowPosting) {
        String description = "PAYOUT-" + payoutId;
        if (finalCashFlowPosting.isSetDetails()) {
            description += ": " + finalCashFlowPosting.getDetails();
        }
        return description;
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
                    case payout:
                        return AccountType.MERCHANT_PAYOUT;
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
            case MERCHANT_PAYOUT:
                return CashFlowAccount.merchant(MerchantCashFlowAccount.payout);
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

    private String toPlanId(String payoutId) {
        return "payout_" + payoutId;
    }

    private String toRevertPlanId(String payoutId) {
        return "revert_" + toPlanId(payoutId);
    }

}
