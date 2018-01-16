package com.rbkmoney.payouter.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.payout_processing.PaidDetails;
import com.rbkmoney.damsel.payout_processing.PayoutChange;
import com.rbkmoney.damsel.payout_processing.ShopParams;
import com.rbkmoney.damsel.payout_processing.UserInfo;
import com.rbkmoney.geck.serializer.kit.json.JsonHandler;
import com.rbkmoney.geck.serializer.kit.tbase.TBaseProcessor;
import com.rbkmoney.payouter.dao.*;
import com.rbkmoney.payouter.domain.enums.PayoutStatus;
import com.rbkmoney.payouter.domain.enums.PayoutType;
import com.rbkmoney.payouter.domain.tables.pojos.*;
import com.rbkmoney.payouter.exception.DaoException;
import com.rbkmoney.payouter.exception.InvalidStateException;
import com.rbkmoney.payouter.exception.StorageException;
import com.rbkmoney.payouter.model.PayoutToolData;
import com.rbkmoney.payouter.service.EventSinkService;
import com.rbkmoney.payouter.service.PartyManagementService;
import com.rbkmoney.payouter.service.PayoutService;
import com.rbkmoney.payouter.service.ShumwayService;
import com.rbkmoney.payouter.service.report.Report1CSendService;
import com.rbkmoney.payouter.service.report._1c.Report1CService;
import com.rbkmoney.payouter.util.WoodyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PayoutServiceImpl implements PayoutService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ShopMetaDao shopMetaDao;

    private final PaymentDao paymentDao;

    private final RefundDao refundDao;

    private final AdjustmentDao adjustmentDao;

    private final PayoutDao payoutDao;

    private final ReportDao reportDao;

    private final ShumwayService shumwayService;

    private final PartyManagementService partyManagementService;

    private final EventSinkService eventSinkService;

    private final Report1CSendService report1CSendService;

    private final Report1CService report1CService;

    @Autowired
    public PayoutServiceImpl(ShopMetaDao shopMetaDao,
                             PaymentDao paymentDao,
                             RefundDao refundDao,
                             AdjustmentDao adjustmentDao,
                             PayoutDao payoutDao,
                             ReportDao reportDao,
                             ShumwayService shumwayService,
                             PartyManagementService partyManagementService,
                             EventSinkService eventSinkService,
                             Report1CSendService report1CSendService,
                             Report1CService report1CService) {
        this.shopMetaDao = shopMetaDao;
        this.paymentDao = paymentDao;
        this.refundDao = refundDao;
        this.adjustmentDao = adjustmentDao;
        this.payoutDao = payoutDao;
        this.reportDao = reportDao;
        this.shumwayService = shumwayService;
        this.partyManagementService = partyManagementService;
        this.eventSinkService = eventSinkService;
        this.report1CSendService = report1CSendService;
        this.report1CService = report1CService;
        //over
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public List<Long> createPayouts(LocalDateTime fromTime, LocalDateTime toTime, PayoutType payoutType) throws InvalidStateException, StorageException {
        log.info("Trying to create payouts, fromTime={}, toTime={}, payoutType={}", fromTime, toTime, payoutType);
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
        log.info("Trying to create payout, partyId={}, shopId={}, fromTime={}, toTime={}, payoutType={}",
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
            payout.setId(payoutId);

            paymentDao.includeToPayout(payoutId, payments);
            refundDao.includeToPayout(payoutId, refunds);
            adjustmentDao.includeToPayout(payoutId, adjustments);

            shopMetaDao.updateLastPayoutCreatedAt(shopMeta.getPartyId(), shopMeta.getShopId(), payout.getCreatedAt());

            UserInfo userInfo = WoodyUtils.getUserInfo();
            PayoutEvent payoutEvent = buildPayoutCreatedEvent(payout, userInfo);
            eventSinkService.saveEvent(payoutEvent);

            shumwayService.hold(payoutId);

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
        log.info("Trying to pay a payout, payoutId={}", payoutId);
        try {
            Payout payout = payoutDao.getExclusive(payoutId);

            if (payout.getStatus() != PayoutStatus.UNPAID) {
                throw new InvalidStateException(
                        String.format("Invalid status for 'pay' action, payoutId='%d', currentStatus='%s'", payoutId, payout.getStatus())
                );
            }

            payoutDao.changeStatus(payoutId, PayoutStatus.PAID);
            PayoutEvent payoutEvent = buildPayoutPaidEvent(payout);
            eventSinkService.saveEvent(payoutEvent);
            log.info("Payout have been paid, payoutId={}", payoutId);
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to pay a payout, payoutId='%d'", payoutId), ex);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void confirm(long payoutId) throws InvalidStateException, StorageException {
        log.info("Trying to confirm a payout, payoutId={}", payoutId);
        try {
            Payout payout = payoutDao.getExclusive(payoutId);

            if (payout.getStatus() != PayoutStatus.PAID) {
                throw new InvalidStateException(
                        String.format("Invalid status for 'confirm' action, payoutId='%d', currentStatus='%s'", payoutId, payout.getStatus())
                );
            }

            payoutDao.changeStatus(payoutId, PayoutStatus.CONFIRMED);
            UserInfo userInfo = WoodyUtils.getUserInfo();
            PayoutEvent payoutEvent = buildPayoutConfirmedEvent(payout, userInfo);
            eventSinkService.saveEvent(payoutEvent);
            shumwayService.commit(payoutId);
            log.info("Payout have been confirmed, payoutId={}", payoutId);
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to confirm a payout, payoutId='%d'", payoutId), ex);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void cancel(long payoutId, String details) throws InvalidStateException, StorageException {
        log.info("Trying to cancel a payout, payoutId={}", payoutId);
        try {
            Payout payout = payoutDao.getExclusive(payoutId);

            UserInfo userInfo = WoodyUtils.getUserInfo();
            PayoutEvent payoutEvent = buildPayoutCancelledEvent(payout, details, userInfo);
            eventSinkService.saveEvent(payoutEvent);

            switch (payout.getStatus()) {
                case UNPAID:
                case PAID:
                    payoutDao.changeStatus(payoutId, PayoutStatus.CANCELLED);
                    shumwayService.rollback(payoutId);
                    break;
                case CONFIRMED:
                    payoutDao.changeStatus(payoutId, PayoutStatus.CANCELLED);
                    shumwayService.revert(payoutId);
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
        if (unpaidPayouts.isEmpty()) return;
        unpaidPayouts.forEach(p -> pay(p.getId()));
        report1CService.generate(unpaidPayouts);
    }

    @Scheduled(fixedDelay = 5000)
    public void sendReports() {
        List<Report> reportsForSend = reportDao.getForSend();
        if (reportsForSend.isEmpty()) return;
        report1CSendService.send(reportsForSend);
    }

    public PayoutEvent buildPayoutPaidEvent(Payout payoutRecord) throws StorageException {
        PayoutEvent payoutEvent = new PayoutEvent();
        payoutEvent.setEventType(PayoutChange._Fields.PAYOUT_STATUS_CHANGED.getFieldName());
        payoutEvent.setPayoutId(Long.toString(payoutRecord.getId()));
        payoutEvent.setPayoutStatus(com.rbkmoney.damsel.payout_processing.PayoutStatus._Fields.PAID.getFieldName());
        payoutEvent.setPayoutPaidDetailsType(PaidDetails._Fields.ACCOUNT_DETAILS.getFieldName());
        return payoutEvent;
    }

    public PayoutEvent buildPayoutCancelledEvent(Payout payoutRecord, String details, UserInfo userInfo) throws StorageException {
        PayoutEvent payoutEvent = new PayoutEvent();
        payoutEvent.setEventType(PayoutChange._Fields.PAYOUT_STATUS_CHANGED.getFieldName());
        payoutEvent.setPayoutId(Long.toString(payoutRecord.getId()));
        payoutEvent.setPayoutStatus(com.rbkmoney.damsel.payout_processing.PayoutStatus._Fields.CANCELLED.getFieldName());
        payoutEvent.setUserId(userInfo.getId());
        payoutEvent.setUserType(userInfo.getType().getSetField().getFieldName());
        payoutEvent.setPayoutStatusCancelDetails(details);
        return payoutEvent;
    }

    public PayoutEvent buildPayoutConfirmedEvent(Payout payoutRecord, UserInfo userInfo) throws StorageException {
        PayoutEvent payoutEvent = new PayoutEvent();
        payoutEvent.setEventType(PayoutChange._Fields.PAYOUT_STATUS_CHANGED.getFieldName());
        payoutEvent.setPayoutId(Long.toString(payoutRecord.getId()));
        payoutEvent.setPayoutStatus(com.rbkmoney.damsel.payout_processing.PayoutStatus._Fields.CONFIRMED.getFieldName());
        payoutEvent.setUserId(userInfo.getId());
        payoutEvent.setUserType(userInfo.getType().getSetField().getFieldName());
        return payoutEvent;
    }

    private PayoutEvent buildPayoutCreatedEvent(Payout payoutRecord, UserInfo userInfo) throws StorageException {
        PayoutEvent payoutEvent = new PayoutEvent();
        payoutEvent.setEventType(PayoutChange._Fields.PAYOUT_CREATED.getFieldName());
        payoutEvent.setPayoutId(Long.toString(payoutRecord.getId()));
        payoutEvent.setPayoutStatus(com.rbkmoney.damsel.payout_processing.PayoutStatus._Fields.UNPAID.getFieldName());
        payoutEvent.setPayoutCreatedAt(payoutRecord.getCreatedAt());
        payoutEvent.setPayoutPartyId(payoutRecord.getPartyId());
        payoutEvent.setPayoutShopId(payoutRecord.getShopId());

        //account
        payoutEvent.setPayoutType(com.rbkmoney.damsel.payout_processing.PayoutType._Fields.BANK_ACCOUNT.getFieldName());
        payoutEvent.setPayoutAccountId(payoutRecord.getBankAccount());
        payoutEvent.setPayoutAccountBankPostId(payoutRecord.getBankPostAccount());
        payoutEvent.setPayoutAccountBankName(payoutRecord.getBankName());
        payoutEvent.setPayoutAccountBankBik(payoutRecord.getBankBik());
        payoutEvent.setPayoutAccountPurpose(payoutRecord.getPurpose());
        payoutEvent.setPayoutAccountInn(payoutRecord.getInn());

        //account cash flow
        FinalCashFlowPosting finalCashFlowPosting = new FinalCashFlowPosting();
        finalCashFlowPosting.setSource(
                new FinalCashFlowAccount(
                        CashFlowAccount.merchant(MerchantCashFlowAccount.settlement),
                        payoutRecord.getShopAcc()
                )
        );
        finalCashFlowPosting.setDestination(
                new FinalCashFlowAccount(
                        CashFlowAccount.merchant(MerchantCashFlowAccount.settlement),
                        payoutRecord.getShopPayoutAcc()
                )
        );
        finalCashFlowPosting.setVolume(
                new Cash(
                        payoutRecord.getAmount(),
                        new CurrencyRef(payoutRecord.getCurrencyCode())
                )
        );
        try {
            payoutEvent.setPayoutCashFlow(
                    new ObjectMapper().writeValueAsString(Arrays.asList(
                            new TBaseProcessor().process(finalCashFlowPosting, new JsonHandler())
                    ))
            );
        } catch (IOException ex) {
            throw new StorageException("Failed to generate cash flow", ex);
        }

        payoutEvent.setPayoutAccountLegalAgreementId(payoutRecord.getAccountLegalAgreementId());
        payoutEvent.setPayoutAccountLegalAgreementSignedAt(payoutRecord.getAccountLegalAgreementSignedAt());

        payoutEvent.setUserId(userInfo.getId());
        payoutEvent.setUserType(userInfo.getType().getSetField().getFieldName());
        return payoutEvent;
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

    private long calculateAvailableAmount(List<Payment> payments, List<Refund> refunds, List<Adjustment> adjustments) {
        long paymentAmount = payments.stream()
                .mapToLong(payment -> payment.getAmount() - payment.getFee() - payment.getGuaranteeDeposit())
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
