package com.rbkmoney.payouter.scheduler;

import com.rbkmoney.payouter.AbstractIntegrationTest;
import com.rbkmoney.payouter.scheduler.SimplePayoutScheduler.TimeRange;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.Assert.assertEquals;

public class SchedulerTest extends AbstractIntegrationTest {

    @Autowired
    public SimplePayoutScheduler simplePayoutScheduler;

    @Test
    public void testTimeRange() {
        //with weekend
        TimeRange timeRange = simplePayoutScheduler.buildTimeRange(toMoscowDateTime("2015-06-17T00:00:00Z"), 3, 1);
        assertEquals("TimeRange{from=2015-06-10T21:00, to=2015-06-14T21:00}", timeRange.toString());

        TimeRange timeRange2 = simplePayoutScheduler.buildTimeRange(toMoscowDateTime("2015-06-18T22:10:00Z"), 3, 1);
        assertEquals("TimeRange{from=2015-06-14T21:00, to=2015-06-15T21:00}", timeRange2.toString());

        TimeRange timeRange3 = simplePayoutScheduler.buildTimeRange(toMoscowDateTime("2015-06-15T13:10:00Z"), 3, 1);
        assertEquals("TimeRange{from=2015-06-08T21:00, to=2015-06-09T21:00}", timeRange3.toString());

        //if saturday
        TimeRange timeRange4 = simplePayoutScheduler.buildTimeRange(toMoscowDateTime("2015-06-20T13:10:00Z"), 3, 1);
        assertEquals("TimeRange{from=2015-06-16T21:00, to=2015-06-17T21:00}", timeRange4.toString());
    }

    @Test
    public void testWhenSaturdayIsWorkingDay() {
        TimeRange timeRange = simplePayoutScheduler.buildTimeRange(toMoscowDateTime("2018-06-14T15:00:00Z"), 3, 1);
        assertEquals("TimeRange{from=2018-06-07T21:00, to=2018-06-08T21:00}", timeRange.toString());

        //May 1
        timeRange = simplePayoutScheduler.buildTimeRange(toMoscowDateTime("2018-05-07T18:00:00Z"), 3,1);
        assertEquals("TimeRange{from=2018-04-27T21:00, to=2018-05-02T21:00}", timeRange.toString());
    }

    @Test
    public void testHappyNewYear() {
        TimeRange timeRange = simplePayoutScheduler.buildTimeRange(toMoscowDateTime("2018-01-11T15:00:00Z"), 3,1);
        assertEquals("TimeRange{from=2017-12-28T21:00, to=2018-01-08T21:00}", timeRange.toString());
    }

    @Test
    public void testHolidaysInNovember() {
        TimeRange timeRange = simplePayoutScheduler.buildTimeRange(toMoscowDateTime("2017-11-03T15:00:00Z"), 3, 1);
        assertEquals("TimeRange{from=2017-10-30T21:00, to=2017-10-31T21:00}", timeRange.toString());

        timeRange = simplePayoutScheduler.buildTimeRange(toMoscowDateTime("2017-11-07T11:00:00Z"), 3, 1);
        assertEquals("TimeRange{from=2017-10-31T21:00, to=2017-11-01T21:00}", timeRange.toString());

        timeRange = simplePayoutScheduler.buildTimeRange(toMoscowDateTime("2017-11-08T11:00:00Z"), 3, 1);
        assertEquals("TimeRange{from=2017-11-01T21:00, to=2017-11-02T21:00}", timeRange.toString());

        timeRange = simplePayoutScheduler.buildTimeRange(toMoscowDateTime("2017-11-09T11:00:00Z"), 3, 1);
        assertEquals("TimeRange{from=2017-11-02T21:00, to=2017-11-06T21:00}", timeRange.toString());

        timeRange = simplePayoutScheduler.buildTimeRange(toMoscowDateTime("2017-11-10T11:00:00Z"), 3, 1);
        assertEquals("TimeRange{from=2017-11-06T21:00, to=2017-11-07T21:00}", timeRange.toString());
    }

    public static ZonedDateTime toMoscowDateTime(String dateString) {
        return ZonedDateTime.parse(dateString).withZoneSameLocal(ZoneId.of("Europe/Moscow"));
    }

}
