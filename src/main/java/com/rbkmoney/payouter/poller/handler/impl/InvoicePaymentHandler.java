package com.rbkmoney.payouter.poller.handler.impl;

import com.rbkmoney.damsel.domain.FinalCashFlowPosting;
import com.rbkmoney.damsel.domain.InvoicePayment;
import com.rbkmoney.damsel.domain.PaymentRoute;
import com.rbkmoney.damsel.payment_processing.Event;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentStarted;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.geck.filter.Filter;
import com.rbkmoney.payouter.dao.InvoiceDao;
import com.rbkmoney.payouter.dao.PaymentDao;
import com.rbkmoney.payouter.domain.enums.PaymentStatus;
import com.rbkmoney.payouter.domain.tables.pojos.Invoice;
import com.rbkmoney.payouter.domain.tables.pojos.Payment;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.poller.handler.Handler;
import com.rbkmoney.payouter.service.PartyManagementService;
import com.rbkmoney.payouter.util.CashFlowType;
import com.rbkmoney.payouter.util.DamselUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static com.rbkmoney.payouter.util.CashFlowType.*;

@Component
public class InvoicePaymentHandler implements Handler<InvoiceChange, Event> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final InvoiceDao invoiceDao;

    private final PaymentDao paymentDao;

    private final PartyManagementService partyManagementService;

    @Autowired
    public InvoicePaymentHandler(InvoiceDao invoiceDao, PaymentDao paymentDao, PartyManagementService partyManagementService) {
        this.invoiceDao = invoiceDao;
        this.paymentDao = paymentDao;
        this.partyManagementService = partyManagementService;
    }

    @Override
    public void handle(InvoiceChange invoiceChange, Event event) {
        InvoicePaymentStarted invoicePaymentStarted = invoiceChange
                .getInvoicePaymentChange()
                .getPayload()
                .getInvoicePaymentStarted();

        Payment payment = new Payment();
        InvoicePayment invoicePayment = invoicePaymentStarted.getPayment();

        payment.setEventId(event.getId());
        String invoiceId = event.getSource().getInvoiceId();
        payment.setInvoiceId(invoiceId);

        Invoice invoice = invoiceDao.get(invoiceId);
        if (invoice == null) {
            throw new NotFoundException(String.format("Invoice on payment not found, invoiceId='%s', paymentId='%s'",
                    invoiceId, invoicePayment.getId()));
        }

        payment.setPartyId(invoice.getPartyId());
        payment.setShopId(invoice.getShopId());

        PaymentRoute paymentRoute = invoicePaymentStarted.getRoute();
        int providerId = paymentRoute.getProvider().getId();
        payment.setProviderId(providerId);
        int terminalId = paymentRoute.getTerminal().getId();
        payment.setTerminalId(terminalId);

        payment.setPaymentId(invoicePayment.getId());
        payment.setCurrencyCode(invoicePayment.getCost().getCurrency().getSymbolicCode());
        payment.setStatus(PaymentStatus.PENDING);

        Instant paymentCreatedAt = TypeUtil.stringToInstant(invoicePayment.getCreatedAt());
        payment.setCreatedAt(LocalDateTime.ofInstant(paymentCreatedAt, ZoneOffset.UTC));
        payment.setDomainRevision(invoicePayment.getDomainRevision());

        List<FinalCashFlowPosting> finalCashFlow = invoicePaymentStarted.getCashFlow();
        Map<CashFlowType, Long> parsedCashFlow = DamselUtil.parseCashFlow(finalCashFlow);
        payment.setAmount(parsedCashFlow.getOrDefault(AMOUNT, 0L));
        payment.setFee(parsedCashFlow.getOrDefault(FEE, 0L));
        payment.setProviderFee(parsedCashFlow.getOrDefault(PROVIDER_FEE, 0L));
        payment.setExternalFee(parsedCashFlow.getOrDefault(EXTERNAL_FEE, 0L));
        payment.setGuaranteeDeposit(parsedCashFlow.getOrDefault(GUARANTEE_DEPOSIT, 0L));

        payment.setTest(
                partyManagementService.isTestCategoryType(
                        payment.getPartyId(),
                        payment.getShopId(),
                        payment.getDomainRevision(),
                        paymentCreatedAt
                )
        );

        paymentDao.save(payment);
        log.info("Payment have been saved, eventId={}, payment={}", event.getId(), payment);
    }

    @Override
    public Filter<InvoiceChange> getFilter() {
        return invoiceChange -> invoiceChange.isSetInvoicePaymentChange()
                && invoiceChange.getInvoicePaymentChange().getPayload().isSetInvoicePaymentStarted();
    }
}
