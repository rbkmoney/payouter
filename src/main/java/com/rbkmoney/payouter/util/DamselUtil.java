package com.rbkmoney.payouter.util;

import com.cronutils.builder.CronBuilder;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.field.expression.And;
import com.cronutils.model.field.expression.FieldExpression;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import com.rbkmoney.damsel.base.*;
import com.rbkmoney.damsel.base.Schedule;
import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.payout_processing.*;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.geck.serializer.kit.json.JsonProcessor;
import com.rbkmoney.geck.serializer.kit.tbase.TBaseHandler;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutEvent;
import com.rbkmoney.payouter.exception.NotFoundException;
import org.apache.thrift.TBase;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.cronutils.model.field.expression.FieldExpression.always;
import static com.cronutils.model.field.expression.FieldExpressionFactory.every;
import static com.cronutils.model.field.expression.FieldExpressionFactory.on;
import static com.rbkmoney.payouter.util.CashFlowType.*;

public class DamselUtil {

    public final static ObjectMapper objectMapper = new ObjectMapper();

    public final static JsonProcessor jsonProcessor = new JsonProcessor();

    public static Map<CashFlowType, Long> parseCashFlow(List<FinalCashFlowPosting> finalCashFlow) {
        Map<CashFlowType, Long> collect = finalCashFlow.stream()
                .collect(
                        Collectors.groupingBy(
                                DamselUtil::getCashFlowType,
                                Collectors.summingLong(cashFlow -> cashFlow.getVolume().getAmount()
                                )
                        )
                );
        return collect;
    }

    public static CashFlowType getCashFlowType(FinalCashFlowPosting cashFlowPosting) {
        if (checkRoute(
                CashFlowAccount.provider(ProviderCashFlowAccount.settlement),
                CashFlowAccount.merchant(MerchantCashFlowAccount.settlement),
                cashFlowPosting)) {
            return AMOUNT;
        }
        if (checkRoute(
                CashFlowAccount.merchant(MerchantCashFlowAccount.settlement),
                CashFlowAccount.system(SystemCashFlowAccount.settlement),
                cashFlowPosting)) {
            return FEE;
        }
        if (checkRoute(
                CashFlowAccount.system(SystemCashFlowAccount.settlement),
                CashFlowAccount.provider(ProviderCashFlowAccount.settlement),
                cashFlowPosting)) {
            return PROVIDER_FEE;
        }
        if (checkRoute(
                CashFlowAccount.system(SystemCashFlowAccount.settlement),
                CashFlowAccount.external(ExternalCashFlowAccount.income),
                cashFlowPosting)) {
            return EXTERNAL_FEE;
        }
        if (checkRoute(
                CashFlowAccount.system(SystemCashFlowAccount.settlement),
                CashFlowAccount.external(ExternalCashFlowAccount.outcome),
                cashFlowPosting)) {
            return EXTERNAL_FEE;
        }
        if (checkRoute(
                CashFlowAccount.merchant(MerchantCashFlowAccount.settlement),
                CashFlowAccount.provider(ProviderCashFlowAccount.settlement),
                cashFlowPosting)) {
            return REFUND_AMOUNT;
        }
        if (checkRoute(
                CashFlowAccount.merchant(MerchantCashFlowAccount.settlement),
                CashFlowAccount.merchant(MerchantCashFlowAccount.guarantee),
                cashFlowPosting)) {
            return GUARANTEE_DEPOSIT;
        }

        throw new UnsupportedOperationException("Unsupported cashflow");
    }


    public static boolean checkRoute(CashFlowAccount source, CashFlowAccount destination, FinalCashFlowPosting cashFlow) {
        return source.equals(cashFlow.getSource().getAccountType()) &&
                destination.equals(cashFlow.getDestination().getAccountType());

    }

    public static <T extends TBase> T jsonToTBase(JsonNode jsonNode, Class<T> type) throws IOException {
        return jsonProcessor.process(jsonNode, new TBaseHandler<>(type));
    }

    public static PayoutCreated toDamselPayoutCreated(PayoutEvent payoutEvent) {
        PayoutCreated payoutCreated = new PayoutCreated();
        payoutCreated.setInitiator(toDamselUserInfo(payoutEvent));
        payoutCreated.setPayout(toDamselPayout(payoutEvent));
        return payoutCreated;
    }

    public static UserInfo buildUserInfo() {
        UserInfo userInfo = new UserInfo();
        userInfo.setId("TODO: getUser");
        userInfo.setType(UserType.internal_user(new InternalUser()));
        return userInfo;
    }

    public static PayoutStatus toDamselPayoutStatus(com.rbkmoney.payouter.domain.tables.pojos.Payout payout) {
        com.rbkmoney.payouter.domain.enums.PayoutStatus recordStatus = payout.getStatus();

        switch (recordStatus) {
            case UNPAID: {
                return PayoutStatus.unpaid(new PayoutUnpaid());
            }
            case PAID: {
                PaidDetails paidDetails = new PaidDetails();
                paidDetails.setAccountDetails(new AccountPaidDetails());
                return PayoutStatus.paid(new PayoutPaid(paidDetails));
            }
            case CONFIRMED: {
                return PayoutStatus.confirmed(new PayoutConfirmed(buildUserInfo()));
            }
            case CANCELLED: {
                PayoutCancelled payoutCancelled = new PayoutCancelled();
                payoutCancelled.setDetails(payout.getDescription());
                payoutCancelled.setUserInfo(buildUserInfo());
                return PayoutStatus.cancelled(payoutCancelled);
            }
            default: {
                throw new UnsupportedOperationException("Can't parse Payoutstatus enum." + recordStatus.name());
            }
        }
    }

