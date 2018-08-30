/*
 * This file is generated by jOOQ.
*/
package com.rbkmoney.payouter.domain.tables;


import com.rbkmoney.payouter.domain.Keys;
import com.rbkmoney.payouter.domain.Sht;
import com.rbkmoney.payouter.domain.enums.PayoutAccountType;
import com.rbkmoney.payouter.domain.enums.PayoutStatus;
import com.rbkmoney.payouter.domain.enums.PayoutType;
import com.rbkmoney.payouter.domain.tables.records.PayoutRecord;

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
public class Payout extends TableImpl<PayoutRecord> {

    private static final long serialVersionUID = -760341276;

    /**
     * The reference instance of <code>sht.payout</code>
     */
    public static final Payout PAYOUT = new Payout();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<PayoutRecord> getRecordType() {
        return PayoutRecord.class;
    }

    /**
     * The column <code>sht.payout.id</code>.
     */
    public final TableField<PayoutRecord, Long> ID = createField("id", org.jooq.impl.SQLDataType.BIGINT.nullable(false).defaultValue(org.jooq.impl.DSL.field("nextval('sht.payout_id_sequence'::regclass)", org.jooq.impl.SQLDataType.BIGINT)), this, "");

    /**
     * The column <code>sht.payout.party_id</code>.
     */
    public final TableField<PayoutRecord, String> PARTY_ID = createField("party_id", org.jooq.impl.SQLDataType.VARCHAR.nullable(false), this, "");

    /**
     * The column <code>sht.payout.shop_id</code>.
     */
    public final TableField<PayoutRecord, String> SHOP_ID = createField("shop_id", org.jooq.impl.SQLDataType.VARCHAR.nullable(false), this, "");

    /**
     * The column <code>sht.payout.created_at</code>.
     */
    public final TableField<PayoutRecord, LocalDateTime> CREATED_AT = createField("created_at", org.jooq.impl.SQLDataType.LOCALDATETIME.nullable(false), this, "");

    /**
     * The column <code>sht.payout.from_time</code>.
     */
    public final TableField<PayoutRecord, LocalDateTime> FROM_TIME = createField("from_time", org.jooq.impl.SQLDataType.LOCALDATETIME, this, "");

    /**
     * The column <code>sht.payout.to_time</code>.
     */
    public final TableField<PayoutRecord, LocalDateTime> TO_TIME = createField("to_time", org.jooq.impl.SQLDataType.LOCALDATETIME, this, "");

    /**
     * The column <code>sht.payout.status</code>.
     */
    public final TableField<PayoutRecord, PayoutStatus> STATUS = createField("status", org.jooq.util.postgres.PostgresDataType.VARCHAR.asEnumDataType(com.rbkmoney.payouter.domain.enums.PayoutStatus.class), this, "");

    /**
     * The column <code>sht.payout.type</code>.
     */
    public final TableField<PayoutRecord, PayoutType> TYPE = createField("type", org.jooq.util.postgres.PostgresDataType.VARCHAR.asEnumDataType(com.rbkmoney.payouter.domain.enums.PayoutType.class), this, "");

    /**
     * The column <code>sht.payout.amount</code>.
     */
    public final TableField<PayoutRecord, Long> AMOUNT = createField("amount", org.jooq.impl.SQLDataType.BIGINT, this, "");

    /**
     * The column <code>sht.payout.shop_acc</code>.
     */
    public final TableField<PayoutRecord, Long> SHOP_ACC = createField("shop_acc", org.jooq.impl.SQLDataType.BIGINT, this, "");

    /**
     * The column <code>sht.payout.shop_payout_acc</code>.
     */
    public final TableField<PayoutRecord, Long> SHOP_PAYOUT_ACC = createField("shop_payout_acc", org.jooq.impl.SQLDataType.BIGINT, this, "");

    /**
     * The column <code>sht.payout.currency_code</code>.
     */
    public final TableField<PayoutRecord, String> CURRENCY_CODE = createField("currency_code", org.jooq.impl.SQLDataType.VARCHAR, this, "");

    /**
     * The column <code>sht.payout.bank_account</code>.
     */
    public final TableField<PayoutRecord, String> BANK_ACCOUNT = createField("bank_account", org.jooq.impl.SQLDataType.VARCHAR, this, "");

    /**
     * The column <code>sht.payout.bank_local_code</code>.
     */
    public final TableField<PayoutRecord, String> BANK_LOCAL_CODE = createField("bank_local_code", org.jooq.impl.SQLDataType.VARCHAR, this, "");

    /**
     * The column <code>sht.payout.bank_name</code>.
     */
    public final TableField<PayoutRecord, String> BANK_NAME = createField("bank_name", org.jooq.impl.SQLDataType.VARCHAR, this, "");

    /**
     * The column <code>sht.payout.bank_post_account</code>.
     */
    public final TableField<PayoutRecord, String> BANK_POST_ACCOUNT = createField("bank_post_account", org.jooq.impl.SQLDataType.VARCHAR, this, "");

    /**
     * The column <code>sht.payout.inn</code>.
     */
    public final TableField<PayoutRecord, String> INN = createField("inn", org.jooq.impl.SQLDataType.VARCHAR, this, "");

    /**
     * The column <code>sht.payout.purpose</code>.
     */
    public final TableField<PayoutRecord, String> PURPOSE = createField("purpose", org.jooq.impl.SQLDataType.VARCHAR, this, "");

