package com.rbkmoney.payouter.service;

import com.rbkmoney.payouter.domain.tables.pojos.Payout;

import java.util.List;

public interface MailContentService {
    String getTemplateFileName();

    String generateContent(List<Payout> payouts);
}
