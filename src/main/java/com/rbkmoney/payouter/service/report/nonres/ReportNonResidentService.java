package com.rbkmoney.payouter.service.report.nonres;

import com.rbkmoney.payouter.dao.CashFlowPostingDao;
import com.rbkmoney.payouter.dao.ReportDao;
import com.rbkmoney.payouter.domain.enums.ReportStatus;
import com.rbkmoney.payouter.domain.tables.pojos.CashFlowPosting;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.domain.tables.pojos.Report;
import com.rbkmoney.payouter.exception.DaoException;
import com.rbkmoney.payouter.service.report.ReportService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.valueOf;

@Service
public class ReportNonResidentService implements ReportService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String DATE_FORMAT = "dd.MM.yyyy";

    @Value("${report.nonres.file.name.prefix}")
    private String prefix;

    @Value("${report.nonres.file.name.extension}")
    private String extension;

    @Value("${report.nonres.templateFileName}")
    private String templateFileName;

    @Value("${report.nonres.timezone}")
    private ZoneId zoneId;

    @Autowired
    private FreeMarkerConfigurer freeMarkerConfigurer;

    @Autowired
    private ReportDao reportDao;

    //todo: проверить как считаются суммы с adjustment
    @Override
    public Report generate(List<Payout> payoutRecords) {
        final List<Map<String, Object>> payoutsAttributes = new ArrayList<>();
        final StringBuilder reportDescription = new StringBuilder("Выплаты для: <br>");
        for (Payout payoutRecord : payoutRecords) {
            Map<String, Object> payout = new HashMap<>();

            payout.put("party_id", payoutRecord.getPartyId());
            payout.put("shop_id", payoutRecord.getShopId());
            payout.put("payout_id", payoutRecord.getId());
            payout.put("sum_spis", new BigDecimal(payoutRecord.getAmount()).movePointLeft(2).toString());
            payout.put("fee", ""); //TODO
            payout.put("sum_poluch", "");//TODO
            payout.put("curr", payoutRecord.getCurrencyCode());
            payout.put("course", "");//TODO
            payout.put("legal_name", payoutRecord.getAccountLegalName());
            payout.put("reg_address", payoutRecord.getAccountRegisteredAddress());
            payout.put("reg_num", payoutRecord.getAccountRegisteredNumber());
            payout.put("bank_acc", "");//TODO
            payout.put("bank_swift","");//TODO
            payout.put("bank_name", payoutRecord.getBankName());
            payout.put("bank_address", payoutRecord.getBankAddress());
            payout.put("bank_local_code", payoutRecord.getBankLocalCode());
            payout.put("contract_num", "");//TODO
            payout.put("contract_date", "");//TODO
            payout.put("purpose", payoutRecord.getPurpose());

            reportDescription.append(payoutRecord.getDescription()).append(": ").append(valueOf(payout.get("sum_spis"))).append(" <br> ");

            payoutsAttributes.add(payout);
        }

        Instant instant = Instant.now();
        final Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("payouts", payoutsAttributes);
        dataModel.put("date", currentDate(instant));

        final String reportContent = processTemplate(dataModel, templateFileName);

        List<String> payoutIds = payoutRecords.stream().map(p -> p.getId().toString()).collect(Collectors.toList());
        Report report = new Report();
        report.setName(prefix + "_" + currentDate(instant) + extension);
        report.setStatus(ReportStatus.READY);
        report.setContent(reportContent);
        report.setDescription(reportDescription.toString());
        report.setPayoutids(String.join(",", payoutIds));
        report.setCreatedAt(currentUTC(instant));
        try {
            reportDao.save(report);
        } catch (DaoException e) {
            throw new RuntimeException(String.format("Couldn't save report '%s' to db", report.getName()), e);
        }

        log.info("Generated Non-Resident Report id:{} for payouts: {}", report.getId(), payoutIds);
        return report;
    }

    private String currentDate(Instant instant) {
        return DateTimeFormatter.ofPattern(DATE_FORMAT).format(instant.atZone(zoneId));
    }

    private LocalDateTime currentUTC(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
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
