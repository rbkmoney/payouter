package com.rbkmoney.payouter.scheduler;

import com.opencsv.CSVReader;
import com.rbkmoney.damsel.base.*;
import com.rbkmoney.damsel.domain.Calendar;
import com.rbkmoney.damsel.domain.CalendarHoliday;
import com.rbkmoney.payouter.trigger.FreezeTimeCronTrigger;
import com.rbkmoney.payouter.util.SchedulerUtil;
import org.junit.Ignore;
import org.junit.Test;
import org.quartz.CronExpression;
import org.quartz.impl.calendar.HolidayCalendar;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

import static com.rbkmoney.damsel.base.DayOfWeek.*;
import static com.rbkmoney.damsel.base.Month.*;
import static org.junit.Assert.*;

public class SchedulerUtilTest {

    @Test
    public void testStartOfWeekOnThirdWorkingDay() throws ParseException, IOException {
        FreezeTimeCronTrigger trigger = new FreezeTimeCronTrigger();
        trigger.setCronExpression(new CronExpression("0 0 0 ? * MON *"));
        trigger.setStartTime(
                Date.from(
                        LocalDate.of(2018, java.time.Month.APRIL, 24)
                                .atStartOfDay(ZoneId.of("Europe/Moscow"))
                                .toInstant()
                )
        );
        trigger.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
        trigger.withDays(2);

        HolidayCalendar calendar = SchedulerUtil.buildCalendar(buildTestCalendar());
        trigger.computeFirstFireTime(calendar);

        //since April 24, 2018
        assertEquals(
                LocalDate.of(2018, java.time.Month.APRIL, 25)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextFireTime().toInstant()
        );
        assertEquals(
                LocalDate.of(2018, java.time.Month.APRIL, 23)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextCronTime().toInstant()
        );

        //continue from 25 April, 2018
        trigger.triggered(calendar);
        assertEquals(
                LocalDate.of(2018, java.time.Month.MAY, 7)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextFireTime().toInstant()
        );
        assertEquals(
                LocalDate.of(2018, java.time.Month.APRIL, 30)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextCronTime().toInstant()
        );

        //continue from 7 May, 2018
        trigger.triggered(calendar);

        assertEquals(
                LocalDate.of(2018, java.time.Month.MAY, 10)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextFireTime().toInstant()
        );
        assertEquals(
                LocalDate.of(2018, java.time.Month.MAY, 07)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextCronTime().toInstant()
        );

        //since December 29, 2017
        trigger.setStartTime(
                Date.from(
                        LocalDate.of(2017, java.time.Month.DECEMBER, 29)
                                .atStartOfDay(ZoneId.of("Europe/Moscow"))
                                .toInstant()
                )
        );

        trigger.computeFirstFireTime(calendar);
        assertEquals(
                LocalDate.of(2018, java.time.Month.JANUARY, 11)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextFireTime().toInstant()
        );
        assertEquals(
                LocalDate.of(2018, java.time.Month.JANUARY, 8)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextCronTime().toInstant()
        );

        //continue 11 january, 2018
        trigger.triggered(calendar);
        assertEquals(
                LocalDate.of(2018, java.time.Month.JANUARY, 17)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextFireTime().toInstant()
        );
        assertEquals(
                LocalDate.of(2018, java.time.Month.JANUARY, 15)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextCronTime().toInstant()
        );

        trigger.updateWithNewCalendar(calendar, 1000L);
        assertEquals(
                LocalDate.of(2018, java.time.Month.JANUARY, 24)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextFireTime().toInstant()
        );
        assertEquals(
                LocalDate.of(2018, java.time.Month.JANUARY, 22)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextCronTime().toInstant()
        );
    }

    @Test
    public void testStartOfMonthOnThirdWorkingDay() throws ParseException, IOException {
        FreezeTimeCronTrigger trigger = new FreezeTimeCronTrigger();
        trigger.setCronExpression(new CronExpression("0 0 0 1 * ? *"));
        trigger.setStartTime(
                Date.from(
                        LocalDate.of(2018, java.time.Month.APRIL, 24)
                                .atStartOfDay(ZoneId.of("Europe/Moscow"))
                                .toInstant()
                )
        );
        trigger.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
        trigger.withDays(2);

        HolidayCalendar calendar = SchedulerUtil.buildCalendar(buildTestCalendar());
        trigger.computeFirstFireTime(calendar);

        //since April 24, 2018
        assertEquals(
                LocalDate.of(2018, java.time.Month.MAY, 7)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextFireTime().toInstant()
        );
        assertEquals(
                LocalDate.of(2018, java.time.Month.MAY, 01)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextCronTime().toInstant()
        );

        //continue from 7 May, 2018
        trigger.triggered(calendar);
        assertEquals(
                LocalDate.of(2018, java.time.Month.JUNE, 5)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextFireTime().toInstant()
        );
        assertEquals(
                LocalDate.of(2018, java.time.Month.JUNE, 01)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextCronTime().toInstant()
        );

        //since December 29, 2017
        trigger.setStartTime(
                Date.from(
                        LocalDate.of(2017, java.time.Month.DECEMBER, 29)
                                .atStartOfDay(ZoneId.of("Europe/Moscow"))
                                .toInstant()
                )
        );

        trigger.computeFirstFireTime(calendar);
        assertEquals(
                LocalDate.of(2018, java.time.Month.JANUARY, 11)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextFireTime().toInstant()
        );
        assertEquals(
                LocalDate.of(2018, java.time.Month.JANUARY, 01)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextCronTime().toInstant()
        );

        //continue 11 january, 2018
        trigger.triggered(calendar);
        assertEquals(
                LocalDate.of(2018, java.time.Month.FEBRUARY, 5)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextFireTime().toInstant()
        );
        assertEquals(
                LocalDate.of(2018, java.time.Month.FEBRUARY, 1)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextCronTime().toInstant()
        );

        trigger.updateWithNewCalendar(calendar, 1000L);
        assertEquals(
                LocalDate.of(2018, java.time.Month.MARCH, 5)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextFireTime().toInstant()
        );
        assertEquals(
                LocalDate.of(2018, java.time.Month.MARCH, 1)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextCronTime().toInstant()
        );
    }

