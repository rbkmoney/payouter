package com.rbkmoney.payouter.scheduler;

import com.rbkmoney.damsel.payout_processing.InternalUser;
import com.rbkmoney.damsel.payout_processing.UserType;
import com.rbkmoney.glock.calendar.DefaultWorkingDayCalendar;
import com.rbkmoney.glock.calendar.WorkingDayCalendar;
import com.rbkmoney.glock.utils.CalendarUtils;
import com.rbkmoney.payouter.domain.enums.PayoutType;
import com.rbkmoney.payouter.service.PayoutService;
import com.rbkmoney.payouter.util.WoodyUtils;
import com.rbkmoney.woody.api.flow.WFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class SimplePayoutScheduler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final PayoutService payoutService;

    private final WorkingDayCalendar workingDayCalendar;

    private final WFlow wFlow;

    @Value("${scheduler.enabled}")
    private boolean schedulerEnabled;

    @Value("${scheduler.timezone}")
    private ZoneId schedulerTimezone;

    @Value("${scheduler.user-id}")
    private String userId;

    @Value("${scheduler.delayDays}")
    private int delayDays;

    @Value("${scheduler.periodDays}")
    private int periodDays;

    @Autowired
    public SimplePayoutScheduler(PayoutService payoutService) {
        this.payoutService = payoutService;
        this.workingDayCalendar = new DefaultWorkingDayCalendar();
        this.wFlow = new WFlow();
    }

    @Scheduled(cron = "${scheduler.cron}", zone = "${scheduler.timezone}")
    public void generateDailyPayout() throws Exception {
        if (!schedulerEnabled) {
            log.info("Scheduler disabled. Do nothing.");
            return;
        }

        ZonedDateTime now = ZonedDateTime.now(schedulerTimezone);
        if (workingDayCalendar.isHoliday(now.toLocalDate())) {
            log.info("Today is holiday. Do nothing.");
            return;
        }
        TimeRange timeRange = buildTimeRange(now, delayDays, periodDays);
        List<Long> payoutIds = wFlow.createServiceFork(
                () -> {
                    WoodyUtils.setUserInfo(userId, UserType.internal_user(new InternalUser()));
                    return payoutService.createPayouts(timeRange.getFrom(), timeRange.getTo(), PayoutType.bank_account);
                }
        ).call();

        log.info("Scheduled generate payout end. PayoutIds='{}'", payoutIds);
    }

    public TimeRange buildTimeRange(ZonedDateTime dayOfPayout, int delayDays, int periodDays) {
        LocalDateTime today = dayOfPayout.truncatedTo(ChronoUnit.DAYS)
                .toLocalDateTime();

        LocalDate from = CalendarUtils.minusWorkDays(workingDayCalendar, today.toLocalDate(), delayDays);
        LocalDate to = CalendarUtils.plusWorkDays(workingDayCalendar, from, periodDays);

        return new TimeRange(
                from.atStartOfDay(schedulerTimezone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime(),
                to.atStartOfDay(schedulerTimezone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
        );
    }

    public static class TimeRange {
        private final LocalDateTime from;
        private final LocalDateTime to;

        public TimeRange(LocalDateTime from, LocalDateTime to) {
            this.from = from;
            this.to = to;
        }

        public LocalDateTime getFrom() {
            return from;
        }

        public LocalDateTime getTo() {
            return to;
        }

        @Override
        public String toString() {
            return "TimeRange{" +
                    "from=" + from +
                    ", to=" + to +
                    '}';
        }
    }
}
