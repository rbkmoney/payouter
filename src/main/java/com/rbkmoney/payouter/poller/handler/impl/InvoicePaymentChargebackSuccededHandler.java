package com.rbkmoney.payouter.poller.handler.impl;

import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentChargebackChange;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.geck.filter.Filter;
import com.rbkmoney.geck.filter.PathConditionFilter;
import com.rbkmoney.geck.filter.condition.IsNullCondition;
import com.rbkmoney.geck.filter.rule.PathConditionRule;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.payouter.dao.ChargebackDao;
import com.rbkmoney.payouter.poller.handler.PaymentProcessingHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoicePaymentChargebackSuccededHandler implements PaymentProcessingHandler {

    private static final Filter PREDICATE_FILTER = new PathConditionFilter(new PathConditionRule(
            "invoice_payment_change.payload.invoice_payment_chargeback_change.payload.invoice_payment_chargeback_status_changed.status.accepted",
            new IsNullCondition().not()));

    private final ChargebackDao chargebackDao;

    @Override
    public void handle(InvoiceChange invoiceChange, MachineEvent event) {
        long eventId = event.getEventId();
        String invoiceId = event.getSourceId();

        InvoicePaymentChange invoicePaymentChange = invoiceChange.getInvoicePaymentChange();
        String paymentId = invoicePaymentChange.getId();

        InvoicePaymentChargebackChange invoicePaymentChargebackChange = invoicePaymentChange.getPayload()
                .getInvoicePaymentChargebackChange();
        String chargebackId = invoicePaymentChargebackChange.getId();

        LocalDateTime succeededAt = TypeUtil.stringToLocalDateTime(event.getCreatedAt());
        chargebackDao.markAsAccepted(eventId, invoiceId, paymentId, chargebackId, succeededAt);
        log.info("Chargeback have been accepted, invoiceId={}, paymentId={}, refundId={}",
                invoiceId, paymentId, chargebackId);
    }

    @Override
    public Filter<InvoiceChange> getFilter() {
        return PREDICATE_FILTER;
    }
}
