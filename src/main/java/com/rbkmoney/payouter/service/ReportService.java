package com.rbkmoney.payouter.service;

import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.domain.tables.pojos.Report;
import com.rbkmoney.payouter.exception.StorageException;

import java.util.List;

public interface ReportService {

    long generateAndSave(List<Payout> payouts) throws StorageException;

    long save(Report report) throws StorageException;

}
