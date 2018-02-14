/*
 * This file is generated by jOOQ.
*/
package com.rbkmoney.payouter.domain;


import com.rbkmoney.payouter.domain.tables.Adjustment;
import com.rbkmoney.payouter.domain.tables.CashFlowDescription;
import com.rbkmoney.payouter.domain.tables.CashFlowPosting;
import com.rbkmoney.payouter.domain.tables.Invoice;
import com.rbkmoney.payouter.domain.tables.Payment;
import com.rbkmoney.payouter.domain.tables.Payout;
import com.rbkmoney.payouter.domain.tables.PayoutEvent;
import com.rbkmoney.payouter.domain.tables.Refund;
import com.rbkmoney.payouter.domain.tables.Report;
import com.rbkmoney.payouter.domain.tables.ShopMeta;
import com.rbkmoney.payouter.domain.tables.records.AdjustmentRecord;
import com.rbkmoney.payouter.domain.tables.records.CashFlowDescriptionRecord;
import com.rbkmoney.payouter.domain.tables.records.CashFlowPostingRecord;
import com.rbkmoney.payouter.domain.tables.records.InvoiceRecord;
import com.rbkmoney.payouter.domain.tables.records.PaymentRecord;
import com.rbkmoney.payouter.domain.tables.records.PayoutEventRecord;
import com.rbkmoney.payouter.domain.tables.records.PayoutRecord;
import com.rbkmoney.payouter.domain.tables.records.RefundRecord;
import com.rbkmoney.payouter.domain.tables.records.ReportRecord;
import com.rbkmoney.payouter.domain.tables.records.ShopMetaRecord;

import javax.annotation.Generated;

import org.jooq.Identity;
import org.jooq.UniqueKey;
import org.jooq.impl.AbstractKeys;


