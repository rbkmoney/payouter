package com.rbkmoney.payouter.dao.impl;

import com.rbkmoney.payouter.dao.ReportDao;
import com.rbkmoney.payouter.dao.mapper.RecordRowMapper;
import com.rbkmoney.payouter.domain.enums.ReportStatus;
import com.rbkmoney.payouter.domain.tables.pojos.Report;
import com.rbkmoney.payouter.exception.DaoException;
import org.jooq.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;

import static com.rbkmoney.payouter.domain.Tables.REPORT;

@Component
public class ReportDaoImpl extends AbstractGenericDao implements ReportDao {

    private final RowMapper<Report> reportRowMapper;

    @Autowired
    public ReportDaoImpl(DataSource dataSource) {
        super(dataSource);
        reportRowMapper = new RecordRowMapper<>(REPORT, Report.class);
    }

    @Override
    public Report get(long reportId) throws DaoException {
        Query query = getDslContext().selectFrom(REPORT)
                .where(REPORT.ID.eq(reportId));

        return fetchOne(query, reportRowMapper);
    }

    @Override
    public long save(Report report) throws DaoException {
        Query query = getDslContext().insertInto(REPORT)
                .set(getDslContext().newRecord(REPORT, report))
                .returning(REPORT.ID);

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        executeOneWithReturn(query, keyHolder);

        return keyHolder.getKey().longValue();
    }

    @Override
    public List<Report> getForSend() throws DaoException {
        Query query = getDslContext().selectFrom(REPORT)
                .where(REPORT.STATUS.in(ReportStatus.READY, ReportStatus.FAILED))
                .forUpdate();

        return fetch(query, reportRowMapper);
    }

    @Override
    public void changeStatus(long reportId, ReportStatus reportStatus, LocalDateTime sendTime) throws DaoException {
        Query query = getDslContext().update(REPORT)
                .set(REPORT.STATUS, reportStatus)
                .set(REPORT.LAST_SEND_AT, sendTime)
                .where(REPORT.ID.eq(reportId));

        executeOne(query);
    }
}