    @Test
    @Ignore //TODO PASHA
    public void testEveryDayOnThirdWorkingDay() throws ParseException, IOException {
        FreezeTimeCronTrigger trigger = new FreezeTimeCronTrigger();
        trigger.setCronExpression(new CronExpression("0 0 0 * * ? *"));
        trigger.withDays(2);

        HolidayCalendar calendar = SchedulerUtil.buildCalendar(buildTestCalendar());

        //since December 29, 2017
        trigger.setStartTime(
                Date.from(
                        LocalDate.of(2017, java.time.Month.DECEMBER, 29)
                                .atStartOfDay(ZoneId.of("Europe/Moscow"))
                                .toInstant()
                )
        );

        trigger.computeFirstFireTime(calendar);
        assertEquals(
                LocalDate.of(2017, java.time.Month.DECEMBER, 29)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextFireTime().toInstant()
        );
        assertEquals(
                LocalDate.of(2017, java.time.Month.DECEMBER, 27)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextCronTime().toInstant()
        );

        trigger.triggered(calendar);
        assertEquals(
                LocalDate.of(2018, java.time.Month.JANUARY, 9)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextFireTime().toInstant()
        );
        assertEquals(
                LocalDate.of(2017, java.time.Month.DECEMBER, 28)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextCronTime().toInstant()
        );

        trigger.triggered(calendar);
        assertEquals(
                LocalDate.of(2018, java.time.Month.JANUARY, 10)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextFireTime().toInstant()
        );
        assertEquals(
                LocalDate.of(2017, java.time.Month.DECEMBER, 29)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextCronTime().toInstant()
        );

        trigger.triggered(calendar);
        assertEquals(
                LocalDate.of(2018, java.time.Month.JANUARY, 11)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextFireTime().toInstant()
        );
        assertEquals(
                LocalDate.of(2018, java.time.Month.JANUARY, 9)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextCronTime().toInstant()
        );

        trigger.triggered(calendar);
        assertEquals(
                LocalDate.of(2018, java.time.Month.JANUARY, 12)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextFireTime().toInstant()
        );
        assertEquals(
                LocalDate.of(2018, java.time.Month.JANUARY, 10)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextCronTime().toInstant()
        );

        //since March 6, 2018
        trigger.setStartTime(
                Date.from(
                        LocalDate.of(2018, java.time.Month.MARCH, 6)
                                .atStartOfDay(ZoneId.of("Europe/Moscow"))
                                .toInstant()
                )
        );
        trigger.computeFirstFireTime(calendar);

        assertEquals(
                LocalDate.of(2018, java.time.Month.MARCH, 6)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextFireTime().toInstant()
        );
        assertEquals(
                LocalDate.of(2018, java.time.Month.MARCH, 2)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextCronTime().toInstant()
        );

        trigger.triggered(calendar);
        assertEquals(
                LocalDate.of(2018, java.time.Month.MARCH, 7)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextFireTime().toInstant()
        );
        assertEquals(
                LocalDate.of(2018, java.time.Month.MARCH, 5)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextCronTime().toInstant()
        );

        trigger.triggered(calendar);
        assertEquals(
                LocalDate.of(2018, java.time.Month.MARCH, 12)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextFireTime().toInstant()
        );
        assertEquals(
                LocalDate.of(2018, java.time.Month.MARCH, 6)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextCronTime().toInstant()
        );

        trigger.triggered(calendar);
        assertEquals(
                LocalDate.of(2018, java.time.Month.MARCH, 13)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextFireTime().toInstant()
        );
        assertEquals(
                LocalDate.of(2018, java.time.Month.MARCH, 7)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextCronTime().toInstant()
        );

        trigger.triggered(calendar);
        assertEquals(
                LocalDate.of(2018, java.time.Month.MARCH, 14)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextFireTime().toInstant()
        );
        assertEquals(
                LocalDate.of(2018, java.time.Month.MARCH, 12)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextCronTime().toInstant()
        );

        trigger.updateWithNewCalendar(calendar, 1000L);
        assertEquals(
                LocalDate.of(2018, java.time.Month.MARCH, 15)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextFireTime().toInstant()
        );
        assertEquals(
                LocalDate.of(2018, java.time.Month.MARCH, 13)
                        .atStartOfDay(ZoneId.of("Europe/Moscow"))
                        .toInstant(),
                trigger.getNextCronTime().toInstant()
        );
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
        assertEquals(1, cronList.size());
        assertEquals("* * * * * ? *", cronList.get(0));
        assertTrue(cronList.stream().allMatch(cron -> CronExpression.isValidExpression(cron)));
    }

