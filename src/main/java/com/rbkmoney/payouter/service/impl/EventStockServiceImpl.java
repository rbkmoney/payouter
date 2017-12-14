package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.damsel.event_stock.SourceEvent;
import com.rbkmoney.damsel.event_stock.StockEvent;
import com.rbkmoney.damsel.payment_processing.EventPayload;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.payouter.poller.handler.Handler;
import com.rbkmoney.payouter.service.EventStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventStockServiceImpl implements EventStockService {

    private final List<Handler> handlers;

    @Autowired
    public EventStockServiceImpl(List<Handler> handlers) {
        this.handlers = handlers;
    }

//    @Override
//    public Long getLastEventId() throws StorageException {
//
//    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processStockEvent(StockEvent stockEvent) {
        SourceEvent sourceEvent = stockEvent.getSourceEvent();
        if (sourceEvent.isSetProcessingEvent()) {
            EventPayload payload = sourceEvent.getProcessingEvent().getPayload();
            if (payload.isSetInvoiceChanges()) {
                for (InvoiceChange invoiceChange : payload.getInvoiceChanges()) {
                    Handler handler = getHandler(invoiceChange);
                    if (handler != null) {
                        handler.handle(invoiceChange, stockEvent);
                    }
                }
            }
        }
    }

    private Handler getHandler(InvoiceChange invoiceChange) {
        for (Handler handler : handlers) {
            if (handler.accept(invoiceChange)) {
                return handler;
            }
        }
        return null;
    }

}
