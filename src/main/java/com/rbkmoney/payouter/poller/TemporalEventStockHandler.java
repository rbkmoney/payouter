package com.rbkmoney.payouter.poller;

import com.rbkmoney.damsel.domain.Invoice;
import com.rbkmoney.damsel.domain.InvoicePayment;
import com.rbkmoney.damsel.domain.Shop;
import com.rbkmoney.damsel.event_stock.StockEvent;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.eventstock.client.EventAction;
import com.rbkmoney.eventstock.client.EventHandler;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.payouter.dao.InvoiceDao;
import com.rbkmoney.payouter.dao.PaymentDao;
import com.rbkmoney.payouter.exception.DaoException;
import com.rbkmoney.payouter.service.PartyManagementService;
import com.rbkmoney.woody.api.flow.error.WRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TemporalEventStockHandler implements EventHandler<StockEvent> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final InvoiceDao invoiceDao;

    private final PaymentDao paymentDao;

    private final PartyManagementService partyManagementService;

    @Autowired
    public TemporalEventStockHandler(InvoiceDao invoiceDao, PaymentDao paymentDao, PartyManagementService partyManagementService) {
        this.invoiceDao = invoiceDao;
        this.paymentDao = paymentDao;
        this.partyManagementService = partyManagementService;
    }

    @Override
    public EventAction handle(StockEvent stockEvent, String subsKey) throws Exception {
        try {
            Event event = stockEvent.getSourceEvent().getProcessingEvent();
            if (event.getPayload().isSetInvoiceChanges()) {
                for (InvoiceChange invoiceChange : event.getPayload().getInvoiceChanges()) {
                    if (invoiceChange.isSetInvoiceCreated()) {
                        InvoiceCreated invoiceCreated = invoiceChange.getInvoiceCreated();
                        Invoice invoice = invoiceCreated.getInvoice();

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
                        log.info("Invoice have been updated, eventId='{}', invoiceId='{}', contractId='{}'", event.getId(), invoice.getId(), shop.getContractId());
                    } else if (invoiceChange.isSetInvoicePaymentChange()) {
                        InvoicePaymentChangePayload invoicePaymentChangePayload = invoiceChange.getInvoicePaymentChange().getPayload();
                        if (invoicePaymentChangePayload.isSetInvoicePaymentStarted()) {
                            String invoiceId = event.getSource().getInvoiceId();
                            InvoicePaymentStarted invoicePaymentStarted = invoicePaymentChangePayload.getInvoicePaymentStarted();
                            InvoicePayment invoicePayment = invoicePaymentStarted.getPayment();
                            String contractId = invoiceDao.get(invoiceId).getContractId();

                            paymentDao.updatePaymentMeta(
                                    event.getSource().getInvoiceId(),
                                    invoicePayment.getId(),
                                    contractId,
                                    invoicePayment.isSetPartyRevision() ? invoicePayment.getPartyRevision() : null
                            );
                            log.info("Payment have been updated, eventId='{}', invoiceId='{}', paymentId='{}', contractId='{}'", event.getId(), invoiceId, invoicePayment.getId(), contractId);
                        }

                    }
                }
            }

            return EventAction.CONTINUE;
        } catch (DaoException | WRuntimeException ex) {
            log.warn("Failed to handle event, retry", ex);
            return EventAction.DELAYED_RETRY;
        } catch (Exception ex) {
            log.error("Failed to handle event, interrupted", ex);
            return EventAction.INTERRUPT;
        }
    }
}