/**
 * A class modelling foreign key relationships between tables of the <code>sht</code> 
 * schema
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.9.6"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    // -------------------------------------------------------------------------
    // IDENTITY definitions
    // -------------------------------------------------------------------------

    public static final Identity<AdjustmentRecord, Long> IDENTITY_ADJUSTMENT = Identities0.IDENTITY_ADJUSTMENT;
    public static final Identity<CashFlowDescriptionRecord, Long> IDENTITY_CASH_FLOW_DESCRIPTION = Identities0.IDENTITY_CASH_FLOW_DESCRIPTION;
    public static final Identity<CashFlowPostingRecord, Long> IDENTITY_CASH_FLOW_POSTING = Identities0.IDENTITY_CASH_FLOW_POSTING;
    public static final Identity<PaymentRecord, Long> IDENTITY_PAYMENT = Identities0.IDENTITY_PAYMENT;
    public static final Identity<PayoutRecord, Long> IDENTITY_PAYOUT = Identities0.IDENTITY_PAYOUT;
    public static final Identity<PayoutEventRecord, Long> IDENTITY_PAYOUT_EVENT = Identities0.IDENTITY_PAYOUT_EVENT;
    public static final Identity<RefundRecord, Long> IDENTITY_REFUND = Identities0.IDENTITY_REFUND;
    public static final Identity<ReportRecord, Long> IDENTITY_REPORT = Identities0.IDENTITY_REPORT;

    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<AdjustmentRecord> ADJUSTMENT_PKEY = UniqueKeys0.ADJUSTMENT_PKEY;
    public static final UniqueKey<CashFlowDescriptionRecord> CASH_FLOW_DESCRIPTION_PKEY = UniqueKeys0.CASH_FLOW_DESCRIPTION_PKEY;
    public static final UniqueKey<CashFlowPostingRecord> POSTING_PKEY = UniqueKeys0.POSTING_PKEY;
    public static final UniqueKey<InvoiceRecord> INVOICE_PKEY = UniqueKeys0.INVOICE_PKEY;
    public static final UniqueKey<PaymentRecord> PAYMENT_PKEY = UniqueKeys0.PAYMENT_PKEY;
    public static final UniqueKey<PayoutRecord> PAYOUT_PKEY1 = UniqueKeys0.PAYOUT_PKEY1;
    public static final UniqueKey<PayoutEventRecord> PAYOUT_EVENT_PKEY = UniqueKeys0.PAYOUT_EVENT_PKEY;
    public static final UniqueKey<RefundRecord> REFUND_PKEY = UniqueKeys0.REFUND_PKEY;
    public static final UniqueKey<ReportRecord> REPORT_PKEY = UniqueKeys0.REPORT_PKEY;
    public static final UniqueKey<ShopMetaRecord> SHOP_META_PKEY = UniqueKeys0.SHOP_META_PKEY;

    // -------------------------------------------------------------------------
    // FOREIGN KEY definitions
    // -------------------------------------------------------------------------


    // -------------------------------------------------------------------------
    // [#1459] distribute members to avoid static initialisers > 64kb
    // -------------------------------------------------------------------------

    private static class Identities0 extends AbstractKeys {
        public static Identity<AdjustmentRecord, Long> IDENTITY_ADJUSTMENT = createIdentity(Adjustment.ADJUSTMENT, Adjustment.ADJUSTMENT.ID);
        public static Identity<CashFlowDescriptionRecord, Long> IDENTITY_CASH_FLOW_DESCRIPTION = createIdentity(CashFlowDescription.CASH_FLOW_DESCRIPTION, CashFlowDescription.CASH_FLOW_DESCRIPTION.ID);
        public static Identity<CashFlowPostingRecord, Long> IDENTITY_CASH_FLOW_POSTING = createIdentity(CashFlowPosting.CASH_FLOW_POSTING, CashFlowPosting.CASH_FLOW_POSTING.ID);
        public static Identity<PaymentRecord, Long> IDENTITY_PAYMENT = createIdentity(Payment.PAYMENT, Payment.PAYMENT.ID);
        public static Identity<PayoutRecord, Long> IDENTITY_PAYOUT = createIdentity(Payout.PAYOUT, Payout.PAYOUT.ID);
        public static Identity<PayoutEventRecord, Long> IDENTITY_PAYOUT_EVENT = createIdentity(PayoutEvent.PAYOUT_EVENT, PayoutEvent.PAYOUT_EVENT.EVENT_ID);
        public static Identity<RefundRecord, Long> IDENTITY_REFUND = createIdentity(Refund.REFUND, Refund.REFUND.ID);
        public static Identity<ReportRecord, Long> IDENTITY_REPORT = createIdentity(Report.REPORT, Report.REPORT.ID);
    }

    private static class UniqueKeys0 extends AbstractKeys {
        public static final UniqueKey<AdjustmentRecord> ADJUSTMENT_PKEY = createUniqueKey(Adjustment.ADJUSTMENT, "adjustment_pkey", Adjustment.ADJUSTMENT.ID);
        public static final UniqueKey<CashFlowDescriptionRecord> CASH_FLOW_DESCRIPTION_PKEY = createUniqueKey(CashFlowDescription.CASH_FLOW_DESCRIPTION, "cash_flow_description_pkey", CashFlowDescription.CASH_FLOW_DESCRIPTION.ID);
        public static final UniqueKey<CashFlowPostingRecord> POSTING_PKEY = createUniqueKey(CashFlowPosting.CASH_FLOW_POSTING, "posting_pkey", CashFlowPosting.CASH_FLOW_POSTING.ID);
        public static final UniqueKey<InvoiceRecord> INVOICE_PKEY = createUniqueKey(Invoice.INVOICE, "invoice_pkey", Invoice.INVOICE.ID);
        public static final UniqueKey<PaymentRecord> PAYMENT_PKEY = createUniqueKey(Payment.PAYMENT, "payment_pkey", Payment.PAYMENT.ID);
        public static final UniqueKey<PayoutRecord> PAYOUT_PKEY1 = createUniqueKey(Payout.PAYOUT, "payout_pkey1", Payout.PAYOUT.ID);
        public static final UniqueKey<PayoutEventRecord> PAYOUT_EVENT_PKEY = createUniqueKey(PayoutEvent.PAYOUT_EVENT, "payout_event_pkey", PayoutEvent.PAYOUT_EVENT.EVENT_ID);
        public static final UniqueKey<RefundRecord> REFUND_PKEY = createUniqueKey(Refund.REFUND, "refund_pkey", Refund.REFUND.ID);
        public static final UniqueKey<ReportRecord> REPORT_PKEY = createUniqueKey(Report.REPORT, "report_pkey", Report.REPORT.ID);
        public static final UniqueKey<ShopMetaRecord> SHOP_META_PKEY = createUniqueKey(ShopMeta.SHOP_META, "shop_meta_pkey", ShopMeta.SHOP_META.PARTY_ID, ShopMeta.SHOP_META.SHOP_ID);
    }
}
