package com.rbkmoney.payouter.poller.handler.impl;

import com.rbkmoney.damsel.domain.Invoice;
import com.rbkmoney.damsel.event_stock.StockEvent;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.geck.filter.Filter;
import com.rbkmoney.payouter.dao.InvoiceDao;
import com.rbkmoney.payouter.dao.ShopMetaDao;
import com.rbkmoney.payouter.poller.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InvoiceHandler implements Handler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ShopMetaDao shopMetaDao;

    private final InvoiceDao invoiceDao;

    @Autowired
    public InvoiceHandler(ShopMetaDao shopMetaDao, InvoiceDao invoiceDao) {
        this.shopMetaDao = shopMetaDao;
        this.invoiceDao = invoiceDao;
    }

    @Override
    public void handle(InvoiceChange invoiceChange, StockEvent stockEvent) {
        Invoice invoice = invoiceChange.getInvoiceCreated().getInvoice();
        shopMetaDao.save(invoice.getOwnerId(), invoice.getShopId());
        invoiceDao.save(invoice.getId(), invoice.getOwnerId(), invoice.getShopId());
        log.info("Invoice and merchant shop have been successfully handled, eventId={}, invoiceId={}, partyId={}, shopId={}",
                invoice.getId(), invoice.getOwnerId(), invoice.getShopId());
    }

    @Override
    public Filter<InvoiceChange> getFilter() {
        return invoiceChange -> invoiceChange.isSetInvoiceCreated();
    }
}
