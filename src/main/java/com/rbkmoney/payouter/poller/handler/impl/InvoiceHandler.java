package com.rbkmoney.payouter.poller.handler.impl;

import com.rbkmoney.damsel.domain.Invoice;
import com.rbkmoney.damsel.domain.Shop;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.damsel.payment_processing.PartyRevisionParam;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.geck.filter.Filter;
import com.rbkmoney.geck.filter.PathConditionFilter;
import com.rbkmoney.geck.filter.condition.IsNullCondition;
import com.rbkmoney.geck.filter.rule.PathConditionRule;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.payouter.dao.InvoiceDao;
import com.rbkmoney.payouter.dao.ShopMetaDao;
import com.rbkmoney.payouter.exception.DaoException;
import com.rbkmoney.payouter.poller.handler.Handler;
import com.rbkmoney.payouter.poller.handler.PaymentProcessingHandler;
import com.rbkmoney.payouter.service.PartyManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceHandler implements PaymentProcessingHandler {

    private final ShopMetaDao shopMetaDao;

    private final InvoiceDao invoiceDao;

    private final PartyManagementService partyManagementService;

    private final Filter filter = new PathConditionFilter(
            new PathConditionRule(
                    "invoice_created",
                    new IsNullCondition().not()
            )
    );

    @Override
    public void handle(InvoiceChange invoiceChange, MachineEvent event) throws DaoException {
        Invoice invoice = invoiceChange.getInvoiceCreated().getInvoice();

        shopMetaDao.save(invoice.getOwnerId(), invoice.getShopId());
        log.info("Merchant shop have been saved, invoiceId={}, partyId={}, shopId={}",
                invoice.getId(), invoice.getOwnerId(), invoice.getShopId());

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
        log.info("Invoice have been saved, invoiceId={}, partyId={}, shopId={}",
                invoice.getId(), invoice.getOwnerId(), invoice.getShopId());
    }

    @Override
    public Filter<InvoiceChange> getFilter() {
        return filter;
    }
}