    /**
     * The column <code>sht.payout.description</code>.
     */
    public final TableField<PayoutRecord, String> DESCRIPTION = createField("description", org.jooq.impl.SQLDataType.VARCHAR, this, "");

    /**
     * The column <code>sht.payout.account_legal_agreement_id</code>.
     */
    public final TableField<PayoutRecord, String> ACCOUNT_LEGAL_AGREEMENT_ID = createField("account_legal_agreement_id", org.jooq.impl.SQLDataType.VARCHAR, this, "");

    /**
     * The column <code>sht.payout.account_legal_agreement_signed_at</code>.
     */
    public final TableField<PayoutRecord, LocalDateTime> ACCOUNT_LEGAL_AGREEMENT_SIGNED_AT = createField("account_legal_agreement_signed_at", org.jooq.impl.SQLDataType.LOCALDATETIME, this, "");

    /**
     * The column <code>sht.payout.account_type</code>.
     */
    public final TableField<PayoutRecord, PayoutAccountType> ACCOUNT_TYPE = createField("account_type", org.jooq.util.postgres.PostgresDataType.VARCHAR.asEnumDataType(com.rbkmoney.payouter.domain.enums.PayoutAccountType.class), this, "");

    /**
     * The column <code>sht.payout.fee</code>.
     */
    public final TableField<PayoutRecord, Long> FEE = createField("fee", org.jooq.impl.SQLDataType.BIGINT.defaultValue(org.jooq.impl.DSL.field("0", org.jooq.impl.SQLDataType.BIGINT)), this, "");

    /**
     * The column <code>sht.payout.bank_address</code>.
     */
    public final TableField<PayoutRecord, String> BANK_ADDRESS = createField("bank_address", org.jooq.impl.SQLDataType.VARCHAR, this, "");

    /**
     * The column <code>sht.payout.bank_bic</code>.
     */
    public final TableField<PayoutRecord, String> BANK_BIC = createField("bank_bic", org.jooq.impl.SQLDataType.VARCHAR, this, "");

    /**
     * The column <code>sht.payout.bank_iban</code>.
     */
    public final TableField<PayoutRecord, String> BANK_IBAN = createField("bank_iban", org.jooq.impl.SQLDataType.VARCHAR, this, "");

    /**
     * The column <code>sht.payout.account_legal_name</code>.
     */
    public final TableField<PayoutRecord, String> ACCOUNT_LEGAL_NAME = createField("account_legal_name", org.jooq.impl.SQLDataType.VARCHAR, this, "");

    /**
     * The column <code>sht.payout.account_trading_name</code>.
     */
    public final TableField<PayoutRecord, String> ACCOUNT_TRADING_NAME = createField("account_trading_name", org.jooq.impl.SQLDataType.VARCHAR, this, "");

    /**
     * The column <code>sht.payout.account_registered_address</code>.
     */
    public final TableField<PayoutRecord, String> ACCOUNT_REGISTERED_ADDRESS = createField("account_registered_address", org.jooq.impl.SQLDataType.VARCHAR, this, "");

    /**
     * The column <code>sht.payout.account_actual_address</code>.
     */
    public final TableField<PayoutRecord, String> ACCOUNT_ACTUAL_ADDRESS = createField("account_actual_address", org.jooq.impl.SQLDataType.VARCHAR, this, "");

    /**
     * The column <code>sht.payout.account_registered_number</code>.
     */
    public final TableField<PayoutRecord, String> ACCOUNT_REGISTERED_NUMBER = createField("account_registered_number", org.jooq.impl.SQLDataType.VARCHAR, this, "");

    /**
     * The column <code>sht.payout.shop_url</code>.
     */
    public final TableField<PayoutRecord, String> SHOP_URL = createField("shop_url", org.jooq.impl.SQLDataType.VARCHAR, this, "");

    /**
     * The column <code>sht.payout.contract_id</code>.
     */
    public final TableField<PayoutRecord, String> CONTRACT_ID = createField("contract_id", org.jooq.impl.SQLDataType.VARCHAR.nullable(false), this, "");

    /**
     * The column <code>sht.payout.payment_institution_id</code>.
     */
    public final TableField<PayoutRecord, Integer> PAYMENT_INSTITUTION_ID = createField("payment_institution_id", org.jooq.impl.SQLDataType.INTEGER, this, "");

    /**
     * Create a <code>sht.payout</code> table reference
     */
    public Payout() {
        this("payout", null);
    }

    /**
     * Create an aliased <code>sht.payout</code> table reference
     */
    public Payout(String alias) {
        this(alias, PAYOUT);
    }

    private Payout(String alias, Table<PayoutRecord> aliased) {
        this(alias, aliased, null);
    }

    private Payout(String alias, Table<PayoutRecord> aliased, Field<?>[] parameters) {
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
    public Identity<PayoutRecord, Long> getIdentity() {
        return Keys.IDENTITY_PAYOUT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UniqueKey<PayoutRecord> getPrimaryKey() {
        return Keys.PAYOUT_PKEY1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UniqueKey<PayoutRecord>> getKeys() {
        return Arrays.<UniqueKey<PayoutRecord>>asList(Keys.PAYOUT_PKEY1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Payout as(String alias) {
        return new Payout(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Payout rename(String name) {
        return new Payout(name, null);
    }
}
