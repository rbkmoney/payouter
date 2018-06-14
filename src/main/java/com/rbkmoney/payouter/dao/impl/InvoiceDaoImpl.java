package com.rbkmoney.payouter.dao.impl;

import com.rbkmoney.payouter.dao.InvoiceDao;
import com.rbkmoney.payouter.dao.mapper.RecordRowMapper;
import com.rbkmoney.payouter.domain.tables.pojos.Invoice;
import com.rbkmoney.payouter.exception.DaoException;
import org.jooq.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDateTime;

import static com.rbkmoney.payouter.domain.tables.Invoice.INVOICE;

@Component
public class InvoiceDaoImpl extends AbstractGenericDao implements InvoiceDao {

    private final RowMapper<Invoice> invoiceRowMapper;

    @Autowired
    public InvoiceDaoImpl(DataSource dataSource) {
        super(dataSource);
        invoiceRowMapper = new RecordRowMapper<>(INVOICE, Invoice.class);
    }

    @Override
    public void save(String invoiceId, String partyId, String shopId, String contractId, Long partyRevision, LocalDateTime createdAt) throws DaoException {
        Query query = getDslContext().insertInto(INVOICE)
                .set(INVOICE.ID, invoiceId)
                .set(INVOICE.PARTY_ID, partyId)
                .set(INVOICE.SHOP_ID, shopId)
                .set(INVOICE.CONTRACT_ID, contractId)
                .set(INVOICE.PARTY_REVISION, partyRevision)
                .set(INVOICE.CREATED_AT, createdAt)
                .onDuplicateKeyUpdate()
                .set(INVOICE.CONTRACT_ID, contractId)
                .set(INVOICE.PARTY_REVISION, partyRevision)
                .set(INVOICE.CREATED_AT, createdAt);

        execute(query);
    }

    @Override
    public Invoice get(String invoiceId) throws DaoException {
        Query query = getDslContext().selectFrom(INVOICE)
                .where(INVOICE.ID.eq(invoiceId));
        return fetchOne(query, invoiceRowMapper);
    }

}
