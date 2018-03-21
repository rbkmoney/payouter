package com.rbkmoney.payouter.util;

import java.math.BigDecimal;

public class FormatUtil {

    public static String getFormattedAmount(long amount) {
        return BigDecimal.valueOf(amount).movePointLeft(2).toString();
    }

}
