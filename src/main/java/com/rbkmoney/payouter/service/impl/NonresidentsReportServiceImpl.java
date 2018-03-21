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
import com.rbkmoney.payouter.util.FormatUtil;
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

import static com.opencsv.CSVWriter.DEFAULT_ESCAPE_CHARACTER;
import static com.opencsv.CSVWriter.DEFAULT_LINE_END;
import static com.opencsv.CSVWriter.DEFAULT_QUOTE_CHARACTER;

@Service
public class NonresidentsReportServiceImpl implements ReportService {

    public final static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public final static String[] headerRow = {
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

    private final NonResidentsMailContentServiceImpl nonResidentsMailContentService;

    private final PayoutService payoutService;

    @Value("${report.nonresidents.file.name.prefix}")
    private String prefix;

    @Value("${report.nonresidents.file.name.extension}")
    private String extension;

    @Value("${report.nonresidents.file.delimiter}")
    private char delimiter;

    @Value("${report.nonresidents.file.encoding}")
    private String encoding;

    @Value("${report.nonresidents.timezone}")
    private ZoneId zoneId;

    @Autowired
    public NonresidentsReportServiceImpl(ReportDao reportDao, NonResidentsMailContentServiceImpl nonResidentsMailContentService, PayoutService payoutService) {
        this.reportDao = reportDao;
        this.nonResidentsMailContentService = nonResidentsMailContentService;
        this.payoutService = payoutService;
    }

    @Scheduled(cron = "${report.nonresidents.cron}", zone = "${report.nonresidents.timezone}")
    @Transactional(propagation = Propagation.REQUIRED)
    public void createNewReportsJob() throws StorageException {
        log.info("Report job for nonresidents starting");
        try {
            List<Payout> payouts = payoutService.getUnpaidPayoutsByAccountType(PayoutAccountType.international_payout_account);

            if (!payouts.isEmpty()) {
                generateAndSave(payouts);
                payouts.forEach(payout -> payoutService.pay(payout.getId()));
            }
        } finally {
            log.info("Report job for nonresidents ending");
        }
    }

    @Override
    public long generateAndSave(List<Payout> payouts) {
        log.info("Trying to generate and save report for nonresidents, payouts='%s'", payouts);
        String reportContent;
        try (StringWriter stringWriter = new StringWriter()) {
            try (CSVWriter csvWriter = new CSVWriter(
                    stringWriter,
                    delimiter,
                    DEFAULT_QUOTE_CHARACTER,
                    DEFAULT_ESCAPE_CHARACTER,
                    DEFAULT_LINE_END
            )) {
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
        report.setSubject("Выплаты для нерезидентов, сгенерированные " + createdAtFormatted);
        report.setDescription(nonResidentsMailContentService.generateContent(payouts));
        report.setPayoutIds(String.join(",", payoutIds));
        report.setStatus(ReportStatus.READY);
        report.setContent(reportContent);
        report.setEncoding(encoding);
        report.setCreatedAt(createdAt);
        log.info("Report for nonresidents have been successfully generated, report='{}', payouts='{}'", report, payouts);

        return save(report);
    }

    private List<String[]> buildRows(List<Payout> payouts) {
        return payouts.stream().map(
                payout -> new String[]{
                        payout.getPartyId(),
                        payout.getShopId(),
                        String.valueOf(payout.getId()),
                        FormatUtil.getFormattedAmount(payout.getAmount() + payout.getFee()),
                        FormatUtil.getFormattedAmount(payout.getFee()),
                        FormatUtil.getFormattedAmount(payout.getAmount()),
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
