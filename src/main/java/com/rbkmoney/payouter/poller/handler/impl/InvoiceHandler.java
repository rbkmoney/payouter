package com.rbkmoney.payouter.poller.handler.impl;

import com.rbkmoney.damsel.domain.Invoice;
import com.rbkmoney.damsel.domain.Shop;
import com.rbkmoney.damsel.payment_processing.Event;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.damsel.payment_processing.PartyRevisionParam;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.geck.filter.Filter;
import com.rbkmoney.geck.filter.PathConditionFilter;
import com.rbkmoney.geck.filter.condition.IsNullCondition;
import com.rbkmoney.geck.filter.rule.PathConditionRule;
import com.rbkmoney.payouter.dao.InvoiceDao;
import com.rbkmoney.payouter.dao.ShopMetaDao;
import com.rbkmoney.payouter.poller.handler.Handler;
import com.rbkmoney.payouter.service.PartyManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InvoiceHandler implements Handler<InvoiceChange, Event> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ShopMetaDao shopMetaDao;

    private final InvoiceDao invoiceDao;

    private final PartyManagementService partyManagementService;

    private final Filter filter;

    @Autowired
    public InvoiceHandler(ShopMetaDao shopMetaDao, InvoiceDao invoiceDao, PartyManagementService partyManagementService) {
        this.shopMetaDao = shopMetaDao;
        this.invoiceDao = invoiceDao;
        this.partyManagementService = partyManagementService;
        this.filter = new PathConditionFilter(new PathConditionRule(
                "invoice_created",
                new IsNullCondition().not()));
    }

    @Override
    public void handle(InvoiceChange invoiceChange, Event event) {
        Invoice invoice = invoiceChange.getInvoiceCreated().getInvoice();
        long eventId = event.getId();

        shopMetaDao.save(invoice.getOwnerId(), invoice.getShopId());
        log.info("Merchant shop have been saved, eventId={}, invoiceId={}, partyId={}, shopId={}",
                eventId, invoice.getId(), invoice.getOwnerId(), invoice.getShopId());

        PartyRevisionParam partyRevisionParam;
        if (invoice.isSetPartyRevision()) {
            partyRevisionParam = PartyRevisionParam.revision(invoice.getPartyRevision());
        } else {
            partyRevisionParam = PartyRevisionParam.timestamp(invoice.getCreatedAt());
        }
        Shop shop = partyManagementService.getShop(invoice.getOwnerId(), invoice.getShopId(), partyRevisionParam);

        invoiceDao.save(
                invoice.getId(),
                invoice.getOwnerId(),
                invoice.getShopId(),
                shop.getContractId(),
                invoice.isSetPartyRevision() ? invoice.getPartyRevision() : null,
                TypeUtil.stringToLocalDateTime(invoice.getCreatedAt())
        );
        log.info("Invoice have been saved, eventId={}, invoiceId={}, partyId={}, shopId={}",
                eventId, invoice.getId(), invoice.getOwnerId(), invoice.getShopId());
    }

    @Override
    public Filter<InvoiceChange> getFilter() {
        return filter;
    }
}
