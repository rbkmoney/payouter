package com.rbkmoney.payouter.trigger;

import org.quartz.Trigger;

import java.util.TimeZone;

public interface FreezeTimeCronTrigger extends Trigger {

    int MISFIRE_INSTRUCTION_FIRE_ONCE_NOW = 1;

    int MISFIRE_INSTRUCTION_DO_NOTHING = 2;

    String getCronExpression();

    TimeZone getTimeZone();

    String getExpressionSummary();

}
