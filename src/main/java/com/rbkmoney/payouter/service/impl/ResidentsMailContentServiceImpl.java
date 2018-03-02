package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.payouter.dao.CashFlowDescriptionDao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

@Service
public class ResidentsMailContentServiceImpl extends MailContentServiceImpl{

    @Value("${report.residents.mailTemplateFileName}")
    private String mailTemplateFileName;

    public ResidentsMailContentServiceImpl(FreeMarkerConfigurer freeMarkerConfigurer, CashFlowDescriptionDao cashFlowDescriptionDao) {
        super(freeMarkerConfigurer, cashFlowDescriptionDao);
    }

    @Override
    public String getTemplateFileName() {
        return mailTemplateFileName;
    }
}
