package com.rbkmoney.payouter.poller.handler.impl;

import com.rbkmoney.damsel.payment_processing.Event;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentAdjustmentChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentChange;
import com.rbkmoney.geck.filter.Filter;
import com.rbkmoney.geck.filter.PathConditionFilter;
import com.rbkmoney.geck.filter.condition.IsNullCondition;
import com.rbkmoney.geck.filter.rule.PathConditionRule;
import com.rbkmoney.payouter.dao.AdjustmentDao;
import com.rbkmoney.payouter.poller.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InvoicePaymentAdjustmentCancelledHandler implements Handler<InvoiceChange, Event> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final AdjustmentDao adjustmentDao;

    private final Filter filter;

    @Autowired
    public InvoicePaymentAdjustmentCancelledHandler(AdjustmentDao adjustmentDao) {
        this.adjustmentDao = adjustmentDao;
        this.filter = new PathConditionFilter(new PathConditionRule(
                "invoice_payment_change.payload.invoice_payment_adjustment_change.payload.invoice_payment_adjustment_status_changed.status.cancelled",
                new IsNullCondition().not()));
    }

    @Override
    public void handle(InvoiceChange invoiceChange, Event event) {
        long eventId = event.getId();
        String invoiceId = event.getSource().getInvoiceId();

        InvoicePaymentChange invoicePaymentChange = invoiceChange.getInvoicePaymentChange();
        String paymentId = invoiceChange.getInvoicePaymentChange().getId();

        InvoicePaymentAdjustmentChange invoicePaymentAdjustmentChange = invoicePaymentChange.getPayload()
                .getInvoicePaymentAdjustmentChange();

        String adjustmentId = invoicePaymentAdjustmentChange.getId();

        adjustmentDao.markAsCancelled(eventId, invoiceId, paymentId, adjustmentId);
        log.info("Adjustment have been cancelled, eventId={}, invoiceId={}, paymentId={}, adjustmentId={}",
                eventId, invoiceId, paymentId, adjustmentId);
    }

    @Override
    public Filter<InvoiceChange> getFilter() {
        return filter;
    }
}
