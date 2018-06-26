package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.msgpack.Value;
import com.rbkmoney.damsel.payout_processing.UserInfo;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.payouter.dao.*;
import com.rbkmoney.payouter.domain.enums.PayoutAccountType;
import com.rbkmoney.payouter.domain.enums.PayoutStatus;
import com.rbkmoney.payouter.domain.enums.PayoutType;
import com.rbkmoney.payouter.domain.tables.pojos.*;
import com.rbkmoney.payouter.exception.*;
import com.rbkmoney.payouter.service.*;
import com.rbkmoney.payouter.util.CashFlowType;
import com.rbkmoney.payouter.util.DamselUtil;
import com.rbkmoney.payouter.util.WoodyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PayoutServiceImpl implements PayoutService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ShopMetaDao shopMetaDao;

    private final PaymentDao paymentDao;

    private final RefundDao refundDao;

    private final AdjustmentDao adjustmentDao;

    private final PayoutDao payoutDao;

    private final PayoutSummaryService payoutSummaryService;

    private final ShumwayService shumwayService;

    private final PartyManagementService partyManagementService;

    private final EventSinkService eventSinkService;

    @Autowired
    public PayoutServiceImpl(ShopMetaDao shopMetaDao,
                             PaymentDao paymentDao,
                             RefundDao refundDao,
                             AdjustmentDao adjustmentDao,
                             PayoutDao payoutDao,
                             PayoutSummaryService payoutSummaryService,
                             ShumwayService shumwayService,
                             PartyManagementService partyManagementService,
                             EventSinkService eventSinkService) {
        this.shopMetaDao = shopMetaDao;
        this.paymentDao = paymentDao;
        this.refundDao = refundDao;
        this.adjustmentDao = adjustmentDao;
        this.payoutDao = payoutDao;
        this.payoutSummaryService = payoutSummaryService;
        this.shumwayService = shumwayService;
        this.partyManagementService = partyManagementService;
        this.eventSinkService = eventSinkService;
        //over
    }

    @Override
    public List<Long> createPayouts(String partyId, String shopId, LocalDateTime fromTime, LocalDateTime toTime, PayoutType payoutType) throws InvalidStateException, NotFoundException, StorageException {
        return createPayouts(partyId, shopId, fromTime, toTime, payoutType, LocalDateTime.now(ZoneOffset.UTC));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public List<Long> createPayouts(String partyId, String shopId, LocalDateTime fromTime, LocalDateTime toTime, PayoutType payoutType, LocalDateTime createdAt) throws InvalidStateException, NotFoundException, StorageException {
        log.info("Trying to create payouts, partyId='{}', shopId='{}', fromTime='{}', toTime='{}', payoutType='{}'", partyId, shopId, fromTime, toTime, payoutType);
        List<Long> payoutIds = new ArrayList<>();
        try {
            Set<String> contractIds = getContractsForPayouts(partyId, shopId, toTime);
            if (contractIds.isEmpty()) {
                throw new NotFoundException(String.format("Contracts not found, payouts can't be created, partyId='%s', shopId='%s', fromTime='%s', toTime='%s', payoutType='%s'", partyId, shopId, fromTime, toTime, payoutType));
            }

            for (String contractId : contractIds) {
                try {
                    long payoutId = createPayout(partyId, shopId, contractId, fromTime, toTime, payoutType, createdAt);
                    payoutIds.add(payoutId);
                } catch (InsufficientFundsException ex) {
                    log.info("Payout can't be created, reason='{}', partyId='{}', shopId='{}', contractId='{}', fromTime='{}', toTime='{}', payoutType='{}'", ex.getMessage(), partyId, shopId, contractId, fromTime, toTime, payoutType);
                }
            }
            log.info("Payouts have been created, payoutIds='{}', partyId='{}', shopId='{}', fromTime='{}', toTime='{}', payoutType='{}'", payoutIds, partyId, shopId, fromTime, toTime, payoutType);
            return payoutIds;
        } catch (RuntimeException ex) {
            payoutIds.forEach(payoutId -> shumwayService.rollback(String.valueOf(payoutId)));
            throw ex;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Set<String> getContractsForPayouts(String partyId, String shopId, LocalDateTime toTime) {
        log.info("Trying to get contracts for payouts, partyId='{}', shopId='{}', toTime='{}'", partyId, shopId, toTime);
        try {
            Set<String> contractIds = new HashSet<>();
            contractIds.addAll(paymentDao.getContracts(partyId, shopId, toTime));
            contractIds.addAll(refundDao.getContracts(partyId, shopId, toTime));
            contractIds.addAll(adjustmentDao.getContracts(partyId, shopId, toTime));
            log.info("Contracts for payouts has been found, contractIds='{}', partyId='{}', shopId='{}', toTime='{}'", contractIds, partyId, shopId, toTime);
            return contractIds;
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to get contracts for payouts, partyId='%s', shopId='%s', toTime='%s'", partyId, shopId, toTime));
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public long createPayout(String partyId, String shopId, String contractId, LocalDateTime fromTime, LocalDateTime toTime, PayoutType payoutType, LocalDateTime createdAt) throws InvalidStateException, NotFoundException, StorageException {
        log.info("Trying to create payout, partyId='{}', shopId='{}', contractId='{}', fromTime='{}', toTime='{}', payoutType='{}', createdAt='{}'",
                partyId, shopId, contractId, fromTime, toTime, payoutType, createdAt);
        try {
            if (isBlockedForPayouts(partyId)) {
                throw new InvalidStateException(
                        String.format("Party is blocked for payouts, partyId='%s', shopId='%s'", partyId, shopId)
                );
            }

            ShopMeta shopMeta = shopMetaDao.getExclusive(partyId, shopId);

            List<Payment> payments = paymentDao.getUnpaid(partyId, shopId, contractId, toTime);
            List<Refund> refunds = refundDao.getUnpaid(partyId, shopId, contractId, toTime);
            List<Adjustment> adjustments = adjustmentDao.getUnpaid(partyId, shopId, contractId, toTime);

            long availableAmount = calculateAvailableAmount(payments, refunds, adjustments);

            if (availableAmount <= 0) {
                throw new InsufficientFundsException("Available amount must be greater than 0");
            }

            Payout payout = buildPayout(partyId, shopId, contractId, fromTime, toTime, payoutType, createdAt);
            List<FinalCashFlowPosting> cashFlowPostings = partyManagementService.computePayoutCashFlow(
                    partyId,
                    shopId,
                    new Cash(availableAmount, new CurrencyRef(payout.getCurrencyCode())),
                    payout.getCreatedAt().toInstant(ZoneOffset.UTC)
            );

            Map<CashFlowType, Long> cashFlow = DamselUtil.parseCashFlow(cashFlowPostings);
            payout.setAmount(cashFlow.getOrDefault(CashFlowType.PAYOUT_AMOUNT, 0L) - cashFlow.getOrDefault(CashFlowType.PAYOUT_FIXED_FEE, 0L));
            payout.setFee(cashFlow.getOrDefault(CashFlowType.FEE, 0L) + cashFlow.getOrDefault(CashFlowType.PAYOUT_FIXED_FEE, 0L));
            if (payout.getAmount() <= 0) {
                throw new InsufficientFundsException(
                        String.format("Negative amount in payout cash flow, amount='%d', fee='%d'", payout.getAmount(), payout.getFee())
                );
            }

            long payoutId = payoutDao.save(payout);
            payoutSummaryService.save(payoutId, payout.getCurrencyCode(), payments, refunds, adjustments);

            String purpose = buildPurpose(payoutId, payout);
            payoutDao.changePurpose(payoutId, purpose);

            paymentDao.includeToPayout(payoutId, payments);
            refundDao.includeToPayout(payoutId, refunds);
            adjustmentDao.includeToPayout(payoutId, adjustments);

            shopMetaDao.updateLastPayoutCreatedAt(shopMeta.getPartyId(), shopMeta.getShopId(), payout.getCreatedAt());

            eventSinkService.savePayoutCreatedEvent(
                    String.valueOf(payoutId),
                    purpose,
                    payout,
                    cashFlowPostings,
                    WoodyUtils.getUserInfo()
            );
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

    private String buildPurpose(long payoutId, Payout payout) {
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
                        payoutId
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
        log.info("Trying to pay a payout, payoutId='{}'", payoutId);
        try {
            Payout payout = payoutDao.getExclusive(payoutId);

            if (payout.getStatus() != PayoutStatus.UNPAID) {
                throw new InvalidStateException(
                        String.format("Invalid status for 'pay' action, payoutId='%d', currentStatus='%s'", payoutId, payout.getStatus())
                );
            }

            payoutDao.changeStatus(payoutId, PayoutStatus.PAID);
            eventSinkService.savePayoutPaidEvent(String.valueOf(payoutId));
            log.info("Payout have been paid, payoutId='{}'", payoutId);
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
            eventSinkService.savePayoutConfirmedEvent(String.valueOf(payoutId), WoodyUtils.getUserInfo());
            shumwayService.commit(String.valueOf(payoutId));
            log.info("Payout have been confirmed, payoutId={}", payoutId);
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to confirm a payout, payoutId='%d'", payoutId), ex);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void cancel(long payoutId, String details) throws InvalidStateException, StorageException {
        log.info("Trying to cancel a payout, payoutId='{}'", payoutId);
        try {
            Payout payout = payoutDao.getExclusive(payoutId);
            payoutDao.changeStatus(payoutId, PayoutStatus.CANCELLED);
            excludeFromPayout(payoutId);

            UserInfo userInfo = WoodyUtils.getUserInfo();
            eventSinkService.savePayoutCancelledEvent(String.valueOf(payoutId), details, userInfo);

            switch (payout.getStatus()) {
                case UNPAID:
                case PAID:
                    shumwayService.rollback(String.valueOf(payoutId));
                    break;
                case CONFIRMED:
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
    public List<Payout> getUnpaidPayoutsByAccountType(PayoutAccountType accountType) throws StorageException {
        try {
            log.info("Trying to get unpaid payouts by account type, accountType='{}'", accountType);
            List<Payout> payouts = payoutDao.getUnpaidPayoutsByAccountType(accountType);
            log.info("Unpaid payouts has been found, accountType='{}', payouts='{}'", accountType, payouts);
            return payouts;
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to get unpaid payouts by account type, accountType='%s'", accountType), ex);
        }
    }

    @Override
    public List<Payout> search(Optional<PayoutStatus> payoutStatus, Optional<LocalDateTime> fromTime, Optional<LocalDateTime> toTime, Optional<List<Long>> payoutIds, Optional<Long> fromId, Optional<Integer> size) {
        return payoutDao.search(payoutStatus, fromTime, toTime, payoutIds, fromId, size);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void excludeFromPayout(long payoutId) throws StorageException {
        log.info("Trying to exclude operations from payout, payoutId='{}'", payoutId);
        try {
            int paymentCount = paymentDao.excludeFromPayout(payoutId);
            int refundCount = refundDao.excludeFromPayout(payoutId);
            int adjustmentCount = adjustmentDao.excludeFromPayout(payoutId);
            log.info("Operations have been excluded from payout, payoutId='{}' (paymentCount='{}', refundCount='{}', adjustmentCount='{}')", payoutId, paymentCount, refundCount, adjustmentCount);
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to exclude operations from payout, payoutId='%d'", payoutId), ex);
        }
    }

    private Payout buildPayout(String partyId, String shopId, String contractId, LocalDateTime fromTime, LocalDateTime toTime, PayoutType payoutType, LocalDateTime createdAt) {
        Payout payout = new Payout();
        payout.setPartyId(partyId);
        payout.setShopId(shopId);
        payout.setContractId(contractId);
        payout.setFromTime(fromTime);
        payout.setToTime(toTime);
        payout.setType(payoutType);
        payout.setCreatedAt(createdAt);
        payout.setStatus(PayoutStatus.UNPAID);

        Party party = partyManagementService.getParty(partyId, createdAt.toInstant(ZoneOffset.UTC));

        Shop shop = party.getShops().get(shopId);
        if (shop == null) {
            throw new NotFoundException(String.format("Shop not found, partyId='%s', contractId='%s', timestamp='%s'", partyId, shopId, createdAt));
        }
        if (shop.getLocation().isSetUrl()) {
            payout.setShopUrl(shop.getLocation().getUrl());
        }
        ShopAccount shopAccount = shop.getAccount();
        payout.setShopAcc(shopAccount.getSettlement());
        payout.setShopPayoutAcc(shopAccount.getPayout());
        payout.setCurrencyCode(shopAccount.getCurrency().getSymbolicCode());

        Contract contract = party.getContracts().get(shop.getContractId());
        if (contract == null) {
            throw new NotFoundException(String.format("Contract not found, partyId='%s', contractId='%s', timestamp='%s'", partyId, shop.getId(), createdAt));
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
