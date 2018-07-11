package com.rbkmoney.payouter.poller.handler.impl;

import com.rbkmoney.damsel.domain.PaymentRoute;
import com.rbkmoney.damsel.payment_processing.Event;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentChange;
import com.rbkmoney.geck.filter.Filter;
import com.rbkmoney.geck.filter.PathConditionFilter;
import com.rbkmoney.geck.filter.condition.IsNullCondition;
import com.rbkmoney.geck.filter.rule.PathConditionRule;
import com.rbkmoney.payouter.dao.PaymentDao;
import com.rbkmoney.payouter.domain.tables.pojos.Payment;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.poller.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InvoicePaymentRouteChangedHandler implements Handler<InvoiceChange, Event> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final PaymentDao paymentDao;

    private final Filter filter;

    @Autowired
    public InvoicePaymentRouteChangedHandler(PaymentDao paymentDao) {
        this.paymentDao = paymentDao;
        this.filter = new PathConditionFilter(new PathConditionRule(
                "invoice_payment_change.payload.invoice_payment_route_changed",
                new IsNullCondition().not()));
    }

    @Override
    public void handle(InvoiceChange change, Event event) {
        InvoicePaymentChange invoicePaymentChange = change.getInvoicePaymentChange();
        String invoiceId = event.getSource().getInvoiceId();
        String paymentId = invoicePaymentChange.getId();
        Payment payment = paymentDao.get(invoiceId, paymentId);
        if (payment == null) {
            throw new NotFoundException(String.format("Invoice payment not found, invoiceId='%s', paymentId='%s'",
                    invoiceId, paymentId));
        }

        PaymentRoute paymentRoute = invoicePaymentChange.getPayload().getInvoicePaymentRouteChanged().getRoute();
        payment.setProviderId(paymentRoute.getProvider().getId());
        payment.setTerminalId(paymentRoute.getTerminal().getId());

        paymentDao.save(payment);
        log.info("Payment route have been saved, route='{}', eventId='{}', invoiceId='{}', paymentId='{}'", paymentRoute, event.getId(), invoiceId, paymentId);
    }

    @Override
    public Filter<InvoiceChange> getFilter() {
        return filter;
    }
}
