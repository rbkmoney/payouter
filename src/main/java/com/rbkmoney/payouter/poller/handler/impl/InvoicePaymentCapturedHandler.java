package com.rbkmoney.payouter.poller.handler.impl;

import com.rbkmoney.damsel.event_stock.StockEvent;
import com.rbkmoney.damsel.payment_processing.Event;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.geck.filter.Filter;
import com.rbkmoney.payouter.dao.PaymentDao;
import com.rbkmoney.payouter.poller.handler.Handler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class InvoicePaymentCapturedHandler implements Handler {

    private final PaymentDao paymentDao;

    @Autowired
    public InvoicePaymentCapturedHandler(PaymentDao paymentDao) {
        this.paymentDao = paymentDao;
    }

    @Override
    public void handle(InvoiceChange invoiceChange, StockEvent stockEvent) {
        Event event = stockEvent.getSourceEvent().getProcessingEvent();
        long eventId = event.getId();
        LocalDateTime capturedAt = TypeUtil.stringToLocalDateTime(event.getCreatedAt());
        String invoiceId = event.getSource().getInvoiceId();
        String paymentId = invoiceChange.getInvoicePaymentChange().getId();

        paymentDao.markAsCaptured(eventId, invoiceId, paymentId, capturedAt);
    }

    @Override
    public Filter<InvoiceChange> getFilter() {
        return invoiceChange -> invoiceChange.isSetInvoicePaymentChange()
                && invoiceChange.getInvoicePaymentChange().getPayload().isSetInvoicePaymentStatusChanged()
                && invoiceChange.getInvoicePaymentChange()
                .getPayload().getInvoicePaymentStatusChanged().getStatus().isSetCaptured();
    }
}