    public static PayoutStatus toDamselPayoutStatus(PayoutEvent payoutEvent) {
        PayoutStatus._Fields payoutStatus = PayoutStatus._Fields.findByName(payoutEvent.getPayoutStatus());
        switch (payoutStatus) {
            case UNPAID:
                return PayoutStatus.unpaid(new PayoutUnpaid());
            case PAID:
                return PayoutStatus.paid(toDamselPayoutStatusPaid(payoutEvent));
            case CONFIRMED:
                return PayoutStatus.confirmed(new PayoutConfirmed(toDamselUserInfo(payoutEvent)));
            case CANCELLED:
                return PayoutStatus.cancelled(new PayoutCancelled(
                        toDamselUserInfo(payoutEvent),
                        payoutEvent.getPayoutStatusCancelDetails()
                ));
            default:
                throw new NotFoundException(String.format("Payout status not found, status = %s", payoutStatus));
        }
    }

    public static UserInfo toDamselUserInfo(PayoutEvent payoutEvent) {
        return new UserInfo(
                payoutEvent.getUserId(),
                toDamselUserType(payoutEvent)
        );
    }

    public static Payout toDamselPayout(PayoutEvent payoutEvent) {
        Payout payout = new Payout();
        payout.setId(payoutEvent.getPayoutId());
        payout.setPartyId(payoutEvent.getPayoutPartyId());
        payout.setShopId(payoutEvent.getPayoutShopId());
        payout.setCreatedAt(TypeUtil.temporalToString(payoutEvent.getPayoutCreatedAt()));
        payout.setStatus(toDamselPayoutStatus(payoutEvent));
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
            case BANK_CARD:
                return PayoutType.bank_card(new PayoutCard(
                        new BankCard(
                                payoutEvent.getPayoutCardToken(),
                                BankCardPaymentSystem.valueOf(payoutEvent.getPayoutCardPaymentSystem()),
                                payoutEvent.getPayoutCardBin(),
                                payoutEvent.getPayoutCardMaskedPan()
                        )
                ));
            case BANK_ACCOUNT:
                LegalAgreement legalAgreement = new LegalAgreement();
                legalAgreement.setLegalAgreementId(payoutEvent.getPayoutAccountLegalAgreementId());
                legalAgreement.setSignedAt(TypeUtil.temporalToString(payoutEvent.getPayoutAccountLegalAgreementSignedAt()));

                return PayoutType.bank_account(new PayoutAccount(
                        new RussianBankAccount(
                                payoutEvent.getPayoutAccountId(),
                                payoutEvent.getPayoutAccountBankName(),
                                payoutEvent.getPayoutAccountBankPostId(),
                                payoutEvent.getPayoutAccountBankBik()
                        ),
                        payoutEvent.getPayoutAccountInn(),
                        payoutEvent.getPayoutAccountPurpose(),
                        legalAgreement
                ));
            default:
                throw new NotFoundException(String.format("Payout type not found, type = %s", payoutType));
        }
    }

    public static PayoutPaid toDamselPayoutStatusPaid(PayoutEvent payoutEvent) {
        PayoutPaid payoutPaid = new PayoutPaid();
        payoutPaid.setDetails(toDamselPayoutPaidDetails(payoutEvent));
        return payoutPaid;
    }

    public static PaidDetails toDamselPayoutPaidDetails(PayoutEvent payoutEvent) {
        PaidDetails._Fields paidDetails = PaidDetails._Fields.findByName(payoutEvent.getPayoutPaidDetailsType());
        switch (paidDetails) {
            case CARD_DETAILS:
                return PaidDetails.card_details(new CardPaidDetails(
                        new ProviderDetails(
                                payoutEvent.getPayoutCardProviderName(),
                                payoutEvent.getPayoutCardProviderTransactionId()
                        )
                ));
            case ACCOUNT_DETAILS:
                return PaidDetails.account_details(new AccountPaidDetails());
            default:
                throw new NotFoundException(String.format("Paid details type not found, detailsType = %s", paidDetails));
        }
    }

    public static UserType toDamselUserType(PayoutEvent payoutEvent) {
        UserType._Fields userType = UserType._Fields.findByName(payoutEvent.getUserType());
        switch (userType) {
            case SERVICE_USER:
                return UserType.service_user(new ServiceUser());
            case EXTERNAL_USER:
                return UserType.external_user(new ExternalUser());
            case INTERNAL_USER:
                return UserType.internal_user(new InternalUser());
            default:
                throw new NotFoundException(String.format("User type not found, userType = %s", userType));
        }
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
}
