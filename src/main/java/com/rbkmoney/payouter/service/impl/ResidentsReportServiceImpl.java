package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.payouter.dao.ReportDao;
import com.rbkmoney.payouter.domain.enums.PayoutAccountType;
import com.rbkmoney.payouter.domain.enums.ReportStatus;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.domain.tables.pojos.Report;
import com.rbkmoney.payouter.exception.DaoException;
import com.rbkmoney.payouter.exception.StorageException;
import com.rbkmoney.payouter.service.PayoutService;
import com.rbkmoney.payouter.service.ReportService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ResidentsReportServiceImpl implements ReportService {

    public static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ReportDao reportDao;

    private final PayoutService payoutService;

    private final FreeMarkerConfigurer freeMarkerConfigurer;

    @Value("${report.residents.file.name.prefix}")
    private String prefix;

    @Value("${report.residents.file.name.extension}")
    private String extension;

    @Value("${report.residents.templateFileName}")
    private String templateFileName;

    @Value("${report.residents.file.encoding}")
    private String encoding;

    @Value("${report.residents.timezone}")
    private ZoneId zoneId;

    @Autowired
    public ResidentsReportServiceImpl(ReportDao reportDao, PayoutService payoutService, FreeMarkerConfigurer freeMarkerConfigurer) {
        this.reportDao = reportDao;
        this.payoutService = payoutService;
        this.freeMarkerConfigurer = freeMarkerConfigurer;
    }

    @Override
    @Scheduled(cron="${report.residents.cron}", zone="${report.residents.timezone}")
    @Transactional(propagation = Propagation.REQUIRED)
    public long generateAndSave() throws StorageException {
        List<Payout> payouts = payoutService.getUnpaidPayoutsByAccountType(PayoutAccountType.russian_payout_account);

        long reportId = generateAndSave(payouts);
        payouts.forEach(payout -> payoutService.pay(payout.getId()));
        return reportId;
    }

    @Override
    public long generateAndSave(List<Payout> payouts) throws StorageException {
        log.info("Trying to generate and save report for residents, payouts='%s'", payouts);
        final List<Map<String, Object>> payoutsAttributes = new ArrayList<>();
        final StringBuilder reportDescription = new StringBuilder("Выплаты для резидентов: <br>");
        for (Payout payout : payouts) {
            Map<String, Object> payoutData = new HashMap<>();

            payoutData.put("corr_account", payout.getBankPostAccount());
            payoutData.put("bik", payout.getBankLocalCode());
            payoutData.put("calc_account", payout.getBankAccount());
            payoutData.put("descr", payout.getDescription());
            payoutData.put("inn", payout.getInn());
            payoutData.put("sum", BigDecimal.valueOf(payout.getAmount()).movePointLeft(2));
            payoutData.put("purpose", payout.getPurpose());

            reportDescription
                    .append(payoutData.get("descr"))
                    .append(": ")
                    .append(payoutData.get("sum"))
                    .append(" <br> ");

            payoutsAttributes.add(payoutData);
        }

        LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);
        String createdAtFormatted = LocalDateTime.now(zoneId).format(dateTimeFormatter);

        final Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("payouts", payoutsAttributes);
        dataModel.put("date", createdAtFormatted);

        final String reportContent = processTemplate(dataModel, templateFileName);

        List<String> payoutIds = payouts.stream().map(p -> p.getId().toString()).collect(Collectors.toList());
        Report report = new Report();
        report.setName(prefix + "_" + createdAtFormatted + extension);
        report.setSubject("Выплаты для резидентов, сгенерированные " + createdAtFormatted);
        report.setDescription(reportDescription.toString());
        report.setStatus(ReportStatus.READY);
        report.setContent(reportContent);
        report.setEncoding(encoding);
        report.setPayoutIds(String.join(",", payoutIds));
        report.setCreatedAt(createdAt);
        log.info("Report for residents have been successfully generated, report='{}', payouts='{}'", report, payouts);

        return save(report);
    }

    @Override
    public long save(Report report) throws StorageException {
        log.info("Trying to save report for residents, payoutIds='{}'", report.getPayoutIds());
        try {
            long reportId = reportDao.save(report);
            log.info("Report for residents have been successfully saved, reportId='{}', payoutIds='{}'", reportId, report.getPayoutIds());
            return reportId;
        } catch (DaoException ex) {
            throw new StorageException(String.format("Failed to save report for residents, payoutIds='%s'", report.getPayoutIds()), ex);
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
