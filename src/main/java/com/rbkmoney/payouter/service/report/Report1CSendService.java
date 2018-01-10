package com.rbkmoney.payouter.service.report;

import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.domain.tables.pojos.Report;
import com.rbkmoney.payouter.exception.ReportException;
import com.rbkmoney.payouter.service.report._1c.Report1CService;
import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class Report1CSendService {

    @Value("#{'${service.dudoser.mail.to}'.split(',')}")
    private List<String> to;

    @Value("${report.1c.file.encoding}")
    private String encoding;

    @Value("${report.1c.timezone}")
    private ZoneId zoneId;

    @Autowired
    private Report1CService report1CService;

    @Autowired
    private ReportSendService reportSendService;

    public void generateAndSend(List<Payout> payouts) throws ReportException {
        String subject = "Выплаты, сгенерированные " + DateTimeFormatter.ofPattern("dd.MM.yyyy").format(LocalDate.now(zoneId));
        try {
            Report report = report1CService.generate(payouts);
            reportSendService.sendEmail(to, subject, report, encoding);
        } catch (Exception e) {
            throw new ReportException(String.format("Couldn't send report with subject %s", subject), e);
        }
    }
}
