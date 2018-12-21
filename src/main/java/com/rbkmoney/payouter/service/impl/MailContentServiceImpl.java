package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.payouter.dao.PayoutSummaryDao;
import com.rbkmoney.payouter.domain.tables.pojos.Payout;
import com.rbkmoney.payouter.service.MailContentService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public abstract class MailContentServiceImpl implements MailContentService {

    public static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    protected final PayoutSummaryDao payoutSummaryDao;
    private final FreeMarkerConfigurer freeMarkerConfigurer;

    @Autowired
    public MailContentServiceImpl(FreeMarkerConfigurer freeMarkerConfigurer,
                                  PayoutSummaryDao payoutSummaryDao) {
        this.freeMarkerConfigurer = freeMarkerConfigurer;
        this.payoutSummaryDao = payoutSummaryDao;
    }

    @Override
    public String generateContent(List<Payout> payouts) {
        Map<String, Object> data = buildReportData(payouts);
        return processTemplate(data, getTemplateFileName());
    }

    abstract protected Map<String, Object> buildReportData(List<Payout> payouts);

    protected String getFormattedDateDescription(LocalDateTime dateTime, ZoneId zoneId) {
        LocalDateTime localizedDate = dateTime.atZone(ZoneOffset.UTC).withZoneSameInstant(zoneId).toLocalDateTime();
        if (localizedDate.truncatedTo(ChronoUnit.DAYS).isEqual(localizedDate)) {
            return localizedDate.minusDays(1).format(dateFormatter) + " включительно";
        }
        return localizedDate.format(dateTimeFormatter);
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
