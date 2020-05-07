package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.msgpack.Value;
import com.rbkmoney.damsel.shumpune.Balance;
import com.rbkmoney.damsel.shumpune.Clock;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.payouter.dao.*;
import com.rbkmoney.payouter.domain.enums.PayoutAccountType;
import com.rbkmoney.payouter.domain.enums.PayoutStatus;
import com.rbkmoney.payouter.domain.enums.PayoutType;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutSummary;
import com.rbkmoney.payouter.domain.tables.pojos.ShopMeta;
import com.rbkmoney.payouter.exception.*;
import com.rbkmoney.payouter.service.*;
import com.rbkmoney.payouter.util.CashFlowType;
import com.rbkmoney.payouter.util.DamselUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutServiceImpl implements PayoutService {

    private final EventSinkService eventSinkService;

    private final ShopMetaDao shopMetaDao;

    private final PaymentDao paymentDao;

    private final RefundDao refundDao;

    private final AdjustmentDao adjustmentDao;

    private final PayoutDao payoutDao;

    private final ChargebackDao chargebackDao;

    private final ShumwayService shumwayService;

    private final PayoutSummaryService payoutSummaryService;

    private final PartyManagementService partyManagementService;

    private final FistfulService fistfulService;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public String createPayoutByRange(
            String partyId,
            String shopId,
            LocalDateTime fromTime,
            LocalDateTime toTime
    ) throws InsufficientFundsException, InvalidStateException, NotFoundException, StorageException {
        shopMetaDao.getExclusive(partyId, shopId);
        long partyRevision = partyManagementService.getPartyRevision(partyId);
        Shop shop = partyManagementService.getShop(partyId, shopId, partyRevision);
        if (shop.getBlocking().isSetBlocked() || isBlockedForPayouts(partyId)) {
            throw new InvalidStateException(
                    String.format("Party or shop blocked for payouts, partyId='%s', shopId='%s'", partyId, shopId)
            );
        }

        String payoutId = generatePayoutId();
        String payoutToolId = shop.getPayoutToolId();
        String currencyCode = shop.getAccount().getCurrency().getSymbolicCode();

        includeUnpaid(payoutId, partyId, shopId, toTime);
        saveRangeData(payoutId, partyId, shopId, fromTime, toTime);
        long availableAmount = calculateAvailableAmount(payoutId);
        buildAndSavePayoutSummaryData(payoutId);

        return create(
                payoutId,
                partyId,
                shopId,
                payoutToolId,
                availableAmount,
                currencyCode,
                partyRevision
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public String create(
            String payoutId,
            String partyId,
            String shopId,
            String payoutToolId,
            long amount,
            String currencyCode
    ) throws InsufficientFundsException, InvalidStateException, NotFoundException, StorageException {
        return create(
                payoutId,
                partyId,
                shopId,
                payoutToolId,
                amount,
                currencyCode,
                partyManagementService.getPartyRevision(partyId)
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public String create(
            String payoutId,
            String partyId,
            String shopId,
            String payoutToolId,
            long amount,
            String currencyCode,
            long partyRevision
    ) throws InsufficientFundsException, InvalidStateException, NotFoundException, StorageException {
        try {
            ShopMeta shopMeta = shopMetaDao.getExclusive(partyId, shopId);
            if (amount <= 0) {
                throw new InsufficientFundsException("Available amount must be greater than 0");
            }

            Payout payout = buildAndValidatePayout(payoutId, partyId, shopId, payoutToolId, currencyCode, partyRevision);
            List<FinalCashFlowPosting> cashFlowPostings = partyManagementService.computePayoutCashFlow(
                    partyId,
                    shopId,
                    payoutToolId,
                    new Cash(amount, new CurrencyRef(currencyCode)),
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
            payoutDao.save(payout);
            eventSinkService.savePayoutCreatedEvent(payout, cashFlowPostings);
            shopMetaDao.updateLastPayoutCreatedAt(shopMeta.getPartyId(), shopMeta.getShopId(), payout.getCreatedAt());

            Clock clock = shumwayService.hold(payoutId, cashFlowPostings);
            Balance balance = shumwayService.getBalance(payout.getShopAcc(), clock, payoutId);
            if (balance == null || balance.getMinAvailableAmount() < 0) {
                shumwayService.rollback(payoutId);
                throw new InsufficientFundsException(String.format("Invalid available amount in shop account, balance='%s'", balance));
            }

            return payoutId;
        } catch (DaoException ex) {
            throw new StorageException(ex);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void pay(String payoutId) throws InvalidStateException, StorageException {
        log.info("Trying to pay a payout, payoutId='{}'", payoutId);
        try {
            Payout payout = payoutDao.getExclusive(payoutId);

            if (payout.getStatus() == PayoutStatus.PAID) {
                log.info("Payout already paid, payoutId='{}'", payoutId);
                return;
            } else if (payout.getStatus() != PayoutStatus.UNPAID) {
                throw new InvalidStateException(
                        String.format("Invalid status for 'pay' action, payoutId='%s', currentStatus='%s'", payoutId, payout.getStatus())
                );
            }

            payoutDao.changeStatus(payoutId, PayoutStatus.PAID);
            eventSinkService.savePayoutPaidEvent(payoutId);
            log.info("Payout have been paid, payoutId='{}'", payoutId);
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to confirm a payout, payoutId='%s'", payoutId), ex);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void confirm(String payoutId) throws InvalidStateException, StorageException {
        log.info("Trying to confirm a payout, payoutId='{}'", payoutId);
        try {
            Payout payout = payoutDao.getExclusive(payoutId);

            if (payout.getStatus() == PayoutStatus.CONFIRMED) {
                log.info("Payout already confirmed, payoutId='{}'", payoutId);
                return;
            } else if (payout.getStatus() != PayoutStatus.PAID) {
                throw new InvalidStateException(
                        String.format("Invalid status for 'confirm' action, payoutId='%s', currentStatus='%s'", payoutId, payout.getStatus())
                );
            }

            payoutDao.changeStatus(payoutId, PayoutStatus.CONFIRMED);
            eventSinkService.savePayoutConfirmedEvent(payoutId);
            shumwayService.commit(payoutId);

            if (payout.getType() == PayoutType.wallet) {
                fistfulService.createDeposit(payoutId, payout.getWalletId(), payout.getAmount(), payout.getCurrencyCode());
            }
            log.info("Payout have been confirmed, payoutId='{}'", payoutId);
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to confirm a payout, payoutId='%s'", payoutId), ex);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void cancel(String payoutId, String details) throws InvalidStateException, StorageException {
        log.info("Trying to cancel a payout, payoutId='{}'", payoutId);
        try {
            Payout payout = payoutDao.getExclusive(payoutId);
            if (payout.getStatus() == PayoutStatus.CANCELLED) {
                log.info("Payout already cancelled, payoutId='{}'", payoutId);
                return;
            }

            payoutDao.changeStatus(payoutId, PayoutStatus.CANCELLED);
            eventSinkService.savePayoutCancelledEvent(payoutId, details);
            if (payout.getType() != PayoutType.wallet) {
                excludeFromPayout(payoutId);
            }

            switch (payout.getStatus()) {
                case UNPAID:
                case PAID:
                    shumwayService.rollback(payoutId);
                    break;
                case CONFIRMED:
                    if (payout.getType() == PayoutType.wallet) {
                        throw new InvalidStateException(String.format("Unable to cancel confirmed payout to wallet, payoutId='%s'", payoutId));
                    }
                    shumwayService.revert(payoutId);
                    break;
                default:
                    throw new InvalidStateException(String.format("Invalid status for 'cancel' action, payoutId='%s', currentStatus='%s'", payoutId, payout.getStatus()));
            }
            log.info("Payout have been cancelled, payoutId='{}'", payoutId);
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to cancel a payout, payoutId='%s'", payoutId), ex);
        }
    }

    @Override
    public Payout get(String payoutId) throws StorageException {
        try {
            return payoutDao.get(payoutId);
        } catch (DaoException ex) {
            throw new StorageException(ex);
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
    public void includeUnpaid(String payoutId, String partyId, String shopId, LocalDateTime toTime) throws StorageException {
        log.info("Trying to include operations in payout, payoutId='{}', partyId='{}', shopId='{}', toTime='{}'", payoutId, partyId, shopId, toTime);
        try {
            int paymentCount = paymentDao.includeUnpaid(payoutId, partyId, shopId, toTime);
            int refundCount = refundDao.includeUnpaid(payoutId, partyId, shopId);
            int adjustmentCount = adjustmentDao.includeUnpaid(payoutId, partyId, shopId, toTime);
            int payoutCount = payoutDao.includeUnpaid(payoutId, partyId, shopId);
            int chargebackCount = chargebackDao.includeUnpaid(payoutId, partyId, shopId);
            log.info("Operations have been included in payout, payoutId='{}' (paymentCount='{}', refundCount='{}', adjustmentCount='{}', payoutCount='{}', chargebackCount='{}')",
                    payoutId, paymentCount, refundCount, adjustmentCount, payoutCount, chargebackCount);
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to include operations in payout, payoutId='%s'", payoutId), ex);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void excludeFromPayout(String payoutId) throws StorageException {
        log.info("Trying to exclude operations from payout, payoutId='{}'", payoutId);
        try {
            int paymentCount = paymentDao.excludeFromPayout(payoutId);
            int refundCount = refundDao.excludeFromPayout(payoutId);
            int adjustmentCount = adjustmentDao.excludeFromPayout(payoutId);
            int payoutCount = payoutDao.excludeFromPayout(payoutId);
            int chargebackCount = chargebackDao.excludeFromPayout(payoutId);
            log.info("Operations have been excluded from payout, payoutId='{}' (paymentCount='{}', refundCount='{}', adjustmentCount='{}', payoutCount='{}', chargebackCount='{}')",
                    payoutId, paymentCount, refundCount, adjustmentCount, payoutCount, chargebackCount);
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to exclude operations from payout, payoutId='%s'", payoutId), ex);
        }
    }

    @Override
    public List<Payout> getByIds(Set<String> payoutIds) throws StorageException {
        log.info("Trying to get payouts by ids, ids='{}'", payoutIds);
        try {
            List<Payout> payouts = payoutDao.getByIds(payoutIds);
            log.info("Payouts has been found, payouts='{}'", payouts);
            return payouts;
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to get payouts by ids, ids='%s'", payoutIds));
        }
    }

    @Override
    public List<Payout> search(
            PayoutStatus payoutStatus,
            LocalDateTime fromTime,
            LocalDateTime toTime,
            List<String> payoutIds,
            Long minAmount,
            Long maxAmount,
            CurrencyRef currency,
            PayoutType payoutType,
            Long fromId,
            int size
    ) throws StorageException {
        return payoutDao.search(payoutStatus, fromTime, toTime, payoutIds, minAmount, maxAmount, currency, payoutType, fromId, size);
    }

    private Payout buildAndValidatePayout(String payoutId, String partyId, String shopId, String payoutToolId, String currencyCode, long partyRevision) throws InvalidStateException, NotFoundException {
        Party party = partyManagementService.getParty(partyId, partyRevision);

        Payout payout = new Payout();
        payout.setPayoutId(payoutId);
        payout.setPartyId(partyId);
        payout.setShopId(shopId);
        payout.setPartyRevision(partyRevision);
        payout.setCurrencyCode(currencyCode);
        payout.setPayoutToolId(payoutToolId);

        payout.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        payout.setStatus(PayoutStatus.UNPAID);

        Shop shop = party.getShops().get(shopId);
        if (shop == null) {
            throw new NotFoundException(String.format("Shop not found, partyId='%s', shopId='%s'", partyId, shopId));
        }
        if (shop.getLocation().isSetUrl()) {
            payout.setShopUrl(shop.getLocation().getUrl());
        }
        ShopAccount shopAccount = shop.getAccount();
        payout.setShopAcc(shopAccount.getSettlement());
        payout.setShopPayoutAcc(shopAccount.getPayout());
        if (!currencyCode.equals(shop.getAccount().getCurrency().getSymbolicCode())) {
            throw new InvalidStateException("Shop account and payout currency must be equals");
        }

        Contract contract = party.getContracts().values().stream()
                .filter(
                        contractValue -> contractValue.getPayoutTools().stream()
                                .anyMatch(payoutToolValue -> payoutToolValue.getId().equals(payoutToolId))
                )
                .findFirst()
                .orElseThrow(() -> new NotFoundException(String.format("Contract for payout tool not found, partyId='%s', payoutToolId='%s'", partyId, payoutToolId)));

        payout.setContractId(contract.getId());
        if (!contract.isSetLegalAgreement()) {
            throw new NotFoundException(
                    String.format("Legal agreement not found, partyId='%s', shopId='%s', contractId='%s'",
                            partyId, shopId, contract.getId()));
        }

        payout.setAccountLegalAgreementId(contract.getLegalAgreement().getLegalAgreementId());
        payout.setAccountLegalAgreementSignedAt(
                TypeUtil.stringToLocalDateTime(contract.getLegalAgreement().getSignedAt())
        );
        if (contract.isSetPaymentInstitution()) {
            payout.setPaymentInstitutionId(contract.getPaymentInstitution().getId());
        }
        LegalEntity legalEntity = contract.getContractor().getLegalEntity();
        if (legalEntity.isSetInternationalLegalEntity()) {
            InternationalLegalEntity internationalLegalEntity = legalEntity.getInternationalLegalEntity();
            payout.setAccountLegalName(internationalLegalEntity.getLegalName());
            payout.setAccountTradingName(internationalLegalEntity.getTradingName());
            payout.setAccountRegisteredAddress(internationalLegalEntity.getRegisteredAddress());
            payout.setAccountActualAddress(internationalLegalEntity.getActualAddress());
            payout.setAccountRegisteredNumber(internationalLegalEntity.getRegisteredNumber());
        }

        if (legalEntity.isSetRussianLegalEntity()) {
            RussianLegalEntity russianLegalEntity = legalEntity.getRussianLegalEntity();
            payout.setInn(russianLegalEntity.getInn());
            payout.setDescription(russianLegalEntity.getRegisteredName());
            payout.setAccountRegisteredNumber(russianLegalEntity.getRegisteredNumber());
        }

        PayoutTool payoutTool = contract.getPayoutTools().stream()
                .filter(payoutToolValue -> payoutToolValue.getId().equals(payoutToolId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(String.format("Payout tool not found, partyId='%s', payoutToolId='%s'", partyId, payoutToolId)));

        if (!shopAccount.getCurrency().equals(payoutTool.getCurrency())) {
            throw new InvalidStateException("Shop account and payout tool currency must be equals");
        }
        PayoutToolInfo payoutToolInfo = payoutTool.getPayoutToolInfo();
        if (payoutToolInfo.isSetRussianBankAccount()) {
            payout.setType(PayoutType.bank_account);
            payout.setAccountType(PayoutAccountType.russian_payout_account);
            RussianBankAccount bankAccount = payoutToolInfo.getRussianBankAccount();

            payout.setBankAccount(bankAccount.getAccount());
            payout.setBankLocalCode(bankAccount.getBankBik());
            payout.setBankName(bankAccount.getBankName());
            payout.setBankPostAccount(bankAccount.getBankPostAccount());
        }

        if (payoutToolInfo.isSetInternationalBankAccount()) {
            payout.setType(PayoutType.bank_account);
            payout.setAccountType(PayoutAccountType.international_payout_account);
            InternationalBankAccount bankAccount = payoutToolInfo.getInternationalBankAccount();

            payout.setBankAccount(bankAccount.getAccountHolder());
            payout.setBankNumber(bankAccount.getNumber());
            payout.setBankIban(bankAccount.getIban());
            if (bankAccount.isSetBank()) {
                InternationalBankDetails bankDetails = bankAccount.getBank();
                payout.setBankName(bankDetails.getName());
                payout.setBankAddress(bankDetails.getAddress());
                payout.setBankBic(bankDetails.getBic());
                payout.setBankAbaRtn(bankDetails.getAbaRtn());
                payout.setBankCountryCode(
                        Optional.ofNullable(bankDetails.getCountry())
                                .map(country -> country.toString())
                                .orElse(null)
                );
            }

            //OH SHI—
            if (bankAccount.isSetCorrespondentAccount()) {
                InternationalBankAccount correspondentAccount = bankAccount.getCorrespondentAccount();
                payout.setIntCorrBankAccount(correspondentAccount.getAccountHolder());
                payout.setIntCorrBankNumber(correspondentAccount.getNumber());
                payout.setIntCorrBankIban(correspondentAccount.getIban());
                if (correspondentAccount.isSetBank()) {
                    InternationalBankDetails corrBankDetails = correspondentAccount.getBank();
                    payout.setIntCorrBankName(corrBankDetails.getName());
                    payout.setIntCorrBankAddress(corrBankDetails.getAddress());
                    payout.setIntCorrBankBic(corrBankDetails.getBic());
                    payout.setIntCorrBankAbaRtn(corrBankDetails.getAbaRtn());
                    payout.setIntCorrBankCountryCode(
                            Optional.ofNullable(corrBankDetails.getCountry())
                                    .map(country -> country.toString())
                                    .orElse(null)
                    );
                }
            }
        }

        if (payoutToolInfo.isSetWalletInfo()) {
            payout.setType(PayoutType.wallet);
            payout.setWalletId(payoutToolInfo.getWalletInfo().getWalletId());
        }

        payout.setPurpose(buildPurpose(payout));

        if (payout.getType() != PayoutType.wallet
                && !payout.getPayoutToolId().equals(shop.getPayoutToolId())) {
            throw new InvalidStateException("Payout tool must be same as in shop for non-wallets payouts");
        }

        return payout;
    }

    private void saveRangeData(String payoutId, String partyId, String shopId, LocalDateTime fromTime, LocalDateTime toTime) throws StorageException {
        try {
            log.info("Trying to save payout range data, payoutId='{}', partyId='{}', shopId='{}', fromTime='{}', toTime='{}'",
                    payoutId, partyId, shopId, fromTime, toTime);
            payoutDao.saveRangeData(payoutId, partyId, shopId, fromTime, toTime);
            log.info("Payout range data have been saved, payoutId='{}', partyId='{}', shopId='{}', fromTime='{}', toTime='{}'",
                    payoutId, partyId, shopId, fromTime, toTime);
        } catch (DaoException ex) {
            throw new StorageException("Failed to save payout range data", ex);
        }
    }

    private long calculateAvailableAmount(String payoutId) {
        try {
            long availableAmount = payoutDao.getAvailableAmount(payoutId);
            log.info("Available amount have been calculated, payoutId='{}', availableAmount={}", payoutId, availableAmount);
            return availableAmount;
        } catch (DaoException ex) {
            throw new StorageException(ex);
        }
    }

    private void buildAndSavePayoutSummaryData(String payoutId) {
        try {
            List<PayoutSummary> payoutSummaries = new ArrayList<>();
            Optional.ofNullable(paymentDao.getSummary(payoutId)).ifPresent(
                    summary -> payoutSummaries.add(summary)
            );
            Optional.ofNullable(refundDao.getSummary(payoutId)).ifPresent(
                    summary -> payoutSummaries.add(summary)
            );
            Optional.ofNullable(payoutDao.getSummary(payoutId)).ifPresent(
                    summary -> payoutSummaries.add(summary)
            );
            Optional.ofNullable(chargebackDao.getSummary(payoutId)).ifPresent(
                    summary -> payoutSummaries.add(summary)
            );

            payoutSummaryService.save(payoutSummaries);
        } catch (DaoException ex) {
            throw new StorageException(ex);
        }
    }

    //legacy
    private boolean isBlockedForPayouts(String partyId) {
        Value metaDataValue = partyManagementService.getMetaData(partyId, "payout_blocking");
        return metaDataValue != null && metaDataValue.isSetB() && metaDataValue.getB();
    }

    private String generatePayoutId() {
        return UUID.randomUUID().toString();
    }

    private String buildPurpose(Payout payout) {
        if (payout.getAccountType() == PayoutAccountType.russian_payout_account) {
            return String.format(
                    "Перевод согласно договора номер %s от %s. Без НДС",
                    payout.getAccountLegalAgreementId(),
                    payout.getAccountLegalAgreementSignedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            );
        } else if (payout.getAccountType() == PayoutAccountType.international_payout_account || payout.getType() == PayoutType.wallet) {
            return String.format("Agr %s %s, %s for accepted payments.",
                    payout.getAccountLegalAgreementId(),
                    payout.getAccountLegalAgreementSignedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    payout.getPayoutId()
            );
        }

        throw new IllegalArgumentException(String.format("Unknown type, type='%s', accountType='%s'", payout.getType(), payout.getAccountType()));
    }

}
