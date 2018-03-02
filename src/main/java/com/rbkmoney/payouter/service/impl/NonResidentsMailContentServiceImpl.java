package com.rbkmoney.payouter.service.impl;

import com.rbkmoney.payouter.dao.CashFlowDescriptionDao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

@Service
public class NonResidentsMailContentServiceImpl extends MailContentServiceImpl{

    @Value("${report.nonresidents.mailTemplateFileName}")
    private String mailTemplateFileName;

    public NonResidentsMailContentServiceImpl(FreeMarkerConfigurer freeMarkerConfigurer, CashFlowDescriptionDao cashFlowDescriptionDao) {
        super(freeMarkerConfigurer, cashFlowDescriptionDao);
    }

    @Override
    public String getTemplateFileName() {
        return mailTemplateFileName;
    }
}
