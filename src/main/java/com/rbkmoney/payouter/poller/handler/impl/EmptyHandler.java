package com.rbkmoney.payouter.poller.handler.impl;

import com.rbkmoney.damsel.event_stock.StockEvent;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.geck.filter.Filter;
import com.rbkmoney.payouter.poller.handler.Handler;
import org.springframework.stereotype.Component;

@Component
public class EmptyHandler implements Handler {
    @Override
    public void handle(InvoiceChange invoiceChange, StockEvent stockEvent) {
        // nothing
    }

    @Override
    public Filter<InvoiceChange> getFilter() {
        return invoiceChange -> true;
    }
}
