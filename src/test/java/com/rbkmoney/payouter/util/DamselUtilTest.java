package com.rbkmoney.payouter.util;

import com.rbkmoney.damsel.domain.*;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class DamselUtilTest {

    @Test
    public void testCorrectCashFlowPostings() {
        for (CashFlowType cashFlowType : CashFlowType.values()) {
            cashFlowType.getSources().forEach((sourceAccount) ->
                    cashFlowType.getDestinations().forEach(
                            (destinationAccount) ->
                                    assertEquals(
                                            cashFlowType,
                                            CashFlowType.getCashFlowType(
                                                    new FinalCashFlowPosting(
                                                            new FinalCashFlowAccount(sourceAccount, 1),
                                                            new FinalCashFlowAccount(destinationAccount, 2),
                                                            new Cash(5, new CurrencyRef("UGA"))
                                                    )
                                            )
                                    )

                    )
            );
        }
    }

    @Test
    public void testCorrectCashFlowPostingsWithCashFlowAccount() {
        for (CashFlowType cashFlowType : CashFlowType.values()) {
            cashFlowType.getSources().forEach((sourceAccount) ->
                    cashFlowType.getDestinations().forEach(
                            (destinationAccount) ->
                                    assertEquals(
                                            cashFlowType,
                                            CashFlowType.getCashFlowType(sourceAccount, destinationAccount)
                                    )

                    )
            );
        }
    }

    @Test
    public void testIncorrectCashFlowPostings() {
        assertEquals(
                CashFlowType.UNKNOWN,
                CashFlowType.getCashFlowType(
                new FinalCashFlowPosting(
                        new FinalCashFlowAccount(CashFlowAccount.provider(ProviderCashFlowAccount.settlement), 1),
                        new FinalCashFlowAccount(CashFlowAccount.merchant(MerchantCashFlowAccount.guarantee), 2),
                        new Cash(5, new CurrencyRef("UGA"))
                )
        ));
    }

}
