package com.rbkmoney.payouter.poller.handler.impl;

import com.rbkmoney.damsel.domain.FinalCashFlowPosting;
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
import com.rbkmoney.payouter.util.CashFlowType;
import com.rbkmoney.payouter.util.DamselUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.rbkmoney.payouter.util.CashFlowType.*;

@Component
public class InvoicePaymentCashFlowChangedHandler implements Handler<InvoiceChange, Event> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final PaymentDao paymentDao;

    private final Filter filter;

    public InvoicePaymentCashFlowChangedHandler(PaymentDao paymentDao) {
        this.paymentDao = paymentDao;
        this.filter = new PathConditionFilter(new PathConditionRule(
                "invoice_payment_change.payload.invoice_payment_cash_flow_changed",
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

        List<FinalCashFlowPosting> finalCashFlow = invoicePaymentChange.getPayload().getInvoicePaymentCashFlowChanged().getCashFlow();
        Map<CashFlowType, Long> parsedCashFlow = DamselUtil.parseCashFlow(finalCashFlow);
        payment.setAmount(parsedCashFlow.getOrDefault(AMOUNT, 0L));
        payment.setFee(parsedCashFlow.getOrDefault(FEE, 0L));
        payment.setProviderFee(parsedCashFlow.getOrDefault(PROVIDER_FEE, 0L));
        payment.setExternalFee(parsedCashFlow.getOrDefault(EXTERNAL_FEE, 0L));
        payment.setGuaranteeDeposit(parsedCashFlow.getOrDefault(GUARANTEE_DEPOSIT, 0L));

        paymentDao.save(payment);
        log.info("Payment cash flow have been saved, finalCashFlow='{}', eventId='{}', invoiceId='{}', paymentId='{}'",
                finalCashFlow, event.getId(), invoiceId, paymentId);
    }

    @Override
    public Filter<InvoiceChange> getFilter() {
        return filter;
    }
}
