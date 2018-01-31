package com.rbkmoney.payouter.dao.impl;

import com.rbkmoney.payouter.dao.CashFlowPostingDao;
import com.rbkmoney.payouter.dao.mapper.RecordRowMapper;
import com.rbkmoney.payouter.domain.tables.pojos.CashFlowPosting;
import com.rbkmoney.payouter.exception.DaoException;
import org.jooq.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;

import static com.rbkmoney.payouter.domain.tables.CashFlowPosting.CASH_FLOW_POSTING;

@Component
public class CashFlowPostingDaoImpl extends AbstractGenericDao implements CashFlowPostingDao {

    private final RowMapper<CashFlowPosting> cashFlowPostingRowMapper;

    @Autowired
    public CashFlowPostingDaoImpl(DataSource dataSource) {
        super(dataSource);
        cashFlowPostingRowMapper = new RecordRowMapper<>(CASH_FLOW_POSTING, CashFlowPosting.class);
    }

    @Override
    public void save(List<CashFlowPosting> cashFlowPostings) throws DaoException {
        //todo: Batch insert
        for (CashFlowPosting cashFlowPosting : cashFlowPostings) {
            Query query = getDslContext().insertInto(CASH_FLOW_POSTING)
                    .set(getDslContext().newRecord(CASH_FLOW_POSTING, cashFlowPosting));
            executeOne(query);
        }
    }

    @Override
    public List<CashFlowPosting> getByPayoutId(long payoutId) throws DaoException {
        Query query = getDslContext().selectFrom(CASH_FLOW_POSTING)
                .where(CASH_FLOW_POSTING.PAYOUT_ID.eq(payoutId));

        return fetch(query, cashFlowPostingRowMapper);
    }
}
