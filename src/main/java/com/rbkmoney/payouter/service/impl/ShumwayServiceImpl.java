package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.damsel.accounter.*;
import com.rbkmoney.payouter.dao.PayoutDao;
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

import java.util.Arrays;
import java.util.List;

@Service
public class ShumwayServiceImpl implements ShumwayService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final AccounterSrv.Iface shumwayClient;

    private final PayoutDao payoutDao;

    private final RetryTemplate retryTemplate;

    @Autowired
    public ShumwayServiceImpl(AccounterSrv.Iface shumwayClient, PayoutDao payoutDao, RetryTemplate retryTemplate) {
        this.shumwayClient = shumwayClient;
        this.payoutDao = payoutDao;
        this.retryTemplate = retryTemplate;
    }

    @Override
    public void hold(long payoutId) {
        log.debug("Trying to hold payout, payoutId={}", payoutId);
        try {
            Payout payout = payoutDao.get(payoutId);
            if (payout == null) {
                throw new NotFoundException(String.format("Payout not found, payoutId='%d'", payoutId));
            }

            hold(toPlanId(payoutId), toPostingBatch(payout));
            log.info("Payout has been held, payoutId={}", payoutId);
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
        log.debug("Trying to commit payout, payoutId={}", payoutId);
        try {
            Payout payout = payoutDao.get(payoutId);
            if (payout == null) {
                throw new NotFoundException(String.format("Payout not found, payoutId='%d'", payoutId));
            }

            commit(toPlanId(payoutId), Arrays.asList(toPostingBatch(payout)));
            log.info("Payout has been committed, payoutId={}", payoutId);
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
        log.debug("Trying to rollback payout, payoutId={}", payoutId);
        try {
            Payout payout = payoutDao.get(payoutId);
            if (payout == null) {
                throw new NotFoundException(String.format("Payout not found, payoutId='%d'", payoutId));
            }

            rollback(toPlanId(payoutId), Arrays.asList(toPostingBatch(payout)));
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
            Payout payout = payoutDao.get(payoutId);
            if (payout == null) {
                throw new NotFoundException(String.format("Payout not found, payoutId='%d'", payoutId));
            }

            doRevert(payoutId, payout);
            log.info("Payout has been reverted, payoutId={}", payoutId);
        } catch (Exception ex) {
            throw new AccounterException(String.format("Failed to revert payout, payoutId='%d'", payoutId), ex);
        }
    }

    private void doRevert(long payoutId, Payout payout) throws Exception {
        String revertPlanId = toRevertPlanId(payoutId);
        PostingBatch revertPostingBatch = toRevertPostingBatch(payout);

        try {
            hold(revertPlanId, revertPostingBatch);
            commit(toRevertPlanId(payoutId), Arrays.asList(revertPostingBatch));
        } catch (TException ex) {
            processRollbackRevertWhenError(revertPlanId, Arrays.asList(revertPostingBatch), ex);
        }
    }

    private void processRollbackRevertWhenError(String revertPlanId, List<PostingBatch> postingBatches, TException parent) throws Exception {
        try {
            rollback(revertPlanId, postingBatches);
        } catch (TException ex) {
            log.warn("Inconsistent state of postings in shumway, revertPlanId='{}', postingBatches='{}'", revertPlanId, postingBatches, ex);
            Exception rollbackEx = new RuntimeException(String.format("Failed to rollback postings from revert action, revertPlanId='%s', postingBatches='%s'", revertPlanId, postingBatches), ex);
            rollbackEx.addSuppressed(parent);
            throw rollbackEx;
        }
        throw parent;
    }

    private PostingBatch toRevertPostingBatch(Payout payout) {
        Posting posting = new Posting();
        posting.setFromId(payout.getShopPayoutAcc());
        posting.setToId(payout.getShopAcc());
        posting.setAmount(payout.getAmount());
        posting.setCurrencySymCode(payout.getCurrencyCode());
        posting.setDescription("Revert payout: " + payout.getId());

        return new PostingBatch(
                1L,
                Arrays.asList(posting)
        );
    }

    private PostingBatch toPostingBatch(Payout payout) {
        return new PostingBatch(
                1L,
                Arrays.asList(toPosting(payout))
        );
    }

    private Posting toPosting(Payout payout) {
        Posting posting = new Posting();
        posting.setFromId(payout.getShopAcc());
        posting.setToId(payout.getShopPayoutAcc());
        posting.setAmount(payout.getAmount());
        posting.setCurrencySymCode(payout.getCurrencyCode());
        posting.setDescription("Payout: " + payout.getId());

        return posting;
    }

    private String toPlanId(long payoutId) {
        return "payout_" + payoutId;
    }

    private String toRevertPlanId(long payoutId) {
        return "revert_" + toPlanId(payoutId);
    }

}
