package com.rbkmoney.payouter.trigger;

import org.quartz.CronExpression;
import org.quartz.CronTrigger;
import org.quartz.ScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.spi.MutableTrigger;

import java.text.ParseException;

public class FreezeTimeCronScheduleBuilder extends ScheduleBuilder<FreezeTimeCronTrigger> {

    private CronExpression cronExpression;

    private int misfireInstruction = CronTrigger.MISFIRE_INSTRUCTION_SMART_POLICY;

    private int years;

    private int months;

    private int days;

    private long hours;

    private long minutes;

    private long seconds;

    protected FreezeTimeCronScheduleBuilder(CronExpression cronExpression) {
        this.cronExpression = cronExpression;
    }

    @Override
    public MutableTrigger build() {
        FreezeTimeCronTrigger freezeTimeCronTrigger = new FreezeTimeCronTrigger();
        freezeTimeCronTrigger.setCronExpression(cronExpression);
        freezeTimeCronTrigger.setTimeZone(cronExpression.getTimeZone());
        freezeTimeCronTrigger.setMisfireInstruction(misfireInstruction);
        freezeTimeCronTrigger.withYears(years);
        freezeTimeCronTrigger.withMonths(months);
        freezeTimeCronTrigger.withMonths(days);
        freezeTimeCronTrigger.withHours(hours);
        freezeTimeCronTrigger.withMinutes(minutes);
        freezeTimeCronTrigger.withSeconds(seconds);

        return freezeTimeCronTrigger;
    }

    public static FreezeTimeCronScheduleBuilder cronSchedule(String cronExpression) {
        try {
            return cronSchedule(new CronExpression(cronExpression));
        } catch (ParseException ex) {
            throw new IllegalArgumentException(String.format("CronExpression '%s' is invalid.", cronExpression), ex);
        }
    }

    public FreezeTimeCronScheduleBuilder withYears(int years) {
        this.years = years;
        return this;
    }

    public FreezeTimeCronScheduleBuilder withMonths(int months) {
        this.months = months;
        return this;
    }

    public FreezeTimeCronScheduleBuilder withDays(int days) {
        this.days = days;
        return this;
    }

    public FreezeTimeCronScheduleBuilder withHours(long hours) {
        this.hours = hours;
        return this;
    }

    public FreezeTimeCronScheduleBuilder withMinutes(long minutes) {
        this.minutes = minutes;
        return this;
    }

    public FreezeTimeCronScheduleBuilder withSeconds(long seconds) {
        this.seconds = seconds;
        return this;
    }

    public static FreezeTimeCronScheduleBuilder cronSchedule(CronExpression cronExpression) {
        return new FreezeTimeCronScheduleBuilder(cronExpression);
    }

    public FreezeTimeCronScheduleBuilder withMisfireHandlingInstructionIgnoreMisfires() {
        misfireInstruction = Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY;
        return this;
    }

    public FreezeTimeCronScheduleBuilder withMisfireHandlingInstructionDoNothing() {
        misfireInstruction = CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING;
        return this;
    }

    public FreezeTimeCronScheduleBuilder withMisfireHandlingInstructionFireAndProceed() {
        misfireInstruction = CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;
        return this;
    }

}
