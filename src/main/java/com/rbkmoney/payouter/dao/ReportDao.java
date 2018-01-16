package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.domain.enums.ReportStatus;
import com.rbkmoney.payouter.domain.tables.pojos.Report;
import com.rbkmoney.payouter.exception.DaoException;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportDao {
    void save(Report report) throws DaoException;
    List<Report> getForSend() throws DaoException;
    void changeStatus(long reportId, ReportStatus reportStatus, LocalDateTime sendTime) throws DaoException;
}
