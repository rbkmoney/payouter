package com.rbkmoney.payouter.poller.handler.impl;

import com.rbkmoney.damsel.domain.InvoicePaymentRefund;
import com.rbkmoney.damsel.event_stock.StockEvent;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.geck.filter.Filter;
import com.rbkmoney.payouter.dao.PaymentDao;
import com.rbkmoney.payouter.dao.RefundDao;
import com.rbkmoney.payouter.domain.enums.RefundStatus;
import com.rbkmoney.payouter.domain.tables.pojos.Payment;
import com.rbkmoney.payouter.domain.tables.pojos.Refund;
import com.rbkmoney.payouter.poller.handler.Handler;
import com.rbkmoney.payouter.util.CashFlowType;
import com.rbkmoney.payouter.util.DamselUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.rbkmoney.payouter.util.CashFlowType.FEE;
import static com.rbkmoney.payouter.util.CashFlowType.REFUND_AMOUNT;

@Component
public class InvoicePaymentRefundHandler implements Handler {

    private final RefundDao refundDao;

    private final PaymentDao paymentDao;

    @Autowired
    public InvoicePaymentRefundHandler(RefundDao refundDao, PaymentDao paymentDao) {
        this.refundDao = refundDao;
        this.paymentDao = paymentDao;
    }

    @Override
    public void handle(InvoiceChange invoiceChange, StockEvent stockEvent) {
        Event event = stockEvent.getSourceEvent().getProcessingEvent();
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
        refund.setPartyId(payment.getPartyId());
        refund.setShopId(payment.getShopId());

        refund.setInvoiceId(invoiceId);
        refund.setPaymentId(paymentId);
        refund.setRefundId(invoicePaymentRefund.getId());
        refund.setStatus(RefundStatus.PENDING);
        refund.setCreatedAt(TypeUtil.stringToLocalDateTime(invoicePaymentRefund.getCreatedAt()));
        refund.setReason(invoicePaymentRefund.getReason());

        Map<CashFlowType, Long> cashFlow = DamselUtil.parseCashFlow(invoicePaymentRefundCreated.getCashFlow());
        refund.setAmount(cashFlow.getOrDefault(REFUND_AMOUNT, 0L));
        refund.setFee(cashFlow.getOrDefault(FEE, 0L));

        refundDao.save(refund);
    }

    @Override
    public Filter<InvoiceChange> getFilter() {
        return invoiceChange -> invoiceChange.isSetInvoicePaymentChange()
                && invoiceChange.getInvoicePaymentChange().getPayload().isSetInvoicePaymentRefundChange()
                && invoiceChange.getInvoicePaymentChange().getPayload().getInvoicePaymentRefundChange()
                .getPayload().isSetInvoicePaymentRefundCreated();
    }
}
