package com.rbkmoney.payouter.poller.handler.impl;

import com.rbkmoney.damsel.payment_processing.Event;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentAdjustmentChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentChange;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.geck.filter.Filter;
import com.rbkmoney.payouter.dao.AdjustmentDao;
import com.rbkmoney.payouter.poller.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class InvoicePaymentAdjustmentCapturedHandler implements Handler<InvoiceChange, Event> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final AdjustmentDao adjustmentDao;

    @Autowired
    public InvoicePaymentAdjustmentCapturedHandler(AdjustmentDao adjustmentDao) {
        this.adjustmentDao = adjustmentDao;
    }

    @Override
    public void handle(InvoiceChange invoiceChange, Event event) {
        long eventId = event.getId();
        LocalDateTime capturedAt = TypeUtil.stringToLocalDateTime(event.getCreatedAt());
        String invoiceId = event.getSource().getInvoiceId();

        InvoicePaymentChange invoicePaymentChange = invoiceChange.getInvoicePaymentChange();
        String paymentId = invoiceChange.getInvoicePaymentChange().getId();

        InvoicePaymentAdjustmentChange invoicePaymentAdjustmentChange = invoicePaymentChange.getPayload()
                .getInvoicePaymentAdjustmentChange();

        String adjustmentId = invoicePaymentAdjustmentChange.getId();

        adjustmentDao.markAsCaptured(eventId, invoiceId, paymentId, adjustmentId, capturedAt);
        log.info("Adjustment have been captured, eventId={}, invoiceId={}, paymentId={}, adjustmentId={}",
                eventId, invoiceId, paymentId, adjustmentId);
    }

    @Override
    public Filter<InvoiceChange> getFilter() {
        return invoiceChange -> invoiceChange.isSetInvoicePaymentChange()
                && invoiceChange.getInvoicePaymentChange().getPayload().isSetInvoicePaymentAdjustmentChange()
                && invoiceChange.getInvoicePaymentChange().getPayload().getInvoicePaymentAdjustmentChange()
                .getPayload().isSetInvoicePaymentAdjustmentStatusChanged()
                && invoiceChange.getInvoicePaymentChange().getPayload().getInvoicePaymentAdjustmentChange()
                .getPayload().getInvoicePaymentAdjustmentStatusChanged().getStatus().isSetCaptured();
    }
}
