package com.rbkmoney.payouter.poller.handler.impl;

import com.rbkmoney.damsel.domain.InvoicePaymentAdjustment;
import com.rbkmoney.damsel.payment_processing.Event;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentAdjustmentChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentChange;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.geck.filter.Filter;
import com.rbkmoney.payouter.dao.AdjustmentDao;
import com.rbkmoney.payouter.dao.PaymentDao;
import com.rbkmoney.payouter.domain.enums.AdjustmentStatus;
import com.rbkmoney.payouter.domain.tables.pojos.Adjustment;
import com.rbkmoney.payouter.domain.tables.pojos.Payment;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.poller.handler.Handler;
import com.rbkmoney.payouter.util.CashFlowType;
import com.rbkmoney.payouter.util.DamselUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.rbkmoney.payouter.util.CashFlowType.*;

@Component
public class InvoicePaymentAdjustmentHandler implements Handler<InvoiceChange, Event> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final AdjustmentDao adjustmentDao;

    private final PaymentDao paymentDao;

    @Autowired
    public InvoicePaymentAdjustmentHandler(AdjustmentDao adjustmentDao, PaymentDao paymentDao) {
        this.adjustmentDao = adjustmentDao;
        this.paymentDao = paymentDao;
    }

    @Override
    public void handle(InvoiceChange invoiceChange, Event event) {
        long eventId = event.getId();
        String invoiceId = event.getSource().getInvoiceId();

        InvoicePaymentChange invoicePaymentChange = invoiceChange.getInvoicePaymentChange();

        String paymentId = invoicePaymentChange.getId();

        InvoicePaymentAdjustmentChange invoicePaymentAdjustmentChange = invoicePaymentChange.getPayload()
                .getInvoicePaymentAdjustmentChange();
        InvoicePaymentAdjustment invoicePaymentAdjustment = invoicePaymentAdjustmentChange
                .getPayload().getInvoicePaymentAdjustmentCreated().getAdjustment();

        Adjustment adjustment = new Adjustment();
        adjustment.setEventId(eventId);
        adjustment.setInvoiceId(invoiceId);
        adjustment.setPaymentId(paymentId);

        Payment payment = paymentDao.get(invoiceId, paymentId);
        if (payment == null) {
            throw new NotFoundException(String.format("Payment on adjustment not found, invoiceId='%s', paymentId='%s', adjustmentId='%s'",
                    invoiceId, paymentId, invoicePaymentAdjustment.getId()));
        }

        adjustment.setPartyId(payment.getPartyId());
        adjustment.setShopId(payment.getShopId());
        adjustment.setPaymentAmount(payment.getAmount());
        adjustment.setPaymentFee(payment.getFee());

        adjustment.setAdjustmentId(invoicePaymentAdjustment.getId());
        adjustment.setStatus(AdjustmentStatus.PENDING);
        adjustment.setCreatedAt(TypeUtil.stringToLocalDateTime(invoicePaymentAdjustment.getCreatedAt()));
        adjustment.setDomainRevision(invoicePaymentAdjustment.getDomainRevision());
        adjustment.setReason(invoicePaymentAdjustment.getReason());

        Map<CashFlowType, Long> newCashFlow = DamselUtil.parseCashFlow(invoicePaymentAdjustment.getNewCashFlow());
        adjustment.setNewAmount(newCashFlow.getOrDefault(AMOUNT, 0L));
        adjustment.setNewFee(newCashFlow.getOrDefault(FEE, 0L));
        adjustment.setNewProviderFee(newCashFlow.getOrDefault(PROVIDER_FEE, 0L));
        adjustment.setNewExternalFee(newCashFlow.getOrDefault(EXTERNAL_FEE, 0L));

        adjustmentDao.save(adjustment);
        log.info("Adjustment have been saved, eventId={}, adjustment={}", eventId, adjustment);
    }

    @Override
    public Filter<InvoiceChange> getFilter() {
        return invoiceChange -> invoiceChange.isSetInvoicePaymentChange()
                && invoiceChange.getInvoicePaymentChange().getPayload().isSetInvoicePaymentAdjustmentChange()
                && invoiceChange.getInvoicePaymentChange().getPayload().getInvoicePaymentAdjustmentChange()
                .getPayload().isSetInvoicePaymentAdjustmentCreated();
    }
}
