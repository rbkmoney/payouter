package com.rbkmoney.payouter.dao.impl;

import com.rbkmoney.payouter.dao.CashFlowDescriptionDao;
import com.rbkmoney.payouter.dao.mapper.RecordRowMapper;
import com.rbkmoney.payouter.domain.tables.pojos.CashFlowDescription;
import com.rbkmoney.payouter.domain.tables.records.CashFlowDescriptionRecord;
import com.rbkmoney.payouter.exception.DaoException;
import org.jooq.InsertSetMoreStep;
import org.jooq.InsertSetStep;
import org.jooq.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

import java.util.List;

import static com.rbkmoney.payouter.domain.Tables.CASH_FLOW_DESCRIPTION;

@Component
public class CashFlowDescriptionDaoImpl extends AbstractGenericDao implements CashFlowDescriptionDao {

    private final RowMapper<CashFlowDescription> cashFlowDescriptionRowMapper;

    @Autowired
    public CashFlowDescriptionDaoImpl(DataSource dataSource) {
        super(dataSource);
        cashFlowDescriptionRowMapper = new RecordRowMapper<>(CASH_FLOW_DESCRIPTION, CashFlowDescription.class);
    }

    @Override
    public void save(List<CashFlowDescription> cashFlowDescription) throws DaoException {
        if (cashFlowDescription.isEmpty()) return;
        InsertSetMoreStep<CashFlowDescriptionRecord> query = getDslContext()
                .insertInto(CASH_FLOW_DESCRIPTION)
                .set(getDslContext().newRecord(CASH_FLOW_DESCRIPTION, cashFlowDescription.get(0)));

        cashFlowDescription.stream().skip(1).forEach(cfd -> query.newRecord().set(getDslContext().newRecord(CASH_FLOW_DESCRIPTION, cfd)));
        execute(query);
    }

    @Override
    public List<CashFlowDescription> get(long payoutId) throws DaoException {
        Query query = getDslContext().selectFrom(CASH_FLOW_DESCRIPTION)
                .where(CASH_FLOW_DESCRIPTION.PAYOUT_ID.eq(payoutId));
        return fetch(query, cashFlowDescriptionRowMapper);
    }
}
