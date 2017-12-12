package com.rbkmoney.payouter.dao.impl;

import com.rbkmoney.payouter.dao.ReportDao;
import com.rbkmoney.payouter.domain.tables.pojos.Report;
import com.rbkmoney.payouter.exception.DaoException;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

import static com.rbkmoney.payouter.domain.Tables.REPORT;

@Component
@DependsOn("dbInitializer")
public class ReportDaoPg extends AbstractGenericDao implements ReportDao {

    @Autowired
    DSLContext dslContext;

    public ReportDaoPg(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void save(Report report) throws DaoException {
        Query query = getDslContext().insertInto(REPORT)
                .set(getDslContext().newRecord(REPORT, report));

        executeOne(query);
    }
}
