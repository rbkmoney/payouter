package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.domain.tables.pojos.Report;
import com.rbkmoney.payouter.exception.DaoException;

public interface ReportDao {
    void save(Report report) throws DaoException;
}
