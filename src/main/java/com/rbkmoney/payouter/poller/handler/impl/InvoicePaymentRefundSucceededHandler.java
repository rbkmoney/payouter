package com.rbkmoney.payouter.poller.handler.impl;

import com.rbkmoney.damsel.event_stock.StockEvent;
import com.rbkmoney.damsel.payment_processing.Event;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentRefundChange;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.geck.filter.Filter;
import com.rbkmoney.payouter.dao.RefundDao;
import com.rbkmoney.payouter.poller.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class InvoicePaymentRefundSucceededHandler implements Handler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final RefundDao refundDao;

    @Autowired
    public InvoicePaymentRefundSucceededHandler(RefundDao refundDao) {
        this.refundDao = refundDao;
    }

    @Override
    public void handle(InvoiceChange invoiceChange, StockEvent stockEvent) {
        Event event = stockEvent.getSourceEvent().getProcessingEvent();
        long eventId = event.getId();
        LocalDateTime succeededAt = TypeUtil.stringToLocalDateTime(event.getCreatedAt());
        String invoiceId = event.getSource().getInvoiceId();

        InvoicePaymentChange invoicePaymentChange = invoiceChange.getInvoicePaymentChange();
        String paymentId = invoiceChange.getInvoicePaymentChange().getId();

        InvoicePaymentRefundChange invoicePaymentRefundChange = invoicePaymentChange.getPayload()
                .getInvoicePaymentRefundChange();

        String refundId = invoicePaymentRefundChange.getId();

        refundDao.markAsSucceeded(eventId, invoiceId, paymentId, refundId, succeededAt);
        log.info("Refund have been succeeded, eventId={}, invoiceId={}, paymentId={}, refundId={}",
                eventId, invoiceId, paymentId, refundId);
    }

    @Override
    public Filter<InvoiceChange> getFilter() {
        return invoiceChange -> invoiceChange.isSetInvoicePaymentChange()
                && invoiceChange.getInvoicePaymentChange().getPayload().isSetInvoicePaymentRefundChange()
                && invoiceChange.getInvoicePaymentChange().getPayload().getInvoicePaymentRefundChange()
                .getPayload().isSetInvoicePaymentRefundStatusChanged()
                && invoiceChange.getInvoicePaymentChange().getPayload().getInvoicePaymentRefundChange()
                .getPayload().getInvoicePaymentRefundStatusChanged().getStatus().isSetSucceeded();
    }
}
