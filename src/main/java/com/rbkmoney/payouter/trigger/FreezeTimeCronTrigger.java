package com.rbkmoney.payouter.trigger;

import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import org.quartz.Calendar;
import org.quartz.CronExpression;
import org.quartz.Trigger;
import org.quartz.impl.triggers.CronTriggerImpl;

import java.text.ParseException;
import java.time.*;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import static com.cronutils.model.CronType.QUARTZ;
import static java.time.temporal.ChronoUnit.*;

public class FreezeTimeCronTrigger extends CronTriggerImpl {


    private int years;

    private int months;

    private int days;

    private long hours;

    private long minutes;

    private long seconds;

    private ExecutionTime executionTime;

    public FreezeTimeCronTrigger() {
        super();
    }

    @Override
    public void setCronExpression(String cronExpression) throws ParseException {
        super.setCronExpression(cronExpression);
        initExecutionTime();
    }

    @Override
    public void setCronExpression(CronExpression cronExpression) {
        super.setCronExpression(cronExpression);
        initExecutionTime();
    }

    public void withYears(int years) {
        this.years = years;
    }

    public int getYears() {
        return this.years;
    }

    public void withMonths(int months) {
        this.months = months;
    }

    public int getMonths() {
        return this.months;
    }

    public void withDays(int days) {
        this.days = days;
    }

    public int getDays() {
        return this.days;
    }

    public void withHours(long hours) {
        this.hours = hours;
    }

    public long getHours() {
        return hours;
    }

    public void withMinutes(long minutes) {
        this.minutes = minutes;
    }

    public long getMinutes() {
        return minutes;
    }

    public void withSeconds(long seconds) {
        this.seconds = seconds;
    }

    public long getSeconds() {
        return seconds;
    }

    private void initExecutionTime() {
        this.executionTime = ExecutionTime
                .forCron(new CronParser(CronDefinitionBuilder.instanceDefinitionFor(QUARTZ))
                        .parse(getCronExpression()));
    }

    @Override
    public void updateAfterMisfire(Calendar calendar) {
        int instr = getMisfireInstruction();

        if (instr == Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY)
            return;

        if (instr == MISFIRE_INSTRUCTION_SMART_POLICY) {
            instr = MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;
        }

        if (instr == MISFIRE_INSTRUCTION_DO_NOTHING) {
            setNextFireTime(computeNextFireTime(new Date(), calendar));
        } else if (instr == MISFIRE_INSTRUCTION_FIRE_ONCE_NOW) {
            setNextFireTime(new Date());
        }
    }

    @Override
    public void triggered(Calendar calendar) {
        setPreviousFireTime(getNextFireTime());
        setNextFireTime(computeNextFireTime(getTimeBefore(getNextFireTime()), calendar));
    }

    @Override
    protected Date getTimeBefore(Date eTime) {
        return Optional.ofNullable(executionTime)
                .map(execution -> getTimeBefore(eTime, execution))
                .orElse(null);
    }

    private Date getTimeBefore(Date eTime, ExecutionTime executionTime) {
        return executionTime.lastExecution(ZonedDateTime.ofInstant(eTime.toInstant(), getTimeZone().toZoneId()))
                .map(time -> Date.from(time.toInstant()))
                .orElse(null);
    }

    @Override
    public void updateWithNewCalendar(Calendar calendar, long misfireThreshold) {
        Instant now = Instant.now();
        setNextFireTime(computeNextFireTime(getPreviousFireTime(), calendar));

        if (getNextFireTime() != null && getNextFireTime().toInstant().isBefore(now)) {
            long diff = Duration.between(getNextFireTime().toInstant(), now).toMillis();
            if (diff >= misfireThreshold) {
                setNextFireTime(computeNextFireTime(getNextFireTime(), calendar));
            }
        }
    }

    @Override
    public Date computeFirstFireTime(Calendar calendar) {
        setNextFireTime(computeNextFireTime(new Date(getStartTime().getTime() - 1000L), calendar));
        return getNextFireTime();
    }

    private Date computeNextFireTime(Date currentFireTime, Calendar calendar) {
        return Optional.ofNullable(getFireTimeAfter(currentFireTime))
                .map(time -> computeBoundWithFreezeTime(time, calendar))
                .orElse(null);
    }

    private Date computeBoundWithFreezeTime(Date nextFireTime, Calendar calendar) {
        Objects.requireNonNull(nextFireTime, "nextFireTime must be set");
        LocalDateTime fireTime = LocalDateTime.ofInstant(nextFireTime.toInstant(), ZoneOffset.UTC);

        Duration between = Duration.between(
                fireTime,
                fireTime.plusYears(years)
                        .plusMonths(months)
                        .plusDays(days)
                        .plusHours(hours)
                        .plusMinutes(minutes)
                        .plusSeconds(seconds)
        );

        for (TemporalUnit temporalUnit : Arrays.asList(DAYS, HOURS, MINUTES, SECONDS)) {
            long unitCount = between.getSeconds() / temporalUnit.getDuration().getSeconds();
            for (int unit = 0; unit < unitCount; unit++) {
                fireTime = skipExcludedTimes(fireTime, temporalUnit, calendar).plus(1, temporalUnit);
                fireTime = skipExcludedTimes(fireTime, temporalUnit, calendar);
            }
            between = between.minus(unitCount, temporalUnit);
        }

        return Date.from(fireTime.toInstant(ZoneOffset.UTC));
    }

    public LocalDateTime skipExcludedTimes(LocalDateTime time, TemporalUnit temporalUnit, Calendar calendar) {
        while (calendar != null && !calendar.isTimeIncluded(time.toInstant(ZoneOffset.UTC).toEpochMilli())) {
            time = time.plus(1, temporalUnit);
        }
        return time;
    }

}
