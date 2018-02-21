package com.rbkmoney.payouter.service.impl;

import com.opencsv.CSVWriter;
import com.rbkmoney.payouter.dao.ReportDao;
import com.rbkmoney.payouter.domain.enums.PayoutAccountType;
import com.rbkmoney.payouter.domain.enums.ReportStatus;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.domain.tables.pojos.Report;
import com.rbkmoney.payouter.exception.DaoException;
import com.rbkmoney.payouter.exception.StorageException;
import com.rbkmoney.payouter.service.PayoutService;
import com.rbkmoney.payouter.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NonresidentsReportServiceImpl implements ReportService {

    public static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public static String[] headerRow = {
            "Id участника",
            "Id магазина",
            "Id вывода",
            "Сумма списания",
            "Комиссия за вывод",
            "Сумма получения",
            "Валюта",
            "Курс",
            "Наименование юридического лица",
            "Адрес юридического лица",
            "Регистрационный номер",
            "Счет получателя",
            "SWIFT банка получателя",
            "Название банка получателя",
            "Адрес банка получателя",
            "Национальный код банка",
            "Номер договора",
            "Дата договора",
            "Назначение платежа"
    };

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ReportDao reportDao;

    private final PayoutService payoutService;

    @Value("${report.nonresidents.file.name.prefix}")
    private String prefix;

    @Value("${report.nonresidents.file.name.extension}")
    private String extension;

    @Value("${report.nonresidents.templateFileName}")
    private String templateFileName;

    @Value("${report.nonresidents.timezone}")
    private ZoneId zoneId;

    @Autowired
    public NonresidentsReportServiceImpl(ReportDao reportDao, PayoutService payoutService) {
        this.reportDao = reportDao;
        this.payoutService = payoutService;
    }

    @Override
    @Scheduled(cron = "${report.nonresidents.cron}", zone = "${report.nonresidents.timezone}")
    @Transactional(propagation = Propagation.REQUIRED)
    public long generateAndSave() throws StorageException {
        List<Payout> payouts = payoutService.getUnpaidPayoutsByAccountType(PayoutAccountType.international_payout_account);
        payouts.forEach(payout -> payoutService.pay(payout.getId()));

        return generateAndSave(payouts);
    }

    @Override
    public long generateAndSave(List<Payout> payouts) {
        String reportContent;
        try (StringWriter stringWriter = new StringWriter()) {
            try (CSVWriter csvWriter = new CSVWriter(stringWriter)) {

                csvWriter.writeNext(headerRow);
                csvWriter.writeAll(buildRows(payouts));
                reportContent = stringWriter.getBuffer().toString();
            }
        } catch (IOException ex) {
            throw new RuntimeException(
                    String.format("Failed to generate report content for residents, payouts='%s'", payouts),
                    ex
            );
        }
        LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);
        String createdAtFormatted = LocalDateTime.now(zoneId).format(dateTimeFormatter);
        List<String> payoutIds = payouts.stream().map(p -> String.valueOf(p.getId())).collect(Collectors.toList());
        Report report = new Report();
        report.setName(prefix + "_" + createdAtFormatted + extension);
        report.setDescription(buildDescription(payouts));
        report.setPayoutIds(String.join(",", payoutIds));
        report.setStatus(ReportStatus.READY);
        report.setContent(reportContent);
        report.setCreatedAt(createdAt);

        return save(report);
    }

    private String buildDescription(List<Payout> payouts) {
        StringBuilder stringBuilder = new StringBuilder("Выплаты для нерезидентов: <br>");
        payouts.forEach(payout ->
                stringBuilder
                        .append(payout.getAccountLegalName())
                        .append(": ")
                        .append(BigDecimal.valueOf(payout.getAmount()).movePointLeft(2))
                        .append(", комиссия ")
                        .append(BigDecimal.valueOf(payout.getFee()).movePointLeft(2))
                        .append(" <br> ")
        );
        return stringBuilder.toString();
    }

    private List<String[]> buildRows(List<Payout> payouts) {
        return payouts.stream().map(
                payout -> new String[]{
                        payout.getPartyId(),
                        payout.getShopId(),
                        String.valueOf(payout.getId()),
                        String.valueOf(payout.getAmount() + payout.getFee()),
                        String.valueOf(payout.getFee()),
                        String.valueOf(payout.getAmount()),
                        payout.getCurrencyCode(),
                        "",
                        payout.getAccountLegalName(),
                        payout.getAccountRegisteredAddress(),
                        payout.getAccountRegisteredNumber(),
                        payout.getBankIban(),
                        payout.getBankBic(),
                        payout.getBankName(),
                        payout.getBankAddress(),
                        payout.getBankLocalCode(),
                        payout.getAccountLegalAgreementId(),
                        payout.getAccountLegalAgreementSignedAt().format(dateTimeFormatter),
                        payout.getPurpose()

                }
        ).collect(Collectors.toList());
    }

    @Override
    public long save(Report report) {
        log.info("Trying to save report for nonresidents, payoutIds='{}'", report.getPayoutIds());
        try {
            long reportId = reportDao.save(report);
            log.info("Report for nonresidents have been successfully saved, reportId='{}', payoutIds='{}'", reportId, report.getPayoutIds());
            return reportId;
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to save report for nonresidents, payoutIds='%s'", report.getPayoutIds()), ex);
        }
    }
}
