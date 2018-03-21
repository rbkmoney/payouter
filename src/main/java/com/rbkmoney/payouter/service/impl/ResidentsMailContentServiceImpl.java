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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    protected Map<String, Object> buildPayoutRecordDescription(Payout payout) {
        Map<String, Object> reportDescription = new HashMap<>();
        reportDescription.put("name", payout.getDescription());
        reportDescription.put("sum", FormatUtil.getFormattedAmount(payout.getAmount()));
        reportDescription.put("inn", payout.getInn());
        reportDescription.put("to_date_description", getFormattedDateDescription(payout.getToTime(), zoneId));
        List<PayoutSummary> cashFlowDescriptions = payoutSummaryDao.get(String.valueOf(payout.getId()));
        PayoutSummary payoutSummary = cashFlowDescriptions.stream().filter(cfd -> cfd.getCashFlowType() == PayoutSummaryOperationType.payment).findFirst().get();
        reportDescription.put("payment_sum", FormatUtil.getFormattedAmount(payoutSummary.getAmount()));
        reportDescription.put("rbk_fee_sum", FormatUtil.getFormattedAmount(payoutSummary.getFee()));
        reportDescription.put("payment_count", payoutSummary.getCount());
        cashFlowDescriptions.stream().filter(cfd -> cfd.getCashFlowType() == PayoutSummaryOperationType.refund).findFirst().ifPresent(x -> {
            reportDescription.put("refund_sum", FormatUtil.getFormattedAmount(x.getAmount()));
            reportDescription.put("refund_count", x.getCount());
        });
        reportDescription.put("fee_sum", FormatUtil.getFormattedAmount(payout.getFee()));
        return reportDescription;
    }

    @Override
    public String getTemplateFileName() {
        return mailTemplateFileName;
    }
}
