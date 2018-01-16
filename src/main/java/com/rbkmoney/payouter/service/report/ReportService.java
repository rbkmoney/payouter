package com.rbkmoney.payouter.service.report;

import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.domain.tables.pojos.Report;

import java.util.List;

public interface ReportService {
    Report generate(List<Payout> payoutRecords);
}
