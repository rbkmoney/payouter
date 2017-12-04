package com.rbkmoney.payouter.poller.handler;


import com.rbkmoney.damsel.event_stock.StockEvent;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.geck.filter.Filter;

public interface Handler {

    default boolean accept(InvoiceChange invoiceChange) {
        return getFilter().match(invoiceChange);
    }

    void handle(InvoiceChange invoiceChange, StockEvent stockEvent);

    Filter<InvoiceChange> getFilter();

}
