package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.AbstractIntegrationTest;
import com.rbkmoney.payouter.domain.enums.CashFlowType;
import com.rbkmoney.payouter.domain.tables.pojos.CashFlowDescription;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class CashFlowDescriptionDaoTest extends AbstractIntegrationTest {

    @Autowired
    CashFlowDescriptionDao cashFlowDescriptionDao;


    @Test
    public void saveAndGet() {
        ArrayList<CashFlowDescription> cashFlowDescription = new ArrayList<>();
        CashFlowDescription e = new CashFlowDescription();
        long payoutId = 1L;
        e.setPayoutId(payoutId);
        e.setAmount(123L);
        e.setCashFlowType(CashFlowType.payment);
        e.setCurrencyCode("RUB");
        e.setCount(22);
        e.setDescription("ddf");
        cashFlowDescription.add(e);
        e = new CashFlowDescription();
        e.setPayoutId(payoutId);
        e.setAmount(143L);
        e.setCashFlowType(CashFlowType.fee);
        e.setCurrencyCode("RUB");
        e.setCount(55);
        e.setDescription("ffff");
        cashFlowDescription.add(e);
        cashFlowDescriptionDao.save(cashFlowDescription);
        Assert.assertEquals(2, cashFlowDescriptionDao.get(payoutId).size());
    }
}