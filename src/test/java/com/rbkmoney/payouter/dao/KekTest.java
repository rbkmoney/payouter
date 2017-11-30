package com.rbkmoney.payouter.dao;

import com.rbkmoney.payouter.domain.tables.Adjustment;
import org.junit.Test;

import java.util.Arrays;

import static com.rbkmoney.payouter.dao.DaoTestUtil.getNullColumnNames;
import static com.rbkmoney.payouter.domain.Tables.PAYMENT;
import static com.rbkmoney.payouter.domain.Tables.REFUND;

public class KekTest {

    @Test
    public void kekTest() {
        System.out.println(Arrays.asList(getNullColumnNames(Adjustment.ADJUSTMENT)));
        System.out.println(Arrays.asList(getNullColumnNames(REFUND)));
        System.out.println(Arrays.asList(getNullColumnNames(PAYMENT)));
    }

}
