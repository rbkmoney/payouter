package com.rbkmoney.payouter.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.msgpack.Value;
import com.rbkmoney.damsel.payout_processing.PaidDetails;
import com.rbkmoney.damsel.payout_processing.PayoutChange;
import com.rbkmoney.damsel.payout_processing.ShopParams;
import com.rbkmoney.damsel.payout_processing.UserInfo;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.geck.serializer.kit.json.JsonHandler;
import com.rbkmoney.geck.serializer.kit.tbase.TBaseProcessor;
import com.rbkmoney.payouter.dao.*;
import com.rbkmoney.payouter.domain.enums.PayoutAccountType;
import com.rbkmoney.payouter.domain.enums.PayoutStatus;
import com.rbkmoney.payouter.domain.enums.PayoutType;
import com.rbkmoney.payouter.domain.tables.pojos.*;
import com.rbkmoney.payouter.exception.DaoException;
import com.rbkmoney.payouter.exception.InvalidStateException;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.exception.StorageException;
import com.rbkmoney.payouter.service.*;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    private final CashFlowDescriptionService cashFlowDescriptionService;

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
                             CashFlowDescriptionService cashFlowDescriptionService,
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
        this.cashFlowDescriptionService = cashFlowDescriptionService;
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
    public List<Long> createPayouts(LocalDateTime fromTime, LocalDateTime toTime, PayoutType payoutType) throws InvalidStateException, NotFoundException, StorageException {
        log.info("Trying to create payouts, fromTime={}, toTime={}, payoutType={}", fromTime, toTime, payoutType);
        try {
            List<ShopParams> shops = payoutDao.getUnpaidShops(fromTime, toTime);

            if (shops.isEmpty()) {
                log.info("No shops found for creating payouts, fromTime={}, toTime={}, payoutType={}", fromTime, toTime, payoutType);
                return Collections.emptyList();
            }

            List<Long> payoutIds = new ArrayList<>();
            for (ShopParams shopParams : shops) {
                try {
                    long payoutId = createPayout(shopParams.getPartyId(), shopParams.getShopId(), fromTime, toTime, payoutType);
                    payoutIds.add(payoutId);
                } catch (InvalidStateException ex) {
                    log.warn("Failed to create payout for shop, shopParams='{}', fromTime='{}', toTime='{}', payoutType='{}'",
                            shopParams, fromTime, toTime, payoutType, ex);
                }
            }
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
    public long createPayout(String partyId, String shopId, LocalDateTime fromTime, LocalDateTime toTime, PayoutType payoutType) throws InvalidStateException, NotFoundException, StorageException {
        log.info("Trying to create payout, partyId={}, shopId={}, fromTime={}, toTime={}, payoutType={}",
                partyId, shopId, fromTime, toTime, payoutType);
        try {
            if (isBlockedForPayouts(partyId)) {
                throw new InvalidStateException(
                        String.format("Party is blocked for payouts, partyId='%s', shopId='%s'", partyId, shopId)
                );
            }

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

            List<CashFlowDescription> cashFlowDescriptions = buildCashFlowDescriptions(payments, refunds, adjustments, payoutId, payout.getCurrencyCode());
            cashFlowDescriptionService.save(cashFlowDescriptions);

            String purpose = buildPurpose(payout);
            payoutDao.changePurpose(payoutId, purpose);

            paymentDao.includeToPayout(payoutId, payments);
            refundDao.includeToPayout(payoutId, refunds);
            adjustmentDao.includeToPayout(payoutId, adjustments);

            shopMetaDao.updateLastPayoutCreatedAt(shopMeta.getPartyId(), shopMeta.getShopId(), payout.getCreatedAt());

            List<FinalCashFlowPosting> cashFlowPostings = partyManagementService.computePayoutCashFlow(
                    partyId,
                    shopId,
                    new Cash(availableAmount, new CurrencyRef(payout.getCurrencyCode())),
                    payout.getCreatedAt().toInstant(ZoneOffset.UTC)
            );
            cashFlowPostings.add(0, buildPrimaryPosting(payout));

            UserInfo userInfo = WoodyUtils.getUserInfo();
            payout.setId(payoutId);
            payout.setPurpose(purpose);
            PayoutEvent payoutEvent = buildPayoutCreatedEvent(payout, cashFlowPostings, userInfo);
            eventSinkService.saveEvent(payoutEvent);
            shumwayService.hold(String.valueOf(payoutId), cashFlowPostings);

            log.info("Payout successfully created, payoutId='{}', partyId={}, shopId={}, fromTime={}, toTime={}, payoutType={}",
                    payoutId, partyId, shopId, fromTime, toTime, payoutType);

            return payoutId;
        } catch (DaoException ex) {
            throw new StorageException(
                    String.format("Failed to create payout, partyId='%s', shopId='%s', fromTime='%s', toTime='%s', payoutType='%s'",
                            partyId, shopId, fromTime, toTime, payoutType), ex);
        }
    }

    private FinalCashFlowPosting buildPrimaryPosting(Payout payout) {
        FinalCashFlowPosting finalCashFlowPosting = new FinalCashFlowPosting();
        finalCashFlowPosting.setSource(
                new FinalCashFlowAccount(
                        CashFlowAccount.merchant(MerchantCashFlowAccount.settlement),
                        payout.getShopAcc()
                )
        );
        finalCashFlowPosting.setDestination(
                new FinalCashFlowAccount(
                        CashFlowAccount.merchant(MerchantCashFlowAccount.settlement),
                        payout.getShopPayoutAcc()
                )
        );
        finalCashFlowPosting.setVolume(
                new Cash(
                        payout.getAmount(),
                        new CurrencyRef(payout.getCurrencyCode())
                )
        );
        return finalCashFlowPosting;
    }

    private String buildPurpose(Payout payout) {
        switch (payout.getAccountType()) {
            case russian_payout_account:
                return String.format(
                        "Перевод согласно договора номер %s от %s. Без НДС",
                        payout.getAccountLegalAgreementId(),
                        payout.getAccountLegalAgreementSignedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                );
            case international_payout_account:
                return String.format("Agr %s %s, %d for accepted payments.",
                        payout.getAccountLegalAgreementId(),
                        payout.getAccountLegalAgreementSignedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        payout.getId()
                );
            default:
                throw new IllegalArgumentException(String.format("Unknown account type, accountType='%s'", payout.getAccountType()));
        }
    }

    private boolean isBlockedForPayouts(String partyId) {
        Value metaDataValue = partyManagementService.getMetaData(partyId, "payout_blocking");
        return metaDataValue != null && metaDataValue.isSetB() && metaDataValue.getB();
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
            shumwayService.commit(String.valueOf(payoutId));
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
                    shumwayService.rollback(String.valueOf(payoutId));
                    break;
                case CONFIRMED:
                    payoutDao.changeStatus(payoutId, PayoutStatus.CANCELLED);
                    shumwayService.revert(String.valueOf(payoutId));
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
    public List<Payout> search(Optional<PayoutStatus> payoutStatus, Optional<LocalDateTime> fromTime, Optional<LocalDateTime> toTime, Optional<List<Long>> payoutIds, Optional<Long> fromId, Optional<Integer> size) {
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

    private PayoutEvent buildPayoutCreatedEvent(Payout payoutRecord, List<FinalCashFlowPosting> cashFlowPostings, UserInfo userInfo) throws StorageException {
        PayoutEvent payoutEvent = new PayoutEvent();
        payoutEvent.setEventType(PayoutChange._Fields.PAYOUT_CREATED.getFieldName());
        payoutEvent.setPayoutId(Long.toString(payoutRecord.getId()));
        payoutEvent.setPayoutStatus(com.rbkmoney.damsel.payout_processing.PayoutStatus._Fields.UNPAID.getFieldName());
        payoutEvent.setPayoutCreatedAt(payoutRecord.getCreatedAt());
        payoutEvent.setPayoutPartyId(payoutRecord.getPartyId());
        payoutEvent.setPayoutShopId(payoutRecord.getShopId());
        payoutEvent.setPayoutType(payoutRecord.getType().getLiteral());

        payoutEvent.setPayoutAccountType(payoutRecord.getAccountType().getLiteral());
        payoutEvent.setPayoutAccountId(payoutRecord.getBankAccount());
        payoutEvent.setPayoutAccountLegalName(payoutRecord.getAccountLegalName());
        payoutEvent.setPayoutAccountTradingName(payoutRecord.getAccountTradingName());
        payoutEvent.setPayoutAccountRegisteredAddress(payoutRecord.getAccountRegisteredAddress());
        payoutEvent.setPayoutAccountActualAddress(payoutRecord.getAccountActualAddress());
        payoutEvent.setPayoutAccountRegisteredNumber(payoutRecord.getAccountRegisteredNumber());
        payoutEvent.setPayoutAccountBankPostId(payoutRecord.getBankPostAccount());
        payoutEvent.setPayoutAccountBankName(payoutRecord.getBankName());
        payoutEvent.setPayoutAccountBankAddress(payoutRecord.getBankAddress());
        payoutEvent.setPayoutAccountBankBic(payoutRecord.getBankBic());
        payoutEvent.setPayoutAccountBankIban(payoutRecord.getBankIban());
        payoutEvent.setPayoutAccountBankLocalCode(payoutRecord.getBankLocalCode());
        payoutEvent.setPayoutAccountPurpose(payoutRecord.getPurpose());
        payoutEvent.setPayoutAccountInn(payoutRecord.getInn());

        try {
            payoutEvent.setPayoutCashFlow(
                    new ObjectMapper().writeValueAsString(cashFlowPostings.stream().map(
                            cashFlowPosting -> {
                                try {
                                    return new TBaseProcessor().process(cashFlowPosting, new JsonHandler());
                                } catch (IOException ex) {
                                    throw new RuntimeJsonMappingException(ex.getMessage());
                                }
                            }).collect(Collectors.toList())
                    )
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
        Instant timestamp = Instant.now();

        Payout payout = new Payout();
        payout.setPartyId(partyId);
        payout.setShopId(shopId);
        payout.setFromTime(fromTime);
        payout.setToTime(toTime);
        payout.setType(payoutType);
        payout.setCreatedAt(LocalDateTime.ofInstant(timestamp, ZoneOffset.UTC));
        payout.setStatus(PayoutStatus.UNPAID);

        Party party = partyManagementService.getParty(partyId, timestamp);

        Shop shop = party.getShops().get(shopId);
        if (shop == null) {
            throw new NotFoundException(String.format("Shop not found, partyId='%s', contractId='%s', timestamp='%s'", partyId, shopId, timestamp));
        }
        ShopAccount shopAccount = shop.getAccount();
        payout.setShopAcc(shopAccount.getSettlement());
        payout.setShopPayoutAcc(shopAccount.getPayout());
        payout.setCurrencyCode(shopAccount.getCurrency().getSymbolicCode());

        Contract contract = party.getContracts().get(shop.getContractId());
        if (contract == null) {
            throw new NotFoundException(String.format("Contract not found, partyId='%s', contractId='%s', timestamp='%s'", partyId, shop.getId(), timestamp));
        }

        Optional<PayoutTool> payoutToolOptional = contract.getPayoutTools().stream()
                .filter(payoutTool -> payoutTool.getId().equals(shop.getPayoutToolId()))
                .findFirst();

        if (!payoutToolOptional.isPresent()) {
            throw new NotFoundException(
                    String.format("Payout tool with bank account not found, partyId='%s', shopId='%s', payoutToolId='%s'",
                            partyId, shopId, shop.getPayoutToolId()));
        }

        PayoutTool payoutTool = payoutToolOptional.get();
        if (!payout.getCurrencyCode().equals(payoutTool.getCurrency().getSymbolicCode())) {
            throw new InvalidStateException("Shop account and payout tool currency must be equals");
        }

        if (payoutTool.getPayoutToolInfo().isSetRussianBankAccount()) {
            payout.setAccountType(PayoutAccountType.russian_payout_account);
            RussianBankAccount bankAccount = payoutTool.getPayoutToolInfo().getRussianBankAccount();

            payout.setBankAccount(bankAccount.getAccount());
            payout.setBankLocalCode(bankAccount.getBankBik());
            payout.setBankName(bankAccount.getBankName());
            payout.setBankPostAccount(bankAccount.getBankPostAccount());
            if (contract.getContractor().getLegalEntity().isSetRussianLegalEntity()) {
                RussianLegalEntity legalEntity = contract.getContractor().getLegalEntity().getRussianLegalEntity();
                payout.setInn(legalEntity.getInn());
                payout.setDescription(legalEntity.getRegisteredName());
                payout.setAccountRegisteredNumber(legalEntity.getRegisteredNumber());
            }
        }

        if (payoutTool.getPayoutToolInfo().isSetInternationalBankAccount()) {
            payout.setAccountType(PayoutAccountType.international_payout_account);
            InternationalBankAccount bankAccount = payoutTool.getPayoutToolInfo().getInternationalBankAccount();

            payout.setBankAccount(bankAccount.getAccountHolder());
            payout.setBankName(bankAccount.getBankName());
            payout.setBankAddress(bankAccount.getBankAddress());
            payout.setBankBic(bankAccount.getBic());
            payout.setBankIban(bankAccount.getIban());
            payout.setBankLocalCode(bankAccount.getLocalBankCode());
            if (contract.getContractor().getLegalEntity().isSetInternationalLegalEntity()) {
                InternationalLegalEntity legalEntity = contract.getContractor().getLegalEntity().getInternationalLegalEntity();
                payout.setAccountLegalName(legalEntity.getLegalName());
                payout.setAccountTradingName(legalEntity.getTradingName());
                payout.setAccountRegisteredAddress(legalEntity.getRegisteredAddress());
                payout.setAccountActualAddress(legalEntity.getActualAddress());
                payout.setAccountRegisteredNumber(legalEntity.getRegisteredNumber());
            }
        }

        if (!contract.isSetLegalAgreement()) {
            throw new NotFoundException(
                    String.format("Legal agreement not found, partyId='%s', shopId='%s', contractId='%s'",
                            partyId, shopId, contract.getId()));
        }

        payout.setAccountLegalAgreementId(contract.getLegalAgreement().getLegalAgreementId());
        payout.setAccountLegalAgreementSignedAt(
                TypeUtil.stringToLocalDateTime(contract.getLegalAgreement().getSignedAt())
        );

        return payout;
    }

    private List<CashFlowDescription> buildCashFlowDescriptions(List<Payment> payments, List<Refund> refunds, List<Adjustment> adjustments, long payoutId, String currencyCode) {
        List<CashFlowDescription> result = new ArrayList<>();

        long paymentAmount = payments.stream().mapToLong(Payment::getAmount).sum();
        long paymentFee = payments.stream().mapToLong(Payment::getFee).sum();
        LocalDateTime paymentFromTime = payments.stream().map(Payment::getCreatedAt).min(LocalDateTime::compareTo).get();
        LocalDateTime paymentToTime = payments.stream().map(Payment::getCreatedAt).max(LocalDateTime::compareTo).get();
        CashFlowDescription paymentCashFlow = new CashFlowDescription();
        paymentCashFlow.setAmount(paymentAmount);
        paymentCashFlow.setFee(paymentFee);
        paymentCashFlow.setCurrencyCode(currencyCode);
        paymentCashFlow.setCashFlowType(com.rbkmoney.payouter.domain.enums.CashFlowType.payment);
        paymentCashFlow.setCount(payments.size());
        paymentCashFlow.setPayoutId(String.valueOf(payoutId));
        paymentCashFlow.setFromTime(paymentFromTime);
        paymentCashFlow.setToTime(paymentToTime);
        result.add(paymentCashFlow);

        if (!refunds.isEmpty()) {
            long refundAmount = refunds.stream().mapToLong(Refund::getAmount).sum();
            long refundFee = refunds.stream().mapToLong(Refund::getFee).sum();
            LocalDateTime refundFromTime = refunds.stream().map(Refund::getCreatedAt).min(LocalDateTime::compareTo).get();
            LocalDateTime refundToTime = refunds.stream().map(Refund::getCreatedAt).max(LocalDateTime::compareTo).get();
            CashFlowDescription refundCashFlow = new CashFlowDescription();
            refundCashFlow.setAmount(refundAmount);
            refundCashFlow.setFee(refundFee);
            refundCashFlow.setCurrencyCode(currencyCode);
            refundCashFlow.setCashFlowType(com.rbkmoney.payouter.domain.enums.CashFlowType.refund);
            refundCashFlow.setCount(refunds.size());
            refundCashFlow.setPayoutId(String.valueOf(payoutId));
            refundCashFlow.setFromTime(refundFromTime);
            refundCashFlow.setToTime(refundToTime);
            result.add(refundCashFlow);
        }

        if (!adjustments.isEmpty()) {
            long adjustmentAmount = adjustments.stream().mapToLong(Adjustment::getPaymentFee).sum();
            long adjustmentFee = adjustments.stream().mapToLong(Adjustment::getNewFee).sum();
            LocalDateTime adjustmentFromTime = adjustments.stream().map(Adjustment::getCreatedAt).min(LocalDateTime::compareTo).get();
            LocalDateTime adjustmentToTime = adjustments.stream().map(Adjustment::getCreatedAt).max(LocalDateTime::compareTo).get();
            CashFlowDescription adjustmentCashFlow = new CashFlowDescription();
            adjustmentCashFlow.setAmount(adjustmentAmount);
            adjustmentCashFlow.setFee(adjustmentFee);
            adjustmentCashFlow.setCurrencyCode(currencyCode);
            adjustmentCashFlow.setCashFlowType(com.rbkmoney.payouter.domain.enums.CashFlowType.adjustment);
            adjustmentCashFlow.setCount(adjustments.size());
            adjustmentCashFlow.setPayoutId(String.valueOf(payoutId));
            adjustmentCashFlow.setFromTime(adjustmentFromTime);
            adjustmentCashFlow.setToTime(adjustmentToTime);
            result.add(adjustmentCashFlow);
        }

        return result;
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

        long availableAmount = paymentAmount + adjustmentAmount - refundAmount;

        log.info("Available amount have been calculated, availableAmount={}, payments='{}', refunds='{}', adjustments='{}'",
                availableAmount, payments, refunds, adjustments);
        return availableAmount;
    }

}
