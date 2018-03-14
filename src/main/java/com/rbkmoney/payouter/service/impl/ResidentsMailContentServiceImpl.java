package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.payouter.dao.CashFlowDescriptionDao;
import com.rbkmoney.payouter.domain.enums.CashFlowType;
import com.rbkmoney.payouter.domain.tables.pojos.CashFlowDescription;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
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

    public ResidentsMailContentServiceImpl(FreeMarkerConfigurer freeMarkerConfigurer, CashFlowDescriptionDao cashFlowDescriptionDao) {
        super(freeMarkerConfigurer, cashFlowDescriptionDao);
    }

    @Override
    protected Map<String, Object> buildPayoutRecordDescription(Payout payout) {
        Map<String, Object> reportDescription = new HashMap<>();
        reportDescription.put("name", payout.getDescription());
        reportDescription.put("sum", getFormattedAmount(payout.getAmount()));
        reportDescription.put("inn", payout.getInn());
        reportDescription.put("to_date_description", getFormattedDateDescription(payout.getToTime(), zoneId));
        List<CashFlowDescription> cashFlowDescriptions = cashFlowDescriptionDao.get(String.valueOf(payout.getId()));
        CashFlowDescription paymentCashFlowDescription = cashFlowDescriptions.stream().filter(cfd -> cfd.getCashFlowType() == CashFlowType.payment).findFirst().get();
        reportDescription.put("payment_sum", getFormattedAmount(paymentCashFlowDescription.getAmount()));
        reportDescription.put("rbk_fee_sum", getFormattedAmount(paymentCashFlowDescription.getFee()));
        reportDescription.put("payment_count", paymentCashFlowDescription.getCount());
        cashFlowDescriptions.stream().filter(cfd -> cfd.getCashFlowType() == CashFlowType.refund).findFirst().ifPresent(x -> {
            reportDescription.put("refund_sum", getFormattedAmount(x.getAmount()));
            reportDescription.put("refund_count", x.getCount());
        });
        reportDescription.put("fee_sum", getFormattedAmount(payout.getFee()));
        return reportDescription;
    }

    @Override
    public String getTemplateFileName() {
        return mailTemplateFileName;
    }
}
