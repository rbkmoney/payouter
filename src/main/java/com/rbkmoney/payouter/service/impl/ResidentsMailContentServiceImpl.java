package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.payouter.dao.PayoutDao;
import com.rbkmoney.payouter.dao.PayoutSummaryDao;
import com.rbkmoney.payouter.domain.enums.PayoutSummaryOperationType;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutRangeData;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutSummary;
import com.rbkmoney.payouter.util.FormatUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ResidentsMailContentServiceImpl extends MailContentServiceImpl {

    private final PayoutDao payoutDao;

    @Value("${report.residents.mailTemplateFileName}")
    private String mailTemplateFileName;

    @Value("${report.residents.timezone}")
    private ZoneId zoneId;

    public ResidentsMailContentServiceImpl(
            FreeMarkerConfigurer freeMarkerConfigurer,
            PayoutSummaryDao payoutSummaryDao,
            PayoutDao payoutDao
    ) {
        super(freeMarkerConfigurer, payoutSummaryDao);
        this.payoutDao = payoutDao;
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
            PayoutRangeData payoutRangeData = payoutDao.getRangeData(payout.getPayoutId());
            LocalDateTime toTime = payoutRangeData != null ? payoutRangeData.getToTime() : payout.getCreatedAt();
            payoutDescription.put("to_date_description", getFormattedDateDescription(toTime, zoneId));
            List<PayoutSummary> cashFlowDescriptions = payoutSummaryDao.get(payout.getPayoutId());
            cashFlowDescriptions.stream()
                    .filter(cfd -> cfd.getCashFlowType() == PayoutSummaryOperationType.payment)
                    .findFirst().ifPresent(
                    paymentSummary -> {
                        payoutDescription.put("payment_sum", FormatUtil.getFormattedAmount(paymentSummary.getAmount()));
                        payoutDescription.put("rbk_fee_sum", FormatUtil.getFormattedAmount(paymentSummary.getFee()));
                        payoutDescription.put("payment_count", paymentSummary.getCount());
                    }
            );
            cashFlowDescriptions.stream()
                    .filter(cfd -> cfd.getCashFlowType() == PayoutSummaryOperationType.refund)
                    .findFirst().ifPresent(refundSummary -> {
                        payoutDescription.put("refund_sum", FormatUtil.getFormattedAmount(refundSummary.getAmount()));
                        payoutDescription.put("refund_count", refundSummary.getCount());
                    }
            );
            payoutDescription.put("fee_sum", FormatUtil.getFormattedAmount(payout.getFee()));
            return payoutDescription;
        }).collect(Collectors.toList());
        data.put("payoutDescriptions", payoutDescriptionAttributes);
        return data;
    }

    @Override
    public String getTemplateFileName() {
        return mailTemplateFileName;
    }
}
