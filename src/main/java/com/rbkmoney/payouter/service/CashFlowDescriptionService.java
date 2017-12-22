package com.rbkmoney.payouter.service;

import com.rbkmoney.payouter.domain.tables.pojos.CashFlowDescription;
import com.rbkmoney.payouter.exception.StorageException;

import java.util.List;

public interface CashFlowDescriptionService {
    List<CashFlowDescription> get(long payoutId) throws StorageException;
    void save(List<CashFlowDescription> cashFlowDescription) throws StorageException;
}
