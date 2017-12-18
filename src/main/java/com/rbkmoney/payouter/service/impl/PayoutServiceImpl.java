package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.damsel.payout_processing.ShopParams;
import com.rbkmoney.payouter.dao.*;
import com.rbkmoney.payouter.domain.enums.AccountType;
import com.rbkmoney.payouter.domain.enums.PayoutStatus;
import com.rbkmoney.payouter.domain.enums.PayoutType;
import com.rbkmoney.payouter.domain.tables.pojos.*;
import com.rbkmoney.payouter.exception.DaoException;
import com.rbkmoney.payouter.exception.InvalidStateException;
import com.rbkmoney.payouter.exception.StorageException;
import com.rbkmoney.payouter.model.PayoutToolData;
import com.rbkmoney.payouter.service.PartyManagementService;
import com.rbkmoney.payouter.service.PayoutService;
import com.rbkmoney.payouter.service.ShumwayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PayoutServiceImpl implements PayoutService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ShopMetaDao shopMetaDao;

    private final PaymentDao paymentDao;

    private final RefundDao refundDao;

    private final AdjustmentDao adjustmentDao;

    private final PayoutDao payoutDao;

    private final ShumwayService shumwayService;

    private final PartyManagementService partyManagementService;

    @Autowired
    public PayoutServiceImpl(ShopMetaDao shopMetaDao,
                             PaymentDao paymentDao,
                             RefundDao refundDao,
                             AdjustmentDao adjustmentDao,
                             PayoutDao payoutDao,
                             ShumwayService shumwayService,
                             PartyManagementService partyManagementService) {
        this.shopMetaDao = shopMetaDao;
        this.paymentDao = paymentDao;
        this.refundDao = refundDao;
        this.adjustmentDao = adjustmentDao;
        this.payoutDao = payoutDao;
        this.shumwayService = shumwayService;
        this.partyManagementService = partyManagementService;
        //over
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public List<Long> createPayouts(LocalDateTime fromTime, LocalDateTime toTime, PayoutType payoutType) throws InvalidStateException, StorageException {
        log.debug("Trying to create payouts, fromTime={}, toTime={}, payoutType={}", fromTime, toTime, payoutType);
        try {
            List<ShopParams> shops = payoutDao.getUnpaidShops(fromTime, toTime);

            if (shops.isEmpty()) {
                log.info("No shops found for creating payouts, fromTime={}, toTime={}, payoutType={}", fromTime, toTime, payoutType);
                return Collections.emptyList();
            }

            List<Long> payoutIds = shops.stream()
                    .map(shopParams ->
                            createPayout(shopParams.getPartyId(), shopParams.getShopId(), fromTime, toTime, payoutType))
                    .collect(Collectors.toList());
            log.info("Payouts successfully created, payoutIds='{}', fromTime={}, toTime={}, payoutType={}",
                    payoutIds, fromTime, toTime, payoutType);

            return payoutIds;
        } catch (DaoException ex) {
            throw new StorageException(
                    String.format("Failed to create payouts, fromTime='%s', toTime='%s', payoutType='%s'", fromTime, toTime, payoutType), ex);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public long createPayout(String partyId, String shopId, LocalDateTime fromTime, LocalDateTime toTime, PayoutType payoutType) throws InvalidStateException, StorageException {
        log.debug("Trying to create payout, partyId={}, shopId={}, fromTime={}, toTime={}, payoutType={}",
                partyId, shopId, fromTime, toTime, payoutType);
        try {
            ShopMeta shopMeta = shopMetaDao.getExclusive(partyId, shopId);

            List<Payment> payments = paymentDao.getUnpaid(partyId, shopId, toTime);
            List<Refund> refunds = refundDao.getUnpaid(partyId, shopId, toTime);
            List<Adjustment> adjustments = adjustmentDao.getUnpaid(partyId, shopId, toTime);

            long availableAmount = calculateAvailableAmount(payments, refunds, adjustments);

            if (availableAmount <= 0) {
                throw new InvalidStateException("Available amount must be greater than 0");
            }

            Payout payout = buildPayout(partyId, shopId, fromTime, toTime, payoutType);
            payout.setAmount(availableAmount);

            long payoutId = payoutDao.save(payout);

            paymentDao.includeToPayout(payoutId, payments);
            refundDao.includeToPayout(payoutId, refunds);
            adjustmentDao.includeToPayout(payoutId, adjustments);

            shopMetaDao.updateLastPayoutCreatedAt(shopMeta.getPartyId(), shopMeta.getShopId(), payout.getCreatedAt());

            shumwayService.hold(payoutId, buildPostings(payout));

            log.info("Payout successfully created, payoutId='{}', partyId={}, shopId={}, fromTime={}, toTime={}, payoutType={}",
                    payoutId, partyId, shopId, fromTime, toTime, payoutType);

            return payoutId;
        } catch (DaoException ex) {
            throw new StorageException(
                    String.format("Failed to create payout, partyId='%s', shopId='%s', fromTime='%s', toTime='%s', payoutType='%s'",
                            partyId, shopId, fromTime, toTime, payoutType), ex);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void pay(long payoutId) throws InvalidStateException, StorageException {
        log.debug("Trying to pay a payout, payoutId={}", payoutId);
        try {
            Payout payout = payoutDao.getExclusive(payoutId);

            if (payout.getStatus() != PayoutStatus.UNPAID) {
                throw new InvalidStateException(
                        String.format("Invalid status for 'pay' action, payoutId='%d', currentStatus='%s'", payoutId, payout.getStatus())
                );
            }

            payoutDao.changeStatus(payoutId, PayoutStatus.PAID);
            log.info("Payout have been paid, payoutId={}", payoutId);
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to pay a payout, payoutId='%d'", payoutId), ex);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void confirm(long payoutId) throws InvalidStateException, StorageException {
        log.debug("Trying to confirm a payout, payoutId={}", payoutId);
        try {
            Payout payout = payoutDao.getExclusive(payoutId);

            if (payout.getStatus() != PayoutStatus.PAID) {
                throw new InvalidStateException(
                        String.format("Invalid status for 'confirm' action, payoutId='%d', currentStatus='%s'", payoutId, payout.getStatus())
                );
            }

            payoutDao.changeStatus(payoutId, PayoutStatus.CONFIRMED);
            shumwayService.commit(payoutId, buildPostings(payout));
            log.info("Payout have been confirmed, payoutId={}", payoutId);
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to confirm a payout, payoutId='%d'", payoutId), ex);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void cancel(long payoutId) throws InvalidStateException, StorageException {
        log.debug("Trying to cancel a payout, payoutId={}", payoutId);
        try {
            Payout payout = payoutDao.getExclusive(payoutId);

            switch (payout.getStatus()) {
                case UNPAID:
                case PAID:
                    payoutDao.changeStatus(payoutId, PayoutStatus.CANCELLED);
                    shumwayService.rollback(payoutId, buildPostings(payout));
                    break;
                case CONFIRMED:
                    payoutDao.changeStatus(payoutId, PayoutStatus.CANCELLED);
                    shumwayService.revert(payoutId, buildPostings(payout));
                    break;
                default:
                    throw new InvalidStateException(String.format("Invalid status for 'cancel' action, payoutId='%d', currentStatus='%s'", payoutId, payout.getStatus()));
            }
            log.info("Payout have been cancelled, payoutId={}", payoutId);
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to cancel a payout, payoutId='%d'", payoutId), ex);
        }
    }

    @Override
    public List<Payout> search(Optional<PayoutStatus> payoutStatus, Optional<LocalDateTime> fromTime, Optional<LocalDateTime> toTime, Optional<List<Long>> payoutIds, long fromId, int size) {
        return payoutDao.search(payoutStatus, fromTime, toTime, payoutIds, fromId, size);
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional(propagation = Propagation.REQUIRED)
    public void processUnpaidPayouts() {
        List<Payout> unpaidPayouts = payoutDao.getUnpaidPayouts();
        List<Payout> paidPayouts = new ArrayList<>();
        for (Payout payout : unpaidPayouts) {
            try {
                pay(payout.getId());
                paidPayouts.add(payout);
            } catch (Exception ex) {
                log.warn(ex.getMessage(), ex);
            }
            //TODO mail report
        }
    }

    private Payout buildPayout(String partyId, String shopId, LocalDateTime fromTime, LocalDateTime toTime, PayoutType payoutType) {
        Payout payout = new Payout();
        payout.setPartyId(partyId);
        payout.setShopId(shopId);
        payout.setFromTime(fromTime);
        payout.setToTime(toTime);
        payout.setPayoutType(payoutType);
        payout.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        payout.setStatus(PayoutStatus.UNPAID);

        PayoutToolData payoutToolData = partyManagementService.getPayoutToolData(partyId, shopId);
        payout.setCurrencyCode(payoutToolData.getCurrencyCode());
        payout.setBankAccount(payoutToolData.getBankAccount());
        payout.setBankName(payoutToolData.getBankName());
        payout.setBankBik(payoutToolData.getBankBik());
        payout.setInn(payoutToolData.getInn());
        payout.setShopAcc(payoutToolData.getShopAccountId());
        payout.setShopPayoutAcc(payoutToolData.getShopPayoutAccountId());
        payout.setBankPostAccount(payoutToolData.getBankPostAccount());
        payout.setAccountLegalAgreementId(payoutToolData.getLegalAgreementId());
        payout.setAccountLegalAgreementSignedAt(payoutToolData.getLegalAgreementSignedAt());

        return payout;
    }

    private List<CashFlowPosting> buildPostings(Payout payout) {
        CashFlowPosting cashFlowPosting = new CashFlowPosting();
        cashFlowPosting.setFromAccountId(payout.getShopAcc());
        cashFlowPosting.setFromAccountType(AccountType.merchant);
        cashFlowPosting.setToAccountId(payout.getShopPayoutAcc());
        cashFlowPosting.setFromAccountType(AccountType.merchant);
        cashFlowPosting.setAmount(payout.getAmount());
        cashFlowPosting.setCurrencyCode(payout.getCurrencyCode());
        cashFlowPosting.setDescription("Payout amount");
        return Arrays.asList(cashFlowPosting);
    }

    private long calculateAvailableAmount(List<Payment> payments, List<Refund> refunds, List<Adjustment> adjustments) {
        long paymentAmount = payments.stream()
                .mapToLong(payment -> payment.getAmount() - payment.getFee())
                .sum();

        long refundAmount = refunds.stream()
                .mapToLong(refund -> refund.getAmount() + refund.getFee())
                .sum();

        long adjustmentAmount = adjustments.stream()
                .mapToLong(adjustment -> adjustment.getPaymentFee() - adjustment.getNewFee())
                .sum();

        return paymentAmount + adjustmentAmount - refundAmount;
    }

}
