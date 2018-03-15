package com.rbkmoney.payouter.dao.impl;

import com.rbkmoney.payouter.dao.PayoutSummaryDao;
import com.rbkmoney.payouter.dao.mapper.RecordRowMapper;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutSummary;
import com.rbkmoney.payouter.domain.tables.records.PayoutSummaryRecord;
import com.rbkmoney.payouter.exception.DaoException;
import org.jooq.InsertSetMoreStep;
import org.jooq.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;

import static com.rbkmoney.payouter.domain.Tables.PAYOUT_SUMMARY;

@Component
public class PayoutSummaryDaoImpl extends AbstractGenericDao implements PayoutSummaryDao {

    private final RowMapper<PayoutSummary> payoutSummaryRowMapper;

    @Autowired
    public PayoutSummaryDaoImpl(DataSource dataSource) {
        super(dataSource);
        payoutSummaryRowMapper = new RecordRowMapper<>(PAYOUT_SUMMARY, PayoutSummary.class);
    }

    @Override
    public void save(List<PayoutSummary> payoutSummaries) throws DaoException {
        if (payoutSummaries.isEmpty()) return;
        InsertSetMoreStep<PayoutSummaryRecord> query = getDslContext()
                .insertInto(PAYOUT_SUMMARY)
                .set(getDslContext().newRecord(PAYOUT_SUMMARY, payoutSummaries.get(0)));

        payoutSummaries.stream().skip(1).forEach(cfd -> query.newRecord().set(getDslContext().newRecord(PAYOUT_SUMMARY, cfd)));
        execute(query);
    }

    @Override
    public List<PayoutSummary> get(String payoutId) throws DaoException {
        Query query = getDslContext().selectFrom(PAYOUT_SUMMARY)
                .where(PAYOUT_SUMMARY.PAYOUT_ID.eq(payoutId));
        return fetch(query, payoutSummaryRowMapper);
    }
}