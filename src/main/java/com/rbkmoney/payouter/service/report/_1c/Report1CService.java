package com.rbkmoney.payouter.service.report._1c;

import com.rbkmoney.payouter.dao.ReportDao;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.domain.tables.pojos.Report;
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
public class Report1CService implements ReportService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String DATE_FORMAT = "dd.MM.yyyy";
    private static final ZoneId MOSCOW = ZoneOffset.of("+3");
    private static final String TEMPLATE_NAME = "1c_payout.ftl";

    @Value("${report.1c.file.name.prefix}")
    private String prefix;

    @Value("${report.1c.file.name.extension}")
    private String extension;

    @Autowired
    private FreeMarkerConfigurer freeMarkerConfigurer;

    @Autowired
    private ReportDao reportDao;

    //todo: проверить как считаются суммы с adjustment
    @Override
    public Report generate(List<Payout> payoutRecords)  {
        final List<Map<String, Object>> payoutsAttributes = new ArrayList<>();
        final StringBuilder reportDescription = new StringBuilder("Выплаты для: <br>");
        for (Payout payoutRecord : payoutRecords) {
            Map<String, Object> payout = new HashMap<>();

            payout.put("corr_account", payoutRecord.getBankPostAccount()); // корреспонденский счет
            payout.put("bik", payoutRecord.getBankBik()); //бик
            payout.put("calc_account", payoutRecord.getBankAccount()); //рассчетный счет //may be contract.getContractor().getBankAccount().getAccount()
            payout.put("descr", payoutRecord.getDescription()); // Ex: Индивидуальный предприниматель Иванов Иван Иваныч
            payout.put("inn", payoutRecord.getInn()); // Идентификационный номер налогоплательщика
            payout.put("sum", new BigDecimal(payoutRecord.getAmount()).movePointLeft(2).toString());
            payout.put("purpose", payoutRecord.getPurpose()); //какое-то число; Перевод совгласно договора номер 007285/07 от 12.02.2016. Без НДС

            reportDescription.append(valueOf(payout.get("descr"))).append(": ").append(valueOf(payout.get("sum"))).append(" <br> ");

            payoutsAttributes.add(payout);
        }

        final Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("payouts", payoutsAttributes);
        dataModel.put("date", currentMoscowDate());

        final String reportContent = processTemplate(dataModel, TEMPLATE_NAME);

        List<String> payoutIds = payoutRecords.stream().map(p -> p.getId().toString()).collect(Collectors.toList());
        Report report = new Report();
        report.setName(prefix + "_" + currentMoscowDate() + extension);
        report.setContent(reportContent);
        report.setDescription(reportDescription.toString());
        report.setPayoutids(String.join(",", payoutIds));
        report.setCreatedAt(currentUTC());
        reportDao.save(report);

        log.info("Generated 1CReport id:{} for payouts: {}", report.getId(), payoutIds);
        return report;
    }

    private static String currentMoscowDate() {
        return DateTimeFormatter.ofPattern(DATE_FORMAT).format(LocalDateTime.now(MOSCOW));
    }

    private static LocalDateTime currentUTC() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private String processTemplate(Map<String, Object> data, String templateName) {
        Configuration cfg = freeMarkerConfigurer.getConfiguration();
        try {
            Template template = cfg.getTemplate(templateName);
            StringWriter stringWriter = new StringWriter();
            template.process(data, stringWriter);
            return stringWriter.toString();
        } catch (TemplateException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
