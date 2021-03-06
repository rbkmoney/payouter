package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.damsel.domain.CalendarRef;
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
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResidentsReportServiceImpl implements ReportService {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final ReportDao reportDao;

    private final ResidentsMailContentServiceImpl residentsMailContentService;

    private final PayoutService payoutService;

    private final FreeMarkerConfigurer freeMarkerConfigurer;

    private final DominantService dominantService;

    @Value("${report.residents.file.name.prefix}")
    private String prefix;

    @Value("${report.residents.file.name.extension}")
    private String extension;

    @Value("${report.residents.reportTemplateFileName}")
    private String reportTemplateFileName;

    @Value("${report.residents.mailTemplateFileName}")
    private String mailTemplateFileName;

    @Value("${report.residents.file.encoding}")
    private String encoding;

    @Value("${report.residents.timezone}")
    private ZoneId zoneId;

    @Value("${report.residents.calendar}")
    private int calendarId;

    @Scheduled(cron = "${report.residents.cron}", zone = "${report.residents.timezone}")
    @Transactional(propagation = Propagation.REQUIRED)
    public void createNewReportsJob() throws StorageException {
        log.info("Report job for residents starting");
        try {
            var holidayCalendar = SchedulerUtil.buildCalendar(dominantService.getCalendar(new CalendarRef(calendarId)));
            if (holidayCalendar.isTimeIncluded(Instant.now().toEpochMilli())) {
                var groupedPayoutsMap =
                        payoutService.getUnpaidPayoutsByAccountType(PayoutAccountType.russian_payout_account).stream()
                                .collect(Collectors.groupingBy(p -> Optional.ofNullable(p.getPaymentInstitutionId())));

                groupedPayoutsMap.values().forEach(payouts -> {
                    if (!payouts.isEmpty()) {
                        generateAndSave(payouts);
                        payouts.forEach(payout -> payoutService.pay(payout.getPayoutId()));
                    }
                });
            }
        } finally {
            log.info("Report job for residents ending");
        }
    }

    @Override
    public long generateAndSave(List<Payout> payouts) throws StorageException {
        log.info("Trying to generate and save report for residents, payouts='{}'", payouts);
        final List<Map<String, Object>> payoutsAttributes = new ArrayList<>();
        for (Payout payout : payouts) {
            Map<String, Object> payoutData = new HashMap<>();

            payoutData.put("corr_account", payout.getBankPostAccount());
            payoutData.put("bik", payout.getBankLocalCode());
            payoutData.put("calc_account", payout.getBankAccount());
            payoutData.put("descr", payout.getDescription());
            payoutData.put("inn", payout.getInn());
            payoutData.put("sum", FormatUtil.getFormattedAmount(payout.getAmount()));
            payoutData.put("purpose", payout.getPurpose());
            payoutsAttributes.add(payoutData);
        }

        String createdAtFormatted = LocalDateTime.now(zoneId).format(dateTimeFormatter);

        final Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("payouts", payoutsAttributes);
        dataModel.put("date", createdAtFormatted);

        final String reportContent = processTemplate(dataModel, reportTemplateFileName);
        final String reportMailContent = residentsMailContentService.generateContent(payouts);

        List<String> payoutIds = payouts.stream().map(Payout::getPayoutId).collect(Collectors.toList());
        Report report = new Report();
        report.setName(prefix + "_" + createdAtFormatted + extension);
        report.setSubject(String.format("Выплаты для резидентов, сгенерированные %s (%d)",
                createdAtFormatted, payouts.get(0).getPaymentInstitutionId()));
        report.setDescription(reportMailContent);
        report.setStatus(ReportStatus.READY);
        report.setContent(reportContent);
        report.setEncoding(encoding);
        report.setPayoutIds(String.join(",", payoutIds));
        report.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        log.info("Report for residents have been successfully generated, reportSubject='{}', payoutsIds='{}'",
                report.getSubject(), report.getPayoutIds());

        return save(report);
    }

    @Override
    public long save(Report report) throws StorageException {
        log.info("Trying to save report for residents, reportSubject='{}', payoutIds='{}'",
                report.getSubject(), report.getPayoutIds());
        try {
            long reportId = reportDao.save(report);
            log.info("Report for residents have been successfully saved, " +
                    "reportId='{}', reportSubject='{}', payoutIds='{}'",
                    reportId, report.getSubject(), report.getPayoutIds());
            return reportId;
        } catch (DaoException ex) {
            throw new StorageException(
                    String.format("Failed to save report for residents, payoutIds='%s'", report.getPayoutIds()), ex);
        }
    }

    private String processTemplate(Map<String, Object> data, String templateName) {
        Configuration cfg = freeMarkerConfigurer.getConfiguration();
        try {
            Template template = cfg.getTemplate(templateName);
            StringWriter stringWriter = new StringWriter();
            template.process(data, stringWriter);
            return stringWriter.toString();
        } catch (TemplateException | IOException e) {
            throw new RuntimeException(String.format("Couldn't process template '%s'", templateName), e);
        }
    }

}
