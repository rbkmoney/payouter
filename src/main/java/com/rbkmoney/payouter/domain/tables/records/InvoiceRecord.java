/*
 * This file is generated by jOOQ.
*/
package com.rbkmoney.payouter.domain.tables.records;


import com.rbkmoney.payouter.domain.tables.Invoice;

import java.time.LocalDateTime;

import javax.annotation.Generated;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record6;
import org.jooq.Row6;
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
public class InvoiceRecord extends UpdatableRecordImpl<InvoiceRecord> implements Record6<String, String, String, String, LocalDateTime, Long> {

    private static final long serialVersionUID = 1285533761;

    /**
     * Setter for <code>sht.invoice.id</code>.
     */
    public void setId(String value) {
        set(0, value);
    }

    /**
     * Getter for <code>sht.invoice.id</code>.
     */
    public String getId() {
        return (String) get(0);
    }

    /**
     * Setter for <code>sht.invoice.party_id</code>.
     */
    public void setPartyId(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>sht.invoice.party_id</code>.
     */
    public String getPartyId() {
        return (String) get(1);
    }

    /**
     * Setter for <code>sht.invoice.shop_id</code>.
     */
    public void setShopId(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>sht.invoice.shop_id</code>.
     */
    public String getShopId() {
        return (String) get(2);
    }

    /**
     * Setter for <code>sht.invoice.contract_id</code>.
     */
    public void setContractId(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>sht.invoice.contract_id</code>.
     */
    public String getContractId() {
        return (String) get(3);
    }

    /**
     * Setter for <code>sht.invoice.created_at</code>.
     */
    public void setCreatedAt(LocalDateTime value) {
        set(4, value);
    }

    /**
     * Getter for <code>sht.invoice.created_at</code>.
     */
    public LocalDateTime getCreatedAt() {
        return (LocalDateTime) get(4);
    }

    /**
     * Setter for <code>sht.invoice.party_revision</code>.
     */
    public void setPartyRevision(Long value) {
        set(5, value);
    }

    /**
     * Getter for <code>sht.invoice.party_revision</code>.
     */
    public Long getPartyRevision() {
        return (Long) get(5);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Record1<String> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record6 type implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Row6<String, String, String, String, LocalDateTime, Long> fieldsRow() {
        return (Row6) super.fieldsRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Row6<String, String, String, String, LocalDateTime, Long> valuesRow() {
        return (Row6) super.valuesRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field1() {
        return Invoice.INVOICE.ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field2() {
        return Invoice.INVOICE.PARTY_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field3() {
        return Invoice.INVOICE.SHOP_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field4() {
        return Invoice.INVOICE.CONTRACT_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<LocalDateTime> field5() {
        return Invoice.INVOICE.CREATED_AT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field6() {
        return Invoice.INVOICE.PARTY_REVISION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value1() {
        return getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value2() {
        return getPartyId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value3() {
        return getShopId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value4() {
        return getContractId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalDateTime value5() {
        return getCreatedAt();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value6() {
        return getPartyRevision();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InvoiceRecord value1(String value) {
        setId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InvoiceRecord value2(String value) {
        setPartyId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InvoiceRecord value3(String value) {
        setShopId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InvoiceRecord value4(String value) {
        setContractId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InvoiceRecord value5(LocalDateTime value) {
        setCreatedAt(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InvoiceRecord value6(Long value) {
        setPartyRevision(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InvoiceRecord values(String value1, String value2, String value3, String value4, LocalDateTime value5, Long value6) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached InvoiceRecord
     */
    public InvoiceRecord() {
        super(Invoice.INVOICE);
    }

    /**
     * Create a detached, initialised InvoiceRecord
     */
    public InvoiceRecord(String id, String partyId, String shopId, String contractId, LocalDateTime createdAt, Long partyRevision) {
        super(Invoice.INVOICE);

        set(0, id);
        set(1, partyId);
        set(2, shopId);
        set(3, contractId);
        set(4, createdAt);
        set(5, partyRevision);
    }
}
