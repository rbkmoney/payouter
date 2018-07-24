package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.payouter.dao.PayoutSummaryDao;
import com.rbkmoney.payouter.domain.enums.PayoutSummaryOperationType;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutSummary;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.util.FormatUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResidentsMailContentServiceImpl extends MailContentServiceImpl{

    @Value("${report.residents.mailTemplateFileName}")
    private String mailTemplateFileName;

    @Value("${report.residents.timezone}")
    private ZoneId zoneId;

    public ResidentsMailContentServiceImpl(FreeMarkerConfigurer freeMarkerConfigurer, PayoutSummaryDao payoutSummaryDao) {
        super(freeMarkerConfigurer, payoutSummaryDao);
    }

    @Override
    protected Map<String, Object> buildReportData(List<Payout> payouts) {
        Map<String, Object> data = new HashMap<>();
        List<Payout> sortedPayouts = payouts.stream().sorted((p1, p2) -> p2.getAmount().compareTo(p1.getAmount())).collect(Collectors.toList());
        List<Map<String, Object>> payoutDescriptionAttributes = sortedPayouts.stream().map(payout -> {
            Map<String, Object> payoutDescription = new HashMap<>();
            payoutDescription.put("name", payout.getDescription());
            payoutDescription.put("sum", FormatUtil.getFormattedAmount(payout.getAmount()));
            payoutDescription.put("curr", payout.getCurrencyCode());
            payoutDescription.put("inn", payout.getInn());
            payoutDescription.put("to_date_description", getFormattedDateDescription(payout.getToTime(), zoneId));
            List<PayoutSummary> cashFlowDescriptions = payoutSummaryDao.get(String.valueOf(payout.getId()));
            PayoutSummary payoutSummary = cashFlowDescriptions.stream().filter(cfd -> cfd.getCashFlowType() == PayoutSummaryOperationType.payment).findFirst().get();
            payoutDescription.put("payment_sum", FormatUtil.getFormattedAmount(payoutSummary.getAmount()));
            payoutDescription.put("rbk_fee_sum", FormatUtil.getFormattedAmount(payoutSummary.getFee()));
            payoutDescription.put("payment_count", payoutSummary.getCount());
            cashFlowDescriptions.stream().filter(cfd -> cfd.getCashFlowType() == PayoutSummaryOperationType.refund).findFirst().ifPresent(x -> {
                payoutDescription.put("refund_sum", FormatUtil.getFormattedAmount(x.getAmount()));
                payoutDescription.put("refund_count", x.getCount());
            });
            payoutDescription.put("fee_sum", FormatUtil.getFormattedAmount(payout.getFee()));
            return payoutDescription;
        }).collect(Collectors.toList());
        data.put("payoutDescriptions", payoutDescriptionAttributes);
        data.put("total_amount", FormatUtil.getFormattedAmount(sortedPayouts.stream().mapToLong(Payout::getAmount).sum()));
        return data;
    }

    @Override
    public String getTemplateFileName() {
        return mailTemplateFileName;
    }
}