    @Test
    public void testCronWhenOnlyWeekSet() {
        ScheduleEvery scheduleEvery = new ScheduleEvery();
        Schedule schedule = new Schedule(
                ScheduleYear.every(scheduleEvery),
                ScheduleMonth.every(scheduleEvery),
                ScheduleFragment.every(scheduleEvery),
                ScheduleDayOfWeek.on(new HashSet<>(Arrays.asList(Mon, Sun, Sat))),
                ScheduleFragment.every(scheduleEvery),
                ScheduleFragment.every(scheduleEvery),
                ScheduleFragment.every(scheduleEvery)
        );

        List<String> cronList = SchedulerUtil.buildCron(schedule);
        assertEquals(1, cronList.size());
        assertEquals("* * * ? * 1,6,7 *", cronList.get(0));
        assertTrue(CronExpression.isValidExpression(cronList.get(0)));


        ScheduleEvery scheduleEvery3daysOfWeek = new ScheduleEvery();
        scheduleEvery3daysOfWeek.setNth((byte) 3);
        schedule = new Schedule(
                ScheduleYear.every(scheduleEvery),
                ScheduleMonth.every(scheduleEvery),
                ScheduleFragment.every(scheduleEvery),
                ScheduleDayOfWeek.every(scheduleEvery3daysOfWeek),
                ScheduleFragment.every(scheduleEvery),
                ScheduleFragment.every(scheduleEvery),
                ScheduleFragment.every(scheduleEvery)
        );

        cronList = SchedulerUtil.buildCron(schedule);
        assertEquals(1, cronList.size());
        assertEquals("* * * ? * */3 *", cronList.get(0));
        assertTrue(CronExpression.isValidExpression(cronList.get(0)));
    }

    @Test
    public void testCronWhenOnlyDayOfMonthSet() {
        ScheduleEvery scheduleEvery = new ScheduleEvery();
        Schedule schedule = new Schedule(
                ScheduleYear.every(scheduleEvery),
                ScheduleMonth.every(scheduleEvery),
                ScheduleFragment.on(new HashSet<>(Arrays.asList((byte) 6, (byte) 10, (byte) 31))),
                ScheduleDayOfWeek.every(scheduleEvery),
                ScheduleFragment.every(scheduleEvery),
                ScheduleFragment.every(scheduleEvery),
                ScheduleFragment.every(scheduleEvery)
        );

        List<String> cronList = SchedulerUtil.buildCron(schedule);
        assertEquals(1, cronList.size());
        assertEquals("* * * 6,10,31 * ? *", cronList.get(0));
        assertTrue(CronExpression.isValidExpression(cronList.get(0)));


        ScheduleEvery scheduleEvery3daysOfMonth = new ScheduleEvery();
        scheduleEvery3daysOfMonth.setNth((byte) 3);
        schedule = new Schedule(
                ScheduleYear.every(scheduleEvery),
                ScheduleMonth.every(scheduleEvery),
                ScheduleFragment.every(scheduleEvery3daysOfMonth),
                ScheduleDayOfWeek.every(scheduleEvery),
                ScheduleFragment.every(scheduleEvery),
                ScheduleFragment.every(scheduleEvery),
                ScheduleFragment.every(scheduleEvery)
        );

        cronList = SchedulerUtil.buildCron(schedule);
        assertEquals(1, cronList.size());
        assertEquals("* * * */3 * ? *", cronList.get(0));
        assertTrue(CronExpression.isValidExpression(cronList.get(0)));
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
        assertEquals("*/5 */5 1,3,4,5,12 ? 1,2,3,4,10,11 2,3,6 */5", cronList.get(1));
        assertTrue(cronList.stream().allMatch(cron -> CronExpression.isValidExpression(cron)));
    }

    @Test
    public void testDOWMapToQuartzFormat() {
        Schedule schedule = new Schedule(
                ScheduleYear.every(new ScheduleEvery()),
                ScheduleMonth.every(new ScheduleEvery()),
                ScheduleFragment.every(new ScheduleEvery()),
                ScheduleDayOfWeek.on(new HashSet<>(Arrays.asList(Mon, Fri, Sun))),
                ScheduleFragment.every(new ScheduleEvery()),
                ScheduleFragment.every(new ScheduleEvery()),
                ScheduleFragment.every(new ScheduleEvery())
        );

        List<String> cronList = SchedulerUtil.buildCron(schedule);
        assertEquals(1, cronList.size());
        assertEquals("* * * ? * 1,2,6 *", cronList.get(0));
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
