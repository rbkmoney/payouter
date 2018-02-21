package com.rbkmoney.payouter.service;

import com.rbkmoney.payouter.domain.tables.pojos.Report;

public interface ReportSendService {

    void sendUnsentReports();

    void sendReport(Report report);

}
