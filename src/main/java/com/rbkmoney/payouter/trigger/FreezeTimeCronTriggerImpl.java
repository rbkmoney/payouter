package com.rbkmoney.payouter.trigger;

import lombok.Getter;
import org.quartz.Calendar;
import org.quartz.CronExpression;
import org.quartz.ScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.impl.triggers.AbstractTrigger;
import org.quartz.impl.triggers.CoreTrigger;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;

import static com.rbkmoney.geck.common.util.TypeUtil.toLocalDateTime;
import static java.time.temporal.ChronoUnit.*;

@Getter
public class FreezeTimeCronTriggerImpl
        extends AbstractTrigger<FreezeTimeCronTrigger> implements FreezeTimeCronTrigger, CoreTrigger {

    private CronExpression cronExpression = null;
    private Date startTime = null;
    private Date endTime = null;
    private Date nextFireTime = null;
    private Date previousFireTime = null;
    private transient TimeZone timeZone = null;

    private int years;
    private int months;
    private int days;
    private long hours;
    private long minutes;
    private long seconds;
    private Date currentCronTime;
    private Date nextCronTime;

    protected static final int YEAR_TO_GIVEUP_SCHEDULING_AT = CronExpression.MAX_YEAR;

    public FreezeTimeCronTriggerImpl() {
        super();
    }

    public void withYears(int years) {
        this.years = years;
    }

    public void withMonths(int months) {
        this.months = months;
    }

    public void withDays(int days) {
        this.days = days;
    }

    public void withHours(long hours) {
        this.hours = hours;
    }

    public void withMinutes(long minutes) {
        this.minutes = minutes;
    }

    public void withSeconds(long seconds) {
        this.seconds = seconds;
    }

    public void setTimeZone(TimeZone timeZone) {
        if(cronExpression != null) {
            cronExpression.setTimeZone(timeZone);
        }
        this.timeZone = timeZone;
    }

    public void setCronExpression(CronExpression cronExpression) {
        TimeZone origTz = getTimeZone();
        this.cronExpression = cronExpression;
        this.cronExpression.setTimeZone(origTz);
    }

    @Override
    public void triggered(Calendar calendar) {
        setPreviousFireTime(getNextFireTime());
        currentCronTime = nextCronTime;
        nextFireTime(nextCronTime, calendar);
    }

    @Override
    public Date computeFirstFireTime(Calendar calendar) {
        nextFireTime(computePrevFireTime(new Date(getStartTime().getTime() - 1000L), calendar), calendar);
        return getNextFireTime();
    }

    @Override
    public boolean mayFireAgain() {
        return (getNextFireTime() != null);
    }

    @Override
    public Date getStartTime() {
        return this.startTime;
    }

    @Override
    public void setStartTime(Date startTime) {
        if (startTime == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }

        Date eTime = getEndTime();
        if (eTime != null && eTime.before(startTime)) {
            throw new IllegalArgumentException(
                    "End time cannot be before start time");
        }

        // round off millisecond...
        // Note timeZone is not needed here as parameter for
        // Calendar.getInstance(),
        // since time zone is implicit when using a Date in the setTime method.
        java.util.Calendar cl = java.util.Calendar.getInstance();
        cl.setTime(startTime);
        cl.set(java.util.Calendar.MILLISECOND, 0);

        this.startTime = cl.getTime();
    }

    @Override
    public void setEndTime(Date endTime) {
        Date sTime = getStartTime();
        if (sTime != null && endTime != null && sTime.after(endTime)) {
            throw new IllegalArgumentException(
                    "End time cannot be before start time");
        }

        this.endTime = endTime;
    }

    @Override
    public Date getEndTime() {
        return this.endTime;
    }

    @Override
    public Date getNextFireTime() {
        return this.nextFireTime;
    }

    @Override
    public Date getPreviousFireTime() {
        return this.previousFireTime;
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
    public Date getFinalFireTime() {
        Date resultTime;
        if (getEndTime() != null) {
            resultTime = getTimeBefore(new Date(getEndTime().getTime() + 1000l));
        } else {
            resultTime = (cronExpression == null) ? null : cronExpression.getFinalFireTime();
        }

        if ((resultTime != null) && (getStartTime() != null) && (resultTime.before(getStartTime()))) {
            return null;
        }

        return resultTime;
    }

    @Override
    protected boolean validateMisfireInstruction(int misfireInstruction) {
        return misfireInstruction >= MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY
                && misfireInstruction <= MISFIRE_INSTRUCTION_DO_NOTHING;
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
            nextCronTime = getFireTimeAfter(new Date());
            setNextFireTime(computeNextFireTime(nextCronTime, calendar));
        } else if (instr == MISFIRE_INSTRUCTION_FIRE_ONCE_NOW) {
            setNextFireTime(new Date());
        }
    }

    @Override
    public void updateWithNewCalendar(Calendar calendar, long misfireThreshold) {
        Instant now = Instant.now();
        nextFireTime(computePrevFireTime(Optional.ofNullable(getPreviousFireTime()).orElse(new Date()), calendar), calendar);

        if (getNextFireTime() != null && getNextFireTime().toInstant().isBefore(now)) {
            long diff = Duration.between(getNextFireTime().toInstant(), now).toMillis();
            if (diff >= misfireThreshold) {
                nextFireTime(nextCronTime, calendar);
            }
        }
    }

    private void nextFireTime(Date currentTime, Calendar calendar) {
        nextCronTime = currentTime;
        Date fireTime;
        do {
            nextCronTime = getFireTimeAfter(nextCronTime);
            fireTime = computeNextFireTime(nextCronTime, calendar);
        } while (fireTime == null || fireTime.equals(computeNextFireTime(getFireTimeAfter(nextCronTime), calendar)));
        setNextFireTime(fireTime);
    }

    @Override
    public ScheduleBuilder<FreezeTimeCronTrigger> getScheduleBuilder() {
        FreezeTimeCronScheduleBuilder cb = FreezeTimeCronScheduleBuilder.cronSchedule(getCronExpression())
                .inTimeZone(getTimeZone());

        switch(getMisfireInstruction()) {
            case MISFIRE_INSTRUCTION_DO_NOTHING : cb.withMisfireHandlingInstructionDoNothing();
                break;
            case MISFIRE_INSTRUCTION_FIRE_ONCE_NOW : cb.withMisfireHandlingInstructionFireAndProceed();
                break;
        }

        return cb;
    }

    @Override
    public void setNextFireTime(Date nextFireTime) {
        this.nextFireTime = nextFireTime;
    }

    @Override
    public void setPreviousFireTime(Date previousFireTime) {
        this.previousFireTime = previousFireTime;
    }

    @Override
    public String getCronExpression() {
        return cronExpression == null ? null : cronExpression.getCronExpression();
    }

    @Override
    public TimeZone getTimeZone() {
        if(cronExpression != null) {
            return cronExpression.getTimeZone();
        }

        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
        }
        return timeZone;
    }

    @Override
    public String getExpressionSummary() {
        return cronExpression == null ? null : cronExpression.getExpressionSummary();
    }

    @Override
    public boolean hasAdditionalProperties() {
        return false;
    }

    protected Date getTimeAfter(Date afterTime) {
        return (cronExpression == null) ? null : cronExpression.getTimeAfter(afterTime);
    }

    /**
     * NOT YET IMPLEMENTED: Returns the time before the given time
     * that this <code>CronTrigger</code> will fire.
     */
    protected Date getTimeBefore(Date eTime) {
        return (cronExpression == null) ? null : cronExpression.getTimeBefore(eTime);
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
            cronTime = skipExcludedTimes(cronTime, calendar);
            long unitCount = duration.getSeconds() / temporalUnit.getDuration().getSeconds();
            for (int unit = 0; unit < unitCount; unit++) {
                cronTime = skipExcludedTimes(cronTime.plus(amountToAdd, temporalUnit), amountToAdd, temporalUnit, calendar);
            }
            duration = duration.minus(unitCount, temporalUnit);
        }
        return cronTime;
    }

    private Instant skipExcludedTimes(Instant time, Calendar calendar) {
        Instant nextIncludedTime = Instant.ofEpochMilli(calendar.getNextIncludedTime(time.toEpochMilli()));
        return nextIncludedTime.isAfter(time) ? nextIncludedTime : time;
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
                ", currentCronTime=" + currentCronTime +
                ", nextCronTime=" + nextCronTime +
                ", years=" + years +
                ", months=" + months +
                ", days=" + days +
                ", hours=" + hours +
                ", minutes=" + minutes +
                ", seconds=" + seconds +
                '}';
    }
}
