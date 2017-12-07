package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.payouter.dao.*;
import com.rbkmoney.payouter.domain.enums.AccountType;
import com.rbkmoney.payouter.domain.enums.PayoutStatus;
import com.rbkmoney.payouter.domain.enums.PayoutType;
import com.rbkmoney.payouter.domain.tables.pojos.*;
import com.rbkmoney.payouter.exception.DaoException;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long createPayout(String partyId, String shopId, LocalDateTime fromTime, LocalDateTime toTime, PayoutType payoutType) {
        log.debug("Trying to create payout, partyId={}, shopId={}, fromTime={}, toTime={}, payoutType={}",
                partyId, shopId, fromTime, toTime, payoutType);
        try {
            shopMetaDao.getExclusive(partyId, shopId);

            List<Payment> payments = paymentDao.getUnpaid(partyId, shopId, toTime);
            List<Refund> refunds = refundDao.getUnpaid(partyId, shopId, toTime);
            List<Adjustment> adjustments = adjustmentDao.getUnpaid(partyId, shopId, toTime);

            long availableAmount = calculateAvailableAmount(payments, refunds, adjustments);

            if (availableAmount <= 0) {
                throw new RuntimeException("Available amount must be greater than 0");
            }

            Payout payout = buildPayout(partyId, shopId, fromTime, toTime, payoutType);
            long payoutId = payoutDao.save(payout);

            paymentDao.includeToPayout(payoutId, payments);
            refundDao.includeToPayout(payoutId, refunds);
            adjustmentDao.includeToPayout(payoutId, adjustments);

            shumwayService.hold(payoutId, buildPostings(payout));

            log.info("Payout successfully created, payoutId='{}', partyId={}, shopId={}, fromTime={}, toTime={}, payoutType={}",
                    payoutId, partyId, shopId, fromTime, toTime, payoutType);

            return payoutId;
        } catch (DaoException ex) {
            throw new StorageException(
                    String.format("Failed to create report, partyId='%s', shopId='%s', fromTime='%s', toTime='%s', payoutType='%s'",
                            partyId, shopId, fromTime, toTime, payoutType), ex);
        }
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional(propagation = Propagation.REQUIRED)
    public void processUnpaidPayouts() {
        List<Payout> unpaidPayouts = payoutDao.getUnpaidPayouts();
        List<Payout> paidPayouts = new ArrayList<>();
        for (Payout payout : unpaidPayouts) {
            try {
                doPaid(payout);
                paidPayouts.add(payout);
            } catch (Exception ex) {
                throw new RuntimeException(String.format("Failed to process unpaid payout, payout='%s'", payout), ex);
            }
            //TODO send mail paidPayouts
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void doPaid(Payout payout) {
        log.debug("Trying to change payout status to 'paid', payout='{}'", payout);
        payoutDao.changeStatus(payout.getId(), PayoutStatus.PAID);
        log.debug("Payout status have been changed, payoutId='{}', status='{}'", payout.getId(), PayoutStatus.PAID);
    }

    private Payout buildPayout(String partyId, String shopId, LocalDateTime fromTime, LocalDateTime toTime, PayoutType payoutType) {
        Payout payout = new Payout();
        payout.setPartyId(partyId);
        payout.setShopId(shopId);
        payout.setFromTime(fromTime);
        payout.setToTime(toTime);
        payout.setPayoutType(payoutType);
        payout.setCurrencyCode("RUB");

        PayoutToolData payoutToolData = partyManagementService.getPayoutToolData(partyId, shopId);
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
