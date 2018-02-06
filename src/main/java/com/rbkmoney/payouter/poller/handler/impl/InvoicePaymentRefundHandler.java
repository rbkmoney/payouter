package com.rbkmoney.payouter.poller.handler.impl;

import com.rbkmoney.damsel.domain.InvoicePaymentRefund;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.geck.filter.Filter;
import com.rbkmoney.geck.filter.PathConditionFilter;
import com.rbkmoney.geck.filter.condition.IsNullCondition;
import com.rbkmoney.geck.filter.rule.PathConditionRule;
import com.rbkmoney.payouter.dao.PaymentDao;
import com.rbkmoney.payouter.dao.RefundDao;
import com.rbkmoney.payouter.domain.enums.RefundStatus;
import com.rbkmoney.payouter.domain.tables.pojos.Payment;
import com.rbkmoney.payouter.domain.tables.pojos.Refund;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.poller.handler.Handler;
import com.rbkmoney.payouter.service.PartyManagementService;
import com.rbkmoney.payouter.util.CashFlowType;
import com.rbkmoney.payouter.util.DamselUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.rbkmoney.payouter.util.CashFlowType.FEE;
import static com.rbkmoney.payouter.util.CashFlowType.REFUND_AMOUNT;

@Component
public class InvoicePaymentRefundHandler implements Handler<InvoiceChange, Event> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final RefundDao refundDao;

    private final PaymentDao paymentDao;

    private final Filter filter;

    @Autowired
    public InvoicePaymentRefundHandler(RefundDao refundDao, PaymentDao paymentDao) {
        this.refundDao = refundDao;
        this.paymentDao = paymentDao;
        this.filter = new PathConditionFilter(new PathConditionRule(
                "invoice_payment_change.payload.invoice_payment_refund_change.payload.invoice_payment_refund_created",
                new IsNullCondition().not()));
    }

    @Override
    public void handle(InvoiceChange invoiceChange, Event event) {
        long eventId = event.getId();
        String invoiceId = event.getSource().getInvoiceId();

        InvoicePaymentChange invoicePaymentChange = invoiceChange.getInvoicePaymentChange();
        String paymentId = invoicePaymentChange.getId();

        InvoicePaymentRefundChange invoicePaymentRefundChange = invoicePaymentChange.getPayload()
                .getInvoicePaymentRefundChange();
        InvoicePaymentRefundCreated invoicePaymentRefundCreated = invoicePaymentRefundChange.getPayload()
                .getInvoicePaymentRefundCreated();

        InvoicePaymentRefund invoicePaymentRefund = invoicePaymentRefundCreated.getRefund();

        Refund refund = new Refund();
        refund.setEventId(eventId);

        Payment payment = paymentDao.get(invoiceId, paymentId);

        if (payment == null) {
            throw new NotFoundException(String.format("Payment on refund not found, invoiceId='%s', paymentId='%s', refundId='%s'",
                    invoiceId, paymentId, invoicePaymentRefund.getId()));
        }

        refund.setPartyId(payment.getPartyId());
        refund.setShopId(payment.getShopId());

        refund.setInvoiceId(invoiceId);
        refund.setPaymentId(paymentId);
        refund.setRefundId(invoicePaymentRefund.getId());
        refund.setStatus(RefundStatus.PENDING);
        refund.setCreatedAt(TypeUtil.stringToLocalDateTime(invoicePaymentRefund.getCreatedAt()));
        refund.setReason(invoicePaymentRefund.getReason());
        refund.setDomainRevision(invoicePaymentRefund.getDomainRevision());

        Map<CashFlowType, Long> cashFlow = DamselUtil.parseCashFlow(invoicePaymentRefundCreated.getCashFlow());
        refund.setAmount(cashFlow.getOrDefault(REFUND_AMOUNT, 0L));
        refund.setFee(cashFlow.getOrDefault(FEE, 0L));

        refundDao.save(refund);
        log.info("Refund have been saved, eventId={}, refund={}", eventId, refund);
    }

    @Override
    public Filter<InvoiceChange> getFilter() {
        return filter;
    }
}
