package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.domain.tables.pojos.JobMeta;
import com.rbkmoney.payouter.exception.DaoException;

import java.util.List;
import java.util.Map;

public interface JobMetaDao extends GenericDao {

    void save(String partyId, String contractId, String payoutToolId, int calendarId, int schedulerId) throws DaoException;

    void disableScheduler(String partyId, String contractId, String payoutToolId) throws DaoException;

    JobMeta get(String partyId, String contractId, String payoutToolId) throws DaoException;

    List<JobMeta> getByCalendarAndSchedulerId(int calendarId, int schedulerId) throws DaoException;

    List<Map.Entry<Integer, Integer>> getAllActiveJobs();
}
