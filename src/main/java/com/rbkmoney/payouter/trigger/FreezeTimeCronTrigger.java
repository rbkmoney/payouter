package com.rbkmoney.payouter.trigger;

import org.quartz.Calendar;
import org.quartz.Trigger;
import org.quartz.impl.triggers.CronTriggerImpl;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import static com.rbkmoney.geck.common.util.TypeUtil.toLocalDateTime;
import static java.time.temporal.ChronoUnit.*;

public class FreezeTimeCronTrigger extends CronTriggerImpl {

    private int years;

    private int months;

    private int days;

    private long hours;

    private long minutes;

    private long seconds;

    private Date cronTime;

    public FreezeTimeCronTrigger() {
        super();
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

    public Date getCronTime() {
        return cronTime;
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
            cronTime = getFireTimeAfter(new Date());
            setNextFireTime(computeNextFireTime(cronTime, calendar));
        } else if (instr == MISFIRE_INSTRUCTION_FIRE_ONCE_NOW) {
            setNextFireTime(new Date());
        }
    }

    @Override
    public void triggered(Calendar calendar) {
        setPreviousFireTime(getNextFireTime());
        Date fireTime;
        do {
            cronTime = getFireTimeAfter(cronTime);
            fireTime = computeNextFireTime(cronTime, calendar);
        } while (fireTime.equals(computeNextFireTime(getFireTimeAfter(cronTime), calendar)));
        setNextFireTime(fireTime);
    }

    @Override
    public void updateWithNewCalendar(Calendar calendar, long misfireThreshold) {
        Instant now = Instant.now();
        cronTime = getFireTimeAfter(computePrevFireTime(Optional.ofNullable(getPreviousFireTime()).orElse(new Date()), calendar));
        setNextFireTime(computeNextFireTime(cronTime, calendar));

        if (getNextFireTime() != null && getNextFireTime().toInstant().isBefore(now)) {
            long diff = Duration.between(getNextFireTime().toInstant(), now).toMillis();
            if (diff >= misfireThreshold) {
                cronTime = getFireTimeAfter(cronTime);
                setNextFireTime(computeNextFireTime(cronTime, calendar));
            }
        }
    }

    @Override
    public Date getFireTimeAfter(Date afterTime) {
        if (afterTime == null) {
            afterTime = new Date();
        }

        if (getEndTime() != null && (afterTime.compareTo(getEndTime()) >= 0)) {
            return null;
        }

        Date pot = getTimeAfter(afterTime);
        if (getEndTime() != null && pot != null && pot.after(getEndTime())) {
            return null;
        }

        return pot;
    }

    @Override
    public Date computeFirstFireTime(Calendar calendar) {
        cronTime = getFireTimeAfter(computePrevFireTime(new Date(getStartTime().getTime() - 1000L), calendar));
        setNextFireTime(computeNextFireTime(cronTime, calendar));
        return getNextFireTime();
    }

    private Date computePrevFireTime(Date cronTime, Calendar calendar) {
        return Optional.ofNullable(cronTime)
                .map(time -> Date.from(
                        computeBoundByDuration(cronTime.toInstant(), -1, computeBackwardFreezeTimeDuration(cronTime), calendar)
                )).orElse(null);
    }

    private Date computeNextFireTime(Date cronTime, Calendar calendar) {
        return Optional.ofNullable(cronTime)
                .map(time -> Date.from(
                        computeBoundByDuration(cronTime.toInstant(), 1, computeForwardFreezeTimeDuration(cronTime), calendar)
                )).orElse(null);
    }

    private Instant computeBoundByDuration(Instant cronTime, long amountToAdd, Duration duration, Calendar calendar) {
        for (TemporalUnit temporalUnit : Arrays.asList(DAYS, HOURS, MINUTES, SECONDS)) {
            cronTime = skipExcludedTimes(cronTime, amountToAdd, temporalUnit, calendar);
            long unitCount = duration.getSeconds() / temporalUnit.getDuration().getSeconds();
            for (int unit = 0; unit < unitCount; unit++) {
                cronTime = skipExcludedTimes(cronTime.plus(amountToAdd, temporalUnit), amountToAdd, temporalUnit, calendar);
            }
            duration = duration.minus(unitCount, temporalUnit);
        }
        return cronTime;
    }

    private Instant skipExcludedTimes(Instant time, long amountToAdd, TemporalUnit temporalUnit, Calendar calendar) {
        while (calendar != null && !calendar.isTimeIncluded(time.toEpochMilli())) {
            time = time.plus(amountToAdd, temporalUnit);
        }
        return time;
    }

    private Duration computeBackwardFreezeTimeDuration(Date cronTime) {
        LocalDateTime dateTime = toLocalDateTime(cronTime.toInstant());
        return Duration.between(
                dateTime.minusYears(years)
                        .minusMonths(months)
                        .minusDays(days)
                        .minusHours(hours)
                        .minusMinutes(minutes)
                        .minusSeconds(seconds),
                dateTime
        );
    }

    private Duration computeForwardFreezeTimeDuration(Date cronTime) {
        LocalDateTime dateTime = toLocalDateTime(cronTime.toInstant());
        return Duration.between(
                dateTime,
                dateTime.plusYears(years)
                        .plusMonths(months)
                        .plusDays(days)
                        .plusHours(hours)
                        .plusMinutes(minutes)
                        .plusSeconds(seconds)
        );
    }

    @Override
    public String toString() {
        return "FreezeTimeCronTrigger{" +
                "fullName=" + getFullName() +
                ", nextFireTime=" + getNextFireTime() +
                ", misfireInstruction=" + getMisfireInstruction() +
                ", cronExpression='" + getCronExpression() + "'" +
                ", cronTime=" + cronTime +
                ", years=" + years +
                ", months=" + months +
                ", days=" + days +
                ", hours=" + hours +
                ", minutes=" + minutes +
                ", seconds=" + seconds +
                '}';
    }
}
