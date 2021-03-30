package com.rbkmoney.payouter.poller.handler.impl;

import com.rbkmoney.damsel.domain.Cash;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentChargebackBodyChanged;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentChargebackChange;
import com.rbkmoney.geck.filter.Filter;
import com.rbkmoney.geck.filter.PathConditionFilter;
import com.rbkmoney.geck.filter.condition.IsNullCondition;
import com.rbkmoney.geck.filter.rule.PathConditionRule;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.payouter.dao.ChargebackDao;
import com.rbkmoney.payouter.domain.tables.pojos.Chargeback;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.poller.handler.PaymentProcessingHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoicePaymentChargebackBodyChangedHandler implements PaymentProcessingHandler {

    private static final Filter PREDICATE_FILTER = new PathConditionFilter(new PathConditionRule(
            "invoice_payment_change.payload.invoice_payment_chargeback_change.payload" +
                    ".invoice_payment_chargeback_body_changed",
            new IsNullCondition().not()));

    private final ChargebackDao chargebackDao;

    @Override
    public void handle(InvoiceChange invoiceChange, MachineEvent event) {
        String invoiceId = event.getSourceId();

        InvoicePaymentChange invoicePaymentChange = invoiceChange.getInvoicePaymentChange();
        String paymentId = invoicePaymentChange.getId();

        InvoicePaymentChargebackChange invoicePaymentChargebackChange = invoicePaymentChange.getPayload()
                .getInvoicePaymentChargebackChange();
        String chargebackId = invoicePaymentChargebackChange.getId();

        InvoicePaymentChargebackBodyChanged invoicePaymentChargebackBodyChanged =
                invoicePaymentChargebackChange.getPayload().getInvoicePaymentChargebackBodyChanged();

        Chargeback chargeback = chargebackDao.get(invoiceId, paymentId, chargebackId);
        if (chargeback == null) {
            throw new NotFoundException(
                    String.format("Invoice chargeback not found, invoiceId='%s', paymentId='%s' chargebackId='%s'",
                    invoiceId, paymentId, chargebackId));
        }

        Cash body = invoicePaymentChargebackBodyChanged.getBody();
        chargeback.setAmount(body.getAmount());
        chargeback.setCurrencyCode(body.getCurrency().getSymbolicCode());

        chargebackDao.save(chargeback);

        log.info("Chargeback body have been saved, invoiceId={}, paymentId={}, chargebackId={}",
                invoiceId, paymentId, chargebackId);
    }

    @Override
    public Filter<InvoiceChange> getFilter() {
        return PREDICATE_FILTER;
    }
}
