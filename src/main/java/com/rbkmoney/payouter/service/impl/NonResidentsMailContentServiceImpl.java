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

import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NonResidentsMailContentServiceImpl extends MailContentServiceImpl {

    private final PayoutDao payoutDao;

    @Value("${report.nonresidents.mailTemplateFileName}")
    private String mailTemplateFileName;

    @Value("${report.nonresidents.timezone}")
    private ZoneId zoneId;

    public NonResidentsMailContentServiceImpl(
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
        List<Map<String, Object>> payoutDescriptionAttributes = payouts.stream().map(payout -> {
            Map<String, Object> payoutDescription = new HashMap<>();
            payoutDescription.put("name", payout.getAccountLegalName());
            payoutDescription.put("sum", FormatUtil.getFormattedAmount(payout.getAmount()));
            payoutDescription.put("curr", payout.getCurrencyCode());
            PayoutRangeData payoutRangeData = payoutDao.getRangeData(payout.getPayoutId());
            payoutDescription.put("to_date_description", getFormattedDateDescription(payoutRangeData.getToTime(), zoneId));
            List<PayoutSummary> payoutSummaries = payoutSummaryDao.get(payout.getPayoutId());
            PayoutSummary payoutSummary = payoutSummaries.stream().filter(cfd -> cfd.getCashFlowType() == PayoutSummaryOperationType.payment).findFirst().get();
            payoutDescription.put("payment_sum", FormatUtil.getFormattedAmount(payoutSummary.getAmount()));
            payoutDescription.put("rbk_fee_sum", FormatUtil.getFormattedAmount(payoutSummary.getFee()));
            payoutDescription.put("payment_count", payoutSummary.getCount());
            payoutSummaries.stream().filter(cfd -> cfd.getCashFlowType() == PayoutSummaryOperationType.refund).findFirst().ifPresent(x -> {
                payoutDescription.put("refund_sum", FormatUtil.getFormattedAmount(x.getAmount()));
                payoutDescription.put("refund_count", x.getCount());
            });
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
