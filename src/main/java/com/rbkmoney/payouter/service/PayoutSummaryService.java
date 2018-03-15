package com.rbkmoney.payouter.service;

import com.rbkmoney.payouter.domain.tables.pojos.Adjustment;
import com.rbkmoney.payouter.domain.tables.pojos.PayoutSummary;
import com.rbkmoney.payouter.domain.tables.pojos.Payment;
import com.rbkmoney.payouter.domain.tables.pojos.Refund;
import com.rbkmoney.payouter.exception.StorageException;

import java.util.List;

public interface PayoutSummaryService {

    List<PayoutSummary> get(String payoutId) throws StorageException;

    void save(long payoutId, String currencyCode, List<Payment> payments, List<Refund> refunds, List<Adjustment> adjustments) throws StorageException;

    void save(List<PayoutSummary> payoutSummaries) throws StorageException;

}