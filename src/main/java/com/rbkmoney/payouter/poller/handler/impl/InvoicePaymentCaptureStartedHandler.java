package com.rbkmoney.payouter.poller.handler.impl;

import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentCaptureStarted;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.geck.filter.Filter;
import com.rbkmoney.geck.filter.PathConditionFilter;
import com.rbkmoney.geck.filter.condition.IsNullCondition;
import com.rbkmoney.geck.filter.rule.PathConditionRule;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.payouter.dao.PaymentDao;
import com.rbkmoney.payouter.domain.tables.pojos.Payment;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.poller.handler.PaymentProcessingHandler;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class InvoicePaymentCaptureStartedHandler implements PaymentProcessingHandler {

    private final PaymentDao paymentDao;

    private final Filter filter;

    @Autowired
    public InvoicePaymentCaptureStartedHandler(PaymentDao paymentDao) {
        this.paymentDao = paymentDao;
        this.filter = new PathConditionFilter(new PathConditionRule(
                "invoice_payment_change.payload.invoice_payment_capture_started",
                new IsNullCondition().not()));
    }


    @Override
    public void handle(InvoiceChange invoiceChange, MachineEvent event) {
        long eventId = event.getEventId();
        String invoiceId = event.getSourceId();
        String paymentId = invoiceChange.getInvoicePaymentChange().getId();
        Payment payment = paymentDao.get(invoiceId, paymentId);
        if (payment == null) {
            throw new NotFoundException(String.format("Invoice payment not found, invoiceId='%s', paymentId='%s'",
                    invoiceId, paymentId));
        }
        InvoicePaymentCaptureStarted invoicePaymentCaptureStarted = invoiceChange.getInvoicePaymentChange()
                .getPayload()
                .getInvoicePaymentCaptureStarted();
        if (invoicePaymentCaptureStarted.getParams().isSetCash()) {
            payment.setAmount(invoicePaymentCaptureStarted.getParams().getCash().getAmount());
            payment.setCurrencyCode(invoicePaymentCaptureStarted.getParams().getCash().getCurrency().getSymbolicCode());
        }
        paymentDao.save(payment);
        log.info("Payment capture started have been saved invoiceId='{}', paymentId='{}'", invoiceId, paymentId);
    }

    @Override
    public Filter<InvoiceChange> getFilter() {
        return filter;
    }
}
