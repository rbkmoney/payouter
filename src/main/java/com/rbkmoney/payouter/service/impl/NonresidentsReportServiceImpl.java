package com.rbkmoney.payouter.service.impl;

import com.opencsv.CSVWriter;
import com.rbkmoney.damsel.domain.CalendarRef;
import com.rbkmoney.payouter.dao.PaymentDao;
import com.rbkmoney.payouter.dao.ReportDao;
import com.rbkmoney.payouter.domain.enums.PayoutAccountType;
import com.rbkmoney.payouter.domain.enums.ReportStatus;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.domain.tables.pojos.Report;
import com.rbkmoney.payouter.exception.DaoException;
import com.rbkmoney.payouter.exception.StorageException;
import com.rbkmoney.payouter.service.DominantService;
import com.rbkmoney.payouter.service.PayoutService;
import com.rbkmoney.payouter.service.ReportService;
import com.rbkmoney.payouter.util.FormatUtil;
import com.rbkmoney.payouter.util.SchedulerUtil;
import org.quartz.impl.calendar.HolidayCalendar;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.opencsv.CSVWriter.*;

@Service
public class NonresidentsReportServiceImpl implements ReportService {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static final String[] headerRow = {
            "Id участника",
            "Id магазина",
            "Наименование юридического лица",
            "URL магазина",
            "Дата создания заявки на вывод в системе",
            "Id вывода",
            "Валюта",
            "Сумма списания",
            "Комиссия за вывод",
            "Сумма получения",
            "Курс",
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

    private final PaymentDao paymentDao;

    private final NonResidentsMailContentServiceImpl nonResidentsMailContentService;

    private final PayoutService payoutService;

    private final DominantService dominantService;

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

    @Value("${report.nonresidents.calendar}")
    private int calendarId;

    @Autowired
    public NonresidentsReportServiceImpl(ReportDao reportDao,
                                         PaymentDao paymentDao,
                                         NonResidentsMailContentServiceImpl nonResidentsMailContentService,
                                         PayoutService payoutService,
                                         DominantService dominantService) {
        this.reportDao = reportDao;
        this.paymentDao = paymentDao;
        this.nonResidentsMailContentService = nonResidentsMailContentService;
        this.payoutService = payoutService;
        this.dominantService = dominantService;
    }

    @Scheduled(cron = "${report.nonresidents.cron}", zone = "${report.nonresidents.timezone}")
    @Transactional(propagation = Propagation.REQUIRED)
    public void createNewReportsJob() throws StorageException {
        log.info("Report job for nonresidents starting");
        try {
            var holidayCalendar = SchedulerUtil.buildCalendar(dominantService.getCalendar(new CalendarRef(calendarId)));
            if (holidayCalendar.isTimeIncluded(Instant.now().toEpochMilli())) {
                var groupedPayoutsMap =
                        payoutService.getUnpaidPayoutsByAccountType(PayoutAccountType.international_payout_account)
                        .stream().collect(Collectors.groupingBy(p -> Optional.ofNullable(p.getPaymentInstitutionId())));

                groupedPayoutsMap.values().forEach(payouts -> {
                    if (!payouts.isEmpty()) {
                        generateAndSave(payouts);
                        payouts.forEach(payout -> payoutService.pay(payout.getPayoutId()));
                    }
                });
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
        List<String> payoutIds = payouts.stream().map(p -> p.getPayoutId()).collect(Collectors.toList());
        Report report = new Report();
        report.setName(prefix + "_" + createdAtFormatted + extension);
        report.setSubject(String.format("Выплаты для нерезидентов, сгенерированные %s (%d)",
                createdAtFormatted, payouts.get(0).getPaymentInstitutionId()));
        report.setDescription(nonResidentsMailContentService.generateContent(payouts));
        report.setPayoutIds(String.join(",", payoutIds));
        report.setStatus(ReportStatus.READY);
        report.setContent(reportContent);
        report.setEncoding(encoding);
        report.setCreatedAt(createdAt);
        log.info("Report for nonresidents have been successfully generated, " +
                "report='{}', payouts='{}'", report, payouts);

        return save(report);
    }

    private List<String[]> buildRows(List<Payout> payouts) {
        return payouts.stream().map(
                payout -> {
                    return new String[]{
                            payout.getPartyId(),
                            payout.getShopId(),
                            payout.getAccountLegalName(),
                            payout.getShopUrl(),
                            payout.getCreatedAt().format(dateTimeFormatter),
                            payout.getPayoutId(),
                            payout.getCurrencyCode(),
                            FormatUtil.getFormattedAmount(payout.getAmount() + payout.getFee()),
                            FormatUtil.getFormattedAmount(payout.getFee()),
                            FormatUtil.getFormattedAmount(payout.getAmount()),
                            "",
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

                    };
                }
        ).collect(Collectors.toList());
    }

    @Override
    public long save(Report report) {
        log.info("Trying to save report for nonresidents, payoutIds='{}'", report.getPayoutIds());
        try {
            long reportId = reportDao.save(report);
            log.info("Report for nonresidents have been successfully saved, " +
                    "reportId='{}', payoutIds='{}'", reportId, report.getPayoutIds());
            return reportId;
        } catch (DaoException ex) {
            throw new StorageException(
                    String.format("Failed to save report for nonresidents, payoutIds='%s'", report.getPayoutIds()), ex);
        }
    }
}
