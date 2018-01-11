package com.rbkmoney.payouter.service.report;

import com.rbkmoney.payouter.dao.ReportDao;
import com.rbkmoney.payouter.domain.enums.ReportStatus;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.domain.tables.pojos.Report;
import com.rbkmoney.payouter.exception.ReportException;
import com.rbkmoney.payouter.service.report._1c.Report1CService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
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
    private ReportDao reportDao;

    @Autowired
    private Report1CService report1CService;

    @Autowired
    private ReportSendService reportSendService;

    public void generate(List<Payout> payouts) throws ReportException {
        try {
            report1CService.generate(payouts);
        } catch (Exception e) {
            throw new ReportException(String.format("Couldn't generate report"), e);
        }
    }

    public void send(List<Report> reports) throws ReportException {
        for (Report report : reports) {
            String subject = "Выплаты, сгенерированные " + DateTimeFormatter.ofPattern("dd.MM.yyyy").format(report.getCreatedAt().atZone(zoneId));
            try {
                reportSendService.sendEmail(to, subject, report, encoding);
                reportDao.changeStatus(report.getId(), ReportStatus.SENT, LocalDateTime.now(ZoneOffset.UTC));
            } catch (Exception e) {
                reportDao.changeStatus(report.getId(), ReportStatus.FAILED, LocalDateTime.now(ZoneOffset.UTC));
                throw new ReportException(String.format("Couldn't send report with subject %s", subject), e);
            }
        }
    }
}
