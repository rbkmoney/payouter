package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.payouter.dao.CashFlowDescriptionDao;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.service.MailContentService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class MailContentServiceImpl implements MailContentService {

    public static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final FreeMarkerConfigurer freeMarkerConfigurer;

    protected final CashFlowDescriptionDao cashFlowDescriptionDao;

    private Map<String, Object> data;

    @Autowired
    public MailContentServiceImpl(FreeMarkerConfigurer freeMarkerConfigurer,
                                  CashFlowDescriptionDao cashFlowDescriptionDao) {
        this.freeMarkerConfigurer = freeMarkerConfigurer;
        this.cashFlowDescriptionDao = cashFlowDescriptionDao;
    }

    @Override
    public String generateContent(List<Payout> payouts) {
        data = new HashMap<>();
        List<Map<String, Object>> reportDescriptionAttributes = new ArrayList<>();
        payouts.forEach(p -> reportDescriptionAttributes.add(buildPayoutRecordDescription(p)));
        data.put("reportDescriptions", reportDescriptionAttributes);
        String reportMailContent = processTemplate(data, getTemplateFileName());
        return reportMailContent;
    }

    abstract protected Map<String, Object> buildPayoutRecordDescription(Payout payout);

    protected String getFormattedAmount(Long amount) {
        return BigDecimal.valueOf(amount).movePointLeft(2).toString();
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
