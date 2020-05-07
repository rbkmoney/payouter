package com.rbkmoney.payouter.poller.handler.impl;

import com.rbkmoney.damsel.domain.InvoicePaymentAdjustment;
import com.rbkmoney.damsel.payment_processing.Event;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentAdjustmentChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentChange;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.geck.filter.Filter;
import com.rbkmoney.geck.filter.PathConditionFilter;
import com.rbkmoney.geck.filter.condition.IsNullCondition;
import com.rbkmoney.geck.filter.rule.PathConditionRule;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.payouter.dao.AdjustmentDao;
import com.rbkmoney.payouter.dao.PaymentDao;
import com.rbkmoney.payouter.domain.enums.AdjustmentStatus;
import com.rbkmoney.payouter.domain.tables.pojos.Adjustment;
import com.rbkmoney.payouter.domain.tables.pojos.Payment;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.poller.handler.Handler;
import com.rbkmoney.payouter.poller.handler.PaymentProcessingHandler;
import com.rbkmoney.payouter.util.CashFlowType;
import com.rbkmoney.payouter.util.DamselUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.rbkmoney.payouter.util.CashFlowType.*;

@Component
public class InvoicePaymentAdjustmentHandler implements PaymentProcessingHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final AdjustmentDao adjustmentDao;

    private final PaymentDao paymentDao;

    private final Filter filter;

    @Autowired
    public InvoicePaymentAdjustmentHandler(AdjustmentDao adjustmentDao, PaymentDao paymentDao) {
        this.adjustmentDao = adjustmentDao;
        this.paymentDao = paymentDao;
        this.filter = new PathConditionFilter(new PathConditionRule(
                "invoice_payment_change.payload.invoice_payment_adjustment_change.payload.invoice_payment_adjustment_created",
                new IsNullCondition().not()));
    }

    @Override
    public void handle(InvoiceChange invoiceChange, MachineEvent event) {
        long eventId = event.getEventId();
        String invoiceId = event.getSourceId();

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

        adjustment.setAdjustmentId(invoicePaymentAdjustment.getId());
        adjustment.setStatus(AdjustmentStatus.PENDING);
        adjustment.setCreatedAt(TypeUtil.stringToLocalDateTime(invoicePaymentAdjustment.getCreatedAt()));
        adjustment.setDomainRevision(invoicePaymentAdjustment.getDomainRevision());
        adjustment.setReason(invoicePaymentAdjustment.getReason());

        Map<CashFlowType, Long> oldCashFlowInverse = DamselUtil.parseInverseCashFlow(invoicePaymentAdjustment.getOldCashFlowInverse());
        Long paymentAmount = oldCashFlowInverse.getOrDefault(AMOUNT, 0L);
        Long paymentFee = oldCashFlowInverse.getOrDefault(FEE, 0L);
        Long paymentGuaranteeDeposit = oldCashFlowInverse.getOrDefault(GUARANTEE_DEPOSIT, 0L);

        Map<CashFlowType, Long> newCashFlow = DamselUtil.parseCashFlow(invoicePaymentAdjustment.getNewCashFlow());
        Long newAmount = newCashFlow.getOrDefault(AMOUNT, 0L);
        Long newFee = newCashFlow.getOrDefault(FEE, 0L);
        Long newGuaranteeDeposit = newCashFlow.getOrDefault(GUARANTEE_DEPOSIT, 0L);

        Long amount = (newAmount - newFee - newGuaranteeDeposit) -
                (paymentAmount - paymentFee - paymentGuaranteeDeposit);

        adjustment.setAmount(amount);

        adjustmentDao.save(adjustment);
        log.info("Adjustment have been saved, adjustment={}", adjustment);
    }

    @Override
    public Filter<InvoiceChange> getFilter() {
        return filter;
    }
}
