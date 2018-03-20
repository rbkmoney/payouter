package com.rbkmoney.payouter.util;

import com.cronutils.builder.CronBuilder;
import com.cronutils.mapper.ConstantsMapper;
import com.cronutils.mapper.WeekDay;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.field.CronFieldName;
import com.cronutils.model.field.definition.DayOfWeekFieldDefinition;
import com.cronutils.model.field.expression.And;
import com.cronutils.model.field.expression.FieldExpression;
import com.rbkmoney.damsel.base.*;
import com.rbkmoney.damsel.domain.Calendar;
import com.rbkmoney.damsel.domain.CalendarHoliday;
import org.quartz.impl.calendar.HolidayCalendar;

import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.cronutils.model.field.CronFieldName.DAY_OF_MONTH;
import static com.cronutils.model.field.CronFieldName.DAY_OF_WEEK;
import static com.cronutils.model.field.expression.FieldExpression.always;
import static com.cronutils.model.field.expression.FieldExpression.questionMark;
import static com.cronutils.model.field.expression.FieldExpressionFactory.every;
import static com.cronutils.model.field.expression.FieldExpressionFactory.on;

public class SchedulerUtil {

    public static List<String> buildCron(Schedule schedule) {
        if (schedule.getDayOfMonth().isSetEvery() && !schedule.getDayOfMonth().getEvery().isSetNth()) {
            if (schedule.getDayOfWeek().isSetEvery() && !schedule.getDayOfWeek().getEvery().isSetNth()) {
                return Arrays.asList(buildCron(schedule, DAY_OF_WEEK));
            } else {
                return Arrays.asList(buildCron(schedule, DAY_OF_MONTH));
            }
        }

        if (schedule.getDayOfWeek().isSetEvery() && !schedule.getDayOfWeek().getEvery().isSetNth()) {
            return Arrays.asList(buildCron(schedule, DAY_OF_WEEK));
        } else {
            return Arrays.asList(
                    buildCron(schedule, DAY_OF_WEEK),
                    buildCron(schedule, DAY_OF_MONTH));
        }

    }

    public static String buildCron(Schedule schedule, CronFieldName questionField) {
        CronBuilder cronBuilder = CronBuilder.cron(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ))
                .withYear(buildExpression(schedule.getYear()))
                .withMonth(buildExpression(schedule.getMonth()))
                .withDoM(buildExpression(schedule.getDayOfMonth()))
                .withDoW(buildExpression(schedule.getDayOfWeek()))
                .withHour(buildExpression(schedule.getHour()))
                .withMinute(buildExpression(schedule.getMinute()))
                .withSecond(buildExpression(schedule.getSecond()));

        switch (questionField) {
            case DAY_OF_WEEK:
                cronBuilder.withDoW(questionMark());
                break;
            case DAY_OF_MONTH:
                cronBuilder.withDoM(questionMark());
                break;
            default:
                throw new IllegalArgumentException("'?' can only be specified for Day-of-Month or Day-of-Week");
        }
        return cronBuilder.instance().asString();
    }

    public static FieldExpression buildScheduleEveryExpression(ScheduleEvery scheduleEvery) {
        FieldExpression fieldExpression;
        if (scheduleEvery.isSetNth()) {
            fieldExpression = every(scheduleEvery.getNth());
        } else {
            fieldExpression = always();
        }
        return fieldExpression;
    }

    private static FieldExpression buildDaysOfWeekOnExpression(Set<DayOfWeek> days) {
        Set<Integer> dayValues = days.stream()
                .map(dayValue -> ConstantsMapper.weekDayMapping(ConstantsMapper.JAVA8, ConstantsMapper.QUARTZ_WEEK_DAY, dayValue.getValue()))
                .collect(Collectors.toSet());
        return buildOnExpression(dayValues);
    }

    private static FieldExpression buildMonthOnExpression(Set<Month> months) {
        Set<Integer> monthValues = months.stream()
                .map(monthValue -> monthValue.getValue())
                .collect(Collectors.toSet());
        return buildOnExpression(monthValues);
    }

    public static FieldExpression buildOnExpression(Set<Integer> times) {
        if (times.isEmpty()) {
            throw new IllegalArgumentException("Expression 'On' must not be empty");
        }
        FieldExpression fieldExpression = new And();
        for (int value : times) {
            fieldExpression.and(on(value));
        }
        return fieldExpression;
    }

    private static FieldExpression buildExpression(ScheduleYear scheduleYear) {
        ScheduleYear._Fields field = scheduleYear.getSetField();
        switch (field) {
            case EVERY:
                return buildScheduleEveryExpression(scheduleYear.getEvery());
            case ON:
                return buildOnExpression(scheduleYear.getOn());
            default:
                throw new IllegalArgumentException(String.format("Unknown ScheduleYear field, field='%s'", field));
        }
    }

    public static FieldExpression buildExpression(ScheduleMonth scheduleMonth) {
        ScheduleMonth._Fields field = scheduleMonth.getSetField();
        switch (field) {
            case EVERY:
                return buildScheduleEveryExpression(scheduleMonth.getEvery());
            case ON:
                return buildMonthOnExpression(scheduleMonth.getOn());
            default:
                throw new IllegalArgumentException(String.format("Unknown ScheduleMonth field, field='%s'", field));
        }
    }

    public static FieldExpression buildExpression(ScheduleDayOfWeek scheduleDayOfWeek) {
        ScheduleDayOfWeek._Fields field = scheduleDayOfWeek.getSetField();
        switch (field) {
            case EVERY:
                return buildScheduleEveryExpression(scheduleDayOfWeek.getEvery());
            case ON:
                return buildDaysOfWeekOnExpression(scheduleDayOfWeek.getOn());
            default:
                throw new IllegalArgumentException(String.format("Unknown DayOfWeek field, field='%s'", field));
        }
    }

    public static FieldExpression buildExpression(ScheduleFragment scheduleFragment) {
        ScheduleFragment._Fields field = scheduleFragment.getSetField();
        switch (field) {
            case EVERY:
                return buildScheduleEveryExpression(scheduleFragment.getEvery());
            case ON:
                return buildOnExpression(
                        scheduleFragment.getOn().stream()
                                .map(byteValue -> byteValue.intValue())
                                .collect(Collectors.toSet())
                );
            default:
                throw new IllegalArgumentException(String.format("Unknown ScheduleFragment field, field='%s'", field));
        }
    }

    public static HolidayCalendar buildCalendar(Calendar calendar) {
        HolidayCalendar holidayCalendar = new HolidayCalendar();

        String description = calendar.getName();
        if (calendar.isSetDescription()) {
            description += ": " + calendar.getDescription();
        }
        holidayCalendar.setDescription(description);

        holidayCalendar.setTimeZone(TimeZone.getTimeZone(calendar.getTimezone()));

        for (Map.Entry<Integer, Set<CalendarHoliday>> yearsHolidays : calendar.getHolidays().entrySet()) {
            int year = yearsHolidays.getKey();
            for (CalendarHoliday holiday : yearsHolidays.getValue()) {
                Date excludedDate = Date.valueOf(LocalDate.of(year, holiday.getMonth().getValue(), (int) holiday.getDay()));

                holidayCalendar.addExcludedDate(excludedDate);
            }
        }
        return holidayCalendar;
    }

}
