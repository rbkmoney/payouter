package com.rbkmoney.payouter.service.report;

import com.rbkmoney.payouter.dao.PayoutDao;
import com.rbkmoney.payouter.dao.ReportDao;
import com.rbkmoney.payouter.domain.tables.records.PayoutRecord;
import com.rbkmoney.payouter.domain.tables.records.ReportRecord;
import com.rbkmoney.payouter.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.valueOf;

@Service
public class Report1CService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${ones.file.name.prefix}")
    String namePrefix;

    @Value("${ones.file.extension}")
    String extension;

    @Autowired
    TemplateEngine templateEngine;

    @Autowired
    PayoutDao payoutDao;

    @Autowired
    ReportDao reportDao;

    //todo: проверить как считаются суммы с adjustment
    public ReportRecord generateReport(List<PayoutRecord> payoutRecords)  {
        final List<Map<String, Object>> payoutsAttributes = new ArrayList<>();
        final StringBuilder reportDescription = new StringBuilder("Выплаты для: <br>");
        for (PayoutRecord payoutRecord : payoutRecords) {
            Map<String, Object> payout = new HashMap<>();

            payout.put("cor_account", payoutRecord.getCorAccount()); // корреспонденский счет
            payout.put("bic", payoutRecord.getBankBik()); //бик
            payout.put("calc_account", payoutRecord.getBankAccount()); //рассчетный счет //may be contract.getContractor().getBankAccount().getAccount()
            payout.put("descr", payoutRecord.getDescription()); // Ex: Индивидуальный предприниматель Иванов Иван Иваныч
            payout.put("inn", payoutRecord.getInn()); // Идентификационный номер налогоплательщика
            payout.put("sum", new BigDecimal(payoutRecord.getAmount()).movePointLeft(2).toString());
            payout.put("purpose", payoutRecord.getPurpose()); //какое-то число; Перевод совгласно договора номер 007285/07 от 12.02.2016. Без НДС

            reportDescription.append(valueOf(payout.get("descr")) + ": " + valueOf(payout.get("sum")) + " <br> ");

            payoutsAttributes.add(payout);
        }

        final Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("payouts", payoutsAttributes);
        dataModel.put("date", TimeUtils.currentMoscowDate());

        final String reportContent = templateEngine.process(dataModel, "ones_payout.ftl");

        List<String> payoutIds = payoutRecords.stream().map(p -> p.getId().toString()).collect(Collectors.toList());
        ReportRecord report = new ReportRecord();
        report.setName(namePrefix + "_" + TimeUtils.currentMoscowDate() + extension);
        report.setContent(reportContent);
        report.setDescription(reportDescription.toString());
        report.setPayoutids(String.join(",", payoutIds));
        report.setCreatedAt(TimeUtils.currentUTC());
        reportDao.save(report);

        log.info("Generated OneSReport id:{} for payouts: {}", report.getId(), payoutIds);
        return report;
    }
}
