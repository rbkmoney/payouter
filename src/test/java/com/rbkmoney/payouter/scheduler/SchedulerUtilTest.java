package com.rbkmoney.payouter.scheduler;

import au.com.bytecode.opencsv.CSVReader;
import com.rbkmoney.damsel.base.Month;
import com.rbkmoney.damsel.base.*;
import com.rbkmoney.damsel.domain.Calendar;
import com.rbkmoney.damsel.domain.CalendarHoliday;
import com.rbkmoney.payouter.util.SchedulerUtil;
import org.junit.Test;
import org.quartz.CronExpression;
import org.quartz.impl.calendar.HolidayCalendar;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.*;
import java.util.*;

import static com.rbkmoney.damsel.base.DayOfWeek.*;
import static com.rbkmoney.damsel.base.Month.*;
import static org.junit.Assert.*;

public class SchedulerUtilTest {

    @Test
    public void testTimeBoundCompute() throws IOException {
        TimeSpan timeSpan = new TimeSpan();
        timeSpan.setYears((short) 1);
        timeSpan.setMonths((short) 2);
        timeSpan.setWeeks((short) 4);
        timeSpan.setDays((short) 3);
        timeSpan.setHours((short) 3);
        timeSpan.setMinutes((short) 34);
        timeSpan.setSeconds((short) 55);

        Instant instant = SchedulerUtil.computeToTimeBound(
                LocalDateTime.of(2018, java.time.Month.DECEMBER, 12, 00, 00)
                        .toInstant(ZoneOffset.UTC),
                timeSpan,
                SchedulerUtil.buildCalendar(buildTestCalendar())
        );
        assertEquals("2017-02-08T20:25:05Z", instant.toString());
    }

    @Test
    public void testTimeBoundComputeOnThreeDays() throws IOException {
        TimeSpan timeSpan = new TimeSpan();
        timeSpan.setDays((short) 3);

        HolidayCalendar holidayCalendar = SchedulerUtil.buildCalendar(buildTestCalendar());

        assertEquals("2017-12-26T06:00:00Z", SchedulerUtil.computeToTimeBound(Instant.parse("2017-12-29T06:00:00Z"), timeSpan, holidayCalendar).toString());
        assertEquals("2017-12-27T06:00:00Z", SchedulerUtil.computeToTimeBound(Instant.parse("2018-01-09T06:00:00Z"), timeSpan, holidayCalendar).toString());
        assertEquals("2017-12-28T06:00:00Z", SchedulerUtil.computeToTimeBound(Instant.parse("2018-01-10T06:00:00Z"), timeSpan, holidayCalendar).toString());
        assertEquals("2017-12-29T15:00:00Z", SchedulerUtil.computeToTimeBound(Instant.parse("2018-01-11T15:00:00Z"), timeSpan, holidayCalendar).toString());
        assertEquals("2018-01-09T15:00:00Z", SchedulerUtil.computeToTimeBound(Instant.parse("2018-01-12T15:00:00Z"), timeSpan, holidayCalendar).toString());

        assertEquals("2015-06-11T00:00:00Z", SchedulerUtil.computeToTimeBound(Instant.parse("2015-06-17T00:00:00Z"), timeSpan, holidayCalendar).toString());
        assertEquals("2015-06-15T22:10:00Z", SchedulerUtil.computeToTimeBound(Instant.parse("2015-06-18T22:10:00Z"), timeSpan, holidayCalendar).toString());
    }

    @Test
    public void testCronBuilderScheduleEvery() {
        ScheduleEvery scheduleEvery = new ScheduleEvery();

        Schedule schedule = new Schedule(
                ScheduleYear.every(scheduleEvery),
                ScheduleMonth.every(scheduleEvery),
                ScheduleFragment.every(scheduleEvery),
                ScheduleDayOfWeek.every(scheduleEvery),
                ScheduleFragment.every(scheduleEvery),
                ScheduleFragment.every(scheduleEvery),
                ScheduleFragment.every(scheduleEvery)
        );

        List<String> cronList = SchedulerUtil.buildCron(schedule);
        assertEquals(2, cronList.size());
        assertEquals("* * * * * ? *", cronList.get(0));
        assertEquals("* * * ? * * *", cronList.get(1));
        assertTrue(cronList.stream().allMatch(cron -> CronExpression.isValidExpression(cron)));
    }

    @Test
    public void testCronBuilderWithCustomValues() {
        ScheduleEvery scheduleEvery = new ScheduleEvery();
        scheduleEvery.setNth((byte) 5);

        Schedule schedule = new Schedule(
                ScheduleYear.every(scheduleEvery),
                ScheduleMonth.on(new HashSet<>(Arrays.asList(Jan, Feb, Mar, Apr, Oct, Nov))),
                ScheduleFragment.every(scheduleEvery),
                ScheduleDayOfWeek.on(new HashSet<>(Arrays.asList(Mon, Tue, Fri))),
                ScheduleFragment.on(new HashSet<>(Arrays.asList((byte) 1, (byte) 3, (byte) 4, (byte) 5, (byte) 12))),
                ScheduleFragment.every(scheduleEvery),
                ScheduleFragment.every(scheduleEvery)
        );

        List<String> cronList = SchedulerUtil.buildCron(schedule);
        assertEquals(2, cronList.size());
        assertEquals("*/5 */5 1,3,4,5,12 */5 1,2,3,4,10,11 ? */5", cronList.get(0));
        assertEquals("*/5 */5 1,3,4,5,12 ? 1,2,3,4,10,11 1,2,5 */5", cronList.get(1));
        assertTrue(cronList.stream().allMatch(cron -> CronExpression.isValidExpression(cron)));
    }

