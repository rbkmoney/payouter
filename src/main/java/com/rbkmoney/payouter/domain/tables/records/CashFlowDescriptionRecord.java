/*
 * This file is generated by jOOQ.
*/
package com.rbkmoney.payouter.domain.tables.records;


import com.rbkmoney.payouter.domain.enums.CashFlowType;
import com.rbkmoney.payouter.domain.tables.CashFlowDescription;

import java.time.LocalDateTime;

import javax.annotation.Generated;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record9;
import org.jooq.Row9;
import org.jooq.impl.UpdatableRecordImpl;


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
public class CashFlowDescriptionRecord extends UpdatableRecordImpl<CashFlowDescriptionRecord> implements Record9<Long, Long, CashFlowType, Integer, Long, Long, String, LocalDateTime, LocalDateTime> {

    private static final long serialVersionUID = -1636507776;

    /**
     * Setter for <code>sht.cash_flow_description.id</code>.
     */
    public void setId(Long value) {
        set(0, value);
    }

    /**
     * Getter for <code>sht.cash_flow_description.id</code>.
     */
    public Long getId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>sht.cash_flow_description.payout_id</code>.
     */
    public void setPayoutId(Long value) {
        set(1, value);
    }

    /**
     * Getter for <code>sht.cash_flow_description.payout_id</code>.
     */
    public Long getPayoutId() {
        return (Long) get(1);
    }

    /**
     * Setter for <code>sht.cash_flow_description.cash_flow_type</code>.
     */
    public void setCashFlowType(CashFlowType value) {
        set(2, value);
    }

    /**
     * Getter for <code>sht.cash_flow_description.cash_flow_type</code>.
     */
    public CashFlowType getCashFlowType() {
        return (CashFlowType) get(2);
    }

    /**
     * Setter for <code>sht.cash_flow_description.count</code>.
     */
    public void setCount(Integer value) {
        set(3, value);
    }

    /**
     * Getter for <code>sht.cash_flow_description.count</code>.
     */
    public Integer getCount() {
        return (Integer) get(3);
    }

    /**
     * Setter for <code>sht.cash_flow_description.amount</code>.
     */
    public void setAmount(Long value) {
        set(4, value);
    }

    /**
     * Getter for <code>sht.cash_flow_description.amount</code>.
     */
    public Long getAmount() {
        return (Long) get(4);
    }

    /**
     * Setter for <code>sht.cash_flow_description.fee</code>.
     */
    public void setFee(Long value) {
        set(5, value);
    }

    /**
     * Getter for <code>sht.cash_flow_description.fee</code>.
     */
    public Long getFee() {
        return (Long) get(5);
    }

    /**
     * Setter for <code>sht.cash_flow_description.currency_code</code>.
     */
    public void setCurrencyCode(String value) {
        set(6, value);
    }

    /**
     * Getter for <code>sht.cash_flow_description.currency_code</code>.
     */
    public String getCurrencyCode() {
        return (String) get(6);
    }

    /**
     * Setter for <code>sht.cash_flow_description.from_time</code>.
     */
    public void setFromTime(LocalDateTime value) {
        set(7, value);
    }

    /**
     * Getter for <code>sht.cash_flow_description.from_time</code>.
     */
    public LocalDateTime getFromTime() {
        return (LocalDateTime) get(7);
    }

    /**
     * Setter for <code>sht.cash_flow_description.to_time</code>.
     */
    public void setToTime(LocalDateTime value) {
        set(8, value);
    }

    /**
     * Getter for <code>sht.cash_flow_description.to_time</code>.
     */
    public LocalDateTime getToTime() {
        return (LocalDateTime) get(8);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Record1<Long> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record9 type implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Row9<Long, Long, CashFlowType, Integer, Long, Long, String, LocalDateTime, LocalDateTime> fieldsRow() {
        return (Row9) super.fieldsRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Row9<Long, Long, CashFlowType, Integer, Long, Long, String, LocalDateTime, LocalDateTime> valuesRow() {
        return (Row9) super.valuesRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field1() {
        return CashFlowDescription.CASH_FLOW_DESCRIPTION.ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field2() {
        return CashFlowDescription.CASH_FLOW_DESCRIPTION.PAYOUT_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<CashFlowType> field3() {
        return CashFlowDescription.CASH_FLOW_DESCRIPTION.CASH_FLOW_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Integer> field4() {
        return CashFlowDescription.CASH_FLOW_DESCRIPTION.COUNT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field5() {
        return CashFlowDescription.CASH_FLOW_DESCRIPTION.AMOUNT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field6() {
        return CashFlowDescription.CASH_FLOW_DESCRIPTION.FEE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field7() {
        return CashFlowDescription.CASH_FLOW_DESCRIPTION.CURRENCY_CODE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<LocalDateTime> field8() {
        return CashFlowDescription.CASH_FLOW_DESCRIPTION.FROM_TIME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<LocalDateTime> field9() {
        return CashFlowDescription.CASH_FLOW_DESCRIPTION.TO_TIME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value1() {
        return getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value2() {
        return getPayoutId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CashFlowType value3() {
        return getCashFlowType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer value4() {
        return getCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value5() {
        return getAmount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value6() {
        return getFee();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value7() {
        return getCurrencyCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalDateTime value8() {
        return getFromTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalDateTime value9() {
        return getToTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CashFlowDescriptionRecord value1(Long value) {
        setId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CashFlowDescriptionRecord value2(Long value) {
        setPayoutId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CashFlowDescriptionRecord value3(CashFlowType value) {
        setCashFlowType(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CashFlowDescriptionRecord value4(Integer value) {
        setCount(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CashFlowDescriptionRecord value5(Long value) {
        setAmount(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CashFlowDescriptionRecord value6(Long value) {
        setFee(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CashFlowDescriptionRecord value7(String value) {
        setCurrencyCode(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CashFlowDescriptionRecord value8(LocalDateTime value) {
        setFromTime(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CashFlowDescriptionRecord value9(LocalDateTime value) {
        setToTime(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CashFlowDescriptionRecord values(Long value1, Long value2, CashFlowType value3, Integer value4, Long value5, Long value6, String value7, LocalDateTime value8, LocalDateTime value9) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached CashFlowDescriptionRecord
     */
    public CashFlowDescriptionRecord() {
        super(CashFlowDescription.CASH_FLOW_DESCRIPTION);
    }

    /**
     * Create a detached, initialised CashFlowDescriptionRecord
     */
    public CashFlowDescriptionRecord(Long id, Long payoutId, CashFlowType cashFlowType, Integer count, Long amount, Long fee, String currencyCode, LocalDateTime fromTime, LocalDateTime toTime) {
        super(CashFlowDescription.CASH_FLOW_DESCRIPTION);

        set(0, id);
        set(1, payoutId);
        set(2, cashFlowType);
        set(3, count);
        set(4, amount);
        set(5, fee);
        set(6, currencyCode);
        set(7, fromTime);
        set(8, toTime);
    }
}