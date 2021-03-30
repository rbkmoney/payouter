package com.rbkmoney.payouter.util;

import com.rbkmoney.damsel.domain.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

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
                                new FinalCashFlowAccount(
                                        CashFlowAccount.provider(ProviderCashFlowAccount.settlement), 1),
                                new FinalCashFlowAccount(
                                        CashFlowAccount.merchant(MerchantCashFlowAccount.guarantee), 2),
                                new Cash(5, new CurrencyRef("UGA"))
                        )
                ));
    }

    @Test
    public void testComputeAdjustmentAmount() {
        List<FinalCashFlowPosting> paymentCashFlow = List.of(

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.system(SystemCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.provider(ProviderCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(8945)),

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.provider(ProviderCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(483500)),

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.system(SystemCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(16923))
        );

        List<FinalCashFlowPosting> oldCashFlow = List.of(

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.provider(ProviderCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.system(SystemCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(8945)),

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.provider(ProviderCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(483500)),

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.system(SystemCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(16923))
        );

        List<FinalCashFlowPosting> newCashFlow = List.of(

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.system(SystemCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.provider(ProviderCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(8945)),

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.provider(ProviderCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(483500)),

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.system(SystemCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(14505))
        );

        long paymentAmount = DamselUtil.computeMerchantAmount(paymentCashFlow);
        long oldAmount = DamselUtil.computeMerchantAmount(oldCashFlow);
        long newAmount = DamselUtil.computeMerchantAmount(newCashFlow);

        assertEquals(paymentAmount, -oldAmount);
        assertEquals(2418, newAmount + oldAmount);
    }



    @Test
    public void testAdjustmentStatusChangeCaptureToFailed() {
        List<FinalCashFlowPosting> paymentCashFlow = List.of(

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.system(SystemCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.provider(ProviderCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(8945)),

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.provider(ProviderCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(483500)),

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.system(SystemCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(16923))
        );

        List<FinalCashFlowPosting> oldCashFlow = List.of(

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.provider(ProviderCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.system(SystemCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(8945)),

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.provider(ProviderCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(483500)),

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.system(SystemCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(16923))
        );

        List<FinalCashFlowPosting> newCashFlow = new ArrayList<>();

        long paymentAmount = DamselUtil.computeMerchantAmount(paymentCashFlow);
        long oldAmount = DamselUtil.computeMerchantAmount(oldCashFlow);
        long newAmount = DamselUtil.computeMerchantAmount(newCashFlow);

        assertEquals(0, newAmount);
        assertEquals(-paymentAmount, newAmount + oldAmount);
    }

    @Test
    public void testAdjustmentStatusChangeFailedToCapture() {
        List<FinalCashFlowPosting> paymentCashFlow = List.of(

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.system(SystemCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.provider(ProviderCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(8945)),

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.provider(ProviderCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(483500)),

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.system(SystemCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(16923))
        );

        List<FinalCashFlowPosting> oldCashFlow = new ArrayList<>();

        List<FinalCashFlowPosting> newCashFlow = List.of(
                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.system(SystemCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.provider(ProviderCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(8945)),

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.provider(ProviderCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(483500)),

                new FinalCashFlowPosting()
                        .setSource(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)))
                        .setDestination(new FinalCashFlowAccount().setAccountType(
                                CashFlowAccount.system(SystemCashFlowAccount.settlement)))
                        .setVolume(new Cash().setAmount(16923))
        );

        long paymentAmount = DamselUtil.computeMerchantAmount(paymentCashFlow);
        long oldAmount = DamselUtil.computeMerchantAmount(oldCashFlow);
        long newAmount = DamselUtil.computeMerchantAmount(newCashFlow);

        assertEquals(paymentAmount, newAmount + oldAmount);
    }
}
