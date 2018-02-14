/*
 * This file is generated by jOOQ.
*/
package com.rbkmoney.payouter.domain.tables;


import com.rbkmoney.payouter.domain.Keys;
import com.rbkmoney.payouter.domain.Sht;
import com.rbkmoney.payouter.domain.enums.CashFlowType;
import com.rbkmoney.payouter.domain.tables.records.CashFlowDescriptionRecord;

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
public class CashFlowDescription extends TableImpl<CashFlowDescriptionRecord> {

    private static final long serialVersionUID = -1579352372;

    /**
     * The reference instance of <code>sht.cash_flow_description</code>
     */
    public static final CashFlowDescription CASH_FLOW_DESCRIPTION = new CashFlowDescription();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<CashFlowDescriptionRecord> getRecordType() {
        return CashFlowDescriptionRecord.class;
    }

    /**
     * The column <code>sht.cash_flow_description.id</code>.
     */
    public final TableField<CashFlowDescriptionRecord, Long> ID = createField("id", org.jooq.impl.SQLDataType.BIGINT.nullable(false).defaultValue(org.jooq.impl.DSL.field("nextval('sht.cash_flow_description_id_seq'::regclass)", org.jooq.impl.SQLDataType.BIGINT)), this, "");

    /**
     * The column <code>sht.cash_flow_description.payout_id</code>.
     */
    public final TableField<CashFlowDescriptionRecord, Long> PAYOUT_ID = createField("payout_id", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>sht.cash_flow_description.cash_flow_type</code>.
     */
    public final TableField<CashFlowDescriptionRecord, CashFlowType> CASH_FLOW_TYPE = createField("cash_flow_type", org.jooq.util.postgres.PostgresDataType.VARCHAR.asEnumDataType(com.rbkmoney.payouter.domain.enums.CashFlowType.class), this, "");

    /**
     * The column <code>sht.cash_flow_description.count</code>.
     */
    public final TableField<CashFlowDescriptionRecord, Integer> COUNT = createField("count", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>sht.cash_flow_description.amount</code>.
     */
    public final TableField<CashFlowDescriptionRecord, Long> AMOUNT = createField("amount", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>sht.cash_flow_description.fee</code>.
     */
    public final TableField<CashFlowDescriptionRecord, Long> FEE = createField("fee", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>sht.cash_flow_description.currency_code</code>.
     */
    public final TableField<CashFlowDescriptionRecord, String> CURRENCY_CODE = createField("currency_code", org.jooq.impl.SQLDataType.VARCHAR.nullable(false), this, "");

    /**
     * The column <code>sht.cash_flow_description.from_time</code>.
     */
    public final TableField<CashFlowDescriptionRecord, LocalDateTime> FROM_TIME = createField("from_time", org.jooq.impl.SQLDataType.LOCALDATETIME.nullable(false), this, "");

    /**
     * The column <code>sht.cash_flow_description.to_time</code>.
     */
    public final TableField<CashFlowDescriptionRecord, LocalDateTime> TO_TIME = createField("to_time", org.jooq.impl.SQLDataType.LOCALDATETIME.nullable(false), this, "");

    /**
     * Create a <code>sht.cash_flow_description</code> table reference
     */
    public CashFlowDescription() {
        this("cash_flow_description", null);
    }

    /**
     * Create an aliased <code>sht.cash_flow_description</code> table reference
     */
    public CashFlowDescription(String alias) {
        this(alias, CASH_FLOW_DESCRIPTION);
    }

    private CashFlowDescription(String alias, Table<CashFlowDescriptionRecord> aliased) {
        this(alias, aliased, null);
    }

    private CashFlowDescription(String alias, Table<CashFlowDescriptionRecord> aliased, Field<?>[] parameters) {
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
    public Identity<CashFlowDescriptionRecord, Long> getIdentity() {
        return Keys.IDENTITY_CASH_FLOW_DESCRIPTION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UniqueKey<CashFlowDescriptionRecord> getPrimaryKey() {
        return Keys.CASH_FLOW_DESCRIPTION_PKEY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UniqueKey<CashFlowDescriptionRecord>> getKeys() {
        return Arrays.<UniqueKey<CashFlowDescriptionRecord>>asList(Keys.CASH_FLOW_DESCRIPTION_PKEY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CashFlowDescription as(String alias) {
        return new CashFlowDescription(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public CashFlowDescription rename(String name) {
        return new CashFlowDescription(name, null);
    }
}
