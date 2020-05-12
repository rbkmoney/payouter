package com.rbkmoney.payouter.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.payout_processing.Wallet;
import com.rbkmoney.damsel.payout_processing.*;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.geck.serializer.kit.json.JsonHandler;
import com.rbkmoney.geck.serializer.kit.json.JsonProcessor;
import com.rbkmoney.geck.serializer.kit.tbase.TBaseHandler;
import com.rbkmoney.geck.serializer.kit.tbase.TBaseProcessor;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutEvent;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutSummary;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.exception.StorageException;
import org.apache.thrift.TBase;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DamselUtil {

    private final static ObjectMapper objectMapper = new ObjectMapper();
    private final static JsonProcessor jsonProcessor = new JsonProcessor();

    public static Long computeMerchantAmount(List<FinalCashFlowPosting> finalCashFlow) {
        long amountSource = computeAmount(finalCashFlow, FinalCashFlowPosting::getSource);
        long amountDest = computeAmount(finalCashFlow, FinalCashFlowPosting::getDestination);
        return amountDest - amountSource;
    }

    private static long computeAmount(List<FinalCashFlowPosting> finalCashFlow,
                                      Function<FinalCashFlowPosting, FinalCashFlowAccount> func) {
        return finalCashFlow.stream()
                .filter(f -> isMerchantSettlement(func.apply(f).getAccountType()))
                .mapToLong(cashFlow -> cashFlow.getVolume().getAmount())
                .sum();
    }

    private static boolean isMerchantSettlement(CashFlowAccount cashFlowAccount) {
        return cashFlowAccount.isSetMerchant() &&
                cashFlowAccount.getMerchant() == MerchantCashFlowAccount.settlement;
    }

    public static Map<CashFlowType, Long> parseCashFlow(List<FinalCashFlowPosting> finalCashFlow) {
        return finalCashFlow.stream().collect(
                Collectors.groupingBy(CashFlowType::getCashFlowType,
                        Collectors.summingLong(cashFlow -> cashFlow.getVolume().getAmount())));
    }

    public static <T extends TBase> T jsonToTBase(JsonNode jsonNode, Class<T> type) throws IOException {
        return jsonProcessor.process(jsonNode, new TBaseHandler<>(type));
    }

    public static PayoutCreated toDamselPayoutCreated(PayoutEvent payoutEvent) {
        PayoutCreated payoutCreated = new PayoutCreated();
        payoutCreated.setPayout(toDamselPayout(payoutEvent));
        return payoutCreated;
    }

    public static PayoutEvent toPayoutEvent(Payout payout, List<FinalCashFlowPosting> cashFlowPostings) {
        PayoutEvent payoutEvent = new PayoutEvent();
        payoutEvent.setEventType(PayoutChange._Fields.PAYOUT_CREATED.getFieldName());
        payoutEvent.setPayoutStatus(PayoutStatus._Fields.valueOf(payout.getStatus().toString()).getFieldName());
        payoutEvent.setPayoutId(payout.getPayoutId());
        payoutEvent.setPayoutCreatedAt(payout.getCreatedAt());
        payoutEvent.setPayoutPartyId(payout.getPartyId());
        payoutEvent.setPayoutShopId(payout.getShopId());
        payoutEvent.setContractId(payout.getContractId());
        payoutEvent.setPayoutType(payout.getType().getLiteral());
        payoutEvent.setAmount(payout.getAmount());
        payoutEvent.setFee(payout.getFee());
        payoutEvent.setCurrencyCode(payout.getCurrencyCode());

        payoutEvent.setPayoutAccountType(
                Optional.ofNullable(payout.getAccountType())
                        .map(accountType -> accountType.getLiteral())
                        .orElse(null)
        );
        payoutEvent.setPayoutAccountId(payout.getBankAccount());
        payoutEvent.setPayoutAccountLegalName(payout.getAccountLegalName());
        payoutEvent.setPayoutAccountTradingName(payout.getAccountTradingName());
        payoutEvent.setPayoutAccountRegisteredAddress(payout.getAccountRegisteredAddress());
        payoutEvent.setPayoutAccountActualAddress(payout.getAccountActualAddress());
        payoutEvent.setPayoutAccountRegisteredNumber(payout.getAccountRegisteredNumber());
        payoutEvent.setPayoutAccountBankPostId(payout.getBankPostAccount());
        payoutEvent.setPayoutAccountBankName(payout.getBankName());
        payoutEvent.setPayoutAccountBankNumber(payout.getBankNumber());
        payoutEvent.setPayoutAccountBankAddress(payout.getBankAddress());
        payoutEvent.setPayoutAccountBankBic(payout.getBankBic());
        payoutEvent.setPayoutAccountBankIban(payout.getBankIban());
        payoutEvent.setPayoutAccountBankLocalCode(payout.getBankLocalCode());
        payoutEvent.setPayoutAccountBankAbaRtn(payout.getBankAbaRtn());
        payoutEvent.setPayoutAccountBankCountryCode(payout.getBankCountryCode());

        //OH SHI—
        payoutEvent.setPayoutInternationalCorrespondentAccountBankAccount(payout.getIntCorrBankAccount());
        payoutEvent.setPayoutInternationalCorrespondentAccountBankName(payout.getIntCorrBankName());
        payoutEvent.setPayoutInternationalCorrespondentAccountBankNumber(payout.getIntCorrBankNumber());
        payoutEvent.setPayoutInternationalCorrespondentAccountBankAddress(payout.getIntCorrBankAddress());
        payoutEvent.setPayoutInternationalCorrespondentAccountBankBic(payout.getIntCorrBankBic());
        payoutEvent.setPayoutInternationalCorrespondentAccountBankIban(payout.getIntCorrBankIban());
        payoutEvent.setPayoutInternationalCorrespondentAccountBankAbaRtn(payout.getIntCorrBankAbaRtn());
        payoutEvent.setPayoutInternationalCorrespondentAccountBankCountryCode(payout.getIntCorrBankCountryCode());

        payoutEvent.setPayoutAccountInn(payout.getInn());
        payoutEvent.setPayoutAccountPurpose(payout.getPurpose());

        payoutEvent.setWalletId(payout.getWalletId());

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

        payoutEvent.setPayoutAccountLegalAgreementId(payout.getAccountLegalAgreementId());
        payoutEvent.setPayoutAccountLegalAgreementSignedAt(payout.getAccountLegalAgreementSignedAt());

        return payoutEvent;
    }

    public static PayoutStatus toDamselPayoutStatus(PayoutEvent payoutEvent) {
        PayoutStatus._Fields payoutStatus = PayoutStatus._Fields.findByName(payoutEvent.getPayoutStatus());
        switch (payoutStatus) {
            case UNPAID:
                return PayoutStatus.unpaid(new PayoutUnpaid());
            case PAID:
                return PayoutStatus.paid(new PayoutPaid());
            case CONFIRMED:
                return PayoutStatus.confirmed(new PayoutConfirmed());
            case CANCELLED:
                return PayoutStatus.cancelled(new PayoutCancelled(payoutEvent.getPayoutStatusCancelDetails()));
            default:
                throw new NotFoundException(String.format("Payout status not found, status = %s", payoutStatus));
        }
    }

    public static com.rbkmoney.damsel.payout_processing.Payout toDamselPayout(Payout payout, List<FinalCashFlowPosting> cashFlowPostings) {
        return toDamselPayout(toPayoutEvent(payout, cashFlowPostings));
    }

    public static com.rbkmoney.damsel.payout_processing.Payout toDamselPayout(PayoutEvent payoutEvent) {
        com.rbkmoney.damsel.payout_processing.Payout payout = new com.rbkmoney.damsel.payout_processing.Payout();
        payout.setId(payoutEvent.getPayoutId());
        payout.setPartyId(payoutEvent.getPayoutPartyId());
        payout.setShopId(payoutEvent.getPayoutShopId());
        payout.setContractId(payoutEvent.getContractId());
        payout.setCreatedAt(TypeUtil.temporalToString(payoutEvent.getPayoutCreatedAt()));
        payout.setStatus(toDamselPayoutStatus(payoutEvent));
        payout.setAmount(payoutEvent.getAmount());
        payout.setFee(payoutEvent.getFee());
        payout.setCurrency(new CurrencyRef(payoutEvent.getCurrencyCode()));
        payout.setType(toDamselPayoutType(payoutEvent));
        payout.setPayoutFlow(toDamselPayoutFlow(payoutEvent));
        return payout;
    }

    public static List<FinalCashFlowPosting> toDamselPayoutFlow(PayoutEvent payoutEvent) {
        List<FinalCashFlowPosting> finalCashFlowPostings = new ArrayList<>();
        try {
            for (JsonNode jsonNode : objectMapper.readTree(payoutEvent.getPayoutCashFlow())) {
                FinalCashFlowPosting finalCashFlowPosting = jsonToTBase(jsonNode, FinalCashFlowPosting.class);
                finalCashFlowPostings.add(finalCashFlowPosting);
            }
        } catch (IOException ex) {
            throw new RuntimeJsonMappingException(ex.getMessage());
        }
        return finalCashFlowPostings;
    }

    public static PayoutType toDamselPayoutType(PayoutEvent payoutEvent) {
        PayoutType._Fields payoutType = PayoutType._Fields.findByName(payoutEvent.getPayoutType());
        switch (payoutType) {
            case BANK_ACCOUNT:
                return PayoutType.bank_account(toPayoutAccount(payoutEvent));
            case WALLET:
                return PayoutType.wallet(new Wallet(payoutEvent.getWalletId()));
            default:
                throw new NotFoundException(String.format("Payout type not found, type = %s", payoutType));
        }
    }

    public static PayoutAccount toPayoutAccount(PayoutEvent payoutEvent) {
        LegalAgreement legalAgreement = new LegalAgreement();
        legalAgreement.setLegalAgreementId(payoutEvent.getPayoutAccountLegalAgreementId());
        legalAgreement.setSignedAt(TypeUtil.temporalToString(payoutEvent.getPayoutAccountLegalAgreementSignedAt()));

        PayoutAccount._Fields payoutAccountType = PayoutAccount._Fields.findByName(payoutEvent.getPayoutAccountType());
        switch (payoutAccountType) {
            case RUSSIAN_PAYOUT_ACCOUNT:
                return PayoutAccount.russian_payout_account(
                        new RussianPayoutAccount(
                                new RussianBankAccount(
                                        payoutEvent.getPayoutAccountId(),
                                        payoutEvent.getPayoutAccountBankName(),
                                        payoutEvent.getPayoutAccountBankPostId(),
                                        payoutEvent.getPayoutAccountBankLocalCode()
                                ),
                                payoutEvent.getPayoutAccountInn(),
                                payoutEvent.getPayoutAccountPurpose(),
                                legalAgreement
                        )
                );
            case INTERNATIONAL_PAYOUT_ACCOUNT:
                return PayoutAccount.international_payout_account(
                        new InternationalPayoutAccount(
                                toInternationalBankAccount(payoutEvent),
                                toInternationalLegalEntity(payoutEvent),
                                payoutEvent.getPayoutAccountPurpose(),
                                legalAgreement
                        )
                );
            default:
                throw new NotFoundException(String.format("Payout account type not found, type = %s", payoutAccountType));
        }
    }

    private static InternationalLegalEntity toInternationalLegalEntity(PayoutEvent payoutEvent) {
        InternationalLegalEntity legalEntity = new InternationalLegalEntity();
        legalEntity.setLegalName(payoutEvent.getPayoutAccountLegalName());
        legalEntity.setTradingName(payoutEvent.getPayoutAccountTradingName());
        legalEntity.setRegisteredAddress(payoutEvent.getPayoutAccountRegisteredAddress());
        legalEntity.setActualAddress(payoutEvent.getPayoutAccountActualAddress());
        legalEntity.setRegisteredNumber(payoutEvent.getPayoutAccountRegisteredNumber());
        return legalEntity;
    }

    private static InternationalBankAccount toInternationalBankAccount(PayoutEvent payoutEvent) {
        InternationalBankAccount bankAccount = new InternationalBankAccount();
        bankAccount.setAccountHolder(payoutEvent.getPayoutAccountId());
        bankAccount.setNumber(payoutEvent.getPayoutAccountBankNumber());
        bankAccount.setIban(payoutEvent.getPayoutAccountBankIban());

        InternationalBankDetails bankDetails = new InternationalBankDetails();
        bankDetails.setName(payoutEvent.getPayoutAccountBankName());
        bankDetails.setBic(payoutEvent.getPayoutAccountBankBic());
        bankDetails.setAbaRtn(payoutEvent.getPayoutAccountBankAbaRtn());
        bankDetails.setAddress(payoutEvent.getPayoutAccountBankAddress());
        bankDetails.setCountry(TypeUtil.toEnumField(payoutEvent.getPayoutAccountBankCountryCode(), Residence.class));
        bankAccount.setBank(bankDetails);

        //OH SHI—
        InternationalBankAccount correspondentBankAccount = new InternationalBankAccount();
        correspondentBankAccount.setAccountHolder(payoutEvent.getPayoutInternationalCorrespondentAccountBankAccount());
        correspondentBankAccount.setNumber(payoutEvent.getPayoutInternationalCorrespondentAccountBankNumber());
        correspondentBankAccount.setIban(payoutEvent.getPayoutInternationalCorrespondentAccountBankIban());

        InternationalBankDetails correspondentBankDetails = new InternationalBankDetails();
        correspondentBankDetails.setName(payoutEvent.getPayoutInternationalCorrespondentAccountBankName());
        correspondentBankDetails.setBic(payoutEvent.getPayoutInternationalCorrespondentAccountBankBic());
        correspondentBankDetails.setAddress(payoutEvent.getPayoutInternationalCorrespondentAccountBankAddress());
        correspondentBankDetails.setAbaRtn(payoutEvent.getPayoutInternationalCorrespondentAccountBankAbaRtn());
        correspondentBankDetails.setCountry(TypeUtil.toEnumField(payoutEvent.getPayoutInternationalCorrespondentAccountBankCountryCode(), Residence.class));
        correspondentBankAccount.setBank(correspondentBankDetails);
        bankAccount.setCorrespondentAccount(correspondentBankAccount);

        return bankAccount;
    }

    public static PayoutChange toDamselPayoutChange(PayoutEvent payoutEvent) {
        PayoutChange._Fields payoutChangeType = PayoutChange._Fields.findByName(payoutEvent.getEventType());
        switch (payoutChangeType) {
            case PAYOUT_CREATED:
                return PayoutChange.payout_created(
                        DamselUtil.toDamselPayoutCreated(payoutEvent)
                );
            case PAYOUT_STATUS_CHANGED:
                return PayoutChange.payout_status_changed(new PayoutStatusChanged(DamselUtil.toDamselPayoutStatus(payoutEvent)));
            default:
                throw new NotFoundException(String.format("Payout event type not found, eventType = %s", payoutChangeType));
        }
    }

    public static Event toDamselEvent(PayoutEvent payoutEvent) {
        Event event = new Event();
        event.setId(payoutEvent.getEventId());
        event.setCreatedAt(TypeUtil.temporalToString(payoutEvent.getEventCreatedAt()));
        event.setSource(EventSource.payout_id(payoutEvent.getPayoutId()));
        event.setPayload(EventPayload.payout_changes(Arrays.asList(
                DamselUtil.toDamselPayoutChange(payoutEvent)
        )));
        return event;
    }

    public static List<PayoutSummaryItem> toDamselPayoutSummary(List<PayoutSummary> payoutSummaries) {
        return payoutSummaries.stream().map(cfd -> {
            PayoutSummaryItem payoutSummaryItem = new PayoutSummaryItem();
            payoutSummaryItem.setAmount(cfd.getAmount());
            payoutSummaryItem.setFee(Optional.ofNullable(cfd.getFee()).orElse(0L));
            payoutSummaryItem.setCurrencySymbolicCode(cfd.getCurrencyCode());
            payoutSummaryItem.setCount(cfd.getCount());
            payoutSummaryItem.setOperationType(OperationType.valueOf(cfd.getCashFlowType().getLiteral()));
            payoutSummaryItem.setFromTime(TypeUtil.temporalToString(cfd.getFromTime()));
            payoutSummaryItem.setToTime(TypeUtil.temporalToString(cfd.getToTime()));
            return payoutSummaryItem;
        }).collect(Collectors.toList());
    }
}
