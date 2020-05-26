package com.rbkmoney.payouter.poller.handler.impl;

import com.rbkmoney.damsel.domain.Cash;
import com.rbkmoney.damsel.domain.InvoicePaymentChargeback;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentChargebackChange;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentChargebackCreated;
import com.rbkmoney.geck.common.util.TBaseUtil;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.geck.filter.Filter;
import com.rbkmoney.geck.filter.PathConditionFilter;
import com.rbkmoney.geck.filter.condition.IsNullCondition;
import com.rbkmoney.geck.filter.rule.PathConditionRule;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.payouter.dao.ChargebackDao;
import com.rbkmoney.payouter.dao.PaymentDao;
import com.rbkmoney.payouter.domain.enums.ChargebackCategory;
import com.rbkmoney.payouter.domain.enums.ChargebackStage;
import com.rbkmoney.payouter.domain.enums.ChargebackStatus;
import com.rbkmoney.payouter.domain.tables.pojos.Chargeback;
import com.rbkmoney.payouter.domain.tables.pojos.Payment;
import com.rbkmoney.payouter.exception.NotFoundException;
import com.rbkmoney.payouter.poller.handler.PaymentProcessingHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoicePaymentChargebackHandler implements PaymentProcessingHandler {

    private static final Filter PREDICATE_FILTER = new PathConditionFilter(new PathConditionRule(
            "invoice_payment_change.payload.invoice_payment_chargeback_change.payload.invoice_payment_chargeback_created",
            new IsNullCondition().not()));

    private final ChargebackDao chargebackDao;

    private final PaymentDao paymentDao;

    @Override
    public void handle(InvoiceChange invoiceChange, MachineEvent event) {
        long eventId = event.getEventId();
        String invoiceId = event.getSourceId();

        InvoicePaymentChange invoicePaymentChange = invoiceChange.getInvoicePaymentChange();
        String paymentId = invoicePaymentChange.getId();

        InvoicePaymentChargebackChange invoicePaymentChargebackChange = invoicePaymentChange.getPayload()
                .getInvoicePaymentChargebackChange();
        InvoicePaymentChargebackCreated invoicePaymentChargebackCreated = invoicePaymentChargebackChange.getPayload()
                .getInvoicePaymentChargebackCreated();

        InvoicePaymentChargeback invoicePaymentChargeback = invoicePaymentChargebackCreated.getChargeback();

        Payment payment = paymentDao.get(invoiceId, paymentId);

        if (payment == null) {
            throw new NotFoundException(String.format("Payment on chargeback not found, invoiceId='%s', paymentId='%s', chargebackId='%s'",
                    invoiceId, paymentId, invoicePaymentChargeback.getId()));
        }

        Chargeback chargeback = new Chargeback();
        chargeback.setId(eventId);
        chargeback.setEventId(eventId);
        chargeback.setPartyId(payment.getPartyId());
        chargeback.setShopId(payment.getShopId());
        chargeback.setInvoiceId(invoiceId);
        chargeback.setPaymentId(paymentId);
        chargeback.setChargebackId(invoicePaymentChargeback.getId());
        chargeback.setStatus(ChargebackStatus.PENDING);
        chargeback.setCreatedAt(TypeUtil.stringToLocalDateTime(invoicePaymentChargeback.getCreatedAt()));
        chargeback.setReason(invoicePaymentChargeback.getReason().getCode());
        chargeback.setReasonCategory(
                TBaseUtil.unionFieldToEnum(invoicePaymentChargeback.getReason().getCategory(), ChargebackCategory.class)
        );
        if (invoicePaymentChargeback.isSetBody()) {
            Cash chargebackCash = invoicePaymentChargeback.getBody();
            chargeback.setAmount(chargebackCash.getAmount());
            chargeback.setCurrencyCode(chargebackCash.getCurrency().getSymbolicCode());
        } else {
            chargeback.setAmount(payment.getAmount());
            chargeback.setCurrencyCode(payment.getCurrencyCode());
        }
        chargeback.setLevyAmount(invoicePaymentChargeback.getLevy().getAmount());
        chargeback.setLevyCurrencyCode(invoicePaymentChargeback.getLevy().getCurrency().getSymbolicCode());
        chargeback.setDomainRevision(invoicePaymentChargeback.getDomainRevision());
        chargeback.setPartyRevision(invoicePaymentChargeback.getPartyRevision());
        chargeback.setChargebackStage(
                TBaseUtil.unionFieldToEnum(invoicePaymentChargeback.getStage(), ChargebackStage.class)
        );

        chargebackDao.save(chargeback);
        log.info("Chargeback have been saved, chargeback={}", chargeback);
    }

    @Override
    public Filter<InvoiceChange> getFilter() {
        return PREDICATE_FILTER;
    }

}