    @Test
    public void testBuildCalendar() throws IOException {
        Calendar calendar = buildTestCalendar();

        HolidayCalendar holidayCalendar = SchedulerUtil.buildCalendar(calendar);
        assertEquals(calendar.getName(), holidayCalendar.getDescription());
        assertEquals(calendar.getTimezone(), holidayCalendar.getTimeZone().getID());

        assertFalse(holidayCalendar.isTimeIncluded(LocalDate.of(2017, 11, 3).atTime(21, 00).toInstant(ZoneOffset.UTC).toEpochMilli()));
        assertFalse(holidayCalendar.isTimeIncluded(LocalDate.of(2017, 11, 4).atStartOfDay(ZoneId.of("Europe/Moscow")).toInstant().toEpochMilli()));

        assertTrue(holidayCalendar.isTimeIncluded(LocalDate.of(2017, 12, 29).atStartOfDay(ZoneId.of("Europe/Moscow")).toInstant().toEpochMilli()));
        assertTrue(holidayCalendar.isTimeIncluded(LocalDate.of(2018, 04, 27).atStartOfDay(ZoneId.of("Europe/Moscow")).toInstant().toEpochMilli()));
        assertTrue(holidayCalendar.isTimeIncluded(LocalDate.of(2018, 04, 28).atStartOfDay(ZoneId.of("Europe/Moscow")).toInstant().toEpochMilli()));
        assertTrue(holidayCalendar.isTimeIncluded(LocalDate.of(2018, 06, 9).atStartOfDay(ZoneId.of("Europe/Moscow")).toInstant().toEpochMilli()));
        assertTrue(holidayCalendar.isTimeIncluded(LocalDate.of(2018, 06, 8).atStartOfDay(ZoneId.of("Europe/Moscow")).toInstant().toEpochMilli()));

        assertFalse(holidayCalendar.isTimeIncluded(LocalDate.of(2017, 12, 30).atStartOfDay(ZoneId.of("Europe/Moscow")).toInstant().toEpochMilli()));
        assertFalse(holidayCalendar.isTimeIncluded(LocalDate.of(2017, 12, 31).atStartOfDay(ZoneId.of("Europe/Moscow")).toInstant().toEpochMilli()));
        assertFalse(holidayCalendar.isTimeIncluded(LocalDate.of(2018, 06, 11).atStartOfDay(ZoneId.of("Europe/Moscow")).toInstant().toEpochMilli()));
        assertFalse(holidayCalendar.isTimeIncluded(LocalDate.of(2018, 06, 12).atStartOfDay(ZoneId.of("Europe/Moscow")).toInstant().toEpochMilli()));

        for (Map.Entry<Integer, Set<CalendarHoliday>> holidays : calendar.getHolidays().entrySet()) {
            for (CalendarHoliday calendarHoliday : holidays.getValue()) {
                assertFalse(
                        holidayCalendar.isTimeIncluded(
                                LocalDate.of(
                                        holidays.getKey(),
                                        calendarHoliday.getMonth().getValue(), calendarHoliday.getDay()
                                ).atStartOfDay(ZoneId.of("Europe/Moscow")).toInstant().toEpochMilli()));
            }
        }
    }

    private Calendar buildTestCalendar() throws IOException {
        Calendar calendar = new Calendar();
        calendar.setName("test-calendar");
        calendar.setTimezone("Europe/Moscow");

        ClassPathResource resource = new ClassPathResource("/data/calendar-test.csv");
        try (CSVReader reader = new CSVReader(new InputStreamReader(resource.getInputStream()))) {
            reader.readNext();

            String[] row;
            Map<Integer, Set<CalendarHoliday>> years = new HashMap<>();
            while ((row = reader.readNext()) != null) {
                Set<CalendarHoliday> calendarHolidays = new HashSet<>();
                for (int monthValue = 1; monthValue <= 12; monthValue++) {
                    Month month = Month.findByValue(monthValue);
                    for (String day : row[monthValue].split(",")) {
                        if (!day.endsWith("*")) {
                            CalendarHoliday holiday = new CalendarHoliday();
                            holiday.setName("holiday");
                            holiday.setDay(Byte.valueOf(day));
                            holiday.setMonth(month);
                            calendarHolidays.add(holiday);
                        }
                    }
                }
                int year = Integer.valueOf(row[0]);
                years.put(year, calendarHolidays);
            }
            calendar.setHolidays(years);
        }
        return calendar;
    }
}
