package com.rbkmoney.payouter.util;

import com.rbkmoney.damsel.domain.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum CashFlowType {

    UNKNOWN(Collections.emptyList(), Collections.emptyList()),
    AMOUNT(
            CashFlowAccount.provider(ProviderCashFlowAccount.settlement),
            CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)
    ),
    EXTERNAL_FEE(
            CashFlowAccount.system(SystemCashFlowAccount.settlement),
            Arrays.asList(
                    CashFlowAccount.external(ExternalCashFlowAccount.income),
                    CashFlowAccount.external(ExternalCashFlowAccount.outcome)
            )
    ),
    PROVIDER_FEE(
            CashFlowAccount.system(SystemCashFlowAccount.settlement),
            CashFlowAccount.provider(ProviderCashFlowAccount.settlement)
    ),
    RETURN_FEE(
            CashFlowAccount.system(SystemCashFlowAccount.settlement),
            CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)
    ),
    FEE(
            CashFlowAccount.merchant(MerchantCashFlowAccount.settlement),
            CashFlowAccount.system(SystemCashFlowAccount.settlement)
    ),
    REFUND_AMOUNT(
            CashFlowAccount.merchant(MerchantCashFlowAccount.settlement),
            CashFlowAccount.provider(ProviderCashFlowAccount.settlement)
    ),
    GUARANTEE_DEPOSIT(
            CashFlowAccount.merchant(MerchantCashFlowAccount.settlement),
            CashFlowAccount.merchant(MerchantCashFlowAccount.guarantee)
    ),
    PAYOUT_AMOUNT(
            CashFlowAccount.merchant(MerchantCashFlowAccount.settlement),
            CashFlowAccount.merchant(MerchantCashFlowAccount.payout)
    ),
    PAYOUT_FIXED_FEE(
            CashFlowAccount.merchant(MerchantCashFlowAccount.payout),
            CashFlowAccount.system(SystemCashFlowAccount.settlement)
    );

    private List<CashFlowAccount> sources;

    private List<CashFlowAccount> destinations;

    CashFlowType(CashFlowAccount source, CashFlowAccount destination) {
        this(Collections.singletonList(source), Collections.singletonList(destination));
    }

    CashFlowType(CashFlowAccount source, List<CashFlowAccount> destinations) {
        this(Collections.singletonList(source), destinations);
    }

    CashFlowType(List<CashFlowAccount> sources, List<CashFlowAccount> destinations) {
        this.sources = sources;
        this.destinations = destinations;
    }

    public List<CashFlowAccount> getSources() {
        return sources;
    }

    public List<CashFlowAccount> getDestinations() {
        return destinations;
    }

    public static CashFlowType getCashFlowType(FinalCashFlowPosting cashFlowPosting) {
        for (CashFlowType cashFlowType : values()) {
            if (cashFlowType.sources.contains(cashFlowPosting.getSource().getAccountType())
                    && cashFlowType.destinations.contains(cashFlowPosting.getDestination().getAccountType())) {
                return cashFlowType;
            }
        }
        return UNKNOWN;
    }
}
