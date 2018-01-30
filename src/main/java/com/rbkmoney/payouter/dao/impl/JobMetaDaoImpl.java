package com.rbkmoney.payouter.dao.impl;

import com.rbkmoney.payouter.dao.JobMetaDao;
import com.rbkmoney.payouter.dao.mapper.RecordRowMapper;
import com.rbkmoney.payouter.domain.tables.pojos.JobMeta;
import com.rbkmoney.payouter.exception.DaoException;
import org.jooq.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

import static com.rbkmoney.payouter.domain.tables.JobMeta.JOB_META;

@Component
public class JobMetaDaoImpl extends AbstractGenericDao implements JobMetaDao {

    private final RowMapper<JobMeta> jobMetaRowMapper;

    @Autowired
    public JobMetaDaoImpl(DataSource dataSource) {
        super(dataSource);
        jobMetaRowMapper = new RecordRowMapper<>(JOB_META, JobMeta.class);
    }

    @Override
    public void save(String partyId, String contractId, String payoutToolId, int calendarId, int schedulerId) throws DaoException {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        Query query = getDslContext().insertInto(JOB_META)
                .set(JOB_META.PARTY_ID, partyId)
                .set(JOB_META.CONTRACT_ID, contractId)
                .set(JOB_META.PAYOUT_TOOL_ID, payoutToolId)
                .set(JOB_META.SCHEDULER_ID, schedulerId)
                .set(JOB_META.WTIME, now)
                .onDuplicateKeyUpdate()
                .set(JOB_META.CALENDAR_ID, calendarId)
                .set(JOB_META.SCHEDULER_ID, schedulerId)
                .set(JOB_META.WTIME, now);

        executeOne(query);
    }

    @Override
    public void disableScheduler(String partyId, String contractId, String payoutToolId) throws DaoException {
        Query query = getDslContext().update(JOB_META)
                .set(JOB_META.SCHEDULER_ID, (Integer) null)
                .where(JOB_META.PARTY_ID.eq(partyId)
                        .and(JOB_META.CONTRACT_ID.eq(contractId))
                        .and(JOB_META.PAYOUT_TOOL_ID.eq(payoutToolId)));

        executeOne(query);
    }

    @Override
    public JobMeta get(String partyId, String contractId, String payoutToolId) throws DaoException {
        Query query = getDslContext().selectFrom(JOB_META)
                .where(JOB_META.PARTY_ID.eq(partyId)
                        .and(JOB_META.CONTRACT_ID.eq(contractId))
                        .and(JOB_META.PAYOUT_TOOL_ID.eq(payoutToolId)));
        return fetchOne(query, jobMetaRowMapper);
    }

    @Override
    public List<JobMeta> getByCalendarAndSchedulerId(int calendarId, int schedulerId) throws DaoException {
        Query query = getDslContext().selectFrom(JOB_META)
                .where(
                        JOB_META.CALENDAR_ID.eq(calendarId)
                                .and(JOB_META.SCHEDULER_ID.eq(schedulerId))
                );

        return fetch(query, jobMetaRowMapper);
    }

    @Override
    public List<Map.Entry<Integer, Integer>> getAllActiveJobs() {
        Query query = getDslContext().select(JOB_META.CALENDAR_ID, JOB_META.SCHEDULER_ID)
                .from(JOB_META)
                .where(JOB_META.SCHEDULER_ID.isNotNull())
                .groupBy(JOB_META.CALENDAR_ID, JOB_META.SCHEDULER_ID);

        return fetch(query, (row, i) -> new AbstractMap.SimpleEntry<>(
                row.getInt(JOB_META.CALENDAR_ID.getName()),
                row.getInt(JOB_META.SCHEDULER_ID.getName())
        ));
    }
}
