package com.rbkmoney.payouter.service.data;

import com.rbkmoney.damsel.domain.InvoicePaymentChargebackPending;
import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.generation.EventsGenerator;
import com.rbkmoney.generation.GeneratorConfig;
import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestData {

    private static GeneratorConfig config = new GeneratorConfig();

    public static Event createChargebackCreated() {
        InvoicePaymentChargebackChangePayload invoicePaymentChargebackChangePayload = new InvoicePaymentChargebackChangePayload();
        invoicePaymentChargebackChangePayload.setInvoicePaymentChargebackCreated(buildChargebackCreated());
        InvoicePaymentChargebackChange invoicePaymentChargebackChange = new InvoicePaymentChargebackChange();
        invoicePaymentChargebackChange.setId("testId");
        invoicePaymentChargebackChange.setPayload(invoicePaymentChargebackChangePayload);
        InvoicePaymentChangePayload invoicePaymentChangePayload = new InvoicePaymentChangePayload();
        invoicePaymentChangePayload.setInvoicePaymentChargebackChange(invoicePaymentChargebackChange);
        InvoicePaymentChange invoicePaymentChange = new InvoicePaymentChange();
        invoicePaymentChange.setPayload(invoicePaymentChangePayload);
        invoicePaymentChange.setId(config.paymentId);
        InvoiceChange invoiceChange = new InvoiceChange();
        invoiceChange.setInvoicePaymentChange(invoicePaymentChange);

        return EventsGenerator.buildEvent(50, TypeUtil.temporalToString(Instant.now()), invoiceChange, "testInvoiceId");
    }

    public static Event createChargebackCaptured() {
        InvoicePaymentChargebackChangePayload invoicePaymentChargebackChangePayload = new InvoicePaymentChargebackChangePayload();
        invoicePaymentChargebackChangePayload.setInvoicePaymentChargebackStatusChanged(buildChargebackStatus());
        InvoicePaymentChargebackChange invoicePaymentChargebackChange = new InvoicePaymentChargebackChange();
        invoicePaymentChargebackChange.setId("testId");
        invoicePaymentChargebackChange.setPayload(invoicePaymentChargebackChangePayload);
        InvoicePaymentChangePayload invoicePaymentChangePayload = new InvoicePaymentChangePayload();
        invoicePaymentChangePayload.setInvoicePaymentChargebackChange(invoicePaymentChargebackChange);
        InvoicePaymentChange invoicePaymentChange = new InvoicePaymentChange();
        invoicePaymentChange.setPayload(invoicePaymentChangePayload);
        invoicePaymentChange.setId("testPaymentId");
        InvoiceChange invoiceChange = new InvoiceChange();
        invoiceChange.setInvoicePaymentChange(invoicePaymentChange);

        return EventsGenerator.buildEvent(64, TypeUtil.temporalToString(Instant.now()), invoiceChange, "testInvoiceId");
    }

    private static InvoicePaymentChargebackCreated buildChargebackCreated() {
        InvoicePaymentChargeback invoicePaymentChargeback = new InvoicePaymentChargeback();
        invoicePaymentChargeback.setId("testId");
        invoicePaymentChargeback.setCreatedAt(TypeUtil.temporalToString(Instant.now()));
        InvoicePaymentChargebackStage invoicePaymentChargebackStage = new InvoicePaymentChargebackStage();
        invoicePaymentChargebackStage.setChargeback(new InvoicePaymentChargebackStageChargeback());
        invoicePaymentChargeback.setStage(invoicePaymentChargebackStage);

        InvoicePaymentChargebackReason invoicePaymentChargebackReason = new InvoicePaymentChargebackReason();
        invoicePaymentChargebackReason.setCode("testCode");
        InvoicePaymentChargebackCategory invoicePaymentChargebackCategory = new InvoicePaymentChargebackCategory();
        invoicePaymentChargebackCategory.setFraud(new InvoicePaymentChargebackCategoryFraud());
        invoicePaymentChargebackReason.setCategory(invoicePaymentChargebackCategory);

        invoicePaymentChargeback.setReason(invoicePaymentChargebackReason)
        ;
        InvoicePaymentChargebackStatus invoicePaymentChargebackStatus = new InvoicePaymentChargebackStatus();
        invoicePaymentChargebackStatus.setPending(new InvoicePaymentChargebackPending());
        invoicePaymentChargeback.setStatus(invoicePaymentChargebackStatus);
        invoicePaymentChargeback.setBody(EnhancedRandom.random(Cash.class));
        invoicePaymentChargeback.setLevy(EnhancedRandom.random(Cash.class));
        invoicePaymentChargeback.setDomainRevision(12345);
        invoicePaymentChargeback.setPartyRevision(12345);

        InvoicePaymentChargebackCreated invoicePaymentChargebackCreated = new InvoicePaymentChargebackCreated();
        invoicePaymentChargebackCreated.setChargeback(invoicePaymentChargeback);

        return invoicePaymentChargebackCreated;
    }

    private static InvoicePaymentChargebackStatusChanged buildChargebackStatus() {
        InvoicePaymentChargebackStatusChanged invoicePaymentChargebackStatusChanged = new InvoicePaymentChargebackStatusChanged();
        InvoicePaymentChargebackStatus invoicePaymentChargebackStatus = new InvoicePaymentChargebackStatus();
        invoicePaymentChargebackStatus.setAccepted(new InvoicePaymentChargebackAccepted());
        invoicePaymentChargebackStatusChanged.setStatus(invoicePaymentChargebackStatus);

        return invoicePaymentChargebackStatusChanged;
    }

}
