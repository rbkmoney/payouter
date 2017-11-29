/*
 * This file is generated by jOOQ.
*/
package com.rbkmoney.payouter.domain.tables;


import com.rbkmoney.payouter.domain.Keys;
import com.rbkmoney.payouter.domain.Sht;
import com.rbkmoney.payouter.domain.enums.PaymentStatus;
import com.rbkmoney.payouter.domain.tables.records.PaymentRecord;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import org.jooq.Field;
import org.jooq.Identity;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.9.6"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Payment extends TableImpl<PaymentRecord> {

    private static final long serialVersionUID = -391748387;

    /**
     * The reference instance of <code>sht.payment</code>
     */
    public static final Payment PAYMENT = new Payment();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<PaymentRecord> getRecordType() {
        return PaymentRecord.class;
    }

    /**
     * The column <code>sht.payment.id</code>.
     */
    public final TableField<PaymentRecord, Long> ID = createField("id", org.jooq.impl.SQLDataType.BIGINT.nullable(false).defaultValue(org.jooq.impl.DSL.field("nextval('sht.payment_id_seq'::regclass)", org.jooq.impl.SQLDataType.BIGINT)), this, "");

    /**
     * The column <code>sht.payment.event_id</code>.
     */
    public final TableField<PaymentRecord, Long> EVENT_ID = createField("event_id", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>sht.payment.invoice_id</code>.
     */
    public final TableField<PaymentRecord, String> INVOICE_ID = createField("invoice_id", org.jooq.impl.SQLDataType.VARCHAR.nullable(false), this, "");

    /**
     * The column <code>sht.payment.payment_id</code>.
     */
    public final TableField<PaymentRecord, String> PAYMENT_ID = createField("payment_id", org.jooq.impl.SQLDataType.VARCHAR.nullable(false), this, "");

    /**
     * The column <code>sht.payment.party_id</code>.
     */
    public final TableField<PaymentRecord, String> PARTY_ID = createField("party_id", org.jooq.impl.SQLDataType.VARCHAR.nullable(false), this, "");

    /**
     * The column <code>sht.payment.shop_id</code>.
     */
    public final TableField<PaymentRecord, String> SHOP_ID = createField("shop_id", org.jooq.impl.SQLDataType.VARCHAR.nullable(false), this, "");

    /**
     * The column <code>sht.payment.provider_id</code>.
     */
    public final TableField<PaymentRecord, Integer> PROVIDER_ID = createField("provider_id", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>sht.payment.status</code>.
     */
    public final TableField<PaymentRecord, PaymentStatus> STATUS = createField("status", org.jooq.util.postgres.PostgresDataType.VARCHAR.asEnumDataType(com.rbkmoney.payouter.domain.enums.PaymentStatus.class), this, "");

    /**
     * The column <code>sht.payment.payout_id</code>.
     */
    public final TableField<PaymentRecord, Long> PAYOUT_ID = createField("payout_id", org.jooq.impl.SQLDataType.BIGINT, this, "");

    /**
     * The column <code>sht.payment.amount</code>.
     */
    public final TableField<PaymentRecord, Long> AMOUNT = createField("amount", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>sht.payment.provider_fee</code>.
     */
    public final TableField<PaymentRecord, Long> PROVIDER_FEE = createField("provider_fee", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>sht.payment.fee</code>.
     */
    public final TableField<PaymentRecord, Long> FEE = createField("fee", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>sht.payment.external_fee</code>.
     */
    public final TableField<PaymentRecord, Long> EXTERNAL_FEE = createField("external_fee", org.jooq.impl.SQLDataType.BIGINT, this, "");

    /**
     * The column <code>sht.payment.currency_code</code>.
     */
    public final TableField<PaymentRecord, String> CURRENCY_CODE = createField("currency_code", org.jooq.impl.SQLDataType.VARCHAR.nullable(false), this, "");

    /**
     * The column <code>sht.payment.captured_at</code>.
     */
    public final TableField<PaymentRecord, LocalDateTime> CAPTURED_AT = createField("captured_at", org.jooq.impl.SQLDataType.LOCALDATETIME, this, "");

    /**
     * The column <code>sht.payment.test</code>.
     */
    public final TableField<PaymentRecord, Boolean> TEST = createField("test", org.jooq.impl.SQLDataType.BOOLEAN.defaultValue(org.jooq.impl.DSL.field("false", org.jooq.impl.SQLDataType.BOOLEAN)), this, "");

    /**
     * The column <code>sht.payment.created_at</code>.
     */
    public final TableField<PaymentRecord, LocalDateTime> CREATED_AT = createField("created_at", org.jooq.impl.SQLDataType.LOCALDATETIME.nullable(false), this, "");

    /**
     * The column <code>sht.payment.terminal_id</code>.
     */
    public final TableField<PaymentRecord, Integer> TERMINAL_ID = createField("terminal_id", org.jooq.impl.SQLDataType.INTEGER, this, "");

    /**
     * The column <code>sht.payment.domain_revision</code>.
     */
    public final TableField<PaymentRecord, Long> DOMAIN_REVISION = createField("domain_revision", org.jooq.impl.SQLDataType.BIGINT, this, "");

    /**
     * Create a <code>sht.payment</code> table reference
     */
    public Payment() {
        this("payment", null);
    }

    /**
     * Create an aliased <code>sht.payment</code> table reference
     */
    public Payment(String alias) {
        this(alias, PAYMENT);
    }

    private Payment(String alias, Table<PaymentRecord> aliased) {
        this(alias, aliased, null);
    }

    private Payment(String alias, Table<PaymentRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema getSchema() {
        return Sht.SHT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Identity<PaymentRecord, Long> getIdentity() {
        return Keys.IDENTITY_PAYMENT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UniqueKey<PaymentRecord> getPrimaryKey() {
        return Keys.PAYMENT_PKEY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UniqueKey<PaymentRecord>> getKeys() {
        return Arrays.<UniqueKey<PaymentRecord>>asList(Keys.PAYMENT_PKEY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Payment as(String alias) {
        return new Payment(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Payment rename(String name) {
        return new Payment(name, null);
    }
}
