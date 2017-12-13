package com.rbkmoney.payouter.service.report;

import com.rbkmoney.payouter.dao.ReportDao;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.domain.tables.pojos.Report;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.valueOf;

@Service
public class Report1CService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String DATE_FORMAT = "dd.MM.yyyy";
    private static final ZoneId MOSCOW = ZoneOffset.of("Europe/Moscow");
    private static final ZoneId UTC = ZoneOffset.of("UTC");
    private static final String TEMPLATE_NAME = "1c_payout.ftl";

    @Value("${report.1c.fileName.prefix}")
    private String namePrefix;

    @Value("${report.1c.fileName.extension}")
    private String extension;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private ReportDao reportDao;

    //todo: проверить как считаются суммы с adjustment
    public Report generateReport(List<Payout> payoutRecords)  {
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

        final String reportContent = templateEngine.process(dataModel, TEMPLATE_NAME);

        List<String> payoutIds = payoutRecords.stream().map(p -> p.getId().toString()).collect(Collectors.toList());
        Report report = new Report();
        report.setName(namePrefix + "_" + currentMoscowDate() + extension);
        report.setContent(reportContent);
        report.setDescription(reportDescription.toString());
        report.setPayoutids(String.join(",", payoutIds));
        report.setCreatedAt(currentUTC());
        reportDao.save(report);

        log.info("Generated 1CReport id:{} for payouts: {}", report.getId(), payoutIds);
        return report;
    }

    public static String currentMoscowDate() {
        return DateTimeFormatter.ofPattern(DATE_FORMAT).format(LocalDateTime.now(MOSCOW));
    }

    public static LocalDateTime currentUTC() {
        return LocalDateTime.now(UTC);
    }
}
