package com.rbkmoney.payouter.trigger;

import org.quartz.Calendar;
import org.quartz.Trigger;
import org.quartz.impl.triggers.CronTriggerImpl;

import java.time.*;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.*;

public class FreezeTimeCronTrigger extends CronTriggerImpl {

    private Period period;

    private long hours;

    private long minutes;

    private long seconds;

    public FreezeTimeCronTrigger() {
        super();
        period = Period.ZERO;
    }

    public void withYears(int years) {
        this.period = period.withYears(years);
    }

    public int getYears() {
        return this.period.getYears();
    }

    public void withMonths(int months) {
        this.period = period.withMonths(months);
    }

    public int getMonths() {
        return this.period.getMonths();
    }

    public void withDays(int days) {
        this.period = period.withDays(days);
    }

    public int getDays() {
        return this.period.getDays();
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
        setNextFireTime(computeNextFireTime(getNextFireTime(), calendar));
    }

    @Override
    public void updateWithNewCalendar(Calendar calendar, long misfireThreshold) {
        Instant now = Instant.now();
        setNextFireTime(computeNextFireTime(getPreviousFireTime(), calendar));

        if (getNextFireTime() != null && getNextFireTime().toInstant().isBefore(now)) {
            long diff = Duration.between(now, getNextFireTime().toInstant()).toMillis();
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
                fireTime
                        .plusYears(period.getYears())
                        .plusMonths(period.getMonths())
                        .plusDays(period.getDays())
                        .plusHours(hours)
                        .plusMinutes(minutes)
                        .plusSeconds(seconds),
                fireTime
        );

        for (TemporalUnit temporalUnit : Arrays.asList(DAYS, HOURS, MINUTES, SECONDS)) {
            long unitCount = between.getSeconds() / temporalUnit.getDuration().getSeconds();
            for (int unit = 0; unit < unitCount; unit++) {
                fireTime = fireTime.plus(1, temporalUnit);
                while (calendar != null && !calendar.isTimeIncluded(fireTime.toInstant(ZoneOffset.UTC).toEpochMilli())) {
                    fireTime = fireTime.plus(1, temporalUnit);
                }
            }
            between = between.minus(unitCount, temporalUnit);
        }

        return Date.from(fireTime.toInstant(ZoneOffset.UTC));
    }

}
