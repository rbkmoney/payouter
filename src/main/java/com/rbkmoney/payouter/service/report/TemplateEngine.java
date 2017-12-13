package com.rbkmoney.payouter.service.report;


import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

@Service
public class TemplateEngine {
    @Autowired
    private FreeMarkerConfigurer freeMarkerConfigurer;

    public String process(Map<String, Object> data, String templateName) {
        Configuration cfg = freeMarkerConfigurer.getConfiguration();
        Template template = null;
        try {
            template = cfg.getTemplate(templateName);
            StringWriter stringWriter = new StringWriter();
            template.process(data, stringWriter);
            String text = stringWriter.toString();
            return text;
        } catch (TemplateException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
